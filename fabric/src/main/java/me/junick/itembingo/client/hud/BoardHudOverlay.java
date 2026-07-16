package me.junick.itembingo.client.hud;

import me.junick.itembingo.client.config.ModConfig;
import me.junick.itembingo.client.net.ModProtocol;
import me.junick.itembingo.client.screen.BingoBoardScreen;
import me.junick.itembingo.client.screen.BoardCamera;
import me.junick.itembingo.client.screen.Glyphs;
import me.junick.itembingo.client.state.BoardClientState;
import me.junick.itembingo.client.state.BoardClientState.ConnectionState;
import me.junick.itembingo.client.state.CellState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Picture-in-picture board overlay. In plain gameplay it's a passive HUD
 * element; whenever any screen with a free cursor is open (inventory, chest,
 * chat, pause, ...) it renders on top of that screen and becomes interactive.
 *
 * <p>Unlocked: drag the panel to move it (snaps to the nearest corner and
 * persists), drag the bottom-right grip to resize. Locked (padlock button,
 * top-left on hover): the panel can't move or resize — instead dragging pans
 * the board camera, like the fullscreen view. Scroll / shift+scroll pans and
 * ctrl+scroll zooms in both modes. The camera is shared with the fullscreen
 * board, so both stay in sync. The only thing the overlay can't do is submit.
 */
public final class BoardHudOverlay implements HudElement {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("itembingo", "board_overlay");

    private static final BoardCamera CAMERA = new BoardCamera();

    private static final int GRIP = 8;
    private static final int LOCK_SIZE = 10;

    /** Panel geometry in screen px, refreshed every rendered frame. */
    private static int panelX;
    private static int panelY;
    private static int panelW;
    private static int panelH;
    private static int guiW;
    private static int guiH;
    private static boolean hoveredNow;

    private static boolean dragging;
    private static boolean resizing;
    private static boolean panningBoard;
    private static double grabDx;
    private static double grabDy;
    private static int dragX;
    private static int dragY;
    /** Live inner size (camera units) while resizing. */
    private static int resizeW;
    private static int resizeH;

