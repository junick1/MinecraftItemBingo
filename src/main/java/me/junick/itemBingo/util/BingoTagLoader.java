package me.junick.itemBingo.util;

import me.junick.itemBingo.enums.BingoItemTag;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class BingoTagLoader {
    private final Map<Material, EnumSet<BingoItemTag>> tagMap = new EnumMap<>(Material.class);

    public BingoTagLoader(JavaPlugin plugin) {
        load(plugin);
    }

    private void load(JavaPlugin plugin) {
        InputStream stream = plugin.getResource("tags.yml");
        if (stream == null) {
            throw new IllegalStateException("tags.yml not found in plugin resources");
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        );

        ConfigurationSection root = config.getConfigurationSection("tags");
        if (root == null) return;

        for (String tagKey : root.getKeys(false)) {
            BingoItemTag tag;
            try {
                tag = BingoItemTag.valueOf(tagKey);
            } catch (IllegalArgumentException e) {
                continue;
            }

            ConfigurationSection section = root.getConfigurationSection(tagKey);
            if (section == null) continue;

            for (String matName : section.getStringList("materials")) {
                Material mat = Material.matchMaterial(matName);
                if (mat != null) {
                    addTag(mat, tag);
                }
            }

            for (String pattern : section.getStringList("match")) {
                Pattern regex = toRegex(pattern);

                for (Material mat : Material.values()) {
                    if (!mat.isItem()) continue;

                    if (regex.matcher(mat.name()).matches()) {
                        addTag(mat, tag);
                    }
                }
            }
        }
    }

    private void addTag(Material material, BingoItemTag tag) {
        tagMap
                .computeIfAbsent(material, m -> EnumSet.noneOf(BingoItemTag.class))
                .add(tag);
    }

    private Pattern toRegex(String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");
        return Pattern.compile(regex);
    }

    public EnumSet<BingoItemTag> getTags(Material material) {
        return tagMap.getOrDefault(material, EnumSet.noneOf(BingoItemTag.class));
    }
}
