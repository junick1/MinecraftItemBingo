package me.junick.itembingo.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain-JSON client config ({@code config/itembingo.json}), edited by hand or
 * via the HUD toggle keybind. Missing or corrupt files fall back to defaults
 * and are rewritten.
 */
public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data data = new Data();

    public enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    /** Serialized shape of the config file. */
    public static final class Data {
        public boolean overlayEnabled = true;
        public Corner corner = Corner.TOP_RIGHT;
        public int offsetX = 4;
        public int offsetY = 4;
        public float scale = 1.0f;
        public int maxGridWidth = 7;
        public int maxGridHeight = 7;
    }

    private ModConfig() {}

    public static boolean overlayEnabled() { return data.overlayEnabled; }
    public static Corner corner() { return data.corner == null ? Corner.TOP_RIGHT : data.corner; }
    public static int offsetX() { return data.offsetX; }
    public static int offsetY() { return data.offsetY; }
    public static float scale() { return Math.clamp(data.scale, 0.5f, 3.0f); }
    public static int maxGridWidth() { return Math.clamp(data.maxGridWidth, 1, 15); }
    public static int maxGridHeight() { return Math.clamp(data.maxGridHeight, 1, 15); }

    public static boolean toggleOverlay() {
        data.overlayEnabled = !data.overlayEnabled;
        save();
        return data.overlayEnabled;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("itembingo.json");
    }

    public static void load() {
        Path file = file();
        if (Files.exists(file)) {
            try {
                Data loaded = GSON.fromJson(Files.readString(file), Data.class);
                if (loaded != null) {
                    data = loaded;
                    return;
                }
            } catch (IOException | com.google.gson.JsonParseException e) {
                // fall through: rewrite with defaults
            }
        }
        data = new Data();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(data));
        } catch (IOException e) {
            // Non-fatal: the config just won't persist this session.
        }
    }
}
