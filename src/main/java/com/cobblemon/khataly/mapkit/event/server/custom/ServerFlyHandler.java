package com.cobblemon.khataly.mapkit.event.server.custom;

import com.cobblemon.khataly.mapkit.config.FlyTargetConfig;
import com.cobblemon.khataly.mapkit.config.PlayerFlyProgress;
import com.cobblemon.khataly.mapkit.networking.packet.fly.FlyMenuS2CPacket;
import com.cobblemon.khataly.mapkit.util.PlayerUtils;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;

public class ServerFlyHandler {
    public static void register() {
        CobblemonEvents.POKEMON_SENT_PRE.subscribe(Priority.NORMAL, event -> {

            ServerPlayerEntity player = event.getPokemon().getOwnerPlayer();
            if (player == null) return null; // NPC

            // Filtra i target: solo quelli sbloccati dal player
            Map<String, FlyTargetConfig.TargetInfo> all = FlyTargetConfig.getAllTargets();
            var unlockedKeys = PlayerFlyProgress.getUnlocked(player.getUuid());
            Map<String, FlyTargetConfig.TargetInfo> visible = new HashMap<>();
            for (var e : all.entrySet()) {
                if (unlockedKeys.contains(e.getKey())) {
                    visible.put(e.getKey(), e.getValue());
                }
            }

            boolean canFly = PlayerUtils.pokemonHasMoveToGUI(player, event.getPokemon().getUuid(), "fly");
            System.out.println("hasmoveguiFly: " + canFly);
            ServerPlayNetworking.send(player, FlyMenuS2CPacket.fromServerData(event.getPokemon().getUuid(), canFly, visible));

            return null;
        });
    }
}
