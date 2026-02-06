package com.cobblemon.khataly.mapkit.networking.packet.localweather;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Client -> Server: sends two corners (a,b) and a weather type string.
 * weatherType examples: "clear" | "rain" | "thunder" | "sandstorm" | "snow"
 */
public record PlaceLocalWeatherC2SPacket(BlockPos a, BlockPos b, String weatherType) implements CustomPayload {

    public static final CustomPayload.Id<PlaceLocalWeatherC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "place_local_weather"));

    public static final PacketCodec<RegistryByteBuf, PlaceLocalWeatherC2SPacket> CODEC =
            PacketCodec.of(
                    // IMPORTANT: (packet, buf) in this order
                    (packet, buf) -> {
                        BlockPos.PACKET_CODEC.encode(buf, packet.a());
                        BlockPos.PACKET_CODEC.encode(buf, packet.b());
                        String wt = (packet.weatherType() == null || packet.weatherType().isBlank())
                                ? "rain"
                                : packet.weatherType();
                        buf.writeString(wt, 32);
                    },
                    buf -> {
                        BlockPos a = BlockPos.PACKET_CODEC.decode(buf);
                        BlockPos b = BlockPos.PACKET_CODEC.decode(buf);
                        String wt = buf.readString(32);
                        return new PlaceLocalWeatherC2SPacket(a, b, wt);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
