package me.junick.itemBingo.enums;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.interfaces.MapOption;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;

import java.util.Locale;

public enum BingoBiome implements MapOption {
    DESERT(Biome.DESERT, Material.SAND, Environment.NORMAL),
    BADLANDS("is_badlands", Material.RED_SAND, Environment.NORMAL),
    JUNGLE("is_jungle", Material.JUNGLE_LOG, Environment.NORMAL),
    BAMBOO_JUNGLE(Biome.BAMBOO_JUNGLE, Material.BAMBOO_BLOCK, Environment.NORMAL),
    TAIGA("is_taiga", Material.SPRUCE_LOG, Environment.NORMAL),
    SAVANNA("is_savanna", Material.ACACIA_LOG, Environment.NORMAL),
    WARM_OCEAN(Biome.WARM_OCEAN, Material.BUBBLE_CORAL, Environment.NORMAL),
    CHERRY_GROVE(Biome.CHERRY_GROVE, Material.CHERRY_LOG, Environment.NORMAL),
    PALE_GARDEN(Biome.PALE_GARDEN, Material.PALE_OAK_LOG, Environment.NORMAL),
    MANGROVE_SWAMP(Biome.MANGROVE_SWAMP, Material.MANGROVE_LOG, Environment.NORMAL),
    SWAMP(Biome.SWAMP, Material.LILY_PAD, Environment.NORMAL),
    DEEP_DARK(Biome.DEEP_DARK, Material.SCULK, Environment.NORMAL),
    LUSH_CAVES(Biome.LUSH_CAVES, Material.MOSS_BLOCK, Environment.NORMAL),
    DRIPSTONE_CAVES(Biome.DRIPSTONE_CAVES, Material.DRIPSTONE_BLOCK, Environment.NORMAL),
    SULFUR_CAVES(Biome.SULFUR_CAVES, Material.SULFUR, Environment.NORMAL),
    CRIMSON_FOREST(Biome.CRIMSON_FOREST, Material.NETHER_WART_BLOCK, Environment.NETHER),
    WARPED_FOREST(Biome.WARPED_FOREST, Material.WARPED_WART_BLOCK, Environment.NETHER),
    SOUL_SAND_VALLEY(Biome.SOUL_SAND_VALLEY, Material.SOUL_SAND, Environment.NETHER),
    BASALT_DELTAS(Biome.BASALT_DELTAS, Material.BASALT, Environment.NETHER)
    ;

    /**
     * Biomes this option resolves to. A single-biome option returns one element;
     * a tag-backed option (e.g. {@code #minecraft:is_taiga}) resolves the tag
     * against the live biome registry into every member biome. Resolved lazily so
     * the registry is only touched when a map is actually located (main thread).
     */
    public Biome[] getBiomes() {
        if (biome != null) return new Biome[]{ biome };

        Registry<Biome> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
        TagKey<Biome> tagKey = TagKey.create(RegistryKey.BIOME, Key.key("minecraft", biomeTag));
        Tag<Biome> tag = registry.getTag(tagKey);
        return tag.resolve(registry).toArray(new Biome[0]);
    }

    @Override
    public String displayName(SupportedLocale loc) {
        return Messages.legacy(loc, "biome." + name().toLowerCase(Locale.ROOT));
    }

    @Override
    public Material getIcon() {
        return icon;
    }

    @Override
    public Environment getDimension() {
        return dimension;
    }

    private final Biome biome;
    private final String biomeTag;
    private final Material icon;
    private final Environment dimension;

    BingoBiome(Biome biome, Material icon, Environment dimension) {
        this.biome = biome;
        this.biomeTag = null;
        this.icon = icon;
        this.dimension = dimension;
    }

    BingoBiome(String biomeTag, Material icon, Environment dimension) {
        this.biome = null;
        this.biomeTag = biomeTag;
        this.icon = icon;
        this.dimension = dimension;
    }
}
