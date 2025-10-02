package com.cobblemon.khataly.modhm.networking.packet.badgebox;

import com.cobblemon.khataly.modhm.HMMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public record InsertBadgeC2SPacket(Identifier badgeId, Hand handUsed) implements CustomPayload {

    public static final Id<InsertBadgeC2SPacket> ID =
            new Id<>(Identifier.of(HMMod.MOD_ID, "badge_box_insert_c2s"));

    private static final PacketCodec<ByteBuf, Hand> HAND_CODEC =
            PacketCodecs.indexed(i -> Hand.values()[i], Hand::ordinal);

    public static final PacketCodec<RegistryByteBuf, InsertBadgeC2SPacket> CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC, InsertBadgeC2SPacket::badgeId,
            HAND_CODEC, InsertBadgeC2SPacket::handUsed,
            InsertBadgeC2SPacket::new
    );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
