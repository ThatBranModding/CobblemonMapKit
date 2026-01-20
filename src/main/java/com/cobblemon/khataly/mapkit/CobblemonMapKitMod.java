package com.cobblemon.khataly.mapkit;

import com.cobblemon.khataly.mapkit.block.ModBlocks;
import com.cobblemon.khataly.mapkit.block.entity.ModBlockEntities;
import com.cobblemon.khataly.mapkit.command.ModCommands;
import com.cobblemon.khataly.mapkit.config.*;
import com.cobblemon.khataly.mapkit.entity.BicycleEntity;
import com.cobblemon.khataly.mapkit.entity.ModEntities;
import com.cobblemon.khataly.mapkit.event.server.ServerEventHandler;
import com.cobblemon.khataly.mapkit.event.server.custom.GrassEncounterTicker;
import com.cobblemon.khataly.mapkit.item.ModItemGroups;
import com.cobblemon.khataly.mapkit.item.ModItems;
import com.cobblemon.khataly.mapkit.networking.ModNetworking;
import com.cobblemon.khataly.mapkit.networking.handlers.BadgeTagUseHandler;
import com.cobblemon.khataly.mapkit.networking.handlers.CurioTagUseHandler;
import com.cobblemon.khataly.mapkit.networking.manager.TeleportAnimationManager;
import com.cobblemon.khataly.mapkit.screen.ModScreenHandlers;
import com.cobblemon.khataly.mapkit.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
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
        CurioTagUseHandler.register();

        ServerEventHandler.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ModCommands.register(dispatcher);
        });
        GrassEncounterTicker.register();
        ServerTickEvents.END_SERVER_TICK.register(ModNetworking::tick);
        ModEntities.register();
        TeleportAnimationManager.register();

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player.hasVehicle() && player.getVehicle() instanceof BicycleEntity bike) {
                if (!world.isClient) {
                    bike.toggleGear(player);
                }
                return TypedActionResult.success(player.getStackInHand(hand), world.isClient());
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });

        LOGGER.info("MapKit mod loaded ✅");
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
