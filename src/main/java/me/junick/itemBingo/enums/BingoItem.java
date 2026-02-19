package me.junick.itemBingo.enums;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

public enum BingoItem {
    BINGO_FILLER(
            "§d빙고 제출권",
            Material.LIGHT_BLUE_DYE,
            Map.of(BingoRewardType.LINE, 5),
            "빙고칸 중 하나를 즉시 제출합니다."
    ),
    DYE_SELECTOR(
            "§a염료 획득권",
            Material.COMMAND_BLOCK,
            Map.of(BingoRewardType.DIAMOND, 5),
            "염료를 하나 선택해 획득합니다."
    );

    private final String display;
    private final Material icon;
    private final Map<BingoRewardType, Integer> prices;
    private final String[] lore;

    BingoItem(String display, Material icon, Map<BingoRewardType, Integer> prices, String... lore) {
        this.display = display;
        this.icon = icon;
        this.prices = new EnumMap<>(prices);
        this.lore = lore;
    }

    public String getDisplay() { return display; }
    public Material getIcon() { return icon; }
    public String[] getLore() { return lore; }

    public int getPrice(BingoRewardType type) {
        return prices.getOrDefault(type, 0);
    }

    public Map<BingoRewardType, Integer> getAllPrices() {
        return Map.copyOf(prices);
    }
}
