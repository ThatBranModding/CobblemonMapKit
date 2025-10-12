package com.cobblemon.khataly.modhm.networking.handlers;

import com.cobblemon.khataly.modhm.block.ModBlocks;
import com.cobblemon.khataly.modhm.block.entity.custom.UltraHolePortalEntity;
import com.cobblemon.khataly.modhm.config.ModConfig;
import com.cobblemon.khataly.modhm.networking.packet.ultrahole.UltraHolePacketC2S;
import com.cobblemon.khataly.modhm.networking.util.NetUtil;
import com.cobblemon.khataly.modhm.util.PlayerUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public final class UltraHoleHandler {
    private UltraHoleHandler() {}

    private static final Map<UUID, BlockPos> activePortals = new HashMap<>();

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(UltraHolePacketC2S.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            ctx.server().execute(() -> {
                List<String> requiredMoves = List.of("sunsteelstrike", "moongeistbeam");
                Optional<String> known = requiredMoves.stream().filter(m -> PlayerUtils.hasMove(p, m)).findFirst();
                if (known.isEmpty()) {
                    NetUtil.msg(p, "❌ None of your Pokémon know the moves required to open an UltraHole!");
                    return;
                }
                if (!NetUtil.requireItem(p, ModConfig.ULTRAHOLE.item, ModConfig.ULTRAHOLE.message)) return;

                if (activePortals.containsKey(p.getUuid())) {
                    NetUtil.msg(p, "⚠️ You already have an active UltraHole portal!");
                    return;
                }

                NetUtil.sendAnimation(p, known.get());

                BlockPos portalPos = p.getBlockPos().offset(p.getHorizontalFacing(), 5).up(1);
                p.getWorld().setBlockState(portalPos, ModBlocks.ULTRAHOLE_PORTAL.getDefaultState());

                var be = p.getWorld().getBlockEntity(portalPos);
                if (be instanceof UltraHolePortalEntity portal) {
                    String currentDim = p.getWorld().getRegistryKey().getValue().toString();
                    String targetDim = ModConfig.ULTRAHOLE_SETTINGS.destinationDimension;

                    if (currentDim.equals(targetDim)) {
                        BlockPos spawn = Objects.requireNonNull(p.getServer()).getOverworld().getSpawnPos();
                        portal.setTarget("minecraft:overworld", spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
                    } else {
                        portal.setTarget(targetDim, ModConfig.ULTRAHOLE_SETTINGS.x, ModConfig.ULTRAHOLE_SETTINGS.y, ModConfig.ULTRAHOLE_SETTINGS.z);
                    }

                    portal.setOnRemove(() -> activePortals.remove(p.getUuid()));
                    activePortals.put(p.getUuid(), portalPos);
                }
            });
        });
    }
}
