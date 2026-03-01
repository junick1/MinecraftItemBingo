package me.junick.itemBingo.enums;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public enum BingoItemTag {
    COLOR(NamedTextColor.AQUA, "★ 색상 아이템"),
    SILK_TOUCH(NamedTextColor.LIGHT_PURPLE, "⛏ 섬세한 손길 아이템"),
    THE_END(NamedTextColor.DARK_PURPLE, "⏣ 엔드 아이템"),
    SMITHING_TEMPLATE(NamedTextColor.GOLD, "⚒ 대장장이 형판 아이템"),
    THE_NETHER(NamedTextColor.DARK_RED, "💢 네더 아이템"),
    COPPER(TextColor.fromHexString("#C57255"), "🐱 산화되는 구리 아이템"),
    WAXED(TextColor.fromHexString("#F7DB28"), "🍯 밀랍칠한 아이템"),
    DISC(TextColor.fromHexString("#03404F"), "💿 음반 아이템"),
    POTTERY_SHERD(TextColor.fromHexString("#9D5848"), "🍶 도자기 조각 아이템");

    private final TextColor color;
    private final String display;

    BingoItemTag(TextColor color, String display) {
        this.color = color;
        this.display = display;
    }

    public TextColor color() {
        return color;
    }

    public String display() {
        return display;
    }

    public Component component() {
        return Component.text(display, color)
                .decoration(TextDecoration.ITALIC, false);
    }

    public Component bullet() {
        return Component.text("• ", NamedTextColor.GRAY)
                .append(component())
                .decoration(TextDecoration.ITALIC, false);
    }
}
