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

public final class PasswordManager {
    private PasswordManager() {}

    private static final Map<UUID, String> passwords = new HashMap<>();
    private static File file;

    public static void load() {
        file = new File(ItemBingo.getInstance().getDataFolder(), "password.yml");
        if (!file.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            String password = yml.getString(key);
            if (password == null) continue;
            try {
                passwords.put(UUID.fromString(key), password);
            } catch (IllegalArgumentException ignored) {
                // skip malformed uuid keys
            }
        }
    }

    public static void save() {
        if (file == null) return;

        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : passwords.entrySet()) {
            yml.set(e.getKey().toString(), e.getValue());
        }
        try {
            yml.save(file);
        } catch (IOException ex) {
            ItemBingo.getInstance().getLogger().severe("Failed to save password.yml: " + ex.getMessage());
        }
    }

    public static @Nullable String getPassword(UUID uuid) {
        return passwords.get(uuid);
    }

    public static void setPassword(UUID uuid, String pass) {
        passwords.put(uuid, pass);
        save();
    }

    public static void clearPassword(UUID uuid) {
        if (passwords.remove(uuid) != null) {
            save();
        }
    }

}
