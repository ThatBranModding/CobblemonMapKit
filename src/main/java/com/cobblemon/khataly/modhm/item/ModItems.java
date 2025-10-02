package com.cobblemon.khataly.modhm.item;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.item.custom.BadgeCaseItem;
import com.cobblemon.khataly.modhm.item.custom.BadgeItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    // === MEDAGLIE (semplici, non impilabili) ===
    public static final Item BOULDER_BADGE = registerItem("boulder_badge",
            new BadgeItem(new Item.Settings().maxCount(1)));

    public static final Item CASCADE_BADGE = registerItem("cascade_badge",
            new BadgeItem(new Item.Settings().maxCount(1)));

    public static final Item THUNDER_BADGE = registerItem("thunder_badge",
            new BadgeItem(new Item.Settings().maxCount(1)));

    public static final Item RAINBOW_BADGE = registerItem("rainbow_badge",
            new BadgeItem(new Item.Settings().maxCount(1)));

    public static final Item SOUL_BADGE = registerItem("soul_badge",
            new BadgeItem(new Item.Settings().maxCount(1)));

    public static final Item MARSH_BADGE = registerItem("marsh_badge",
            new BadgeItem(new Item.Settings().maxCount(1)));

    public static final Item VOLCANO_BADGE = registerItem("volcano_badge",
            new BadgeItem(new Item.Settings().maxCount(1)));

    public static final Item EARTH_BADGE = registerItem("earth_badge",
            new BadgeItem(new Item.Settings().maxCount(1)));

    // === PORTA MEDAGLIE (semplice item, unico) ===
    public static final Item BADGE_CASE = registerItem("badge_case",
            new BadgeCaseItem(new Item.Settings().maxCount(1)));

    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(HMMod.MOD_ID, name), item);
    }

    public static void registerModItems(){
        HMMod.LOGGER.info("Registering Mod Items for " + HMMod.MOD_ID);
    }
}
