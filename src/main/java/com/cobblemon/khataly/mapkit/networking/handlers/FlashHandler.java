package com.cobblemon.khataly.mapkit.networking.handlers;

import com.cobblemon.khataly.mapkit.config.HMConfig;
import com.cobblemon.khataly.mapkit.networking.packet.flash.FlashPacketC2S;
import com.cobblemon.khataly.mapkit.networking.util.NetUtil;
import com.cobblemon.khataly.mapkit.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FlashHandler {
    private FlashHandler() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(FlashPacketC2S.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            ctx.server().execute(() -> {
                if (!NetUtil.requireMove(p, "flash", "❌ No Pokémon in your party knows Flash!")) return;
                if (!NetUtil.requireItem(p, HMConfig.FLASH.item, HMConfig.FLASH.message)) return;

                if (p.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                    NetUtil.msg(p, "❗ Flash is already active!");
                    return;
                }

                NetUtil.sendAnimation(p, "flash");
                int ticks = HMConfig.FLASH_DURATION * 20;
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, ticks, 0, false, false));
                NetUtil.playPlayerSound(p, ModSounds.FLASH);
                NetUtil.msg(p, "✨ Flash activated! You can see clearly for " + HMConfig.FLASH_DURATION + " seconds.");
            });
        });
    }
}
