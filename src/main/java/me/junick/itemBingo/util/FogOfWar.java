package me.junick.itemBingo.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fog of War reveal logic for the bingo board.
 *
 * <p>A cell is <b>revealed</b> (shows its real item / submitted icon) when it is
 * one of the initial center reveals, OR it has been submitted, OR it is adjacent
 * to a submitted cell. Crucially {@code REVEAL != SUBMITTED}: an
 * initially-revealed center cell does not spread the fog any further until it is
 * itself submitted — only submitted cells open up their neighbours.
 *
 * <p>Initial reveals depend on the parity of the board dimensions:
 * <ul>
 *   <li>both odd → the single center cell + its 4 orthogonal neighbours</li>
 *   <li>one even → the two center cells + their 6 orthogonal neighbours</li>
 *   <li>both even → the four center cells only (no extra reveals)</li>
 * </ul>
 * (Neighbours that fall outside the board are simply skipped.)
 */
public final class FogOfWar {
    private FogOfWar() {}

    private static final int[][] ORTHOGONAL = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
    private static final int[][] DIAGONAL = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    /** The cells revealed at the very start of a Fog of War game. */
    public static Set<Integer> initialReveals(int width, int height) {
        Set<Integer> reveal = new HashSet<>();

        int[] centerCols = centerLines(width);
        int[] centerRows = centerLines(height);

        List<int[]> centers = new ArrayList<>();
        for (int cy : centerRows) {
            for (int cx : centerCols) {
                centers.add(new int[]{cx, cy});
                reveal.add(cy * width + cx);
            }
        }

        // Both-even boards reveal only the four center cells; every other parity
        // also opens the orthogonal neighbours of the center cell(s).
        boolean bothEven = (width % 2 == 0) && (height % 2 == 0);
        if (!bothEven) {
            for (int[] c : centers) {
                for (int[] d : ORTHOGONAL) {
                    addIfInBounds(reveal, c[0] + d[0], c[1] + d[1], width, height);
                }
            }
        }

        return reveal;
    }

    /**
     * Every revealed cell index given the current set of submitted slots:
     * initial reveals ∪ submitted ∪ neighbours-of-submitted.
     *
     * @param diagonal when true, submitted cells also reveal their diagonal
     *                 neighbours (8-directional) instead of only orthogonal ones
     */
    public static Set<Integer> revealedSlots(int width, int height, Set<Integer> submitted, boolean diagonal) {
        Set<Integer> revealed = new HashSet<>(initialReveals(width, height));
        revealed.addAll(submitted);

        for (int idx : submitted) {
            int x = idx % width;
            int y = idx / width;
            for (int[] d : ORTHOGONAL) {
                addIfInBounds(revealed, x + d[0], y + d[1], width, height);
            }
            if (diagonal) {
                for (int[] d : DIAGONAL) {
                    addIfInBounds(revealed, x + d[0], y + d[1], width, height);
                }
            }
        }

        return revealed;
    }

    /** The center column indices (one if odd, the two middle ones if even). */
    private static int[] centerLines(int n) {
        if (n % 2 == 1) {
            return new int[]{(n - 1) / 2};
        }
        return new int[]{n / 2 - 1, n / 2};
    }

    private static void addIfInBounds(Set<Integer> set, int x, int y, int width, int height) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            set.add(y * width + x);
        }
    }
}
