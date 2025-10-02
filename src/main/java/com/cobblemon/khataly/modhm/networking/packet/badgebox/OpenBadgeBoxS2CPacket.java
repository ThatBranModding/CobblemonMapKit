package com.cobblemon.khataly.modhm.networking.packet.badgebox;

import com.cobblemon.khataly.modhm.HMMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

public record OpenBadgeBoxS2CPacket(Hand handUsed, int totalSlots, List<BadgeEntry> badges, Optional<Identifier> animInsertedId) implements CustomPayload {

    public static final Id<OpenBadgeBoxS2CPacket> ID =
            new Id<>(Identifier.of(HMMod.MOD_ID, "badge_box_open_s2c"));

    private static final PacketCodec<ByteBuf, Hand> HAND_CODEC =
            PacketCodecs.indexed(i -> Hand.values()[i], Hand::ordinal);

    private static final PacketCodec<RegistryByteBuf, BadgeEntry> BADGE_ENTRY =
            PacketCodec.tuple(
                    Identifier.PACKET_CODEC, BadgeEntry::id,
                    PacketCodecs.VAR_INT,   BadgeEntry::shine,
                    BadgeEntry::new
            );

    public static final PacketCodec<RegistryByteBuf, OpenBadgeBoxS2CPacket> CODEC =
            PacketCodec.tuple(
                    HAND_CODEC,                                     OpenBadgeBoxS2CPacket::handUsed,
                    PacketCodecs.VAR_INT,                           OpenBadgeBoxS2CPacket::totalSlots,
                    BADGE_ENTRY.collect(PacketCodecs.toList(16)),   OpenBadgeBoxS2CPacket::badges,
                    PacketCodecs.optional(Identifier.PACKET_CODEC), OpenBadgeBoxS2CPacket::animInsertedId,
                    OpenBadgeBoxS2CPacket::new
            );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
