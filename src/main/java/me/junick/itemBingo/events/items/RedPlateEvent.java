package me.junick.itemBingo.events.items;

import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.util.CustomItems;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RedPlateEvent implements Listener {
    static List<Material> itemList = new ArrayList<>();
    static {
        for (Material material: Material.values()) {
            if (material.name().toLowerCase().contains("stained_glass") && !material.name().toLowerCase().contains("pane")) {
                itemList.add(material);
            }
        }
    }
    static List<Material> item2List = new ArrayList<>();
    static {
        for (Material material: Material.values()) {
            if (material.name().toLowerCase().contains("stained_glass_pane")) {
                item2List.add(material);
            }
        }
    }
    static Random random = new Random();

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!e.getAction().isRightClick()) return;

        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (!(CustomItems.is(hand, BingoItem.RED_PLATE1) || CustomItems.is(hand, BingoItem.RED_PLATE2))) return;

        e.setCancelled(true);

        int num = 2;
        double r = random.nextDouble(0, 1);
        if (r < 0.05) num = 5;
        else if (r < 0.20) num = 4;
        else if (r < 0.50) num = 3;

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);

        List<Material> pool = new ArrayList<>(itemList);
        if (CustomItems.is(hand, BingoItem.RED_PLATE2)) pool = new ArrayList<>(item2List);
        Collections.shuffle(pool, random);

        ItemStack[] toGive = new ItemStack[num];
        for (int i = 0; i < num; i++) {
            toGive[i] = new ItemStack(pool.get(i), CustomItems.is(hand, BingoItem.RED_PLATE1) ? random.nextInt(1, 5) : random.nextInt(1, 17));
        }

        var leftover = player.getInventory().addItem(toGive);
        if (!leftover.isEmpty()) {
            for (var item : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
        }

        hand.setAmount(hand.getAmount() - 1);
    }
}
