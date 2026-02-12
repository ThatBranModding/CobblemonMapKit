package com.cobblemon.khataly.mapkit.networking.packet.fly;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record FlyPacketC2S(UUID pokemonId, Identifier worldKeyId, BlockPos pos) implements CustomPayload {

    public static final CustomPayload.Id<FlyPacketC2S> ID =
            new CustomPayload.Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "fly_request"));

    // ✅ Avoid ambiguous method ref by using lambdas
    private static final PacketCodec<RegistryByteBuf, UUID> UUID_CODEC =
            PacketCodec.of(
                    (uuid, buf) -> buf.writeUuid(uuid),
                    (buf) -> buf.readUuid()
            );

    public static final PacketCodec<RegistryByteBuf, FlyPacketC2S> CODEC =
            PacketCodec.tuple(
                    UUID_CODEC, FlyPacketC2S::pokemonId,
                    Identifier.PACKET_CODEC, FlyPacketC2S::worldKeyId,
                    BlockPos.PACKET_CODEC, FlyPacketC2S::pos,
                    FlyPacketC2S::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
