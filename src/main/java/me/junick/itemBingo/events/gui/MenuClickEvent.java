package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.gui.BingoGUI;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.MenuGUI;
import me.junick.itemBingo.gui.ShopGUI;
import me.junick.itemBingo.gui.SummaryGUI;
import me.junick.itemBingo.util.ChestManager;
import me.junick.itemBingo.util.RankMessageGenerator;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuClickEvent implements Listener {
    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!BingoGuiHolder.is(e.getView().getTopInventory(), BingoGuiHolder.Gui.MENU)) return;

        e.setCancelled(true);

        switch (e.getRawSlot()) {
            case MenuGUI.SLOT_BOARD -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                BingoGUI.open(p);
            }
            case MenuGUI.SLOT_RESULTS -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                p.closeInventory();
                SummaryGUI.tryOpen(p);
            }
            case MenuGUI.SLOT_RANK -> {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                RankMessageGenerator.sendRankMessage(p);
                p.closeInventory();
            }
            case MenuGUI.SLOT_SHOP -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                ShopGUI.open(p);
            }
            case MenuGUI.SLOT_CHEST -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                p.closeInventory();
                ChestManager.open(p);
            }
            default -> { /* filler or unmapped slot */ }
        }
    }
}
