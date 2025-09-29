package com.cobblemon.khataly.modhm.networking.handlers;

import com.cobblemon.khataly.modhm.config.ModConfig;
import com.cobblemon.khataly.modhm.networking.packet.FlashPacketC2S;
import com.cobblemon.khataly.modhm.networking.util.NetUtil;
import com.cobblemon.khataly.modhm.sound.ModSounds;
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
                if (!NetUtil.requireItem(p, ModConfig.FLASH.item, ModConfig.FLASH.message)) return;

                if (p.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                    NetUtil.msg(p, "❗ Flash is already active!");
                    return;
                }

                NetUtil.sendAnimation(p, "flash");
                int ticks = ModConfig.FLASH_DURATION * 20;
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, ticks, 0, false, false));
                NetUtil.playPlayerSound(p, ModSounds.FLASH);
                NetUtil.msg(p, "✨ Flash activated! You can see clearly for " + ModConfig.FLASH_DURATION + " seconds.");
            });
        });
    }
}
