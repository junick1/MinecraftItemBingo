package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.EffectShopGUI;
import me.junick.itemBingo.gui.ItemShopGUI;
import me.junick.itemBingo.gui.ShopGUI;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ShopClickEvent implements Listener {
    @EventHandler
    public void onShopClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!BingoGuiHolder.is(e.getView().getTopInventory(), BingoGuiHolder.Gui.SHOP)) return;

        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == ShopGUI.SLOT_EFFECT) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            EffectShopGUI.open(p);
        } else if (slot == ShopGUI.SLOT_ITEM) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            ItemShopGUI.open(p);
        }
    }
}
