package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.gui.ItemShopGUI;
import me.junick.itemBingo.gui.ShopGUI;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.util.CustomItems;
import me.junick.itemBingo.util.PlayerDataManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ItemShopClickEvent implements Listener {
    @EventHandler
    public void onItemShopClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String title = LegacyComponentSerializer.legacySection().serialize(e.getView().title());
        if (!title.equals(ItemShopGUI.TITLE)) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getItemMeta().getDisplayName().contains("돌아가기")) {
            ShopGUI.open(p);
            return;
        }

        BingoItem selected = getClickedShopItem(clicked);
        if (selected == null) return;

        handlePurchase(p, selected);
    }

    private ItemStack createItem(BingoItem item) {
        return switch (item) {
            case BINGO_FILLER -> CustomItems.get(BingoItem.BINGO_FILLER);
            case DYE_SELECTOR -> CustomItems.get(BingoItem.DYE_SELECTOR);
            case COPPER_OXIDIZER -> CustomItems.get(BingoItem.COPPER_OXIDIZER);
            default -> null;
        };
    }

    private BingoItem getClickedShopItem(ItemStack item) {
        String key = item.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(ItemBingo.getInstance(), "shop_item"),
                PersistentDataType.STRING
        );
        if (key == null) return null;

        try {
            return BingoItem.valueOf(key);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void handlePurchase(Player p, BingoItem item) {
        PlayerBingoProgress prog = PlayerDataManager.get(p);

        if (!canAfford(prog, item)) {
            p.sendMessage("§e[상점] §c포인트가 부족합니다!");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            return;
        }

        ItemStack toGive = createItem(item);
        if (toGive == null) return;

        var leftover = p.getInventory().addItem(toGive);
        if (!leftover.isEmpty()) {
            int leftAmount = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            if (leftAmount < toGive.getAmount()) {
                int givenAmount = toGive.getAmount() - leftAmount;
                ItemStack remove = toGive.clone();
                remove.setAmount(givenAmount);
                p.getInventory().removeItem(remove);
            }

            p.sendMessage("§e[상점] §c인벤토리가 가득 차서 구매할 수 없습니다!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            ItemShopGUI.open(p);
            return;
        }

        deductCost(prog, item);
        PlayerDataManager.save(p);

        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        p.sendMessage("§e[상점] §a" + item.getDisplay() + "§f(을)를 구매했습니다!");
        ItemShopGUI.open(p);
    }

    private boolean canAfford(PlayerBingoProgress prog, BingoItem item) {
        for (BingoRewardType type : BingoRewardType.values()) {
            int price = item.getPrice(type);
            int balance = prog.getCurrency(type);

            if (price > 0 && balance < price) {
                return false;
            }
        }
        return true;
    }

    private void deductCost(PlayerBingoProgress prog, BingoItem item) {
        for (BingoRewardType type : BingoRewardType.values()) {
            int price = item.getPrice(type);
            if (price > 0) prog.addCurrency(type, -price);
        }
    }
}
