package com.cobblemon.khataly.modhm.block.custom;

import com.cobblemon.khataly.modhm.block.entity.ModBlockEntities;
import com.cobblemon.khataly.modhm.block.entity.custom.UltraHolePortalEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class UltraHolePortal extends BlockWithEntity {

    public static final MapCodec<UltraHolePortal> CODEC = createCodec(UltraHolePortal::new);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    // Spessore “lastra” del portale
    private static final double TH = 0.5;

    // Forme per ogni direzione (piano sottile verticale)
    private static final VoxelShape SHAPE_NORTH = Block.createCuboidShape(0, 0, 0, 16, 16, TH);
    private static final VoxelShape SHAPE_SOUTH = Block.createCuboidShape(0, 0, 16 - TH, 16, 16, 16);
    private static final VoxelShape SHAPE_WEST  = Block.createCuboidShape(0, 0, 0, TH, 16, 16);
    private static final VoxelShape SHAPE_EAST  = Block.createCuboidShape(16 - TH, 0, 0, 16, 16, 16);

    public UltraHolePortal(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    // ----- State -----

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // orienta il portale verso la direzione del giocatore
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    // ----- Forma/Rendering -----

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case EAST  -> SHAPE_EAST;
            default    -> SHAPE_NORTH;
        };
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    // ----- BlockEntity -----

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new UltraHolePortalEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        if (type != ModBlockEntities.ULTRAHOLE_ROCK_BE) return null; // <-- verifica il tuo id giusto
        return (serverWorld, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof UltraHolePortalEntity portal) {
                portal.tick();
            }
        };
    }

    // ----- Posizionamento “un blocco sopra” mantenendo il facing -----

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient) {
            // rimuove il blocco alla posizione cliccata
            world.removeBlock(pos, false);

            // ripiazza un blocco più in alto, preservando il facing
            BlockPos newPos = pos.up(1);
            BlockState newState = state.with(FACING, state.get(FACING));
            world.setBlockState(newPos, newState, 3);

            BlockEntity be = world.getBlockEntity(newPos);
            if (be instanceof UltraHolePortalEntity portal) {
                portal.markDirty();
            }
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof UltraHolePortalEntity portal) {
                portal.removePortal();
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
