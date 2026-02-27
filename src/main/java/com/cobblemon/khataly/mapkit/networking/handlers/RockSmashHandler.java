package com.cobblemon.khataly.mapkit.networking.handlers;

import com.cobblemon.khataly.mapkit.config.HMConfig;
import com.cobblemon.khataly.mapkit.networking.manager.RestoreManager;
import com.cobblemon.khataly.mapkit.networking.packet.rocksmash.RockSmashPacketC2S;
import com.cobblemon.khataly.mapkit.networking.util.NetUtil;
import com.cobblemon.khataly.mapkit.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RockSmashHandler {
    private RockSmashHandler() {}
    private static final Logger LOGGER = LoggerFactory.getLogger("RockSmashHandler");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(RockSmashPacketC2S.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            ctx.server().execute(() -> {
                BlockPos pos = payload.pos();

                if (!NetUtil.requireMove(p, "rocksmash", "❌ No Pokémon in your party knows Rock Smash!")) return;
                if (!NetUtil.requireItem(p, HMConfig.ROCKSMASH.item, HMConfig.ROCKSMASH.message)) return;

                BlockState original = p.getWorld().getBlockState(pos);
                if (original.isAir()) {
                    NetUtil.msg(p, "⚠️ There's nothing to break here!");
                    return;
                }
                if (RestoreManager.get().isBusy((ServerWorld)p.getWorld(), pos))  {
                    NetUtil.msg(p, "⏳ The block has already been smashed, wait for it to return!");
                    return;
                }

                NetUtil.msg(p, "💥 you used Rock Smash!");
                NetUtil.playPlayerSound(p, ModSounds.BREAKABLE_ROCK);

                p.getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
                RestoreManager.get().addTimed((ServerWorld)p.getWorld(), pos, original, HMConfig.ROCKSMASH_RESPAWN);

                NetUtil.sendParticles(p, ParticleTypes.CLOUD, pos, 0.3f, 0.3f, 0.3f, 0.1f, 20);
                LOGGER.info("Block Rock removed at {}, restore timer started", pos);

                // No wild encounters from Rock Smash rocks:
                NetUtil.sendAnimation(p, "rocksmash");
            });
        });
    }
}