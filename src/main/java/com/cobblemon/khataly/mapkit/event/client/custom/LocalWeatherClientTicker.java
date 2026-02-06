package com.cobblemon.khataly.mapkit.event.client.custom;

import com.cobblemon.khataly.mapkit.networking.packet.localweather.RequestLocalWeatherZonesC2SPacket;
import com.cobblemon.khataly.mapkit.util.LocalWeatherClientCache;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

public final class LocalWeatherClientTicker {

    private LocalWeatherClientTicker() {}

    private static int refreshCooldown = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        // refresh zones every ~5 seconds
        if (refreshCooldown <= 0) {
            ClientPlayNetworking.send(new RequestLocalWeatherZonesC2SPacket());
            refreshCooldown = 100;
        } else {
            refreshCooldown--;
        }

        String worldKeyStr = client.world.getRegistryKey().getValue().toString();
        BlockPos p = client.player.getBlockPos();

        // find first matching zone
        String weatherType = null;
        for (var z : LocalWeatherClientCache.getZones()) {
            if (!z.worldKey().equals(worldKeyStr)) continue;

            int minX = Math.min(z.minX(), z.maxX());
            int maxX = Math.max(z.minX(), z.maxX());
            int minY = Math.min(z.minY(), z.maxY());
            int maxY = Math.max(z.minY(), z.maxY());
            int minZ = Math.min(z.minZ(), z.maxZ());
            int maxZ = Math.max(z.minZ(), z.maxZ());

            if (p.getX() >= minX && p.getX() <= maxX
                    && p.getY() >= minY && p.getY() <= maxY
                    && p.getZ() >= minZ && p.getZ() <= maxZ) {
                weatherType = z.weatherType();
                break;
            }
        }

        if (weatherType == null) return;

        weatherType = weatherType.trim().toLowerCase(Locale.ROOT);

        // simple particle “weather”
        double x = client.player.getX();
        double y = client.player.getY() + 1.2;
        double z = client.player.getZ();

        // spawn a few particles around player
        for (int i = 0; i < 6; i++) {
            double ox = (client.player.getRandom().nextDouble() - 0.5) * 6.0;
            double oy = client.player.getRandom().nextDouble() * 2.0;
            double oz = (client.player.getRandom().nextDouble() - 0.5) * 6.0;

            if ("snow".equals(weatherType)) {
                client.world.addParticle(ParticleTypes.SNOWFLAKE, x + ox, y + oy, z + oz, 0.0, -0.02, 0.0);
            } else if ("sandstorm".equals(weatherType) || "sand".equals(weatherType)) {
                // “sandstorm-like” haze (vanilla-safe particles)
                client.world.addParticle(ParticleTypes.ASH, x + ox, y + oy, z + oz, 0.02, 0.0, 0.02);
            } else {
                // fallback
                client.world.addParticle(ParticleTypes.CLOUD, x + ox, y + oy, z + oz, 0.0, 0.0, 0.0);
            }
        }
    }
}
