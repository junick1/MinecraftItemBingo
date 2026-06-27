package me.junick.itemBingo.i18n;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The languages ItemBingo ships with. To add another language, add a constant
 * here and a matching {@code lang/<id>.yml} resource — nothing else needs to
 * change.
 *
 * <p>{@link #EN_US} is the {@link #DEFAULT}: it is the complete fallback bundle
 * (every key must exist in it) and is used for console output and for any client
 * whose language is neither English nor Korean.</p>
 */
public enum SupportedLocale {
    EN_US("en_us"),
    KO_KR("ko_kr");

    /** Fallback locale: complete bundle, used for console and unknown client locales. */
    public static final SupportedLocale DEFAULT = EN_US;

    private final String id;

    SupportedLocale(String id) {
        this.id = id;
    }

    /** Resource id, e.g. {@code "en_us"} maps to {@code lang/en_us.yml}. */
    public String id() {
        return id;
    }

    /**
     * Best-effort match for a client locale, by language only: any Korean variant
     * ({@code ko}, {@code ko_KR}, …) resolves to {@link #KO_KR}; everything else
     * (including {@code null}) falls back to {@link #DEFAULT}.
     */
    public static SupportedLocale fromClient(@Nullable Locale locale) {
        if (locale != null && "ko".equalsIgnoreCase(locale.getLanguage())) {
            return KO_KR;
        }
        return DEFAULT;
    }

    /** Parses a stored/typed id (case-insensitive); {@code null} if unrecognized. */
    public static @Nullable SupportedLocale fromId(@Nullable String id) {
        if (id == null) return null;
        for (SupportedLocale loc : values()) {
            if (loc.id.equalsIgnoreCase(id)) return loc;
        }
        return null;
    }
}
