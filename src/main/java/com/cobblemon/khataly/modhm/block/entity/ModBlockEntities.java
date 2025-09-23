package com.cobblemon.khataly.modhm.block.entity;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.block.ModBlocks;
import com.cobblemon.khataly.modhm.block.entity.custom.BreakableRockEntity;
import com.cobblemon.khataly.modhm.block.entity.custom.ClimbableRockEntity;
import com.cobblemon.khataly.modhm.block.entity.custom.CuttableTreeEntity;
import com.cobblemon.khataly.modhm.block.entity.custom.MovableRockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static final BlockEntityType<BreakableRockEntity> BREAKABLE_ROCK_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(HMMod.MOD_ID, "breakable_rock_be"),
                    BlockEntityType.Builder.create(BreakableRockEntity::new, ModBlocks.BREAKABLE_ROCK).build(null));

    public static final BlockEntityType<CuttableTreeEntity> CUTTABLE_TREE_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(HMMod.MOD_ID, "cuttable_tree_be"),
                    BlockEntityType.Builder.create(CuttableTreeEntity::new, ModBlocks.CUTTABLE_TREE).build(null));

    public static final BlockEntityType<MovableRockEntity> MOVABLE_ROCK =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(HMMod.MOD_ID, "movable_rock_be"),
                    BlockEntityType.Builder.create(MovableRockEntity::new, ModBlocks.MOVABLE_ROCK).build(null));

    public static final BlockEntityType<ClimbableRockEntity> CLIMBABLE_ROCK_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(HMMod.MOD_ID, "climbable_rock_be"),
                    BlockEntityType.Builder.create(ClimbableRockEntity::new, ModBlocks.CLIMBABLE_ROCK).build(null));

    public static void registerBlockEntities() {
        HMMod.LOGGER.info("Registering Block Entities for " + HMMod.MOD_ID);
    }
}
