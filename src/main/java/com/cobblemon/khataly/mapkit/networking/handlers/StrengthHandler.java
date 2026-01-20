package com.cobblemon.khataly.mapkit.networking.handlers;

import com.cobblemon.khataly.mapkit.config.HMConfig;
import com.cobblemon.khataly.mapkit.networking.manager.RestoreManager;
import com.cobblemon.khataly.mapkit.networking.manager.StrengthWindowManager;
import com.cobblemon.khataly.mapkit.networking.packet.strength.StrengthPacketC2S;
import com.cobblemon.khataly.mapkit.networking.util.NetUtil;
import com.cobblemon.khataly.mapkit.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class StrengthHandler {
    private StrengthHandler() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(StrengthPacketC2S.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            BlockPos clickedPos = payload.pos();

            ctx.server().execute(() -> {
                // Packet path: always allow fast-path logic
                handleStrength(player, clickedPos, true);
            });
        });
    }

    /**
     * Direct server-side call (e.g. from block right-click)
     */
    public static void handleDirect(ServerPlayerEntity player, BlockPos clickedPos) {
        handleStrength(player, clickedPos, true);
    }

    /**
     * @param allowFastPath If true, will skip animation when the player's Strength window is active.
     */
    private static void handleStrength(ServerPlayerEntity player, BlockPos clickedPos, boolean allowFastPath) {
        ServerWorld world = (ServerWorld) player.getWorld();

        if (!NetUtil.requireMove(player, "strength", "❌ No Pokémon in your party knows Strength!")) return;
        if (!NetUtil.requireItem(player, HMConfig.STRENGTH.item, HMConfig.STRENGTH.message)) return;

        // Resolve original, respecting aliases
        BlockPos originalPos = RestoreManager.get().resolveOriginal(world, clickedPos);
        RestoreManager.TimedBlock tb = RestoreManager.get().getTimed(world, originalPos);
        BlockPos currentPos = (tb != null && tb.movedTo != null) ? tb.movedTo : originalPos;

        BlockState state = world.getBlockState(currentPos);
        if (state.isAir()) {
            NetUtil.msg(player, "⚠️ There's nothing to move here!");
            return;
        }

        BlockPos targetPos = currentPos.offset(player.getHorizontalFacing());
        if (!world.getBlockState(targetPos).isAir()) {
            NetUtil.msg(player, "⛔ Cannot push the block there!");
            NetUtil.playPlayerSound(player, ModSounds.WALL_BUMP);
            return;
        }

        boolean fast = allowFastPath && StrengthWindowManager.isActive(player);

        // Only play animation AND grant window if NOT in free-move window
        if (!fast) {
            NetUtil.sendAnimation(player, "strength");
            NetUtil.msg(player, "💥 you used Strength!");

            // Grant free-move window ONLY when animation actually plays
            StrengthWindowManager.grant(player);
        }

        NetUtil.playPlayerSound(player, ModSounds.MOVABLE_ROCK);

        // Move the block
        world.setBlockState(currentPos, Blocks.AIR.getDefaultState());
        RestoreManager.get().forgetAlias(world, currentPos);

        world.setBlockState(targetPos, state);
        RestoreManager.get().registerMove(world, originalPos, targetPos, state, HMConfig.STRENGTH_RESPAWN);
    }
}
