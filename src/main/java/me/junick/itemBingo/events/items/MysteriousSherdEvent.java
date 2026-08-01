package me.junick.itemBingo.events.items;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.util.CustomItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MysteriousSherdEvent implements Listener {
    static List<Material> itemList = new ArrayList<>();
    static {
        for (Material material: Material.values()) {
            if (material.name().toLowerCase().contains("sherd")) {
                itemList.add(material);
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

        if (!CustomItems.is(hand, BingoItem.MYSTERIOUS_SHERD)) return;

        e.setCancelled(true);

        int num = 1;
        double r = random.nextDouble(0, 1);
        if (r < 0.05) num = 4;
        else if (r < 0.20) num = 3;
        else if (r < 0.50) num = 2;

        player.sendMessage("+" + num);
        switch (num) {
            case 1 -> player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            case 2 -> player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            case 3 -> player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            case 4 -> player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 2.0f) ;
        }

        List<Material> pool = new ArrayList<>(itemList);
        Collections.shuffle(pool, random);

        ItemStack[] toGive = new ItemStack[num];
        for (int i = 0; i < num; i++) {
            toGive[i] = new ItemStack(pool.get(i));
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
