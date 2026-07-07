package me.junick.itemBingo.events.items;

import me.junick.itemBingo.gui.DiamondExchangeGUI;
import me.junick.itemBingo.gui.EmeraldExchangeGUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EmeraldEvent implements Listener {
    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!e.getAction().isRightClick()) return;
        if (e.getItem() == null) return;

        Player p = e.getPlayer();
        ItemStack item = e.getItem();

        if (item.getType() != Material.EMERALD) return;

        e.setCancelled(true);

        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        EmeraldExchangeGUI.open(p, 1);
    }
}
