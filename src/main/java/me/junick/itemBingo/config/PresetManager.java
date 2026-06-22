package me.junick.itemBingo.config;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.model.BingoBoard;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence for named board presets ("saved layouts").
 *
 * <p>A preset is created empty (just dimensions) and filled in-game via the editor, mirroring the
 * bundle workflow. Its items are stored under {@code presets.&lt;id&gt;.items} as a fixed-length
 * row-major list ({@code index = y * width + x}, matching {@code BingoGUI}); empty cells are stored
 * as {@code null} so grid positions are preserved while editing. A preset is only
 * {@link PresetInfo#isComplete() complete} — and thus applicable — once every cell is filled.</p>
 *
 * <p>Ids are restricted to {@link #ID_PATTERN} so they're safe single YAML path segments.</p>
 */
public class PresetManager {
    private static final File file = new File(ItemBingo.getInstance().getDataFolder(), "presets.yml");

    /** Allowed characters in a preset id — keeps it a safe single YAML path segment. */
    public static final String ID_PATTERN = "[A-Za-z0-9_-]+";

    /** Board dimension limits, mirrored from {@code /rollbingo}. */
    public static final int MAX_WIDTH = 9;
    public static final int MAX_HEIGHT = 6;

    /** Lightweight metadata for listing presets and checking applicability. */
    public record PresetInfo(String id, int width, int height, int filled) {
        public int slotCount() { return width * height; }
        /** A preset is applicable only once every cell holds an item. */
        public boolean isComplete() { return slotCount() > 0 && filled >= slotCount(); }
    }

    private static String path(String id) {
        return "presets." + id;
    }

    private static void save(YamlConfiguration config) {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int countFilled(List<?> items) {
        if (items == null) return 0;
        int n = 0;
        for (Object o : items) if (o != null) n++;
        return n;
    }

    public static boolean exists(String id) {
        return YamlConfiguration.loadConfiguration(file).contains(path(id));
    }

    /** @return metadata for {@code id}, or null if it doesn't exist. */
    public static PresetInfo getInfo(String id) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains(path(id))) return null;
        return new PresetInfo(
                id,
                config.getInt(path(id) + ".width"),
                config.getInt(path(id) + ".height"),
                countFilled(config.getList(path(id) + ".items")));
    }

    /** @return all presets' metadata, sorted by id, in a single config read. Never null. */
    public static List<PresetInfo> listInfo() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("presets");
        List<PresetInfo> result = new ArrayList<>();
        if (section == null) return result;

        List<String> ids = new ArrayList<>(section.getKeys(false));
        ids.sort(String::compareTo);
        for (String id : ids) {
            result.add(new PresetInfo(
                    id,
                    section.getInt(id + ".width"),
                    section.getInt(id + ".height"),
                    countFilled(section.getList(id + ".items"))));
        }
        return result;
    }

    /** @return preset ids in alphabetical order. Never null. */
    public static List<String> getIds() {
        List<String> ids = new ArrayList<>();
        for (PresetInfo info : listInfo()) ids.add(info.id());
        return ids;
    }

    /**
     * Creates a new, empty preset of the given dimensions (no items yet — fill it in the editor).
     *
     * @return true if created, false if {@code id} already exists.
     */
    public static boolean create(String id, int width, int height) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.contains(path(id))) return false;

        config.set(path(id) + ".width", width);
        config.set(path(id) + ".height", height);
        save(config);
        return true;
    }

    /** Persists the editor's board cells for {@code id} (a row-major list that may contain nulls). */
    public static void saveItems(String id, List<ItemStack> items) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains(path(id))) return;
        config.set(path(id) + ".items", items);
        save(config);
    }

    /** @return true if a preset was removed, false if {@code id} did not exist. */
    public static boolean delete(String id) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains(path(id))) return false;
        config.set(path(id), null);
        save(config);
        return true;
    }

    /** @return the saved board for {@code id}, or null if it doesn't exist. May contain null cells. */
    @SuppressWarnings("unchecked")
    public static BingoBoard load(String id) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains(path(id))) return null;

        int width = config.getInt(path(id) + ".width");
        int height = config.getInt(path(id) + ".height");
        List<ItemStack> items = (List<ItemStack>) config.getList(path(id) + ".items");
        if (items == null) items = new ArrayList<>();
        return new BingoBoard(width, height, items);
    }
}
