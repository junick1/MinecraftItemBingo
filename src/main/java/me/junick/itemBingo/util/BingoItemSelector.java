package me.junick.itemBingo.util;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.enums.BingoItemTag;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.*;


class WeightedCollection<E> {
    private NavigableMap<Long, E> map = new TreeMap<>();
    private Random random;
    private long total = 0;

    public WeightedCollection() {
        this(new Random());
    }

    public WeightedCollection(Random random) {
        this.random = random;
    }

    public void add(int weight, E object) {
        if (weight <= 0) return;
        total += weight;
        map.put(total, object);
    }

    public E next() {
        long value = random.nextLong(total) + 1; // Can we use floating-point weights?
        E e = map.ceilingEntry(value).getValue();
        return e;
    }
}

public class BingoItemSelector {
    private static final List<ItemStack> CANDIDATE_ITEMS = new ArrayList<>();
    private static final WeightedCollection<ItemStack> WEIGHTED_COLLECTION = new WeightedCollection<>();

    static {
        for (Material material : Material.values()) {
            if (!material.isItem()) continue;
            if (material.isAir()) continue;

            if (material.name().contains("COMMAND_BLOCK")) continue;
            if (material.name().contains("SPAWN_EGG")) continue;
            if (material.name().contains("STRUCTURE_")) continue;
            if (material.name().contains("INFESTED_")) continue;

            if (material == Material.BARRIER) continue;
            if (material == Material.DEBUG_STICK) continue;
            if (material == Material.KNOWLEDGE_BOOK) continue;
            if (material == Material.JIGSAW) continue;
            if (material == Material.LIGHT) continue;
            if (material == Material.TEST_BLOCK) continue;
            if (material == Material.TEST_INSTANCE_BLOCK) continue;

            if (material == Material.BEDROCK) continue;
            if (material == Material.END_PORTAL_FRAME) continue;

            if (material == Material.REINFORCED_DEEPSLATE) continue;
            if (material == Material.SPAWNER) continue;
            if (material == Material.VAULT) continue;
            if (material == Material.TRIAL_SPAWNER) continue;

            if (material == Material.PETRIFIED_OAK_SLAB) continue;

            if (material == Material.PLAYER_HEAD) continue;

            if (material == Material.BUDDING_AMETHYST) continue;
            if (material == Material.CHORUS_PLANT) continue;
            if (material == Material.DIRT_PATH) continue;
            if (material == Material.FARMLAND) continue;
            if (material == Material.SUSPICIOUS_GRAVEL) continue;
            if (material == Material.SUSPICIOUS_SAND) continue;

            CANDIDATE_ITEMS.add(new ItemStack(material));
        }
    }

    public static ItemStack getRandomSurvivalItem() {
        return getRandomSurvivalItems(1).get(0);
    }

    public static List<ItemStack> getWeightedRandomSurvivalItems(int count) {
        ArrayList<ItemStack> pool = new ArrayList<>();
        TreeSet<Material> reject = new TreeSet<>();
        while (pool.size() < count) {
            ItemStack item = WEIGHTED_COLLECTION.next();
            if (reject.contains(item.getType())) continue;
            reject.add(item.getType());
            pool.add(item);
        }
        return pool;
    }

    public static List<ItemStack> getRandomSurvivalItems(int count) {
        List<ItemStack> pool = new ArrayList<>(CANDIDATE_ITEMS);
        Collections.shuffle(pool);
        return pool.subList(0, Math.min(count, pool.size()));
    }

    static {
        for (Material material : Material.values()) {
            if (!material.isItem() || material.isAir() || material.name().contains("COMMAND_BLOCK") || material.name().contains("SPAWN_EGG") || material.name().contains("STRUCTURE_") || material.name().contains("INFESTED_") || material == Material.BARRIER || material == Material.DEBUG_STICK || material == Material.KNOWLEDGE_BOOK || material == Material.JIGSAW || material == Material.LIGHT || material == Material.TEST_BLOCK || material == Material.TEST_INSTANCE_BLOCK || material == Material.BEDROCK || material == Material.END_PORTAL_FRAME || material == Material.REINFORCED_DEEPSLATE || material == Material.SPAWNER || material == Material.VAULT || material == Material.TRIAL_SPAWNER || material == Material.PETRIFIED_OAK_SLAB || material == Material.PLAYER_HEAD || material == Material.PLAYER_WALL_HEAD || material == Material.BUDDING_AMETHYST || material == Material.CHORUS_PLANT || material == Material.DIRT_PATH || material == Material.FARMLAND || material == Material.FROGSPAWN || material == Material.SUSPICIOUS_GRAVEL || material == Material.SUSPICIOUS_SAND) continue;
            CANDIDATE_ITEMS.add(new ItemStack(material));
            EnumSet<BingoItemTag> tags = ItemBingo.getInstance().getTagLoader().getTags(new ItemStack(material).getType());
            int weight = 200;
            if (!tags.isEmpty()) {
                for (BingoItemTag tag : tags) {
                    switch(tag) {
                        case COLOR -> weight -= 0;
                        case SILK_TOUCH -> weight -= 0;
                        case WAXED -> weight -= 100;
                        case DISC -> weight -= 0;
                        case COPPER -> weight -= 0;
                        case THE_END -> weight -= 200;
                        case THE_NETHER -> weight -= 0;
                        case POTTERY_SHERD -> weight -= 0;
                        case SMITHING_TEMPLATE -> weight -= 0;
                    }
                }
            }
            if (weight < 0) weight = 0;
            WEIGHTED_COLLECTION.add(weight, new ItemStack(material));
        }
    }
}
