package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.util.IconGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class MenuGUI {
    public static final String TITLE = "§6빙고 메뉴";

    public static void openMain(Player p) {
        ItemBingo plugin = ItemBingo.getInstance();

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayGlass.getItemMeta(); grayMeta.setHideTooltip(true); grayGlass.setItemMeta(grayMeta);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, grayGlass);
        }

        inv.setItem(11, IconGenerator.icon(Material.OAK_SIGN, "§e랭킹 보기", "§7현재 빙고 랭킹을 확인합니다."));
        inv.setItem(13, IconGenerator.icon(Material.MAP, "§b빙고판 보기", "§7현재 빙고판을 확인합니다."));
        inv.setItem(15, IconGenerator.icon((Settings.isShopEnabled() ? Material.EMERALD : Material.GRAY_DYE), "§a빙고 상점", "§7빙고 상점을 엽니다."));

        p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
        p.openInventory(inv);
    }
}
