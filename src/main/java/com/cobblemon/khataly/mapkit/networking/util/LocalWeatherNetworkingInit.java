package com.cobblemon.khataly.mapkit.networking.util;

import com.cobblemon.khataly.mapkit.networking.packet.localweather.LocalWeatherZonesSyncS2CPacket;
import com.cobblemon.khataly.mapkit.util.LocalWeatherClientCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class LocalWeatherNetworkingInit {
    private LocalWeatherNetworkingInit() {}

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(
                LocalWeatherZonesSyncS2CPacket.ID,
                (payload, ctx) -> ctx.client().execute(() ->
                        LocalWeatherClientCache.setZones(payload.zones()))
        );
    }
}
