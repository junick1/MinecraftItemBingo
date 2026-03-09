package me.junick.itemBingo.events.items;

import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.gui.ItemShopGUI;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DyeSelectorEvent implements Listener {
    private static final String TITLE = "§b염료 선택 메뉴";

    @EventHandler
    public void onDyeSelect(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!e.getAction().isRightClick()) return;

        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (!CustomItems.is(hand, BingoItem.DYE_SELECTOR)) return;

        e.setCancelled(true);

        openDyeSelectorGUI(player);
    }

    private void openDyeSelectorGUI(Player player) {
        Material[] dyes = {
                Material.WHITE_DYE,
                Material.ORANGE_DYE,
                Material.MAGENTA_DYE,
                Material.LIGHT_BLUE_DYE,
                Material.YELLOW_DYE,
                Material.LIME_DYE,
                Material.PINK_DYE,
                Material.GRAY_DYE,
                Material.LIGHT_GRAY_DYE,
                Material.CYAN_DYE,
                Material.PURPLE_DYE,
                Material.BLUE_DYE,
                Material.BROWN_DYE,
                Material.GREEN_DYE,
                Material.RED_DYE,
                Material.BLACK_DYE
        };

        Inventory inv = Bukkit.createInventory(null, (dyes.length + 8) / 9 * 9, TITLE);
        for (Material dye : dyes) {
            ItemStack item = new ItemStack(dye);
            ItemMeta meta = item.getItemMeta();

            List<Component> lore = List.of(
                    Component.text("§7클릭하면 이 염료를 받습니다.")
            );

            meta.lore(lore);
            item.setItemMeta(meta);
            inv.addItem(item);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    @EventHandler
    public void onClickDye(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!CustomItems.is(hand, BingoItem.DYE_SELECTOR)) return;

        String title = LegacyComponentSerializer.legacySection().serialize(e.getView().title());
        if (!title.equals(TITLE)) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Material dyeType = clicked.getType();
        if (!dyeType.name().endsWith("_DYE")) return;

        ItemStack active = player.getInventory().getItemInMainHand();
        if (!CustomItems.is(active, BingoItem.DYE_SELECTOR)) {
            player.closeInventory();
            player.sendMessage("§c오류: 유효한 선택권이 인식되지 않습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        ItemStack toGive = new ItemStack(dyeType);

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

        player.closeInventory();
    }
}
