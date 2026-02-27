package com.cobblemon.khataly.mapkit.networking.handlers;

import com.cobblemon.khataly.mapkit.config.FlyTargetConfig;
import com.cobblemon.khataly.mapkit.config.HMConfig;
import com.cobblemon.khataly.mapkit.config.PlayerFlyProgress;
import com.cobblemon.khataly.mapkit.networking.packet.AnimationHMPacketS2C;
import com.cobblemon.khataly.mapkit.networking.packet.fly.FlyPacketC2S;
import com.cobblemon.khataly.mapkit.util.PlayerUtils;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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

        if (!PlayerUtils.hasRequiredItem(player, HMConfig.FLY.item)) {
            String msg = (HMConfig.FLY.message == null || HMConfig.FLY.message.isBlank())
                    ? "You can't use Fly yet."
                    : HMConfig.FLY.message;
            player.sendMessage(Text.literal(msg), false);
            return;
        }

        UUID pokemonId = packet.pokemonId();
        Identifier worldId = packet.worldKeyId();
        BlockPos pos = packet.pos();
        if (worldId == null || pos == null) return;

        String matchedKey = findTargetKeyByWorldAndPos(worldId, pos);
        if (matchedKey == null) {
            player.sendMessage(Text.literal("That destination no longer exists."), false);
            return;
        }

        String keyLower = matchedKey.toLowerCase(Locale.ROOT);
        if (!PlayerFlyProgress.isUnlocked(player.getUuid(), keyLower)) {
            player.sendMessage(Text.literal("You haven't unlocked that Fly destination yet."), false);
            return;
        }

        RegistryKey<World> targetKey = RegistryKey.of(RegistryKeys.WORLD, worldId);
        ServerWorld targetWorld = server.getWorld(targetKey);
        if (targetWorld == null) {
            player.sendMessage(Text.literal("That destination no longer exists: " + worldId), false);
            return;
        }

        var renderable = findRenderableFromParty(player, pokemonId);
        if (renderable != null) {
            ServerPlayNetworking.send(player, new AnimationHMPacketS2C(renderable, "fly"));
        }

        targetWorld.getChunk(pos);

        player.teleport(
                targetWorld,
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                player.getYaw(),
                player.getPitch()
        );
    }

    private static String findTargetKeyByWorldAndPos(Identifier worldId, BlockPos pos) {
        Map<String, FlyTargetConfig.TargetInfo> all = FlyTargetConfig.getAllTargets();
        if (all == null || all.isEmpty()) return null;

        for (var e : all.entrySet()) {
            if (e == null || e.getValue() == null) continue;
            FlyTargetConfig.TargetInfo info = e.getValue();
            if (info.worldKey == null || info.pos == null) continue;

            if (worldId.equals(info.worldKey.getValue()) && pos.equals(info.pos)) {
                return e.getKey();
            }
        }
        return null;
    }

    private static com.cobblemon.mod.common.pokemon.RenderablePokemon findRenderableFromParty(ServerPlayerEntity player, UUID pokemonId) {
        try {
            var party = Cobblemon.INSTANCE.getStorage().getParty(player);
            for (Pokemon p : party) {
                if (p != null && p.getUuid().equals(pokemonId)) {
                    return p.asRenderablePokemon();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}