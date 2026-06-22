package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.BundleManager;
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
 * Renders the bundle manager.
 *
 * <pre>
 * row 0 (0-8)   : 4 glass | bundle preview (slot 4) | 4 glass
 * row 1-3 (9-35): editable region — the selected template's items
 * row 4 (36-44) : glass separator
 * row 5 (45-53) : 9 terracotta template selectors (0..8)
 * </pre>
 */
public class BundleGUI {
    public static final String TITLE = "§6번들 매니저";

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

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        ItemStack glass = hiddenGlass();
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = SEPARATOR_START; i < TEMPLATE_ROW_START; i++) inv.setItem(i, glass);

        inv.setItem(BUNDLE_SLOT, bundlePreview(templateIdx));

        // Editable region: only template 1..8 actually carry items.
        List<ItemStack> items = BundleManager.loadTemplate(templateIdx);
        for (int i = 0; i < BundleManager.BUNDLE_SIZE; i++) {
            ItemStack it = i < items.size() ? items.get(i) : null;
            inv.setItem(EDIT_START + i, it);
        }

        for (int i = 0; i < BundleManager.TEMPLATE_COUNT; i++) {
            inv.setItem(TEMPLATE_ROW_START + i, templateIcon(i, templateIdx));
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

    private static ItemStack bundlePreview(int templateIdx) {
        ItemStack bundle = new ItemStack(Material.BUNDLE);
        ItemMeta meta = bundle.getItemMeta();
        meta.displayName(Component.text("번들 미리보기 — 템플릿 #" + templateIdx, NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(line("아래 27칸에 시작 아이템을 배치하세요.", NamedTextColor.GRAY));
        lore.add(line("하단의 점토로 템플릿을 전환합니다.", NamedTextColor.GRAY));
        lore.add(Component.empty());
        if (templateIdx == 0) {
            lore.add(line("이 템플릿은 비어 있으며 수정할 수 없습니다.", NamedTextColor.RED));
            lore.add(line("선택 시 플레이어는 빈손으로 시작합니다.", NamedTextColor.RED));
        } else {
            lore.add(line("이 템플릿이 현재 선택되어 있습니다.", NamedTextColor.GREEN));
            lore.add(line("빙고 시작 시 이 구성이 지급됩니다.", NamedTextColor.GREEN));
        }
        meta.lore(lore);

        meta.getPersistentDataContainer().set(ItemBingo.KEY_BUNDLE_TEMPLATE, PersistentDataType.INTEGER, templateIdx);
        bundle.setItemMeta(meta);
        return bundle;
    }

    private static ItemStack templateIcon(int idx, int selected) {
        ItemStack item = new ItemStack(TEMPLATE_COLORS[idx]);
        ItemMeta meta = item.getItemMeta();

        String name = idx == 0 ? "빈 번들 (수정 불가)" : "번들 템플릿 #" + idx;
        meta.displayName(Component.text(name, idx == selected ? NamedTextColor.YELLOW : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (idx == selected) {
            lore.add(line("✔ 현재 선택됨", NamedTextColor.GREEN));
        } else {
            lore.add(line("클릭하여 이 템플릿을 선택", NamedTextColor.GRAY));
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
