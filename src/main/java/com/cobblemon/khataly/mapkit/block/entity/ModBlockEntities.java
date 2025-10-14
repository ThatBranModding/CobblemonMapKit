package com.cobblemon.khataly.mapkit.block.entity;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.cobblemon.khataly.mapkit.block.ModBlocks;
import com.cobblemon.khataly.mapkit.block.entity.custom.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static final BlockEntityType<BreakableRockEntity> BREAKABLE_ROCK_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(CobblemonMapKitMod.MOD_ID, "breakable_rock_be"),
                    BlockEntityType.Builder.create(BreakableRockEntity::new, ModBlocks.BREAKABLE_ROCK).build(null));

    public static final BlockEntityType<CuttableTreeEntity> CUTTABLE_TREE_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(CobblemonMapKitMod.MOD_ID, "cuttable_tree_be"),
                    BlockEntityType.Builder.create(CuttableTreeEntity::new, ModBlocks.CUTTABLE_TREE).build(null));

    public static final BlockEntityType<MovableRockEntity> MOVABLE_ROCK =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(CobblemonMapKitMod.MOD_ID, "movable_rock_be"),
                    BlockEntityType.Builder.create(MovableRockEntity::new, ModBlocks.MOVABLE_ROCK).build(null));

    public static final BlockEntityType<ClimbableRockEntity> CLIMBABLE_ROCK_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(CobblemonMapKitMod.MOD_ID, "climbable_rock_be"),
                    BlockEntityType.Builder.create(ClimbableRockEntity::new, ModBlocks.CLIMBABLE_ROCK).build(null));

    public static final BlockEntityType<UltraHolePortalEntity> ULTRAHOLE_ROCK_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(CobblemonMapKitMod.MOD_ID, "ultrahole_rock_be"),
                    BlockEntityType.Builder.create(UltraHolePortalEntity::new, ModBlocks.ULTRAHOLE_PORTAL).build(null));
    public static void registerBlockEntities() {
        CobblemonMapKitMod.LOGGER.info("Registering Block Entities for " + CobblemonMapKitMod.MOD_ID);
    }
}
