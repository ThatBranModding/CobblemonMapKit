package com.cobblemon.khataly.modhm.block.custom;

import com.cobblemon.khataly.modhm.block.entity.custom.UltraHolePortalEntity;
import com.cobblemon.khataly.modhm.block.entity.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class UltraHolePortal extends BlockWithEntity {
    public static final MapCodec<UltraHolePortal> CODEC = createCodec(UltraHolePortal::new);

    public UltraHolePortal(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL; // Usa modello normale
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new UltraHolePortalEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null; // Solo server
        if (type != ModBlockEntities.ULTRAHOLE_ROCK_BE) return null;

        return (serverWorld, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof UltraHolePortalEntity portalEntity) {
                portalEntity.tick();
            }
        };
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof UltraHolePortalEntity portal) {
                portal.removePortal(); // rimuove il BlockEntity se il blocco è cambiato/distrutto
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
