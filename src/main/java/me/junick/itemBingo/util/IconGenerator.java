package me.junick.itemBingo.util;

import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.interfaces.access.BingoProgressAccess;
import me.junick.itemBingo.model.PlayerBingoProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class IconGenerator {
    public static ItemStack icon(Material m, String name, List<Component> lore) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack icon(Material m, String name, String... lore) {
        List<String> loreList = List.of(lore);
        List<Component> loreComponents = loreList.stream()
                .map(line -> Component.text(line).decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());

        return icon(m, name, loreComponents);
    }

    public static ItemStack icon(Material m, String name, int amount, List<Component> lore) {
        ItemStack item = icon(m, name, lore);
        item.setAmount(amount);
        return item;
    }

    public static ItemStack icon(Material m, String name, int amount, String... lore) {
        ItemStack item = icon(m, name, lore);
        item.setAmount(amount);
        return item;
    }

    public static ItemStack currencyIcon(Player p, BingoProgressAccess prog) {
        SupportedLocale loc = Messages.localeOf(p);
        return icon(
                Material.EMERALD,
                Messages.legacy(loc, "gui.currency.title"),
                Messages.getList(loc, "gui.currency.lore",
                        "slot", prog.getCurrency(p, BingoRewardType.SLOT),
                        "line", prog.getCurrency(p, BingoRewardType.LINE),
                        "diamond", prog.getCurrency(p, BingoRewardType.DIAMOND))
        );
    }


    public static ItemStack backIcon(SupportedLocale loc) {
        return icon(
                Material.ARROW,
                Messages.legacy(loc, "gui.back.name"),
                Messages.legacy(loc, "gui.back.lore")
        );
    }
}
