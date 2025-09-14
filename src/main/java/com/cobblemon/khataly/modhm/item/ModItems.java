package com.cobblemon.khataly.modhm.item;

import com.cobblemon.khataly.modhm.HMMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {


    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(HMMod.MOD_ID, name), item);
    }

    public static void registerModItems(){
        HMMod.LOGGER.info("Registering Mod Items for " + HMMod.MOD_ID);
    }
}
