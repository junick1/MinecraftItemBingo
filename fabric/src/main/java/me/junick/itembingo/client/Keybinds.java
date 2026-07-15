package me.junick.itembingo.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.junick.itembingo.client.config.ModConfig;
import me.junick.itembingo.client.screen.BingoBoardScreen;
import me.junick.itembingo.client.state.BoardClientState;
import me.junick.itembingo.client.state.BoardClientState.ConnectionState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class Keybinds {
    private Keybinds() {}

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("itembingo", "main"));

    public static KeyMapping openBoard;
    public static KeyMapping toggleHud;

    public static void init() {
        openBoard = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.itembingo.open_board", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
        toggleHud = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.itembingo.toggle_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(Keybinds::tick);
    }

    private static void tick(Minecraft client) {
        if (client.player == null) return;

        while (openBoard.consumeClick()) {
            if (BoardClientState.connection() == ConnectionState.ACTIVE) {
                client.setScreenAndShow(new BingoBoardScreen());
            } else if (client.getConnection() != null) {
                // No plugin (or version mismatch): fall back to the command so
                // the server's chest GUI opens like for any vanilla client.
                client.getConnection().sendCommand("bingo");
            }
        }

        while (toggleHud.consumeClick()) {
            boolean enabled = ModConfig.toggleOverlay();
            client.player.sendOverlayMessage(
                    Component.translatable(enabled ? "itembingo.hud.on" : "itembingo.hud.off"));
        }
    }
}
