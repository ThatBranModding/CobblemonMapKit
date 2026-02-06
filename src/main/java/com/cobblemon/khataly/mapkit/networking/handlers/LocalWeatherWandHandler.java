package com.cobblemon.khataly.mapkit.networking.handlers;

import com.cobblemon.khataly.mapkit.config.LocalWeatherZonesConfig;
import com.cobblemon.khataly.mapkit.networking.packet.localweather.PlaceLocalWeatherC2SPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class LocalWeatherWandHandler {

    private LocalWeatherWandHandler() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
                PlaceLocalWeatherC2SPacket.ID,
                (payload, ctx) -> ctx.server().execute(() -> createZone(ctx.player(), payload))
        );
    }

    private static void createZone(ServerPlayerEntity player, PlaceLocalWeatherC2SPacket payload) {
        if (player == null) return;
        World world = player.getWorld();

        BlockPos a = payload.a();
        BlockPos b = payload.b();

        int minX = Math.min(a.getX(), b.getX());
        int maxX = Math.max(a.getX(), b.getX());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxZ = Math.max(a.getZ(), b.getZ());
        int minY = Math.min(a.getY(), b.getY());
        int maxY = Math.max(a.getY(), b.getY());

        if (LocalWeatherZonesConfig.overlaps(world.getRegistryKey(), minX, minZ, maxX, maxZ, minY, maxY)) {
            player.sendMessage(Text.literal("Cannot create weather zone: it overlaps an existing one."), false);
            return;
        }

        // Convert string -> enum
        LocalWeatherZonesConfig.LocalWeatherType weatherType = parseWeatherType(payload.weatherType());

        String zoneName = nextAvailableZoneName();

        UUID id = LocalWeatherZonesConfig.addZone(
                zoneName,
                world.getRegistryKey(),
                minX, minZ, maxX, maxZ,
                minY, maxY,
                weatherType
        );

        player.sendMessage(Text.literal(
                "Local weather zone created: " + zoneName + " (" + id + ") weather=" +
                        weatherType.name().toLowerCase(Locale.ROOT)
        ), false);
    }

    private static String nextAvailableZoneName() {
        Set<String> existing = LocalWeatherZonesConfig.getAll()
                .stream()
                .map(z -> z.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        int i = 1;
        while (true) {
            String candidate = "WeatherZone" + i;
            if (!existing.contains(candidate.toLowerCase(Locale.ROOT))) return candidate;
            i++;
        }
    }

    private static LocalWeatherZonesConfig.LocalWeatherType parseWeatherType(String raw) {
        if (raw == null || raw.isBlank()) return LocalWeatherZonesConfig.LocalWeatherType.RAIN;

        String m = raw.trim().toLowerCase(Locale.ROOT);
        return switch (m) {
            case "clear" -> LocalWeatherZonesConfig.LocalWeatherType.CLEAR;
            case "thunder", "storm" -> LocalWeatherZonesConfig.LocalWeatherType.THUNDER;
            case "sand", "sandstorm" -> LocalWeatherZonesConfig.LocalWeatherType.SANDSTORM;
            case "snow" -> LocalWeatherZonesConfig.LocalWeatherType.SNOW; // if your enum has SNOW
            default -> LocalWeatherZonesConfig.LocalWeatherType.RAIN;
        };
    }
}
