package com.cobblemon.khataly.mapkit.networking.packet.curiocase;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record SyncCurioCaseS2CPacket(int totalSlots, List<CurioEntry> curios) implements CustomPayload {

    public static final Id<SyncCurioCaseS2CPacket> ID =
            new Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "curio_case_sync_s2c"));

    private static final PacketCodec<RegistryByteBuf, CurioEntry> CURIO_ENTRY =
            PacketCodec.tuple(
                    Identifier.PACKET_CODEC, CurioEntry::id,
                    PacketCodecs.VAR_INT,    CurioEntry::shine,
                    CurioEntry::new
            );

    public static final PacketCodec<RegistryByteBuf, SyncCurioCaseS2CPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT,                         SyncCurioCaseS2CPacket::totalSlots,
                    CURIO_ENTRY.collect(PacketCodecs.toList(8)),  SyncCurioCaseS2CPacket::curios,
                    SyncCurioCaseS2CPacket::new
            );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
