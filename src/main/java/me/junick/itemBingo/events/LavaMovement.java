package me.junick.itemBingo.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;

public class LavaMovement implements Listener {
    private static HashSet<Player> ActivePlayerList = new HashSet<>();

    public static void addPlayer(Player p) { ActivePlayerList.add(p); }
    public static void removePlayer(Player p) { ActivePlayerList.remove(p); }
    public static void resetPlayer(Player p) { ActivePlayerList.clear(); }

    @EventHandler
    public void lavaMovement(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (!ActivePlayerList.contains(player)) return;
        if (player.getLocation().getBlock().getType() == Material.LAVA) {
            var velocity = player.getVelocity();
            var velocity3 = player.getLocation().getDirection().setY(0).normalize().multiply(0.33333);
            var velocity4 = player.getLocation().getDirection().normalize().multiply(0.33333);

            if (player.getCurrentInput().isSprint() && player.getCurrentInput().isForward()) {
                player.setVelocity(velocity.add(velocity4));
            }
        }
    }
}
