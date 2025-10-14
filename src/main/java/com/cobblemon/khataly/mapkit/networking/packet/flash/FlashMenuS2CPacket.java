package com.cobblemon.khataly.mapkit.networking.packet.flash;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record FlashMenuS2CPacket(
        UUID pokemonId,
        boolean canFlash
) implements CustomPayload {

    public static final CustomPayload.Id<FlashMenuS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "show_flash_menu_s2c"));

    // Codec manuale per UUID (stessa logica di TeleportMenuS2CPacket)
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

    public static final PacketCodec<RegistryByteBuf, FlashMenuS2CPacket> CODEC = PacketCodec.tuple(
            UUID_CODEC, FlashMenuS2CPacket::pokemonId,
            net.minecraft.network.codec.PacketCodecs.BOOL, FlashMenuS2CPacket::canFlash,
            FlashMenuS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /** Factory per creare il pacchetto lato server */
    public static FlashMenuS2CPacket fromServerData(UUID pokemonId, boolean canFlash) {
        return new FlashMenuS2CPacket(pokemonId, canFlash);
    }
}
