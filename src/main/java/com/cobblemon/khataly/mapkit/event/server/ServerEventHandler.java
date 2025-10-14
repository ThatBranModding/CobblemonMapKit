package com.cobblemon.khataly.mapkit.event.server;

import com.cobblemon.khataly.mapkit.event.server.custom.*;


public class ServerEventHandler {
    public static void register() {
        ServerFlyHandler.register();
        ServerFlashHandler.register();
        ServerTeleportHandler.register();
        ServerUltraHoleHandler.register();
        LevelCapEnforcer.register();
        LevelCapProgressionWatcher.register();
        FlyTargetProximityWatcher.register();
    }
}
