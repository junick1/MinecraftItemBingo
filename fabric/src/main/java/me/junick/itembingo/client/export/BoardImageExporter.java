package me.junick.itembingo.client.export;

import com.mojang.blaze3d.platform.NativeImage;
import me.junick.itembingo.client.net.ClientNetworking;
import me.junick.itembingo.client.net.ModProtocol;
import me.junick.itembingo.client.screen.BingoBoardScreen;
import me.junick.itembingo.client.state.BoardClientState;
import me.junick.itembingo.client.state.CellState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Exports the board as a PNG: either the pristine original (server-gated —
 * denied mid-Fog-of-War) or the player's current per-viewer progress view.
 *
 * <p>Capture strategy: while armed, {@link BingoBoardScreen} renders a clean
 * export layout instead of its normal UI for a couple of frames; we then read
 * the main framebuffer (physical resolution — GUI scale gives 2-3x the logical
 * pixels) and crop the board rectangle. No offscreen render plumbing, real
 * item models, version-proof.
 */
public final class BoardImageExporter {
    private BoardImageExporter() {}

    public enum Variant { ORIGINAL, PROGRESS }
    public enum Action { COPY, SAVE }

    private enum Phase { IDLE, WAIT_ORIGINAL, ARMED }

    private static Phase phase = Phase.IDLE;
    private static Variant variant;
    private static Action action;
    private static int timeoutTicks;

    private static CellState[] cells;
    private static int boardW;
    private static int boardH;
    private static Component caption;

    /** Feedback line shown in the export popup ({@code null} = nothing). */
    @Nullable
    private static Component status;

