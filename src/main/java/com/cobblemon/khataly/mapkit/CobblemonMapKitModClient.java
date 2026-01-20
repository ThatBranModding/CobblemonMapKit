package com.cobblemon.khataly.mapkit;

import com.cobblemon.khataly.mapkit.block.ModBlocks;
import com.cobblemon.khataly.mapkit.block.entity.ModBlockEntities;
import com.cobblemon.khataly.mapkit.block.renderer.UltraHolePortalRenderer;
import com.cobblemon.khataly.mapkit.entity.ModEntities;
import com.cobblemon.khataly.mapkit.entity.model.BicycleEntityModel;
import com.cobblemon.khataly.mapkit.entity.model.ModModelLayers;
import com.cobblemon.khataly.mapkit.entity.render.BicycleRenderer;
import com.cobblemon.khataly.mapkit.entity.render.ModEntityRenderers;
import com.cobblemon.khataly.mapkit.event.client.ClientEventHandler;
import com.cobblemon.khataly.mapkit.item.ModItems;
import com.cobblemon.khataly.mapkit.networking.handlers.BadgeBoxClientHandler;
import com.cobblemon.khataly.mapkit.networking.handlers.CurioCaseClientHandler;
import com.cobblemon.khataly.mapkit.networking.packet.RotatePlayerS2CPacket;
import com.cobblemon.khataly.mapkit.networking.packet.bike.ToggleBikeGearC2SPacket;
import com.cobblemon.khataly.mapkit.networking.packet.bike.BikeWheelieC2SPacket;
import com.cobblemon.khataly.mapkit.networking.util.ClientAnimationState;
import com.cobblemon.khataly.mapkit.networking.util.GrassNetworkingInit;
import com.cobblemon.khataly.mapkit.screen.ModScreenHandlers;
import com.cobblemon.khataly.mapkit.screen.custom.CutScreen;
import com.cobblemon.khataly.mapkit.screen.custom.RockClimbScreen;
import com.cobblemon.khataly.mapkit.screen.custom.RockSmashScreen;
import com.cobblemon.khataly.mapkit.screen.custom.StrengthScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class CobblemonMapKitModClient implements ClientModInitializer {

    private boolean wasUsePressed   = false;
    private boolean wasJumpPressed  = false;

    @Override
    public void onInitializeClient() {
        registerScreensAndBlocks();
        registerRenderers();
        registerClientReceivers();
        registerClientEvents();
        registerBicycleInputHandlers();
    }

    private void registerScreensAndBlocks() {
        HandledScreens.register(ModScreenHandlers.ROCK_SMASH_SCREEN_HANDLER, RockSmashScreen::new);
        HandledScreens.register(ModScreenHandlers.CUT_SCREEN_HANDLER,        CutScreen::new);
        HandledScreens.register(ModScreenHandlers.STRENGTH_SCREEN_HANDLER,   StrengthScreen::new);
        HandledScreens.register(ModScreenHandlers.ROCKCLIMB_SCREEN_HANDLER,  RockClimbScreen::new);

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CLIMBABLE_ROCK,   RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ULTRAHOLE_PORTAL, RenderLayer.getCutout());

        BlockEntityRendererFactories.register(ModBlockEntities.ULTRAHOLE_ROCK_BE, UltraHolePortalRenderer::new);
    }

    private void registerRenderers() {
        ArmorRenderer invisibleBoots = (matrices, vertexConsumers, stack, entity, slot, light, model) -> {};
        ArmorRenderer.register(invisibleBoots, ModItems.RUNNING_SHOES);

        ModEntityRenderers.register();
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.BICYCLE, BicycleEntityModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.BICYCLE, BicycleRenderer::new);
    }

    private void registerClientReceivers() {
        BadgeBoxClientHandler.register();
        CurioCaseClientHandler.register();

        GrassNetworkingInit.registerReceivers();

        ClientPlayNetworking.registerGlobalReceiver(RotatePlayerS2CPacket.ID, (payload, ctx) -> {
            float total = payload.totalRotation();
            int ticks   = Math.max(1, payload.durationTicks());
            ctx.client().execute(() -> {
                ClientAnimationState.rotationPerTick = total / ticks;
                ClientAnimationState.ticksRemaining  = ticks;
            });
        });
    }

    private void registerClientEvents() {
        ClientEventHandler.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ClientAnimationState.ticksRemaining > 0 && client.player != null) {
                float delta = ClientAnimationState.rotationPerTick;
                var p = client.player;
                p.setYaw(p.getYaw() + delta);
                p.setHeadYaw(p.getHeadYaw() + delta);
                ClientAnimationState.ticksRemaining--;
                if (ClientAnimationState.ticksRemaining == 0) {
                    ClientAnimationState.rotationPerTick = 0f;
                }
            }
        });
    }

    private void registerBicycleInputHandlers() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            final var player = client.player;
            if (client.world == null || player == null) return;

            final boolean ridingBike = player.hasVehicle()
                    && player.getVehicle() instanceof com.cobblemon.khataly.mapkit.entity.BicycleEntity;

            boolean usePressed  = client.options.useKey.isPressed();
            boolean useJustDown = usePressed && !wasUsePressed;
            wasUsePressed = usePressed;

            if (useJustDown && ridingBike && player.getMainHandStack().isEmpty()) {
                ClientPlayNetworking.send(new ToggleBikeGearC2SPacket());
            }

            boolean jumpPressed  = client.options.jumpKey.isPressed();
            boolean jumpChanged  = jumpPressed != wasJumpPressed;
            wasJumpPressed = jumpPressed;

            if (jumpChanged && ridingBike) {
                ClientPlayNetworking.send(new BikeWheelieC2SPacket(jumpPressed));
            }
        });
    }
}
