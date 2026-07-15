package me.junick.itembingo.client.net;

import me.junick.itembingo.client.export.BoardImageExporter;
import me.junick.itembingo.client.net.payload.BoardPayload;
import me.junick.itembingo.client.net.payload.HelloPayload;
import me.junick.itembingo.client.net.payload.OriginalPayload;
import me.junick.itembingo.client.net.payload.RefreshPayload;
import me.junick.itembingo.client.net.payload.SubmitPayload;
import me.junick.itembingo.client.state.BoardClientState;
import me.junick.itembingo.client.state.BoardClientState.ConnectionState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.chat.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Handshake and payload wiring. The server's plugin registers its channels via
 * {@code minecraft:register}, which fires {@link ServerboundPlayChannelEvents}
 * REGISTER here — that's our cue to say hello. A server that never announces
 * the channels within {@link #HELLO_TIMEOUT_TICKS} of joining is treated as
 * unsupported (vanilla or plugin-less), and the mod quietly stays dormant.
 */
public final class ClientNetworking {
    private ClientNetworking() {}

    private static final int HELLO_TIMEOUT_TICKS = 100;
    private static int helloTimeout = -1;

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SubmitPayload.TYPE, SubmitPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RefreshPayload.TYPE, RefreshPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BoardPayload.TYPE, BoardPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OriginalPayload.TYPE, OriginalPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OriginalPayload.TYPE, OriginalPayload.CODEC);

        // Registering these receivers also makes Fabric announce the S2C
        // channels to the server, which Bukkit requires before it may send.
        ClientPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE,
                (payload, context) -> handleHelloAck(payload.data()));
        ClientPlayNetworking.registerGlobalReceiver(BoardPayload.TYPE,
                (payload, context) -> BoardClientState.applyBoardPacket(payload.data()));
        ClientPlayNetworking.registerGlobalReceiver(OriginalPayload.TYPE,
                (payload, context) -> BoardImageExporter.handleOriginalResponse(payload.data()));

        ServerboundPlayChannelEvents.REGISTER.register((listener, sender, client, channels) -> {
            if (BoardClientState.connection() == ConnectionState.UNKNOWN
                    && channels.contains(HelloPayload.TYPE.id())) {
                sendHello();
            }
        });

        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> {
            BoardClientState.clear();
            helloTimeout = HELLO_TIMEOUT_TICKS;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
            BoardClientState.clear();
            helloTimeout = -1;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (helloTimeout > 0 && --helloTimeout == 0
                    && BoardClientState.connection() == ConnectionState.UNKNOWN) {
                BoardClientState.setConnection(ConnectionState.UNSUPPORTED, -1);
            }
        });
    }

    private static void sendHello() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(ModProtocol.PROTOCOL_VERSION);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (ClientPlayNetworking.canSend(HelloPayload.TYPE)) {
            ClientPlayNetworking.send(new HelloPayload(bytes.toByteArray()));
        }
    }

    private static void handleHelloAck(byte[] data) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            int serverVersion = in.readInt();
            boolean accepted = in.readBoolean();
            if (accepted) {
                BoardClientState.setConnection(ConnectionState.ACTIVE, serverVersion);
            } else {
                BoardClientState.setConnection(ConnectionState.MISMATCH, serverVersion);
                var player = net.minecraft.client.Minecraft.getInstance().player;
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("itembingo.status.mismatch",
                            ModProtocol.PROTOCOL_VERSION, serverVersion));
                }
            }
        } catch (IOException e) {
            // Garbled ack: leave the state as-is; the timeout will mark UNSUPPORTED.
        }
    }

    /**
     * Requests a submission. {@code mode} is {@code ModProtocol.SUBMIT_*};
     * {@code cellIndex} is -1 for SHIFT. {@code expectedItemKey} names the item
     * the client believes sits in {@code inventorySlot} so the server can
     * detect a stale view instead of consuming the wrong thing.
     */
    public static void sendSubmit(byte mode, int cellIndex, int inventorySlot, String expectedItemKey) {
        if (BoardClientState.connection() != ConnectionState.ACTIVE) return;
        if (!ClientPlayNetworking.canSend(SubmitPayload.TYPE)) return;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(mode);
            out.writeInt(cellIndex);
            out.writeByte(inventorySlot);
            out.writeUTF(expectedItemKey);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ClientPlayNetworking.send(new SubmitPayload(bytes.toByteArray()));
    }

    public static void sendRefresh() {
        if (BoardClientState.connection() != ConnectionState.ACTIVE) return;
        if (!ClientPlayNetworking.canSend(RefreshPayload.TYPE)) return;
        ClientPlayNetworking.send(new RefreshPayload(new byte[0]));
    }

    /** Asks for the pristine board (image export); answered on itembingo:original. */
    public static void sendOriginalRequest() {
        if (BoardClientState.connection() != ConnectionState.ACTIVE) return;
        if (!ClientPlayNetworking.canSend(OriginalPayload.TYPE)) return;
        ClientPlayNetworking.send(new OriginalPayload(new byte[0]));
    }
}
