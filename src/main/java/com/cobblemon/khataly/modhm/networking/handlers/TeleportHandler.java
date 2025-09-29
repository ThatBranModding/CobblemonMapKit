package com.cobblemon.khataly.modhm.networking.handlers;

import com.cobblemon.khataly.modhm.config.ModConfig;
import com.cobblemon.khataly.modhm.networking.packet.TeleportPacketC2S;
import com.cobblemon.khataly.modhm.networking.util.NetUtil;
import com.cobblemon.khataly.modhm.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public final class TeleportHandler {
    private TeleportHandler() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(TeleportPacketC2S.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            ctx.server().execute(() -> {
                if (!NetUtil.requireMove(p, "teleport", "❌ No Pokémon in your party knows Teleport!")) return;
                if (!NetUtil.requireItem(p, ModConfig.TELEPORT.item, ModConfig.TELEPORT.message)) return;

                NetUtil.sendAnimation(p, "teleport");

                BlockPos spawnPos = p.getSpawnPointPosition();
                ServerWorld spawnWorld = null;

                if (spawnPos != null) {
                    spawnWorld = Objects.requireNonNull(p.getServer()).getWorld(p.getSpawnPointDimension());
                }
                if (spawnPos == null || spawnWorld == null) {
                    spawnWorld = Objects.requireNonNull(p.getServer()).getOverworld();
                    spawnPos = spawnWorld.getSpawnPos();
                }

                NetUtil.teleportTo(p, spawnWorld, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5);
                NetUtil.playPlayerSound(p, ModSounds.TELEPORT);
                NetUtil.msg(p, "✨ Teleported to your spawn point!");
            });
        });
    }
}
