package com.cobblemon.khataly.modhm.command;


import com.cobblemon.khataly.modhm.command.custom.FlyTargetCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

public class ModCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        FlyTargetCommand.register(dispatcher);
    }

}
