package com.cobblemon.khataly.mapkit.networking;

import com.cobblemon.khataly.mapkit.networking.manager.ClimbManager;
import com.cobblemon.khataly.mapkit.networking.manager.RestoreManager;
import com.cobblemon.khataly.mapkit.networking.manager.TeleportAnimationManager;
import net.minecraft.server.MinecraftServer;

public class NetworkingTick {
    public static void tick(MinecraftServer server) {
        RestoreManager.get().tick(server.getOverworld());
        ClimbManager.get().tick();
        TeleportAnimationManager.register();
    }
}
