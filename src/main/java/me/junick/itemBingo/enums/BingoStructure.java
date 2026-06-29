package me.junick.itemBingo.enums;

import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.interfaces.MapOption;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.generator.structure.Structure;

import java.util.Locale;

public enum BingoStructure implements MapOption {
    VILLAGE_PLAINS(Structure.VILLAGE_PLAINS, Material.STRIPPED_OAK_WOOD, Environment.NORMAL),
    VILLAGE_SAVANNA(Structure.VILLAGE_SAVANNA, Material.STRIPPED_ACACIA_WOOD, Environment.NORMAL),
    VILLAGE_SNOWY(Structure.VILLAGE_SNOWY, Material.STRIPPED_SPRUCE_WOOD, Environment.NORMAL),
    VILLAGE_TAIGA(Structure.VILLAGE_TAIGA, Material.SPRUCE_WOOD, Environment.NORMAL),
    VILLAGE_DESERT(Structure.VILLAGE_DESERT, Material.SANDSTONE, Environment.NORMAL),
    IGLOO(Structure.IGLOO, Material.PACKED_ICE, Environment.NORMAL),
    SWAMP_HUT(Structure.SWAMP_HUT, Material.CAULDRON, Environment.NORMAL),
    SHIPWRECK(Structure.SHIPWRECK, Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Environment.NORMAL),
    BURIED_TREASURE(Structure.BURIED_TREASURE, Material.HEART_OF_THE_SEA, Environment.NORMAL),
    STRONGHOLD(Structure.STRONGHOLD, Material.ENDER_EYE, Environment.NORMAL),
    DESERT_PYRAMID(Structure.DESERT_PYRAMID, Material.CHISELED_SANDSTONE, Environment.NORMAL),
    ANCIENT_CITY(Structure.ANCIENT_CITY, Material.SOUL_LANTERN, Environment.NORMAL),
    MONUMENT(Structure.MONUMENT, Material.PRISMARINE, Environment.NORMAL),
    MINESHAFT(Structure.MINESHAFT, Material.COBWEB, Environment.NORMAL),
    JUNGLE_PYRAMID(Structure.JUNGLE_PYRAMID, Material.MOSSY_COBBLESTONE, Environment.NORMAL),
    RUINED_PORTAL(Structure.RUINED_PORTAL, Material.CRYING_OBSIDIAN, Environment.NORMAL),
    PILLAGER_OUTPOST(Structure.PILLAGER_OUTPOST, Material.CROSSBOW, Environment.NORMAL),
    MANSION(Structure.MANSION, Material.TOTEM_OF_UNDYING, Environment.NORMAL),
    TRIAL_CHAMBERS(Structure.TRIAL_CHAMBERS, Material.WAXED_CUT_COPPER, Environment.NORMAL),
    TRAIL_RUINS(Structure.TRAIL_RUINS, Material.HEART_POTTERY_SHERD, Environment.NORMAL),
    OCEAN_RUIN_COLD(Structure.OCEAN_RUIN_COLD, Material.SUSPICIOUS_GRAVEL, Environment.NORMAL),
    OCEAN_RUIN_WARM(Structure.OCEAN_RUIN_WARM, Material.SUSPICIOUS_SAND, Environment.NORMAL),
    RUINED_PORTAL_NETHER(Structure.RUINED_PORTAL_NETHER, Material.OBSIDIAN, Environment.NETHER),
    FORTRESS(Structure.FORTRESS, Material.NETHER_BRICKS, Environment.NETHER),
    BASTION_REMNANT(Structure.BASTION_REMNANT, Material.GILDED_BLACKSTONE, Environment.NETHER),
    END_CITY(Structure.END_CITY, Material.PURPUR_BLOCK, Environment.THE_END);

    public Structure getStructure() {
        return structure;
    }

    @Override
    public String displayName(SupportedLocale loc) {
        return Messages.legacy(loc, "structure." + name().toLowerCase(Locale.ROOT));
    }

    @Override
    public Material getIcon() {
        return icon;
    }

    @Override
    public Environment getDimension() {
        return dimension;
    }

    private final Structure structure;
    private final Material icon;
    private final Environment dimension;

    BingoStructure(Structure structure, Material icon, Environment dimension) {
        this.structure = structure;
        this.icon = icon;
        this.dimension = dimension;
    }
}
