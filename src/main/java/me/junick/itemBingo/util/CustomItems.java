package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.enums.BingoItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

public class CustomItems {
    private static final NamespacedKey CUSTOM_TAG =
            new NamespacedKey(ItemBingo.getInstance(), "custom_item");

    public static ItemStack get(BingoItem b) {
        ItemStack item = createCustomItem(b.getIcon(),
                b.getDisplay(),
                b.getColor(),
                b.getLore(),
                b.name());
        return b.apply(item);
    }

    public static boolean is(ItemStack item, BingoItem b) { return hasCustomTag(item, b.name()); }

    /* ========================= Helper Methods ========================= */

    private static ItemStack createCustomItem(Material material, String name, TextColor color, List<String> loreLines, String tag) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.itemName(Component.text(name, color));
        meta.lore(createLoreComponents(loreLines));
        setCustomTag(meta, tag);
        addVisualEnchant(meta);
        item.setItemMeta(meta);

        return item;
    }

    private static List<Component> createLoreComponents(List<String> loreLines) {
        return loreLines.stream()
                .map(line -> Component.text(line, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());
    }

    private static void setCustomTag(ItemMeta meta, String tag) {
        meta.getPersistentDataContainer().set(CUSTOM_TAG, PersistentDataType.STRING, tag);
    }

    private static boolean hasCustomTag(ItemStack item, String expectedTag) {
        if (item == null || !item.hasItemMeta()) return false;

        String actualTag = item.getItemMeta()
                .getPersistentDataContainer()
                .get(CUSTOM_TAG, PersistentDataType.STRING);

        return expectedTag.equals(actualTag);
    }

    private static void addVisualEnchant(ItemMeta meta) {
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
    }
}
