package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.gui.BingoGUI;
import me.junick.itemBingo.gui.MenuGUI;
import me.junick.itemBingo.gui.ShopGUI;
import me.junick.itemBingo.gui.SummaryGUI;
import me.junick.itemBingo.util.ChestManager;
import me.junick.itemBingo.util.RankMessageGenerator;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MenuClickEvent implements Listener {
    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String title = LegacyComponentSerializer.legacySection().serialize(e.getView().title());
        if (!title.equals(MenuGUI.TITLE)) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String name = clicked.getItemMeta() != null ? clicked.getItemMeta().getDisplayName() : "";
        if (name.contains("빙고판")) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            BingoGUI.open(p);
        } else if (name.contains("게임 결과")) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            p.closeInventory();
            SummaryGUI.tryOpen(p);
        } else if (name.contains("랭킹")) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            RankMessageGenerator.sendRankMessage(p);
            p.closeInventory();
        } else if (name.contains("상점")) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            ShopGUI.open(p);
        } else if (name.contains("창고")) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            p.closeInventory();
            ChestManager.open(p);
        }
    }
}
