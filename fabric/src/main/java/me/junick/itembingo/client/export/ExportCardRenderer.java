package me.junick.itembingo.client.export;

import me.junick.itembingo.client.net.ModProtocol;
import me.junick.itembingo.client.screen.Glyphs;
import me.junick.itembingo.client.state.CellState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Draws the export "results card" into an extractor (normally an offscreen
 * one). Layout is overlap-free by construction: the title has its own row,
 * chips flow left-to-right and wrap to further rows when they don't fit, the
 * grid uses fixed integer cell sizes, and the contribution rows measure their
 * name column before placing the bars.
 */
final class ExportCardRenderer {
    private ExportCardRenderer() {}

    /** Crop rectangle of the rendered card, in logical (gui-scaled) px. */
    record Layout(int x, int y, int w, int h) {}

    private static final int PAD = 8;
    private static final int TITLE_ROW_H = 20;
    private static final int CHIP_ROW_H = 17;
    private static final float TITLE_SCALE = 1.4f;

    static Layout render(GuiGraphicsExtractor g, int logicalW, int logicalH,
                         CellState[] cells, int boardW, int boardH,
                         Component title, List<Component> chips,
                         List<BoardImageExporter.Contribution> contributions) {
        Font font = Minecraft.getInstance().font;

        int shownRows = Math.min(contributions.size(), 8);
        boolean truncated = contributions.size() > shownRows;
        int contribH = shownRows > 0 ? 18 + shownRows * 22 + (truncated ? 12 : 0) : 0;

        // The panel can never be narrower than its widest single element.
        int minPanelW = shownRows > 0 ? 230 : 170;
        minPanelW = Math.max(minPanelW, Math.round(font.width(title) * TITLE_SCALE) + 4);
        for (Component chip : chips) {
            minPanelW = Math.max(minPanelW, chipWidth(font, chip));
        }

        // Cell size and header height depend on each other through chip
        // wrapping; two refinement passes always reach a fixed point because
        // panelW only ever grows.
        int headerH = TITLE_ROW_H + CHIP_ROW_H + 3;
        int cell = 6;
        int panelW = minPanelW;
        for (int pass = 0; pass < 2; pass++) {
            cell = Math.clamp(Math.min((logicalW - 2 * PAD - 16) / boardW,
                    (logicalH - 2 * PAD - 16 - headerH - contribH) / boardH), 6, 48);
            panelW = Math.max(cell * boardW, minPanelW);
            headerH = TITLE_ROW_H + chipRows(font, chips, panelW) * CHIP_ROW_H + 3;
        }
        int gridW = cell * boardW;
        int totalH = headerH + cell * boardH + contribH;
        int x0 = (logicalW - panelW) / 2;
        int y0 = (logicalH - totalH) / 2;

        g.fill(x0 - PAD, y0 - PAD, x0 + panelW + PAD, y0 + totalH + PAD, 0xFF14161C);
        g.outline(x0 - PAD, y0 - PAD, panelW + 2 * PAD, totalH + 2 * PAD, 0xFF2A303E);

        // Row 1: the title, alone, at 1.4x.
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x0, y0 + 3);
        pose.scale(TITLE_SCALE, TITLE_SCALE);
        g.text(font, title, 0, 0, 0xFFFFFFFF);
        pose.popMatrix();

        // Row 2+: chips flowing left to right, wrapping when out of width.
        int cx = x0;
        int cy = y0 + TITLE_ROW_H;
        for (Component chip : chips) {
            int cw = chipWidth(font, chip);
            if (cx > x0 && cx + cw > x0 + panelW) {
                cx = x0;
                cy += CHIP_ROW_H;
            }
            g.fill(cx, cy, cx + cw, cy + 14, 0xFF1E2430);
            g.outline(cx, cy, cw, 14, 0xFF394152);
            g.text(font, chip, cx + 4, cy + 3, 0xFFB8C4DA);
            cx += cw + 4;
        }

        // Grid, centered when the panel is wider than the board.
        int gx = x0 + (panelW - gridW) / 2;
        int gy = y0 + headerH;
        for (int row = 0; row < boardH; row++) {
            for (int col = 0; col < boardW; col++) {
                drawCell(g, font, cells[row * boardW + col], gx + col * cell, gy + row * cell, cell);
            }
        }

        if (shownRows > 0) {
            renderContributions(g, font, contributions, shownRows, truncated,
                    x0, gy + cell * boardH + 4, panelW);
        }

