package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
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
 * Generic selector for the custom map items. Identified by a {@link BingoGuiHolder}
 * of type {@code MAP_SELECTOR} whose {@code context} is the trigger item's name, so
 * the biome and explorer selectors are distinguished without relying on the (now
 * localized) title. Options not usable in the player's current dimension are greyed
 * out and left untagged, so they can't be selected at all.
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

    public static <T extends Enum<T> & MapOption> void open(Player player, String titleKey, String context, T[] options, Environment env) {
        SupportedLocale loc = Messages.localeOf(player);
        int count = options.length;

        // One row per 9 options, plus a reserved bottom row for controls.
        int contentRows = Math.max(1, (count + COLS - 1) / COLS);
        int rows = Math.min(MAX_ROWS, contentRows + 1);
        int size = rows * COLS;
        int capacity = size - COLS; // last row reserved for controls

        BingoGuiHolder holder = new BingoGuiHolder(BingoGuiHolder.Gui.MAP_SELECTOR, context);
        Inventory inv = Bukkit.createInventory(holder, size, Messages.get(loc, titleKey));
        holder.setInventory(inv);

        NamespacedKey key = optionKey();
        ItemStack filler = filler();

        for (int i = 0; i < capacity; i++) {
            if (i < count) {
                inv.setItem(i, optionIcon(options[i], env, key, loc));
            } else {
                inv.setItem(i, filler);
            }
        }
        // Guard: if the catalog ever outgrows the GUI, the overflow is dropped
        // here deliberately rather than silently scrambled by Inventory#addItem.

        for (int i = capacity; i < size; i++) {
            inv.setItem(i, filler);
        }
        inv.setItem(size - 5, closeButton(loc));

        player.openInventory(inv);
    }

    private static <T extends Enum<T> & MapOption> ItemStack optionIcon(T opt, Environment env, NamespacedKey key, SupportedLocale loc) {
        boolean usable = opt.getDimension() == env;

        ItemStack item = new ItemStack(usable ? opt.getIcon() : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();

        meta.itemName(Component.text(opt.displayName(loc), usable ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Messages.get(loc, "items.mapselector.dimension", "dim", dimensionName(opt.getDimension(), loc))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        if (usable) {
            lore.add(Messages.get(loc, "items.mapselector.click-to-create").decoration(TextDecoration.ITALIC, false));
            // Only usable options are tagged → out-of-dimension picks are impossible.
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, opt.name());
        } else {
            lore.add(Messages.get(loc, "items.mapselector.wrong-dimension").decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack closeButton(SupportedLocale loc) {
        ItemStack it = new ItemStack(Material.BARRIER);
        ItemMeta meta = it.getItemMeta();
        meta.itemName(Messages.get(loc, "items.mapselector.close.name").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Messages.get(loc, "items.mapselector.close.lore").decoration(TextDecoration.ITALIC, false)));
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

    public static String dimensionName(Environment env, SupportedLocale loc) {
        String key = switch (env) {
            case NETHER -> "dimension.nether";
            case THE_END -> "dimension.the_end";
            default -> "dimension.normal";
        };
        return Messages.legacy(loc, key);
    }
}
