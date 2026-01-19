package com.cobblemon.khataly.mapkit.screen;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.cobblemon.khataly.mapkit.screen.custom.CutScreenHandler;
import com.cobblemon.khataly.mapkit.screen.custom.RockClimbScreenHandler;
import com.cobblemon.khataly.mapkit.screen.custom.RockSmashScreenHandler;
import com.cobblemon.khataly.mapkit.screen.custom.StrengthScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ModScreenHandlers {
    public static final ScreenHandlerType<RockSmashScreenHandler> ROCK_SMASH_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(CobblemonMapKitMod.MOD_ID, "rocksmash_screen_handler"),
                    new ExtendedScreenHandlerType<>(RockSmashScreenHandler::new, BlockPos.PACKET_CODEC));

    public static final ScreenHandlerType<CutScreenHandler> CUT_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(CobblemonMapKitMod.MOD_ID, "cut_screen_handler"),
                    new ExtendedScreenHandlerType<>(CutScreenHandler::new, BlockPos.PACKET_CODEC));

    public static final ScreenHandlerType<StrengthScreenHandler> STRENGTH_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(CobblemonMapKitMod.MOD_ID, "strength_screen_handler"),
                    new ExtendedScreenHandlerType<>(StrengthScreenHandler::new, BlockPos.PACKET_CODEC));

    public static final ScreenHandlerType<RockClimbScreenHandler> ROCKCLIMB_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(CobblemonMapKitMod.MOD_ID, "rockclimb_screen_handler"),
                    new ExtendedScreenHandlerType<>(RockClimbScreenHandler::new, BlockPos.PACKET_CODEC));


    public static void registerScreenHandlers() {
        CobblemonMapKitMod.LOGGER.info("Registering Screen Handlers for " + CobblemonMapKitMod.MOD_ID);
    }
}
