package com.cobblemon.khataly.mapkit.compat;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.cobblemon.mod.common.Cobblemon;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;

/**
 * MaxRepel compat:
 * - Detects active effects: maxrepel:repel, maxrepel:super_repel, maxrepel:max_repel
 * - Reads lead Pokémon level (slot 0) via reflection-safe best-effort
 */
public final class MaxRepelCompat {
    private MaxRepelCompat() {}

    private static final Identifier REPEL_ID       = Identifier.of("maxrepel", "repel");
    private static final Identifier SUPER_REPEL_ID = Identifier.of("maxrepel", "super_repel");
    private static final Identifier MAX_REPEL_ID   = Identifier.of("maxrepel", "max_repel");

    private static volatile boolean LOGGED = false;

    /** True if any repel effect is active on this player. Safe even if MaxRepel isn't installed. */
    public static boolean isAnyRepelActive(ServerPlayerEntity player) {
        if (player == null) return false;

        boolean active = hasEffect(player, REPEL_ID) || hasEffect(player, SUPER_REPEL_ID) || hasEffect(player, MAX_REPEL_ID);

        if (!LOGGED) {
            LOGGED = true;
            CobblemonMapKitMod.LOGGER.info("[MaxRepelCompat] Repel effects present? (first check result={})", active);
        }

        return active;
    }

    private static boolean hasEffect(ServerPlayerEntity player, Identifier id) {
        try {
            var entryOpt = Registries.STATUS_EFFECT.getEntry(id);
            if (entryOpt.isEmpty()) return false;
            RegistryEntry<?> entry = entryOpt.get();
            return player.getStatusEffect((RegistryEntry) entry) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Returns the LEAD Pokémon level (slot 0 / first party mon).
     * If we can't read it for any reason, returns -1 (meaning "don't block spawns").
     */
    public static int getLeadPartyLevel(ServerPlayerEntity player) {
        try {
            Object party = Cobblemon.INSTANCE.getStorage().getParty(player);
            if (party == null) return -1;

            Object lead = tryInvokeIndexed(party, "get", 0);
            if (lead == null) lead = tryInvokeIndexed(party, "getPokemon", 0);

            if (lead == null) lead = tryInvokeNoArgs(party, "getFirstPokemon");
            if (lead == null) lead = tryInvokeNoArgs(party, "getFirst");
            if (lead == null) lead = tryInvokeNoArgs(party, "first");

            if (lead == null) {
                Object it = tryInvokeNoArgs(party, "getPokemon");
                if (it instanceof Iterable<?> iterable) {
                    for (Object p : iterable) {
                        if (p != null) { lead = p; break; }
                    }
                }
            }

            if (lead == null) return -1;

            Integer lvl = tryGetLevel(lead);
            return (lvl == null) ? -1 : lvl;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static Object tryInvokeNoArgs(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object tryInvokeIndexed(Object target, String method, int idx) {
        try {
            Method m = target.getClass().getMethod(method, int.class);
            return m.invoke(target, idx);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Integer tryGetLevel(Object pokemon) {
        try {
            Method m = pokemon.getClass().getMethod("getLevel");
            Object v = m.invoke(pokemon);
            if (v instanceof Integer i) return i;
        } catch (Throwable ignored) {}

        try {
            Method m = pokemon.getClass().getMethod("level");
            Object v = m.invoke(pokemon);
            if (v instanceof Integer i) return i;
        } catch (Throwable ignored) {}

        try {
            var f = pokemon.getClass().getDeclaredField("level");
            f.setAccessible(true);
            Object v = f.get(pokemon);
            if (v instanceof Integer i) return i;
        } catch (Throwable ignored) {}

        return null;
    }
}
