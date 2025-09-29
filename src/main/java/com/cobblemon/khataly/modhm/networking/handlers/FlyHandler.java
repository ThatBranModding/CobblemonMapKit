package com.cobblemon.khataly.modhm.networking.handlers;

import com.cobblemon.khataly.modhm.config.ModConfig;
import com.cobblemon.khataly.modhm.networking.packet.FlyPacketC2S;
import com.cobblemon.khataly.modhm.networking.util.NetUtil;
import com.cobblemon.khataly.modhm.sound.ModSounds;
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
                if (!NetUtil.requireItem(p, ModConfig.FLY.item, ModConfig.FLY.message)) return;

                BlockPos pos = payload.pos();
                NetUtil.sendAnimation(p, "fly");
                NetUtil.teleportTo(p, (ServerWorld) p.getWorld(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                NetUtil.playPlayerSound(p, ModSounds.FLY);
                NetUtil.msg(p, "🛫 Teleported to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            });
        });
    }
}
