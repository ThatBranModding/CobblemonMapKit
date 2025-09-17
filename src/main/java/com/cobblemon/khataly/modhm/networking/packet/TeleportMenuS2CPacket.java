package com.cobblemon.khataly.modhm.networking.packet;

import com.cobblemon.khataly.modhm.HMMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;


public record TeleportMenuS2CPacket(boolean cantp) implements CustomPayload {
    public static final Id<TeleportMenuS2CPacket> ID =
            new Id<>(net.minecraft.util.Identifier.of(HMMod.MOD_ID, "show_teleport_menu_s2c"));

    public static final PacketCodec<RegistryByteBuf, TeleportMenuS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, TeleportMenuS2CPacket::cantp,
            TeleportMenuS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
