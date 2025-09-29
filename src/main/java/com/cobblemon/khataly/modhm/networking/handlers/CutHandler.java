package com.cobblemon.khataly.modhm.networking.handlers;

import com.cobblemon.khataly.modhm.config.ModConfig;
import com.cobblemon.khataly.modhm.networking.manager.RestoreManager;
import com.cobblemon.khataly.modhm.networking.packet.CutPacketC2S;
import com.cobblemon.khataly.modhm.networking.util.NetUtil;
import com.cobblemon.khataly.modhm.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CutHandler {
    private CutHandler() {}
    private static final Logger LOGGER = LoggerFactory.getLogger("CutHandler");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(CutPacketC2S.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            ctx.server().execute(() -> {
                BlockPos pos = payload.pos();

                if (!NetUtil.requireMove(p, "cut", "❌ No Pokémon in your party knows Cut!")) return;
                if (!NetUtil.requireItem(p, ModConfig.CUT.item, ModConfig.CUT.message)) return;

                BlockState original = p.getWorld().getBlockState(pos);
                if (original.isAir()) {
                    NetUtil.msg(p, "⚠️ There's nothing to cut here!");
                    return;
                }
                if (RestoreManager.get().isBusy(pos)) {
                    NetUtil.msg(p, "⏳ The block has already been cut, wait for it to return!");
                    return;
                }

                NetUtil.sendAnimation(p, "cut");
                NetUtil.msg(p, "💥 you used Cut!");
                NetUtil.playPlayerSound(p, ModSounds.CUTTABLE_TREE);

                p.getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
                RestoreManager.get().addTimed(pos, original, ModConfig.CUT_RESPAWN);

                NetUtil.sendParticles(p, ParticleTypes.CHERRY_LEAVES, pos, 0.3f, 0.3f, 0.3f, 0.1f, 20);
                LOGGER.info("Block removed at {}, restore timer started", pos);
            });
        });
    }
}
