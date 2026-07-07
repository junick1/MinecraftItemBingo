package me.junick.itemBingo.events.items;

import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.util.CustomItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class VillagerLevelEvent
implements Listener {
    @EventHandler
    public void onUseOnVillager(PlayerInteractAtEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = e.getPlayer();
        Entity entity = e.getRightClicked();

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!CustomItems.is(hand, BingoItem.KNOWLEDGE_BOOK)) {
            return;
        }
        e.setCancelled(true);

        if (!(entity instanceof Villager villager)) return;
        if (villager.getProfession().equals(Villager.Profession.NITWIT)) return;
        if (villager.getProfession().equals(Villager.Profession.NONE)) return;
        villager.increaseLevel(5);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_WORK_FLETCHER,1f, 1.0f);
        hand.setAmount(hand.getAmount() - 1);
    }
}

