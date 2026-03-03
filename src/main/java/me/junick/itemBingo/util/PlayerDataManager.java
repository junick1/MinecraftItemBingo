package me.junick.itemBingo.util;

import me.junick.itemBingo.enums.BingoEffect;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.model.PlayerBingoProgress;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerDataManager {
    private static final Map<UUID, PlayerBingoProgress> data = new HashMap<>();
    private static final File folder = new File(
            Bukkit.getPluginManager().getPlugin("ItemBingo").getDataFolder(),
            "bingo-progress"
    );

    static {
        if (!folder.exists() && !folder.mkdirs()) {
            Bukkit.getLogger().warning("[ItemBingo] Failed to create data folder.");
        }
    }

    /* ========================= Public API ========================= */

    public static @Nonnull PlayerBingoProgress get(Player p) {
        return data.computeIfAbsent(p.getUniqueId(), PlayerDataManager::load);
    }

    public static @Nullable PlayerBingoProgress get(UUID u) {
        return data.get(u);
    }

    public static Map<UUID, PlayerBingoProgress> getAllData() {
        Map<UUID, PlayerBingoProgress> all = new HashMap<>();
        for (UUID uuid : listAllUUIDs()) {
            all.put(uuid, load(uuid));
        }
        return all;
    }

    public static void save(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerBingoProgress progress = data.get(uuid);
        if (progress != null) save(uuid, progress);
    }

    public static void resetAll() {
        data.clear();
        deleteAllFiles();
        Bukkit.getLogger().info("[ItemBingo] All player data has been reset.");
    }

    public static PlayerBingoProgress load(UUID uuid) {
        File file = getFile(uuid);
        if (!file.exists()) return new PlayerBingoProgress();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerBingoProgress progress = new PlayerBingoProgress();

        loadSubmittedSlots(config, progress);
        loadSubmissionTimes(config, progress);
        loadEffectLevels(config, progress);
        loadCurrencies(config, progress);

        return progress;
    }

    /* ========================= Internal Save ========================= */

    public static void save(UUID uuid, PlayerBingoProgress progress) {
        File file = getFile(uuid);
        YamlConfiguration config = new YamlConfiguration();

        saveSubmittedSlots(progress, config);
        saveSubmissionTimes(progress, config);
        saveEffectLevels(progress, config);
        saveCurrencies(progress, config);

        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[ItemBingo] Failed to save data for " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* ========================= File / UUID ========================= */

    private static File getFile(UUID uuid) {
        return new File(folder, uuid.toString() + ".yml");
    }

    private static List<UUID> listAllUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return uuids;

        for (File file : files) {
            try {
                uuids.add(UUID.fromString(file.getName().replace(".yml", "")));
            } catch (IllegalArgumentException ignored) {}
        }
        return uuids;
    }

    private static void deleteAllFiles() {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            if (!file.delete()) {
                Bukkit.getLogger().warning("[ItemBingo] Failed to delete: " + file.getName());
            }
        }
    }

    /* ========================= Save Sections ========================= */

    private static void saveSubmittedSlots(PlayerBingoProgress progress, YamlConfiguration config) {
        List<Integer> submittedList = new ArrayList<>(progress.getSubmittedSlots());
        Collections.sort(submittedList);
        config.set("submitted", submittedList);
    }

    private static void saveSubmissionTimes(PlayerBingoProgress progress, YamlConfiguration config) {
        Map<String, Long> timeMap = new HashMap<>();
        for (Map.Entry<Integer, Long> e : progress.getSubmissionTimes().entrySet()) {
            timeMap.put(String.valueOf(e.getKey()), e.getValue());
        }
        config.createSection("submissionTimes", timeMap);
    }

    private static void saveEffectLevels(PlayerBingoProgress progress, YamlConfiguration config) {
        Map<String, Integer> effMap = new HashMap<>();
        for (Map.Entry<BingoEffect, Integer> e : progress.getEffectLevels().entrySet()) {
            effMap.put(e.getKey().name(), e.getValue());
        }
        config.createSection("effects", effMap);
    }

    private static void saveCurrencies(PlayerBingoProgress progress, YamlConfiguration config) {
        Map<String, Integer> currencyMap = new HashMap<>();
        for (BingoRewardType type : BingoRewardType.values()) {
            currencyMap.put(type.name(), progress.getCurrency(type));
        }
        config.createSection("unspentPoints", currencyMap);
    }

    /* ========================= Load Sections ========================= */

    private static void loadSubmittedSlots(YamlConfiguration config, PlayerBingoProgress progress) {
        for (int index : config.getIntegerList("submitted")) {
            progress.submit(index, false);
        }
    }

    private static void loadSubmissionTimes(YamlConfiguration config, PlayerBingoProgress progress) {
        ConfigurationSection section = config.getConfigurationSection("submissionTimes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            int index = Integer.parseInt(key);
            long time = section.getInt(key, 0);
            progress.getSubmissionTimes().put(index, time);
        }
    }

    private static void loadEffectLevels(YamlConfiguration config, PlayerBingoProgress progress) {
        ConfigurationSection section = config.getConfigurationSection("effects");
        if (section == null) return;

        Map<BingoEffect, Integer> levelMap = new EnumMap<>(BingoEffect.class);
        for (String key : section.getKeys(false)) {
            try {
                BingoEffect effect = BingoEffect.valueOf(key);
                int lvl = config.getInt("effects." + key, 0);
                levelMap.put(effect, lvl);
            } catch (IllegalArgumentException ignored) {}
        }
        progress.setEffectLevels(levelMap);
    }

    private static void loadCurrencies(YamlConfiguration config, PlayerBingoProgress progress) {
        ConfigurationSection section = config.getConfigurationSection("unspentPoints");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                BingoRewardType type = BingoRewardType.valueOf(key);
                progress.setCurrency(type, section.getInt(key, 0));
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
