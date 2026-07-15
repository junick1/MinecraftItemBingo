package me.junick.itembingo.client.net.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** {@code itembingo:original} — C2S empty request / S2C markless board for export. */
public record OriginalPayload(byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OriginalPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("itembingo", "original"));
    public static final StreamCodec<ByteBuf, OriginalPayload> CODEC =
            RawBytes.codec(OriginalPayload::new, OriginalPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
