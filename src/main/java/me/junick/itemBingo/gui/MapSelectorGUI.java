package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.interfaces.MapOption;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic selector for the custom map items. Lays out one icon per option, with
 * a reserved control row holding a close button, and marks each selectable
 * option with a persistent-data tag (rather than relying on its icon material).
 * Options not usable in the player's current dimension are greyed out and left
 * untagged, so they can't be selected at all.
 */
public class MapSelectorGUI {

    private static final int COLS = 9;
    private static final int MAX_ROWS = 6;

    /** PDC key carrying the selected option's enum name on a usable icon. */
    public static NamespacedKey optionKey() {
        return new NamespacedKey(ItemBingo.getInstance(), "map_option");
    }

    /** PDC marker identifying the close button. */
    public static NamespacedKey cancelKey() {
        return new NamespacedKey(ItemBingo.getInstance(), "map_cancel");
    }

    public static <T extends Enum<T> & MapOption> void open(Player player, String title, T[] options, Environment env) {
        int count = options.length;

        // One row per 9 options, plus a reserved bottom row for controls.
        int contentRows = Math.max(1, (count + COLS - 1) / COLS);
        int rows = Math.min(MAX_ROWS, contentRows + 1);
        int size = rows * COLS;
        int capacity = size - COLS; // last row reserved for controls

        Inventory inv = Bukkit.createInventory(null, size, title);

        NamespacedKey key = optionKey();
        ItemStack filler = filler();

        for (int i = 0; i < capacity; i++) {
            if (i < count) {
                inv.setItem(i, optionIcon(options[i], env, key));
            } else {
                inv.setItem(i, filler);
            }
        }
        // Guard: if the catalog ever outgrows the GUI, the overflow is dropped
        // here deliberately rather than silently scrambled by Inventory#addItem.

        for (int i = capacity; i < size; i++) {
            inv.setItem(i, filler);
        }
        inv.setItem(size - 5, closeButton());

        player.openInventory(inv);
    }

    private static <T extends Enum<T> & MapOption> ItemStack optionIcon(T opt, Environment env, NamespacedKey key) {
        boolean usable = opt.getDimension() == env;

        ItemStack item = new ItemStack(usable ? opt.getIcon() : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();

        meta.itemName(Component.text(opt.getName(), usable ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("차원: " + dimensionName(opt.getDimension()), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        if (usable) {
            lore.add(Component.text("클릭하여 지도 생성", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            // Only usable options are tagged → out-of-dimension picks are impossible.
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, opt.name());
        } else {
            lore.add(Component.text("✖ 현재 차원에서 사용할 수 없습니다", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack closeButton() {
        ItemStack it = new ItemStack(Material.BARRIER);
        ItemMeta meta = it.getItemMeta();
        meta.itemName(Component.text("닫기", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("선택을 취소하고 닫습니다.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(cancelKey(), PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack filler() {
        ItemStack it = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        meta.setHideTooltip(true);
        it.setItemMeta(meta);
        return it;
    }

    public static String dimensionName(Environment env) {
        return switch (env) {
            case NETHER -> "네더";
            case THE_END -> "엔드";
            default -> "오버월드";
        };
    }
}
