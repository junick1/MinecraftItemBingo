package me.junick.itemBingo.network;

/**
 * Wire protocol constants shared with the companion Fabric mod.
 *
 * <p><b>MUST match {@code fabric/src/main/java/me/junick/itembingo/client/net/ModProtocol.java}.</b>
 * Bump {@link #PROTOCOL_VERSION} on ANY schema change — the handshake rejects
 * mismatched versions, which makes the client fall back to vanilla behavior.
 *
 * <p>All payloads are big-endian {@code DataOutputStream} bytes; strings are
 * {@code writeUTF}. See the schema comments on each channel constant.
 */
public final class ModProtocol {
    private ModProtocol() {}

    public static final int PROTOCOL_VERSION = 4;

    /** C2S {@code i32 protocolVersion} / S2C {@code i32 serverProtocolVersion, u8 accepted}. */
    public static final String CHANNEL_HELLO = "itembingo:hello";

    /**
     * S2C full per-viewer board view:
     * {@code u8 status}; when {@link #STATUS_OK}: {@code u8 gameMode (Settings.GameMode
     * ordinal), u8 gameStage (TimerManager.GameStage ordinal), u8 flags,
     * u16 width, u16 height, u16 submittedCount, u16 totalCells},
     * then width*height cells row-major ({@code index = y*width + x}), each
     * {@code u8 cellState} followed by state-specific data:
     * VISIBLE → {@code UTF itemKey}; HIDDEN/LOCKED → nothing;
     * SUBMITTED → {@code UTF itemKey, u8 hasSubmitter, [UTF submitterName], i64 elapsedSeconds}.
     */
    public static final String CHANNEL_BOARD = "itembingo:board";

    /**
     * C2S submission request:
     * {@code u8 mode, i32 cellIndex (-1 for SHIFT), u8 inventorySlot (0-35, or
     * SLOT_CURSOR for the item carried on the cursor — DIRECT only),
     * UTF expectedItemKey ("" = skip staleness check)}.
     */
    public static final String CHANNEL_SUBMIT = "itembingo:submit";

    /** C2S empty body — requests a fresh board push. */
    public static final String CHANNEL_REFRESH = "itembingo:refresh";

    /**
     * Original (markless) board for image export.
     * C2S: empty request. S2C: {@code u8 status (ORIGINAL_*)}; when OK:
     * {@code u8 partial}, {@code u16 width, u16 height}, then width*height
     * cells row-major, each {@code u8 hasItem} followed by {@code UTF itemKey}
     * when 1. {@code partial=1} means fog before game start: only the initial
     * starter reveals carry items. Denied while a Fog of War game is RUNNING —
     * the original would reveal exactly what fog hides.
     */
    public static final String CHANNEL_ORIGINAL = "itembingo:original";

    public static final byte ORIGINAL_OK = 0;
    public static final byte ORIGINAL_DENIED_FOG = 1;
    public static final byte ORIGINAL_NO_BOARD = 2;

    public static final byte STAGE_NOT_STARTED = 0;
    public static final byte STAGE_RUNNING = 1;
    public static final byte STAGE_ENDED = 2;

    public static final byte STATUS_OK = 0;
    public static final byte STATUS_NO_BOARD = 1;
    public static final byte STATUS_VIEW_DENIED = 2;

    public static final byte CELL_VISIBLE = 0;
    public static final byte CELL_HIDDEN = 1;
    public static final byte CELL_LOCKED = 2;
    public static final byte CELL_SUBMITTED = 3;

    public static final byte SUBMIT_DIRECT = 0;
    public static final byte SUBMIT_SHIFT = 1;

    /** Sentinel inventorySlot: the item is on the player's cursor, not in a slot. */
    public static final int SLOT_CURSOR = 255;

    /** Fog of War submit-lock is active: hidden cells reject submissions. */
    public static final int FLAG_FOG_SUBMIT_LOCK = 1;
}
