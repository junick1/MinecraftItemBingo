package me.junick.itembingo.client.net.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** {@code itembingo:submit} — C2S submission request. */
public record SubmitPayload(byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SubmitPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("itembingo", "submit"));
    public static final StreamCodec<ByteBuf, SubmitPayload> CODEC =
            RawBytes.codec(SubmitPayload::new, SubmitPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
