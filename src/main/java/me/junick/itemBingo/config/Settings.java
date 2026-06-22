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

    /**
     * The three game modes are mutually exclusive — only one is active at a time.
     * Each mode has its own set of sub-settings (the rows below the mode switch in
     * the admin "모드" tab).
     */
    public enum GameMode {
        NORMAL("일반"),
        SWAPPAGE("Swappage"),
        FOG_OF_WAR("Fog of War"),
        LOCKOUT("Lockout");

        private final String display;

        GameMode(String display) { this.display = display; }

        public String getDisplay() { return display; }
    }

    private static GameMode gameMode;

    // Swappage sub-settings
    private static boolean swapAlert;
    private static boolean swapTimer;

    // Fog of War sub-settings
    private static boolean fogSubmitLock;
    private static boolean fogRevealAlert;
    private static boolean fogDiagonalReveal;

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

        // Mode: prefer the new "mode.type" key, falling back to the legacy
        // "mode.positionSwap" boolean so existing configs keep working.
        gameMode = parseMode(config.getString("mode.type", null),
                config.getBoolean("mode.positionSwap", false));

        swapAlert = config.getBoolean("mode.swap.alert", config.getBoolean("mode.positionSwap.alert", false));
        swapTimer = config.getBoolean("mode.swap.timer", config.getBoolean("mode.positionSwap.timer", false));

        fogSubmitLock = config.getBoolean("mode.fog.submitLock", true);
        fogRevealAlert = config.getBoolean("mode.fog.revealAlert", true);
        fogDiagonalReveal = config.getBoolean("mode.fog.diagonalReveal", false);

        plugin.getLogger().info("모든 설정이 로드되었습니다");
    }

    private static GameMode parseMode(String type, boolean legacyPositionSwap) {
        if (type != null) {
            try {
                return GameMode.valueOf(type);
            } catch (IllegalArgumentException ignored) {
                // fall through to legacy / default
            }
        }
        return legacyPositionSwap ? GameMode.SWAPPAGE : GameMode.NORMAL;
    }

    public static void save(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();

        config.set("team.enabled", teamEnabled);
        config.set("shop.enabled", shopEnabled);
        config.set("shop.effectShopEnabled", effectShopEnabled);
        config.set("shop.itemShopEnabled", itemShopEnabled);
        config.set("vanilla.shovelOxidizeCopper", shovelOxidizeCopper);
        config.set("game.penalty", penaltySystem);

        config.set("mode.type", gameMode.name());
        config.set("mode.swap.alert", swapAlert);
        config.set("mode.swap.timer", swapTimer);
        config.set("mode.fog.submitLock", fogSubmitLock);
        config.set("mode.fog.revealAlert", fogRevealAlert);
        config.set("mode.fog.diagonalReveal", fogDiagonalReveal);

        // Drop the legacy keys now that they're superseded by mode.type / mode.swap.*
        config.set("mode.positionSwap", null);
        config.set("mode.positionSwap.alert", null);
        config.set("mode.positionSwap.timer", null);

        plugin.saveConfig();
        plugin.getLogger().info("설정이  저장되었습니다");
    }

    public static boolean isTeamEnabled() { return teamEnabled; }
    public static boolean isShopEnabled() { return shopEnabled; }
    public static boolean isEffectShopEnabled() { return effectShopEnabled; }
    public static boolean isItemShopEnabled() { return itemShopEnabled; }
    public static boolean isShovelOxidizeCopper() { return shovelOxidizeCopper; }
    public static int getPenaltyInt() { return penaltySystem; }

    // ===== Game mode =====
    public static GameMode getGameMode() { return gameMode; }
    public static boolean isPositionSwapMode() { return gameMode == GameMode.SWAPPAGE; }
    public static boolean isFogOfWarMode() { return gameMode == GameMode.FOG_OF_WAR; }
    public static boolean isLockoutMode() { return gameMode == GameMode.LOCKOUT; }

    /** Cycle Normal → Swappage → Fog of War → Lockout → Normal. */
    public static void cycleGameMode() {
        gameMode = switch (gameMode) {
            case NORMAL -> GameMode.SWAPPAGE;
            case SWAPPAGE -> GameMode.FOG_OF_WAR;
            case FOG_OF_WAR -> GameMode.LOCKOUT;
            case LOCKOUT -> GameMode.NORMAL;
        };
    }

    // ===== Swappage sub-settings =====
    public static boolean isSwapAlert() { return swapAlert; }
    public static boolean isSwapTimer() { return swapTimer; }
    public static void toggleSwapAlert() { swapAlert = !swapAlert; }
    public static void toggleSwapTimer() { swapTimer = !swapTimer; }

    // ===== Fog of War sub-settings =====
    public static boolean isFogSubmitLock() { return fogSubmitLock; }
    public static boolean isFogRevealAlert() { return fogRevealAlert; }
    public static boolean isFogDiagonalReveal() { return fogDiagonalReveal; }
    public static void toggleFogSubmitLock() { fogSubmitLock = !fogSubmitLock; }
    public static void toggleFogRevealAlert() { fogRevealAlert = !fogRevealAlert; }
    public static void toggleFogDiagonalReveal() { fogDiagonalReveal = !fogDiagonalReveal; }

    public static void toggleTeamEnabled() { teamEnabled = !teamEnabled; }
    public static void toggleShopEnabled() { shopEnabled = !shopEnabled; }
    public static void toggleEffectShopEnabled() { effectShopEnabled = !effectShopEnabled; }
    public static void toggleItemShopEnabled() { itemShopEnabled = !itemShopEnabled; }
    public static void toggleShovelOxidizeCopper() { shovelOxidizeCopper = !shovelOxidizeCopper; }
    public static void togglePenaltyInt() { penaltySystem = (penaltySystem + 1) % 3; }

    public enum Penalty {
        TOTAL_SUBMISSION("제출 합"),
        LAST_SUBMISSION("마지막 제출"),
        CODEFORCES("점수제");

        private final String display;

        Penalty(String display) { this.display = display; }

        public String getDisplay() { return display; }
    }

    public static Penalty getPenaltySystem() {
        return switch (getPenaltyInt()) {
            case 0 -> Penalty.TOTAL_SUBMISSION;
            case 1 -> Penalty.LAST_SUBMISSION;
            case 2 -> Penalty.CODEFORCES;
            default -> throw new IllegalStateException("Unexpected value: " + getPenaltyInt());
        };
    }

    /** Korean label for the currently selected penalty mode (for GUI display). */
    public static String getPenaltyDisplay() {
        return getPenaltySystem().getDisplay();
    }

}
