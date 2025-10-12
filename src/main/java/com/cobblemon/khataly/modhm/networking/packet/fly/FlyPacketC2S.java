package com.cobblemon.khataly.modhm.networking.packet.fly;

import com.cobblemon.khataly.modhm.HMMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;


public record FlyPacketC2S(BlockPos pos) implements CustomPayload {
    public static final Identifier ID_RAW = Identifier.of(HMMod.MOD_ID, "fly_request");
    public static final CustomPayload.Id<FlyPacketC2S> ID = new CustomPayload.Id<>(ID_RAW);

    public static final PacketCodec<RegistryByteBuf, FlyPacketC2S> CODEC =
            PacketCodec.tuple(BlockPos.PACKET_CODEC, FlyPacketC2S::pos, FlyPacketC2S::new);


    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

