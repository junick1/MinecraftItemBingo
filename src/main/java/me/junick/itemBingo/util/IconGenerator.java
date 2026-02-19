package me.junick.itemBingo.util;

import me.junick.itemBingo.enums.BingoRewardType;
import me.junick.itemBingo.model.PlayerBingoProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
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

    public static ItemStack currencyIcon(PlayerBingoProgress prog) {
        return icon(
                Material.EMERALD,
                "§f§l보유 포인트",
                "§7빙고판을 채워 포인트를 얻으세요!",
                "",
                "§a빙고칸 포인트: §f" + prog.getCurrency(BingoRewardType.SLOT),
                "§e빙고줄 포인트: §f" + prog.getCurrency(BingoRewardType.LINE),
                "§b다이아몬드 포인트: §f" + prog.getCurrency(BingoRewardType.DIAMOND)
        );
    }

    public static ItemStack backIcon() {
        return icon(
                Material.ARROW,
                "§a돌아가기",
                "§7이전 메뉴로 돌아갑니다."
        );
    }
}
