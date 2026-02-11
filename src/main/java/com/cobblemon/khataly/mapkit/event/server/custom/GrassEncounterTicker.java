package com.cobblemon.khataly.mapkit.event.server.custom;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.cobblemon.khataly.mapkit.compat.MaxRepelCompat;
import com.cobblemon.khataly.mapkit.compat.ShinyDexCompat;
import com.cobblemon.khataly.mapkit.config.GrassZonesConfig;
import com.cobblemon.khataly.mapkit.util.PlayerUtils;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.api.events.pokemon.ShinyChanceCalculationEvent;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.*;

/**
 * Grass Zone step encounters + MaxRepel + supported shiny pipeline.
 *
 * Shiny:
 * - Use Cobblemon base odds + SHINY_CHANCE_CALCULATION (so boosters/mods that hook it apply)
 * - Apply Zone ShinyMultiplier AFTER that
 * - ALSO: ShinyDex compat (because ShinyDex uses SpawnEvent path which GrassZones bypass)
 */
public class GrassEncounterTicker {

    private static final int ENCOUNTER_COOLDOWN_TICKS = 60;
    private static final double BASE_STEP_CHANCE = 0.08;

    private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
    private static final Map<UUID, BlockPos> LAST_BLOCK = new HashMap<>();
    private static final Map<UUID, UUID> ACTIVE_WILD = new HashMap<>();

