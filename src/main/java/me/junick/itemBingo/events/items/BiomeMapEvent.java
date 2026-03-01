package me.junick.itemBingo.events.items;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapDecorations;
import me.junick.itemBingo.enums.BingoBiome;
import me.junick.itemBingo.enums.BingoItem;
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
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BiomeMapEvent
implements Listener {
    private static final String TITLE = "§b바이옴 지도 선택";
    private static final Map<UUID, ItemStack> activeSelectors = new HashMap<>();

    @EventHandler
    public void onUseBiomeMap(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!e.getAction().isRightClick()) {
            return;
        }
        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!CustomItems.is(hand, BingoItem.BIOME_MAP)) {
            return;
        }

        e.setCancelled(true);
        activeSelectors.put(player.getUniqueId(), hand);
        this.openMapSelectorGUI(player);
    }

    public void gainBiomeMap(Player player, BingoBiome biome) {
        World world = player.getWorld();
        if (world.getEnvironment() != biome.getDimension()) {
            player.sendMessage("§c차원이 잘못되었습니다.");
        }
        world.sendMessage(Component.text("§7!!지금 누군가 맵을 뽑아서 잠시 렉이 걸릴 수 있음!!"));

        var res = world.locateNearestBiome(player.getLocation(),1000, 32, 32, biome.getBiome());

        if (res == null) {
            world.sendMessage(Component.text("§c바이옴을 못 찾았대ㅜㅠㅠㅠㅠㅠ"));
            return;
        }
        world.sendMessage(Component.text("§7!찾기 완료!"));

        var playerpos = player.getLocation();
        var loc = res.getLocation();
        double dist = loc.distance(playerpos);

        player.sendMessage(String.format("§e바이옴 발견! §b(%d, %d, %d)§e에 있고, %.2f블록만큼 떨어져 있습니다.", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), dist));

        MapView mapview = Bukkit.createMap(world);
        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta)map.getItemMeta();
        mapview.setCenterX(res.getLocation().getBlockX());
        mapview.setCenterZ(res.getLocation().getBlockZ());
        mapview.setScale(MapView.Scale.CLOSE);
        mapview.setTrackingPosition(true);
        mapview.setUnlimitedTracking(true);

        meta.setMapView(mapview);
        meta.itemName(Component.text(biome.getName() + " 바이옴 지도"));
        map.setItemMeta(meta);
        var data = MapDecorations.mapDecorations().put("+", new MapDecorations.DecorationEntry() {
            @Override
            public MapCursor.Type type() {
                return MapCursor.Type.BANNER_LIGHT_BLUE;
            }

            @Override
            public double x() {
                return res.getLocation().getBlockX();
            }

            @Override
            public double z() {
                return res.getLocation().getBlockZ();
            }

            @Override
            public float rotation() {
                return 180.f;
            }
        }).build();
        map.setData(DataComponentTypes.MAP_DECORATIONS, data);
        player.getInventory().setItemInMainHand(map);

    }

    private void openMapSelectorGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 18, TITLE);
        for (BingoBiome bs : BingoBiome.values()) {
            ItemStack item = new ItemStack(bs.getIcon());
            ItemMeta meta = item.getItemMeta();
            List<TextComponent> lore = List.of(Component.text((String)"§7이 지도로 하시겠습니까?"));
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
        if (!(humanEntity instanceof Player player)) {
            return;
        }
        String title = LegacyComponentSerializer.legacySection().serialize(e.getView().title());
        if (!title.equals(TITLE)) {
            return;
        }
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        ItemStack active = activeSelectors.get(player.getUniqueId());
        if (!CustomItems.is(active, BingoItem.BIOME_MAP)) {
            player.closeInventory();
            player.sendMessage("§c오류: 유효한 지도가 인식되지 않습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        player.closeInventory();
        BingoBiome selectedBiome = null;
        for (BingoBiome bb : BingoBiome.values()) {
            if (bb.getIcon() == clicked.getType()) {
                selectedBiome = bb;
                break;
            }
        }
        if (selectedBiome == null) {
            return;
        }
        gainBiomeMap(player, selectedBiome);
        player.playSound(player.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f, 1.0f);
        activeSelectors.remove(player.getUniqueId());
    }
}
