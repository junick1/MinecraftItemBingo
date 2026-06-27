package me.junick.itemBingo.enums;

import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum BingoItem {
    DIAMOND(
            NamedTextColor.AQUA,
            Material.DIAMOND,
            Map.of(BingoRewardType.LINE, 1)
    ),
    BINGO_FILLER(
            NamedTextColor.LIGHT_PURPLE,
            Material.LIGHT_BLUE_DYE,
            Map.of(BingoRewardType.LINE, 15)
    ),
    DYE_SELECTOR(
            NamedTextColor.GREEN,
            Material.COMMAND_BLOCK,
            Map.of(BingoRewardType.DIAMOND, 5)
    ),
    COPPER_OXIDIZER(
            NamedTextColor.GOLD,
            Material.SUGAR,
            Map.of(BingoRewardType.DIAMOND, 1)
    ),
    EXPLORER_MAP(
            NamedTextColor.GREEN,
            Material.PAPER,
            Map.of(BingoRewardType.DIAMOND, 10)
    ) {
        @Override
        public ItemStack apply(ItemStack item) {
            var meta = item.getItemMeta();
            meta.setMaxStackSize(1);
            item.setItemMeta(meta);
            return item;
        }
    },
    BIOME_MAP(
            NamedTextColor.GREEN,
            Material.PAPER,
            Map.of(BingoRewardType.DIAMOND, 10)
    ) {
        @Override
        public ItemStack apply(ItemStack item) {
            var meta = item.getItemMeta();
            meta.setMaxStackSize(1);
            item.setItemMeta(meta);
            return item;
        }
    };

    private final TextColor color;
    private final Material icon;
    private final Map<BingoRewardType, Integer> prices;
    public ItemStack apply(ItemStack item) { return item; }

    BingoItem(TextColor color, Material icon, Map<BingoRewardType, Integer> prices) {
        this.color = color;
        this.icon = icon;
        this.prices = new EnumMap<>(prices);
    }

    /** Message-key stem, e.g. {@code diamond} → {@code item.diamond.name} / {@code item.diamond.lore}. */
    public String key() { return name().toLowerCase(Locale.ROOT); }

    /** Localized display name (plain text; the name color is {@link #getColor()}). */
    public String displayName(SupportedLocale loc) { return Messages.legacy(loc, "item." + key() + ".name"); }

    /** Localized lore lines (plain text; callers color them). */
    public List<String> lore(SupportedLocale loc) { return Messages.legacyList(loc, "item." + key() + ".lore"); }

    public TextColor getColor() { return color; }
    public Material getIcon() { return icon; }

    public int getPrice(BingoRewardType type) {
        return prices.getOrDefault(type, 0);
    }

    public Map<BingoRewardType, Integer> getAllPrices() {
        return Map.copyOf(prices);
    }
}
