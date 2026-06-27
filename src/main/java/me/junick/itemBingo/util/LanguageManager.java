package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.i18n.SupportedLocale;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player language override. By default a player follows their Minecraft
 * client locale; running {@code /language <english|korean>} stores an explicit
 * choice here, and {@code /language auto} clears it.
 *
 * <p>Persisted to {@code languages.yml} (uuid → locale id) — stored separately
 * from board/progress data so a board reset ({@code applyNewBoard()}) never
 * clears a player's chosen language. Mirrors {@link ChatManager}.</p>
 */
public final class LanguageManager {
    private LanguageManager() {}

    private static final Map<UUID, SupportedLocale> overrides = new HashMap<>();
    private static File file;

    public static void load() {
        file = new File(ItemBingo.getInstance().getDataFolder(), "languages.yml");
        overrides.clear();
        if (!file.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            SupportedLocale loc = SupportedLocale.fromId(yml.getString(key));
            if (loc == null) continue;
            try {
                overrides.put(UUID.fromString(key), loc);
            } catch (IllegalArgumentException ignored) {
                // skip malformed uuid keys
            }
        }
    }

    public static void save() {
        if (file == null) return;

        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, SupportedLocale> e : overrides.entrySet()) {
            yml.set(e.getKey().toString(), e.getValue().id());
        }
        try {
            yml.save(file);
        } catch (IOException ex) {
            ItemBingo.getInstance().getLogger().severe("Failed to save languages.yml: " + ex.getMessage());
        }
    }

    /** The player's explicit choice, or {@code null} if they follow their client locale. */
    public static @Nullable SupportedLocale getOverride(UUID uuid) {
        return overrides.get(uuid);
    }

    public static void setOverride(UUID uuid, SupportedLocale loc) {
        overrides.put(uuid, loc);
        save();
    }

    /** Clears the override so the player follows their client locale again. */
    public static void clearOverride(UUID uuid) {
        if (overrides.remove(uuid) != null) {
            save();
        }
    }
}
