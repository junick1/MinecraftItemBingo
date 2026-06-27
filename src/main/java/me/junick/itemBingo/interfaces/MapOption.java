package me.junick.itemBingo.interfaces;

import me.junick.itemBingo.i18n.SupportedLocale;
import org.bukkit.Material;
import org.bukkit.World.Environment;

/**
 * Something a player can pick in a map-selector GUI (a biome or a structure).
 * Implemented by the catalog enums so the selector GUI and listener can be
 * written once, generically, instead of duplicated per map type.
 */
public interface MapOption {
    /** Localized display name. */
    String displayName(SupportedLocale loc);

    /** Icon shown in the selector GUI. */
    Material getIcon();

    /** Dimension this option can be located in. */
    Environment getDimension();
}
