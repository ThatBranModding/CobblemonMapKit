package com.cobblemon.khataly.mapkit.block;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.cobblemon.khataly.mapkit.block.custom.*;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block BREAKABLE_ROCK = registerBlock("breakable_rock",
            new BreakableRock(AbstractBlock.Settings.create()
                    .strength(-1.0F, 3600000.0F)
                    .nonOpaque()));

    public static final Block CUTTABLE_TREE = registerBlock("cuttable_tree",
            new CuttableTree(AbstractBlock.Settings.create()
                    .strength(-1.0F, 3600000.0F)
                    .nonOpaque()));

    public static final Block MOVABLE_ROCK = registerBlock("movable_rock",
            new MovableRock(AbstractBlock.Settings.create()
                    .strength(-1.0F, 3600000.0F)
                    .nonOpaque()));

    public static final Block CLIMBABLE_ROCK = registerBlock("climbable_rock",
            new ClimbableRock(AbstractBlock.Settings.create()
                    .strength(-1.0F, 3600000.0F)
                    .nonOpaque()));

    public static final Block ULTRAHOLE_PORTAL = registerBlock("ultrahole_portal",
            new UltraHolePortal(AbstractBlock.Settings.create()
                    .strength(-1.0F, 3600000.0F)
                    .nonOpaque()));

    public static final Block DIRECTIONAL_PANEL_BLOCK = registerBlock("directional_panel_block",
            new DirectionalPanelBlock(AbstractBlock.Settings.create()
                    .strength(-1.0F, 3600000.0F)
                    .nonOpaque()));

    public static final Block TELEPORT_BLOCK = registerBlock("teleport_block",
            new TeleportBlock(AbstractBlock.Settings.create()
                    .strength(-1.0F, 3600000.0F)
            ));

    private static Block registerBlock(String name, Block block){
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(CobblemonMapKitMod.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block){
        Registry.register(Registries.ITEM, Identifier.of(CobblemonMapKitMod.MOD_ID, name),
                new BlockItem(block, new Item.Settings()));
    }

    public  static void registerModBlocks(){
        CobblemonMapKitMod.LOGGER.info("Registering Mod Blocks for " + CobblemonMapKitMod.MOD_ID);

    }
}
