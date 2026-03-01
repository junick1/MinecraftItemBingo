package me.junick.itemBingo.enums;

import org.bukkit.Material;

public enum BingoEffect {
    SPEED("신속", 3, 1, Material.LEATHER_BOOTS),
    DOLPHINS_GRACE("돌고래의 가호", 2, 1, Material.DOLPHIN_SPAWN_EGG),
    HASTE("성급함", 5, 2, Material.GOLDEN_PICKAXE),
    CONDUIT_POWER("수중 호흡", 1, 1, Material.HEART_OF_THE_SEA),
    STEP_HEIGHT("스텝 높이", 3, 1, Material.FEATHER),
    EFFICIENCY("효율", 5, 1, Material.DIAMOND_PICKAXE),
    MENDING("수선", 1, 1, Material.EMERALD_BLOCK),
    STRENGTH("힘", 3, 1, Material.IRON_SWORD),
    RESISTANCE("저항", 3, 1, Material.SHIELD),
    HEALTH_BOOST("체력 증가", 3, 2, Material.ENCHANTED_GOLDEN_APPLE),
    FIRE_RESISTANCE("화염 저항", 1, 1, Material.FIRE_CHARGE);

    private final String display;
    private final int maxLevel;
    private final int increment;
    private final Material icon;

    BingoEffect(String display, int maxLevel, int increment, Material icon) {
        this.display = display;
        this.maxLevel = maxLevel;
        this.increment = increment;
        this.icon = icon;
    }

    public String getDisplay() { return display; }
    public int getMaxLevel() { return maxLevel; }
    public int getIncrement() { return increment; }
    public Material getIcon() { return icon; }

    public int getTotalLevel(int level) {
        return increment * Math.min(level, maxLevel);
    }
}
