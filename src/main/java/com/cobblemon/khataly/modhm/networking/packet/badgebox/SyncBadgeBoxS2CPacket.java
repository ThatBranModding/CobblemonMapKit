package com.cobblemon.khataly.modhm.networking.packet.badgebox;

import com.cobblemon.khataly.modhm.HMMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SyncBadgeBoxS2CPacket(int totalSlots, List<Identifier> badgeIds) implements CustomPayload {

    public static final Id<SyncBadgeBoxS2CPacket> ID =
            new Id<>(Identifier.of(HMMod.MOD_ID, "badge_box_sync_s2c"));

    public static final PacketCodec<RegistryByteBuf, SyncBadgeBoxS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SyncBadgeBoxS2CPacket::totalSlots,
            PacketCodecs.collection(ArrayList::new, Identifier.PACKET_CODEC), SyncBadgeBoxS2CPacket::badgeIds,
            SyncBadgeBoxS2CPacket::new
    );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
