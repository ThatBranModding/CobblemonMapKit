package com.cobblemon.khataly.modhm.networking.packet.badgebox;

import com.cobblemon.khataly.modhm.HMMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record OpenBadgeBoxS2CPacket(Hand handUsed, int totalSlots, List<Identifier> badgeIds) implements CustomPayload {

    public static final Id<OpenBadgeBoxS2CPacket> ID =
            new Id<>(Identifier.of(HMMod.MOD_ID, "badge_box_open_s2c"));

    // Codec per Hand (enum) con indexed
    private static final PacketCodec<ByteBuf, Hand> HAND_CODEC =
            PacketCodecs.indexed(i -> Hand.values()[i], Hand::ordinal);

    public static final PacketCodec<RegistryByteBuf, OpenBadgeBoxS2CPacket> CODEC = PacketCodec.tuple(
            HAND_CODEC, OpenBadgeBoxS2CPacket::handUsed,
            PacketCodecs.VAR_INT, OpenBadgeBoxS2CPacket::totalSlots,
            PacketCodecs.collection(ArrayList::new, Identifier.PACKET_CODEC), OpenBadgeBoxS2CPacket::badgeIds,
            OpenBadgeBoxS2CPacket::new
    );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
