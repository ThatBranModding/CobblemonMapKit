package com.cobblemon.khataly.mapkit.networking.packet.curiocase;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EjectCurioC2SPacket(Identifier curioId) implements CustomPayload {

    public static final Id<EjectCurioC2SPacket> ID =
            new Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "curio_eject_c2s"));

    public static final PacketCodec<RegistryByteBuf, EjectCurioC2SPacket> CODEC =
            PacketCodec.tuple(
                    Identifier.PACKET_CODEC, EjectCurioC2SPacket::curioId,
                    EjectCurioC2SPacket::new
            );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
