package com.cobblemon.khataly.mapkit.networking.manager;

import com.cobblemon.khataly.mapkit.sound.ModSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.*;

/**
 * Teleport block logic:
 * - Very slow levitation (~0.5s)
 * - Teleports immediately after or if ceiling is hit
 * - Safe landing and anti-loop
 */
public final class TeleportAnimationManager {

    private TeleportAnimationManager() {}

    private static final int ANIM_TICKS = 10;        // ≈0.5s total
    private static final double LIFT_Y = 0.004;      // extremely slow rise
    private static final int ARRIVAL_SUPPRESS_TICKS = 6;

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Long> LAST_ARRIVAL_TICK = new HashMap<>();
    private static final Map<UUID, Loc> ARRIVAL_SUPPRESS_WHILE_ON_BLOCK = new HashMap<>();

    private record Loc(RegistryKey<World> dim, BlockPos block) {}
    private record Pending(ServerWorld targetWorld, BlockPos targetPos, long startTick) {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TeleportAnimationManager::tick);
    }

    public static boolean shouldIgnoreStep(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        long now = world.getTime();
        Long last = LAST_ARRIVAL_TICK.get(player.getUuid());
        if (last != null && (now - last) < ARRIVAL_SUPPRESS_TICKS) return true;

        Loc loc = ARRIVAL_SUPPRESS_WHILE_ON_BLOCK.get(player.getUuid());
        return loc != null && loc.dim.equals(world.getRegistryKey()) && loc.block.equals(pos);
    }

    public static void queueTeleport(ServerPlayerEntity player, ServerWorld targetWorld, BlockPos targetPos) {
        if (PENDING.containsKey(player.getUuid())) return;

        // stop any existing vertical momentum
        var v = player.getVelocity();
        player.setVelocity(v.x, 0.0, v.z);
        player.velocityModified = true;

        long now = player.getServerWorld().getTime();
        PENDING.put(player.getUuid(), new Pending(targetWorld, targetPos, now));

        // start sound + feedback
        player.getServerWorld().playSound(null, player.getBlockPos(),
                ModSounds.TELEPORT_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static void tick(MinecraftServer server) {
        if (!PENDING.isEmpty()) {
            List<UUID> done = new ArrayList<>();

            for (var entry : PENDING.entrySet()) {
                UUID id = entry.getKey();
                Pending pending = entry.getValue();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
                if (player == null) { done.add(id); continue; }

                long now = player.getServerWorld().getTime();
                long elapsed = now - pending.startTick;

                // stop early if player hits ceiling
                if (playerHitCeiling(player)) {
                    doTeleport(player, pending.targetWorld, pending.targetPos);
                    done.add(id);
                    continue;
                }

                // slow levitation
                if (elapsed < ANIM_TICKS) {
                    animateLift(player);
                } else {
                    doTeleport(player, pending.targetWorld, pending.targetPos);
                    done.add(id);
                }
            }

            done.forEach(PENDING::remove);
        }

        // clean up anti-loop suppression when player leaves destination block
        if (!ARRIVAL_SUPPRESS_WHILE_ON_BLOCK.isEmpty()) {
            List<UUID> clear = new ArrayList<>();
            for (var e : ARRIVAL_SUPPRESS_WHILE_ON_BLOCK.entrySet()) {
                UUID pid = e.getKey();
                Loc loc = e.getValue();
                ServerPlayerEntity pl = server.getPlayerManager().getPlayer(pid);
                if (pl == null) { clear.add(pid); continue; }
                if (!pl.getServerWorld().getRegistryKey().equals(loc.dim)) {
                    clear.add(pid);
                    continue;
                }
                BlockPos belowFeet = pl.getBlockPos().down();
                if (!belowFeet.equals(loc.block)) clear.add(pid);
            }
            clear.forEach(ARRIVAL_SUPPRESS_WHILE_ON_BLOCK::remove);
        }
    }

    /* ----------- Ultra slow levitation ----------- */
    private static void animateLift(ServerPlayerEntity player) {
        // constant vertical velocity
        var v = player.getVelocity();
        player.setVelocity(v.x, LIFT_Y, v.z);
        player.fallDistance = 0;
        player.velocityModified = true;

        var w = player.getServerWorld();
        w.spawnParticles(
                ParticleTypes.PORTAL,
                player.getX(), player.getY() + 0.5, player.getZ(),
                8, 0.15, 0.25, 0.15, 0.0
        );
        if (player.age % 5 == 0) {
            w.playSound(null, player.getBlockPos(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
                    SoundCategory.PLAYERS, 0.3f, 1.8f);
        }
    }

    /* ----------- Teleportation logic ----------- */
    private static void doTeleport(ServerPlayerEntity player, ServerWorld targetWorld, BlockPos base) {
        BlockPos safeFeet = findSafeLandingAbove(targetWorld, base);
        double tx = safeFeet.getX() + 0.5;
        double ty = safeFeet.getY();
        double tz = safeFeet.getZ() + 0.5;

        player.setVelocity(0, 0, 0);
        player.velocityModified = true;
        player.teleport(targetWorld, tx, ty, tz, player.getYaw(), player.getPitch());

        LAST_ARRIVAL_TICK.put(player.getUuid(), targetWorld.getTime());
        ARRIVAL_SUPPRESS_WHILE_ON_BLOCK.put(player.getUuid(), new Loc(targetWorld.getRegistryKey(), base));

        targetWorld.playSound(null, base, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0f, 1.0f);
        targetWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                tx, ty + 0.5, tz, 25, 0.4, 0.4, 0.4, 0.0);
    }

    /* ----------- Ceiling check ----------- */
    private static boolean playerHitCeiling(ServerPlayerEntity player) {
        BlockPos head = player.getBlockPos().up();
        return !player.getWorld().getBlockState(head).isAir();
    }

    /* ----------- Safe landing finder ----------- */
    private static BlockPos findSafeLandingAbove(ServerWorld world, BlockPos base) {
        final int x = base.getX();
        final int z = base.getZ();

        int startY = Math.max(base.getY() + 1, world.getBottomY() + 1);
        int heightmapY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        int maxY = Math.min(world.getTopY(), heightmapY + 6);

        for (int feetY = startY; feetY <= maxY; feetY++) {
            BlockPos feet = new BlockPos(x, feetY, z);
            BlockPos head = feet.up();
            BlockPos below = feet.down();

            BlockState feetState = world.getBlockState(feet);
            BlockState headState = world.getBlockState(head);
            BlockState belowState = world.getBlockState(below);

            boolean spaceFree = feetState.isAir() && headState.isAir();
            boolean canStand = belowState.isOpaqueFullCube(world, below)
                    || belowState.isSideSolidFullSquare(world, below, Direction.UP);

            if (spaceFree && canStand) return feet;
        }
        return base.up(1);
    }
}
