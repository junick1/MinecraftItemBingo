package me.junick.itembingo.client.net.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** {@code itembingo:hello} — C2S handshake and S2C ack share the channel. */
public record HelloPayload(byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HelloPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("itembingo", "hello"));
    public static final StreamCodec<ByteBuf, HelloPayload> CODEC =
            RawBytes.codec(HelloPayload::new, HelloPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
