package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.PresetManager;
import me.junick.itemBingo.config.PresetManager.PresetInfo;
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
 * freely editable — the admin drags items in and out — and the layout is persisted on close (and
 * after a reroll). Every other slot is a non-interactive border; when there's room (board height
 * &lt; 6, or width &lt; 9) a control row/column holds an info panel, a "reroll all" button, and a
 * close button.</p>
 *
 * <p>The preset id travels in the inventory title ({@link #TITLE_PREFIX}{@code <id>}), which is how
 * {@code PresetEditorClickEvent} knows which preset it's editing.</p>
 */
public class PresetEditorGUI {
    public static final String TITLE_PREFIX = "§6프리셋 편집: ";

    private static final int COLS = 9;

    /** PDC key marking a control button; value is the action name ("reroll" / "close"). */
    public static NamespacedKey buttonKey() {
        return new NamespacedKey(ItemBingo.getInstance(), "preset_editor_button");
    }

    public static String title(String id) {
        return TITLE_PREFIX + id;
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
        PresetInfo info = PresetManager.getInfo(id);
        if (info == null) {
            player.sendMessage("§c존재하지 않는 프리셋입니다: " + id);
            return;
        }
        int width = info.width(), height = info.height();
        int size = rows(height) * COLS;

        Inventory inv = Bukkit.createInventory(null, size, title(id));

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

        placeControls(inv, info, size);
        player.openInventory(inv);
    }

    /** Places info / reroll / close into spare (non-board) slots, if any exist. */
    private static void placeControls(Inventory inv, PresetInfo info, int size) {
        if (info.height() < MAX_ROWS) {
            // A full control row sits directly below the board.
            int base = info.height() * COLS;
            inv.setItem(base, infoPanel(info));
            inv.setItem(base + 4, rerollButton());
            inv.setItem(base + 8, closeButton());
            return;
        }
        // Height 6: reuse the unused right-hand columns (none exist for a full 9×6 board).
        List<Integer> spare = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            if (!isBoardCell(slot, info.width(), info.height())) spare.add(slot);
        }
        if (spare.size() >= 1) inv.setItem(spare.get(0), infoPanel(info));
        if (spare.size() >= 2) inv.setItem(spare.get(1), rerollButton());
        if (spare.size() >= 3) inv.setItem(spare.get(spare.size() - 1), closeButton());
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

    private static ItemStack infoPanel(PresetInfo info) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("프리셋: " + info.id(), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(line("크기: " + info.width() + " x " + info.height(), NamedTextColor.GRAY));
        lore.add(line("채워진 칸: " + info.filled() + " / " + info.slotCount(), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(line("칸에 아이템을 끌어다 놓아 편집하세요.", NamedTextColor.GRAY));
        lore.add(line("닫으면 자동으로 저장됩니다.", NamedTextColor.GREEN));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack rerollButton() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("전체 재추첨", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                line("모든 칸을 무작위 아이템으로 다시 채웁니다.", NamedTextColor.GRAY),
                line("기존 구성은 사라집니다.", NamedTextColor.RED)));
        meta.getPersistentDataContainer().set(buttonKey(), PersistentDataType.STRING, "reroll");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack closeButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("저장하고 닫기", NamedTextColor.RED)
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
