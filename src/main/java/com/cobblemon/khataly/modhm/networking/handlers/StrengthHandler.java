package com.cobblemon.khataly.modhm.networking.handlers;

import com.cobblemon.khataly.modhm.config.ModConfig;
import com.cobblemon.khataly.modhm.networking.manager.RestoreManager;
import com.cobblemon.khataly.modhm.networking.packet.strength.StrengthPacketC2S;
import com.cobblemon.khataly.modhm.networking.util.NetUtil;
import com.cobblemon.khataly.modhm.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class StrengthHandler {
    private StrengthHandler() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(StrengthPacketC2S.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            ctx.server().execute(() -> {
                BlockPos clickedPos = payload.pos();

                if (!NetUtil.requireMove(p, "strength", "❌ No Pokémon in your party knows Strength!")) return;
                if (!NetUtil.requireItem(p, ModConfig.STRENGTH.item, ModConfig.STRENGTH.message)) return;

                BlockPos originalPos = RestoreManager.get().resolveOriginal(clickedPos);
                RestoreManager.TimedBlock tb = RestoreManager.get().getTimed(originalPos);
                BlockPos currentPos = (tb != null && tb.movedTo != null) ? tb.movedTo : originalPos;

                BlockState state = p.getWorld().getBlockState(currentPos);
                if (state.isAir()) {
                    NetUtil.msg(p, "⚠️ There's nothing to move here!");
                    return;
                }

                BlockPos targetPos = currentPos.offset(p.getHorizontalFacing());
                if (!p.getWorld().getBlockState(targetPos).isAir()) {
                    NetUtil.msg(p, "⛔ Cannot push the block there!");
                    NetUtil.playPlayerSound(p, ModSounds.WALL_BUMP);
                    return;
                }

                NetUtil.sendAnimation(p, "strength");
                NetUtil.msg(p, "💥 you used Strength!");
                NetUtil.playPlayerSound(p, ModSounds.MOVABLE_ROCK);

                p.getWorld().setBlockState(currentPos, Blocks.AIR.getDefaultState());
                RestoreManager.get().forgetAlias(currentPos);
                p.getWorld().setBlockState(targetPos, state);
                RestoreManager.get().registerMove(originalPos, targetPos, state, ModConfig.STRENGTH_RESPAWN);
            });
        });
    }
}
