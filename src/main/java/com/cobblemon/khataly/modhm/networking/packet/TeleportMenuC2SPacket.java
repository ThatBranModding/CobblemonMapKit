package com.cobblemon.khataly.modhm.networking.packet;

import com.cobblemon.khataly.modhm.HMMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record TeleportMenuC2SPacket(UUID pokemonId) implements CustomPayload {
    public static final Identifier ID_RAW = Identifier.of(HMMod.MOD_ID, "teleport_request_menu");
    public static final Id<TeleportMenuC2SPacket> ID = new Id<>(ID_RAW);

    public static final PacketCodec<RegistryByteBuf, TeleportMenuC2SPacket> CODEC =
            PacketCodec.of(
                    (packet, buf) -> buf.writeUuid(packet.pokemonId),// scrivi
                    buf -> new TeleportMenuC2SPacket(buf.readUuid())// leggi
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
