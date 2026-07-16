package me.junick.itembingo.client.export;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import me.junick.itembingo.client.mixin.GameRendererAccessor;
import me.junick.itembingo.client.net.ClientNetworking;
import me.junick.itembingo.client.net.ModProtocol;
import me.junick.itembingo.client.screen.BingoBoardScreen;
import me.junick.itembingo.client.state.BoardClientState;
import me.junick.itembingo.client.state.CellState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Exports the board as a PNG "results card": header (title + chips), the full
 * grid, and — in team play — a ranked team-contribution list with skin-face
 * avatars. Variants: the pristine original (server-gated) or the player's
 * current progress view.
 *
 * <p>The card is rendered COMPLETELY OFFSCREEN: a private {@link GuiRenderer}
 * draws the extracted card into a throwaway {@link TextureTarget} (swapped in
 * as the "main" target via {@link GameRendererAccessor} for the duration of
 * the render), which is then read back and cropped. The player never sees a
 * frame of it.
 */
public final class BoardImageExporter {
    private BoardImageExporter() {}

    public enum Variant { ORIGINAL, PROGRESS }
    public enum Action { COPY, SAVE }

    /** One teammate's row in the contribution list. */
    public record Contribution(String name, int count) {}

    private enum Phase { IDLE, WAIT_ORIGINAL }

    private static Phase phase = Phase.IDLE;
    private static Variant variant;
    private static Action action;
    private static int timeoutTicks;

