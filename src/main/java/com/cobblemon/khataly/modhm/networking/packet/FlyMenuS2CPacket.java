package com.cobblemon.khataly.modhm.networking.packet;

import com.cobblemon.khataly.modhm.HMMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;


public record FlyMenuS2CPacket(boolean canFly) implements CustomPayload {
    public static final CustomPayload.Id<FlyMenuS2CPacket> ID =
            new CustomPayload.Id<>(net.minecraft.util.Identifier.of(HMMod.MOD_ID, "show_fly_menu_s2c"));

    public static final PacketCodec<RegistryByteBuf, FlyMenuS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, FlyMenuS2CPacket::canFly,
            FlyMenuS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
