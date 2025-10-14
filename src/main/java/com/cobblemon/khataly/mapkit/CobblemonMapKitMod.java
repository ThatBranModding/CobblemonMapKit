package com.cobblemon.khataly.mapkit;


import com.cobblemon.khataly.mapkit.block.ModBlocks;
import com.cobblemon.khataly.mapkit.block.entity.ModBlockEntities;
import com.cobblemon.khataly.mapkit.command.ModCommands;
import com.cobblemon.khataly.mapkit.config.GrassZonesConfig;
import com.cobblemon.khataly.mapkit.config.LevelCapConfig;
import com.cobblemon.khataly.mapkit.config.HMConfig;
import com.cobblemon.khataly.mapkit.event.server.ServerEventHandler;
import com.cobblemon.khataly.mapkit.event.server.custom.GrassEncounterTicker;
import com.cobblemon.khataly.mapkit.item.ModItemGroups;
import com.cobblemon.khataly.mapkit.item.ModItems;
import com.cobblemon.khataly.mapkit.config.FlyTargetConfig;
import com.cobblemon.khataly.mapkit.networking.ModNetworking;
import com.cobblemon.khataly.mapkit.networking.handlers.BadgeTagUseHandler;
import com.cobblemon.khataly.mapkit.screen.ModScreenHandlers;
import com.cobblemon.khataly.mapkit.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CobblemonMapKitMod implements ModInitializer {
    public static final String MOD_ID = "mapkit";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        HMConfig.load();
        GrassZonesConfig.load();
        FlyTargetConfig.load();
        LevelCapConfig.load();

        ModSounds.registerSounds();
        ModScreenHandlers.registerScreenHandlers();
        ModNetworking.registerPackets();
        ModBlockEntities.registerBlockEntities();
        ModItems.registerModItems();
        ModBlocks.registerModBlocks();
        ModItemGroups.registerItemGroups();
        BadgeTagUseHandler.register();
        ServerEventHandler.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ModCommands.register(dispatcher);
        });
        GrassEncounterTicker.register();
        ServerTickEvents.END_SERVER_TICK.register(ModNetworking::tick);



        LOGGER.info("Hello Fabric world!");
    }
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
