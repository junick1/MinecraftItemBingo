package me.junick.itembingo.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Cell glyphs drawn from plain quads so they stay crisp at any zoom — no font
 * glyphs, no textures. Used by both the fullscreen board and the HUD overlay.
 */
public final class Glyphs {
    private Glyphs() {}

    /**
     * A ✓ built from two rotated quads (an "L" turned -45°).
     *
     * @param cx,cy center of the checkmark
     * @param size  overall edge length in screen px
     */
    public static void check(GuiGraphicsExtractor g, float cx, float cy, float size, int color, int shadowColor) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.rotate((float) Math.toRadians(-45));
        // Design space: 16-unit "L" (vertical arm 10 tall, horizontal 16 long,
        // 4 thick), nudged so its visual center sits on the origin.
        pose.scale(size / 16.0f, size / 16.0f);
        pose.translate(0.5f, -1.5f);
        if ((shadowColor >>> 24) != 0) {
            fillL(g, 1, 1, shadowColor);
        }
        fillL(g, 0, 0, color);
        pose.popMatrix();
    }

    private static void fillL(GuiGraphicsExtractor g, int dx, int dy, int color) {
        g.fill(-8 + dx, -5 + dy, -4 + dx, 5 + dy, color); // vertical arm (short stroke)
        g.fill(-8 + dx, 1 + dy, 8 + dx, 5 + dy, color);   // horizontal arm (long stroke)
    }
}
