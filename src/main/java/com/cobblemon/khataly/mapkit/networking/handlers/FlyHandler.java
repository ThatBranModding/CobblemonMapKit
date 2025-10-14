package com.cobblemon.khataly.mapkit.networking.handlers;

import com.cobblemon.khataly.mapkit.config.HMConfig;
import com.cobblemon.khataly.mapkit.networking.packet.fly.FlyPacketC2S;
import com.cobblemon.khataly.mapkit.networking.util.NetUtil;
import com.cobblemon.khataly.mapkit.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class FlyHandler {
    private FlyHandler() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(FlyPacketC2S.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            ctx.server().execute(() -> {
                if (!NetUtil.requireMove(p, "fly", "❌ No Pokémon in your party knows Fly!")) return;
                if (!NetUtil.requireItem(p, HMConfig.FLY.item, HMConfig.FLY.message)) return;

                BlockPos pos = payload.pos();
                NetUtil.sendAnimation(p, "fly");
                NetUtil.teleportTo(p, (ServerWorld) p.getWorld(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                NetUtil.playPlayerSound(p, ModSounds.FLY);
                NetUtil.msg(p, "🛫 Teleported to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            });
        });
    }
}
