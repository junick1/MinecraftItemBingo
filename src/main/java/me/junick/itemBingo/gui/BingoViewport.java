package me.junick.itemBingo.gui;

import me.junick.itemBingo.model.BingoBoard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player scroll state and slot&harr;cell mapping for the bingo board's
 * <em>scrollable viewport</em>, used only for "oversized" boards that don't fit
 * the 9&times;6 inventory limit (see {@link #needsScroll}). Boards that fit are
 * rendered by {@link BingoGUI}'s centered path and never touch this class.
 *
 * <p>The viewport is <b>adaptive</b> — it sizes itself to the board's shape so a
 * long/thin board shows as much as possible rather than a fixed tiny window. The
 * scroll controls live wherever they leave the most room for cells:</p>
 * <ul>
 *   <li><b>Both axes scroll</b> (width&gt;9 and height&gt;6): arrows centered on
 *       all four edges, a 7&times;4 inset cell window.</li>
 *   <li><b>Horizontal only</b>: if the board is short (height&le;5) a control
 *       <em>row</em> sits below a full 9-wide cell area; if it's exactly 6 tall a
 *       control <em>column</em> sits to the right of an 8-wide area.</li>
 *   <li><b>Vertical only</b>: if the board is narrow (width&le;8) a control
 *       <em>column</em> sits right of a 6-tall area; if it's exactly 9 wide a
 *       control <em>row</em> sits below a 5-tall area.</li>
 * </ul>
 * <p>Single-axis boards therefore never need a second scroll axis. Every layout
 * also carries a "recenter" button that jumps back to the board's middle.</p>
 *
 * <p>A player's scroll offset {@code (startRow, startCol)} is the board cell
 * shown at the cell window's top-left; it's clamped against the live board on
 * every read so a stale value can't push cells off-grid.</p>
 */
public final class BingoViewport {
    private BingoViewport() {}

    public static final int GUI_WIDTH = 9;
    public static final int GUI_HEIGHT = 6;

    /** What a clicked control slot does. */
    public enum Control { NONE, UP, DOWN, LEFT, RIGHT, RECENTER }

    /** {startRow, startCol} per player; absent = centered on first open. */
    private static final Map<UUID, int[]> OFFSETS = new HashMap<>();

    /**
     * A fully-resolved view for one player + board: the cell window's placement
     * and size, the clamped scroll offset, centering pads, and the GUI slot of
     * each control ({@code -1} when a layout has no such control) plus whether
     * that direction can still scroll.
     */
    public record Layout(
            int guiRows,
            int originRow, int originCol,
            int viewCols, int viewRows,
            int startRow, int startCol,
            int padX, int padY,
            int upSlot, int downSlot, int leftSlot, int rightSlot, int recenterSlot,
            boolean upActive, boolean downActive, boolean leftActive, boolean rightActive
    ) {}

    /** A board needs the scrollable viewport iff it can't fit the 9&times;6 GUI. */
    public static boolean needsScroll(BingoBoard board) {
        return board.getWidth() > GUI_WIDTH || board.getHeight() > GUI_HEIGHT;
    }

    /* ===================== Scroll-position storage ===================== */

    public static boolean has(UUID id) {
        return OFFSETS.containsKey(id);
    }

    /** Forgets every player's position so the next open re-centers on the new board. */
    public static void clearAll() {
        OFFSETS.clear();
    }

    private static int rawRow(UUID id) {
        int[] o = OFFSETS.get(id);
        return o == null ? 0 : o[0];
    }

    private static int rawCol(UUID id) {
        int[] o = OFFSETS.get(id);
        return o == null ? 0 : o[1];
    }

    /** Stores a raw offset; {@link #layout} clamps it on read, so out-of-range is fine. */
    private static void set(UUID id, int sr, int sc) {
        OFFSETS.put(id, new int[]{sr, sc});
    }

    /* ===================== Layout resolution ===================== */

    public static Layout layout(UUID id, BingoBoard b) {
        int w = b.getWidth(), h = b.getHeight();
        boolean needsH = w > GUI_WIDTH;
        boolean needsV = h > GUI_HEIGHT;

        int guiRows, originRow = 0, originCol = 0, viewCols, viewRows;
        int up = -1, down = -1, left = -1, right = -1, recenter = -1;

        if (needsH && needsV) {
            // Both axes: arrows centered on all four edges, 7x4 inset window.
            guiRows = 6;
            originRow = 1;
            originCol = 1;
            viewCols = 7;
            viewRows = 4;
            up = 4;
            down = 5 * 9 + 4;
            left = 2 * 9;
            right = 2 * 9 + 8;
            recenter = 0;
        } else if (needsH) {
            if (h <= GUI_HEIGHT - 1) {
                // Short + wide: control row below a full-width cell area.
                viewCols = 9;
                viewRows = h;
                guiRows = h + 1;
                int base = h * 9;
                left = base;
                right = base + 8;
                recenter = base + 4;
            } else {
                // Exactly 6 tall + wide: control column right of an 8-wide area.
                viewCols = 8;
                viewRows = 6;
                guiRows = 6;
                left = 9 + 8;
                right = 4 * 9 + 8;
                recenter = 2 * 9 + 8;
            }
        } else { // needsV only
            if (w <= GUI_WIDTH - 1) {
                // Narrow + tall: control column right of a full-height cell area.
                // The 8-wide cell area centers the (narrow) board via padX.
                viewCols = 8;
                viewRows = 6;
                guiRows = 6;
                up = 9 + 8;
                down = 4 * 9 + 8;
                recenter = 2 * 9 + 8;
            } else {
                // Exactly 9 wide + tall: control row below a 5-tall area.
                viewCols = 9;
                viewRows = 5;
                guiRows = 6;
                int base = 5 * 9;
                up = base + 3;
                down = base + 5;
                recenter = base + 4;
            }
        }

        int maxSR = Math.max(0, h - viewRows);
        int maxSC = Math.max(0, w - viewCols);
        int sr = Math.max(0, Math.min(rawRow(id), maxSR));
        int sc = Math.max(0, Math.min(rawCol(id), maxSC));
        int padX = w < viewCols ? (viewCols - w) / 2 : 0;
        int padY = h < viewRows ? (viewRows - h) / 2 : 0;

        return new Layout(guiRows, originRow, originCol, viewCols, viewRows,
                sr, sc, padX, padY,
                up, down, left, right, recenter,
                sr > 0, sr < maxSR, sc > 0, sc < maxSC);
    }

    /** What control, if any, sits at {@code slot} in this layout. */
    public static Control controlAt(Layout l, int slot) {
        if (slot < 0) return Control.NONE;
        if (slot == l.recenterSlot()) return Control.RECENTER;
        if (slot == l.upSlot()) return Control.UP;
        if (slot == l.downSlot()) return Control.DOWN;
        if (slot == l.leftSlot()) return Control.LEFT;
        if (slot == l.rightSlot()) return Control.RIGHT;
        return Control.NONE;
    }

    /* ===================== Mapping ===================== */

    /** GUI slot showing board cell {@code index}, or {@code -1} if it's outside the window. */
    public static int indexToSlot(int index, BingoBoard b, Layout l) {
        int w = b.getWidth();
        int vr = index / w - l.startRow() + l.padY();
        int vc = index % w - l.startCol() + l.padX();
        if (vr < 0 || vr >= l.viewRows() || vc < 0 || vc >= l.viewCols()) return -1;
        return (l.originRow() + vr) * GUI_WIDTH + (l.originCol() + vc);
    }

    /** Board cell index shown at GUI {@code slot}, or {@code -1} if it isn't a cell. */
    public static int slotToIndex(int slot, BingoBoard b, Layout l) {
        int w = b.getWidth(), h = b.getHeight();
        int vr = slot / GUI_WIDTH - l.originRow();
        int vc = slot % GUI_WIDTH - l.originCol();
        if (vr < 0 || vr >= l.viewRows() || vc < 0 || vc >= l.viewCols()) return -1;
        int br = l.startRow() + vr - l.padY();
        int bc = l.startCol() + vc - l.padX();
        if (br < 0 || br >= h || bc < 0 || bc >= w) return -1;
        return br * w + bc;
    }

    /* ===================== Movement ===================== */

    /** Nudges the viewport by {@code (dRow, dCol)} cells (clamped on next read). */
    public static void scroll(UUID id, BingoBoard b, int dRow, int dCol) {
        Layout l = layout(id, b);
        set(id, l.startRow() + dRow, l.startCol() + dCol);
    }

    /**
     * Scrolls so board cell {@code index} sits as near the window center as the
     * edges allow — the clamp on read naturally pushes it toward an edge when true
     * centering would scroll past the board.
     */
    public static void focusOn(UUID id, BingoBoard b, int index) {
        Layout l = layout(id, b);
        int w = b.getWidth();
        int br = index / w, bc = index % w;
        set(id, br - (l.viewRows() - 1) / 2, bc - (l.viewCols() - 1) / 2);
    }

    /** Centers the viewport over the middle of the board — the default first-open / recenter position. */
    public static void centerOn(UUID id, BingoBoard b) {
        focusOn(id, b, (b.getHeight() / 2) * b.getWidth() + b.getWidth() / 2);
    }
}
