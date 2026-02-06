package com.cobblemon.khataly.mapkit.networking.packet.localweather;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.cobblemon.khataly.mapkit.config.LocalWeatherZonesConfig;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record LocalWeatherZonesSyncS2CPacket(List<ZoneDto> zones) implements CustomPayload {

    public static final CustomPayload.Id<LocalWeatherZonesSyncS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(CobblemonMapKitMod.MOD_ID, "local_weather_zones_sync"));

    public record ZoneDto(
            String id,
            String name,
            String worldKey,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            String weatherType // <-- STRING (ex: "rain", "sandstorm")
    ) {}

    public static final PacketCodec<RegistryByteBuf, LocalWeatherZonesSyncS2CPacket> CODEC =
            PacketCodec.of(
                    // IMPORTANT: (packet, buf) in this order
                    (packet, buf) -> {
                        List<ZoneDto> list = (packet.zones() == null) ? List.of() : packet.zones();
                        buf.writeVarInt(list.size());
                        for (ZoneDto z : list) {
                            buf.writeString(z.id() == null ? "" : z.id(), 64);
                            buf.writeString(z.name() == null ? "" : z.name(), 128);
                            buf.writeString(z.worldKey() == null ? "" : z.worldKey(), 256);

                            buf.writeInt(z.minX()); buf.writeInt(z.minY()); buf.writeInt(z.minZ());
                            buf.writeInt(z.maxX()); buf.writeInt(z.maxY()); buf.writeInt(z.maxZ());

                            String wt = (z.weatherType() == null || z.weatherType().isBlank()) ? "rain" : z.weatherType();
                            buf.writeString(wt, 32);
                        }
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        List<ZoneDto> out = new ArrayList<>(Math.max(0, n));
                        for (int i = 0; i < n; i++) {
                            String id = buf.readString(64);
                            String name = buf.readString(128);
                            String worldKey = buf.readString(256);

                            int minX = buf.readInt(); int minY = buf.readInt(); int minZ = buf.readInt();
                            int maxX = buf.readInt(); int maxY = buf.readInt(); int maxZ = buf.readInt();

                            String wt = buf.readString(32);

                            out.add(new ZoneDto(id, name, worldKey, minX, minY, minZ, maxX, maxY, maxZ, wt));
                        }
                        return new LocalWeatherZonesSyncS2CPacket(out);
                    }
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /** Helper for server -> client sync (if you have a client overlay) */
    public static LocalWeatherZonesSyncS2CPacket fromConfig() {
        var list = new ArrayList<ZoneDto>();
        for (var z : LocalWeatherZonesConfig.getAll()) {
            String wt = (z.weatherType() == null)
                    ? "rain"
                    : z.weatherType().name().toLowerCase(java.util.Locale.ROOT);

            list.add(new ZoneDto(
                    z.id().toString(),
                    z.name(),
                    z.worldKey().getValue().toString(),
                    z.minX(), z.minY(), z.minZ(),
                    z.maxX(), z.maxY(), z.maxZ(),
                    wt
            ));
        }
        return new LocalWeatherZonesSyncS2CPacket(list);
    }
}
