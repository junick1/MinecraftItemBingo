package me.junick.itemBingo.network;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.util.Lockout;
import me.junick.itemBingo.util.TimerManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Outbound pushes to companion-mod clients. Cheap no-ops for vanilla players:
 * every send is gated on the {@link ModPlayers} handshake set.
 */
public final class ModSync {
    private ModSync() {}

    /** Sends {@code p} their current per-viewer board state (if they run the mod). */
    public static void sendBoard(Player p) {
        sendBoard(p, null);
    }

    private static void sendBoard(Player p, @Nullable java.util.Set<Integer> allClaimed) {
        if (!ModPlayers.contains(p)) return;
        p.sendPluginMessage(ItemBingo.getInstance(), ModProtocol.CHANNEL_BOARD, BoardViewBuilder.build(p, allClaimed));
    }

    /**
     * Re-sends the board to every handshaken player. Called wherever the chest
     * GUI already refreshes viewers — unlike the GUI, the mod's HUD overlay is
     * always visible, so every modded player needs the update, not just those
     * with a board open. In Lockout the claimed-cell scan runs once here, not
     * once per viewer.
     */
    public static void broadcastBoard() {
        java.util.Set<Integer> allClaimed = Settings.isLockoutMode() ? Lockout.allClaimedSlots() : null;
        ModPlayers.forEach(p -> sendBoard(p, allClaimed));
    }

    /** Asks every online modded client to redo the handshake (plugin reload). */
    public static void requestReHello() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(ModProtocol.PROTOCOL_VERSION);
            out.writeByte(ModProtocol.HELLO_REQUEST);
            byte[] payload = bytes.toByteArray();
            for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                // Delivered only to clients listening on the channel; vanilla
                // players are skipped by Bukkit automatically.
                p.sendPluginMessage(ItemBingo.getInstance(), ModProtocol.CHANNEL_HELLO, payload);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Immediate accept/reject feedback for a submit request. */
    public static void sendSubmitAck(Player p, int cellIndex, byte result) {
        if (!ModPlayers.contains(p)) return;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(cellIndex);
            out.writeByte(result);
            p.sendPluginMessage(ItemBingo.getInstance(), ModProtocol.CHANNEL_ACK, bytes.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Sends the pristine board for image export. Modes that never hide items
     * always get the full board. Fog of War depends on the game stage: RUNNING
     * is denied outright; NOT_STARTED gets a partial board carrying only the
     * initial starter reveals (exactly what everyone can already see); ENDED
     * gets everything, matching {@code /summary board}.
     */
    public static void sendOriginalBoard(Player p) {
        if (!ModPlayers.contains(p)) return;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            BingoBoard board = ItemBingo.currentBingo;
            boolean fog = Settings.isFogOfWarMode();
            TimerManager.GameStage stage = TimerManager.getStage();
            if (board == null) {
                out.writeByte(ModProtocol.ORIGINAL_NO_BOARD);
            } else if (!me.junick.itemBingo.gui.BingoGUI.canView(p)) {
                // Same gate as the live board: no team in team mode, no export.
                out.writeByte(ModProtocol.ORIGINAL_VIEW_DENIED);
            } else if (fog && stage == TimerManager.GameStage.RUNNING) {
                out.writeByte(ModProtocol.ORIGINAL_DENIED_FOG);
            } else {
                boolean partial = fog && stage == TimerManager.GameStage.NOT_STARTED;
                java.util.Set<Integer> revealed = partial
                        ? me.junick.itemBingo.util.FogOfWar.initialReveals(board.getWidth(), board.getHeight())
                        : null;
                out.writeByte(ModProtocol.ORIGINAL_OK);
                out.writeBoolean(partial);
                out.writeShort(board.getWidth());
                out.writeShort(board.getHeight());
                java.util.List<ItemStack> items = board.getItems();
                for (int idx = 0; idx < items.size(); idx++) {
                    boolean hasItem = revealed == null || revealed.contains(idx);
                    out.writeBoolean(hasItem);
                    if (hasItem) {
                        out.writeUTF(items.get(idx).getType().getKey().toString());
                    }
                }
            }
            p.sendPluginMessage(ItemBingo.getInstance(), ModProtocol.CHANNEL_ORIGINAL, bytes.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void sendHelloAck(Player p, byte code) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(ModProtocol.PROTOCOL_VERSION);
            out.writeByte(code);
            p.sendPluginMessage(ItemBingo.getInstance(), ModProtocol.CHANNEL_HELLO, bytes.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
