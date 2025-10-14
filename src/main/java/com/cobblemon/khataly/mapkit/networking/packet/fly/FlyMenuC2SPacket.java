package com.cobblemon.khataly.mapkit.networking.packet.fly;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record FlyMenuC2SPacket(UUID pokemonId) implements CustomPayload {
    public static final Identifier ID_RAW = Identifier.of(CobblemonMapKitMod.MOD_ID, "fly_request_menu");
    public static final CustomPayload.Id<FlyMenuC2SPacket> ID = new CustomPayload.Id<>(ID_RAW);

    public static final PacketCodec<RegistryByteBuf, FlyMenuC2SPacket> CODEC =
            PacketCodec.of(
                    (packet, buf) -> buf.writeUuid(packet.pokemonId),// scrivi
                    buf -> new FlyMenuC2SPacket(buf.readUuid())// leggi
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
