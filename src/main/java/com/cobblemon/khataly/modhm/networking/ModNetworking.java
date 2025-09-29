package com.cobblemon.khataly.modhm.networking;

import com.cobblemon.khataly.modhm.networking.handlers.*;
import com.cobblemon.khataly.modhm.networking.packet.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.server.MinecraftServer;

public class ModNetworking {

    public static void registerPackets() {
        // ===== S2C =====
        PayloadTypeRegistry.playS2C().register(AnimationHMPacketS2C.ID, AnimationHMPacketS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(FlyMenuS2CPacket.ID,     FlyMenuS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(FlashMenuS2CPacket.ID,   FlashMenuS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(TeleportMenuS2CPacket.ID, TeleportMenuS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(UltraHoleMenuS2CPacket.ID, UltraHoleMenuS2CPacket.CODEC);

        // ===== C2S =====
        PayloadTypeRegistry.playC2S().register(RockSmashPacketC2S.ID,   RockSmashPacketC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(CutPacketC2S.ID,         CutPacketC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(StrengthPacketC2S.ID,    StrengthPacketC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(RockClimbPacketC2S.ID,   RockClimbPacketC2S.CODEC);

        PayloadTypeRegistry.playC2S().register(FlyPacketC2S.ID,         FlyPacketC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(FlyMenuC2SPacket.ID,     FlyMenuC2SPacket.CODEC);

        PayloadTypeRegistry.playC2S().register(FlashPacketC2S.ID,       FlashPacketC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(FlashMenuC2SPacket.ID,   FlashMenuC2SPacket.CODEC);

        PayloadTypeRegistry.playC2S().register(TeleportPacketC2S.ID,    TeleportPacketC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(TeleportMenuC2SPacket.ID, TeleportMenuC2SPacket.CODEC);

        PayloadTypeRegistry.playC2S().register(UltraHolePacketC2S.ID,   UltraHolePacketC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(UltraHoleMenuC2SPacket.ID, UltraHoleMenuC2SPacket.CODEC);

        // ===== Handlers =====
        RockSmashHandler.register();
        CutHandler.register();
        StrengthHandler.register();
        RockClimbHandler.register();
        FlyHandler.register();
        TeleportHandler.register();
        FlashHandler.register();
        UltraHoleHandler.register();
    }

    /** Chiamalo dal tuo tick server (o delega a NetworkingTick). */
    public static void tick(MinecraftServer server) {
        NetworkingTick.tick(server);
    }
}
