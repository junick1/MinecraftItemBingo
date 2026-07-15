package me.junick.itembingo.client;

import me.junick.itembingo.client.config.ModConfig;
import me.junick.itembingo.client.screen.BingoBoardScreen;
import me.junick.itembingo.client.state.BoardClientState;
import me.junick.itembingo.client.state.BoardClientState.ConnectionState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;

/**
 * Optional /bingo override: when enabled (config, or the toggle in the board
 * screen) and the handshake is ACTIVE, typing {@code /bingo} opens the mod's
 * board instead of reaching the server. The command is cancelled client-side;
 * the screen opens on the next tick so the chat screen finishes closing first.
 * With the toggle off — or on servers without the plugin — /bingo passes
 * through untouched.
 */
public final class CommandOverride {
    private CommandOverride() {}

    private static boolean pendingOpen;

    public static void init() {
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            if (command.trim().equals("bingo")
                    && ModConfig.overrideBingo()
                    && BoardClientState.connection() == ConnectionState.ACTIVE) {
                pendingOpen = true;
                return false;
            }
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(CommandOverride::tick);
    }

    private static void tick(Minecraft client) {
        if (pendingOpen) {
            pendingOpen = false;
            if (client.player != null) {
                client.setScreenAndShow(new BingoBoardScreen());
            }
        }
    }
}
