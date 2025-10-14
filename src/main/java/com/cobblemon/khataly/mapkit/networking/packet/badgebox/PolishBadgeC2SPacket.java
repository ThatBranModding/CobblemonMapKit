package com.cobblemon.khataly.mapkit.networking.packet.badgebox;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PolishBadgeC2SPacket(Identifier badgeId, int amount) implements CustomPayload {

    public static final Id<PolishBadgeC2SPacket> ID =
            new Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "badge_box_polish_c2s"));

    public static final PacketCodec<RegistryByteBuf, PolishBadgeC2SPacket> CODEC =
            PacketCodec.tuple(
                    Identifier.PACKET_CODEC,          PolishBadgeC2SPacket::badgeId,
                    PacketCodecs.VAR_INT,             PolishBadgeC2SPacket::amount,
                    PolishBadgeC2SPacket::new
            );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
