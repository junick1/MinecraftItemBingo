package me.junick.itemBingo.gui;

import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.enums.BingoEffect;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.util.IconGenerator;
import me.junick.itemBingo.util.PlayerDataManager;
import me.junick.itemBingo.util.ProgressFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

import static me.junick.itemBingo.ItemBingo.KEY_EFFECT;

public class EffectShopGUI {
    /** Title message key. GUI identity is the {@link BingoGuiHolder} marker, not the title. */
    public static final String TITLE_KEY = "gui.effect-shop.title";
    public static final int SLOT_BACK = 9 * 5 + 4;

    /** Slot cost (in SLOT points) of one effect upgrade. */
    private static final int UPGRADE_COST = 2;

    public static void open(Player p) {
        if (!Settings.isShopEnabled()) return;
        if (!Settings.isEffectShopEnabled()) return;

        PlayerBingoProgress prog = PlayerDataManager.get(p);
        BingoProgressAccess proga = ProgressFactory.of(p);
        SupportedLocale loc = Messages.localeOf(p);

        BingoGuiHolder holder = new BingoGuiHolder(BingoGuiHolder.Gui.EFFECT_SHOP);
        Inventory inv = Bukkit.createInventory(holder, 9 * 6, Messages.get(loc, TITLE_KEY));
        holder.setInventory(inv);

        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);

        ItemMeta glassMeta = grayGlass.getItemMeta();
        glassMeta.setHideTooltip(true);

        grayGlass.setItemMeta(glassMeta);
        redGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 9 * 2; i++) inv.setItem(i, grayGlass);
        for (int i = 9 * 2; i < 9 * 3; i++) inv.setItem(i, redGlass);
        for (int i = 9 * 3; i < 9 * 4; i += 8) inv.setItem(i, redGlass);
        for (int i = 9 * 4; i < 9 * 5; i += 8) inv.setItem(i, redGlass);
        for (int i = 9 * 5; i < 9 * 6; i++) inv.setItem(i, redGlass);

        inv.setItem(9 * 1 + 4, IconGenerator.currencyIcon(p, proga));

        int slot = 9 * 3 + 1;
        for (BingoEffect eff : BingoEffect.values()) {
            int lvl = prog.getEffectLevel(eff);

            List<Component> lore = new ArrayList<>();
            lore.add(Messages.get(loc, "gui.effect-shop.upgrade-hint").decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());
            lore.add(Messages.get(loc, "gui.shop-common.price-label").decoration(TextDecoration.ITALIC, false));

            lore.add(Messages.get(loc, "gui.shop-common.cost",
                            "reward", BingoRewardType.SLOT.displayName(loc), "cost", UPGRADE_COST)
                    .decoration(TextDecoration.ITALIC, false));

            StringBuilder bar = new StringBuilder();
            {
                int current = lvl;
                int max = eff.getMaxLevel();

                for (int i = 0; i < max; i++) {
                    if (i < current) bar.append("■");
                    else bar.append("□");
                }
            }

            lore.add(Component.empty());
            lore.add(Component.text("[", NamedTextColor.GRAY)
                    .append(Component.text(bar.toString(), eff.getMaxLevel() == lvl ? NamedTextColor.GREEN : NamedTextColor.AQUA))
                    .append(Component.text("]", NamedTextColor.GRAY))
                    .decoration(TextDecoration.ITALIC, false));

            boolean maxed = lvl >= eff.getMaxLevel();
            boolean affordable = proga.getCurrency(p, BingoRewardType.SLOT) >= UPGRADE_COST;

            lore.add(Component.empty());
            if (maxed) {
                lore.add(Messages.get(loc, "gui.effect-shop.maxed").decoration(TextDecoration.ITALIC, false));
            } else if (affordable) {
                lore.add(Messages.get(loc, "gui.effect-shop.upgradable").decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Messages.get(loc, "gui.shop-common.not-affordable").decoration(TextDecoration.ITALIC, false));
            }

            ItemStack item = IconGenerator.icon(eff.getIcon(), eff.displayName(loc), lore);
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(KEY_EFFECT, PersistentDataType.STRING, eff.name());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);

            inv.setItem(slot, item);

            slot++;
            if (slot == 9 * 3 + 8) slot += 2;
        }

        inv.setItem(SLOT_BACK, IconGenerator.backIcon(loc));

        p.openInventory(inv);
    }

    public static String getTaggedEffect(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        String tag = meta.getPersistentDataContainer().get(KEY_EFFECT, PersistentDataType.STRING);
        return tag;
    }
}
