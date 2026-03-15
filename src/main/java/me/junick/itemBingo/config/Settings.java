package me.junick.itemBingo.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Settings {
    private static boolean teamEnabled;
    private static boolean shopEnabled;
    private static boolean effectShopEnabled;
    private static boolean itemShopEnabled;
    private static boolean shovelOxidizeCopper;
    private static int penaltySystem;

    private static boolean positionSwapMode;
    private static boolean swapAlert;
    private static boolean swapTimer;

    public static void load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        teamEnabled = config.getBoolean("team.enabled", false);
        shopEnabled = config.getBoolean("shop.enabled", false);
        effectShopEnabled = config.getBoolean("shop.effectShopEnabled", false);
        itemShopEnabled = config.getBoolean("shop.itemShopEnabled", false);
        shovelOxidizeCopper = config.getBoolean("vanilla.shovelOxidizeCopper", false);
        penaltySystem = config.getInt("game.penalty", 0);
        positionSwapMode = config.getBoolean("mode.positionSwap", false);
        swapAlert = config.getBoolean("mode.positionSwap.alert", false);
        swapTimer = config.getBoolean("mode.positionSwap.timer", false);

        plugin.getLogger().info("모든 설정이 로드되었습니다");
    }

    public static void save(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();

        config.set("team.enabled", teamEnabled);
        config.set("shop.enabled", shopEnabled);
        config.set("shop.effectShopEnabled", effectShopEnabled);
        config.set("shop.itemShopEnabled", itemShopEnabled);
        config.set("vanilla.shovelOxidizeCopper", shovelOxidizeCopper);
        config.set("game.penalty", penaltySystem);
        config.set("mode.positionSwap", positionSwapMode);
        config.set("mode.positionSwap.alert", swapAlert);
        config.set("mode.positionSwap.timer", swapTimer);

        plugin.saveConfig();
        plugin.getLogger().info("설정이  저장되었습니다");
    }

    public static boolean isTeamEnabled() { return teamEnabled; }
    public static boolean isShopEnabled() { return shopEnabled; }
    public static boolean isEffectShopEnabled() { return effectShopEnabled; }
    public static boolean isItemShopEnabled() { return itemShopEnabled; }
    public static boolean isShovelOxidizeCopper() { return shovelOxidizeCopper; }
    public static int getPenaltyInt() { return penaltySystem; }
    public static boolean isPositionSwapMode() { return positionSwapMode; }
    public static boolean isSwapAlert() { return swapAlert; }
    public static boolean isSwapTimer() { return swapTimer; }

    public static void toggleTeamEnabled() { teamEnabled = !teamEnabled; }
    public static void toggleShopEnabled() { shopEnabled = !shopEnabled; }
    public static void toggleEffectShopEnabled() { effectShopEnabled = !effectShopEnabled; }
    public static void toggleItemShopEnabled() { itemShopEnabled = !itemShopEnabled; }
    public static void toggleShovelOxidizeCopper() { shovelOxidizeCopper = !shovelOxidizeCopper; }
    public static void togglePenaltyInt() { penaltySystem = (penaltySystem + 1) % 3; }
    public static void togglePositionSwapMode() { positionSwapMode = !positionSwapMode; }
    public static void toggleSwapAlert() { swapAlert = !swapAlert; }
    public static void toggleSwapTimer() { swapTimer = !swapTimer; }

    public enum Penalty {
        TOTAL_SUBMISSION, LAST_SUBMISSION, CODEFORCES
    }

    public static Penalty getPenaltySystem() {
        return switch (getPenaltyInt()) {
            case 0 -> Penalty.TOTAL_SUBMISSION;
            case 1 -> Penalty.LAST_SUBMISSION;
            case 2 -> Penalty.CODEFORCES;
            default -> throw new IllegalStateException("Unexpected value: " + getPenaltyInt());
        };
    }

}
