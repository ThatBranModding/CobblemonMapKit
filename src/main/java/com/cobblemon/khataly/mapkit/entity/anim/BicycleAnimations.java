package com.cobblemon.khataly.mapkit.entity.anim;

import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;

/**
 * Made with Blockbench 5.0.3
 * Exported for Minecraft version 1.19 or later with Yarn mappings
 * @author Author
 */
public class BicycleAnimations {
	public static final Animation ongoing = Animation.Builder.create(1.5F)
			.addBoneAnimation("RightPedal", new Transformation(Transformation.Targets.ROTATE,
					new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(1.5F, AnimationHelper.createRotationalVector(1080.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("LeftPedal", new Transformation(Transformation.Targets.ROTATE,
					new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(1.5F, AnimationHelper.createRotationalVector(1080.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("wheelf", new Transformation(Transformation.Targets.ROTATE,
					new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(1.5F, AnimationHelper.createRotationalVector(1080.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("wheelb", new Transformation(Transformation.Targets.ROTATE,
					new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
					new Keyframe(1.5F, AnimationHelper.createRotationalVector(1080.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
			))
			.looping()
			.build();

}
