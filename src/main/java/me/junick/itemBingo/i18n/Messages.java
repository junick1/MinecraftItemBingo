package me.junick.itemBingo.i18n;

import me.junick.itemBingo.config.Settings;
import me.junick.itemBingo.util.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central message store for the plugin's user-facing text. Strings live in
 * {@code lang/<id>.yml} bundles (one per {@link SupportedLocale}); look-ups are
 * resolved against the recipient's locale and returned as Adventure
 * {@link Component}s (or legacy {@code §} {@link String}s where an old API needs
 * one).
 *
 * <h2>Locale resolution</h2>
 * {@link #localeOf(CommandSender)} picks: a persisted per-player override
 * ({@link LanguageManager}) → the player's client locale ({@link Player#locale()})
 * → {@link SupportedLocale#DEFAULT}. A missing key falls back to the default
 * bundle, then to the key text itself (so an untranslated key is visible in-game).
 *
 * <h2>Placeholders</h2>
 * Values are supplied as alternating {@code name, value} pairs and substituted by
 * name, so each locale's template controls word order. Two placeholder forms are
 * recognized:
 * <ul>
 *   <li>{@code {name}} — replaced with the value of {@code name}.</li>
 *   <li>{@code {name:withBatchim/withoutBatchim}} — a Korean particle (조사) chosen
 *       from the value's final 받침 via {@link Particle}, e.g. {@code {item:을/를}}.</li>
 * </ul>
 * Plain {@code {name}} replacement is used rather than {@link java.text.MessageFormat}
 * to avoid its apostrophe/brace quoting pitfalls.
 */
public final class Messages {
    private Messages() {}

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    /** {@code {name}} or {@code {name:withBatchim/withoutBatchim}}. */
    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{([A-Za-z0-9_]+)(?::([^/}]+)/([^}]+))?}");

    private static final Map<SupportedLocale, YamlConfiguration> BUNDLES = new EnumMap<>(SupportedLocale.class);

    /**
     * Loads every {@code lang/<id>.yml} bundled in the jar, then overlays a
     * same-named file from the plugin data folder if present (lets server owners
     * edit or add translations without rebuilding).
     */
    public static void init(JavaPlugin plugin) {
        BUNDLES.clear();
        for (SupportedLocale loc : SupportedLocale.values()) {
            YamlConfiguration cfg = new YamlConfiguration();

            try (InputStream in = plugin.getResource("lang/" + loc.id() + ".yml")) {
                if (in != null) {
                    try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        cfg = YamlConfiguration.loadConfiguration(r);
                    }
                }
            } catch (IOException ignored) {
                // empty bundle; raw() will fall back to default/key
            }

            File external = new File(plugin.getDataFolder(), "lang/" + loc.id() + ".yml");
            if (external.exists()) {
                YamlConfiguration overlay = YamlConfiguration.loadConfiguration(external);
                for (String key : overlay.getKeys(true)) {
                    if (!overlay.isConfigurationSection(key)) {
                        cfg.set(key, overlay.get(key));
                    }
                }
            }

            BUNDLES.put(loc, cfg);
        }
    }

    /**
     * Override → client locale (when {@code language.followClient}) → configured
     * default. Console (and any non-player) gets the configured default.
     */
    public static SupportedLocale localeOf(CommandSender who) {
        if (who instanceof Player p) {
            SupportedLocale override = LanguageManager.getOverride(p.getUniqueId());
            if (override != null) return override;
            if (Settings.isFollowClient()) {
                java.util.Locale client = p.locale();
                if (client != null && "ko".equalsIgnoreCase(client.getLanguage())) {
                    return SupportedLocale.KO_KR;
                }
            }
            return Settings.getDefaultLanguage();
        }
        return Settings.getDefaultLanguage();
    }

    /* ===================== Component ===================== */

    public static Component get(CommandSender who, String key, Object... args) {
        return get(localeOf(who), key, args);
    }

    public static Component get(SupportedLocale loc, String key, Object... args) {
        return LEGACY.deserialize(format(raw(loc, key), args));
    }

    /* ===================== Legacy String ===================== */

    /** Legacy {@code §} string, for APIs that still take a String (inventory titles, scoreboard). */
    public static String legacy(CommandSender who, String key, Object... args) {
        return format(raw(localeOf(who), key), args);
    }

    public static String legacy(SupportedLocale loc, String key, Object... args) {
        return format(raw(loc, key), args);
    }

    /* ===================== Lore lists ===================== */

    public static List<Component> getList(CommandSender who, String key, Object... args) {
        return getList(localeOf(who), key, args);
    }

    public static List<Component> getList(SupportedLocale loc, String key, Object... args) {
        List<String> lines = rawList(loc, key);
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) out.add(LEGACY.deserialize(format(line, args)));
        return out;
    }

    public static List<String> legacyList(SupportedLocale loc, String key, Object... args) {
        List<String> lines = rawList(loc, key);
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) out.add(format(line, args));
        return out;
    }

    /* ===================== Internals ===================== */

    private static String raw(SupportedLocale loc, String key) {
        YamlConfiguration cfg = BUNDLES.get(loc);
        if (cfg != null) {
            String v = cfg.getString(key);
            if (v != null) return v;
        }
        if (loc != SupportedLocale.DEFAULT) {
            YamlConfiguration def = BUNDLES.get(SupportedLocale.DEFAULT);
            if (def != null) {
                String v = def.getString(key);
                if (v != null) return v;
            }
        }
        return key; // surface the missing key so it gets noticed
    }

    private static List<String> rawList(SupportedLocale loc, String key) {
        YamlConfiguration cfg = BUNDLES.get(loc);
        if (cfg != null && cfg.isList(key)) return cfg.getStringList(key);
        if (loc != SupportedLocale.DEFAULT) {
            YamlConfiguration def = BUNDLES.get(SupportedLocale.DEFAULT);
            if (def != null && def.isList(key)) return def.getStringList(key);
        }
        String single = raw(loc, key);
        return single.equals(key) ? List.of() : List.of(single);
    }

    private static String format(String template, Object... args) {
        if (template == null) return "";
        if (args == null || args.length == 0 || template.indexOf('{') < 0) return template;

        Map<String, String> values = new HashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            if (args[i] != null) values.put(args[i].toString(), String.valueOf(args[i + 1]));
        }

        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String replacement;
            if (m.group(2) != null) { // particle filter {noun:withBatchim/withoutBatchim}
                String noun = values.get(name);
                replacement = (noun == null) ? m.group() : Particle.pick(noun, m.group(2), m.group(3));
            } else {                  // value {name}
                String value = values.get(name);
                replacement = (value == null) ? m.group() : value;
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
