package me.junick.itemBingo.events.items;

import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.enums.BingoStructure;
import me.junick.itemBingo.util.CustomItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapCursor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExplorerMapEvent
implements Listener {
    private static final String TITLE = "§b지도 선택";

    @EventHandler
    public void onUseExplorerMap(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!e.getAction().isRightClick()) {
            return;
        }
        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!CustomItems.is(hand, BingoItem.EXPLORER_MAP)) {
            return;
        }

        e.setCancelled(true);
        this.openMapSelectorGUI(player);
    }

    public void gainExplorerMap(Player player, BingoStructure structure) {
        World world = player.getWorld();
        if (world.getEnvironment() != structure.getDimension()) {
            player.sendMessage("§c차원이 잘못되었습니다.");
        }
        world.sendMessage(Component.text("§7!!지금 누군가 맵을 뽑아서 잠시 렉이 걸릴 수 있음!!"));
        var res = world.locateNearestStructure(player.getLocation(), structure.getStructure(), 100, false);
        if (res == null) {
            world.sendMessage(Component.text("§c구조물을 못 찾았대ㅜㅠㅠㅠㅠㅠ"));
            return;
        }
        var playerpos = player.getLocation();
        playerpos.setY(0.0);
        var loc = res.getLocation();

        double dist = loc.distance(playerpos);
        player.sendMessage(String.format("§e구조물 발견! §b(%d, %d)§e에 있고, %.2f블록만큼 떨어져 있습니다.", loc.getBlockX(), loc.getBlockZ(), dist));

        ItemStack map = Bukkit.createExplorerMap(world, loc, structure.getStructure().getStructureType(), MapCursor.Type.BANNER_RED, 10, false);
        if (map == null) {
            world.sendMessage(Component.text("§c구조물을 못 찾았대ㅜㅠㅠㅠㅠㅠ"));
            return;
        }
        world.sendMessage(Component.text("§7!찾기 완료!"));
        var meta = map.getItemMeta();
        meta.itemName(Component.text(structure.getName() + " 탐험가 지도"));
        map.setItemMeta(meta);
        player.getInventory().setItemInMainHand(map);
    }

    private void openMapSelectorGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, (int)(27), TITLE);
        for (BingoStructure bs : BingoStructure.values()) {
            ItemStack item = new ItemStack(bs.getIcon());
            ItemMeta meta = item.getItemMeta();
            List<TextComponent> lore = List.of(Component.text("§7이 지도로 하시겠습니까?"));
            meta.lore(lore);
            meta.itemName(Component.text("§d" + bs.getName()));
            item.setItemMeta(meta);
            inv.addItem(item);
        }
        player.openInventory(inv);

    }
    @EventHandler
    public void onClickMap(InventoryClickEvent e) {
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        String title = LegacyComponentSerializer.legacySection().serialize(e.getView().title());
        if (!title.equals(TITLE)) {
            return;
        }
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        ItemStack active = player.getInventory().getItemInMainHand();
        if (!CustomItems.is(active, BingoItem.EXPLORER_MAP)) {
            player.closeInventory();
            player.sendMessage("§c오류: 유효한 지도가 인식되지 않습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        player.closeInventory();
        BingoStructure selectedStructure = null;
        for (BingoStructure bs : BingoStructure.values()) {
            if (bs.getIcon() == clicked.getType()) {
                selectedStructure = bs;
                break;
            }
        }
        if (selectedStructure == null) {
            return;
        }
        gainExplorerMap(player, selectedStructure);
        player.playSound(player.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f, 1.0f);
    }
}

