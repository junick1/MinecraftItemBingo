package me.junick.itembingo.client;

import me.junick.itembingo.client.config.ModConfig;
import me.junick.itembingo.client.export.BoardImageExporter;
import me.junick.itembingo.client.hud.BoardHudOverlay;
import me.junick.itembingo.client.net.ClientNetworking;
import net.fabricmc.api.ClientModInitializer;

public class ItemBingoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModConfig.load();
        ClientNetworking.init();
        Keybinds.init();
        BoardHudOverlay.register();
        BoardImageExporter.init();
        CommandOverride.init();
    }
}
