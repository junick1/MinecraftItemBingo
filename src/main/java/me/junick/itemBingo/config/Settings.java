package me.junick.itemBingo.config;

import me.junick.itemBingo.ItemBingo;
import org.bukkit.configuration.file.FileConfiguration;

public class Settings {
    private final ItemBingo plugin;

    public Settings(ItemBingo plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration c() {
        return plugin.getConfig();
    }

    public boolean isTeamEnabled() { return c().getBoolean("team.enabled", false); }
    public boolean isShopEnabled() { return c().getBoolean("shop.enabled", false); }
    public boolean isEffectShopEnabled() { return c().getBoolean("shop.effectShopEnabled", false); }
    public boolean isItemShopEnabled() { return c().getBoolean("shop.itemShopEnabled", false); }
    public boolean isShovelOxidizeCopper() { return c().getBoolean("vanilla.shovelOxidizeCopper", false); }

    public void toggleTeam() {
        c().set("team.enabled", !isTeamEnabled());
        plugin.saveConfig();
    }

    public void toggleShop() {
        boolean next = !isShopEnabled();
        c().set("shop.enabled", next);

        plugin.saveConfig();
    }

    public void toggleEffectShop() {
        c().set("shop.effectShopEnabled", !isEffectShopEnabled());
        plugin.saveConfig();
    }

    public void toggleItemShop() {
        c().set("shop.itemShopEnabled", !isItemShopEnabled());
        plugin.saveConfig();
    }

    public void toggleShovelOxidizeCopper() {
        c().set("vanilla.shovelOxidizeCopper", !isShovelOxidizeCopper());
        plugin.saveConfig();
    }
}
