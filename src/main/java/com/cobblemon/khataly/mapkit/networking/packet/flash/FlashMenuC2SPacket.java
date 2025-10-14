package com.cobblemon.khataly.mapkit.networking.packet.flash;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record FlashMenuC2SPacket(UUID pokemonId) implements CustomPayload {
    public static final Identifier ID_RAW = Identifier.of(CobblemonMapKitMod.MOD_ID, "flash_request_menu");
    public static final Id<FlashMenuC2SPacket> ID = new Id<>(ID_RAW);

    public static final PacketCodec<RegistryByteBuf, FlashMenuC2SPacket> CODEC =
            PacketCodec.of(
                    (packet, buf) -> buf.writeUuid(packet.pokemonId),// scrivi
                    buf -> new FlashMenuC2SPacket(buf.readUuid())// leggi
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
