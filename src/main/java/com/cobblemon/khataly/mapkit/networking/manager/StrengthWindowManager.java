package com.cobblemon.khataly.mapkit.networking.manager;

import com.cobblemon.khataly.mapkit.config.HMConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks a per-player time window where Strength can be used without replaying the animation.
 */
public final class StrengthWindowManager {
    private StrengthWindowManager() {}

    // player UUID -> expiry game time (ticks)
    private static final Map<UUID, Long> EXPIRY_TICK = new ConcurrentHashMap<>();

    private static long now(MinecraftServer server) {
        // Overworld time is a reliable server-wide tick counter
        return server.getOverworld().getTime();
    }

    public static boolean isEnabled() {
        return HMConfig.STRENGTH_ANIMATION_WINDOW != null
                && HMConfig.STRENGTH_ANIMATION_WINDOW.enabled;
    }

    public static int seconds() {
        if (HMConfig.STRENGTH_ANIMATION_WINDOW == null) return 0;
        return Math.max(0, HMConfig.STRENGTH_ANIMATION_WINDOW.seconds);
    }

    public static void grant(ServerPlayerEntity player) {
        if (!isEnabled()) return;

        int secs = seconds();
        if (secs <= 0) return;

        long expiry = now(player.getServer()) + (long) secs * 20L;
        EXPIRY_TICK.put(player.getUuid(), expiry);
    }

    public static boolean isActive(ServerPlayerEntity player) {
        if (!isEnabled()) return false;

        Long expiry = EXPIRY_TICK.get(player.getUuid());
        if (expiry == null) return false;

        if (now(player.getServer()) <= expiry) return true;

        // expired
        EXPIRY_TICK.remove(player.getUuid());
        return false;
    }

    public static void clear(ServerPlayerEntity player) {
        EXPIRY_TICK.remove(player.getUuid());
    }
}
