package com.cobblemon.khataly.modhm.block;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.block.custom.BreakableRock;
import com.cobblemon.khataly.modhm.block.custom.ClimbableRock;
import com.cobblemon.khataly.modhm.block.custom.CuttableTree;
import com.cobblemon.khataly.modhm.block.custom.MovableRock;
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



    private static Block registerBlock(String name, Block block){
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(HMMod.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block){
        Registry.register(Registries.ITEM, Identifier.of(HMMod.MOD_ID, name),
                new BlockItem(block, new Item.Settings()));
    }

    public  static void registerModBlocks(){
        HMMod.LOGGER.info("Registering Mod Blocks for " + HMMod.MOD_ID);

    }
}
