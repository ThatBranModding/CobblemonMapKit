package com.cobblemon.khataly.mapkit.networking.handlers;

import com.cobblemon.khataly.mapkit.block.ModBlocks;
import com.cobblemon.khataly.mapkit.networking.manager.ClimbManager;
import com.cobblemon.khataly.mapkit.networking.packet.rockclimb.RockClimbPacketC2S;
import com.cobblemon.khataly.mapkit.networking.util.NetUtil;
import com.cobblemon.khataly.mapkit.config.HMConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class RockClimbHandler {
    private RockClimbHandler() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(RockClimbPacketC2S.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            ctx.server().execute(() -> {
                if (!NetUtil.requireMove(p, "rockclimb", "❌ No Pokémon in your party knows Rock Climb!")) return;
                if (!NetUtil.requireItem(p, HMConfig.ROCKCLIMB.item, HMConfig.ROCKCLIMB.message)) return;

                BlockPos startPos = payload.pos();
                BlockState st = p.getWorld().getBlockState(startPos);
                if (st.isAir() || !st.isOf(ModBlocks.CLIMBABLE_ROCK)) {
                    NetUtil.msg(p, "⚠️ This block cannot be climbed!");
                    return;
                }

                NetUtil.sendAnimation(p, "rockclimb");
                ClimbManager.get().start(p, startPos);
            });
        });
    }
}
