package me.junick.itemBingo.events.items;

import me.junick.itemBingo.util.CustomItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CopperOxidizerEvent implements Listener {
    @EventHandler
    public void onUseCopperOxidizer(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!e.getAction().isRightClick()) return;
        if (e.getClickedBlock() == null) return;

        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (!CustomItems.isCopperOxidizer(hand)) return;

        e.setCancelled(true);

        Block block = e.getClickedBlock();

        Material current = block.getType();
        Material next = getNextOxidation(current);
        if (next == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            player.sendMessage("§c이 블럭에는 사용할 수 없습니다.");
            return;
        }

        String oldDataStr = block.getBlockData().getAsString();
        String newDataStr = oldDataStr.replace(
                current.getKey().toString(),
                next.getKey().toString()
        );

        block.setType(next);

        try {
            BlockData newData = Bukkit.createBlockData(newDataStr);
            block.setBlockData(newData, false);

            hand.setAmount(hand.getAmount() - 1);

            player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_HURT, 1.0f, 1.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.1f, 0.5f);
            player.sendMessage("§a구리가 산화되었습니다!");
        } catch (IllegalArgumentException ex) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            player.sendMessage("§c이 블럭에는 사용할 수 없습니다.");
            return;
        }
    }

    @EventHandler
    public void onUseCopperOxidizerOnCopperGolem(PlayerInteractAtEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
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

            default -> {}
        }
        return next;
    }
}
