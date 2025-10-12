package com.cobblemon.khataly.modhm.event.server.custom;

import com.cobblemon.khataly.modhm.config.GrassZonesConfig;
import com.cobblemon.khataly.modhm.util.PlayerUtils;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.*;

/** Step encounters inside zones: now independent from decorative grass. */
public class GrassEncounterTicker {

    private static final int ENCOUNTER_COOLDOWN_TICKS = 60; // ~3s
    private static final double BASE_STEP_CHANCE = 0.08;    // 8% per step

    private static final Map<UUID, Integer> cooldown = new HashMap<>();
    private static final Map<UUID, BlockPos> lastBlock = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GrassEncounterTicker::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            // cooldown
            cooldown.computeIfPresent(p.getUuid(), (id, cd) -> Math.max(0, cd - 1));

            // invalid states
            if (p.isSpectator() || !p.isOnGround() || p.hasVehicle()) continue;

            // moved to a new block?
            BlockPos now = p.getBlockPos();
            BlockPos prev = lastBlock.put(p.getUuid(), now);
            if (prev != null && prev.equals(now)) continue;

            var world = p.getWorld();
            var wk = world.getRegistryKey();
            var zones = GrassZonesConfig.findAt(wk, now.getX(), now.getY(), now.getZ());
            if (zones.isEmpty()) continue;

            if (cooldown.getOrDefault(p.getUuid(), 0) > 0) continue;

            var zone = zones.getFirst();
            if (now.getY() != zone.y()) continue;

            // roll chance — independent from decorative grass
            Random rng = p.getRandom();
            if (rng.nextDouble() >= BASE_STEP_CHANCE) continue;

            // pick spawn
            var choice = weightedRandom(zone.spawns(), rng);
            if (choice == null) continue;

            int levelRange = choice.maxLevel - choice.minLevel + 1;
            int level = choice.minLevel + (levelRange > 0 ? rng.nextInt(levelRange) : 0);

            // always Singles for now
            BattleFormat format = BattleFormat.Companion.getGEN_9_SINGLES();

            if (startWildBattle(p, choice.species, level, format)) {
                cooldown.put(p.getUuid(), ENCOUNTER_COOLDOWN_TICKS);
            }
        }
    }

    /** Starts a 1v1 PvE battle, always passing the party to avoid crashes. */
    private static boolean startWildBattle(ServerPlayerEntity player, String speciesId, int level, BattleFormat format) {
        var server = player.getServer();
        if (server == null) return false;
        if (!PlayerUtils.hasUsablePokemon(player)) return false;

        String key = speciesId == null ? "" : speciesId.toLowerCase();
        if (key.contains(":")) key = key.substring(key.indexOf(':') + 1);

        Species species = PokemonSpecies.INSTANCE.getByName(key);
        if (species == null) return false;

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setLevel(level);
        pokemon.initializeMoveset(true);
        pokemon.heal();

        var sw = (net.minecraft.server.world.ServerWorld) player.getWorld();
        BlockPos base = player.getBlockPos();
        BlockPos pos = base.add(1, 0, 0);
        var vec = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        PokemonEntity entity = pokemon.sendOut(sw, vec, null, e -> null);
        if (entity == null) return false;

        var party = Cobblemon.INSTANCE.getStorage().getParty(player); // non-null in 1.6

        // Start battle after 1–2 ticks
        server.execute(() -> server.execute(() -> {
            if (!entity.isRemoved() && entity.isAlive()) {
                BattleBuilder.INSTANCE.pve(
                        player,
                        entity,
                        null,        // leading
                        format,      // singles
                        false,       // cloneParties
                        false,       // healFirst
                        16f,         // fleeDistance
                        party
                );
            }
        }));

        return true;
    }

    private static GrassZonesConfig.SpawnEntry weightedRandom(List<GrassZonesConfig.SpawnEntry> entries, Random r) {
        if (entries == null || entries.isEmpty()) return null;
        int total = 0;
        for (var e : entries) total += Math.max(0, e.weight);
        if (total <= 0) return null;
        int roll = r.nextInt(total);
        int acc = 0;
        for (var e : entries) {
            acc += Math.max(0, e.weight);
            if (roll < acc) return e;
        }
        return null;
    }
}
