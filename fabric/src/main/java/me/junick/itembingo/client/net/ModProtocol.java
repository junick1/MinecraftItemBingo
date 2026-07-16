package me.junick.itembingo.client.net;

/**
 * Wire protocol constants shared with the server plugin.
 *
 * <p><b>MUST match {@code src/main/java/me/junick/itemBingo/network/ModProtocol.java}.</b>
 * Bump {@link #PROTOCOL_VERSION} on ANY schema change — the handshake rejects
 * mismatched versions, which makes this client fall back to vanilla behavior.
 *
 * <p>All payloads are big-endian {@code DataOutputStream} bytes; strings are
 * {@code writeUTF}.
 */
public final class ModProtocol {
    private ModProtocol() {}

    public static final int PROTOCOL_VERSION = 5;

    public static final byte HELLO_REJECTED = 0;
    public static final byte HELLO_ACCEPTED = 1;
    /** Server asks the client to re-send its hello (plugin reloaded). */
    public static final byte HELLO_REQUEST = 2;

    public static final byte ACK_OK = 0;
    public static final byte ACK_LOCKED = 1;
    public static final byte ACK_MISMATCH = 2;
    public static final byte ACK_STALE = 3;
    public static final byte ACK_NOT_RUNNING = 4;

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

    /** itembingo:original — S2C status codes for the markless-board export. */
    public static final byte ORIGINAL_OK = 0;
    public static final byte ORIGINAL_DENIED_FOG = 1;
    public static final byte ORIGINAL_NO_BOARD = 2;
    public static final byte ORIGINAL_VIEW_DENIED = 3;

    /** Game lifecycle stage carried in the board payload. */
    public static final byte STAGE_NOT_STARTED = 0;
    public static final byte STAGE_RUNNING = 1;
    public static final byte STAGE_ENDED = 2;

    /** Fog of War submit-lock is active: hidden cells reject submissions. */
    public static final int FLAG_FOG_SUBMIT_LOCK = 1;
    /** Team mode is enabled (labels shift from "my" to "team" wording). */
    public static final int FLAG_TEAM_MODE = 2;
}
