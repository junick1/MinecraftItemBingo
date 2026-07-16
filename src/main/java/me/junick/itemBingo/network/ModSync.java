package me.junick.itemBingo.network;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.model.BingoBoard;
import me.junick.itemBingo.util.TimerManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        if (!ModPlayers.contains(p)) return;
        p.sendPluginMessage(ItemBingo.getInstance(), ModProtocol.CHANNEL_BOARD, BoardViewBuilder.build(p));
    }

    /**
     * Re-sends the board to every handshaken player. Called wherever the chest
     * GUI already refreshes viewers — unlike the GUI, the mod's HUD overlay is
     * always visible, so every modded player needs the update, not just those
     * with a board open.
     */
    public static void broadcastBoard() {
        ModPlayers.forEach(ModSync::sendBoard);
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

    public static void sendHelloAck(Player p, boolean accepted) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(ModProtocol.PROTOCOL_VERSION);
            out.writeBoolean(accepted);
            p.sendPluginMessage(ItemBingo.getInstance(), ModProtocol.CHANNEL_HELLO, bytes.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
