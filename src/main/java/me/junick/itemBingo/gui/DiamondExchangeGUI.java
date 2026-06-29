package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
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
    /** Title message key. GUI identity is the {@link BingoGuiHolder} marker, not the title. */
    public static final String TITLE_KEY = "gui.diamond.title";

    public static void open(Player p, int amount) {
        if (!Settings.isShopEnabled()) return;

        SupportedLocale loc = Messages.localeOf(p);

        // The current amount rides in the holder context so /language can reopen at the same amount.
        BingoGuiHolder holder = new BingoGuiHolder(BingoGuiHolder.Gui.DIAMOND_EXCHANGE, String.valueOf(amount));
        Inventory inv = Bukkit.createInventory(holder, 9 * 5, Messages.get(loc, TITLE_KEY));
        holder.setInventory(inv);

        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayGlass.getItemMeta();
        grayMeta.setHideTooltip(true);
        grayGlass.setItemMeta(grayMeta);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, grayGlass);
        }

        // Step buttons are pure numeric labels — no translation needed.
        inv.setItem(9 * 1 + 1, IconGenerator.icon(Material.RED_STAINED_GLASS, "§c-9", 9));
        inv.setItem(9 * 1 + 2, IconGenerator.icon(Material.RED_STAINED_GLASS, "§c-4", 4));
        inv.setItem(9 * 1 + 3, IconGenerator.icon(Material.RED_STAINED_GLASS, "§c-1"));
        inv.setItem(9 * 1 + 5, IconGenerator.icon(Material.LIME_STAINED_GLASS, "§a+1"));
        inv.setItem(9 * 1 + 6, IconGenerator.icon(Material.LIME_STAINED_GLASS, "§a+4", 4));
        inv.setItem(9 * 1 + 7, IconGenerator.icon(Material.LIME_STAINED_GLASS, "§a+9", 9));

        inv.setItem(9 * 2 + 4, IconGenerator.icon(
                Material.OAK_SIGN,
                Messages.legacy(loc, "gui.diamond.amount.name", "amount", amount),
                Messages.legacy(loc, "gui.diamond.amount.lore")
        ));

        inv.setItem(9 * 1 + 4, IconGenerator.icon(
                Material.DIAMOND,
                Messages.legacy(loc, "gui.diamond.diamond-name"),
                Math.clamp(amount, 1, 64)
        ));

        inv.setItem(9 * 2 + 6, IconGenerator.icon(
                Material.GOLD_INGOT,
                Messages.legacy(loc, "gui.diamond.all.name"),
                Messages.legacy(loc, "gui.diamond.all.lore")
        ));

        inv.setItem(9 * 3 + 2, IconGenerator.icon(
                Material.GREEN_TERRACOTTA,
                Messages.legacy(loc, "gui.diamond.confirm.name"),
                Messages.legacyList(loc, "gui.diamond.confirm.lore", "amount", amount).toArray(new String[0])
        ));
        inv.setItem(9 * 3 + 6, IconGenerator.icon(Material.RED_TERRACOTTA, Messages.legacy(loc, "gui.diamond.cancel.name")));

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
