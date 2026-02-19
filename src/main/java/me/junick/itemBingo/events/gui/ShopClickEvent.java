package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.gui.EffectShopGUI;
import me.junick.itemBingo.gui.ItemShopGUI;
import me.junick.itemBingo.gui.ShopGUI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ShopClickEvent implements Listener {
    @EventHandler
    public void onShopClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String title = LegacyComponentSerializer.legacySection().serialize(e.getView().title());
        if (!title.equals(ShopGUI.TITLE)) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String name = clicked.getItemMeta() != null ? clicked.getItemMeta().getDisplayName() : "";
        if (name.contains("이펙트")) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            EffectShopGUI.open(p);
        } else if (name.contains("아이템")) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            ItemShopGUI.open(p);
        }
    }
}
