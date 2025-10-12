package com.cobblemon.khataly.modhm.networking.packet.rocksmash;

import com.cobblemon.khataly.modhm.HMMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record RockSmashPacketC2S(BlockPos pos) implements CustomPayload {
    public static final Identifier ID_RAW = Identifier.of(HMMod.MOD_ID, "rocksmash_request");
    public static final CustomPayload.Id<RockSmashPacketC2S> ID = new CustomPayload.Id<>(ID_RAW);

    public static final PacketCodec<RegistryByteBuf, RockSmashPacketC2S> CODEC =
            PacketCodec.tuple(BlockPos.PACKET_CODEC, RockSmashPacketC2S::pos, RockSmashPacketC2S::new);


    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
