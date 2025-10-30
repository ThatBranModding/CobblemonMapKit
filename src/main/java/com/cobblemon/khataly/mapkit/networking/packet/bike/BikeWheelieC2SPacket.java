package com.cobblemon.khataly.mapkit.networking.packet.bike;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client → Server: premere o rilasciare la spacebar mentre si è sulla bici.
 * pressed = true  → inizio wheelie / bounce
 * pressed = false → fine wheelie / ritorno alla posizione normale
 */
public record BikeWheelieC2SPacket(boolean pressed) implements CustomPayload {

    public static final Id<BikeWheelieC2SPacket> ID =
            new Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "bike_wheelie_c2s"));

    public static final PacketCodec<RegistryByteBuf, BikeWheelieC2SPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOL, BikeWheelieC2SPacket::pressed,
                    BikeWheelieC2SPacket::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
