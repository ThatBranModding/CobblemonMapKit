package com.cobblemon.khataly.modhm.networking.packet;

import com.cobblemon.khataly.modhm.HMMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record StrengthPacketC2S(BlockPos pos) implements CustomPayload {
    public static final Identifier ID_RAW = Identifier.of(HMMod.MOD_ID, "strenght_request");
    public static final Id<StrengthPacketC2S> ID = new Id<>(ID_RAW);

    public static final PacketCodec<RegistryByteBuf, StrengthPacketC2S> CODEC =
            PacketCodec.tuple(BlockPos.PACKET_CODEC, StrengthPacketC2S::pos, StrengthPacketC2S::new);


    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
