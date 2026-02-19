package me.junick.itemBingo.events.items;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class ShovelOxidizeEvent implements Listener {
    @EventHandler
    public void onUseShovel(PlayerInteractEvent e) {
        if (!new Settings(ItemBingo.getInstance()).isShovelOxidizeCopper()) return;

        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!e.getAction().isRightClick()) return;

        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isShovel(hand)) return;

        Block block = e.getClickedBlock();
        if (block == null) return;

        Material current = block.getType();
        Material next = getNextOxidation(current);
        if (next == null) return;

        e.setCancelled(true);

        String oldDataStr = block.getBlockData().getAsString();
        String newDataStr = oldDataStr.replace(
                current.getKey().toString(),
                next.getKey().toString()
        );

        block.setType(next);

        try {
            player.getClass().getMethod("swingHand", EquipmentSlot.class).invoke(player, EquipmentSlot.HAND);
        } catch (Throwable ignored) { }


        try {
            BlockData newData = Bukkit.createBlockData(newDataStr);
            block.setBlockData(newData, false);

            damageShovelIfNeeded(player, hand);

            player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_HURT, 1.0f, 1.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.1f, 0.5f);
        } catch (IllegalArgumentException ignored) { }
    }

    private boolean isShovel(ItemStack item) {
        if (item == null) return false;
        return switch (item.getType()) {
            case WOODEN_SHOVEL, STONE_SHOVEL, COPPER_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL -> true;
            default -> false;
        };
    }

    private void damageShovelIfNeeded(Player player, ItemStack tool) {
        if (tool == null || tool.getType().isAir()) return;

        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return;
        if (meta.isUnbreakable()) return;

        if (!(meta instanceof Damageable dmg)) return;

        // TODO: Add unbreaking logic

        int currentDamage = dmg.getDamage();
        int max = tool.getType().getMaxDurability();
        int nextDamage = currentDamage + 1;

        if (nextDamage >= max) {
            player.getInventory().setItemInMainHand(null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            return;
        }

        dmg.setDamage(nextDamage);
        tool.setItemMeta((ItemMeta) dmg);
    }

    private Material getNextOxidation(Material type) {
        Material next = null;
        switch (type) {
            case COPPER_BLOCK -> next = Material.EXPOSED_COPPER;
            case EXPOSED_COPPER -> next = Material.WEATHERED_COPPER;
            case WEATHERED_COPPER -> next = Material.OXIDIZED_COPPER;

            case CHISELED_COPPER -> next = Material.EXPOSED_CHISELED_COPPER;
            case EXPOSED_CHISELED_COPPER -> next = Material.WEATHERED_CHISELED_COPPER;
            case WEATHERED_CHISELED_COPPER -> next = Material.OXIDIZED_CHISELED_COPPER;

            case COPPER_GRATE -> next = Material.EXPOSED_COPPER_GRATE;
            case EXPOSED_COPPER_GRATE -> next = Material.WEATHERED_COPPER_GRATE;
            case WEATHERED_COPPER_GRATE -> next = Material.OXIDIZED_COPPER_GRATE;

            case CUT_COPPER -> next = Material.EXPOSED_CUT_COPPER;
            case EXPOSED_CUT_COPPER -> next = Material.WEATHERED_CUT_COPPER;
            case WEATHERED_CUT_COPPER -> next = Material.OXIDIZED_CUT_COPPER;

            case CUT_COPPER_STAIRS -> next = Material.EXPOSED_CUT_COPPER_STAIRS;
            case EXPOSED_CUT_COPPER_STAIRS -> next = Material.WEATHERED_CUT_COPPER_STAIRS;
            case WEATHERED_CUT_COPPER_STAIRS -> next = Material.OXIDIZED_CUT_COPPER_STAIRS;

            case CUT_COPPER_SLAB -> next = Material.EXPOSED_CUT_COPPER_SLAB;
            case EXPOSED_CUT_COPPER_SLAB -> next = Material.WEATHERED_CUT_COPPER_SLAB;
            case WEATHERED_CUT_COPPER_SLAB -> next = Material.OXIDIZED_CUT_COPPER_SLAB;

            case COPPER_BARS -> next = Material.EXPOSED_COPPER_BARS;
            case EXPOSED_COPPER_BARS -> next = Material.WEATHERED_COPPER_BARS;
            case WEATHERED_COPPER_BARS -> next = Material.OXIDIZED_COPPER_BARS;

            case COPPER_DOOR -> next = Material.EXPOSED_COPPER_DOOR;
            case EXPOSED_COPPER_DOOR -> next = Material.WEATHERED_COPPER_DOOR;
            case WEATHERED_COPPER_DOOR -> next = Material.OXIDIZED_COPPER_DOOR;

            case COPPER_TRAPDOOR -> next = Material.EXPOSED_COPPER_TRAPDOOR;
            case EXPOSED_COPPER_TRAPDOOR -> next = Material.WEATHERED_COPPER_TRAPDOOR;
            case WEATHERED_COPPER_TRAPDOOR -> next = Material.OXIDIZED_COPPER_TRAPDOOR;

            case COPPER_BULB -> next = Material.EXPOSED_COPPER_BULB;
            case EXPOSED_COPPER_BULB -> next = Material.WEATHERED_COPPER_BULB;
            case WEATHERED_COPPER_BULB -> next = Material.OXIDIZED_COPPER_BULB;

            case COPPER_CHAIN -> next = Material.EXPOSED_COPPER_CHAIN;
            case EXPOSED_COPPER_CHAIN -> next = Material.WEATHERED_COPPER_CHAIN;
            case WEATHERED_COPPER_CHAIN -> next = Material.OXIDIZED_COPPER_CHAIN;

            case COPPER_LANTERN -> next = Material.EXPOSED_COPPER_LANTERN;
            case EXPOSED_COPPER_LANTERN -> next = Material.WEATHERED_COPPER_LANTERN;
            case WEATHERED_COPPER_LANTERN -> next = Material.OXIDIZED_COPPER_LANTERN;

            case COPPER_CHEST -> next = Material.EXPOSED_COPPER_CHEST;
            case EXPOSED_COPPER_CHEST -> next = Material.WEATHERED_COPPER_CHEST;
            case WEATHERED_COPPER_CHEST -> next = Material.OXIDIZED_COPPER_CHEST;

            case COPPER_GOLEM_STATUE -> next = Material.EXPOSED_COPPER_GOLEM_STATUE;
            case EXPOSED_COPPER_GOLEM_STATUE -> next = Material.WEATHERED_COPPER_GOLEM_STATUE;
            case WEATHERED_COPPER_GOLEM_STATUE -> next = Material.OXIDIZED_COPPER_GOLEM_STATUE;

            case LIGHTNING_ROD -> next = Material.EXPOSED_LIGHTNING_ROD;
            case EXPOSED_LIGHTNING_ROD -> next = Material.WEATHERED_LIGHTNING_ROD;
            case WEATHERED_LIGHTNING_ROD -> next = Material.OXIDIZED_LIGHTNING_ROD;

            default -> {}
        }
        return next;
    }
}
