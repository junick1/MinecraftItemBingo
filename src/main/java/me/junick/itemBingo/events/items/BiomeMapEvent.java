package me.junick.itemBingo.events.items;

import me.junick.itemBingo.enums.BingoBiome;
import me.junick.itemBingo.enums.BingoItem;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.map.MapCursor;

public class BiomeMapEvent extends MapItemListener<BingoBiome> {
    private static final String TITLE = "§b바이옴 지도 선택";

    // Search bounds for World#locateNearestBiome (radius in blocks, sample steps).
    private static final int SEARCH_RADIUS_BLOCKS = 2000;
    private static final int HORIZONTAL_STEP = 32;
    private static final int VERTICAL_STEP = 32;

    @Override protected BingoItem triggerItem() { return BingoItem.BIOME_MAP; }
    @Override protected String title() { return TITLE; }
    @Override protected BingoBiome[] options() { return BingoBiome.values(); }

    @Override
    protected BingoBiome optionByName(String name) {
        try {
            return BingoBiome.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    protected Location locate(World world, Location from, BingoBiome option) {
        // Runs on the main thread: Bukkit world/chunk access is not thread-safe.
        var res = world.locateNearestBiome(from, SEARCH_RADIUS_BLOCKS, HORIZONTAL_STEP, VERTICAL_STEP, option.getBiome());
        return res == null ? null : res.getLocation();
    }

    @Override protected String mapItemName(BingoBiome option) { return option.getName() + " 바이옴 지도"; }
    @Override protected MapCursor.Type markerType() { return MapCursor.Type.BANNER_LIGHT_BLUE; }
}