    /** Feedback line shown in the export popup ({@code null} = nothing). */
    @Nullable
    private static Component status;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(BoardImageExporter::tick);
    }

    public static boolean busy() { return phase != Phase.IDLE; }
    @Nullable public static Component status() { return status; }

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
                case ModProtocol.MODE_SWAPPAGE -> "itembingo.screen.mode.swappage";
                case ModProtocol.MODE_FOG_OF_WAR -> "itembingo.screen.mode.fog_of_war";
                case ModProtocol.MODE_LOCKOUT -> "itembingo.screen.mode.lockout";
                default -> "itembingo.screen.mode.normal";
            });
            Component title = Component.translatable(BoardClientState.isTeamMode()
                    ? "itembingo.export.header.progress.team" : "itembingo.export.header.progress");
            List<Component> chips = List.of(
                    BingoBoardScreen.stageBadge(BoardClientState.gameStage()),
                    Component.literal(w + "×" + h),
                    Component.literal(BoardClientState.submittedCount() + "/" + BoardClientState.totalCells())
                            .append(" · ").append(modeName),
                    Component.literal(dateStamp()));
            export(snapshot, w, h, title, chips, tallyContributions(snapshot), v, a);
        } else {
            status = Component.translatable("itembingo.export.requesting").withStyle(ChatFormatting.GRAY);
            phase = Phase.WAIT_ORIGINAL;
            timeoutTicks = 100;
            ClientNetworking.sendOriginalRequest();
        }
    }

    /** Cells-submitted-per-teammate, most first. Empty outside team play. */
    private static List<Contribution> tallyContributions(CellState[] snapshot) {
        Map<String, Integer> counts = new HashMap<>();
        for (CellState cell : snapshot) {
            if (cell != null && cell.isSubmitted() && cell.hasSubmitter() && !cell.submitterName().isEmpty()) {
                counts.merge(cell.submitterName(), 1, Integer::sum);
            }
        }
        List<Contribution> list = new ArrayList<>();
        counts.forEach((name, count) -> list.add(new Contribution(name, count)));
        list.sort((x, y) -> y.count != x.count ? Integer.compare(y.count, x.count)
                : x.name.compareToIgnoreCase(y.name));
        return list;
    }

    /** Parses the {@code itembingo:original} response. */
    public static void handleOriginalResponse(byte[] data) {
        if (phase != Phase.WAIT_ORIGINAL) return;
        phase = Phase.IDLE;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            byte code = in.readByte();
            switch (code) {
                case ModProtocol.ORIGINAL_OK -> {
                    boolean partial = in.readBoolean();
                    int w = in.readUnsignedShort();
                    int h = in.readUnsignedShort();
                    if (w > 256 || h > 256) {
                        fail("itembingo.export.failed");
                        return;
                    }
                    CellState[] board = new CellState[w * h];
                    for (int i = 0; i < board.length; i++) {
                        // Partial (fog before game start): only the starter
                        // reveals carry items, the rest stays fogged.
                        board[i] = in.readBoolean() ? CellState.visible(in.readUTF()) : CellState.HIDDEN;
                    }
                    Component title = Component.translatable("itembingo.export.header.original");
                    List<Component> chips = new ArrayList<>();
                    chips.add(BingoBoardScreen.stageBadge(BoardClientState.gameStage()));
                    if (partial) {
                        chips.add(Component.translatable("itembingo.export.starter_only")
                                .withStyle(ChatFormatting.AQUA));
                    }
                    chips.add(Component.literal(w + "×" + h));
                    chips.add(Component.literal(dateStamp()));
                    export(board, w, h, title, chips, List.of(), variant, action);
                }
                case ModProtocol.ORIGINAL_DENIED_FOG -> fail("itembingo.export.denied_fog");
                case ModProtocol.ORIGINAL_VIEW_DENIED -> fail("itembingo.screen.view_denied");
                default -> fail("itembingo.export.no_board");
            }
        } catch (IOException e) {
            fail("itembingo.export.failed");
        }
    }

    private static void fail(String key) {
        status = Component.translatable(key).withStyle(ChatFormatting.RED);
        phase = Phase.IDLE;
    }

    /** Abandon a pending original-board request (screen closed, disconnect). */
    public static void cancel() {
        phase = Phase.IDLE;
        status = null;
    }

    private static void tick(Minecraft mc) {
        if (phase == Phase.WAIT_ORIGINAL && --timeoutTicks <= 0) {
            fail("itembingo.export.failed");
        }
    }

    /* ------------------------- offscreen render + capture ------------------------- */

    private static void export(CellState[] cells, int boardW, int boardH,
                               Component title, List<Component> chips, List<Contribution> contributions,
                               Variant v, Action a) {
        Minecraft mc = Minecraft.getInstance();
        try {
            int physicalW = mc.getWindow().getWidth();
            int physicalH = mc.getWindow().getHeight();
            int logicalW = mc.getWindow().getGuiScaledWidth();
            int logicalH = mc.getWindow().getGuiScaledHeight();
            int scale = mc.getWindow().getGuiScale();

            GuiRenderState state = new GuiRenderState();
            GuiGraphicsExtractor g = new GuiGraphicsExtractor(mc, state, logicalW, logicalH);
            ExportCardRenderer.Layout rect = ExportCardRenderer.render(
                    g, logicalW, logicalH, cells, boardW, boardH, title, chips, contributions);

            // Window-sized so the GUI projection matches; the card is cropped out.
            RenderTarget mainTarget = mc.gameRenderer.mainRenderTarget();
            TextureTarget target = new TextureTarget("ItemBingo export", physicalW, physicalH,
                    true, mainTarget.getColorTexture().getFormat());

            GameRendererAccessor accessor = (GameRendererAccessor) mc.gameRenderer;
            try (GuiRenderer renderer = new GuiRenderer(state, mc.gameRenderer.featureRenderDispatcher(), List.of())) {
                accessor.itembingo$setMainRenderTarget(target);
                renderer.render();
                renderer.endFrame();
            } finally {
                accessor.itembingo$setMainRenderTarget(mainTarget);
            }

            int sx = Math.clamp(rect.x() * scale, 0, physicalW - 1);
            int sy = Math.clamp(rect.y() * scale, 0, physicalH - 1);
            int sw = Math.min(rect.w() * scale, physicalW - sx);
            int sh = Math.min(rect.h() * scale, physicalH - sy);

            Screenshot.takeScreenshot(target, full -> {
                try (full) {
                    NativeImage cropped = new NativeImage(full.format(), sw, sh, false);
                    full.copyRect(cropped, sx, sy, 0, 0, sw, sh, false, false);
                    deliver(mc, cropped, v, a);
                } catch (Exception e) {
                    message(mc, Component.translatable("itembingo.export.failed").withStyle(ChatFormatting.RED));
                } finally {
                    target.destroyBuffers();
                }
            });
        } catch (Throwable t) {
            // An export must never crash the client — report and move on.
            message(mc, Component.translatable("itembingo.export.failed").withStyle(ChatFormatting.RED));
        }
    }

    /* ------------------------- delivery ------------------------- */

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

    /** AWT image clipboard; works on Windows/Linux. Skipped on macOS, where
     *  initializing AWT next to GLFW can deadlock rather than throw. */
    private static boolean copyToClipboard(NativeImage image) {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")) {
            return false;
        }
        try {
            System.setProperty("java.awt.headless", "false");
            int w = image.getWidth();
            int h = image.getHeight();
            // getPixelsABGR has a documented layout (A<<24 | B<<16 | G<<8 | R),
            // so the channel mapping below can't silently swap red and blue.
            int[] abgr = image.getPixelsABGR();
            int[] argb = new int[abgr.length];
            for (int i = 0; i < abgr.length; i++) {
                int p = abgr[i];
                int r = p & 0xFF;
                int gr = (p >> 8) & 0xFF;
                int b = (p >> 16) & 0xFF;
                argb[i] = 0xFF000000 | (r << 16) | (gr << 8) | b;
            }
            BufferedImage awt = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            awt.setRGB(0, 0, w, h, argb, 0, w);
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

    /** Outcome goes to chat AND the popup's status line. */
    private static void message(Minecraft mc, Component text) {
        status = text;
        if (mc.player != null) {
            mc.player.sendSystemMessage(text);
        }
    }

    private static String dateStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
