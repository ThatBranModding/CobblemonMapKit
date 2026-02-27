package com.cobblemon.khataly.mapkit.networking.util;

import com.cobblemon.khataly.mapkit.networking.packet.AnimationHMPacketS2C;
import com.cobblemon.khataly.mapkit.util.PlayerUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class NetUtil {
    private NetUtil() {}

    public static boolean requireMove(ServerPlayerEntity player, String move, String failMessage) {
        if (!PlayerUtils.hasMove(player, move)) {
            msg(player, failMessage);
            return false;
        }
        return true;
    }

    public static boolean requireItem(ServerPlayerEntity player, String requiredItemIdOrNull, String failureMessage) {
        if (requiredItemIdOrNull != null && !PlayerUtils.hasRequiredItem(player, requiredItemIdOrNull)) {
            msg(player, failureMessage);
            return false;
        }
        return true;
    }

    /**
     * Sends the HM animation packet to the client.
     *
     * @param player The player to send to.
     * @param hmId   The HM id / animation id (e.g. "fly", "cut", "rocksmash", "strength", "teleport", "ultrahole").
     *
     * NOTE: For most HMs, hmId matches the move gate string, but it doesn't have to.
     */
    public static void sendAnimation(ServerPlayerEntity player, String hmId) {
        var renderable = PlayerUtils.getRenderPokemonByMove(player, hmId);

        // If hmId isn't literally a move name (e.g. "ultrahole"), you may still want
        // to show *some* Pokémon. If your PlayerUtils method requires a real move,
        // renderable might be null. In that case, we just skip the animation packet.
        if (renderable != null) {
            ServerPlayNetworking.send(player, new AnimationHMPacketS2C(renderable, hmId));
        }
    }

    public static void teleportTo(ServerPlayerEntity player, ServerWorld world, double x, double y, double z) {
        player.teleport(world, x, y, z, player.getYaw(), player.getPitch());
    }

    public static void sendParticles(ServerPlayerEntity player, ParticleEffect type, BlockPos pos,
                                     float offX, float offY, float offZ, float speed, int count) {
        player.networkHandler.sendPacket(new ParticleS2CPacket(
                type, true,
                pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f,
                offX, offY, offZ, speed, count
        ));
    }

    public static void playPlayerSound(ServerPlayerEntity player, SoundEvent sound) {
        player.playSoundToPlayer(sound, SoundCategory.PLAYERS, 1f, 1f);
    }

    public static void msg(ServerPlayerEntity player, String text) {
        player.sendMessage(Text.literal(text), false);
    }

    public static String capFirst(String s) {
        return (s == null || s.isEmpty()) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}