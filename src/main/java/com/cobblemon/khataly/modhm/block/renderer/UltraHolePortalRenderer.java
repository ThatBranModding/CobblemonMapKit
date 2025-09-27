package com.cobblemon.khataly.modhm.block.renderer;

import com.cobblemon.khataly.modhm.block.entity.custom.UltraHolePortalEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

public class UltraHolePortalRenderer implements BlockEntityRenderer<UltraHolePortalEntity> {
    private static final Random RANDOM = new Random();

    public UltraHolePortalRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(UltraHolePortalEntity entity, float tickDelta, net.minecraft.client.util.math.MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return;
        BlockPos pos = entity.getPos();

        double time = world.getTime() + tickDelta;

        // Spirale centrale
        for (int i = 0; i < 20; i++) {
            double angle = i * Math.PI * 0.2 + time * 0.1;
            double radius = 0.5 + 0.2 * Math.sin(time * 0.05 + i);
            double x = pos.getX() + 0.5 + radius * Math.cos(angle);
            double y = pos.getY() + 0.5 + i * 0.05;
            double z = pos.getZ() + 0.5 + radius * Math.sin(angle);

            double vx = Math.cos(angle) * 0.02;
            double vy = 0.01;
            double vz = Math.sin(angle) * 0.02;

            world.addParticle(ParticleTypes.PORTAL, x, y, z, vx, vy, vz);
            world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.03, 0);
        }

        // Piccole particelle casuali per effetto “frattura spazio-tempo”
        for (int i = 0; i < 10; i++) {
            double px = pos.getX() + RANDOM.nextDouble();
            double py = pos.getY() + RANDOM.nextDouble() * 2;
            double pz = pos.getZ() + RANDOM.nextDouble();
            double vx = (RANDOM.nextDouble() - 0.5) * 0.1;
            double vy = (RANDOM.nextDouble() - 0.5) * 0.1;
            double vz = (RANDOM.nextDouble() - 0.5) * 0.1;
            world.addParticle(ParticleTypes.ENCHANT, px, py, pz, vx, vy, vz);
        }
    }
}
