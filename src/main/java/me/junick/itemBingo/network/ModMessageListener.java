package me.junick.itemBingo.network;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.util.ProgressFactory;
import me.junick.itemBingo.util.SubmissionService;
import me.junick.itemBingo.util.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles all inbound companion-mod packets (hello / refresh / submit).
 *
 * <p>Everything a packet claims is re-validated against live server state: the
 * inventory slot's actual contents, the shared {@link SubmissionService} rules
 * (fog submit-lock, lockout, item match), and the same team gate as the chest
 * GUI. Malformed or stale packets are answered with a fresh board push (or
 * silently dropped) — never trusted.
 */
public final class ModMessageListener implements PluginMessageListener {

    /** Per-player message budget: at most this many packets per second. */
    private static final int RATE_LIMIT_PER_SECOND = 10;
    /** uuid → {windowStartMillis, countInWindow}. */
    private static final Map<UUID, long[]> RATE = new ConcurrentHashMap<>();

    static void clearRate(UUID id) {
        RATE.remove(id);
    }

    /** True when this packet exceeds the player's budget (drop silently). */
    private static boolean rateLimited(Player p) {
        long now = System.currentTimeMillis();
        long[] window = RATE.computeIfAbsent(p.getUniqueId(), id -> new long[]{now, 0});
        if (now - window[0] >= 1000) {
            window[0] = now;
            window[1] = 0;
        }
        return ++window[1] > RATE_LIMIT_PER_SECOND;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player p, byte @NotNull [] message) {
        if (rateLimited(p)) return;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            switch (channel) {
                case ModProtocol.CHANNEL_HELLO -> handleHello(p, in);
                case ModProtocol.CHANNEL_REFRESH -> handleRefresh(p);
                case ModProtocol.CHANNEL_SUBMIT -> handleSubmit(p, in);
                case ModProtocol.CHANNEL_ORIGINAL -> ModSync.sendOriginalBoard(p);
                default -> { /* not ours */ }
            }
        } catch (IOException | RuntimeException e) {
            // A malformed (possibly hostile) packet must never throw into the
            // pipeline. Log quietly and move on.
            ItemBingo.getInstance().getLogger().log(Level.FINE,
                    "Ignoring malformed companion-mod packet on " + channel + " from " + p.getName(), e);
        }
    }

    private void handleHello(Player p, DataInputStream in) throws IOException {
        int clientVersion = in.readInt();
        boolean accepted = clientVersion == ModProtocol.PROTOCOL_VERSION;
        if (accepted) {
            ModPlayers.add(p);
        }
        ModSync.sendHelloAck(p, accepted ? ModProtocol.HELLO_ACCEPTED : ModProtocol.HELLO_REJECTED);
        if (accepted) {
            ModSync.sendBoard(p);
        }
    }

    private void handleRefresh(Player p) {
        if (!ModPlayers.contains(p)) return;
        ModSync.sendBoard(p);
    }

    private void handleSubmit(Player p, DataInputStream in) throws IOException {
        if (!ModPlayers.contains(p)) return;

        byte mode = in.readByte();
        int cellIndex = in.readInt();
        int invSlot = in.readUnsignedByte();
        String expectedItemKey = in.readUTF();

        BingoBoard board = ItemBingo.currentBingo;
        if (board == null) {
            ModSync.sendBoard(p); // client thought a board existed — resync
            return;
        }

        // Same gate as the chest GUI: in team mode only assigned players submit
        // (OPs spectate). Silently ignored, matching the cancelled click.
        TeamManager tm = ItemBingo.getInstance().getTeamManager();
        if (Settings.isTeamEnabled() && tm.getTeamId(p) == TeamManager.NO_TEAM) return;

        boolean fromCursor = invSlot == ModProtocol.SLOT_CURSOR;
        if (!fromCursor && invSlot > 35) return; // hotbar 0-8 + main inventory 9-35 only
        if (fromCursor && mode != ModProtocol.SUBMIT_DIRECT) return; // shift-submit is slot-based

        ItemStack item = fromCursor ? p.getItemOnCursor() : p.getInventory().getItem(invSlot);

        // Staleness guard: the client names the item it thinks it is submitting.
        // If the live slot disagrees, the client's inventory view was outdated —
        // resync rather than consuming something the player didn't intend.
        if (!expectedItemKey.isEmpty()
                && (item == null || !item.getType().getKey().toString().equals(expectedItemKey))) {
            ModSync.sendBoard(p);
            ModSync.sendSubmitAck(p, cellIndex, ModProtocol.ACK_STALE);
            return;
        }

        BingoProgressAccess progress = ProgressFactory.of(p);

        if (mode == ModProtocol.SUBMIT_DIRECT) {
            switch (SubmissionService.validateDirect(p, progress, board, cellIndex, item)) {
                case OK, OK_FILLER -> {
                    consumeOne(p, fromCursor ? -1 : invSlot, item);
                    SubmissionService.completeSubmission(p, progress, board, cellIndex);
                    ModSync.sendSubmitAck(p, cellIndex, ModProtocol.ACK_OK);
                }
                case LOCKED -> {
                    SubmissionService.sendLockedMessage(p);
                    ModSync.sendSubmitAck(p, cellIndex, ModProtocol.ACK_LOCKED);
                }
                case ITEM_MISMATCH -> {
                    SubmissionService.sendInvalidItemMessage(p);
                    ModSync.sendSubmitAck(p, cellIndex, ModProtocol.ACK_MISMATCH);
                }
                case GAME_NOT_RUNNING -> {
                    SubmissionService.sendGameNotRunningMessage(p);
                    ModSync.sendSubmitAck(p, cellIndex, ModProtocol.ACK_NOT_RUNNING);
                }
                default -> {
                    ModSync.sendBoard(p); // stale view (already submitted, hidden, ...)
                    ModSync.sendSubmitAck(p, cellIndex, ModProtocol.ACK_STALE);
                }
            }
        } else if (mode == ModProtocol.SUBMIT_SHIFT) {
            if (!SubmissionService.isSubmissionOpen()) {
                SubmissionService.sendGameNotRunningMessage(p);
                ModSync.sendSubmitAck(p, -1, ModProtocol.ACK_NOT_RUNNING);
                return;
            }
            int idx = SubmissionService.findShiftTarget(p, progress, board, item);
            if (idx >= 0) {
                consumeOne(p, invSlot, item);
                SubmissionService.completeSubmission(p, progress, board, idx);
                ModSync.sendSubmitAck(p, idx, ModProtocol.ACK_OK);
            } else {
                ModSync.sendBoard(p); // nothing eligible — client view was stale
                ModSync.sendSubmitAck(p, -1, ModProtocol.ACK_STALE);
            }
        }
    }

    /** Consumes one from a slot, or from the cursor when {@code slot} is -1. */
    private void consumeOne(Player p, int slot, ItemStack item) {
        item.setAmount(item.getAmount() - 1);
        ItemStack remainder = item.getAmount() <= 0 ? null : item;
        if (slot < 0) {
            p.setItemOnCursor(remainder);
        } else {
            p.getInventory().setItem(slot, remainder);
        }
    }
}
