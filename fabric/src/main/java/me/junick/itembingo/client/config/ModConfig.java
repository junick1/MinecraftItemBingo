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
        // TOP_LEFT by default: the top-right corner is where vanilla stacks
        // status-effect icons, which are common mid-game.
        public Corner corner = Corner.TOP_LEFT;
        public int offsetX = 4;
        public int offsetY = 4;
        public float scale = 1.0f;
        public int maxGridWidth = 7;
        public int maxGridHeight = 7;
        /** Overlay board viewport in camera units; -1 = derive from maxGrid*. */
        public int overlayWidth = -1;
        public int overlayHeight = -1;
        public boolean overlayLocked = false;
        public boolean overlayHintShown = false;
        public boolean overrideBingoCommand = true;
    }

    private ModConfig() {}

    public static boolean overlayEnabled() { return data.overlayEnabled; }
    public static Corner corner() { return data.corner == null ? Corner.TOP_LEFT : data.corner; }
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

    /** Whether typing /bingo opens the mod board instead of the server GUI. */
    public static boolean overrideBingo() { return data.overrideBingoCommand; }

    /** Persists the overlay position after a drag (snapped to a corner). */
    public static void setPosition(Corner corner, int offsetX, int offsetY) {
        data.corner = corner;
        data.offsetX = Math.max(0, offsetX);
        data.offsetY = Math.max(0, offsetY);
        save();
    }

    /** Overlay board viewport width in camera units (66 = 3 cells minimum). */
    public static int overlayWidth() {
        int w = data.overlayWidth > 0 ? data.overlayWidth : maxGridWidth() * 22;
        return Math.clamp(w, 66, 1320);
    }

    public static int overlayHeight() {
        int h = data.overlayHeight > 0 ? data.overlayHeight : maxGridHeight() * 22;
        return Math.clamp(h, 66, 1320);
    }

    public static void setOverlaySize(int width, int height) {
        data.overlayWidth = Math.clamp(width, 66, 1320);
        data.overlayHeight = Math.clamp(height, 66, 1320);
        save();
    }

    public static boolean overlayLocked() { return data.overlayLocked; }

    public static boolean toggleOverlayLocked() {
        data.overlayLocked = !data.overlayLocked;
        save();
        return data.overlayLocked;
    }

    /** One-time overlay gesture hint: shown until the player interacts once. */
    public static boolean overlayHintShown() { return data.overlayHintShown; }

    public static void markOverlayHintShown() {
        if (!data.overlayHintShown) {
            data.overlayHintShown = true;
            save();
        }
    }

    public static boolean toggleOverrideBingo() {
        data.overrideBingoCommand = !data.overrideBingoCommand;
        save();
        return data.overrideBingoCommand;
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
