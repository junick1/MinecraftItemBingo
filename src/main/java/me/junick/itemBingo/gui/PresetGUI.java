package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.PresetManager;
import me.junick.itemBingo.config.PresetManager.PresetInfo;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated, read-only browser for saved board presets, used in two modes:
 * {@link Mode#APPLY} (click a preset to make it the active board) and {@link Mode#EDIT} (click to
 * open it in the {@link PresetEditorGUI}). The mode rides in the {@link BingoGuiHolder} context, so
 * the click handler and the page-nav buttons can preserve it without depending on the title.
 */
public class PresetGUI {
    public enum Mode { APPLY, EDIT }

    private static final int SIZE = 54;
    private static final int CONTENT_START = 9;          // rows 1..5
    public static final int PAGE_SIZE = SIZE - CONTENT_START; // 45 icons per page

    private static final int PREV_SLOT = 0;
    private static final int INFO_SLOT = 4;
    private static final int NEXT_SLOT = 8;

    /** PDC key carrying a preset id on each selectable icon. */
    public static NamespacedKey presetKey() {
        return new NamespacedKey(ItemBingo.getInstance(), "preset_id");
    }

    /** PDC key carrying a target page index on the prev/next nav buttons. */
    public static NamespacedKey navKey() {
        return new NamespacedKey(ItemBingo.getInstance(), "preset_nav_page");
    }

    public static void open(Player player, int page, Mode mode) {
        SupportedLocale loc = Messages.localeOf(player);
        List<PresetInfo> presets = PresetManager.listInfo();
        int pageCount = Math.max(1, (presets.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 0) page = 0;
        if (page >= pageCount) page = pageCount - 1;

        BingoGuiHolder holder = new BingoGuiHolder(BingoGuiHolder.Gui.PRESET, mode.name());
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                Messages.get(loc, mode == Mode.EDIT ? "gui.preset.edit-title" : "gui.preset.apply-title"));
        holder.setInventory(inv);

        ItemStack glass = hiddenGlass();
        for (int i = 0; i < CONTENT_START; i++) inv.setItem(i, glass);

        if (page > 0) inv.setItem(PREV_SLOT, navButton(Messages.legacy(loc, "gui.preset.prev"), page - 1));
        if (page < pageCount - 1) inv.setItem(NEXT_SLOT, navButton(Messages.legacy(loc, "gui.preset.next"), page + 1));
        inv.setItem(INFO_SLOT, info(loc, page, pageCount, presets.size(), mode));

        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, presets.size());
        int slot = CONTENT_START;
        for (int i = from; i < to; i++) {
            inv.setItem(slot++, presetIcon(loc, presets.get(i), mode));
        }

        player.openInventory(inv);
    }

    private static ItemStack presetIcon(SupportedLocale loc, PresetInfo preset, Mode mode) {
        boolean complete = preset.isComplete();
        ItemStack item = new ItemStack(complete ? Material.FILLED_MAP : Material.MAP);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(preset.id(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(line(Messages.legacy(loc, "gui.preset.size", "w", preset.width(), "h", preset.height()), NamedTextColor.GRAY));
        lore.add(line(Messages.legacy(loc, "gui.preset.filled", "filled", preset.filled(), "total", preset.slotCount()),
                complete ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        lore.add(Component.empty());
        if (mode == Mode.EDIT) {
            lore.add(line(Messages.legacy(loc, "gui.preset.click-edit"), NamedTextColor.YELLOW));
        } else if (complete) {
            lore.add(line(Messages.legacy(loc, "gui.preset.click-apply"), NamedTextColor.YELLOW));
            lore.add(line(Messages.legacy(loc, "gui.preset.apply-warn"), NamedTextColor.RED));
        } else {
            lore.add(line(Messages.legacy(loc, "gui.preset.incomplete-icon"), NamedTextColor.RED));
            lore.add(line("/editbingo " + preset.id(), NamedTextColor.DARK_GRAY));
        }
        meta.lore(lore);

        meta.getPersistentDataContainer().set(presetKey(), PersistentDataType.STRING, preset.id());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack navButton(String label, int targetPage) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(navKey(), PersistentDataType.INTEGER, targetPage);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack info(SupportedLocale loc, int page, int pageCount, int total, Mode mode) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(Messages.legacy(loc, mode == Mode.EDIT ? "gui.preset.info-edit" : "gui.preset.info-apply"),
                NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(line(Messages.legacy(loc, "gui.preset.page", "page", page + 1, "pageCount", pageCount), NamedTextColor.GRAY));
        lore.add(line(Messages.legacy(loc, "gui.preset.total", "total", total), NamedTextColor.GRAY));
        if (total == 0) {
            lore.add(Component.empty());
            lore.add(line(Messages.legacy(loc, "gui.preset.empty-hint"), NamedTextColor.DARK_GRAY));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack hiddenGlass() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setHideTooltip(true);
        glass.setItemMeta(meta);
        return glass;
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
