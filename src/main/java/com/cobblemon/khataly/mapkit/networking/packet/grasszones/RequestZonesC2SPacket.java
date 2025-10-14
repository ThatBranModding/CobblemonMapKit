package com.cobblemon.khataly.mapkit.networking.packet.grasszones;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestZonesC2SPacket() implements CustomPayload {
    public static final CustomPayload.Id<RequestZonesC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "zones_request_c2s")); // <-- Id<T>

    @Override public Id<? extends CustomPayload> getId() { return ID; }

    public static final PacketCodec<RegistryByteBuf, RequestZonesC2SPacket> CODEC =
            PacketCodec.unit(new RequestZonesC2SPacket());
}
