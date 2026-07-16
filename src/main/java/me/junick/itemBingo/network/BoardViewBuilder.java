package me.junick.itemBingo.network;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.gui.BingoGUI;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.util.BoardCellView;
import me.junick.itemBingo.util.FogOfWar;
import me.junick.itemBingo.util.Lockout;
import me.junick.itemBingo.util.ProgressFactory;
import me.junick.itemBingo.util.TimerManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Serializes the board as ONE viewer is allowed to see it, resolving each cell
 * through the shared {@link BoardCellView} priority. Fog-hidden cells carry no
 * item data at all — a modded client must not learn hidden cells from packet
 * inspection.
 */
public final class BoardViewBuilder {
    private BoardViewBuilder() {}

    public static byte[] build(Player viewer) {
        return build(viewer, null);
    }

    /**
     * @param allClaimed in Lockout broadcasts, every claimed cell (computed
     *                   once per broadcast); {@code null} to derive this
     *                   viewer's locked set directly
     */
    public static byte[] build(Player viewer, @Nullable Set<Integer> allClaimed) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            BingoBoard board = ItemBingo.currentBingo;
            if (board == null) {
                out.writeByte(ModProtocol.STATUS_NO_BOARD);
                return bytes.toByteArray();
            }
            if (!BingoGUI.canView(viewer)) {
                out.writeByte(ModProtocol.STATUS_VIEW_DENIED);
                return bytes.toByteArray();
            }

            BingoProgressAccess progress = ProgressFactory.of(viewer);
            int w = board.getWidth();
            int h = board.getHeight();

            boolean fog = Settings.isFogOfWarMode();
            Set<Integer> revealed = fog
                    ? FogOfWar.revealedSlots(w, h, progress.getSubmittedSlots(), Settings.isFogDiagonalReveal())
                    : null;
            Set<Integer> locked;
            if (allClaimed != null) {
                locked = new HashSet<>(allClaimed);
                locked.removeAll(progress.getSubmittedSlots());
            } else {
                locked = Lockout.lockedSlots(viewer); // empty outside Lockout mode
            }

            int flags = 0;
            if (fog && Settings.isFogSubmitLock()) flags |= ModProtocol.FLAG_FOG_SUBMIT_LOCK;
            if (Settings.isTeamEnabled()) flags |= ModProtocol.FLAG_TEAM_MODE;

            out.writeByte(ModProtocol.STATUS_OK);
            out.writeByte(modeByte());
            out.writeByte(stageByte());
            out.writeByte(flags);
            out.writeShort(w);
            out.writeShort(h);
            out.writeShort(progress.getSubmittedSlots().size());
            out.writeShort(board.getItems().size());

            for (int idx = 0; idx < w * h; idx++) {
                switch (BoardCellView.of(idx, progress, revealed, locked)) {
                    case SUBMITTED -> {
                        out.writeByte(ModProtocol.CELL_SUBMITTED);
                        out.writeUTF(board.getItems().get(idx).getType().getKey().toString());
                        String submitter = progress.getSubmitterName(idx);
                        boolean hasSubmitter = progress.getSubmitterId(idx) != null;
                        out.writeBoolean(hasSubmitter);
                        if (hasSubmitter) {
                            out.writeUTF(submitter != null ? submitter : "");
                        }
                        out.writeLong(progress.getSubmissionTime(idx));
                    }
                    case LOCKED -> out.writeByte(ModProtocol.CELL_LOCKED);
                    case HIDDEN -> out.writeByte(ModProtocol.CELL_HIDDEN);
                    case VISIBLE -> {
                        out.writeByte(ModProtocol.CELL_VISIBLE);
                        out.writeUTF(board.getItems().get(idx).getType().getKey().toString());
                    }
                }
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            // ByteArrayOutputStream never throws; required by DataOutputStream's API.
            throw new UncheckedIOException(e);
        }
    }

    /** Explicit wire bytes — enum ordinals must never leak into the protocol. */
    private static byte modeByte() {
        return switch (Settings.getGameMode()) {
            case SWAPPAGE -> ModProtocol.MODE_SWAPPAGE;
            case FOG_OF_WAR -> ModProtocol.MODE_FOG_OF_WAR;
            case LOCKOUT -> ModProtocol.MODE_LOCKOUT;
            default -> ModProtocol.MODE_NORMAL;
        };
    }

    private static byte stageByte() {
        return switch (TimerManager.getStage()) {
            case RUNNING -> ModProtocol.STAGE_RUNNING;
            case ENDED -> ModProtocol.STAGE_ENDED;
            default -> ModProtocol.STAGE_NOT_STARTED;
        };
    }
}
