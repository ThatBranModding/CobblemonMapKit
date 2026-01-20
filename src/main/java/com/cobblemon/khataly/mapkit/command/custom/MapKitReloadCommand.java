package com.cobblemon.khataly.mapkit.command.custom;

import com.cobblemon.khataly.mapkit.config.FlyTargetConfig;
import com.cobblemon.khataly.mapkit.config.GrassZonesConfig;
import com.cobblemon.khataly.mapkit.config.HMConfig;
import com.cobblemon.khataly.mapkit.config.LevelCapConfig;
import com.cobblemon.khataly.mapkit.networking.manager.StrengthWindowManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public final class MapKitReloadCommand {
    private MapKitReloadCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("mapkit")
                .requires(src -> src.hasPermissionLevel(2)) // OP level 2+
                .then(CommandManager.literal("reload")
                        .executes(ctx -> reload(ctx.getSource()))
                )
        );
    }

    private static int reload(ServerCommandSource src) {
        try {
            // Reload JSON configs
            HMConfig.load();
            GrassZonesConfig.load();
            FlyTargetConfig.load();
            LevelCapConfig.load();

            // Clear cached runtime state
            StrengthWindowManager.clearAll();

            src.sendFeedback(
                    () -> Text.literal("MapKit configs reloaded"),
                    true
            );

            return 1;
        } catch (Exception e) {
            src.sendError(Text.literal("MapKit reload failed"));
            return 0;
        }
    }
}
