package me.junick.itemBingo.enums;

import me.junick.itemBingo.i18n.Messages;
import me.junick.itemBingo.i18n.SupportedLocale;

import java.util.Locale;

public enum BingoRewardType {
    DIAMOND(false),
    SLOT(true),
    LINE(false);

    private final boolean isPersonal;

    BingoRewardType(boolean isPersonal) {
        this.isPersonal = isPersonal;
    }

    /** Message-key stem, e.g. {@code diamond} → {@code reward.diamond.name}. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Localized, color-coded display name (legacy §) for the given locale. */
    public String displayName(SupportedLocale loc) {
        return Messages.legacy(loc, "reward." + key() + ".name");
    }

    public boolean isPersonal() { return isPersonal; }
}
