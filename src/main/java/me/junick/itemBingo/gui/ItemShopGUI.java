package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.util.IconGenerator;
import me.junick.itemBingo.util.ProgressFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemShopGUI {
    /** Title message key. GUI identity is the {@link BingoGuiHolder} marker, not the title. */
    public static final String TITLE_KEY = "gui.item-shop.title";
    public static final int SLOT_BACK = 9 * 4 + 4;

    public static void open(Player p) {
        if (!Settings.isShopEnabled()) return;
        if (!Settings.isItemShopEnabled()) return;

        BingoProgressAccess proga = ProgressFactory.of(p);
        SupportedLocale loc = Messages.localeOf(p);

        BingoGuiHolder holder = new BingoGuiHolder(BingoGuiHolder.Gui.ITEM_SHOP);
        Inventory inv = Bukkit.createInventory(holder, 9 * 5, Messages.get(loc, TITLE_KEY));
        holder.setInventory(inv);

        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);

        ItemMeta glassMeta = grayGlass.getItemMeta();
        glassMeta.setHideTooltip(true);

        grayGlass.setItemMeta(glassMeta);
        redGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 9; i++) inv.setItem(i, grayGlass);
        for (int i = 1; i < 4; i++) {
            inv.setItem(9 * i, redGlass);
            inv.setItem(9 * i + 8, redGlass);
        }
        for (int i = 9 * 4; i < 9 * 5; i++) inv.setItem(i, grayGlass);

        inv.setItem(4, IconGenerator.currencyIcon(p, proga));

        int slot = 9 * 1 + 1;
        for (BingoItem itemEnum : BingoItem.values()) {
            List<Component> lore = new ArrayList<>();
            for (String line : itemEnum.lore(loc)) {
                lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }

            lore.add(Component.empty());
            lore.add(Messages.get(loc, "gui.shop-common.price-label").decoration(TextDecoration.ITALIC, false));

            boolean affordable = true;
            for (Map.Entry<BingoRewardType, Integer> entry : itemEnum.getAllPrices().entrySet()) {
                BingoRewardType type = entry.getKey();
                int cost = entry.getValue();
                if (cost <= 0) continue;

                if (proga.getCurrency(p, type) < cost) affordable = false;

                lore.add(Messages.get(loc, "gui.shop-common.cost",
                                "reward", type.displayName(loc), "cost", cost)
                        .decoration(TextDecoration.ITALIC, false));
            }

            lore.add(Component.empty());
            lore.add((affordable
                    ? Messages.get(loc, "gui.shop-common.affordable")
                    : Messages.get(loc, "gui.shop-common.not-affordable"))
                    .decoration(TextDecoration.ITALIC, false));

            ItemStack item = new ItemStack(itemEnum.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.itemName(Component.text(itemEnum.displayName(loc), itemEnum.getColor()));
            meta.lore(lore);

            meta.getPersistentDataContainer().set(
                    new NamespacedKey(ItemBingo.getInstance(), "shop_item"),
                    PersistentDataType.STRING,
                    itemEnum.name()
            );

            // Glow only what the player can actually buy, as an at-a-glance cue.
            if (affordable) {
                meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);

            inv.setItem(slot++, item);
        }

        inv.setItem(SLOT_BACK, IconGenerator.backIcon(loc));

        p.openInventory(inv);
    }
}
