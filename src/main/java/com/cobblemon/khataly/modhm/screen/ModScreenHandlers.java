package com.cobblemon.khataly.modhm.screen;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.screen.custom.CutScreenHandler;
import com.cobblemon.khataly.modhm.screen.custom.RockClimbScreenHandler;
import com.cobblemon.khataly.modhm.screen.custom.RockSmashScreenHandler;
import com.cobblemon.khataly.modhm.screen.custom.StrengthScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ModScreenHandlers {
    public static final ScreenHandlerType<RockSmashScreenHandler> ROCK_SMASH_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(HMMod.MOD_ID, "rocksmash_screen_handler"),
                    new ExtendedScreenHandlerType<>(RockSmashScreenHandler::new, BlockPos.PACKET_CODEC));

    public static final ScreenHandlerType<CutScreenHandler> CUT_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(HMMod.MOD_ID, "cut_screen_handler"),
                    new ExtendedScreenHandlerType<>(CutScreenHandler::new, BlockPos.PACKET_CODEC));

    public static final ScreenHandlerType<StrengthScreenHandler> STRENGHT_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(HMMod.MOD_ID, "strenght_screen_handler"),
                    new ExtendedScreenHandlerType<>(StrengthScreenHandler::new, BlockPos.PACKET_CODEC));

    public static final ScreenHandlerType<RockClimbScreenHandler> ROCKCLIMB_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(HMMod.MOD_ID, "rockclimb_screen_handler"),
                    new ExtendedScreenHandlerType<>(RockClimbScreenHandler::new, BlockPos.PACKET_CODEC));


    public static void registerScreenHandlers() {
        HMMod.LOGGER.info("Registering Screen Handlers for " + HMMod.MOD_ID);
    }
}
