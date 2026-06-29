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
 * In-game editor for a single board preset, modelled on the bundle editor.
 *
 * <p>The board is laid out top-left as a true {@code width × height} grid (cell {@code (x, y)} →
 * inventory slot {@code y*9 + x}), so it reads exactly like the in-game board. Those cells are
 * freely editable and the layout is persisted on close (and after a reroll). Every other slot is a
 * non-interactive border.</p>
 *
 * <p>The preset id rides in the {@link BingoGuiHolder} context, which is how
 * {@code PresetEditorClickEvent} knows which preset it's editing.</p>
 */
public class PresetEditorGUI {

    private static final int COLS = 9;

    /** PDC key marking a control button; value is the action name ("reroll" / "close"). */
    public static NamespacedKey buttonKey() {
        return new NamespacedKey(ItemBingo.getInstance(), "preset_editor_button");
    }

    /** Inventory rows: the board's rows plus a control row when one fits (height &lt; 6). */
    public static int rows(int height) {
        return Math.min(MAX_ROWS, height + 1);
    }
    private static final int MAX_ROWS = 6;

    public static boolean isBoardCell(int slot, int width, int height) {
        int x = slot % COLS, y = slot / COLS;
        return x < width && y < height;
    }

    public static int slotToIndex(int slot, int width) {
        return (slot / COLS) * width + (slot % COLS);
    }

    public static void open(Player player, String id) {
        SupportedLocale loc = Messages.localeOf(player);
        PresetInfo info = PresetManager.getInfo(id);
        if (info == null) {
            player.sendMessage(Messages.get(player, "command.setbingo.not-found", "id", id));
            return;
        }
        int width = info.width(), height = info.height();
        int size = rows(height) * COLS;

        BingoGuiHolder holder = new BingoGuiHolder(BingoGuiHolder.Gui.PRESET_EDITOR, id);
        Inventory inv = Bukkit.createInventory(holder, size, Messages.get(loc, "gui.preset-editor.title", "id", id));
        holder.setInventory(inv);

        // Board cells get the stored items (null = empty); everything else is a border pane.
        List<ItemStack> stored = PresetManager.load(id).getItems();
        ItemStack glass = hiddenGlass();
        for (int slot = 0; slot < size; slot++) {
            if (isBoardCell(slot, width, height)) {
                int idx = slotToIndex(slot, width);
                inv.setItem(slot, idx < stored.size() ? stored.get(idx) : null);
            } else {
                inv.setItem(slot, glass);
            }
        }

        placeControls(inv, info, size, loc);
        player.openInventory(inv);
    }

    /** Places info / reroll / close into spare (non-board) slots, if any exist. */
    private static void placeControls(Inventory inv, PresetInfo info, int size, SupportedLocale loc) {
        if (info.height() < MAX_ROWS) {
            // A full control row sits directly below the board.
            int base = info.height() * COLS;
            inv.setItem(base, infoPanel(info, loc));
            inv.setItem(base + 4, rerollButton(loc));
            inv.setItem(base + 8, closeButton(loc));
            return;
        }
        // Height 6: reuse the unused right-hand columns (none exist for a full 9×6 board).
        List<Integer> spare = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            if (!isBoardCell(slot, info.width(), info.height())) spare.add(slot);
        }
        if (spare.size() >= 1) inv.setItem(spare.get(0), infoPanel(info, loc));
        if (spare.size() >= 2) inv.setItem(spare.get(1), rerollButton(loc));
        if (spare.size() >= 3) inv.setItem(spare.get(spare.size() - 1), closeButton(loc));
    }

    /** Reads the editor's board cells back into a fixed-length row-major list (nulls for empties). */
    public static void persist(Inventory top, String id, int width, int height) {
        List<ItemStack> items = new ArrayList<>(width * height);
        for (int i = 0; i < width * height; i++) items.add(null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                items.set(y * width + x, top.getItem(y * COLS + x));
            }
        }
        PresetManager.saveItems(id, items);
    }

    private static ItemStack infoPanel(PresetInfo info, SupportedLocale loc) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(Messages.legacy(loc, "gui.preset-editor.info-title", "id", info.id()), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(line(Messages.legacy(loc, "gui.preset.size", "w", info.width(), "h", info.height()), NamedTextColor.GRAY));
        lore.add(line(Messages.legacy(loc, "gui.preset.filled", "filled", info.filled(), "total", info.slotCount()), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(line(Messages.legacy(loc, "gui.preset-editor.drag-hint"), NamedTextColor.GRAY));
        lore.add(line(Messages.legacy(loc, "gui.preset-editor.autosave"), NamedTextColor.GREEN));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack rerollButton(SupportedLocale loc) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(Messages.legacy(loc, "gui.preset-editor.reroll"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                line(Messages.legacy(loc, "gui.preset-editor.reroll-desc1"), NamedTextColor.GRAY),
                line(Messages.legacy(loc, "gui.preset-editor.reroll-desc2"), NamedTextColor.RED)));
        meta.getPersistentDataContainer().set(buttonKey(), PersistentDataType.STRING, "reroll");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack closeButton(SupportedLocale loc) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(Messages.legacy(loc, "gui.preset-editor.save-close"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(buttonKey(), PersistentDataType.STRING, "close");
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
