package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.BundleManager;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the bundle manager. GUI identity is the {@link BingoGuiHolder} marker
 * ({@code BUNDLE}); the shown template index rides on the preview item's PDC.
 */
public class BundleGUI {
    public static final int BUNDLE_SLOT = 4;
    public static final int EDIT_START = 9;
    public static final int EDIT_END = 35;                 // inclusive (27 slots)
    public static final int SEPARATOR_START = 36;
    public static final int TEMPLATE_ROW_START = 45;       // 45..53 -> templates 0..8

    /** Rainbow spectrum, one terracotta per template index. */
    private static final Material[] TEMPLATE_COLORS = {
            Material.RED_TERRACOTTA,
            Material.ORANGE_TERRACOTTA,
            Material.YELLOW_TERRACOTTA,
            Material.LIME_TERRACOTTA,
            Material.GREEN_TERRACOTTA,
            Material.CYAN_TERRACOTTA,
            Material.LIGHT_BLUE_TERRACOTTA,
            Material.BLUE_TERRACOTTA,
            Material.PURPLE_TERRACOTTA,
    };

    public static void open(Player player, int templateIdx) {
        if (templateIdx < 0 || templateIdx >= BundleManager.TEMPLATE_COUNT) templateIdx = 0;

        SupportedLocale loc = Messages.localeOf(player);
        BingoGuiHolder holder = new BingoGuiHolder(BingoGuiHolder.Gui.BUNDLE);
        Inventory inv = Bukkit.createInventory(holder, 54, Messages.get(loc, "gui.bundle.title"));
        holder.setInventory(inv);

        ItemStack glass = hiddenGlass();
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = SEPARATOR_START; i < TEMPLATE_ROW_START; i++) inv.setItem(i, glass);

        inv.setItem(BUNDLE_SLOT, bundlePreview(templateIdx, loc));

        // Editable region: only template 1..8 actually carry items.
        List<ItemStack> items = BundleManager.loadTemplate(templateIdx);
        for (int i = 0; i < BundleManager.BUNDLE_SIZE; i++) {
            ItemStack it = i < items.size() ? items.get(i) : null;
            inv.setItem(EDIT_START + i, it);
        }

        for (int i = 0; i < BundleManager.TEMPLATE_COUNT; i++) {
            inv.setItem(TEMPLATE_ROW_START + i, templateIcon(i, templateIdx, loc));
        }

        player.openInventory(inv);
    }

    private static ItemStack hiddenGlass() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setHideTooltip(true);
        glass.setItemMeta(meta);
        return glass;
    }

    private static ItemStack bundlePreview(int templateIdx, SupportedLocale loc) {
        ItemStack bundle = new ItemStack(Material.BUNDLE);
        ItemMeta meta = bundle.getItemMeta();
        meta.displayName(Component.text(Messages.legacy(loc, "gui.bundle.preview-title", "idx", templateIdx), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(line(Messages.legacy(loc, "gui.bundle.preview-desc1"), NamedTextColor.GRAY));
        lore.add(line(Messages.legacy(loc, "gui.bundle.preview-desc2"), NamedTextColor.GRAY));
        lore.add(Component.empty());
        if (templateIdx == 0) {
            lore.add(line(Messages.legacy(loc, "gui.bundle.preview-empty1"), NamedTextColor.RED));
            lore.add(line(Messages.legacy(loc, "gui.bundle.preview-empty2"), NamedTextColor.RED));
        } else {
            lore.add(line(Messages.legacy(loc, "gui.bundle.preview-selected1"), NamedTextColor.GREEN));
            lore.add(line(Messages.legacy(loc, "gui.bundle.preview-selected2"), NamedTextColor.GREEN));
        }
        meta.lore(lore);

        meta.getPersistentDataContainer().set(ItemBingo.KEY_BUNDLE_TEMPLATE, PersistentDataType.INTEGER, templateIdx);
        bundle.setItemMeta(meta);
        return bundle;
    }

    private static ItemStack templateIcon(int idx, int selected, SupportedLocale loc) {
        ItemStack item = new ItemStack(TEMPLATE_COLORS[idx]);
        ItemMeta meta = item.getItemMeta();

        String name = idx == 0
                ? Messages.legacy(loc, "gui.bundle.empty-template")
                : Messages.legacy(loc, "gui.bundle.template", "idx", idx);
        meta.displayName(Component.text(name, idx == selected ? NamedTextColor.YELLOW : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (idx == selected) {
            lore.add(line(Messages.legacy(loc, "gui.bundle.current"), NamedTextColor.GREEN));
        } else {
            lore.add(line(Messages.legacy(loc, "gui.bundle.click-select"), NamedTextColor.GRAY));
        }
        meta.lore(lore);

        if (idx == selected) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    /** Reads the template index this inventory is showing, recorded on the preview item. */
    public static int readTemplateIndex(Inventory top) {
        ItemStack preview = top.getItem(BUNDLE_SLOT);
        if (preview == null || !preview.hasItemMeta()) return 0;
        Integer idx = preview.getItemMeta().getPersistentDataContainer()
                .get(ItemBingo.KEY_BUNDLE_TEMPLATE, PersistentDataType.INTEGER);
        if (idx == null || idx < 0 || idx >= BundleManager.TEMPLATE_COUNT) return 0;
        return idx;
    }

    /** Saves the editable region of {@code top} into {@code templateIdx} (no-op for template 0). */
    public static void persistEditable(Inventory top, int templateIdx) {
        if (templateIdx <= 0 || templateIdx >= BundleManager.TEMPLATE_COUNT) return;
        List<ItemStack> items = new ArrayList<>(BundleManager.BUNDLE_SIZE);
        for (int i = EDIT_START; i <= EDIT_END; i++) {
            items.add(top.getItem(i));
        }
        BundleManager.saveTemplate(templateIdx, items);
    }
}
