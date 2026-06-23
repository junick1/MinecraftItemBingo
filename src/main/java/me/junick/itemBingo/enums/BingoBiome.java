package me.junick.itemBingo.enums;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import me.junick.itemBingo.interfaces.MapOption;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;

public enum BingoBiome implements MapOption {
    DESERT(Biome.DESERT, "사막", Material.SAND, Environment.NORMAL),
    BADLANDS("is_badlands", "악지", Material.RED_SAND, Environment.NORMAL),
    JUNGLE("is_jungle", "정글", Material.JUNGLE_LOG, Environment.NORMAL),
    BAMBOO_JUNGLE(Biome.BAMBOO_JUNGLE, "대나무 정글", Material.BAMBOO_BLOCK, Environment.NORMAL),
    TAIGA("is_taiga", "타이가", Material.SPRUCE_LOG, Environment.NORMAL),
    SAVANNA("is_savanna", "사바나", Material.ACACIA_LOG, Environment.NORMAL),
    WARM_OCEAN(Biome.WARM_OCEAN, "따뜻한 바다", Material.BUBBLE_CORAL, Environment.NORMAL),
    CHERRY_GROVE(Biome.CHERRY_GROVE, "벚나무 숲", Material.CHERRY_LOG, Environment.NORMAL),
    PALE_GARDEN(Biome.PALE_GARDEN, "창백한 정원", Material.PALE_OAK_LOG, Environment.NORMAL),
    MANGROVE_SWAMP(Biome.MANGROVE_SWAMP, "맹그로브 늪", Material.MANGROVE_LOG, Environment.NORMAL),
    SWAMP(Biome.SWAMP, "늪", Material.LILY_PAD, Environment.NORMAL),
    DEEP_DARK(Biome.DEEP_DARK, "깊은 어둠", Material.SCULK, Environment.NORMAL),
    LUSH_CAVES(Biome.LUSH_CAVES, "무성한 동굴", Material.MOSS_BLOCK, Environment.NORMAL),
    DRIPSTONE_CAVES(Biome.DRIPSTONE_CAVES, "점적석 동굴", Material.DRIPSTONE_BLOCK, Environment.NORMAL),
    SULFUR_CAVES(Biome.SULFUR_CAVES, "유황 동굴", Material.SULFUR, Environment.NORMAL),
    CRIMSON_FOREST(Biome.CRIMSON_FOREST, "진홍빛 숲", Material.NETHER_WART_BLOCK, Environment.NETHER),
    WARPED_FOREST(Biome.WARPED_FOREST, "뒤틀린 숲", Material.WARPED_WART_BLOCK, Environment.NETHER),
    SOUL_SAND_VALLEY(Biome.SOUL_SAND_VALLEY, "영혼 모래 골짜기", Material.SOUL_SAND, Environment.NETHER),
    BASALT_DELTAS(Biome.BASALT_DELTAS, "현무암 삼각주", Material.BASALT, Environment.NETHER)
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
    private final String biomeTag;
    private final String name;
    private final Material icon;
    private final Environment dimension;

    BingoBiome(Biome biome, String name, Material icon, Environment dimension) {
        this.biome = biome;
        this.biomeTag = null;
        this.name = name;
        this.icon = icon;
        this.dimension = dimension;
    }

    BingoBiome(String biomeTag, String name, Material icon, Environment dimension) {
        this.biome = null;
        this.biomeTag = biomeTag;
        this.name = name;
        this.icon = icon;
        this.dimension = dimension;
    }
}
