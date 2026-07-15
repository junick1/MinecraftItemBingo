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
     * Sends the pristine board for image export, gated like {@code /summary
     * board} plus a mode exception: allowed whenever the game is not running,
     * or anytime in modes that never hide items (everything but Fog of War).
     */
    public static void sendOriginalBoard(Player p) {
        if (!ModPlayers.contains(p)) return;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            BingoBoard board = ItemBingo.currentBingo;
            if (board == null) {
                out.writeByte(ModProtocol.ORIGINAL_NO_BOARD);
            } else if (TimerManager.isRunning() && Settings.isFogOfWarMode()) {
                out.writeByte(ModProtocol.ORIGINAL_DENIED_FOG);
            } else {
                out.writeByte(ModProtocol.ORIGINAL_OK);
                out.writeShort(board.getWidth());
                out.writeShort(board.getHeight());
                for (ItemStack item : board.getItems()) {
                    out.writeUTF(item.getType().getKey().toString());
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
