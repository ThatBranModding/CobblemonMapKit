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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ClimbManager {
    private static final ClimbManager INSTANCE = new ClimbManager();
    public static ClimbManager get() { return INSTANCE; }
    private ClimbManager() {}

    private final Map<ServerPlayerEntity, BlockPos> playersClimbing = new ConcurrentHashMap<>();
    private final Map<ServerPlayerEntity, Integer> climbingTicks = new ConcurrentHashMap<>();
    private final Map<ServerPlayerEntity, Set<BlockPos>> visitedClimbBlocks = new ConcurrentHashMap<>();
    private final Map<ServerPlayerEntity, BlockPos> climbingTargets = new ConcurrentHashMap<>();
    private final Set<ServerPlayerEntity> playersPlayingSound = ConcurrentHashMap.newKeySet();

    public void start(ServerPlayerEntity player, BlockPos startPos) {
        playersClimbing.put(player, startPos);
        climbingTicks.put(player, 0);
    }

    public void tick() {
        playersClimbing.forEach((player, startPos) -> {
            if (!player.isAlive()) {
                cleanup(player);
                return;
            }

            climbingTicks.put(player, climbingTicks.getOrDefault(player, 0) + 1);

            visitedClimbBlocks.putIfAbsent(player, new HashSet<>());
            Set<BlockPos> visited = visitedClimbBlocks.get(player);
            visited.add(startPos);

            BlockPos target = climbingTargets.get(player);
            if (target == null) {
                target = findNextClimbStep((ServerWorld) player.getWorld(), startPos, visited);
                if (target == null) {
                    NetUtil.msg(player, "🧗 No climbable blocks!");
                    cleanup(player);
                    return;
                }
                climbingTargets.put(player, target);
                visited.add(target);
            }

            double dx = target.getX() + 0.5 - player.getX();
            double dy = target.getY() + 1.0 - player.getY();
            double dz = target.getZ() + 0.5 - player.getZ();
            double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);

            if (distance < 0.2) {
                BlockPos next = findNextClimbStep((ServerWorld) player.getWorld(), target, visited);
                if (next == null) {
                    double finalBoost = dy > 0 ? 0.2 : -0.2;
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

            double speed = 0.15;
            player.setVelocity(dx / distance * speed, dy / distance * speed, dz / distance * speed);
            player.velocityModified = true;

            int tickDelay = 4;
            if (climbingTicks.get(player) >= tickDelay && !playersPlayingSound.contains(player)) {
                player.playSoundToPlayer(ModSounds.CLIMBABLE_ROCK, SoundCategory.PLAYERS, 1f, 1f);
                playersPlayingSound.add(player);
            }
        });
    }

    private void cleanup(ServerPlayerEntity player) {
        playersClimbing.remove(player);
        climbingTicks.remove(player);
        playersPlayingSound.remove(player);
        climbingTargets.remove(player);
        visitedClimbBlocks.remove(player);
    }

    private BlockPos findNextClimbStep(ServerWorld world, BlockPos from, Set<BlockPos> visited) {
        BlockState state = world.getBlockState(from);

        // 1) su
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

        // 2) giù
        BlockPos down = from.down();
        if (!visited.contains(down) && world.getBlockState(down).isOf(ModBlocks.CLIMBABLE_ROCK)) return down;
        if (state.isOf(ModBlocks.CLIMBABLE_ROCK)) {
            Direction facing = state.get(ClimbableRock.FACING);
            BlockPos downForward = down.offset(facing);
            if (!visited.contains(downForward) && world.getBlockState(downForward).isOf(ModBlocks.CLIMBABLE_ROCK)) return downForward;

            BlockPos downBackward = down.offset(facing.getOpposite());
            if (!visited.contains(downBackward) && world.getBlockState(downBackward).isOf(ModBlocks.CLIMBABLE_ROCK)) return downBackward;
        }

        // 3) orizzontali vicini
        Direction[] horizontals = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction dir : horizontals) {
            BlockPos n = from.offset(dir);
            if (!visited.contains(n) && world.getBlockState(n).isOf(ModBlocks.CLIMBABLE_ROCK)) return n;
        }

        return null;
    }
}
