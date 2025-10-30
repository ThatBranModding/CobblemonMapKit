package com.cobblemon.khataly.mapkit.networking.packet.bike;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client → Server packet to toggle bicycle gear.
 * No extra data is sent; just a trigger action.
 */
public record ToggleBikeGearC2SPacket() implements CustomPayload {

    public static final Id<ToggleBikeGearC2SPacket> ID =
            new Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "toggle_bike_gear_c2s"));

    public static final PacketCodec<RegistryByteBuf, ToggleBikeGearC2SPacket> CODEC =
            PacketCodec.unit(new ToggleBikeGearC2SPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static ToggleBikeGearC2SPacket empty() {
        return new ToggleBikeGearC2SPacket();
    }
}
