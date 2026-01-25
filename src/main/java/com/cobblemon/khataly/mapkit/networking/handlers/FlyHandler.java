package com.cobblemon.khataly.mapkit.networking.handlers;

import com.cobblemon.khataly.mapkit.networking.packet.fly.FlyPacketC2S;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class FlyHandler {

    private FlyHandler() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
                FlyPacketC2S.ID,
                (payload, ctx) -> ctx.server().execute(() -> handle(ctx.server(), ctx.player(), payload))
        );
    }

    private static void handle(MinecraftServer server, ServerPlayerEntity player, FlyPacketC2S packet) {
        if (server == null || player == null || packet == null) return;

        Identifier worldId = packet.worldKeyId();
        BlockPos pos = packet.pos();
        if (pos == null) return;

        RegistryKey<net.minecraft.world.World> targetKey =
                RegistryKey.of(RegistryKeys.WORLD, worldId);

        ServerWorld targetWorld = server.getWorld(targetKey);
        if (targetWorld == null) {
            player.sendMessage(Text.literal("That destination no longer exists: " + worldId), false);
            return;
        }

        // force-load destination chunk
        targetWorld.getChunk(pos);

        // teleport cross-dimension
        player.teleport(
                targetWorld,
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                player.getYaw(),
                player.getPitch()
        );
    }
}
