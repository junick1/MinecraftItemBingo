package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.enums.BingoEffect;
import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.model.PlayerBingoProgress;
import me.junick.itemBingo.util.IconGenerator;
import me.junick.itemBingo.util.PlayerDataManager;
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
    public static final String TITLE = "§b이펙트 상점";

    public static void open(Player p) {
        if (!new Settings(ItemBingo.getInstance()).isShopEnabled()) return;
        if (!new Settings(ItemBingo.getInstance()).isEffectShopEnabled()) return;

        PlayerBingoProgress prog = PlayerDataManager.get(p);

        Inventory inv = Bukkit.createInventory(null, 9*5, TITLE);

        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);

        ItemMeta glassMeta = grayGlass.getItemMeta();
        glassMeta.setHideTooltip(true);

        grayGlass.setItemMeta(glassMeta);
        redGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 9*2; i++) inv.setItem(i, grayGlass);
        for (int i = 9*2; i < 9*5; i++) inv.setItem(i, redGlass);

        inv.setItem(9*1 + 4, IconGenerator.currencyIcon(prog));

        int slot = 9*3 + 1;
        for (BingoEffect eff : BingoEffect.values()) {
            int lvl = prog.getEffectLevel(eff);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("좌클릭으로 업그레이드하세요.", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());
            lore.add(Component.text("가격: ", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(
                    Component.text(" • ", NamedTextColor.WHITE)
                            .append(Component.text(BingoRewardType.SLOT.getDisplayName()))
                            .append(Component.text(": " + 2 + "개", NamedTextColor.WHITE))
                            .decoration(TextDecoration.ITALIC, false)
            );

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

            ItemStack item = IconGenerator.icon(eff.getIcon(), eff.getDisplay(), lore);
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(KEY_EFFECT, PersistentDataType.STRING, eff.name());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);

            inv.setItem(slot, item);
            slot++;
        }

        inv.setItem(9*4 + 4, IconGenerator.backIcon());

        p.openInventory(inv);
    }

    public static String getTaggedEffect(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        String tag = meta.getPersistentDataContainer().get(KEY_EFFECT, PersistentDataType.STRING);
        return tag;
    }
}
