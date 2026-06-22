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
 * <p>Geometry: the GUI is the full 9&times;6. A {@link #VIEW_COLS}&times;{@link #VIEW_ROWS}
 * (7&times;4) window of board cells sits inset by one slot on every side
 * (rows&nbsp;1..4, cols&nbsp;1..7); the surrounding border holds the four scroll
 * arrows — one centered on each edge — and filler panes. A player's scroll
 * offset {@code (startRow, startCol)} is the board cell shown at the viewport's
 * top-left corner.</p>
 *
 * <p>When a board dimension is smaller than the viewport (e.g. a 25&times;3 board
 * is only 3 rows tall) that axis can't scroll, so it's centered within the
 * viewport via {@code padX}/{@code padY} and its arrows stay hidden.</p>
 */
public final class BingoViewport {
    private BingoViewport() {}

    public static final int GUI_WIDTH = 9;
    public static final int GUI_HEIGHT = 6;
    public static final int VIEW_COLS = 7;
    public static final int VIEW_ROWS = 4;

    /** Border arrow slots, centered on each edge of the 9&times;6 GUI. */
    public static final int SLOT_UP    = 4;                  // row 0, col 4
    public static final int SLOT_DOWN  = 5 * GUI_WIDTH + 4;  // row 5, col 4 = 49
    public static final int SLOT_LEFT  = 2 * GUI_WIDTH;      // row 2, col 0 = 18
    public static final int SLOT_RIGHT = 2 * GUI_WIDTH + 8;  // row 2, col 8 = 26

    /** {startRow, startCol} per player; absent = top-left {0, 0}. */
    private static final Map<UUID, int[]> OFFSETS = new HashMap<>();

    /** Immutable snapshot of one player's clamped scroll position and which arrows are live. */
    public record Layout(int startRow, int startCol, int padX, int padY,
                         boolean up, boolean down, boolean left, boolean right) {}

    /** A board needs the scrollable viewport iff it can't fit the 9&times;6 GUI. */
    public static boolean needsScroll(BingoBoard board) {
        return board.getWidth() > GUI_WIDTH || board.getHeight() > GUI_HEIGHT;
    }

    public static boolean isArrowSlot(int slot) {
        return slot == SLOT_UP || slot == SLOT_DOWN || slot == SLOT_LEFT || slot == SLOT_RIGHT;
    }

    /** Whether {@code id} has a remembered scroll position (false → center on next open). */
    public static boolean has(UUID id) {
        return OFFSETS.containsKey(id);
    }

    /**
     * Forgets every player's scroll position. Called when a new board is applied so
     * the next open re-centers on the new board instead of restoring a stale offset.
     */
    public static void clearAll() {
        OFFSETS.clear();
    }

    /** Centers the viewport over the middle of the board — the default first-open position. */
    public static void centerOn(UUID id, BingoBoard b) {
        set(id, b, b.getHeight() / 2 - VIEW_ROWS / 2, b.getWidth() / 2 - VIEW_COLS / 2);
    }

    private static int rawStartRow(UUID id) {
        int[] o = OFFSETS.get(id);
        return o == null ? 0 : o[0];
    }

    private static int rawStartCol(UUID id) {
        int[] o = OFFSETS.get(id);
        return o == null ? 0 : o[1];
    }

    private static int maxStartRow(BingoBoard b) { return Math.max(0, b.getHeight() - VIEW_ROWS); }
    private static int maxStartCol(BingoBoard b) { return Math.max(0, b.getWidth()  - VIEW_COLS); }

    private static int padY(BingoBoard b) { return b.getHeight() < VIEW_ROWS ? (VIEW_ROWS - b.getHeight()) / 2 : 0; }
    private static int padX(BingoBoard b) { return b.getWidth()  < VIEW_COLS ? (VIEW_COLS - b.getWidth())  / 2 : 0; }

    /**
     * The player's current scroll position clamped to {@code board} (which may
     * have shrunk since they last scrolled), plus the centering pads and live
     * arrow flags. This is the single source of truth every render and click
     * mapping reads, so a stale stored offset can never push cells off-grid.
     */
    public static Layout layout(UUID id, BingoBoard b) {
        int sr = Math.max(0, Math.min(rawStartRow(id), maxStartRow(b)));
        int sc = Math.max(0, Math.min(rawStartCol(id), maxStartCol(b)));
        return new Layout(sr, sc, padX(b), padY(b),
                sr > 0, sr < maxStartRow(b),
                sc > 0, sc < maxStartCol(b));
    }

    /** Stores {@code (sr, sc)} clamped to the board's scrollable range. */
    public static void set(UUID id, BingoBoard b, int sr, int sc) {
        sr = Math.max(0, Math.min(sr, maxStartRow(b)));
        sc = Math.max(0, Math.min(sc, maxStartCol(b)));
        OFFSETS.put(id, new int[]{sr, sc});
    }

    /** Nudges the viewport by {@code (dRow, dCol)} cells (clamped). */
    public static void scroll(UUID id, BingoBoard b, int dRow, int dCol) {
        set(id, b, rawStartRow(id) + dRow, rawStartCol(id) + dCol);
    }

    /**
     * Scrolls so board cell {@code index} sits as near the viewport center as the
     * edges allow — the clamp in {@link #set} naturally "pushes" the cell toward
     * an edge when true centering would scroll past the board.
     */
    public static void focusOn(UUID id, BingoBoard b, int index) {
        int br = index / b.getWidth();
        int bc = index % b.getWidth();
        set(id, b, br - (VIEW_ROWS - 1) / 2, bc - (VIEW_COLS - 1) / 2);
    }

    /** GUI slot showing board cell {@code index}, or {@code -1} if it's outside the viewport. */
    public static int indexToSlot(int index, BingoBoard b, Layout l) {
        int vr = index / b.getWidth() - l.startRow() + l.padY();
        int vc = index % b.getWidth() - l.startCol() + l.padX();
        if (vr < 0 || vr >= VIEW_ROWS || vc < 0 || vc >= VIEW_COLS) return -1;
        return (1 + vr) * GUI_WIDTH + (1 + vc);
    }

    /** Board cell index shown at GUI {@code slot}, or {@code -1} if it isn't a viewport cell. */
    public static int slotToIndex(int slot, BingoBoard b, Layout l) {
        int vr = slot / GUI_WIDTH - 1;
        int vc = slot % GUI_WIDTH - 1;
        if (vr < 0 || vr >= VIEW_ROWS || vc < 0 || vc >= VIEW_COLS) return -1;
        int br = l.startRow() + vr - l.padY();
        int bc = l.startCol() + vc - l.padX();
        if (br < 0 || br >= b.getHeight() || bc < 0 || bc >= b.getWidth()) return -1;
        return br * b.getWidth() + bc;
    }
}
