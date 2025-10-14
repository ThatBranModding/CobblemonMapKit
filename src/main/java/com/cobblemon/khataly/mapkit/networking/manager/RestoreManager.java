package com.cobblemon.khataly.mapkit.networking.manager;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RestoreManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("RestoreManager");
    private static final RestoreManager INSTANCE = new RestoreManager();
    public static RestoreManager get() { return INSTANCE; }
    private RestoreManager() {}

    /** originalPos -> TimedBlock */
    private final Map<BlockPos, TimedBlock> blocksToRestore = new ConcurrentHashMap<>();
    /** alias: posizione corrente -> posizione originale */
    private final Map<BlockPos, BlockPos> currentToOriginal = new ConcurrentHashMap<>();

    public boolean isBusy(BlockPos originalPos) {
        return blocksToRestore.containsKey(originalPos);
    }

    public TimedBlock getTimed(BlockPos original) {
        return blocksToRestore.get(original);
    }

    public BlockPos resolveOriginal(BlockPos clicked) {
        return currentToOriginal.getOrDefault(clicked, clicked);
    }

    public void forgetAlias(BlockPos current) {
        currentToOriginal.remove(current);
    }

    public void addTimed(BlockPos originalPos, BlockState originalState, int seconds) {
        blocksToRestore.put(originalPos, new TimedBlock(originalState, seconds * 20, null));
    }

    public void registerMove(BlockPos originalPos, BlockPos movedTo, BlockState state, int seconds) {
        TimedBlock tb = blocksToRestore.get(originalPos);
        if (tb != null) {
            tb.movedTo = movedTo;
            tb.ticksLeft = seconds * 20;
        } else {
            tb = new TimedBlock(state, seconds * 20, movedTo);
            blocksToRestore.put(originalPos, tb);
        }
        currentToOriginal.put(movedTo, originalPos);
    }

    public void tick(ServerWorld world) {
        blocksToRestore.entrySet().removeIf(entry -> {
            BlockPos originalPos = entry.getKey();
            TimedBlock tb = entry.getValue();

            tb.ticksLeft--;
            if (tb.ticksLeft > 0) return false;

            if (tb.fallingEntity != null && tb.fallingEntity.isAlive()) {
                tb.fallingEntity.discard();
            }

            if (tb.movedTo != null && !tb.movedTo.equals(originalPos)) {
                BlockPos moved = tb.movedTo;
                BlockState stateAtMoved = world.getBlockState(moved);
                if (stateAtMoved.isOf(tb.blockState.getBlock())) {
                    world.setBlockState(moved, Blocks.AIR.getDefaultState());
                    currentToOriginal.remove(moved);
                } else {
                    final int maxSearch = 64;
                    BlockPos scan = moved.down();
                    int steps = 0;
                    while (scan.getY() >= world.getBottomY() && steps < maxSearch) {
                        BlockState s = world.getBlockState(scan);
                        if (s.isOf(tb.blockState.getBlock())) {
                            world.setBlockState(scan, Blocks.AIR.getDefaultState());
                            currentToOriginal.remove(scan);
                            break;
                        }
                        scan = scan.down();
                        steps++;
                    }
                    currentToOriginal.remove(moved);
                }
            }

            world.setBlockState(originalPos, tb.blockState);
            currentToOriginal.remove(originalPos);
            LOGGER.info("Block restored at {}", originalPos);
            return true;
        });
    }

    public static class TimedBlock {
        public final BlockState blockState;
        public BlockPos movedTo;
        public int ticksLeft;
        public FallingBlockEntity fallingEntity;

        public TimedBlock(BlockState blockState, int ticksLeft, BlockPos movedTo) {
            this.blockState = blockState;
            this.ticksLeft = ticksLeft;
            this.movedTo = movedTo;
        }
    }
}
