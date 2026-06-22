package me.junick.itemBingo.config;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.gui.BundleGUI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistence + distribution for the starter "bundle".
 *
 * <p>There are {@link #TEMPLATE_COUNT} templates (0..8). Template 0 is always empty and cannot be
 * edited — selecting it means players start with nothing. Exactly one template is "selected" at any
 * time; that selection is persisted and is the kit handed out when bingo starts.</p>
 */
public class BundleManager {
    private static final File file = new File(ItemBingo.getInstance().getDataFolder(), "bundle.yml");
    private static final File player_list = new File(ItemBingo.getInstance().getDataFolder(), "bundle_player_list.yml");

    /** Number of templates, indexed 0..TEMPLATE_COUNT-1. */
    public static final int TEMPLATE_COUNT = 9;
    /** Items per template (the editable region of the GUI). */
    public static final int BUNDLE_SIZE = 27;

    private static String templatePath(int idx) {
        return "templates.template" + idx;
    }

    private static void save(YamlConfiguration config) {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** @return the currently selected template index (clamped to a valid range). */
    public static int getSelectedTemplate() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int sel = config.getInt("selected_template", 0);
        if (sel < 0 || sel >= TEMPLATE_COUNT) sel = 0;
        return sel;
    }

    public static void setSelectedTemplate(int idx) {
        if (idx < 0 || idx >= TEMPLATE_COUNT) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("selected_template", idx);
        save(config);
    }

    /** @return the items stored in {@code idx}. Template 0 is always empty. Never null. */
    @SuppressWarnings("unchecked")
    public static List<ItemStack> loadTemplate(int idx) {
        if (idx <= 0 || idx >= TEMPLATE_COUNT) return new ArrayList<>();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<ItemStack> items = (List<ItemStack>) config.getList(templatePath(idx));
        if (items != null) {
            if (items.size() > BUNDLE_SIZE) items = items.subList(0, BUNDLE_SIZE);
            return items;
        }
        return new ArrayList<>();
    }

    /** Persist {@code items} into {@code idx}. No-op for the empty/uneditable template 0. */
    public static void saveTemplate(int idx, List<ItemStack> items) {
        if (idx <= 0 || idx >= TEMPLATE_COUNT) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set(templatePath(idx), items);
        save(config);
    }

    /** Opens the bundle manager GUI on the currently selected template. */
    public static void openStorage(Player player) {
        BundleGUI.open(player, getSelectedTemplate());
    }

    public static void resetPlayerList() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(player_list);
        config.set("players", new ArrayList<String>());
        try {
            config.save(player_list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Hands the selected template's items to {@code p} once per game. */
    @SuppressWarnings("unchecked")
    public static void getBundle(Player p) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(player_list);
        List<String> list = (List<String>) config.getList("players");
        if (list == null) {
            resetPlayerList();
            list = new ArrayList<>();
        }
        if (list.contains(p.getName())) {
            p.sendMessage("§c이미 번들을 받았습니다.");
            return;
        }

        List<ItemStack> items = new ArrayList<>(loadTemplate(getSelectedTemplate()));
        items.removeAll(Collections.singleton(null));
        if (!items.isEmpty()) {
            p.getInventory().addItem(items.toArray(new ItemStack[0]));
        }

        list.add(p.getName());
        config.set("players", list);
        try {
            config.save(player_list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