    private static int framesRendered;
    private static int rectX, rectY, rectW, rectH;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(BoardImageExporter::tick);
    }

    /* ------------------------- state for the screen ------------------------- */

    public static boolean armed() { return phase == Phase.ARMED; }
    public static boolean busy() { return phase != Phase.IDLE; }
    @Nullable public static Component status() { return status; }
    public static CellState[] cells() { return cells; }
    public static int boardWidth() { return boardW; }
    public static int boardHeight() { return boardH; }
    public static Component caption() { return caption; }

    /** The screen reports the logical rect it drew the export layout into. */
    public static void onExportFrame(int x, int y, int w, int h) {
        rectX = x;
        rectY = y;
        rectW = w;
        rectH = h;
        framesRendered++;
    }

    /* ------------------------- flow ------------------------- */

    public static void begin(Variant v, Action a) {
        if (phase != Phase.IDLE) return;
        variant = v;
        action = a;
        status = null;

        if (v == Variant.PROGRESS) {
            if (!BoardClientState.hasBoard()) {
                status = Component.translatable("itembingo.export.no_board").withStyle(ChatFormatting.RED);
                return;
            }
            int w = BoardClientState.width();
            int h = BoardClientState.height();
            CellState[] snapshot = new CellState[w * h];
            for (int i = 0; i < snapshot.length; i++) {
                snapshot[i] = BoardClientState.cell(i);
            }
            Component modeName = Component.translatable(switch (BoardClientState.gameMode()) {
                case 1 -> "itembingo.screen.mode.swappage";
                case 2 -> "itembingo.screen.mode.fog_of_war";
                case 3 -> "itembingo.screen.mode.lockout";
                default -> "itembingo.screen.mode.normal";
            });
            arm(snapshot, w, h, Component.translatable("itembingo.export.caption.progress",
                    BoardClientState.submittedCount(), BoardClientState.totalCells(), modeName,
                    w + "x" + h, dateStamp()));
        } else {
            status = Component.translatable("itembingo.export.requesting").withStyle(ChatFormatting.GRAY);
            phase = Phase.WAIT_ORIGINAL;
            timeoutTicks = 100;
            ClientNetworking.sendOriginalRequest();
        }
    }

    /** Parses the {@code itembingo:original} response. */
    public static void handleOriginalResponse(byte[] data) {
        if (phase != Phase.WAIT_ORIGINAL) return;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            byte code = in.readByte();
            switch (code) {
                case ModProtocol.ORIGINAL_OK -> {
                    int w = in.readUnsignedShort();
                    int h = in.readUnsignedShort();
                    CellState[] board = new CellState[w * h];
                    for (int i = 0; i < board.length; i++) {
                        board[i] = CellState.visible(in.readUTF());
                    }
                    arm(board, w, h, Component.translatable("itembingo.export.caption.original",
                            w + "x" + h, dateStamp()));
                }
                case ModProtocol.ORIGINAL_DENIED_FOG -> fail("itembingo.export.denied_fog");
                default -> fail("itembingo.export.no_board");
            }
        } catch (IOException e) {
            fail("itembingo.export.failed");
        }
    }

    private static void arm(CellState[] board, int w, int h, Component cap) {
        cells = board;
        boardW = w;
        boardH = h;
        caption = cap;
        framesRendered = 0;
        status = null;
        phase = Phase.ARMED;
    }

    private static void fail(String key) {
        status = Component.translatable(key).withStyle(ChatFormatting.RED);
        phase = Phase.IDLE;
    }

    /** Screen closed or state reset — abandon whatever was pending. */
    public static void cancel() {
        phase = Phase.IDLE;
        status = null;
        cells = null;
    }

    private static void tick(Minecraft mc) {
        switch (phase) {
            case WAIT_ORIGINAL -> {
                if (--timeoutTicks <= 0) fail("itembingo.export.failed");
            }
            case ARMED -> {
                if (!(mc.gui.screen() instanceof BingoBoardScreen)) {
                    cancel();
                } else if (framesRendered >= 2) {
                    capture(mc);
                }
            }
            default -> {}
        }
    }

    /* ------------------------- capture + delivery ------------------------- */

    private static void capture(Minecraft mc) {
        phase = Phase.IDLE; // the framebuffer already holds the export frame
        Variant v = variant;
        Action a = action;
        int scale = mc.getWindow().getGuiScale();
        int sx = rectX * scale;
        int sy = rectY * scale;
        int sw = rectW * scale;
        int sh = rectH * scale;

        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), full -> {
            try (full) {
                int cx = Math.clamp(sx, 0, full.getWidth() - 1);
                int cy = Math.clamp(sy, 0, full.getHeight() - 1);
                int cw = Math.min(sw, full.getWidth() - cx);
                int ch = Math.min(sh, full.getHeight() - cy);
                NativeImage cropped = new NativeImage(cw, ch, false);
                full.copyRect(cropped, cx, cy, 0, 0, cw, ch, false, false);
                deliver(mc, cropped, v, a);
            } catch (Exception e) {
                message(mc, Component.translatable("itembingo.export.failed").withStyle(ChatFormatting.RED));
            }
        });
    }

    private static void deliver(Minecraft mc, NativeImage image, Variant v, Action a) {
        if (a == Action.COPY) {
            if (copyToClipboard(image)) {
                image.close();
                message(mc, Component.translatable("itembingo.export.copied").withStyle(ChatFormatting.GREEN));
                return;
            }
            message(mc, Component.translatable("itembingo.export.copy_failed_saved").withStyle(ChatFormatting.YELLOW));
        }
        save(mc, image, v);
    }

    /** AWT image clipboard; works on Windows/Linux, may refuse on macOS. */
    private static boolean copyToClipboard(NativeImage image) {
        try {
            System.setProperty("java.awt.headless", "false");
            int w = image.getWidth();
            int h = image.getHeight();
            BufferedImage awt = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = image.getPixel(x, y);
                    int r = abgr & 0xFF;
                    int g = (abgr >> 8) & 0xFF;
                    int b = (abgr >> 16) & 0xFF;
                    awt.setRGB(x, y, (r << 16) | (g << 8) | b);
                }
            }
            Transferable payload = new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[]{DataFlavor.imageFlavor};
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return DataFlavor.imageFlavor.equals(flavor);
                }

                @Override
                public Object getTransferData(DataFlavor flavor) {
                    return awt;
                }
            };
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(payload, null);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void save(Minecraft mc, NativeImage image, Variant v) {
        try (image) {
            File dir = new File(mc.gameDirectory, "screenshots/itembingo");
            if (!dir.exists() && !dir.mkdirs()) {
                message(mc, Component.translatable("itembingo.export.failed").withStyle(ChatFormatting.RED));
                return;
            }
            String name = "bingo-" + (v == Variant.ORIGINAL ? "original" : "progress")
                    + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".png";
            File file = new File(dir, name);
            image.writeToFile(file);
            Component link = Component.literal(name).withStyle(style -> style
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent.OpenFile(file)));
            message(mc, Component.translatable("itembingo.export.saved", link).withStyle(ChatFormatting.GREEN));
        } catch (IOException e) {
            message(mc, Component.translatable("itembingo.export.failed").withStyle(ChatFormatting.RED));
        }
    }

    private static void message(Minecraft mc, Component text) {
        if (mc.player != null) {
            mc.player.sendSystemMessage(text);
        }
    }

    private static String dateStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
