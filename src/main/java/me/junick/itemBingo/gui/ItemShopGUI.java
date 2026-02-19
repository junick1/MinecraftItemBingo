package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.util.IconGenerator;
import me.junick.itemBingo.util.PlayerDataManager;
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
    public static final String TITLE = "§e아이템 상점";

    public static void open(Player p) {
        PlayerBingoProgress prog = PlayerDataManager.get(p);

        Inventory inv = Bukkit.createInventory(null, 9*5, TITLE);

        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);

        ItemMeta glassMeta = grayGlass.getItemMeta();
        glassMeta.setHideTooltip(true);

        grayGlass.setItemMeta(glassMeta);
        redGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 9; i++) inv.setItem(i, grayGlass);
        for (int i = 1; i < 4; i++) {
            inv.setItem(9*i, redGlass);
            inv.setItem(9*i + 8, redGlass);
        }
        for (int i = 9*4; i < 9*5; i++) inv.setItem(i, grayGlass);

        inv.setItem(4, IconGenerator.currencyIcon(prog));

        int slot = 9*1 + 1;
        for (BingoItem itemEnum : BingoItem.values()) {
            List<Component> lore = new ArrayList<>();
            for (String line : itemEnum.getLore()) {
                lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }

            lore.add(Component.empty());
            lore.add(Component.text("가격: ", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));

            for (Map.Entry<BingoRewardType, Integer> entry : itemEnum.getAllPrices().entrySet()) {
                BingoRewardType type = entry.getKey();
                int cost = entry.getValue();
                if (cost <= 0) continue;

                lore.add(
                        Component.text(" • ", NamedTextColor.WHITE)
                                .append(Component.text(type.getDisplayName()))
                                .append(Component.text(": " + cost + "개", NamedTextColor.WHITE))
                                .decoration(TextDecoration.ITALIC, false)
                );
            }

            ItemStack item = new ItemStack(itemEnum.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(itemEnum.getDisplay()));
            meta.lore(lore);

            meta.getPersistentDataContainer().set(
                    new NamespacedKey(ItemBingo.getInstance(), "shop_item"),
                    PersistentDataType.STRING,
                    itemEnum.name()
            );

            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);

            inv.setItem(slot++, item);
        }

        inv.setItem(9*4 + 4, IconGenerator.backIcon());

        p.openInventory(inv);
    }
}
