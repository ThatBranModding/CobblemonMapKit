package com.cobblemon.khataly.mapkit.event.server.custom;

import com.cobblemon.khataly.mapkit.config.LocalWeatherZonesConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player local weather controller.
 * Uses client-side weather packets only (does NOT affect world weather).
 */
public final class LocalWeatherTicker {

    private LocalWeatherTicker() {}

    private static final Map<UUID, LocalWeatherZonesConfig.LocalWeatherType> LAST = new HashMap<>();
    private static int particleTicker = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(LocalWeatherTicker::onTick);
    }

    private static void onTick(MinecraftServer server) {
        particleTicker++;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator()) continue;

            var worldKey = player.getServerWorld().getRegistryKey();
            var pos = player.getBlockPos();

            var zone = LocalWeatherZonesConfig.findBestAt(
                    worldKey,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
            );

            LocalWeatherZonesConfig.LocalWeatherType type =
                    (zone == null)
                            ? LocalWeatherZonesConfig.LocalWeatherType.CLEAR
                            : zone.weatherType();

            var last = LAST.get(player.getUuid());
            if (last != type) {
                applyLocalWeather(player, type);
                LAST.put(player.getUuid(), type);
            }

            // Sandstorm particles (~every 5 ticks)
            if (type == LocalWeatherZonesConfig.LocalWeatherType.SANDSTORM && (particleTicker % 5 == 0)) {
                sendSandParticles(player);
            }
        }
    }

    private static void applyLocalWeather(ServerPlayerEntity player,
                                          LocalWeatherZonesConfig.LocalWeatherType type) {

        switch (type) {
            case CLEAR -> {
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STOPPED, 0f));
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, 0f));
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, 0f));
            }

            case RAIN, SNOW -> {
                // Snow automatically renders in cold biomes
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, 0f));
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, 1f));
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, 0f));
            }

            case THUNDER -> {
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, 0f));
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, 1f));
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, 1f));
            }

            case SANDSTORM -> {
                // No rain visuals — sandstorm is particles only
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STOPPED, 0f));
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, 0f));
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, 0f));
            }
        }
    }

    private static void sendSandParticles(ServerPlayerEntity player) {
        Vec3d p = player.getPos();

        // CLOUD works as a valid ParticleEffect (unlike DUST)
        var pkt = new ParticleS2CPacket(
                ParticleTypes.CLOUD,
                true,
                p.x,
                p.y + 1.0,
                p.z,
                1.2f,
                0.6f,
                1.2f,
                0.01f,
                18
        );

        player.networkHandler.sendPacket(pkt);
    }
}
