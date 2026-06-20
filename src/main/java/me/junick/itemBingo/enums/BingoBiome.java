package me.junick.itemBingo.enums;

import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;

public enum BingoBiome {
    DESERT(Biome.DESERT, "사막", Material.SAND, Environment.NORMAL),
    BADLANDS(Biome.BADLANDS, "악지(메사)", Material.RED_SAND, Environment.NORMAL),
    JUNGLE(Biome.JUNGLE, "정글", Material.JUNGLE_LOG, Environment.NORMAL),
    BAMBOO_JUNGLE(Biome.BAMBOO_JUNGLE, "대나무 정글", Material.BAMBOO_BLOCK, Environment.NORMAL),
    WARM_OCEAN(Biome.WARM_OCEAN, "따뜻한 바다", Material.BUBBLE_CORAL, Environment.NORMAL),
    CHERRY_GROVE(Biome.CHERRY_GROVE, "체리", Material.CHERRY_LOG, Environment.NORMAL),
    PALE_GARDEN(Biome.PALE_GARDEN, "페일 오크", Material.PALE_OAK_LOG, Environment.NORMAL),
    MANGROVE_SWAMP(Biome.MANGROVE_SWAMP, "맹그로브 늪", Material.MANGROVE_LOG, Environment.NORMAL),
    SWAMP(Biome.SWAMP, "늪", Material.LILY_PAD, Environment.NORMAL),
    DEEP_DARK(Biome.DEEP_DARK, "딥 다크", Material.SCULK, Environment.NORMAL),
    LUSH_CAVES(Biome.LUSH_CAVES, "무성한 동굴", Material.MOSS_BLOCK, Environment.NORMAL),
    DRIPSTONE_CAVES(Biome.DRIPSTONE_CAVES, "점적석 동굴", Material.DRIPSTONE_BLOCK, Environment.NORMAL),
    SULFUR_CAVES(Biome.SULFUR_CAVES, "유황 동굴", Material.SULFUR, Environment.NORMAL);


    public Biome getBiome() { return biome; }

    public String getName() {
        return name;
    }

    public Material getIcon() {
        return icon;
    }

    public Environment getDimension() {
        return dimension;
    }

    private final Biome biome;
    private final String name;
    private final Material icon;
    private final Environment dimension;

    BingoBiome(Biome biome, String name, Material icon, Environment dimension) {
        this.biome = biome;
        this.name = name;
        this.icon = icon;
        this.dimension = dimension;
    }
}

