package me.junick.itembingo.client.screen;

/**
 * Pure pan/zoom math for the fullscreen board. Board space is measured in
 * pixels at zoom 1 ({@link #CELL} px per cell); {@code offsetX/offsetY} is the
 * board-space point shown at the viewport's top-left corner. The last camera
 * is remembered for the session so reopening the screen restores the view;
 * a board with different dimensions resets it.
 */
final class BoardCamera {
    /** Cell edge in board-space pixels at zoom 1 (16px icon + frame). */
    static final int CELL = 22;

    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 3.0f;

    private static double savedOffsetX;
    private static double savedOffsetY;
    private static float savedZoom = 1.0f;
    private static int savedBoardW = -1;
    private static int savedBoardH = -1;

    private int boardW;
    private int boardH;
    private int vpW;
    private int vpH;

    double offsetX;
    double offsetY;
    float zoom = 1.0f;

    /** Binds the camera to the current board and viewport, restoring or centering. */
    void attach(int boardW, int boardH, int vpW, int vpH) {
        this.boardW = boardW;
        this.boardH = boardH;
        this.vpW = vpW;
        this.vpH = vpH;
        if (boardW == savedBoardW && boardH == savedBoardH) {
            offsetX = savedOffsetX;
            offsetY = savedOffsetY;
            zoom = savedZoom;
            clamp();
        } else {
            zoom = 1.0f;
            center();
        }
    }

    /** Viewport size changed (window resize); keep the view, re-clamp. */
    void resize(int vpW, int vpH) {
        this.vpW = vpW;
        this.vpH = vpH;
        clamp();
    }

    /** New board dimensions arrived mid-view. */
    void boardChanged(int boardW, int boardH) {
        if (boardW == this.boardW && boardH == this.boardH) return;
        this.boardW = boardW;
        this.boardH = boardH;
        zoom = 1.0f;
        center();
    }

    void center() {
        offsetX = (boardW * CELL - vpW / (double) zoom) / 2.0;
        offsetY = (boardH * CELL - vpH / (double) zoom) / 2.0;
        clamp();
    }

    /** Pans by a screen-space delta (drag follows the cursor 1:1). */
    void pan(double screenDx, double screenDy) {
        offsetX -= screenDx / zoom;
        offsetY -= screenDy / zoom;
        clamp();
    }

    /** Zooms toward the given viewport-relative point so it stays put. */
    void zoomAt(double viewX, double viewY, double steps) {
        float newZoom = Math.clamp(zoom * (float) Math.pow(1.15, steps), MIN_ZOOM, MAX_ZOOM);
        if (newZoom == zoom) return;
        offsetX = offsetX + viewX / zoom - viewX / newZoom;
        offsetY = offsetY + viewY / zoom - viewY / newZoom;
        zoom = newZoom;
        clamp();
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

    /** Screen x of a cell's left edge, given the viewport's left edge. */
    double cellScreenX(int col, int vpX) {
        return vpX + (col * CELL - offsetX) * zoom;
    }

    double cellScreenY(int row, int vpY) {
        return vpY + (row * CELL - offsetY) * zoom;
    }

    double cellScreenSize() {
        return CELL * (double) zoom;
    }

    /** Board column under a screen x, or -1 outside the board. */
    int colAt(double screenX, int vpX) {
        int col = (int) Math.floor((offsetX + (screenX - vpX) / zoom) / CELL);
        return (col < 0 || col >= boardW) ? -1 : col;
    }

    int rowAt(double screenY, int vpY) {
        int row = (int) Math.floor((offsetY + (screenY - vpY) / zoom) / CELL);
        return (row < 0 || row >= boardH) ? -1 : row;
    }

    int visibleTopLeftCol() {
        return Math.clamp((int) Math.floor(Math.max(0, offsetX) / CELL), 0, Math.max(0, boardW - 1));
    }

    int visibleTopLeftRow() {
        return Math.clamp((int) Math.floor(Math.max(0, offsetY) / CELL), 0, Math.max(0, boardH - 1));
    }

    void save() {
        savedOffsetX = offsetX;
        savedOffsetY = offsetY;
        savedZoom = zoom;
        savedBoardW = boardW;
        savedBoardH = boardH;
    }
}