    private static volatile boolean EVENTS_HOOKED = false;
    private static volatile boolean LOGGED_ONCE = false;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GrassEncounterTicker::onServerTick);
        hookBattleFleeDespawnOnce();
    }

    private static void hookBattleFleeDespawnOnce() {
        if (EVENTS_HOOKED) return;
        EVENTS_HOOKED = true;

        CobblemonEvents.BATTLE_FLED.subscribe(
                Priority.NORMAL,
                (BattleFledEvent event) -> {
                    try {
                        PlayerBattleActor actor = event.getPlayer();
                        ServerPlayerEntity player = actor.getEntity();
                        if (player == null) return kotlin.Unit.INSTANCE;

                        UUID pid = player.getUuid();
                        UUID wid = ACTIVE_WILD.remove(pid);
                        if (wid == null) return kotlin.Unit.INSTANCE;

                        var sw = (ServerWorld) player.getWorld();
                        var ent = sw.getEntity(wid);
                        if (ent instanceof PokemonEntity pe && pe.isAlive() && !pe.isRemoved()) {
                            pe.discard();
                        }
                    } catch (Throwable ignored) {}
                    return kotlin.Unit.INSTANCE;
                }
        );
    }

    private static void onServerTick(MinecraftServer server) {
        if (!LOGGED_ONCE) {
            LOGGED_ONCE = true;
            CobblemonMapKitMod.LOGGER.info("[GrassEncounterTicker] Shiny: Cobblemon base -> SHINY_CHANCE_CALCULATION -> apply Zone ShinyMultiplier -> ShinyDex compat (if installed).");
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

            COOLDOWN.computeIfPresent(player.getUuid(), (id, cd) -> Math.max(0, cd - 1));
            if (!isValidStepState(player)) continue;

            BlockPos now = player.getBlockPos();
            BlockPos prev = LAST_BLOCK.put(player.getUuid(), now);
            if (prev != null && prev.equals(now)) continue;

            if (COOLDOWN.getOrDefault(player.getUuid(), 0) > 0) continue;

            var world = player.getWorld();
            var wk = world.getRegistryKey();

            var zones = GrassZonesConfig.findAt(wk, now.getX(), now.getY(), now.getZ());
            if (zones.isEmpty()) continue;

            GrassZonesConfig.Zone zone = zones.getFirst();
            if (isInBattle(player)) continue;

            Random rng = player.getRandom();
            if (rng.nextDouble() >= BASE_STEP_CHANCE) continue;

            List<GrassZonesConfig.SpawnEntry> timeFiltered = filterByTime(zone.spawns(), world);
            if (timeFiltered.isEmpty()) continue;

            boolean inWater = isPlayerInWaterForEncounters(player);
            List<GrassZonesConfig.SpawnEntry> pool = filterByMedium(timeFiltered, inWater);
            if (pool.isEmpty()) continue;

            GrassZonesConfig.SpawnEntry choice = weightedRandom(pool, rng);
            if (choice == null) continue;

            int levelRange = Math.max(1, choice.maxLevel - choice.minLevel + 1);
            int level = choice.minLevel + rng.nextInt(levelRange);

            // MaxRepel: if active, only allow if spawnLevel > lead level
            if (MaxRepelCompat.isAnyRepelActive(player)) {
                int leadLevel = MaxRepelCompat.getLeadPartyLevel(player);
                if (leadLevel >= 0 && level <= leadLevel) continue;
            }

            BattleFormat format = BattleFormat.Companion.getGEN_9_SINGLES();

            if (startWildBattle(player, choice.species, level, format, zone.shinyMultiplier(), choice.aspect, rng)) {
                COOLDOWN.put(player.getUuid(), ENCOUNTER_COOLDOWN_TICKS);
            }
        }
    }

    private static boolean isValidStepState(ServerPlayerEntity p) {
        if (p.isSpectator()) return false;
        if (p.hasVehicle()) return false;

        boolean onLand = p.isOnGround();
        boolean inWater = isPlayerInWaterForEncounters(p);
        if (!onLand && !inWater) return false;

        return !isInBattle(p);
    }

    private static boolean isPlayerInWaterForEncounters(ServerPlayerEntity p) {
        return p.isTouchingWater() || p.isSwimming() || p.isSubmergedInWater();
    }

    private static boolean isInBattle(ServerPlayerEntity player) {
        try {
            var reg = Cobblemon.INSTANCE.getBattleRegistry();
            return reg.getBattleByParticipatingPlayer(player) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static List<GrassZonesConfig.SpawnEntry> filterByTime(List<GrassZonesConfig.SpawnEntry> entries, net.minecraft.world.World world) {
        if (entries == null || entries.isEmpty()) return Collections.emptyList();
        if (!world.getDimension().hasSkyLight()) return entries;

        long dayTime = world.getTimeOfDay() % 24000L;
        boolean isDay = dayTime < 12000L;

        List<GrassZonesConfig.SpawnEntry> out = new ArrayList<>(entries.size());
        for (GrassZonesConfig.SpawnEntry e : entries) {
            if (e == null) continue;
            switch (e.time) {
                case BOTH -> out.add(e);
                case DAY -> { if (isDay) out.add(e); }
                case NIGHT -> { if (!isDay) out.add(e); }
            }
        }
        return out;
    }

    private static List<GrassZonesConfig.SpawnEntry> filterByMedium(List<GrassZonesConfig.SpawnEntry> entries, boolean inWater) {
        if (entries == null || entries.isEmpty()) return Collections.emptyList();

        List<GrassZonesConfig.SpawnEntry> out = new ArrayList<>(entries.size());
        for (GrassZonesConfig.SpawnEntry e : entries) {
            if (e == null) continue;

            GrassZonesConfig.MediumBand m = (e.medium == null)
                    ? GrassZonesConfig.MediumBand.BOTH
                    : e.medium;

            if (m == GrassZonesConfig.MediumBand.BOTH) out.add(e);
            else if (m == GrassZonesConfig.MediumBand.WATER) { if (inWater) out.add(e); }
            else { if (!inWater) out.add(e); } // LAND
        }
        return out;
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

    private static boolean startWildBattle(ServerPlayerEntity player,
                                           String speciesId,
                                           int level,
                                           BattleFormat format,
                                           double shinyMultiplier,
                                           String aspect,
                                           Random rng) {
        var server = player.getServer();
        if (server == null) return false;
        if (!PlayerUtils.hasUsablePokemon(player)) return false;
        if (isInBattle(player)) return false;

        String key = speciesId == null ? "" : speciesId.toLowerCase(Locale.ROOT);
        if (key.contains(":")) key = key.substring(key.indexOf(':') + 1);

        Species species = PokemonSpecies.getByName(key);
        if (species == null) return false;

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);

        if (aspect != null && !aspect.isBlank()) {
            pokemon.setForcedAspects(Collections.singleton(aspect.toLowerCase(Locale.ROOT)));
            try { pokemon.updateForm(); } catch (Throwable ignored) {}
        }

        pokemon.setLevel(level);

        boolean isShiny = rollShiny(player, pokemon, shinyMultiplier, rng);
        pokemon.setShiny(isShiny);

        pokemon.initializeMoveset(true);
        pokemon.heal();

        var sw = (ServerWorld) player.getWorld();

        final double targetY = player.getY();
        Vec3d spawnPos = null;
        Vec3d base = player.getPos();

        Vec3d[] candidates = new Vec3d[] {
                new Vec3d(base.x + 1.0, targetY, base.z),
                new Vec3d(base.x - 1.0, targetY, base.z),
                new Vec3d(base.x, targetY, base.z + 1.0),
                new Vec3d(base.x, targetY, base.z - 1.0),
                new Vec3d(base.x + 1.0, targetY, base.z + 1.0),
                new Vec3d(base.x - 1.0, targetY, base.z - 1.0)
        };

        for (Vec3d cand : candidates) {
            BlockPos bp = BlockPos.ofFloored(cand.x, cand.y, cand.z);

            boolean spaceOk = sw.isAir(bp) || !sw.getFluidState(bp).isEmpty();
            boolean aboveOk = sw.isAir(bp.up()) || !sw.getFluidState(bp.up()).isEmpty();

            if (spaceOk && aboveOk) {
                spawnPos = new Vec3d(bp.getX() + 0.5, targetY, bp.getZ() + 0.5);
                break;
            }
        }

        if (spawnPos == null) {
            spawnPos = new Vec3d(Math.floor(base.x) + 1.5, targetY, Math.floor(base.z) + 0.5);
        }

        PokemonEntity entity = pokemon.sendOut(sw, spawnPos, null, e -> null);
        if (entity == null) return false;

        ACTIVE_WILD.put(player.getUuid(), entity.getUuid());

        var party = Cobblemon.INSTANCE.getStorage().getParty(player);

        server.execute(() -> server.execute(() -> {
            if (!entity.isRemoved() && entity.isAlive() && !isInBattle(player)) {
                BattleBuilder.INSTANCE.pve(
                        player,
                        entity,
                        null,
                        format,
                        false,
                        false,
                        16f,
                        party
                );
            }
        }));

        return true;
    }

    /**
     * Shiny roll:
     * 1) Base odds from Cobblemon config
     * 2) Run SHINY_CHANCE_CALCULATION so mods that hook it apply
     * 3) Apply Zone multiplier: odds /= multiplier
     * 4) Roll probability = 1/odds
     * 5) If ShinyDex is installed + charm says yes => force shiny (because ShinyDex normally relies on SpawnEvent)
     */
    private static boolean rollShiny(ServerPlayerEntity player, Pokemon pokemon, double zoneMultiplier, Random rng) {
        float baseOdds;
        try {
            baseOdds = Cobblemon.INSTANCE.getConfig().getShinyRate(); // "1 in N"
        } catch (Throwable t) {
            baseOdds = 8192f;
        }

        float effectiveOdds = baseOdds;

        try {
            ShinyChanceCalculationEvent ev = new ShinyChanceCalculationEvent(baseOdds, pokemon);
            CobblemonEvents.SHINY_CHANCE_CALCULATION.post(ev);
            effectiveOdds = ev.calculate(player);
        } catch (Throwable ignored) {
            effectiveOdds = baseOdds;
        }

        double mult = zoneMultiplier;
        if (Double.isNaN(mult) || Double.isInfinite(mult) || mult <= 0) mult = 1.0;

        double finalOdds = ((double) effectiveOdds) / mult;
        if (finalOdds < 1.0) finalOdds = 1.0;

        float chance = (float) (1.0 / finalOdds);
        boolean shiny = rng.nextFloat() < chance;

        // ShinyDex compat: if player has charm and it procs, force shiny
        if (!shiny && ShinyDexCompat.shouldForceShiny(player)) {
            return true;
        }

        return shiny;
    }
}
