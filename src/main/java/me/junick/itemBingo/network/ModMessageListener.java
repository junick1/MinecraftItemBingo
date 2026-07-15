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

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player p, byte @NotNull [] message) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            switch (channel) {
                case ModProtocol.CHANNEL_HELLO -> handleHello(p, in);
                case ModProtocol.CHANNEL_REFRESH -> handleRefresh(p);
                case ModProtocol.CHANNEL_SUBMIT -> handleSubmit(p, in);
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
        ModSync.sendHelloAck(p, accepted);
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

        if (invSlot > 35) return; // hotbar 0-8 + main inventory 9-35 only

        ItemStack item = p.getInventory().getItem(invSlot);

        // Staleness guard: the client names the item it thinks it is submitting.
        // If the live slot disagrees, the client's inventory view was outdated —
        // resync rather than consuming something the player didn't intend.
        if (!expectedItemKey.isEmpty()
                && (item == null || !item.getType().getKey().toString().equals(expectedItemKey))) {
            ModSync.sendBoard(p);
            return;
        }

        BingoProgressAccess progress = ProgressFactory.of(p);

        if (mode == ModProtocol.SUBMIT_DIRECT) {
            switch (SubmissionService.validateDirect(p, progress, board, cellIndex, item)) {
                case OK, OK_FILLER -> {
                    consumeOne(p, invSlot, item);
                    SubmissionService.completeSubmission(p, progress, board, cellIndex);
                }
                case LOCKED -> SubmissionService.sendLockedMessage(p);
                case ITEM_MISMATCH -> SubmissionService.sendInvalidItemMessage(p);
                default -> ModSync.sendBoard(p); // stale view (already submitted, hidden, ...)
            }
        } else if (mode == ModProtocol.SUBMIT_SHIFT) {
            int idx = SubmissionService.findShiftTarget(p, progress, board, item);
            if (idx >= 0) {
                consumeOne(p, invSlot, item);
                SubmissionService.completeSubmission(p, progress, board, idx);
            } else {
                ModSync.sendBoard(p); // nothing eligible — client view was stale
            }
        }
    }

    private void consumeOne(Player p, int slot, ItemStack item) {
        item.setAmount(item.getAmount() - 1);
        p.getInventory().setItem(slot, item.getAmount() <= 0 ? null : item);
    }
}
