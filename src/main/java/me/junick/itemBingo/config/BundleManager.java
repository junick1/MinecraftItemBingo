package me.junick.itemBingo.config;

import me.junick.itemBingo.ItemBingo;
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

public class BundleManager {
    private static final File file = new File(ItemBingo.getInstance().getDataFolder(), "bundle.yml");
    private static final File player_list = new File(ItemBingo.getInstance().getDataFolder(), "bundle_player_list.yml");
    public static final String TITLE = "§6번들 매니저 - 위 27개가 시작시 기본템으로 주어진다.";

    public static void openStorage(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        var items = loadStroage();
        if (items != null) {
            inv.setContents(items.toArray(new ItemStack[0]));
        }
        player.openInventory(inv);
    }

    public static List<ItemStack> loadStroage() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<ItemStack> items = (List<ItemStack>)config.getList("bundle_list");
        if (items != null) {
            return items;
        }
        return new ArrayList<>();
    }

    public static void saveStorage(Inventory inv) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("bundle_list", inv.getContents());
        try {
            config.save(file);
            Bukkit.getLogger().info("번들을 저장했다.");
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void getBundle(Player p) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(player_list);
        List<String> list = (List<String>)config.getList("players");
        if (list == null) {
            resetPlayerList();
            list = new ArrayList<>();
        }
        if (list.contains(p.getName())) {
            p.sendMessage("§c이미 번들을 받았습니다.");
        }
        else {
            var items = loadStroage();
            if (items.size() > 27) items = items.subList(0, 27);
            items.removeAll(Collections.singleton(null));
            p.getInventory().addItem(items.toArray(new ItemStack[0]));
            list.add(p.getName());
            config.set("players", list);
            try {
                config.save(player_list);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}