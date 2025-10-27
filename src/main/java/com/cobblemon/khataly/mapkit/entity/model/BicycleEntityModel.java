package com.cobblemon.khataly.mapkit.entity.model;

import com.cobblemon.khataly.mapkit.entity.BicycleEntity;
import com.cobblemon.khataly.mapkit.entity.anim.BicycleAnimations;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;


// Made with Blockbench 5.0.3
// Exported for Minecraft version 1.17+ for Yarn
// Paste this class into your mod and generate all required imports
public class BicycleEntityModel extends SinglePartEntityModel<BicycleEntity> {
	private final ModelPart bike;
	private final ModelPart steel;
	private final ModelPart wheelr;
	private final ModelPart wheelf;
	public BicycleEntityModel(ModelPart root) {
		this.bike = root.getChild("bike");
		this.steel = this.bike.getChild("steel");
		this.wheelr = this.bike.getChild("wheelr");
		this.wheelf = this.bike.getChild("wheelf");
	}
	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData bike = modelPartData.addChild("bike", ModelPartBuilder.create(), ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, -1.6144F, 0.0F));

		ModelPartData steel = bike.addChild("steel", ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -1.0F, 0.0F, 3.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(0, 7).cuboid(-3.0F, -1.0F, 0.0F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -2.0F, 0.0F));

		ModelPartData steel_r1 = steel.addChild("steel_r1", ModelPartBuilder.create().uv(0, 2).cuboid(-1.0F, -1.0F, 0.0F, 3.0F, 1.0F, 1.0F, new Dilation(-0.001F)), ModelTransform.of(-4.5638F, -2.4528F, 1.0407F, 3.1416F, 1.5272F, -2.138F));

		ModelPartData steel_r2 = steel.addChild("steel_r2", ModelPartBuilder.create().uv(6, 7).cuboid(-1.0F, -1.0F, 0.0F, 2.0F, 1.0F, 1.0F, new Dilation(-0.001F)), ModelTransform.of(-3.5376F, -0.842F, -0.001F, 0.0F, 0.0F, 1.0036F));

		ModelPartData wheelr = bike.addChild("wheelr", ModelPartBuilder.create().uv(6, 4).cuboid(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(1.0F, -1.0F, 0.5F));

		ModelPartData wheelf = bike.addChild("wheelf", ModelPartBuilder.create().uv(0, 4).cuboid(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.pivot(-2.0F, -1.0F, 0.5F));
		return TexturedModelData.of(modelData, 16, 16);
	}
	@Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
		bike.render(matrices, vertexConsumer, light, overlay, color);
	}

	@Override
	public ModelPart getPart() {
		return bike;
	}

	@Override
	public void setAngles(BicycleEntity entity, float limbSwing, float limbSwingAmount,
						  float ageInTicks, float netHeadYaw, float headPitch) {
		// reset pose
		this.getPart().traverse().forEach(ModelPart::resetTransform);
		// anima ruote usando l’AnimationState sull’entity
		this.updateAnimation(entity.goingAnimation, BicycleAnimations.GOING, ageInTicks, 1.0F);
	}

}