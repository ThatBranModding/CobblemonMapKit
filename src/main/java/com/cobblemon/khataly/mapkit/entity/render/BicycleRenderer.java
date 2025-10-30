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

/**
 * Renderer della bicicletta con impennata e saltelli.
 * Ora la ruota posteriore rimane a terra e quella anteriore si alza.
 */
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

        // 1) Rotazione yaw (orientamento)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));

        // 2) Impennata: pitch NEGATIVO = ruota anteriore si alza
        float pitch = entity.getPitch(tickDelta);
        if (pitch != 0) {
            // cambia il segno rispetto a prima per alzare la ruota anteriore
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-pitch));
        }

        // 3) Spostamento indietro durante l’impennata
        float offset = entity.getWheelieOffset();
        if (offset > 0) {
            matrices.translate(0.0, 0.0, -offset);
        }

        // 4) Scala e traslazione base
        float scale = 2.0f;
        matrices.scale(-scale, -scale, scale);
        matrices.translate(0.0, -1.501F, 0.0);

        // 5) Animazioni del modello
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