    public static void register() {
        HudElementRegistry.addLast(ID, new BoardHudOverlay());

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof BingoBoardScreen) return; // fullscreen board has its own camera view

            ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> {
                if (!visible()) return;
                g.nextStratum();
                render(g, mouseX, mouseY);
            });
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !handleClick(event));
            ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dx, dy) -> !handleDrag(event, dx, dy));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> !handleRelease(event));
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, sx, sy) ->
                    !handleScroll(mouseX, mouseY, sx, sy));
        });
    }

    /** Passive HUD path — only when no screen is open (screens draw it themselves). */
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null) return;
        dragging = resizing = panningBoard = false; // no cursor: interactions are over
        if (!visible()) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;
        render(g, -1, -1);
    }

    private static boolean visible() {
        Minecraft mc = Minecraft.getInstance();
        return ModConfig.overlayEnabled()
                && BoardClientState.connection() == ConnectionState.ACTIVE
                && BoardClientState.hasBoard()
                && mc.player != null;
    }

    /* ------------------------- geometry ------------------------- */

    private static float scale() {
        return ModConfig.scale();
    }

    /** Board viewport inside the panel, in camera units (pre-scale px). */
    private static int innerW() {
        return resizing ? resizeW : ModConfig.overlayWidth();
    }

    private static int innerH() {
        return resizing ? resizeH : ModConfig.overlayHeight();
    }

    private static void layout(GuiGraphicsExtractor g) {
        float s = scale();
        guiW = g.guiWidth();
        guiH = g.guiHeight();
        panelW = Math.round((innerW() + 4) * s);
        panelH = Math.round((innerH() + 4) * s) + 11; // + progress line (unscaled)

        if (dragging) {
            panelX = dragX;
            panelY = dragY;
        } else {
            panelX = switch (ModConfig.corner()) {
                case TOP_LEFT, BOTTOM_LEFT -> ModConfig.offsetX();
                case TOP_RIGHT, BOTTOM_RIGHT -> guiW - panelW - ModConfig.offsetX();
            };
            panelY = switch (ModConfig.corner()) {
                case TOP_LEFT, TOP_RIGHT -> ModConfig.offsetY();
                case BOTTOM_LEFT, BOTTOM_RIGHT -> guiH - panelH - ModConfig.offsetY();
            };
        }
        panelX = Math.clamp(panelX, 0, Math.max(0, guiW - panelW));
        panelY = Math.clamp(panelY, 0, Math.max(0, guiH - panelH));
    }

    private static boolean inPanel(double mx, double my) {
        return mx >= panelX && mx < panelX + panelW && my >= panelY && my < panelY + panelH;
    }

    private static boolean inLockButton(double mx, double my) {
        return mx >= panelX + 2 && mx < panelX + 2 + LOCK_SIZE
                && my >= panelY + 2 && my < panelY + 2 + LOCK_SIZE;
    }

    private static boolean inResizeGrip(double mx, double my) {
        return mx >= panelX + panelW - GRIP && mx < panelX + panelW
                && my >= panelY + panelH - GRIP && my < panelY + panelH;
    }

    /* ------------------------- rendering ------------------------- */

    private static void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        layout(g);
        hoveredNow = mouseX >= 0 && inPanel(mouseX, mouseY);

        float s = scale();
        int ix = panelX + Math.round(2 * s);
        int iy = panelY + Math.round(2 * s);
        int iw = Math.round(innerW() * s);
        int ih = Math.round(innerH() * s);

        CAMERA.sync(BoardClientState.width(), BoardClientState.height(), innerW(), innerH());

        boolean locked = ModConfig.overlayLocked();
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x90101014);
        if (hoveredNow || dragging || resizing || panningBoard) {
            g.outline(panelX, panelY, panelW, panelH,
                    dragging || resizing ? 0xFFE8C84A : locked ? 0xFF7A93C4 : 0xFF6E7A92);
        }

        g.enableScissor(ix, iy, ix + iw, iy + ih);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(ix, iy);
        pose.scale(s, s);

        long now = System.currentTimeMillis();
        int w = BoardClientState.width();
        for (int row = CAMERA.firstVisibleRow(); row <= CAMERA.lastVisibleRow(); row++) {
            int y = (int) Math.round(CAMERA.cellScreenY(row, 0));
            int y2 = (int) Math.round(CAMERA.cellScreenY(row + 1, 0));
            for (int col = CAMERA.firstVisibleCol(); col <= CAMERA.lastVisibleCol(); col++) {
                int x = (int) Math.round(CAMERA.cellScreenX(col, 0));
                int x2 = (int) Math.round(CAMERA.cellScreenX(col + 1, 0));
                CellState cell = BoardClientState.cell(col, row);
                if (cell == null) continue;
                renderCell(g, cell, x, y, x2 - x, y2 - y,
                        BoardClientState.flashAlpha(row * w + col, now));
            }
        }

        pose.popMatrix();
        g.disableScissor();

        renderWindowMarkers(g, ix, iy, iw, ih);

        // Controls only appear while the cursor is over the panel.
        if (hoveredNow || dragging || resizing) {
            renderLockButton(g, locked, mouseX, mouseY);
            if (!locked) {
                renderResizeGrip(g);
            }
        }

        Minecraft mc = Minecraft.getInstance();
        String progress = BoardClientState.submittedCount() + "/" + BoardClientState.totalCells();
        g.text(mc.font, progress, panelX + (panelW - mc.font.width(progress)) / 2,
                panelY + panelH - 10, 0xFFCCCCCC);
    }

    private static void renderCell(GuiGraphicsExtractor g, CellState cell, int x, int y,
                                   int cw, int ch, float flash) {
        switch (cell.kind()) {
            case ModProtocol.CELL_HIDDEN ->
                    g.fillGradient(x + 1, y + 1, x + cw - 1, y + ch - 1, 0xF02A3247, 0xF0161B26);
            case ModProtocol.CELL_LOCKED ->
                    g.fill(x + 1, y + 1, x + cw - 1, y + ch - 1, 0xC0701818);
            default -> {
                g.fill(x + 1, y + 1, x + cw - 1, y + ch - 1, 0x60000000);
                int size = Math.min(cw, ch);
                if (cell.item() != null) {
                    float iconScale = (size - 4) / 16.0f;
                    var pose = g.pose();
                    pose.pushMatrix();
                    pose.translate(x + (cw - size) / 2f + 2, y + (ch - size) / 2f + 2);
                    pose.scale(iconScale, iconScale);
                    g.item(new ItemStack(cell.item()), 0, 0);
                    pose.popMatrix();
                }
                if (cell.isSubmitted()) {
                    g.fill(x + 1, y + 1, x + cw - 1, y + ch - 1, 0x8A1E7A2E);
                    Glyphs.check(g, x + cw / 2.0f, y + ch / 2.0f, size * 0.62f,
                            0xFFEAFFEA, 0x900A2F10);
                }
            }
        }
        if (flash > 0) {
            int alpha = (int) (flash * 0xA0) << 24;
            g.fill(x + 1, y + 1, x + cw - 1, y + ch - 1, alpha | 0xFFFFFF);
        }
        g.outline(x, y, cw, ch, 0xFF32323C);
    }

    /** Tiny quad-drawn padlock: closed+gold when locked, open+gray otherwise. */
    private static void renderLockButton(GuiGraphicsExtractor g, boolean locked, int mouseX, int mouseY) {
        int x = panelX + 2;
        int y = panelY + 2;
        boolean hover = inLockButton(mouseX, mouseY);
        g.fill(x, y, x + LOCK_SIZE, y + LOCK_SIZE, hover ? 0xE0303644 : 0xC0202430);
        g.outline(x, y, LOCK_SIZE, LOCK_SIZE, hover ? 0xFFAAB6CC : 0xFF4A5266);

        int color = locked ? 0xFFE8C84A : 0xFF9AA3B4;
        // Body.
        g.fill(x + 2, y + 5, x + 8, y + 9, color);
        if (locked) {
            // Closed shackle: both legs into the body.
            g.fill(x + 3, y + 2, x + 4, y + 5, color);
            g.fill(x + 6, y + 2, x + 7, y + 5, color);
            g.fill(x + 3, y + 2, x + 7, y + 3, color);
        } else {
            // Open shackle: swung to the right, one leg loose.
            g.fill(x + 6, y + 1, x + 7, y + 5, color);
            g.fill(x + 6, y + 1, x + 9, y + 2, color);
            g.fill(x + 8, y + 1, x + 9, y + 4, color);
        }
    }

    /** Diagonal ticks in the bottom-right corner while resizable. */
    private static void renderResizeGrip(GuiGraphicsExtractor g) {
        int x = panelX + panelW;
        int y = panelY + panelH;
        int color = 0xFFAAB6CC;
        for (int i = 2; i <= 6; i += 2) {
            g.fill(x - i - 1, y - 2, x - i, y - 1, color);
            g.fill(x - 2, y - i - 1, x - 1, y - i, color);
        }
    }

    /** Bright edge ticks show the panel is a window into a larger board. */
    private static void renderWindowMarkers(GuiGraphicsExtractor g, int ix, int iy, int iw, int ih) {
        int color = 0xFFE0E060;
        int midX = ix + iw / 2;
        int midY = iy + ih / 2;
        if (CAMERA.firstVisibleRow() > 0) g.fill(midX - 4, iy, midX + 4, iy + 1, color);
        if (CAMERA.lastVisibleRow() < BoardClientState.height() - 1) g.fill(midX - 4, iy + ih - 1, midX + 4, iy + ih, color);
        if (CAMERA.firstVisibleCol() > 0) g.fill(ix, midY - 4, ix + 1, midY + 4, color);
        if (CAMERA.lastVisibleCol() < BoardClientState.width() - 1) g.fill(ix + iw - 1, midY - 4, ix + iw, midY + 4, color);
    }

    /* ------------------------- interaction (screens only) ------------------------- */

    /** @return true when the event was consumed by the overlay. */
    private static boolean handleClick(MouseButtonEvent event) {
        if (!visible() || !inPanel(event.x(), event.y())) return false;
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            boolean locked = ModConfig.overlayLocked();
            if (inLockButton(event.x(), event.y())) {
                ModConfig.toggleOverlayLocked();
            } else if (!locked && inResizeGrip(event.x(), event.y())) {
                resizing = true;
                resizeW = ModConfig.overlayWidth();
                resizeH = ModConfig.overlayHeight();
            } else if (locked) {
                panningBoard = true;
            } else {
                dragging = true;
                grabDx = event.x() - panelX;
                grabDy = event.y() - panelY;
                dragX = panelX;
                dragY = panelY;
            }
        }
        return true; // swallow clicks over the panel either way
    }

    private static boolean handleDrag(MouseButtonEvent event, double dx, double dy) {
        float s = scale();
        if (dragging) {
            dragX = Math.clamp((int) (event.x() - grabDx), 0, Math.max(0, guiW - panelW));
            dragY = Math.clamp((int) (event.y() - grabDy), 0, Math.max(0, guiH - panelH));
            return true;
        }
        if (resizing) {
            resizeW = Math.clamp((int) ((event.x() - panelX) / s) - 4, 66, 1320);
            resizeH = Math.clamp((int) ((event.y() - panelY - 11) / s) - 4, 66, 1320);
            return true;
        }
        if (panningBoard) {
            CAMERA.pan(dx / s, dy / s);
            return true;
        }
        return false;
    }

    private static boolean handleRelease(MouseButtonEvent event) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (panningBoard) {
            panningBoard = false;
            return true;
        }
        if (resizing) {
            resizing = false;
            ModConfig.setOverlaySize(resizeW, resizeH);
            return true;
        }
        if (dragging) {
            dragging = false;
            // Snap to the nearest corner so the position survives window resizes.
            boolean left = dragX + panelW / 2 < guiW / 2;
            boolean top = dragY + panelH / 2 < guiH / 2;
            ModConfig.Corner corner = left
                    ? (top ? ModConfig.Corner.TOP_LEFT : ModConfig.Corner.BOTTOM_LEFT)
                    : (top ? ModConfig.Corner.TOP_RIGHT : ModConfig.Corner.BOTTOM_RIGHT);
            int offsetX = left ? dragX : guiW - (dragX + panelW);
            int offsetY = top ? dragY : guiH - (dragY + panelH);
            ModConfig.setPosition(corner, offsetX, offsetY);
            return true;
        }
        return false;
    }

    private static boolean handleScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!visible() || !inPanel(mouseX, mouseY)) return false;
        Minecraft mc = Minecraft.getInstance();
        float s = scale();
        double viewX = (mouseX - (panelX + 2 * s)) / s;
        double viewY = (mouseY - (panelY + 2 * s)) / s;
        if (mc.hasControlDown()) {
            CAMERA.zoomAt(viewX, viewY, scrollY);
        } else if (mc.hasShiftDown()) {
            CAMERA.pan((scrollY + scrollX) * BoardCamera.CELL * CAMERA.zoom() / 2.0, 0);
        } else {
            CAMERA.pan(scrollX * BoardCamera.CELL * CAMERA.zoom() / 2.0,
                    scrollY * BoardCamera.CELL * CAMERA.zoom() / 2.0);
        }
        return true;
    }
}
