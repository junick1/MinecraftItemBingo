package me.junick.itemBingo.events.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.ItemShopGUI;
import me.junick.itemBingo.gui.ShopGUI;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.util.CustomItems;
import me.junick.itemBingo.util.GuiSync;
import me.junick.itemBingo.util.PlayerDataManager;
import me.junick.itemBingo.util.ProgressFactory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
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
        if (!BingoGuiHolder.is(e.getView().getTopInventory(), BingoGuiHolder.Gui.ITEM_SHOP)) return;

        e.setCancelled(true);

        if (e.getRawSlot() == ItemShopGUI.SLOT_BACK) {
            ShopGUI.open(p);
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        BingoItem selected = getClickedShopItem(clicked);
        if (selected == null) return;

        handlePurchase(p, selected);
    }

    private ItemStack createItem(BingoItem item, SupportedLocale loc) {
        return switch (item) {
            case BINGO_FILLER -> CustomItems.get(BingoItem.BINGO_FILLER, loc);
            case DYE_SELECTOR -> CustomItems.get(BingoItem.DYE_SELECTOR, loc);
            case COPPER_OXIDIZER -> CustomItems.get(BingoItem.COPPER_OXIDIZER, loc);
            case EXPLORER_MAP -> CustomItems.get(BingoItem.EXPLORER_MAP, loc);
            case BIOME_MAP -> CustomItems.get(BingoItem.BIOME_MAP, loc);
            case DIAMOND -> {
                var itemStack = new ItemStack(Material.DIAMOND);
                itemStack.setAmount(10);
                yield itemStack;
            }
            case MYSTERIOUS_SHERD -> CustomItems.get(BingoItem.MYSTERIOUS_SHERD, loc);
            case GOLDEN_PICKAXE -> {
                var itemStack = new ItemStack(Material.GOLDEN_PICKAXE);
                var meta = itemStack.getItemMeta();
                meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                itemStack.setItemMeta(meta);
                yield itemStack;
            }
            case IRON_PICKAXE -> {
                var itemStack = new ItemStack(Material.IRON_PICKAXE);
                var meta = itemStack.getItemMeta();
                meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                itemStack.setItemMeta(meta);
                yield itemStack;
            }
            case HONEY_FILLER -> CustomItems.get(BingoItem.HONEY_FILLER, loc);
            case BEEHIVE_BREAKER -> CustomItems.get(BingoItem.BEEHIVE_BREAKER, loc);
            case EGG_QOL -> CustomItems.get(BingoItem.EGG_QOL, loc);
            case KNOWLEDGE_BOOK -> CustomItems.get(BingoItem.KNOWLEDGE_BOOK, loc);
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
        BingoProgressAccess prog = ProgressFactory.of(p);
        SupportedLocale loc = Messages.localeOf(p);

        if (!canAfford(prog, p, item)) {
            p.sendMessage(Messages.get(p, "shop.not-enough"));
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            return;
        }

        ItemStack toGive = createItem(item, loc);
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

            p.sendMessage(Messages.get(p, "shop.inventory-full"));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            ItemShopGUI.open(p);
            return;
        }

        deductCost(prog, p, item);
        PlayerDataManager.save(p);

        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        p.sendMessage(Messages.get(p, "shop.purchased", "item", item.displayName(loc)));
        // Purchase can spend team-shared currency, so refresh every teammate's
        // open shop (includes the buyer) to reflect the new balance.
        GuiSync.refreshShops(prog.viewers(p));
    }

    private boolean canAfford(BingoProgressAccess prog, Player p, BingoItem item) {
        for (BingoRewardType type : BingoRewardType.values()) {
            int price = item.getPrice(type);
            int balance = prog.getCurrency(p, type);
            if (price > 0 && balance < price) {
                return false;
            }
        }
        return true;
    }

    private void deductCost(BingoProgressAccess prog, Player p, BingoItem item) {
        for (BingoRewardType type : BingoRewardType.values()) {
            int price = item.getPrice(type);
            if (price > 0) prog.addCurrency(p, type, -price);
        }
    }
}
