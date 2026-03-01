package me.junick.itemBingo.enums;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum BingoItem {
    BINGO_FILLER(
            "빙고 제출권",
            NamedTextColor.LIGHT_PURPLE,
            Material.LIGHT_BLUE_DYE,
            Map.of(BingoRewardType.LINE, 5),
            List.of("빙고칸 중 하나를 즉시 제출합니다.")
    ),
    DYE_SELECTOR(
            "염료 획득권",
            NamedTextColor.GREEN,
            Material.COMMAND_BLOCK,
            Map.of(BingoRewardType.DIAMOND, 5),
            List.of("염료를 하나 선택해 획득합니다.")
    ),
    COPPER_OXIDIZER(
            "구리 산화제",
            NamedTextColor.GOLD,
            Material.SUGAR,
            Map.of(BingoRewardType.DIAMOND, 1),
            List.of("밀랍칠하지 않은 구리류 블록에 우클릭하면 한 단계 산화시킵니다.", "구리 골렘에 우클릭하면 즉시 동상으로 변환시킵니다.")
    );

    private final String display;
    private final TextColor color;
    private final Material icon;
    private final Map<BingoRewardType, Integer> prices;
    private final List<String> lore;
    public ItemStack apply(ItemStack item) { return item; }

    BingoItem(String display, TextColor color, Material icon, Map<BingoRewardType, Integer> prices, List<String> lore) {
        this.display = display;
        this.color = color;
        this.icon = icon;
        this.prices = new EnumMap<>(prices);
        this.lore = lore;
    }

    public String getDisplay() { return display; }
    public TextColor getColor() { return color; }
    public Material getIcon() { return icon; }
    public List<String> getLore() { return lore; }

    public int getPrice(BingoRewardType type) {
        return prices.getOrDefault(type, 0);
    }

    public Map<BingoRewardType, Integer> getAllPrices() {
        return Map.copyOf(prices);
    }
}
