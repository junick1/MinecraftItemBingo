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

    public static final int PROTOCOL_VERSION = 5;

    /**
     * C2S {@code i32 protocolVersion} / S2C {@code i32 serverProtocolVersion,
     * u8 code (HELLO_*)}. {@code HELLO_REQUEST} asks the client to re-send its
     * hello — used after a plugin reload wipes the handshake set.
     */
    public static final String CHANNEL_HELLO = "itembingo:hello";

    public static final byte HELLO_REJECTED = 0;
    public static final byte HELLO_ACCEPTED = 1;
    public static final byte HELLO_REQUEST = 2;

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
     * S2C submission acknowledgement: {@code i32 cellIndex (-1 when unknown),
     * u8 result (ACK_*)} — lets the client give immediate rejection feedback.
     */
    public static final String CHANNEL_ACK = "itembingo:submit_ack";

    public static final byte ACK_OK = 0;
    public static final byte ACK_LOCKED = 1;
    public static final byte ACK_MISMATCH = 2;
    public static final byte ACK_STALE = 3;
    public static final byte ACK_NOT_RUNNING = 4;

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
    public static final byte ORIGINAL_VIEW_DENIED = 3;

    public static final byte STAGE_NOT_STARTED = 0;
    public static final byte STAGE_RUNNING = 1;
    public static final byte STAGE_ENDED = 2;

    /** Explicit wire values for the game mode — never enum ordinals. */
    public static final byte MODE_NORMAL = 0;
    public static final byte MODE_SWAPPAGE = 1;
    public static final byte MODE_FOG_OF_WAR = 2;
    public static final byte MODE_LOCKOUT = 3;

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
    /** Team mode is enabled (labels shift from "my" to "team" wording). */
    public static final int FLAG_TEAM_MODE = 2;
}
