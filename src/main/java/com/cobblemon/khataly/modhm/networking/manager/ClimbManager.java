package com.cobblemon.khataly.modhm.networking.manager;

import com.cobblemon.khataly.modhm.block.ModBlocks;
import com.cobblemon.khataly.modhm.block.custom.ClimbableRock;
import com.cobblemon.khataly.modhm.networking.util.NetUtil;
import com.cobblemon.khataly.modhm.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestisce lo stato di arrampicata dei giocatori.
 * Fix principale: azzera fallDistance durante la scalata
 * e mantiene una breve immunità al danno da caduta dopo la fine.
 */
public final class ClimbManager {
    private static final ClimbManager INSTANCE = new ClimbManager();
    public static ClimbManager get() { return INSTANCE; }
    private ClimbManager() {}

    // --- Stato per-giocatore ---
    private final Map<ServerPlayerEntity, BlockPos> playersClimbing = new ConcurrentHashMap<>();
    private final Map<ServerPlayerEntity, Integer> climbingTicks = new ConcurrentHashMap<>();
    private final Map<ServerPlayerEntity, Set<BlockPos>> visitedClimbBlocks = new ConcurrentHashMap<>();
    private final Map<ServerPlayerEntity, BlockPos> climbingTargets = new ConcurrentHashMap<>();
    private final Set<ServerPlayerEntity> playersPlayingSound = ConcurrentHashMap.newKeySet();

    // Immunità al danno da caduta post-scalata (grace period)
    private final Map<ServerPlayerEntity, Integer> fallImmunityTicks = new ConcurrentHashMap<>();

    // Config “interna” per la scalata
    private static final double CLIMB_SPEED = 0.15;
    private static final double REACH_EPSILON = 0.2;
    private static final int SOUND_TICK_DELAY = 4;
    private static final int FALL_IMMUNITY_GRACE_TICKS = 10;

    /** Avvia la scalata da una posizione iniziale. */
    public void start(ServerPlayerEntity player, BlockPos startPos) {
        playersClimbing.put(player, startPos);
        climbingTicks.put(player, 0);
        // Evita accumulo iniziale
        player.fallDistance = 0f;
    }

    /** Da chiamare ad ogni tick server. */
    public void tick() {
        // --- Fase scalata: muove i player e azzera fallDistance ---
        playersClimbing.forEach((player, startPos) -> {
            if (!player.isAlive()) {
                cleanup(player);
                return;
            }

            // Mentre si scala, niente danno da caduta
            player.fallDistance = 0f;

            climbingTicks.put(player, climbingTicks.getOrDefault(player, 0) + 1);

            visitedClimbBlocks.putIfAbsent(player, new HashSet<>());
            Set<BlockPos> visited = visitedClimbBlocks.get(player);
            visited.add(startPos);

            BlockPos target = climbingTargets.get(player);
            if (target == null) {
                target = findNextClimbStep((ServerWorld) player.getWorld(), startPos, visited);
                if (target == null) {
                    NetUtil.msg(player, "🧗 No climbable blocks!");
                    grantFallImmunity(player);
                    cleanup(player);
                    return;
                }
                climbingTargets.put(player, target);
                visited.add(target);
            }

            double dx = target.getX() + 0.5 - player.getX();
            double dy = target.getY() + 1.0 - player.getY();
            double dz = target.getZ() + 0.5 - player.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance < REACH_EPSILON) {
                BlockPos next = findNextClimbStep((ServerWorld) player.getWorld(), target, visited);
                if (next == null) {
                    // Ultimo “boost” e fine: garantisci immunità e azzera
                    double finalBoost = dy > 0 ? 0.2 : -0.2;
                    player.fallDistance = 0f;
                    grantFallImmunity(player);

                    player.setVelocity(0, finalBoost, 0);
                    player.velocityModified = true;

                    NetUtil.msg(player, dy > 0 ? "🧗 You climbed up!" : "🧗 You climbed down!");
                    cleanup(player);
                    return;
                }
                climbingTargets.put(player, next);
                visited.add(next);
                return;
            }

            // Movimento verso il target
            player.setVelocity(dx / distance * CLIMB_SPEED, dy / distance * CLIMB_SPEED, dz / distance * CLIMB_SPEED);
            player.velocityModified = true;

            // Suono di arrampicata dopo un breve delay
            if (climbingTicks.get(player) >= SOUND_TICK_DELAY && !playersPlayingSound.contains(player)) {
                player.playSoundToPlayer(ModSounds.CLIMBABLE_ROCK, SoundCategory.PLAYERS, 1f, 1f);
                playersPlayingSound.add(player);
            }
        });

