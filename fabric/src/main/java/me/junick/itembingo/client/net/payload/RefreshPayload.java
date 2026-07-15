package me.junick.itembingo.client.net.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** {@code itembingo:refresh} — C2S request for a fresh board push (empty body). */
public record RefreshPayload(byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RefreshPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("itembingo", "refresh"));
    public static final StreamCodec<ByteBuf, RefreshPayload> CODEC =
            RawBytes.codec(RefreshPayload::new, RefreshPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
