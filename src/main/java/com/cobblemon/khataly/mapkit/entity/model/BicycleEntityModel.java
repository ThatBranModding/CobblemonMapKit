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
	private final ModelPart Bike;
	private final ModelPart Seat;
	private final ModelPart Pedals;
	private final ModelPart RightPedal;
	private final ModelPart LeftPedal;
	private final ModelPart RightSide;
	private final ModelPart FrontWheel;
	private final ModelPart LeftSide;
	private final ModelPart Handle;
	private final ModelPart wheelf;
	private final ModelPart BackWheel;
	private final ModelPart LeftSide2;
	private final ModelPart RightSide2;
	private final ModelPart wheelb;
	public BicycleEntityModel(ModelPart root) {
		this.Bike = root.getChild("Bike");
		this.Seat = this.Bike.getChild("Seat");
		this.Pedals = this.Bike.getChild("Pedals");
		this.RightPedal = this.Pedals.getChild("RightPedal");
		this.LeftPedal = this.Pedals.getChild("LeftPedal");
		this.RightSide = this.Bike.getChild("RightSide");
		this.FrontWheel = this.Bike.getChild("FrontWheel");
		this.LeftSide = this.FrontWheel.getChild("LeftSide");
		this.Handle = this.FrontWheel.getChild("Handle");
		this.wheelf = this.FrontWheel.getChild("wheelf");
		this.BackWheel = this.Bike.getChild("BackWheel");
		this.LeftSide2 = this.BackWheel.getChild("LeftSide2");
		this.RightSide2 = this.BackWheel.getChild("RightSide2");
		this.wheelb = this.BackWheel.getChild("wheelb");
	}
	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData Bike = modelPartData.addChild("Bike", ModelPartBuilder.create(), ModelTransform.pivot(-0.025F, 16.35F, -4.925F));

		ModelPartData Seat = Bike.addChild("Seat", ModelPartBuilder.create(), ModelTransform.pivot(0.025F, 7.65F, 6.825F));

		ModelPartData Seat_r1 = Seat.addChild("Seat_r1", ModelPartBuilder.create().uv(12, 12).cuboid(0.0F, -0.5F, 0.5F, 1.0F, 3.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(1.0F, -8.875F, -1.825F, 0.0F, 0.0F, 1.5708F));

		ModelPartData SeatShaft_r1 = Seat.addChild("SeatShaft_r1", ModelPartBuilder.create().uv(26, 37).cuboid(0.0F, -3.5F, 0.5F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-0.5F, -5.525F, -1.825F, -0.4625F, 0.0F, 0.0F));

		ModelPartData Pedals = Bike.addChild("Pedals", ModelPartBuilder.create().uv(15, 0).cuboid(-1.5F, 0.5F, -0.5F, 2.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.525F, 3.725F, 4.375F));

		ModelPartData RightPedal = Pedals.addChild("RightPedal", ModelPartBuilder.create(), ModelTransform.pivot(-1.5F, 1.5F, 0.5F));

		ModelPartData cube_r1 = RightPedal.addChild("cube_r1", ModelPartBuilder.create().uv(21, 52).cuboid(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, 0.3F, 0.0F, 1.5708F, 0.0F));

		ModelPartData cube_r2 = RightPedal.addChild("cube_r2", ModelPartBuilder.create().uv(21, 52).cuboid(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 0.0F, 0.3F, 0.0F, 1.5708F, -1.5708F));

		ModelPartData cube_r3 = RightPedal.addChild("cube_r3", ModelPartBuilder.create().uv(21, 50).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, -1.0F, 0.025F, 0.0F, 3.1416F, 0.0F));

		ModelPartData LeftPedal = Pedals.addChild("LeftPedal", ModelPartBuilder.create().uv(21, 50).cuboid(0.0F, 1.0F, -1.075F, 2.0F, 0.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.5F, 1.5F, 0.6F));

		ModelPartData cube_r4 = LeftPedal.addChild("cube_r4", ModelPartBuilder.create().uv(21, 52).mirrored().cuboid(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 1.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.of(1.0F, 0.0F, -0.325F, 0.0F, 1.5708F, -3.1416F));

		ModelPartData cube_r5 = LeftPedal.addChild("cube_r5", ModelPartBuilder.create().uv(21, 52).mirrored().cuboid(-1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 1.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.of(1.0F, 0.0F, -0.325F, 0.0F, 1.5708F, 1.5708F));

		ModelPartData RightSide = Bike.addChild("RightSide", ModelPartBuilder.create(), ModelTransform.pivot(-0.45F, 7.65F, 4.925F));

		ModelPartData Shaft_r1 = RightSide.addChild("Shaft_r1", ModelPartBuilder.create().uv(0, 12).cuboid(0.002F, -0.505F, -4.4901F, 0.0F, 1.0F, 6.0F, new Dilation(0.0F)), ModelTransform.of(1.025F, -3.55F, -1.175F, -0.7854F, 0.0F, 0.0F));

		ModelPartData Shaft_r2 = RightSide.addChild("Shaft_r2", ModelPartBuilder.create().uv(22, 20).cuboid(0.0F, -0.505F, -4.4901F, 0.0F, 1.0F, 5.0F, new Dilation(0.0F)), ModelTransform.of(1.05F, -2.475F, -5.6F, -1.9635F, 0.0F, 0.0F));

		ModelPartData FrontWheel = Bike.addChild("FrontWheel", ModelPartBuilder.create(), ModelTransform.pivot(0.025F, 7.65F, 3.925F));

		ModelPartData LeftSide = FrontWheel.addChild("LeftSide", ModelPartBuilder.create(), ModelTransform.pivot(0.5F, 0.0F, 0.0F));

		ModelPartData Shaft_r3 = LeftSide.addChild("Shaft_r3", ModelPartBuilder.create().uv(38, 33).cuboid(0.0F, -0.505F, -4.4901F, 0.0F, 1.0F, 6.0F, new Dilation(0.0F)), ModelTransform.of(-1.025F, -3.55F, -0.175F, -0.7854F, 0.0F, 0.0F));

		ModelPartData Shaft_r4 = LeftSide.addChild("Shaft_r4", ModelPartBuilder.create().uv(0, 24).cuboid(-0.001F, -0.505F, -4.4901F, 0.0F, 1.0F, 5.0F, new Dilation(0.0F)), ModelTransform.of(-1.05F, -2.475F, -4.6F, -1.9635F, 0.0F, 0.0F));

		ModelPartData Handle = FrontWheel.addChild("Handle", ModelPartBuilder.create(), ModelTransform.of(0.0F, -10.9F, -3.225F, 0.0349F, 0.0F, 0.0F));

		ModelPartData Shaft_r5 = Handle.addChild("Shaft_r5", ModelPartBuilder.create().uv(14, 4).cuboid(-1.0F, -2.5F, 0.5F, 1.0F, 5.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.5F, 3.25F, -0.7F, -0.2182F, 0.0F, 0.0F));

		ModelPartData Shaft_r6 = Handle.addChild("Shaft_r6", ModelPartBuilder.create().uv(6, 19).cuboid(1.0F, -0.5F, 1.5F, 1.0F, 2.0F, 1.0F, new Dilation(0.0F))
				.uv(0, 19).cuboid(0.0F, -0.5F, 0.5F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.475F, -1.35F, -0.675F, -0.0046F, 0.2269F, 1.571F));

		ModelPartData HandleBars_r1 = Handle.addChild("HandleBars_r1", ModelPartBuilder.create().uv(22, 37).cuboid(0.0F, -3.5F, 0.5F, 1.0F, 8.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(0.475F, -1.375F, -0.65F, -0.0046F, 0.2269F, 1.571F));

		ModelPartData wheelf = FrontWheel.addChild("wheelf", ModelPartBuilder.create().uv(23, 10).cuboid(0.0F, -2.5F, -2.475F, 1.0F, 5.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(-0.5F, -2.5F, -4.5F));

		ModelPartData BackWheel = Bike.addChild("BackWheel", ModelPartBuilder.create(), ModelTransform.pivot(0.025F, 7.65F, 4.925F));

		ModelPartData LeftSide2 = BackWheel.addChild("LeftSide2", ModelPartBuilder.create().uv(34, 26).cuboid(-1.025F, -2.98F, 0.9849F, 0.0F, 1.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(0.5F, 0.0F, 0.0F));

		ModelPartData Shaft_r7 = LeftSide2.addChild("Shaft_r7", ModelPartBuilder.create().uv(36, 7).cuboid(0.001F, -0.505F, -10.4901F, 0.0F, 1.0F, 11.0F, new Dilation(0.0F)), ModelTransform.of(-1.05F, -2.475F, 5.475F, -0.5149F, 0.0F, 0.0F));

		ModelPartData RightSide2 = BackWheel.addChild("RightSide2", ModelPartBuilder.create().uv(40, 19).cuboid(1.025F, -2.98F, 0.9849F, 0.0F, 1.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(-0.475F, 0.0F, 0.0F));

		ModelPartData Shaft_r8 = RightSide2.addChild("Shaft_r8", ModelPartBuilder.create().uv(36, 7).cuboid(0.001F, -0.505F, -10.4901F, 0.0F, 1.0F, 11.0F, new Dilation(0.0F)), ModelTransform.of(1.025F, -2.475F, 5.475F, -0.5149F, 0.0F, 0.0F));

		ModelPartData wheelb = BackWheel.addChild("wheelb", ModelPartBuilder.create().uv(23, 10).cuboid(-0.25F, -2.5F, -2.5F, 1.0F, 5.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(-0.25F, -2.5F, 5.5F));
		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(BicycleEntity entity, float limbSwing, float limbSwingAmount,
						  float ageInTicks, float netHeadYaw, float headPitch) {
		// reset pose
		this.getPart().traverse().forEach(ModelPart::resetTransform);
		// anima ruote usando l’AnimationState sull’entity
		this.updateAnimation(entity.goingAnimation, BicycleAnimations.ongoing, ageInTicks, 1.0F);
	}
	@Override
	public ModelPart getPart() {
		return Bike;
	}
	@Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
		Bike.render(matrices, vertexConsumer, light, overlay, color);
	}
}
