package com.cobblemon.khataly.mapkit.compat;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;

/**
 * ShinyDex (Shiny Charm) compat for GrassZones.
 *
 * ShinyDex does NOT use Cobblemon's SHINY_CHANCE_CALCULATION.
 * It hooks Cobblemon SpawnEvent and checks SpawnCause -> player.
 *
 * GrassZones bypass Cobblemon spawning, so we manually call ShinyDex logic:
 * - if player is wearing charm AND SpawnHandler.getIsShiny() says yes => force shiny.
 */
public final class ShinyDexCompat {
    private ShinyDexCompat() {}

    private static volatile boolean INIT = false;
    private static volatile boolean AVAILABLE = false;

    private static Method M_IS_WEARING = null; // ShinyCharmItem.isWearingShinyCharm(LivingEntity)
    private static Method M_GET_IS_SHINY = null; // SpawnHandler.getIsShiny()

    private static volatile boolean LOGGED = false;

    public static boolean isAvailable() {
        init();
        return AVAILABLE;
    }

    /** Returns true if ShinyDex says to force-shiny this encounter. */
    public static boolean shouldForceShiny(ServerPlayerEntity player) {
        init();
        if (!AVAILABLE || player == null) return false;

        try {
            boolean wearing = (boolean) M_IS_WEARING.invoke(null, player);
            if (!wearing) return false;

            return (boolean) M_GET_IS_SHINY.invoke(null);
        } catch (Throwable t) {
            // If something changes in future ShinyDex versions, fail open (don’t force shiny).
            return false;
        }
    }

    private static void init() {
        if (INIT) return;
        synchronized (ShinyDexCompat.class) {
            if (INIT) return;

            try {
                ClassLoader cl = ShinyDexCompat.class.getClassLoader();

                Class<?> cCharm = Class.forName("dev.darcosse.shiny_charm.fabric.item.ShinyCharmItem", false, cl);
                Class<?> cSpawn = Class.forName("dev.darcosse.shiny_charm.fabric.handlers.SpawnHandler", false, cl);

                M_IS_WEARING = cCharm.getMethod("isWearingShinyCharm", net.minecraft.entity.LivingEntity.class);
                M_GET_IS_SHINY = cSpawn.getMethod("getIsShiny");

                AVAILABLE = true;
            } catch (Throwable ignored) {
                AVAILABLE = false;
            }

            INIT = true;

            if (!LOGGED) {
                LOGGED = true;
                CobblemonMapKitMod.LOGGER.info("[ShinyDexCompat] ShinyDex detected: {}", AVAILABLE);
            }
        }
    }
}
