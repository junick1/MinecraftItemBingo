package me.junick.itemBingo.events.items;

import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.util.CustomItems;
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

import java.util.List;

public class DyeSelectorEvent implements Listener {

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
        SupportedLocale loc = Messages.localeOf(player);

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

        BingoGuiHolder holder = new BingoGuiHolder(BingoGuiHolder.Gui.DYE_SELECTOR);
        Inventory inv = Bukkit.createInventory(holder, (dyes.length + 8) / 9 * 9, Messages.get(loc, "items.dye.title"));
        holder.setInventory(inv);

        for (Material dye : dyes) {
            ItemStack item = new ItemStack(dye);
            ItemMeta meta = item.getItemMeta();
            meta.lore(List.of(Messages.get(loc, "items.dye.lore")));
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

        if (!BingoGuiHolder.is(e.getView().getTopInventory(), BingoGuiHolder.Gui.DYE_SELECTOR)) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Material dyeType = clicked.getType();
        if (!dyeType.name().endsWith("_DYE")) return;

        ItemStack active = player.getInventory().getItemInMainHand();
        if (!CustomItems.is(active, BingoItem.DYE_SELECTOR)) {
            player.closeInventory();
            player.sendMessage(Messages.get(player, "items.dye.invalid"));
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

            player.sendMessage(Messages.get(player, "items.dye.inventory-full"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        hand.setAmount(hand.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);

        player.closeInventory();
    }
}
