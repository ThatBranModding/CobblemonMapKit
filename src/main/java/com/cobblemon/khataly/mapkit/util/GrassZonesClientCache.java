package com.cobblemon.khataly.mapkit.util;
import com.cobblemon.khataly.mapkit.networking.packet.grasszones.GrassZonesSyncS2CPacket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class GrassZonesClientCache {
    private GrassZonesClientCache() {}
    private static final AtomicReference<List<GrassZonesSyncS2CPacket.ZoneDto>> ZONES =
            new AtomicReference<>(List.of());

    public static void setZones(List<GrassZonesSyncS2CPacket.ZoneDto> zones) {
        ZONES.set(zones == null ? List.of() : List.copyOf(zones));
    }
    public static List<GrassZonesSyncS2CPacket.ZoneDto> getZones() {
        return Collections.unmodifiableList(ZONES.get());
    }
}
