package me.junick.itemBingo.enums;

import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.w3c.dom.Text;

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
    },
    MYSTERIOUS_SHERD(
            TextColor.fromHexString("#9D5848"),
            Material.BRICK,
            Map.of(BingoRewardType.DIAMOND, 7)
    ),
    RED_PLATE1(
            TextColor.color(0, 0, 0),
            Material.RED_STAINED_GLASS_PANE,
            Map.of(BingoRewardType.DIAMOND, 0)
    ),
    RED_PLATE2(
            TextColor.color(0, 0, 0),
            Material.RED_STAINED_GLASS_PANE,
            Map.of(BingoRewardType.DIAMOND, 0)
    ),
    GOLDEN_PICKAXE(
            NamedTextColor.GOLD,
            Material.GOLDEN_PICKAXE,
            Map.of(BingoRewardType.EMERALD, 3)
    ),
    IRON_PICKAXE(
            NamedTextColor.GRAY,
            Material.IRON_PICKAXE,
            Map.of(BingoRewardType.EMERALD, 10)
    ),
    HONEY_FILLER(
            NamedTextColor.GOLD,
            Material.GOLD_NUGGET,
            Map.of(BingoRewardType.EMERALD, 3)
    ),
    BEEHIVE_BREAKER(
            NamedTextColor.GOLD,
            Material.WOODEN_HOE,
            Map.of(BingoRewardType.EMERALD, 0)
    ),
    EGG_QOL(
            NamedTextColor.DARK_RED,
            Material.BOWL,
            Map.of(BingoRewardType.EMERALD, 3)
    ),
    KNOWLEDGE_BOOK(
            NamedTextColor.YELLOW,
            Material.KNOWLEDGE_BOOK,
            Map.of(BingoRewardType.EMERALD, 10)
    );

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
