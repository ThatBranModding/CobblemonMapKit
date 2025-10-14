package com.cobblemon.khataly.mapkit.networking.packet.badgebox;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EjectBadgeC2SPacket(Identifier badgeId) implements CustomPayload {

    public static final Id<EjectBadgeC2SPacket> ID =
            new Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "badge_box_eject_c2s"));

    public static final PacketCodec<RegistryByteBuf, EjectBadgeC2SPacket> CODEC =
            PacketCodec.tuple(
                    Identifier.PACKET_CODEC, EjectBadgeC2SPacket::badgeId,
                    EjectBadgeC2SPacket::new
            );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
