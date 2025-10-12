package com.cobblemon.khataly.modhm.networking.handlers;

import com.cobblemon.khataly.modhm.block.ModBlocks;
import com.cobblemon.khataly.modhm.networking.manager.ClimbManager;
import com.cobblemon.khataly.modhm.networking.packet.rockclimb.RockClimbPacketC2S;
import com.cobblemon.khataly.modhm.networking.util.NetUtil;
import com.cobblemon.khataly.modhm.config.ModConfig;
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
                if (!NetUtil.requireItem(p, ModConfig.ROCKCLIMB.item, ModConfig.ROCKCLIMB.message)) return;

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
