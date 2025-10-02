package com.cobblemon.khataly.modhm.networking.packet.badgebox;

import com.cobblemon.khataly.modhm.HMMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EjectBadgeC2SPacket(Identifier badgeId) implements CustomPayload {

    public static final Id<EjectBadgeC2SPacket> ID =
            new Id<>(Identifier.of(HMMod.MOD_ID, "badge_box_eject_c2s"));

    public static final PacketCodec<RegistryByteBuf, EjectBadgeC2SPacket> CODEC =
            PacketCodec.tuple(
                    Identifier.PACKET_CODEC, EjectBadgeC2SPacket::badgeId,
                    EjectBadgeC2SPacket::new
            );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
