package com.cobblemon.khataly.mapkit.event.server;

import com.cobblemon.khataly.mapkit.config.FlyTargetConfig;
import com.cobblemon.khataly.mapkit.config.PlayerFlyProgress;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;

public class FlyTargetProximityWatcher {

    /** Raggio di sblocco in blocchi. */
    private static final int UNLOCK_RADIUS = 8;
    private static final int UNLOCK_RADIUS_SQ = UNLOCK_RADIUS * UNLOCK_RADIUS;

    /** Check ogni N tick (riduce il carico). */
    private static final int TICKS_INTERVAL = 10;
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(FlyTargetProximityWatcher::onEndServerTick);
    }

    private static void onEndServerTick(MinecraftServer server) {
        if ((++tickCounter % TICKS_INTERVAL) != 0) return;

        Map<String, FlyTargetConfig.TargetInfo> catalog = FlyTargetConfig.getAllTargets();
        if (catalog.isEmpty()) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            World playerWorld = player.getWorld();
            BlockPos playerPos = player.getBlockPos();

            for (Map.Entry<String, FlyTargetConfig.TargetInfo> e : catalog.entrySet()) {
                String key = e.getKey();
                String id = PlayerFlyProgress.canon(key);
                FlyTargetConfig.TargetInfo info = e.getValue();

                // stessa dimensione?
                if (!playerWorld.getRegistryKey().equals(info.worldKey)) continue;

                // già sbloccato?
                if (id.isEmpty()) continue;
                if (PlayerFlyProgress.isUnlocked(player.getUuid(), id)) continue;

                // distanza
                BlockPos tPos = info.pos;
                int dx = playerPos.getX() - tPos.getX();
                int dy = playerPos.getY() - tPos.getY();
                int dz = playerPos.getZ() - tPos.getZ();
                int distSq = dx * dx + dy * dy + dz * dz;

                if (distSq <= UNLOCK_RADIUS_SQ) {
                    boolean added = PlayerFlyProgress.unlock(player.getUuid(), id);
                    if (added) {
                        // Show the *display* name, not the internal canonical id
                        sendUnlockTitle(player, prettifyKey(key));
                    }
                }
            }
        }
    }

    private static void sendUnlockTitle(ServerPlayerEntity player, String targetName) {
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Fly Target Unlocked!")));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("Now you can fly to " + targetName)));
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 60, 10));

        RegistryEntry<SoundEvent> entry = Registries.SOUND_EVENT.getEntry(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                entry,
                SoundCategory.PLAYERS,
                player.getX(), player.getY(), player.getZ(),
                1.0f, 1.0f,
                player.getWorld().random.nextLong()
        ));
    }

    private static String prettifyKey(String keyLower) {
        String[] parts = keyLower.replace('_', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
            sb.append(' ');
        }
        return sb.toString().trim();
    }
}
