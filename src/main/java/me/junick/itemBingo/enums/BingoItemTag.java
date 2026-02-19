package me.junick.itemBingo.enums;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public enum BingoItemTag {
    COLOR(NamedTextColor.AQUA, "★ 색상 아이템"),
    SILK_TOUCH(NamedTextColor.LIGHT_PURPLE, "⛏ 섬세한 손길 아이템"),
    THE_END(NamedTextColor.DARK_PURPLE, "⏣ 엔드 아이템"),
    SMITHING_TEMPLATE(NamedTextColor.GOLD, "⚒ 대장장이 형판 아이템"),;

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
