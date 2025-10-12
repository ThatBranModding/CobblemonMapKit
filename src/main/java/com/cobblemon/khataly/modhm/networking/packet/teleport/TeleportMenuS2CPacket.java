package com.cobblemon.khataly.modhm.networking.packet.teleport;

import com.cobblemon.khataly.modhm.HMMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record TeleportMenuS2CPacket(
        UUID pokemonId,
        boolean canTeleport
) implements CustomPayload {

    public static final CustomPayload.Id<TeleportMenuS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(HMMod.MOD_ID, "show_teleport_menu_s2c"));

    // Codec manuale per UUID (stessa logica di FlyMenuS2CPacket)
    private static final PacketCodec<RegistryByteBuf, UUID> UUID_CODEC = new PacketCodec<>() {
        @Override
        public UUID decode(RegistryByteBuf buf) {
            long most = buf.readLong();
            long least = buf.readLong();
            return new UUID(most, least);
        }

        @Override
        public void encode(RegistryByteBuf buf, UUID uuid) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public static final PacketCodec<RegistryByteBuf, TeleportMenuS2CPacket> CODEC = PacketCodec.tuple(
            UUID_CODEC, TeleportMenuS2CPacket::pokemonId,
            net.minecraft.network.codec.PacketCodecs.BOOL, TeleportMenuS2CPacket::canTeleport,
            TeleportMenuS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /** Factory per creare il pacchetto lato server */
    public static TeleportMenuS2CPacket fromServerData(UUID pokemonId, boolean canTeleport) {
        return new TeleportMenuS2CPacket(pokemonId, canTeleport);
    }
}
