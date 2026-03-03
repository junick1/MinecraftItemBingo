package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.util.IconGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class DiamondExchangeGUI {
    public static final String TITLE = "§b다이아몬드 환전";

    public static void open(Player p, int amount) {
        if (!new Settings(ItemBingo.getInstance()).isShopEnabled()) return;

        Inventory inv = Bukkit.createInventory(null, 9*5, TITLE);

        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayGlass.getItemMeta();
        grayMeta.setHideTooltip(true);
        grayGlass.setItemMeta(grayMeta);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, grayGlass);
        }

        inv.setItem(9*1 + 1, IconGenerator.icon(
                Material.RED_STAINED_GLASS,
                "§c-9",
                9
        ));


        inv.setItem(9*1 + 2, IconGenerator.icon(
                Material.RED_STAINED_GLASS,
                "§c-4",
                4
        ));

        inv.setItem(9*1 + 3, IconGenerator.icon(
                Material.RED_STAINED_GLASS,
                "§c-1"
        ));

        inv.setItem(9*1 + 5, IconGenerator.icon(
                Material.LIME_STAINED_GLASS,
                "§a+1"
        ));

        inv.setItem(9*1 + 6, IconGenerator.icon(
                Material.LIME_STAINED_GLASS,
                "§a+4",
                4
        ));

        inv.setItem(9*1 + 7, IconGenerator.icon(
                Material.LIME_STAINED_GLASS,
                "§a+9",
                9
        ));

        inv.setItem(9*2 + 4, IconGenerator.icon(
                Material.OAK_SIGN,
                "§e" + amount + "개",
                "§7환전할 다이아몬드의 개수입니다."
        ));

        inv.setItem(9*1 + 4, IconGenerator.icon(
                Material.DIAMOND,
                "§b다이아몬드",
                Math.clamp(amount, 1, 64)
        ));

        inv.setItem(9*3 + 2, IconGenerator.icon(Material.GREEN_TERRACOTTA, "§a확인"));
        inv.setItem(9*3 + 6, IconGenerator.icon(Material.RED_TERRACOTTA, "§c취소"));

        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(ItemBingo.getInstance(), "exchange_amount"),
                    PersistentDataType.INTEGER, amount
            );
            item.setItemMeta(meta);
        }
        p.openInventory(inv);
    }
}
