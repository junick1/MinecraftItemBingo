package me.junick.itemBingo.events.items;

import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.util.CustomItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CopperOxidizerEvent
implements Listener {
    @EventHandler
    public void onUseCopperOxidizer(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!e.getAction().isRightClick()) {
            return;
        }
        if (e.getClickedBlock() == null) {
            return;
        }
        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!CustomItems.is(hand, BingoItem.COPPER_OXIDIZER)) {
            return;
        }
        e.setCancelled(true);
        Block block = e.getClickedBlock();
        Material current = block.getType();
        Material next = this.getNextOxidation(current);
        if (next == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            player.sendMessage(Messages.get(player, "items.copper.invalid-block"));
            return;
        }
        String oldDataStr = block.getBlockData().getAsString();
        String newDataStr = oldDataStr.replace(current.getKey().toString(), next.getKey().toString());
        block.setType(next);
        try {
            BlockData newData = Bukkit.createBlockData((String)newDataStr);
            block.setBlockData(newData, false);
            hand.setAmount(hand.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_HURT, 1.0f, 1.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.1f, 0.5f);
            player.sendMessage(Messages.get(player, "items.copper.oxidized"));
        }
        catch (IllegalArgumentException ex) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            player.sendMessage(Messages.get(player, "items.copper.invalid-block"));
            return;
        }
    }

    @EventHandler
    public void onUseCopperOxidizerOnCopperGolem(PlayerInteractAtEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = e.getPlayer();
        Entity entity = e.getRightClicked();

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!CustomItems.is(hand, BingoItem.COPPER_OXIDIZER)) {
            return;
        }
        if (!(entity instanceof CopperGolem copper)) return;
        copper.getWorld().dropItem(copper.getLocation(), new ItemStack(Material.COPPER_GOLEM_STATUE));
        copper.setHealth(0.0);
        e.setCancelled(true);
        hand.setAmount(hand.getAmount() - 1);
    }

    protected static Material getNextOxidation(Material type) {
        return switch (type) {
            case COPPER_BLOCK -> Material.EXPOSED_COPPER;
            case EXPOSED_COPPER -> Material.WEATHERED_COPPER;
            case WEATHERED_COPPER -> Material.OXIDIZED_COPPER;

            case CHISELED_COPPER -> Material.EXPOSED_CHISELED_COPPER;
            case EXPOSED_CHISELED_COPPER -> Material.WEATHERED_CHISELED_COPPER;
            case WEATHERED_CHISELED_COPPER -> Material.OXIDIZED_CHISELED_COPPER;

            case COPPER_GRATE -> Material.EXPOSED_COPPER_GRATE;
            case EXPOSED_COPPER_GRATE -> Material.WEATHERED_COPPER_GRATE;
            case WEATHERED_COPPER_GRATE -> Material.OXIDIZED_COPPER_GRATE;

            case CUT_COPPER -> Material.EXPOSED_CUT_COPPER;
            case EXPOSED_CUT_COPPER -> Material.WEATHERED_CUT_COPPER;
            case WEATHERED_CUT_COPPER -> Material.OXIDIZED_CUT_COPPER;

            case CUT_COPPER_STAIRS -> Material.EXPOSED_CUT_COPPER_STAIRS;
            case EXPOSED_CUT_COPPER_STAIRS -> Material.WEATHERED_CUT_COPPER_STAIRS;
            case WEATHERED_CUT_COPPER_STAIRS -> Material.OXIDIZED_CUT_COPPER_STAIRS;

            case CUT_COPPER_SLAB -> Material.EXPOSED_CUT_COPPER_SLAB;
            case EXPOSED_CUT_COPPER_SLAB -> Material.WEATHERED_CUT_COPPER_SLAB;
            case WEATHERED_CUT_COPPER_SLAB -> Material.OXIDIZED_CUT_COPPER_SLAB;

            case COPPER_BARS -> Material.EXPOSED_COPPER_BARS;
            case EXPOSED_COPPER_BARS -> Material.WEATHERED_COPPER_BARS;
            case WEATHERED_COPPER_BARS -> Material.OXIDIZED_COPPER_BARS;

            case COPPER_DOOR -> Material.EXPOSED_COPPER_DOOR;
            case EXPOSED_COPPER_DOOR -> Material.WEATHERED_COPPER_DOOR;
            case WEATHERED_COPPER_DOOR -> Material.OXIDIZED_COPPER_DOOR;

            case COPPER_TRAPDOOR -> Material.EXPOSED_COPPER_TRAPDOOR;
            case EXPOSED_COPPER_TRAPDOOR -> Material.WEATHERED_COPPER_TRAPDOOR;
            case WEATHERED_COPPER_TRAPDOOR -> Material.OXIDIZED_COPPER_TRAPDOOR;

            case COPPER_BULB -> Material.EXPOSED_COPPER_BULB;
            case EXPOSED_COPPER_BULB -> Material.WEATHERED_COPPER_BULB;
            case WEATHERED_COPPER_BULB -> Material.OXIDIZED_COPPER_BULB;

            case COPPER_CHAIN -> Material.EXPOSED_COPPER_CHAIN;
            case EXPOSED_COPPER_CHAIN -> Material.WEATHERED_COPPER_CHAIN;
            case WEATHERED_COPPER_CHAIN -> Material.OXIDIZED_COPPER_CHAIN;

            case COPPER_LANTERN -> Material.EXPOSED_COPPER_LANTERN;
            case EXPOSED_COPPER_LANTERN -> Material.WEATHERED_COPPER_LANTERN;
            case WEATHERED_COPPER_LANTERN -> Material.OXIDIZED_COPPER_LANTERN;

            case COPPER_CHEST -> Material.EXPOSED_COPPER_CHEST;
            case EXPOSED_COPPER_CHEST -> Material.WEATHERED_COPPER_CHEST;
            case WEATHERED_COPPER_CHEST -> Material.OXIDIZED_COPPER_CHEST;

            case COPPER_GOLEM_STATUE -> Material.EXPOSED_COPPER_GOLEM_STATUE;
            case EXPOSED_COPPER_GOLEM_STATUE -> Material.WEATHERED_COPPER_GOLEM_STATUE;
            case WEATHERED_COPPER_GOLEM_STATUE -> Material.OXIDIZED_COPPER_GOLEM_STATUE;

            case LIGHTNING_ROD -> Material.EXPOSED_LIGHTNING_ROD;
            case EXPOSED_LIGHTNING_ROD -> Material.WEATHERED_LIGHTNING_ROD;
            case WEATHERED_LIGHTNING_ROD -> Material.OXIDIZED_LIGHTNING_ROD;

            default -> null;
        };
    }
}

