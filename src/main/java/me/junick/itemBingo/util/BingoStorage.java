package me.junick.itemBingo.util;

import me.junick.itemBingo.model.BingoBoard;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BingoStorage {
    private static final File FILE = new File(Bukkit.getPluginManager().getPlugin("ItemBingo").getDataFolder(), "bingo.yml");

    public static void save(BingoBoard board) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("width", board.getWidth());
        config.set("height", board.getHeight());
        config.set("items", board.getItems());

        try {
            config.save(FILE);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Could not save bingo board to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static BingoBoard load() {
        if (!FILE.exists()) return null;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(FILE);
        int width = config.getInt("width");
        int height = config.getInt("height");

        List<ItemStack> items = (List<ItemStack>) config.getList("items");
        if (items == null) {
            return null;
        }

        return new BingoBoard(width, height, items);
    }
}
