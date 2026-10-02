package me.junick.itemBingo.events.items;

import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.util.CustomItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.TurtleEgg;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EggEvent
implements Listener {
    @EventHandler
    public void onUseSnifferEgg(PlayerInteractEvent e) {
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
        if (!CustomItems.is(hand, BingoItem.EGG_QOL)) { // check honey filler item
            return;
        }
        Block block = e.getClickedBlock();
        e.setCancelled(true);
        if (block.getType() == Material.SNIFFER_EGG) {
            block.setType(Material.AIR);
            Location loc = block.getLocation();
            World world = block.getWorld();
            world.spawnEntity(loc.add(0, 0.5, 0), EntityType.SNIFFER);
            player.playSound(block.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE,0.5f, 1.0f);
            hand.setAmount(hand.getAmount() - 1);
        } else if (block.getType() == Material.TURTLE_EGG) {
            if (block.getState() instanceof TurtleEgg turtleEgg) {
                int n = turtleEgg.getEggs();
                for (int i = 0; i < n; i++) {
                    Location loc = block.getLocation();
                    World world = block.getWorld();
                    Turtle entity = (Turtle)world.spawnEntity(loc.add(0, 0.5, 0), EntityType.TURTLE);
                    entity.setBaby();
                    entity.setAge(0);
                }
                player.playSound(block.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE,0.5f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onUseOnChicken(PlayerInteractAtEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = e.getPlayer();
        Entity entity = e.getRightClicked();

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!CustomItems.is(hand, BingoItem.EGG_QOL)) {
            return;
        }
        e.setCancelled(true);

        if (!(entity instanceof Chicken chicken)) return;
        chicken.setEggLayTime(1);

        hand.setAmount(hand.getAmount() - 1);
    }
}

