package com.cobblemon.khataly.mapkit.networking.packet.localweather;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client asks server for current local weather zones list. */
public record RequestLocalWeatherZonesC2SPacket() implements CustomPayload {

    public static final CustomPayload.Id<RequestLocalWeatherZonesC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "request_local_weather_zones"));

    public static final PacketCodec<RegistryByteBuf, RequestLocalWeatherZonesC2SPacket> CODEC =
            PacketCodec.of((buf, pkt) -> {}, buf -> new RequestLocalWeatherZonesC2SPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
