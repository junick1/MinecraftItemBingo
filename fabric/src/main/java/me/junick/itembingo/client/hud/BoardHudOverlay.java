package me.junick.itembingo.client.hud;

import me.junick.itembingo.client.config.ModConfig;
import me.junick.itembingo.client.net.ModProtocol;
import me.junick.itembingo.client.screen.Glyphs;
import me.junick.itembingo.client.state.BoardClientState;
import me.junick.itembingo.client.state.BoardClientState.ConnectionState;
import me.junick.itembingo.client.state.CellState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * In-game board overlay: a small window into the board (the region last viewed
 * in the fullscreen screen), so players can keep hunting items without opening
 * anything. Kept deliberately quiet — thin panel, small cells, and it hides
 * whenever any screen or the debug HUD is up.
 */
public final class BoardHudOverlay implements HudElement {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("itembingo", "board_overlay");

    /** Base cell edge in px before the config scale is applied. */
    private static final int BASE_CELL = 12;

    public static void register() {
        HudElementRegistry.addLast(ID, new BoardHudOverlay());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!ModConfig.overlayEnabled()) return;
        if (BoardClientState.connection() != ConnectionState.ACTIVE) return;
        if (!BoardClientState.hasBoard()) return;
        if (mc.player == null) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;

        int boardW = BoardClientState.width();
        int boardH = BoardClientState.height();
        int gridW = Math.min(boardW, ModConfig.maxGridWidth());
        int gridH = Math.min(boardH, ModConfig.maxGridHeight());
        int startCol = Math.clamp(BoardClientState.hudCol(), 0, boardW - gridW);
        int startRow = Math.clamp(BoardClientState.hudRow(), 0, boardH - gridH);

        int cell = Math.max(6, Math.round(BASE_CELL * ModConfig.scale()));
        int panelW = gridW * cell + 4;
        int panelH = gridH * cell + 4 + 11; // + progress line

        int x = switch (ModConfig.corner()) {
            case TOP_LEFT, BOTTOM_LEFT -> ModConfig.offsetX();
            case TOP_RIGHT, BOTTOM_RIGHT -> g.guiWidth() - panelW - ModConfig.offsetX();
        };
        int y = switch (ModConfig.corner()) {
            case TOP_LEFT, TOP_RIGHT -> ModConfig.offsetY();
            case BOTTOM_LEFT, BOTTOM_RIGHT -> g.guiHeight() - panelH - ModConfig.offsetY();
        };

        g.fill(x, y, x + panelW, y + panelH, 0x90101014);

        for (int r = 0; r < gridH; r++) {
            for (int c = 0; c < gridW; c++) {
                CellState state = BoardClientState.cell(startCol + c, startRow + r);
                if (state == null) continue;
                renderCell(g, state, x + 2 + c * cell, y + 2 + r * cell, cell);
            }
        }

        renderWindowMarkers(g, x, y, panelW, panelH - 11,
                startCol > 0, startCol + gridW < boardW,
                startRow > 0, startRow + gridH < boardH);

        Minecraft minecraft = Minecraft.getInstance();
        String progress = BoardClientState.submittedCount() + "/" + BoardClientState.totalCells();
        g.text(minecraft.font, progress,
                x + (panelW - minecraft.font.width(progress)) / 2, y + panelH - 10, 0xFFCCCCCC);
    }

    private void renderCell(GuiGraphicsExtractor g, CellState state, int x, int y, int cell) {
        switch (state.kind()) {
            case ModProtocol.CELL_HIDDEN ->
                    g.fillGradient(x, y, x + cell - 1, y + cell - 1, 0xF02A3247, 0xF0161B26);
            case ModProtocol.CELL_LOCKED -> g.fill(x, y, x + cell - 1, y + cell - 1, 0xC0701818);
            default -> {
                g.fill(x, y, x + cell - 1, y + cell - 1, 0x60000000);
                if (state.item() != null) {
                    float scale = (cell - 2) / 16.0f;
                    var pose = g.pose();
                    pose.pushMatrix();
                    pose.translate(x + 1, y + 1);
                    pose.scale(scale, scale);
                    g.item(new ItemStack(state.item()), 0, 0);
                    pose.popMatrix();
                }
                if (state.isSubmitted()) {
                    // Same layered treatment as the fullscreen board, miniaturized:
                    // green wash over the icon, check on top.
                    g.fill(x, y, x + cell - 1, y + cell - 1, 0x8A1E7A2E);
                    Glyphs.check(g, x + (cell - 1) / 2.0f, y + (cell - 1) / 2.0f,
                            cell * 0.62f, 0xFFEAFFEA, 0x900A2F10);
                }
            }
        }
    }

    /** Bright edge ticks show the overlay is a window into a larger board. */
    private void renderWindowMarkers(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                     boolean left, boolean right, boolean up, boolean down) {
        int color = 0xFFE0E060;
        int midX = x + w / 2;
        int midY = y + h / 2;
        if (up) g.fill(midX - 4, y, midX + 4, y + 1, color);
        if (down) g.fill(midX - 4, y + h - 1, midX + 4, y + h, color);
        if (left) g.fill(x, midY - 4, x + 1, midY + 4, color);
        if (right) g.fill(x + w - 1, midY - 4, x + w, midY + 4, color);
    }
}
