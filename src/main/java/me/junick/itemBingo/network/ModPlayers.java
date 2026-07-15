package me.junick.itemBingo.network;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Tracks which online players completed the companion-mod handshake with a
 * matching protocol version. Board pushes and submit packets are gated on
 * membership here — a player who never said hello (or said it with the wrong
 * version) is treated exactly like a vanilla client.
 */
public final class ModPlayers implements Listener {
    private static final Set<UUID> MODDED = ConcurrentHashMap.newKeySet();

    public static void add(Player p) {
        MODDED.add(p.getUniqueId());
    }

    public static void remove(Player p) {
        MODDED.remove(p.getUniqueId());
    }

    public static boolean contains(Player p) {
        return MODDED.contains(p.getUniqueId());
    }

    /** Runs {@code action} for every online handshaken player. */
    public static void forEach(Consumer<Player> action) {
        for (UUID id : MODDED) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                action.accept(p);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        remove(e.getPlayer());
    }
}
