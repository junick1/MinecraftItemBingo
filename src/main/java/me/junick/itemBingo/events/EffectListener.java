package me.junick.itemBingo.events;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.util.EffectApplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class EffectListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(ItemBingo.getInstance(), () -> EffectApplier.applyFor(p), 1L);
    }

    @EventHandler
    public void onRespawn(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(ItemBingo.getInstance(), () -> EffectApplier.applyFor(p), 1L);
    }
}
