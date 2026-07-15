package me.junick.itemBingo.events.items;

import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.util.CustomItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MysteriousSherdEvent implements Listener {
    private static final String TITLE = "§b염료 선택 메뉴";

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

        ItemStack toGive = new ItemStack(itemList.get(random.nextInt(itemList.size())));

        var leftover = player.getInventory().addItem(toGive);
        if (!leftover.isEmpty()) {
            int leftAmount = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            if (leftAmount < toGive.getAmount()) {
                int givenAmount = toGive.getAmount() - leftAmount;
                ItemStack remove = toGive.clone();
                remove.setAmount(givenAmount);
                player.getInventory().removeItem(remove);
            }

            player.sendMessage("§c오류: 인벤토리가 꽉 차있습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        hand.setAmount(hand.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        
    }
}
