package com.cobblemon.khataly.mapkit.entity.render;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.cobblemon.khataly.mapkit.entity.BicycleEntity;
import com.cobblemon.khataly.mapkit.entity.model.BicycleEntityModel;
import com.cobblemon.khataly.mapkit.entity.model.ModModelLayers;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class BicycleRenderer extends EntityRenderer<BicycleEntity> {

    private static final Identifier TEX =
             Identifier.of(CobblemonMapKitMod.MOD_ID, "textures/entity/bicycle.png");

    private final BicycleEntityModel model;

    public BicycleRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new BicycleEntityModel(ctx.getPart(ModModelLayers.BICYCLE));
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(BicycleEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider buffers, int light) {
        matrices.push();

        // 1) orientamento (yaw interpolato passato dal dispatcher)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));

        // 2) flip "alla vanilla": scala negativa su X e Y
        //    (evita la rotazione X che inverte anche la traslazione precedente)
        float s = 2.8f; // scala del tuo modello
        matrices.scale(-s, -s, s);

        // 3) traslazione verticale dopo il flip (root pivot = 24px ≈ 1.5 blocchi)
        //    LivingEntityRenderer usa ~1.501F
        matrices.translate(0.0, -1.501F, 0.0);

        // 4) eventuali animazioni
        this.model.setAngles(entity, 0, 0, entity.age + tickDelta, 0, 0);

        var layer = RenderLayer.getEntityCutoutNoCull(TEX);
        var vc = buffers.getBuffer(layer);
        this.model.render(matrices, vc, light, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, buffers, light);
    }

    @Override
    public Identifier getTexture(BicycleEntity entity) {
        return TEX;
    }
}
