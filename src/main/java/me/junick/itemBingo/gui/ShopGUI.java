package me.junick.itemBingo.gui;

import me.junick.itemBingo.ItemBingo;
import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.util.IconGenerator;
import me.junick.itemBingo.util.ProgressFactory;
import me.junick.itemBingo.util.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ShopGUI {
    public static final String TITLE = "§a상점";

    public static void open(Player p) {
        if (!Settings.isShopEnabled()) {
            p.sendMessage("§c상점이 비활성화되어 있습니다.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        TeamManager tm = ItemBingo.getInstance().getTeamManager();
        int teamId = tm.getTeamId(p);

        // if (teamId != TeamManager.NO_TEAM) {
        //     p.sendMessage("§cDisabled on Team Mode!");
        //     p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        //     return;
        // }

        Inventory inv = Bukkit.createInventory(null, 9*3, TITLE);

        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);

        ItemMeta glassMeta = grayGlass.getItemMeta();
        glassMeta.setHideTooltip(true);

        grayGlass.setItemMeta(glassMeta);
        redGlass.setItemMeta(glassMeta);

        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, grayGlass);

        inv.setItem(4, IconGenerator.currencyIcon(p, ProgressFactory.of(p)));

        inv.setItem(11, IconGenerator.icon(
            Material.BEACON,
            "§b이펙트 상점",
            "§7클릭하면 이펙트 관련 상점으로 이동합니다."
        ));

        inv.setItem(15, IconGenerator.icon(
                Material.CHEST,
                "§e아이템 상점",
                "§7클릭하면 아이템 관련 상점으로 이동합니다."
        ));

        p.openInventory(inv);
    }
}
