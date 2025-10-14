package com.cobblemon.khataly.mapkit.networking.packet.ultrahole;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;


public record UltraHolePacketC2S(BlockPos pos) implements CustomPayload {
    public static final Identifier ID_RAW = Identifier.of(CobblemonMapKitMod.MOD_ID, "ultrahole_request");
    public static final Id<UltraHolePacketC2S> ID = new Id<>(ID_RAW);

    public static final PacketCodec<RegistryByteBuf, UltraHolePacketC2S> CODEC =
            PacketCodec.tuple(BlockPos.PACKET_CODEC, UltraHolePacketC2S::pos, UltraHolePacketC2S::new);


    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

