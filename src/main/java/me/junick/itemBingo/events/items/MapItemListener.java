package me.junick.itemBingo.events.items;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapDecorations;
import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.gui.BingoGuiHolder;
import me.junick.itemBingo.gui.MapSelectorGUI;
import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import me.junick.itemBingo.interfaces.MapOption;
import me.junick.itemBingo.util.CustomItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.Map;

/**
 * Shared behaviour for the custom map items (biome map, explorer map): the
 * right-click → selector GUI → locate → hand over a finished map flow. Concrete
 * subclasses only supply the catalog, the locate call, and cosmetic naming.
 *
 * @param <T> the option enum (e.g. {@code BingoBiome}, {@code BingoStructure})
 */
public abstract class MapItemListener<T extends Enum<T> & MapOption> implements Listener {

    /** Subtitle fades in instantly (0s), holds, then fades out. */
    private static final Title.Times TIMES =
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500));

    /* ===================== subclass hooks ===================== */

    protected abstract BingoItem triggerItem();

    /** Message key for the selector GUI title. */
    protected abstract String titleKey();

    protected abstract T[] options();
    protected abstract T optionByName(String name);

    /** Locate the target on the main thread, or null if nothing was found. */
    protected abstract Location locate(World world, Location from, T option);

    /** Localized display name for the produced map item. */
    protected abstract String mapItemName(T option, SupportedLocale loc);

    /** Marker drawn on the produced map. */
    protected abstract MapCursor.Type markerType();

    /* ===================== event flow ===================== */

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!e.getAction().isRightClick()) return;

        Player player = e.getPlayer();
        if (!CustomItems.is(player.getInventory().getItemInMainHand(), triggerItem())) return;

        e.setCancelled(true);
        // Context = the trigger item name, so the biome/explorer selectors stay distinct.
        MapSelectorGUI.open(player, titleKey(), triggerItem().name(), options(), player.getWorld().getEnvironment());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        BingoGuiHolder holder = BingoGuiHolder.of(e.getView().getTopInventory());
        if (holder == null || holder.type() != BingoGuiHolder.Gui.MAP_SELECTOR
                || !triggerItem().name().equals(holder.context())) {
            return;
        }

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        var pdc = clicked.getItemMeta().getPersistentDataContainer();

        if (pdc.has(MapSelectorGUI.cancelKey(), PersistentDataType.BYTE)) {
            player.closeInventory();
            return;
        }

        // Only usable (in-dimension) options carry this tag, so an out-of-dimension
        // pick simply isn't resolvable here.
        String name = pdc.get(MapSelectorGUI.optionKey(), PersistentDataType.STRING);
        if (name == null) return;
        T option = optionByName(name);
        if (option == null) return;

        SupportedLocale loc = Messages.localeOf(player);

        if (!CustomItems.is(player.getInventory().getItemInMainHand(), triggerItem())) {
            player.closeInventory();
            announce(player, Messages.get(loc, "items.map.invalid-head"), Messages.get(loc, "items.map.invalid"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        player.closeInventory();
        draw(player, option, loc);
    }

    /* ===================== map drawing ===================== */

    private void draw(Player player, T option, SupportedLocale loc) {
        World world = player.getWorld();

        // Safety net — the GUI already prevents out-of-dimension picks.
        if (world.getEnvironment() != option.getDimension()) {
            announce(player, Messages.get(loc, "items.map.wrong-dim-head"),
                    Messages.get(loc, "items.map.wrong-dim", "dim", MapSelectorGUI.dimensionName(option.getDimension(), loc)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        Location target = locate(world, player.getLocation(), option);
        if (target == null) {
            announce(player, Messages.get(loc, "items.map.not-found-head"),
                    Messages.get(loc, "items.map.not-found", "name", option.displayName(loc)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        ItemStack map = buildMap(world, target, mapItemName(option, loc), markerType());

        // Consume exactly one map item and hand over the result (no stack wipe).
        consumeOne(player);
        giveOrDrop(player, map);

        double dist = player.getLocation().distance(target);
        announce(player, Messages.get(loc, "items.map.found-head"),
                Messages.get(loc, "items.map.found",
                        "name", option.displayName(loc),
                        "x", target.getBlockX(), "y", target.getBlockY(), "z", target.getBlockZ(),
                        "dist", String.format("%.0f", dist)));
        player.playSound(player.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f, 1.0f);
    }

    private void consumeOne(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!CustomItems.is(hand, triggerItem())) return;

        int amount = hand.getAmount();
        if (amount <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(amount - 1);
        }
    }

    private void giveOrDrop(Player player, ItemStack map) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(map);
        for (ItemStack remaining : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), remaining);
        }
    }

    private static ItemStack buildMap(World world, Location target, String name, MapCursor.Type marker) {
        MapView view = Bukkit.createMap(world);
        view.setCenterX(target.getBlockX());
        view.setCenterZ(target.getBlockZ());
        view.setScale(MapView.Scale.CLOSE);
        view.setTrackingPosition(true);
        view.setUnlimitedTracking(true);

        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        meta.setMapView(view);
        meta.itemName(Component.text(name));
        map.setItemMeta(meta);

        MapDecorations decorations = MapDecorations.mapDecorations()
                .put("target", new MapDecorations.DecorationEntry() {
                    @Override public MapCursor.Type type() { return marker; }
                    @Override public double x() { return target.getBlockX(); }
                    @Override public double z() { return target.getBlockZ(); }
                    @Override public float rotation() { return 180.0f; }
                }).build();
        map.setData(DataComponentTypes.MAP_DECORATIONS, decorations);

        return map;
    }

    /**
     * Feedback goes only to the acting player: a chat line (kept in history for
     * the coordinates) plus a center-screen subtitle headline.
     */
    private void announce(Player player, Component subtitle, Component chat) {
        player.sendMessage(chat);
        player.showTitle(Title.title(Component.empty(), subtitle, TIMES));
    }
}
