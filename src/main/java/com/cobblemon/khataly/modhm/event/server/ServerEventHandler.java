package com.cobblemon.khataly.modhm.event.server;

import com.cobblemon.khataly.modhm.event.server.custom.ServerFlashHandler;
import com.cobblemon.khataly.modhm.event.server.custom.ServerFlyHandler;
import com.cobblemon.khataly.modhm.event.server.custom.ServerTeleportHandler;


public class ServerEventHandler {
    public static void register() {
        ServerFlyHandler.register();
        ServerFlashHandler.register();
        ServerTeleportHandler.register();
    }
}
