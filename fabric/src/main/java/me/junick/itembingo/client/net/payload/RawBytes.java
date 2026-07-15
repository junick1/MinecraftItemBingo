package me.junick.itembingo.client.net.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.Function;

/**
 * Every ItemBingo payload is an opaque {@code byte[]} — the server side speaks
 * Bukkit plugin messages (raw arrays), so the codec here just copies all
 * remaining bytes instead of using any {@code FriendlyByteBuf} conventions.
 */
final class RawBytes {
    private RawBytes() {}

    static <T extends CustomPacketPayload> StreamCodec<ByteBuf, T> codec(
            Function<byte[], T> constructor, Function<T, byte[]> data) {
        return StreamCodec.of(
                (buf, payload) -> buf.writeBytes(data.apply(payload)),
                buf -> {
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    return constructor.apply(bytes);
                });
    }
}
