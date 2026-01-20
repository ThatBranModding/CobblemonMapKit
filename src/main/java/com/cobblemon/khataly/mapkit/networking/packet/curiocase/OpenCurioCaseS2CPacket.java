package com.cobblemon.khataly.mapkit.networking.packet.curiocase;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

public record OpenCurioCaseS2CPacket(Hand handUsed, int totalSlots, List<CurioEntry> curios, Optional<Identifier> animInsertedId)
        implements CustomPayload {

    public static final Id<OpenCurioCaseS2CPacket> ID =
            new Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "curio_case_open_s2c"));

    private static final PacketCodec<ByteBuf, Hand> HAND_CODEC =
            PacketCodecs.indexed(i -> Hand.values()[i], Hand::ordinal);

    private static final PacketCodec<RegistryByteBuf, CurioEntry> CURIO_ENTRY =
            PacketCodec.tuple(
                    Identifier.PACKET_CODEC, CurioEntry::id,
                    PacketCodecs.VAR_INT,    CurioEntry::shine,
                    CurioEntry::new
            );

    public static final PacketCodec<RegistryByteBuf, OpenCurioCaseS2CPacket> CODEC =
            PacketCodec.tuple(
                    HAND_CODEC,                                      OpenCurioCaseS2CPacket::handUsed,
                    PacketCodecs.VAR_INT,                            OpenCurioCaseS2CPacket::totalSlots,
                    CURIO_ENTRY.collect(PacketCodecs.toList(8)),     OpenCurioCaseS2CPacket::curios,
                    PacketCodecs.optional(Identifier.PACKET_CODEC),  OpenCurioCaseS2CPacket::animInsertedId,
                    OpenCurioCaseS2CPacket::new
            );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
