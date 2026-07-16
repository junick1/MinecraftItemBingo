package me.junick.itembingo.client.screen;

/**
 * Pan/zoom math over board space ({@link #CELL} px per cell at zoom 1).
 *
 * <p>The board-space center and zoom are SHARED, session-wide state: the
 * fullscreen board and the HUD overlay each own a {@code BoardCamera} with
 * their own viewport size, but {@link #sync} reads the shared view every
 * frame and {@link #pan}/{@link #zoomAt} write it back — so panning one view
 * moves the other identically. New board dimensions reset the shared view to
 * a centered 100%.
 */
public final class BoardCamera {
    /** Cell edge in board-space pixels at zoom 1 (16px icon + frame). */
    public static final int CELL = 22;

    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 3.0f;

    private static double sharedCenterX;
    private static double sharedCenterY;
    private static float sharedZoom = 1.0f;
    private static int sharedBoardW = -1;
    private static int sharedBoardH = -1;

    private int boardW = 1;
    private int boardH = 1;
    private int vpW = 1;
    private int vpH = 1;

    private double offsetX;
    private double offsetY;
    private float zoom = 1.0f;

    /**
     * Adopts the current board/viewport sizes and reads the shared view.
     * Call once per frame before rendering or hit-testing.
     */
    public void sync(int boardW, int boardH, int vpW, int vpH) {
        this.boardW = Math.max(1, boardW);
        this.boardH = Math.max(1, boardH);
        this.vpW = Math.max(1, vpW);
        this.vpH = Math.max(1, vpH);
        if (this.boardW != sharedBoardW || this.boardH != sharedBoardH) {
            sharedBoardW = this.boardW;
            sharedBoardH = this.boardH;
            sharedZoom = 1.0f;
            sharedCenterX = this.boardW * CELL / 2.0;
            sharedCenterY = this.boardH * CELL / 2.0;
        }
        zoom = sharedZoom;
        offsetX = sharedCenterX - this.vpW / (2.0 * zoom);
        offsetY = sharedCenterY - this.vpH / (2.0 * zoom);
        clamp();
    }

    /** Pans by a viewport-space delta (drag follows the cursor 1:1). */
    public void pan(double screenDx, double screenDy) {
        offsetX -= screenDx / zoom;
        offsetY -= screenDy / zoom;
        clamp();
        write();
    }

    /** Zooms toward the given viewport-relative point so it stays put. */
    public void zoomAt(double viewX, double viewY, double steps) {
        float newZoom = Math.clamp(zoom * (float) Math.pow(1.15, steps), MIN_ZOOM, MAX_ZOOM);
        if (newZoom == zoom) return;
        offsetX = offsetX + viewX / zoom - viewX / newZoom;
        offsetY = offsetY + viewY / zoom - viewY / newZoom;
        zoom = newZoom;
        clamp();
        write();
    }

    private void write() {
        sharedZoom = zoom;
        sharedCenterX = offsetX + vpW / (2.0 * zoom);
        sharedCenterY = offsetY + vpH / (2.0 * zoom);
    }

    /**
     * Keeps the board on screen: axes larger than the viewport clamp to its
     * edges, smaller ones stay centered (offset goes negative — padding).
     */
    private void clamp() {
        double viewW = vpW / (double) zoom;
        double viewH = vpH / (double) zoom;
        double boardPxW = boardW * CELL;
        double boardPxH = boardH * CELL;
        offsetX = boardPxW <= viewW ? (boardPxW - viewW) / 2.0 : Math.clamp(offsetX, 0, boardPxW - viewW);
        offsetY = boardPxH <= viewH ? (boardPxH - viewH) / 2.0 : Math.clamp(offsetY, 0, boardPxH - viewH);
    }

    public float zoom() {
        return zoom;
    }

    /** Viewport x of a cell's left edge, given the viewport's left edge. */
    public double cellScreenX(int col, int vpX) {
        return vpX + (col * CELL - offsetX) * zoom;
    }

    public double cellScreenY(int row, int vpY) {
        return vpY + (row * CELL - offsetY) * zoom;
    }

    public double cellScreenSize() {
        return CELL * (double) zoom;
    }

    /** Board column under a viewport x, or -1 outside the board. */
    public int colAt(double screenX, int vpX) {
        int col = (int) Math.floor((offsetX + (screenX - vpX) / zoom) / CELL);
        return (col < 0 || col >= boardW) ? -1 : col;
    }

    public int rowAt(double screenY, int vpY) {
        int row = (int) Math.floor((offsetY + (screenY - vpY) / zoom) / CELL);
        return (row < 0 || row >= boardH) ? -1 : row;
    }

    /* Exact visible cell window, so render loops touch only on-screen cells. */

    public int firstVisibleCol() {
        return Math.max(0, (int) Math.floor(offsetX / CELL));
    }

    public int lastVisibleCol() {
        return Math.min(boardW - 1, (int) Math.floor((offsetX + vpW / (double) zoom) / CELL));
    }

    public int firstVisibleRow() {
        return Math.max(0, (int) Math.floor(offsetY / CELL));
    }

    public int lastVisibleRow() {
        return Math.min(boardH - 1, (int) Math.floor((offsetY + vpH / (double) zoom) / CELL));
    }
}
