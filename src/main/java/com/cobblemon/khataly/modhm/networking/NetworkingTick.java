package com.cobblemon.khataly.modhm.networking;

import com.cobblemon.khataly.modhm.networking.manager.ClimbManager;
import com.cobblemon.khataly.modhm.networking.manager.RestoreManager;
import net.minecraft.server.MinecraftServer;

public class NetworkingTick {
    public static void tick(MinecraftServer server) {
        RestoreManager.get().tick(server.getOverworld());
        ClimbManager.get().tick();
    }
}
