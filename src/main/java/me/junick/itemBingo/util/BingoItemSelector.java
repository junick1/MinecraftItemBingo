package me.junick.itemBingo.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.*;

public class BingoItemSelector {
    private static final List<ItemStack> CANDIDATE_ITEMS = new ArrayList<>();

    static {
        for (Material material : Material.values()) {
            if (!material.isItem()) continue;
            if (material.isAir()) continue;

            if (material.name().contains("COMMAND_BLOCK")) continue;
            if (material.name().contains("SPAWN_EGG")) continue;
            if (material.name().contains("STRUCTURE_")) continue;
            if (material.name().contains("INFESTED_")) continue;

//            if (material == Material.SMITHING_TEMP)

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

            if (material == Material.POTION) continue;
            if (material == Material.SPLASH_POTION) continue;
            if (material == Material.LINGERING_POTION) continue;

            if (material == Material.ENCHANTED_BOOK) continue;

            if (material == Material.SPAWNER) continue;
            if (material == Material.VAULT) continue;
            if (material == Material.TRIAL_SPAWNER) continue;

            if (material == Material.PETRIFIED_OAK_SLAB) continue;

            if (material == Material.PLAYER_HEAD) continue;
            if (material == Material.PLAYER_WALL_HEAD) continue;

            if (material == Material.BUDDING_AMETHYST) continue;
            if (material == Material.CHORUS_PLANT) continue;
            if (material == Material.DIRT_PATH) continue;
            if (material == Material.FARMLAND) continue;

            CANDIDATE_ITEMS.add(new ItemStack(material));
        }
    }

    public static ItemStack getRandomSurvivalItem() {
        return getRandomSurvivalItems(1).get(0);
    }

    public static List<ItemStack> getRandomSurvivalItems(int count) {
        List<ItemStack> pool = new ArrayList<>(CANDIDATE_ITEMS);
        Collections.shuffle(pool);
        return pool.subList(0, Math.min(count, pool.size()));
    }
}
