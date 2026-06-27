package me.junick.itemBingo.enums;

import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Locale;

public enum BingoItemTag {
    COLOR(NamedTextColor.AQUA),
    SILK_TOUCH(NamedTextColor.LIGHT_PURPLE),
    THE_END(NamedTextColor.DARK_PURPLE),
    SMITHING_TEMPLATE(NamedTextColor.GOLD),
    THE_NETHER(NamedTextColor.DARK_RED),
    COPPER(TextColor.fromHexString("#C57255")),
    WAXED(TextColor.fromHexString("#F7DB28")),
    DISC(TextColor.fromHexString("#03404F")),
    POTTERY_SHERD(TextColor.fromHexString("#9D5848"));

    private final TextColor color;

    BingoItemTag(TextColor color) {
        this.color = color;
    }

    /** Message-key stem, e.g. {@code color} → {@code tag.color.name}. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public TextColor color() {
        return color;
    }

    /** Localized label (plain text incl. the leading symbol; the color is {@link #color()}). */
    public String display(SupportedLocale loc) {
        return Messages.legacy(loc, "tag." + key() + ".name");
    }

    public Component component(SupportedLocale loc) {
        return Component.text(display(loc), color)
                .decoration(TextDecoration.ITALIC, false);
    }

    public Component bullet(SupportedLocale loc) {
        return Component.text("• ", NamedTextColor.GRAY)
                .append(component(loc))
                .decoration(TextDecoration.ITALIC, false);
    }
}
