package me.junick.itemBingo.enums;

import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.generator.structure.Structure;

public enum BingoStructure {
    VILLAGE_PLAINS(Structure.VILLAGE_PLAINS, "평원 마을", Material.STRIPPED_OAK_WOOD, Environment.NORMAL),
    VILLAGE_SAVANNA(Structure.VILLAGE_SAVANNA, "사바나 마을", Material.STRIPPED_ACACIA_WOOD, Environment.NORMAL),
    VILLAGE_SNOWY(Structure.VILLAGE_SNOWY, "snowy 마을", Material.STRIPPED_SPRUCE_WOOD, Environment.NORMAL),
    VILLAGE_TAIGA(Structure.VILLAGE_TAIGA, "타이가 마을", Material.SPRUCE_WOOD, Environment.NORMAL),
    VILLAGE_DESERT(Structure.VILLAGE_DESERT, "사막 마을", Material.SANDSTONE, Environment.NORMAL),
    IGLOO(Structure.IGLOO, "이글루", Material.PACKED_ICE, Environment.NORMAL),
    SWAMP_HUT(Structure.SWAMP_HUT, "마녀 오두막", Material.CAULDRON, Environment.NORMAL),
    SHIPWRECK(Structure.SHIPWRECK, "난파선", Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Environment.NORMAL),
    BURIED_TREASURE(Structure.BURIED_TREASURE, "보물", Material.HEART_OF_THE_SEA, Environment.NORMAL),
    STRONGHOLD(Structure.STRONGHOLD, "엔드 유적", Material.ENDER_EYE, Environment.NORMAL),
    DESERT_PYRAMID(Structure.DESERT_PYRAMID, "피라미드", Material.CHISELED_SANDSTONE, Environment.NORMAL),
    ANCIENT_CITY(Structure.ANCIENT_CITY, "고대 도시", Material.SOUL_LANTERN, Environment.NORMAL),
    MONUMENT(Structure.MONUMENT, "바다 신전", Material.PRISMARINE, Environment.NORMAL),
    MINESHAFT(Structure.MINESHAFT, "폐광", Material.COBWEB, Environment.NORMAL),
    JUNGLE_PYRAMID(Structure.JUNGLE_PYRAMID, "정글 사원", Material.MOSSY_COBBLESTONE, Environment.NORMAL),
    RUINED_PORTAL(Structure.RUINED_PORTAL, "무너진 차원문", Material.CRYING_OBSIDIAN, Environment.NORMAL),
    PILLAGER_OUTPOST(Structure.PILLAGER_OUTPOST, "약탈자 전초기지", Material.CROSSBOW, Environment.NORMAL),
    MANSION(Structure.MANSION, "삼림 대저택", Material.TOTEM_OF_UNDYING, Environment.NORMAL),
    TRIAL_CHAMBERS(Structure.TRIAL_CHAMBERS, "인생은 시련의 연속", Material.WAXED_CUT_COPPER, Environment.NORMAL),
    TRAIL_RUINS(Structure.TRAIL_RUINS, "자취 폐허(trail ruins)", Material.HEART_POTTERY_SHERD, Environment.NORMAL),
    OCEAN_RUIN_COLD(Structure.OCEAN_RUIN_COLD, "바다 폐허 (차가움)", Material.SUSPICIOUS_GRAVEL, Environment.NORMAL),
    OCEAN_RUIN_WARM(Structure.OCEAN_RUIN_WARM, "바다 폐허 (따뜻함)", Material.SUSPICIOUS_SAND, Environment.NORMAL),
    RUINED_PORTAL_NETHER(Structure.RUINED_PORTAL_NETHER, "루인드 포탈 (네더)", Material.OBSIDIAN, Environment.NETHER),
    FORTRESS(Structure.FORTRESS, "포트리스", Material.NETHER_BRICKS, Environment.NETHER),
    BASTION_REMNANT(Structure.BASTION_REMNANT, "바스티온", Material.GILDED_BLACKSTONE, Environment.NETHER),
    END_CITY(Structure.END_CITY, "엔드 시티", Material.PURPUR_BLOCK, Environment.THE_END);

    public Structure getStructure() {
        return structure;
    }

    public String getName() {
        return name;
    }

    public Material getIcon() {
        return icon;
    }

    public Environment getDimension() {
        return dimension;
    }

    private final Structure structure;
    private final String name;
    private final Material icon;
    private final Environment dimension;

    BingoStructure(Structure structure, String name, Material icon, Environment dimension) {
        this.structure = structure;
        this.name = name;
        this.icon = icon;
        this.dimension = dimension;
    }
}

