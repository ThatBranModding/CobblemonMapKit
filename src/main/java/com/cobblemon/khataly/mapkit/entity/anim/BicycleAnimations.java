package com.cobblemon.khataly.mapkit.entity.anim;

import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;

public final class BicycleAnimations {
	public static final Animation GOING = Animation.Builder.create(1.5F)
			.addBoneAnimation("wheelr", new Transformation(
					Transformation.Targets.ROTATE,
					new Keyframe(0.0F,  AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F),   Transformation.Interpolations.LINEAR),
					new Keyframe(1.5F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 732.5F), Transformation.Interpolations.LINEAR)
			))
			.addBoneAnimation("wheelf", new Transformation(
					Transformation.Targets.ROTATE,
					new Keyframe(0.0F,  AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F),  Transformation.Interpolations.LINEAR),
					new Keyframe(1.5F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 705.0F), Transformation.Interpolations.CUBIC)
			))
			.looping()
			.build();

	private BicycleAnimations() {}
}
