package com.cobblemon.khataly.modhm.networking.packet;

import com.cobblemon.khataly.modhm.HMMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record RockClimbPacketC2S(BlockPos pos) implements CustomPayload {
    public static final Identifier ID_RAW = Identifier.of(HMMod.MOD_ID, "rockclimb_request");
    public static final Id<RockClimbPacketC2S> ID = new Id<>(ID_RAW);

    public static final PacketCodec<RegistryByteBuf, RockClimbPacketC2S> CODEC =
            PacketCodec.tuple(BlockPos.PACKET_CODEC, RockClimbPacketC2S::pos, RockClimbPacketC2S::new);


    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