        return new Layout(x0 - PAD, y0 - PAD, panelW + 2 * PAD, totalH + 2 * PAD);
    }

    private static int chipWidth(Font font, Component chip) {
        return font.width(chip) + 8;
    }

    private static int chipRows(Font font, List<Component> chips, int panelW) {
        if (chips.isEmpty()) return 0;
        int rows = 1;
        int cx = 0;
        for (Component chip : chips) {
            int cw = chipWidth(font, chip);
            if (cx > 0 && cx + cw > panelW) {
                rows++;
                cx = 0;
            }
            cx += cw + 4;
        }
        return rows;
    }

    private static void drawCell(GuiGraphicsExtractor g, Font font, CellState cell, int x, int y, int size) {
        if (cell == null) return;
        if (cell.kind() == ModProtocol.CELL_HIDDEN) {
            g.fillGradient(x + 1, y + 1, x + size - 1, y + size - 1, 0xF02A3247, 0xF0161B26);
        } else {
            g.fill(x + 1, y + 1, x + size - 1, y + size - 1,
                    cell.kind() == ModProtocol.CELL_LOCKED ? 0x80581414 : 0x60000000);
        }
        switch (cell.kind()) {
            case ModProtocol.CELL_LOCKED -> drawScaledItem(g, CellState.BARRIER_STACK, x, y, size);
            case ModProtocol.CELL_VISIBLE, ModProtocol.CELL_SUBMITTED -> {
                if (cell.stack() != null) {
                    drawScaledItem(g, cell.stack(), x, y, size);
                } else {
                    g.centeredText(font, "?", x + size / 2, y + (size - 9) / 2, 0xFFFFCC44);
                }
                if (cell.isSubmitted()) {
                    g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x8A1E7A2E);
                    Glyphs.check(g, x + size / 2.0f, y + size / 2.0f, size * 0.62f,
                            0xFFEAFFEA, 0x900A2F10);
                }
            }
            default -> {}
        }
        g.outline(x, y, size, size, 0xFF3C3C46);
    }

    private static void drawScaledItem(GuiGraphicsExtractor g, net.minecraft.world.item.ItemStack stack,
                                       int x, int y, int size) {
        float iconScale = (size - 4) / 16.0f;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x + 2, y + 2);
        pose.scale(iconScale, iconScale);
        g.item(stack, 0, 0);
        pose.popMatrix();
    }

    /** Ranked teammate list: avatar, name, bar, cell count; gold #1 row. */
    private static void renderContributions(GuiGraphicsExtractor g, Font font,
                                            List<BoardImageExporter.Contribution> contribs,
                                            int shownRows, boolean truncated,
                                            int x0, int y, int panelW) {
        g.text(font, Component.translatable("itembingo.export.contribution"), x0, y + 3, 0xFFDDE2EE);

        int maxCount = Math.max(1, contribs.getFirst().count());
        int nameColW = 40;
        for (int i = 0; i < shownRows; i++) {
            nameColW = Math.max(nameColW, Math.min(104, font.width(contribs.get(i).name()) + 6));
        }
        int rowsTop = y + 15;
        for (int i = 0; i < shownRows; i++) {
            BoardImageExporter.Contribution entry = contribs.get(i);
            int ry = rowsTop + i * 22;
            boolean mvp = i == 0;

            g.fill(x0, ry, x0 + panelW, ry + 20, mvp ? 0x30E8C84A : 0x14FFFFFF);
            if (mvp) {
                g.outline(x0, ry, panelW, 20, 0xFFE8C84A);
            }

            g.text(font, String.valueOf(i + 1), x0 + 6, ry + 6, mvp ? 0xFFE8C84A : 0xFF8A93A6);
            drawPlayerFace(g, font, entry.name(), x0 + 18, ry + 1, 18);

            int nameX = x0 + 42;
            g.text(font, entry.name(), nameX, ry + 6, mvp ? 0xFFF6E6A8 : 0xFFE8ECF4);

            String count = String.valueOf(entry.count());
            int countW = font.width(count);
            g.text(font, count, x0 + panelW - countW - 6, ry + 6, 0xFFFFFFFF);

            int barX = nameX + nameColW;
            int barW = x0 + panelW - countW - 14 - barX;
            if (barW > 24) {
                int barY = ry + 8;
                g.fill(barX, barY, barX + barW, barY + 4, 0xFF262B36);
                int fill = Math.max(2, (int) ((long) barW * entry.count() / maxCount));
                g.fill(barX, barY, barX + fill, barY + 4, mvp ? 0xFFE8C84A : 0xFF4C7DD8);
            }
        }

        if (truncated) {
            g.text(font, Component.translatable("itembingo.export.more", contribs.size() - shownRows),
                    x0 + 4, rowsTop + shownRows * 22 + 2, 0xFF8A93A6);
        }
    }

    /** Skin face for online teammates; tinted initial tile for offline ones. */
    private static void drawPlayerFace(GuiGraphicsExtractor g, Font font, String name, int x, int y, int size) {
        var connection = Minecraft.getInstance().getConnection();
        var info = connection != null ? connection.getPlayerInfo(name) : null;
        if (info != null) {
            PlayerFaceExtractor.extractRenderState(g, info.getSkin(), x, y, size);
            return;
        }
        int[] palette = {0xFF534AB7, 0xFF0F6E56, 0xFF993C1D, 0xFF993556, 0xFF185FA5, 0xFF854F0B};
        g.fill(x, y, x + size, y + size, palette[Math.floorMod(name.hashCode(), palette.length)]);
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        g.centeredText(font, initial, x + size / 2, y + (size - 9) / 2 + 1, 0xFFFFFFFF);
    }
}
