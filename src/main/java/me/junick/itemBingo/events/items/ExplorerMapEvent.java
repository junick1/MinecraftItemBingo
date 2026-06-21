package me.junick.itemBingo.events.items;

import me.junick.itemBingo.enums.BingoItem;
import me.junick.itemBingo.enums.BingoStructure;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.map.MapCursor;

public class ExplorerMapEvent extends MapItemListener<BingoStructure> {
    private static final String TITLE = "§b지도 선택";

    // Search radius for World#locateNearestStructure, in chunks.
    private static final int SEARCH_RADIUS_CHUNKS = 100;

    @Override protected BingoItem triggerItem() { return BingoItem.EXPLORER_MAP; }
    @Override protected String title() { return TITLE; }
    @Override protected BingoStructure[] options() { return BingoStructure.values(); }

    @Override
    protected BingoStructure optionByName(String name) {
        try {
            return BingoStructure.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    protected Location locate(World world, Location from, BingoStructure option) {
        // Runs on the main thread: Bukkit world/chunk access is not thread-safe.
        var res = world.locateNearestStructure(from, option.getStructure(), SEARCH_RADIUS_CHUNKS, false);
        return res == null ? null : res.getLocation();
    }

    @Override protected String mapItemName(BingoStructure option) { return option.getName() + " 탐험가 지도"; }
    @Override protected MapCursor.Type markerType() { return MapCursor.Type.BANNER_RED; }
}
