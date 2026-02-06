package com.cobblemon.khataly.mapkit.event.client.custom;

import com.cobblemon.khataly.mapkit.item.ModItems;
import com.cobblemon.khataly.mapkit.networking.packet.localweather.PlaceLocalWeatherC2SPacket;
import com.cobblemon.khataly.mapkit.networking.packet.localweather.RequestLocalWeatherZonesC2SPacket;
import com.cobblemon.khataly.mapkit.networking.packet.localweather.LocalWeatherZonesSyncS2CPacket;
import com.cobblemon.khataly.mapkit.util.LocalWeatherClientCache;
import com.cobblemon.khataly.mapkit.util.RenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

@Environment(EnvType.CLIENT)
public final class LocalWeatherWandClient {

    private static final String NBT_WEATHER = "weather_type"; // "snow" | "sandstorm" etc

    private static BlockPos startPos = null;
    private static BlockPos curPos = null;
    private static boolean selecting = false;

    private static int zonesRefreshCooldown = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean holding = isHoldingWand(client);

            if (holding) {
                if (zonesRefreshCooldown <= 0) {
                    ClientPlayNetworking.send(new RequestLocalWeatherZonesC2SPacket());
                    zonesRefreshCooldown = 40; // ~2s
                } else zonesRefreshCooldown--;
            } else {
                zonesRefreshCooldown = 0;
            }

            boolean using = holding && client.player.isUsingItem();

            if (using) {
                if (!selecting) {
                    startPos = rayBlock(client);
                    selecting = startPos != null;
                }
                curPos = rayBlock(client);
            } else {
                if (selecting && startPos != null && curPos != null) {
                    String weatherType = readWeatherType(client);
                    ClientPlayNetworking.send(new PlaceLocalWeatherC2SPacket(startPos, curPos, weatherType));
                }
                selecting = false;
                startPos = null;
                curPos = null;
            }
        });

        // render existing zones + selection box
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            var mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;

            MatrixStack matrices = ctx.matrixStack();
            var providers = ctx.consumers();
            if (providers == null || matrices == null) return;

            var cam = ctx.camera();
            double camX = cam.getPos().x;
            double camY = cam.getPos().y;
            double camZ = cam.getPos().z;

            if (isHoldingWand(mc)) {
                var worldKeyStr = mc.world.getRegistryKey().getValue().toString();

                for (LocalWeatherZonesSyncS2CPacket.ZoneDto z : LocalWeatherClientCache.getZones()) {
                    if (!z.worldKey().equals(worldKeyStr)) continue;

                    double minX = Math.min(z.minX(), z.maxX());
                    double maxX = Math.max(z.minX(), z.maxX()) + 1;
                    double minY = Math.min(z.minY(), z.maxY());
                    double maxY = Math.max(z.minY(), z.maxY()) + 1;
                    double minZ = Math.min(z.minZ(), z.maxZ());
                    double maxZ = Math.max(z.minZ(), z.maxZ()) + 1;

                    Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ).offset(-camX, -camY, -camZ);

                    // Cyan-ish overlay
                    RenderUtils.drawFilledBox(matrices, providers, box, 0f, 1f, 1f, 0.10f);
                    RenderUtils.drawOutlineBox(matrices, box, 0f, 1f, 1f, 0.90f);
                }
            }

            if (selecting && startPos != null && curPos != null) {
                double minX = Math.min(startPos.getX(), curPos.getX());
                double minY = Math.min(startPos.getY(), curPos.getY());
                double minZ = Math.min(startPos.getZ(), curPos.getZ());
                double maxX = Math.max(startPos.getX(), curPos.getX()) + 1;
                double maxY = Math.max(startPos.getY(), curPos.getY()) + 1;
                double maxZ = Math.max(startPos.getZ(), curPos.getZ()) + 1;

                Box sel = new Box(minX, minY, minZ, maxX, maxY, maxZ).offset(-camX, -camY, -camZ);

                // Blue selection overlay
                RenderUtils.drawFilledBox(matrices, providers, sel, 0f, 0.4f, 1f, 0.22f);
                RenderUtils.drawOutlineBox(matrices, sel, 0f, 0.6f, 1f, 0.9f);
            }
        });
    }

    private static boolean isHoldingWand(MinecraftClient client) {
        ItemStack main = client.player.getMainHandStack();
        ItemStack off  = client.player.getOffHandStack();
        return (!main.isEmpty() && main.getItem() == ModItems.WEATHER_WAND)
                || (!off.isEmpty() && off.getItem() == ModItems.WEATHER_WAND);
    }

    private static String readWeatherType(MinecraftClient client) {
        ItemStack main = client.player.getMainHandStack();
        ItemStack off  = client.player.getOffHandStack();

        ItemStack stack = (!main.isEmpty() && main.getItem() == ModItems.WEATHER_WAND) ? main
                : (!off.isEmpty() && off.getItem() == ModItems.WEATHER_WAND) ? off
                : ItemStack.EMPTY;

        if (stack.isEmpty()) return "snow";

        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return "snow";

        String t = data.copyNbt().getString(NBT_WEATHER);
        if (t == null || t.isBlank()) return "snow";
        return t;
    }

    private static BlockPos rayBlock(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit instanceof BlockHitResult bhr) return bhr.getBlockPos();
        return null;
    }
}
