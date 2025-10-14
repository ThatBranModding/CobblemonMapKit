package com.cobblemon.khataly.mapkit.networking.packet.cut;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record CutPacketC2S(BlockPos pos) implements CustomPayload {
    public static final Identifier ID_RAW = Identifier.of(CobblemonMapKitMod.MOD_ID, "cut_request");
    public static final CustomPayload.Id<CutPacketC2S> ID = new CustomPayload.Id<>(ID_RAW);

    public static final PacketCodec<RegistryByteBuf, CutPacketC2S> CODEC =
            PacketCodec.tuple(BlockPos.PACKET_CODEC, CutPacketC2S::pos, CutPacketC2S::new);


    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
