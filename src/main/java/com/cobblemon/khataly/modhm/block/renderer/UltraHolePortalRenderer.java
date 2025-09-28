package com.cobblemon.khataly.modhm.block.renderer;

import com.cobblemon.khataly.modhm.block.entity.custom.UltraHolePortalEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

public class UltraHolePortalRenderer implements BlockEntityRenderer<UltraHolePortalEntity> {
    private static final Random RANDOM = new Random();

    public UltraHolePortalRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(UltraHolePortalEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return;

        BlockPos pos = entity.getPos();

        // ---- Parametri temporali ----
        int growDuration = 200;      // circa 10 secondi
        int stableDuration = 600;    // circa 30 secondi
        int shrinkDuration = 200;    // circa 10 secondi
        float maxRadiusFinal = 1.5f; // portale più piccolo
        float minRadius = 0.05f;     // raggio minimo per evitare sparizione immediata

        // ---- Aggiorna età portale ----
        int localAge = entity.getAge() + 1;
        entity.setAge(localAge);

        double time = localAge + tickDelta;

        // ---- Calcolo raggio ----
        float radius;
        if (localAge < growDuration) {
            // fase di crescita lenta
            float progress = (localAge + tickDelta) / (float) growDuration;
            radius = maxRadiusFinal * progress;
        } else if (localAge < growDuration + stableDuration) {
            // fase stabile
            radius = maxRadiusFinal;
        } else if (localAge < growDuration + stableDuration + shrinkDuration) {
            // fase di shrink lineare lenta
            int shrinkAge = localAge - (growDuration + stableDuration);
            float progress = 1.0f - (shrinkAge + tickDelta) / (float) shrinkDuration;
            radius = Math.max(maxRadiusFinal * progress, minRadius);
        } else {
            // mantiene un raggio minimo fino alla rimozione
            radius = minRadius;
        }

        // ---- Rendering cerchi concentrici ----
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEndPortal());

        int depthLayers = 10;
        int segments = 90;

        for (int layer = 0; layer < depthLayers; layer++) {
            float zOffset = -0.12f * layer;
            float alpha = 200 - layer * 15;
            float layerRadius = radius * (1.0f - layer * 0.07f);
            double rotation = time * 0.02 * (layer % 2 == 0 ? 1 : -1);

            for (int i = 0; i < segments; i++) {
                double angle1 = 2 * Math.PI * i / segments + rotation;
                double angle2 = 2 * Math.PI * (i + 1) / segments + rotation;

                float x1 = (float)(Math.cos(angle1) * layerRadius);
                float y1 = (float)(Math.sin(angle1) * layerRadius);
                float x2 = (float)(Math.cos(angle2) * layerRadius);
                float y2 = (float)(Math.sin(angle2) * layerRadius);

                int rCol = (int)(120 + 120 * Math.sin(time * 0.05 + i));
                int gCol = (int)(80 + 100 * Math.cos(time * 0.07 + i));
                int bCol = 255;

                buffer.vertex(matrices.peek().getPositionMatrix(), 0, 0, zOffset)
                        .color(rCol, gCol, bCol, (int)alpha)
                        .light(light)
                        .normal(0f, 0f, 1f);

                buffer.vertex(matrices.peek().getPositionMatrix(), x1, y1, zOffset)
                        .color(rCol, gCol, bCol, (int)alpha)
                        .light(light)
                        .normal(0f, 0f, 1f);

                buffer.vertex(matrices.peek().getPositionMatrix(), x2, y2, zOffset)
                        .color(rCol, gCol, bCol, (int)alpha)
                        .light(light)
                        .normal(0f, 0f, 1f);
            }
        }
        matrices.pop();

        // ---- Particelle ----
        int particleCount = (int)(25 * (radius / maxRadiusFinal));
        for (int i = 0; i < particleCount; i++) {
            double px = pos.getX() + (RANDOM.nextDouble() - 0.5) * radius * 2;
            double py = pos.getY() + RANDOM.nextDouble() * 2 * (radius / maxRadiusFinal);
            double pz = pos.getZ() + (RANDOM.nextDouble() - 0.5) * radius * 2;
            double vx = (RANDOM.nextDouble() - 0.5) * 0.1;
            double vy = (RANDOM.nextDouble() - 0.5) * 0.1;
            double vz = (RANDOM.nextDouble() - 0.5) * 0.1;
            world.addParticle(ParticleTypes.PORTAL, px, py, pz, vx, vy, vz);
        }
    }
}
