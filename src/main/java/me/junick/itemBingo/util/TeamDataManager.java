package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.enums.BingoEffect;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.model.TeamBingoProgress;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeamDataManager {
    private static final Map<Integer, TeamBingoProgress> cache = new ConcurrentHashMap<>();

    private static final File folder = new File(
            ItemBingo.getInstance().getDataFolder(),
            "team-progress"
    );

    static {
        if (!folder.exists() && !folder.mkdirs()) {
            Bukkit.getLogger().warning("[ItemBingo] Failed to create team data folder.");
        }
    }

    /* ========================= Public API ========================= */

    public static TeamBingoProgress get(int teamId) {
        return cache.computeIfAbsent(teamId, TeamDataManager::load);
    }

    public static Map<Integer, TeamBingoProgress> getAllData() {
        Map<Integer, TeamBingoProgress> all = new HashMap<>();
        for (int teamId : listAllTeamIds()) {
            all.put(teamId, load(teamId));
        }
        return all;
    }

    public static void save(int teamId) {
        TeamBingoProgress progress = cache.get(teamId);
        if (progress != null) save(teamId, progress);
    }

    public static void saveAll() {
        for (int teamId : cache.keySet()) {
            save(teamId);
        }
    }

    public static void resetAll() {
        cache.clear();
        deleteAllFiles();
        Bukkit.getLogger().info("[ItemBingo] All team data has been reset.");
    }

    public static void clearCache() {
        cache.clear();
    }

    public static TeamBingoProgress load(int teamId) {
        File file = getFile(teamId);
        TeamBingoProgress progress = new TeamBingoProgress(teamId);

        if (!file.exists()) return progress;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        loadSubmittedSlots(config, progress);
        loadSubmissionTimes(config, progress);
        loadCurrencies(config, progress);

        return progress;
    }

    /* ========================= Internal Save ========================= */

    private static void save(int teamId, TeamBingoProgress progress) {
        File file = getFile(teamId);
        YamlConfiguration config = new YamlConfiguration();

        saveSubmittedSlots(progress, config);
        saveSubmissionTimes(progress, config);
        saveCurrencies(progress, config);

        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[ItemBingo] Failed to save team data for " + teamId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* ========================= File / Team ID ========================= */

    private static File getFile(int teamId) {
        return new File(folder, "team-" + teamId + ".yml");
    }

    private static List<Integer> listAllTeamIds() {
        List<Integer> ids = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) ->
                name.startsWith("team-") && name.endsWith(".yml"));

        if (files == null) return ids;

        for (File file : files) {
            try {
                String idStr = file.getName()
                        .replace("team-", "")
                        .replace(".yml", "");
                ids.add(Integer.parseInt(idStr));
            } catch (NumberFormatException ignored) {}
        }

        return ids;
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

    private static void saveSubmittedSlots(TeamBingoProgress progress, YamlConfiguration config) {
        List<Integer> submittedList = new ArrayList<>(progress.getSubmittedSlots());
        Collections.sort(submittedList);
        config.set("submitted", submittedList);
    }

    private static void saveSubmissionTimes(TeamBingoProgress progress, YamlConfiguration config) {
        Map<String, Long> timeMap = new HashMap<>();
        for (Map.Entry<Integer, Long> e : progress.getSubmissionTimes().entrySet()) {
            timeMap.put(String.valueOf(e.getKey()), e.getValue());
        }
        config.createSection("submissionTimes", timeMap);
    }

    private static void saveCurrencies(TeamBingoProgress progress, YamlConfiguration config) {
        Map<String, Integer> currencyMap = new HashMap<>();
        for (BingoRewardType type : BingoRewardType.values()) {
            currencyMap.put(type.name(), progress.getCurrency(type));
        }
        config.createSection("currency", currencyMap);
    }

    /* ========================= Load Sections ========================= */

    private static void loadSubmittedSlots(YamlConfiguration config, TeamBingoProgress progress) {
        for (int index : config.getIntegerList("submitted")) {
            progress.submit(index);
        }
    }

    private static void loadSubmissionTimes(YamlConfiguration config, TeamBingoProgress progress) {
        ConfigurationSection section = config.getConfigurationSection("submissionTimes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            int index = Integer.parseInt(key);
            long time = section.getInt(key, 0);
            progress.getSubmissionTimes().put(index, time);
        }
    }

    private static void loadCurrencies(YamlConfiguration config, TeamBingoProgress progress) {
        ConfigurationSection section = config.getConfigurationSection("currency");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                BingoRewardType type = BingoRewardType.valueOf(key);
                progress.addCurrency(type, section.getInt(key, 0));
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
