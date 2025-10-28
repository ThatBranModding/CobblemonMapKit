package com.cobblemon.khataly.mapkit.command;


import com.cobblemon.khataly.mapkit.command.custom.FlyTargetCommand;
import com.cobblemon.khataly.mapkit.command.custom.GrassZoneCommands;
import com.cobblemon.khataly.mapkit.command.custom.LevelCapCommands;
import com.cobblemon.khataly.mapkit.command.custom.TeleportBlockCommands;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

public class ModCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        FlyTargetCommand.register(dispatcher);
        GrassZoneCommands.register(dispatcher);
        LevelCapCommands.register(dispatcher);
        TeleportBlockCommands.register(dispatcher);
    }


}
