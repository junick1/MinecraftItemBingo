package me.junick.itembingo.client.state;

import me.junick.itembingo.client.net.ModProtocol;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * One board cell as the server allowed this viewer to see it.
 *
 * @param kind          one of {@code ModProtocol.CELL_*}
 * @param item          resolved item for VISIBLE/SUBMITTED cells; {@code null}
 *                      for HIDDEN/LOCKED or when the key didn't resolve
 * @param rawItemKey    the wire item key, kept for a fallback tooltip when the
 *                      client couldn't resolve it (registry skew)
 * @param hasSubmitter  SUBMITTED only: whether a submitter is known (team play)
 * @param submitterName SUBMITTED only: display name of the submitter, or ""
 * @param elapsedSeconds SUBMITTED only: game time at submission, -1 if unknown
 */
public record CellState(byte kind, @Nullable Item item, @Nullable String rawItemKey,
                        boolean hasSubmitter, String submitterName, long elapsedSeconds) {

    public static final CellState HIDDEN = new CellState(ModProtocol.CELL_HIDDEN, null, null, false, "", -1);
    public static final CellState LOCKED = new CellState(ModProtocol.CELL_LOCKED, null, null, false, "", -1);

    public static CellState visible(String itemKey) {
        return new CellState(ModProtocol.CELL_VISIBLE, resolve(itemKey), itemKey, false, "", -1);
    }

    public static CellState submitted(String itemKey, boolean hasSubmitter, String submitterName, long elapsedSeconds) {
        return new CellState(ModProtocol.CELL_SUBMITTED, resolve(itemKey), itemKey, hasSubmitter, submitterName, elapsedSeconds);
    }

    @Nullable
    private static Item resolve(String key) {
        Identifier id = Identifier.tryParse(key);
        if (id == null) return null;
        Item item = BuiltInRegistries.ITEM.getValue(id);
        // The item registry is defaulted: unknown keys resolve to air. A board
        // never contains air, so air here means "this client doesn't know the
        // item" (server/client registry skew) — fall back to the raw key.
        return item == Items.AIR ? null : item;
    }

    public boolean isSubmitted() {
        return kind == ModProtocol.CELL_SUBMITTED;
    }

    public boolean isVisible() {
        return kind == ModProtocol.CELL_VISIBLE;
    }
}
