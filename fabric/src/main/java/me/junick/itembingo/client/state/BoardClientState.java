package me.junick.itembingo.client.state;

import me.junick.itembingo.client.net.ModProtocol;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The client's single source of truth for everything the server has told us:
 * handshake outcome and the latest per-viewer board view. The fullscreen
 * screen and the HUD overlay only ever read from here; network receivers only
 * ever write. {@link #revision} bumps on every change so open UIs re-read
 * lazily instead of being pushed to.
 */
public final class BoardClientState {
    private BoardClientState() {}

    public enum ConnectionState {
        /** Haven't heard from the server yet (handshake pending). */
        UNKNOWN,
        /** Server never announced our channels — vanilla server or no plugin. */
        UNSUPPORTED,
        /** Plugin present but its protocol version differs from ours. */
        MISMATCH,
        /** Handshake complete; board syncing is live. */
        ACTIVE
    }

    private static ConnectionState connection = ConnectionState.UNKNOWN;
    private static int serverProtocolVersion = -1;

    private static byte status = ModProtocol.STATUS_NO_BOARD;
    private static byte gameMode;
    private static byte flags;
    private static int width;
    private static int height;
    private static int submittedCount;
    private static int totalCells;
    private static CellState[] cells = new CellState[0];
    private static int revision;

    /** Board cell coordinates of the HUD overlay window's top-left corner. */
    private static int hudCol;
    private static int hudRow;
    /** False until the player frames a region themselves (HUD centers instead). */
    private static boolean hudViewportSet;

    /** How long a freshly-submitted cell glows, in ms. */
    public static final long FLASH_MS = 700;
    /** Cell index → ms timestamp of the push that turned it submitted. */
    private static final Map<Integer, Long> FLASHES = new HashMap<>();

    /* ------------------------- connection ------------------------- */

    public static ConnectionState connection() { return connection; }
    public static int serverProtocolVersion() { return serverProtocolVersion; }

    public static void setConnection(ConnectionState state, int serverVersion) {
        connection = state;
        serverProtocolVersion = serverVersion;
        revision++;
    }

    /** Reset everything on disconnect; the next join re-runs the handshake. */
    public static void clear() {
        connection = ConnectionState.UNKNOWN;
        serverProtocolVersion = -1;
        status = ModProtocol.STATUS_NO_BOARD;
        cells = new CellState[0];
        width = height = submittedCount = totalCells = 0;
        gameMode = 0;
        flags = 0;
        hudViewportSet = false;
        FLASHES.clear();
        revision++;
    }

    /* ------------------------- board view ------------------------- */

    public static byte status() { return status; }
    public static boolean hasBoard() { return status == ModProtocol.STATUS_OK && cells.length > 0; }
    public static byte gameMode() { return gameMode; }
    public static boolean fogSubmitLock() { return (flags & ModProtocol.FLAG_FOG_SUBMIT_LOCK) != 0; }
    public static int width() { return width; }
    public static int height() { return height; }
    public static int submittedCount() { return submittedCount; }
    public static int totalCells() { return totalCells; }
    public static int revision() { return revision; }

    /** Row-major: {@code index = row * width + col}; null outside the board. */
    @Nullable
    public static CellState cell(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) return null;
        return cells[row * width + col];
    }

    @Nullable
    public static CellState cell(int index) {
        if (index < 0 || index >= cells.length) return null;
        return cells[index];
    }

    /** Decodes an {@code itembingo:board} push. Malformed data is dropped whole. */
    public static void applyBoardPacket(byte[] data) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            byte newStatus = in.readByte();
            if (newStatus != ModProtocol.STATUS_OK) {
                status = newStatus;
                cells = new CellState[0];
                width = height = submittedCount = totalCells = 0;
                FLASHES.clear();
                revision++;
                return;
            }

            byte newMode = in.readByte();
            byte newFlags = in.readByte();
            int w = in.readUnsignedShort();
            int h = in.readUnsignedShort();
            int submitted = in.readUnsignedShort();
            int total = in.readUnsignedShort();

            CellState[] newCells = new CellState[w * h];
            for (int i = 0; i < newCells.length; i++) {
                byte kind = in.readByte();
                newCells[i] = switch (kind) {
                    case ModProtocol.CELL_VISIBLE -> CellState.visible(in.readUTF());
                    case ModProtocol.CELL_HIDDEN -> CellState.HIDDEN;
                    case ModProtocol.CELL_LOCKED -> CellState.LOCKED;
                    case ModProtocol.CELL_SUBMITTED -> {
                        String key = in.readUTF();
                        boolean hasSubmitter = in.readBoolean();
                        String name = hasSubmitter ? in.readUTF() : "";
                        long elapsed = in.readLong();
                        yield CellState.submitted(key, hasSubmitter, name, elapsed);
                    }
                    default -> throw new IOException("unknown cell state " + kind);
                };
            }

            // Same board still in play: flash cells that just turned submitted.
            // A new/resized board (or the first push) starts with a clean slate,
            // and the HUD re-centers on it.
            boolean sameBoard = status == ModProtocol.STATUS_OK && w == width && h == height;
            if (sameBoard) {
                long now = System.currentTimeMillis();
                for (int i = 0; i < newCells.length; i++) {
                    if (newCells[i].isSubmitted() && !cells[i].isSubmitted()) {
                        FLASHES.put(i, now);
                    }
                }
            } else {
                FLASHES.clear();
                hudViewportSet = false;
            }

            status = ModProtocol.STATUS_OK;
            gameMode = newMode;
            flags = newFlags;
            width = w;
            height = h;
            submittedCount = submitted;
            totalCells = total;
            cells = newCells;
            hudCol = Math.clamp(hudCol, 0, Math.max(0, w - 1));
            hudRow = Math.clamp(hudRow, 0, Math.max(0, h - 1));
            revision++;
        } catch (IOException e) {
            // Malformed push (shouldn't happen with a matching protocol) — keep
            // the previous consistent view rather than showing garbage.
        }
    }

    /* ------------------------- flashes ------------------------- */

    /** 0..1 glow strength for a freshly-submitted cell, 0 when idle. */
    public static float flashAlpha(int idx, long now) {
        if (FLASHES.isEmpty()) return 0;
        Long start = FLASHES.get(idx);
        if (start == null) return 0;
        long age = now - start;
        if (age >= FLASH_MS) {
            FLASHES.remove(idx);
            return 0;
        }
        return 1.0f - (float) age / FLASH_MS;
    }

    /* ------------------------- HUD viewport ------------------------- */

    public static int hudCol() { return hudCol; }
    public static int hudRow() { return hudRow; }

    /** Whether the player has framed a region themselves (via the fullscreen GUI). */
    public static boolean hasHudViewport() { return hudViewportSet; }

    public static void setHudViewport(int col, int row) {
        hudCol = Math.clamp(col, 0, Math.max(0, width - 1));
        hudRow = Math.clamp(row, 0, Math.max(0, height - 1));
        hudViewportSet = true;
    }
}
