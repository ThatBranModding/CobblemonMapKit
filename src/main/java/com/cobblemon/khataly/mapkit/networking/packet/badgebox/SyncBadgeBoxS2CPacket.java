package com.cobblemon.khataly.mapkit.networking.packet.badgebox;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record SyncBadgeBoxS2CPacket(int totalSlots, List<BadgeEntry> badges) implements CustomPayload {

    public static final Id<SyncBadgeBoxS2CPacket> ID =
            new Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "badge_box_sync_s2c"));

    private static final PacketCodec<RegistryByteBuf, BadgeEntry> BADGE_ENTRY =
            PacketCodec.tuple(
                    Identifier.PACKET_CODEC, BadgeEntry::id,
                    PacketCodecs.VAR_INT,   BadgeEntry::shine,
                    BadgeEntry::new
            );

    public static final PacketCodec<RegistryByteBuf, SyncBadgeBoxS2CPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT,                           SyncBadgeBoxS2CPacket::totalSlots,
                    BADGE_ENTRY.collect(PacketCodecs.toList(16)),   SyncBadgeBoxS2CPacket::badges,
                    SyncBadgeBoxS2CPacket::new
            );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
