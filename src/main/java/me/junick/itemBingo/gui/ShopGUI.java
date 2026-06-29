package me.junick.itemBingo.gui;

import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.util.IconGenerator;
import me.junick.itemBingo.util.ProgressFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ShopGUI {
    /** Title message key. GUI identity is the {@link BingoGuiHolder} marker, not the title. */
    public static final String TITLE_KEY = "gui.shop.title";

    /** Entry-button slots (used by {@code ShopClickEvent} for locale-independent routing). */
    public static final int SLOT_EFFECT = 11;
    public static final int SLOT_ITEM = 15;

    public static void open(Player p) {
        if (!Settings.isShopEnabled()) {
            p.sendMessage(Messages.get(p, "shop.disabled"));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        SupportedLocale loc = Messages.localeOf(p);

        BingoGuiHolder holder = new BingoGuiHolder(BingoGuiHolder.Gui.SHOP);
        Inventory inv = Bukkit.createInventory(holder, 9 * 3, Messages.get(loc, TITLE_KEY));
        holder.setInventory(inv);

        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = grayGlass.getItemMeta();
        glassMeta.setHideTooltip(true);
        grayGlass.setItemMeta(glassMeta);

        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, grayGlass);

        inv.setItem(4, IconGenerator.currencyIcon(p, ProgressFactory.of(p)));

        inv.setItem(SLOT_EFFECT, IconGenerator.icon(
                Material.BEACON,
                Messages.legacy(loc, "gui.shop.effect.name"),
                Messages.legacy(loc, "gui.shop.effect.lore")
        ));

        inv.setItem(SLOT_ITEM, IconGenerator.icon(
                Material.CHEST,
                Messages.legacy(loc, "gui.shop.item.name"),
                Messages.legacy(loc, "gui.shop.item.lore")
        ));

        p.openInventory(inv);
    }
}
