package me.junick.itemBingo.enums;

import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import org.bukkit.Material;

import java.util.Locale;

public enum BingoEffect {
    SPEED(5, 1, Material.LEATHER_BOOTS),
    DOLPHINS_GRACE(3, 1, Material.DOLPHIN_SPAWN_EGG),
    HASTE(10, 2, Material.GOLDEN_PICKAXE),
    CONDUIT_POWER(1, 1, Material.HEART_OF_THE_SEA),
    STEP_HEIGHT(3, 1, Material.FEATHER),
    EFFICIENCY(10, 1, Material.DIAMOND_PICKAXE),
    MENDING(1, 1, Material.EMERALD_BLOCK),
    STRENGTH(4, 2, Material.IRON_SWORD),
    RESISTANCE(2, 2, Material.SHIELD),
    HEALTH_BOOST(4, 5, Material.ENCHANTED_GOLDEN_APPLE),
    FIRE_RESISTANCE(1, 1, Material.FIRE_CHARGE),
    IMPROVE_LAVA_MOVEMENT(1, 1, Material.STRIDER_SPAWN_EGG);

    private final int maxLevel;
    private final int increment;
    private final Material icon;

    BingoEffect(int maxLevel, int increment, Material icon) {
        this.maxLevel = maxLevel;
        this.increment = increment;
        this.icon = icon;
    }

    /** Message-key stem, e.g. {@code speed} → {@code effect.speed.name}. */
    public String key() { return name().toLowerCase(Locale.ROOT); }

    /** Localized display name (legacy §). */
    public String displayName(SupportedLocale loc) { return Messages.legacy(loc, "effect." + key() + ".name"); }

    public int getMaxLevel() { return maxLevel; }
    public int getIncrement() { return increment; }
    public Material getIcon() { return icon; }

    public int getTotalLevel(int level) {
        return increment * Math.min(level, maxLevel);
    }
}
