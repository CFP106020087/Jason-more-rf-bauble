package com.moremod.client.animation;

import net.minecraft.client.model.ModelRenderer;

/**
 * 关键帧动画系统 - 将 1.19+ 的动画转换为 1.12.2 可用
 * 基于你的 Blockbench 动画数据
 */
public class KeyframeAnimationSystem {

    /**
     * 关键帧数据类
     */
    public static class Keyframe {
        public final float time;
        public final float x, y, z;

        public Keyframe(float time, float x, float y, float z) {
            this.time = time;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /**
     * 动画通道类
     */
    public static class AnimationChannel {
        public enum Target {
            ROTATION,
            POSITION,
            SCALE
        }

        public final Target target;
        public final Keyframe[] keyframes;

        public AnimationChannel(Target target, Keyframe... keyframes) {
            this.target = target;
            this.keyframes = keyframes;
        }
    }

    /**
     * 你的 "around" 动画定义
     * 基于 Blockbench 导出的数据
     */
    public static class MechanicalCoreAnimations {

        // bone 的旋转动画（Y轴旋转，Z轴摆动）
        public static final AnimationChannel BONE_ROTATION = new AnimationChannel(
                AnimationChannel.Target.ROTATION,
                new Keyframe(0.0F, 0.0F, 0.0F, 0.0F),
                new Keyframe(0.5F, 0.0F, -45.0F, 12.5F),
                new Keyframe(1.0F, 0.0F, -90.0F, 0.0F),
                new Keyframe(1.5F, 0.0F, -90.0F, -12.5F),
                new Keyframe(2.0F, 0.0F, -357.5F, 0.0F)
        );

        // bone 的位置动画（上下浮动）
        public static final AnimationChannel BONE_POSITION = new AnimationChannel(
                AnimationChannel.Target.POSITION,
                new Keyframe(0.0F, 0.0F, 0.0F, 0.0F),
                new Keyframe(1.0F, 0.0F, 1.0F, 0.0F),
                new Keyframe(2.0F, 0.0F, 0.0F, 0.0F)
        );

        // bone3 的旋转动画（Z轴摆动）
        public static final AnimationChannel BONE3_ROTATION = new AnimationChannel(
                AnimationChannel.Target.ROTATION,
                new Keyframe(0.0F, 0.0F, 0.0F, 0.0F),
                new Keyframe(0.25F, 0.0F, 0.0F, 5.0F),
                new Keyframe(0.5F, 0.0F, 0.0F, 10.0F),
                new Keyframe(0.75F, 0.0F, 0.0F, 10.0F),
                new Keyframe(1.0F, 0.0F, 0.0F, 5.0F),
                new Keyframe(1.25F, 0.0F, 0.0F, 0.0F),
                new Keyframe(1.5F, 0.0F, 0.0F, -5.0F),
                new Keyframe(1.75F, 0.0F, 0.0F, -10.0F),
                new Keyframe(2.0F, 0.0F, 0.0F, 0.0F)
        );

        // bone3 的位置动画
        public static final AnimationChannel BONE3_POSITION = new AnimationChannel(
                AnimationChannel.Target.POSITION,
                new Keyframe(0.0F, 0.0F, 0.0F, 0.0F),
                new Keyframe(0.5F, -0.25F, 0.25F, 0.0F),
                new Keyframe(1.0F, -0.25F, 0.75F, 0.0F),
                new Keyframe(1.5F, -0.25F, 0.25F, 0.0F),
                new Keyframe(1.75F, 0.0F, 0.25F, 0.0F),
                new Keyframe(2.0F, 0.0F, 0.0F, 0.0F)
        );

        // 动画总长度（秒）
        public static final float ANIMATION_LENGTH = 2.0F;
    }

    /**
     * 应用动画到模型
     * @param bone 模型骨骼
     * @param channel 动画通道
     * @param animationTime 当前动画时间（秒）
     * @param partialTicks 部分刻
     */
    public static void applyAnimation(ModelRenderer bone, AnimationChannel channel, float animationTime, float partialTicks) {
        if (bone == null || channel == null || channel.keyframes.length < 2) {
            return;
        }

        // 循环动画
        float loopTime = animationTime % MechanicalCoreAnimations.ANIMATION_LENGTH;

        // 找到当前关键帧区间
        Keyframe prevFrame = channel.keyframes[0];
        Keyframe nextFrame = channel.keyframes[1];
        int frameIndex = 0;

        for (int i = 0; i < channel.keyframes.length - 1; i++) {
            if (loopTime >= channel.keyframes[i].time && loopTime <= channel.keyframes[i + 1].time) {
                prevFrame = channel.keyframes[i];
                nextFrame = channel.keyframes[i + 1];
                frameIndex = i;
                break;
            }
        }

        // 计算插值因子
        float frameDuration = nextFrame.time - prevFrame.time;
        float frameProgress = (loopTime - prevFrame.time) / frameDuration;

        // Catmull-Rom 插值（简化为平滑插值）
        frameProgress = smoothstep(frameProgress);

        // 插值计算
        float x = lerp(prevFrame.x, nextFrame.x, frameProgress);
        float y = lerp(prevFrame.y, nextFrame.y, frameProgress);
        float z = lerp(prevFrame.z, nextFrame.z, frameProgress);

        // 应用到模型
        switch (channel.target) {
            case ROTATION:
                bone.rotateAngleX = (float) Math.toRadians(x);
                bone.rotateAngleY = (float) Math.toRadians(y);
                bone.rotateAngleZ = (float) Math.toRadians(z);
                break;
            case POSITION:
                bone.offsetX = x * 0.0625F; // 转换为 Minecraft 单位
                bone.offsetY = y * 0.0625F;
                bone.offsetZ = z * 0.0625F;
                break;
            case SCALE:
                // 1.12.2 不直接支持骨骼缩放
                break;
        }
    }

    /**
     * 线性插值
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * 平滑插值（模拟 Catmull-Rom）
     */
    private static float smoothstep(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    /**
     * 应用完整的机械核心动画
     */
    public static void applyMechanicalCoreAnimation(ModelRenderer bone, ModelRenderer bone2, ModelRenderer bone3,
                                                    float animationTime, float partialTicks) {
        // 应用 bone 的动画
        if (bone != null) {
            applyAnimation(bone, MechanicalCoreAnimations.BONE_ROTATION, animationTime, partialTicks);
            applyAnimation(bone, MechanicalCoreAnimations.BONE_POSITION, animationTime, partialTicks);
        }

        // 应用 bone3 的动画
        if (bone3 != null) {
            applyAnimation(bone3, MechanicalCoreAnimations.BONE3_ROTATION, animationTime, partialTicks);
            applyAnimation(bone3, MechanicalCoreAnimations.BONE3_POSITION, animationTime, partialTicks);
        }

        // bone2 没有定义动画，可以添加自定义效果
        if (bone2 != null) {
            // 简单的反向旋转
            bone2.rotateAngleY = -bone.rotateAngleY * 0.5F;
        }
    }
}