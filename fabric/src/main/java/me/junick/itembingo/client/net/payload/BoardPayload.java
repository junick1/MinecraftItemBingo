package me.junick.itembingo.client.net.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** {@code itembingo:board} — S2C full per-viewer board view. */
public record BoardPayload(byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BoardPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("itembingo", "board"));
    public static final StreamCodec<ByteBuf, BoardPayload> CODEC =
            RawBytes.codec(BoardPayload::new, BoardPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
