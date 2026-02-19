package me.junick.itemBingo;

import org.bukkit.Material;

import java.io.FileWriter;
import java.io.IOException;

public class MaterialExporter {
    public static void exportToFile() {
        try (FileWriter writer = new FileWriter("materials.txt")) {
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

                if (material == Material.POTION) continue;
                if (material == Material.SPLASH_POTION) continue;
                if (material == Material.LINGERING_POTION) continue;
                if (material == Material.TIPPED_ARROW) continue;

                if (material == Material.ENCHANTED_BOOK) continue;

                if (material == Material.SPAWNER) continue;
                if (material == Material.VAULT) continue;

                if (material == Material.PETRIFIED_OAK_SLAB) continue;

                if (material == Material.PLAYER_HEAD) continue;
                if (material == Material.PLAYER_WALL_HEAD) continue;

                if (material == Material.BUDDING_AMETHYST) continue;
                if (material == Material.CHORUS_PLANT) continue;
                if (material == Material.DIRT_PATH) continue;
                if (material == Material.FARMLAND) continue;

                writer.write(material.name() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
