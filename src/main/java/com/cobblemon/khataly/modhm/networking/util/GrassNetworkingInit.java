package com.cobblemon.khataly.modhm.networking.util;

import com.cobblemon.khataly.modhm.networking.packet.grasszones.GrassZonesSyncS2CPacket;
import com.cobblemon.khataly.modhm.util.GrassZonesClientCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class GrassNetworkingInit {
    private GrassNetworkingInit() {}
    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(
                GrassZonesSyncS2CPacket.ID,
                (payload, ctx) -> ctx.client().execute(() ->
                        GrassZonesClientCache.setZones(payload.zones()))
        );
    }
}