        // --- Fase post-scalata: mantieni immunità per qualche tick ---
        // Usiamo un iteratore per rimuovere in sicurezza
        var it = fallImmunityTicks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ServerPlayerEntity, Integer> e = it.next();
            ServerPlayerEntity player = e.getKey();
            int ticksLeft = e.getValue();

            // Continuiamo ad azzerare per tutta la durata dell’immunità
            player.fallDistance = 0f;

            int next = ticksLeft - 1;
            if (next <= 0) {
                it.remove();
            } else {
                e.setValue(next);
            }
        }
    }

    /** Ripulisce tutto lo stato di scalata (ma NON l'immunità). */
    private void cleanup(ServerPlayerEntity player) {
        playersClimbing.remove(player);
        climbingTicks.remove(player);
        playersPlayingSound.remove(player);
        climbingTargets.remove(player);
        visitedClimbBlocks.remove(player);
        // Per sicurezza, azzera subito
        player.fallDistance = 0f;
        // L'immunità post-scalata resta gestita dal ciclo sopra.
    }

    /** Concede immunità al danno da caduta per un certo numero di tick. */
    private void grantFallImmunity(ServerPlayerEntity player) {
        // Mantieni il valore più alto (utile se chiamato ripetutamente)
        fallImmunityTicks.merge(player, ClimbManager.FALL_IMMUNITY_GRACE_TICKS, Math::max);
    }

    /** Trova il prossimo passo di arrampicata. */
    private BlockPos findNextClimbStep(ServerWorld world, BlockPos from, Set<BlockPos> visited) {
        BlockState state = world.getBlockState(from);

        // 1) Su
        BlockPos up = from.up();
        if (!visited.contains(up) && state.isOf(ModBlocks.CLIMBABLE_ROCK) && world.getBlockState(up).isOf(ModBlocks.CLIMBABLE_ROCK)) {
            return up;
        }
        if (state.isOf(ModBlocks.CLIMBABLE_ROCK)) {
            Direction facing = state.get(ClimbableRock.FACING);
            BlockPos upForward = up.offset(facing);
            if (!visited.contains(upForward) && world.getBlockState(upForward).isOf(ModBlocks.CLIMBABLE_ROCK)) return upForward;

            BlockPos upBackward = up.offset(facing.getOpposite());
            if (!visited.contains(upBackward) && world.getBlockState(upBackward).isOf(ModBlocks.CLIMBABLE_ROCK)) return upBackward;
        }

        // 2) Giù
        BlockPos down = from.down();
        if (!visited.contains(down) && world.getBlockState(down).isOf(ModBlocks.CLIMBABLE_ROCK)) return down;
        if (state.isOf(ModBlocks.CLIMBABLE_ROCK)) {
            Direction facing = state.get(ClimbableRock.FACING);
            BlockPos downForward = down.offset(facing);
            if (!visited.contains(downForward) && world.getBlockState(downForward).isOf(ModBlocks.CLIMBABLE_ROCK)) return downForward;

            BlockPos downBackward = down.offset(facing.getOpposite());
            if (!visited.contains(downBackward) && world.getBlockState(downBackward).isOf(ModBlocks.CLIMBABLE_ROCK)) return downBackward;
        }

        // 3) Orizzontali vicini
        Direction[] horizontals = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction dir : horizontals) {
            BlockPos n = from.offset(dir);
            if (!visited.contains(n) && world.getBlockState(n).isOf(ModBlocks.CLIMBABLE_ROCK)) return n;
        }

        return null;
    }
}
