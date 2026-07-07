package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.DiamondExchangeGUI;
import me.junick.itemBingo.gui.EmeraldExchangeGUI;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.util.GuiSync;
import me.junick.itemBingo.util.PlayerDataManager;
import me.junick.itemBingo.util.ProgressFactory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class EmeraldExchangeClickEvent implements Listener {
    @EventHandler
    public void onDiamondExchangeClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (!BingoGuiHolder.is(e.getView().getTopInventory(), BingoGuiHolder.Gui.EMERALD_EXCHANGE)) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(ItemBingo.getInstance(), "exchange_amount");
        int amount = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 1);

        switch (clicked.getType()) {
            case RED_STAINED_GLASS -> {
                if (amount <= 1) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                    return;
                }

                int decreaseAmount = clicked.getAmount();
                int newAmount = Math.max(1, amount - decreaseAmount);

                EmeraldExchangeGUI.open(p, newAmount);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }

            case LIME_STAINED_GLASS -> {
                int currentDiamonds = countDiamonds(p);
                if (amount >= currentDiamonds) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                    return;
                }

                int increaseAmount = clicked.getAmount();
                int newAmount = Math.min(currentDiamonds, amount + increaseAmount);

                EmeraldExchangeGUI.open(p, newAmount);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }

            case GOLD_INGOT -> {
                int currentDiamonds = countDiamonds(p);
                if (currentDiamonds <= 0) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                    return;
                }

                EmeraldExchangeGUI.open(p, currentDiamonds);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }

            case GREEN_TERRACOTTA -> confirm(p, amount);
            case RED_TERRACOTTA -> cancel(p);
        }
    }

    private void confirm(Player p, int amount) {
        int diamonds = countDiamonds(p);
        if (diamonds < amount) {
            p.sendMessage(Messages.get(p, "gui.diamond.not-enough"));
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            return;
        }

        removeDiamonds(p, amount);

        int gained = amount;
        BingoProgressAccess proga = ProgressFactory.of(p);

        proga.addCurrencyAll(BingoRewardType.DIAMOND, gained);
        PlayerDataManager.save(p);

        // DIAMOND points are team-shared, so refresh teammates' open shops.
        GuiSync.refreshShops(proga.viewers(p));

        p.sendMessage(Messages.get(p, "gui.diamond.exchanged", "amount", amount, "gained", gained));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        p.closeInventory();
    }

    private void cancel(Player p) {
        p.sendMessage(Messages.get(p, "gui.diamond.cancelled"));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        p.closeInventory();
    }

    private int countDiamonds(Player p) {
        return p.getInventory().all(Material.EMERALD)
                .values()
                .stream()
                .mapToInt(ItemStack::getAmount)
                .sum();
    }

    private void removeDiamonds(Player p, int count) {
        for (ItemStack stack : p.getInventory().getContents()) {
            if (stack == null || stack.getType() != Material.EMERALD) continue;
            int take = Math.min(count, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            count -= take;
            if (count <= 0) break;
        }
    }
}
