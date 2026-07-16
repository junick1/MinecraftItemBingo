package me.junick.itembingo.client.net.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** {@code itembingo:submit_ack} — S2C immediate accept/reject for a submit. */
public record SubmitAckPayload(byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SubmitAckPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("itembingo", "submit_ack"));
    public static final StreamCodec<ByteBuf, SubmitAckPayload> CODEC =
            RawBytes.codec(SubmitAckPayload::new, SubmitAckPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
