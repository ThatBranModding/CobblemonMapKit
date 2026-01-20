package com.cobblemon.khataly.mapkit.networking.packet.curiocase;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PolishCurioC2SPacket(Identifier curioId, int amount) implements CustomPayload {

    public static final Id<PolishCurioC2SPacket> ID =
            new Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "curio_polish_c2s"));

    public static final PacketCodec<RegistryByteBuf, PolishCurioC2SPacket> CODEC =
            PacketCodec.tuple(
                    Identifier.PACKET_CODEC, PolishCurioC2SPacket::curioId,
                    PacketCodecs.VAR_INT,    PolishCurioC2SPacket::amount,
                    PolishCurioC2SPacket::new
            );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
