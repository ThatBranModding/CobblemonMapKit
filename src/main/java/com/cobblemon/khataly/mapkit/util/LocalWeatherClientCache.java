package com.cobblemon.khataly.mapkit.util;

import com.cobblemon.khataly.mapkit.networking.packet.localweather.LocalWeatherZonesSyncS2CPacket;

import java.util.Collections;
import java.util.List;

public final class LocalWeatherClientCache {
    private LocalWeatherClientCache() {}

    private static volatile List<LocalWeatherZonesSyncS2CPacket.ZoneDto> ZONES = List.of();

    public static void setZones(List<LocalWeatherZonesSyncS2CPacket.ZoneDto> zones) {
        ZONES = (zones == null) ? List.of() : List.copyOf(zones);
    }

    public static List<LocalWeatherZonesSyncS2CPacket.ZoneDto> getZones() {
        return Collections.unmodifiableList(ZONES);
    }
}
