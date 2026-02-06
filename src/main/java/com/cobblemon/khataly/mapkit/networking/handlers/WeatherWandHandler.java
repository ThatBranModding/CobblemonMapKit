package com.cobblemon.khataly.mapkit.networking.handlers;

import com.cobblemon.khataly.mapkit.config.LocalWeatherZonesConfig;
import com.cobblemon.khataly.mapkit.item.ModItems;
import com.cobblemon.khataly.mapkit.networking.packet.localweather.PlaceLocalWeatherC2SPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class WeatherWandHandler {

    private WeatherWandHandler() {}

    private static final String NBT_MODE = "weather_mode";
    // Supported values:
    // clear | rain | thunder | sandstorm
    // NOTE: snow is achieved via RAIN in cold biomes

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
                PlaceLocalWeatherC2SPacket.ID,
                (payload, ctx) -> {
                    var player = ctx.player();
                    BlockPos a = payload.a();
                    BlockPos b = payload.b();
                    ctx.server().execute(() -> createZone(player, a, b));
                }
        );
    }

    private static void createZone(ServerPlayerEntity player, BlockPos a, BlockPos b) {
        if (player == null) return;
        World world = player.getWorld();

        int minX = Math.min(a.getX(), b.getX());
        int maxX = Math.max(a.getX(), b.getX());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxZ = Math.max(a.getZ(), b.getZ());
        int minY = Math.min(a.getY(), b.getY());
        int maxY = Math.max(a.getY(), b.getY());

        if (LocalWeatherZonesConfig.overlaps(
                world.getRegistryKey(),
                minX, minZ, maxX, maxZ,
                minY, maxY)) {

            player.sendMessage(
                    Text.literal("Cannot create weather zone: it overlaps an existing one."),
                    false
            );
            return;
        }

        LocalWeatherZonesConfig.LocalWeatherType weather = readWeatherMode(player);
        String zoneName = nextAvailableZoneName();

        UUID id = LocalWeatherZonesConfig.addZone(
                zoneName,
                world.getRegistryKey(),
                minX, minZ, maxX, maxZ,
                minY, maxY,
                weather
        );

        player.sendMessage(Text.literal(
                "Local weather zone created: " + zoneName +
                        " (" + id + ") weather=" +
                        weather.name().toLowerCase(Locale.ROOT)
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

    private static LocalWeatherZonesConfig.LocalWeatherType readWeatherMode(ServerPlayerEntity player) {
        var main = player.getMainHandStack();
        var off  = player.getOffHandStack();

        String mode = null;

        if (!main.isEmpty() && main.getItem() == ModItems.WEATHER_WAND) {
            NbtComponent data = main.get(DataComponentTypes.CUSTOM_DATA);
            if (data != null) mode = data.copyNbt().getString(NBT_MODE);
        } else if (!off.isEmpty() && off.getItem() == ModItems.WEATHER_WAND) {
            NbtComponent data = off.get(DataComponentTypes.CUSTOM_DATA);
            if (data != null) mode = data.copyNbt().getString(NBT_MODE);
        }

        if (mode == null || mode.isBlank()) {
            return LocalWeatherZonesConfig.LocalWeatherType.RAIN;
        }

        String m = mode.trim().toLowerCase(Locale.ROOT);
        return switch (m) {
            case "thunder", "storm" -> LocalWeatherZonesConfig.LocalWeatherType.THUNDER;
            case "sand", "sandstorm" -> LocalWeatherZonesConfig.LocalWeatherType.SANDSTORM;
            case "clear" -> LocalWeatherZonesConfig.LocalWeatherType.CLEAR;
            default -> LocalWeatherZonesConfig.LocalWeatherType.RAIN;
        };
    }
}
