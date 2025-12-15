package com.moremod.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * 3D Printer Model - Converted from Blockbench 5.0.5
 * Original export: MC 1.17+/1.19+ with Mojang mappings
 * Converted to: MC 1.12.2 Forge
 */
public class Model3DPrinter extends ModelBase {

    // 动画常量
    public static final float ANIMATION_LENGTH = 14.1667F; // 动画总长度（秒）
    private static final float DEG_TO_RAD = (float) Math.PI / 180F;

    // 打印状态（由渲染器设置）
    private boolean isPrinting = false;
    private boolean hasRecipe = false;
    private float printProgress = 0; // 0.0 - 1.0

    // 主要部件
    public ModelRenderer all;
    public ModelRenderer base;

    // 草方块打印层
    public ModelRenderer grass;
    public ModelRenderer grass1_2;
    public ModelRenderer grass1_3;
    public ModelRenderer grass1_4;
    public ModelRenderer grass1_5;

    // 石头打印层
    public ModelRenderer stone;
    public ModelRenderer stone1_2;
    public ModelRenderer stone1_3;
    public ModelRenderer stone1_4;
    public ModelRenderer stone1_5;

    // 机械臂
    public ModelRenderer roboticArm;
    public ModelRenderer roboticArm1_1;
    public ModelRenderer bone;
    public ModelRenderer roboticArm1_2;
    public ModelRenderer roboticArm1_3;
    public ModelRenderer roboticArm1_3_r1;
    public ModelRenderer roboticArm1_3_r2;
    public ModelRenderer roboticArm1_3_r3;
    public ModelRenderer roboticArm1_4;
    public ModelRenderer roboticArm1_4_r1;
    public ModelRenderer roboticArm1_4_r2;

    public Model3DPrinter() {
        this.textureWidth = 64;
        this.textureHeight = 64;

        // === all (根节点) ===
        // 原始: offset(0.0F, 24.0F, 0.0F) - 在1.12.2中Y轴需要调整
        this.all = new ModelRenderer(this, 0, 0);
        this.all.setRotationPoint(0.0F, 24.0F, 0.0F);
        // 底板: texOffs(0, 0), addBox(-8.0F, -2.0F, -8.0F, 16, 2, 16)
        this.all.addBox(-8.0F, -2.0F, -8.0F, 16, 2, 16, 0.0F);

        // 小平台: texOffs(0, 29), addBox(-1.0F, -4.0F, -3.0F, 7, 2, 7)
        ModelRenderer allPlatform = new ModelRenderer(this, 0, 29);
        allPlatform.setRotationPoint(0.0F, 0.0F, 0.0F);
        allPlatform.addBox(-1.0F, -4.0F, -3.0F, 7, 2, 7, 0.0F);
        this.all.addChild(allPlatform);

        // === base (打印底座，可旋转) ===
        this.base = new ModelRenderer(this, 0, 18);
        this.base.setRotationPoint(3.0F, -4.5F, 0.0F);
        this.base.addBox(-5.0F, -0.5F, -5.0F, 10, 1, 10, 0.02F);
        this.all.addChild(this.base);

        // === grass 组 (草方块打印层) ===
        this.grass = new ModelRenderer(this, 0, 0);
        this.grass.setRotationPoint(0.0F, -2.0F, 0.0F);
        this.base.addChild(this.grass);

        this.grass1_2 = new ModelRenderer(this, 0, 48);
        this.grass1_2.setRotationPoint(0.0F, 1.0F, 0.0F);
        this.grass1_2.addBox(-2.0F, -0.5F, -2.0F, 4, 1, 4, 0.0F);
        this.grass.addChild(this.grass1_2);

        this.grass1_3 = new ModelRenderer(this, 0, 48);
        this.grass1_3.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.grass1_3.addBox(-2.0F, -0.5F, -2.0F, 4, 1, 4, 0.0F);
        this.grass.addChild(this.grass1_3);

        this.grass1_4 = new ModelRenderer(this, 0, 48);
        this.grass1_4.setRotationPoint(0.0F, -1.0F, 0.0F);
        this.grass1_4.addBox(-2.0F, -0.5F, -2.0F, 4, 1, 4, 0.0F);
        this.grass.addChild(this.grass1_4);

        // grass1_5 - 顶部装饰（负尺寸在1.12.2中需要特殊处理）
        this.grass1_5 = new ModelRenderer(this, 4, 0);
        this.grass1_5.setRotationPoint(-3.0F, 6.5F, 0.0F);
        // 原始: addBox(5.0F, -5.0F, 2.0F, -4, -3, -4) 负尺寸需要转换
        // 转换为: addBox(1.0F, -8.0F, -2.0F, 4, 3, 4) 并缩小
        this.grass1_5.addBox(1.0F, -8.0F, -2.0F, 4, 3, 4, -0.3F);
        this.grass.addChild(this.grass1_5);

        // === stone 组 (石头打印层) ===
        this.stone = new ModelRenderer(this, 0, 0);
        this.stone.setRotationPoint(0.0F, -0.5F, 0.0F);
        this.base.addChild(this.stone);

        this.stone1_2 = new ModelRenderer(this, 0, 57);
        this.stone1_2.setRotationPoint(-3.0F, 5.0F, 0.0F);
        this.stone1_2.addBox(1.0F, -6.0F, -2.0F, 4, 1, 4, 0.0F);
        this.stone.addChild(this.stone1_2);

        this.stone1_3 = new ModelRenderer(this, 0, 57);
        this.stone1_3.setRotationPoint(-3.0F, 5.0F, 0.0F);
        this.stone1_3.addBox(1.0F, -7.0F, -2.0F, 4, 1, 4, 0.0F);
        this.stone.addChild(this.stone1_3);

        this.stone1_4 = new ModelRenderer(this, 0, 57);
        this.stone1_4.setRotationPoint(-3.0F, 5.0F, 0.0F);
        this.stone1_4.addBox(1.0F, -8.0F, -2.0F, 4, 1, 4, 0.0F);
        this.stone.addChild(this.stone1_4);

        this.stone1_5 = new ModelRenderer(this, 4, 0);
        this.stone1_5.setRotationPoint(-3.0F, 5.0F, 0.0F);
        this.stone1_5.addBox(1.0F, -8.0F, -2.0F, 4, 3, 4, -0.3F);
        this.stone.addChild(this.stone1_5);

        // === roboticArm 组 (机械臂) ===
        this.roboticArm = new ModelRenderer(this);
        this.roboticArm.setRotationPoint(0.0F, -1.0F, -5.75F);
        this.all.addChild(this.roboticArm);

        this.roboticArm1_1 = new ModelRenderer(this, 28, 29);
        this.roboticArm1_1.setRotationPoint(-3.0F, -1.0F, 5.0F);
        this.roboticArm1_1.addBox(-5.0F, -1.0F, -1.75F, 5, 1, 5, 0.0F);
        this.roboticArm.addChild(this.roboticArm1_1);

        this.bone = new ModelRenderer(this, 0, 38);
        this.bone.setRotationPoint(-2.5F, -1.0F, 0.75F);
        this.bone.addBox(-2.0F, -2.0F, -2.0F, 4, 2, 4, 0.0F);
        this.roboticArm1_1.addChild(this.bone);

        this.roboticArm1_2 = new ModelRenderer(this, 16, 38);
        this.roboticArm1_2.setRotationPoint(0.0F, -1.25F, 0.0F);
        setRotation(this.roboticArm1_2, 0.0F, 0.0F, -0.3054F);
        this.roboticArm1_2.addBox(-1.5F, -6.0F, -1.5F, 3, 6, 3, 0.0F);
        this.bone.addChild(this.roboticArm1_2);

        this.roboticArm1_3 = new ModelRenderer(this, 28, 35);
        this.roboticArm1_3.setRotationPoint(0.0F, -7.5F, 0.0F);
        this.roboticArm1_3.addBox(-1.5F, -2.75F, -1.0F, 7, 3, 2, 0.0F);
        this.roboticArm1_2.addChild(this.roboticArm1_3);

        // roboticArm1_3 的额外盒子
        ModelRenderer arm3Rail = new ModelRenderer(this, 40, 25);
        arm3Rail.setRotationPoint(0.0F, 0.0F, 0.0F);
        arm3Rail.addBox(-1.0F, -4.25F, -0.5F, 8, 1, 1, 0.0F);
        this.roboticArm1_3.addChild(arm3Rail);

        // 滑块装饰
        ModelRenderer slider1 = new ModelRenderer(this, 40, 27);
        slider1.setRotationPoint(0.0F, 0.0F, 0.0F);
        slider1.addBox(0.0F, -4.25F, -0.5F, 1, 1, 1, 0.2F);
        this.roboticArm1_3.addChild(slider1);

        ModelRenderer slider2 = new ModelRenderer(this, 12, 44);
        slider2.setRotationPoint(0.0F, 0.0F, 0.0F);
        slider2.addBox(1.75F, -4.25F, -0.5F, 1, 1, 1, 0.2F);
        this.roboticArm1_3.addChild(slider2);

        ModelRenderer slider3 = new ModelRenderer(this, 44, 27);
        slider3.setRotationPoint(0.0F, 0.0F, 0.0F);
        slider3.addBox(3.5F, -4.25F, -0.5F, 1, 1, 1, 0.2F);
        this.roboticArm1_3.addChild(slider3);

        // 旋转部件 r1
        this.roboticArm1_3_r1 = new ModelRenderer(this, 4, 44);
        this.roboticArm1_3_r1.setRotationPoint(-0.7735F, -2.4228F, 0.0F);
        setRotation(this.roboticArm1_3_r1, 0.0F, 0.0F, 0.3054F);
        this.roboticArm1_3_r1.addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, -0.02F);
        this.roboticArm1_3.addChild(this.roboticArm1_3_r1);

        // 旋转部件 r2
        this.roboticArm1_3_r2 = new ModelRenderer(this, 8, 44);
        this.roboticArm1_3_r2.setRotationPoint(6.5F, -2.5F, 0.0F);
        setRotation(this.roboticArm1_3_r2, 0.0F, 0.0F, -0.2618F);
        this.roboticArm1_3_r2.addBox(-0.5F, -1.0F, -0.5F, 1, 2, 1, -0.02F);
        this.roboticArm1_3.addChild(this.roboticArm1_3_r2);

        // 旋转部件 r3 (关节)
        this.roboticArm1_3_r3 = new ModelRenderer(this, 40, 18);
        this.roboticArm1_3_r3.setRotationPoint(0.0F, 0.75F, 0.5F);
        setRotation(this.roboticArm1_3_r3, 0.0F, 0.0F, 0.7854F);
        this.roboticArm1_3_r3.addBox(-1.5F, -1.5F, -2.5F, 3, 3, 4, 0.0F);
        this.roboticArm1_3.addChild(this.roboticArm1_3_r3);

        // === roboticArm1_4 (末端执行器) ===
        this.roboticArm1_4 = new ModelRenderer(this);
        this.roboticArm1_4.setRotationPoint(6.25F, -1.25F, 0.0F);
        this.roboticArm1_3.addChild(this.roboticArm1_4);

        // r1 - 喷头
        this.roboticArm1_4_r1 = new ModelRenderer(this, 40, 40);
        this.roboticArm1_4_r1.setRotationPoint(1.25F, 2.5F, 0.0F);
        setRotation(this.roboticArm1_4_r1, 0.0F, 0.0F, -0.2618F);
        this.roboticArm1_4_r1.addBox(-1.5F, -1.5F, -1.0F, 2, 4, 2, 0.0F);
        this.roboticArm1_4.addChild(this.roboticArm1_4_r1);

        // 喷嘴
        ModelRenderer nozzle = new ModelRenderer(this, 0, 44);
        nozzle.setRotationPoint(1.25F, 2.5F, 0.0F);
        setRotation(nozzle, 0.0F, 0.0F, -0.2618F);
        nozzle.addBox(-1.0F, 2.5F, -0.5F, 1, 2, 1, 0.0F);
        this.roboticArm1_4.addChild(nozzle);

        // r2 - 关节球
        this.roboticArm1_4_r2 = new ModelRenderer(this, 28, 40);
        this.roboticArm1_4_r2.setRotationPoint(0.0F, 0.0F, 0.0F);
        setRotation(this.roboticArm1_4_r2, 0.0F, 0.0F, 0.3491F);
        this.roboticArm1_4_r2.addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3, 0.0F);
        this.roboticArm1_4.addChild(this.roboticArm1_4_r2);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.all.render(scale);
    }

    /**
     * 设置打印状态（由渲染器调用）
     * @param printing 是否正在打印
     * @param hasRecipe 是否有有效配方
     * @param progress 打印进度 0.0-1.0
     */
    public void setPrintingState(boolean printing, boolean hasRecipe, float progress) {
        this.isPrinting = printing;
        this.hasRecipe = hasRecipe;
        this.printProgress = progress;
    }

    /**
     * 设置动画
     * @param ageInTicks 实体存活的tick数，用于计算动画进度
     */
    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);

        // 将tick转换为秒（20 ticks = 1秒）
        float timeInSeconds = (ageInTicks / 20.0F) % ANIMATION_LENGTH;

        // 应用动画
        animateIdle(timeInSeconds);
    }

    /**
     * 播放idle动画
     */
    private void animateIdle(float time) {
        // === base 旋转动画 ===
        if (isPrinting) {
            // 打印时底座旋转
            this.base.rotateAngleY = -printProgress * 2 * (float) Math.PI;
        } else {
            this.base.rotateAngleY = 0;
        }

        // === bone (机械臂底座) Y轴旋转 ===
        this.bone.rotateAngleY = interpolateBoneRotationY(time);

        // === roboticArm1_2 旋转 ===
        float[] arm2Rot = interpolateArm2Rotation(time);
        this.roboticArm1_2.rotateAngleX = arm2Rot[0];
        this.roboticArm1_2.rotateAngleY = arm2Rot[1];
        this.roboticArm1_2.rotateAngleZ = -0.3054F + arm2Rot[2];

        // === roboticArm1_3 Z轴旋转 ===
        this.roboticArm1_3.rotateAngleZ = interpolateArm3RotationZ(time);

        // === roboticArm1_4 Z轴旋转 ===
        this.roboticArm1_4.rotateAngleZ = interpolateArm4RotationZ(time);

        // === 打印层：始终显示（测试用）===
        this.grass.isHidden = false;
        this.stone.isHidden = false;
        this.grass1_2.isHidden = false;
        this.grass1_3.isHidden = false;
        this.grass1_4.isHidden = false;
        this.grass1_5.isHidden = false;
        this.stone1_2.isHidden = false;
        this.stone1_3.isHidden = false;
        this.stone1_4.isHidden = false;
        this.stone1_5.isHidden = false;
    }

    // ==================== 动画插值方法 ====================

    private float interpolateBaseRotation(float time) {
        // base旋转关键帧 (度数 -> 弧度)
        if (time < 0.3333F) return 0;
        if (time < 2.4583F) return lerp(0, -82.5F, (time - 0.3333F) / (2.4583F - 0.3333F)) * DEG_TO_RAD;
        if (time < 3.9167F) return -82.5F * DEG_TO_RAD;
        if (time < 5.625F) return lerp(-82.5F, -180F, (time - 3.9167F) / (5.625F - 3.9167F)) * DEG_TO_RAD;
        if (time < 6.9167F) return -180F * DEG_TO_RAD;
        if (time < 8.7083F) return lerp(-180F, -270F, (time - 6.9167F) / (8.7083F - 6.9167F)) * DEG_TO_RAD;
        if (time < 10.5F) return -270F * DEG_TO_RAD;
        if (time < 12.125F) return lerp(-270F, -360F, (time - 10.5F) / (12.125F - 10.5F)) * DEG_TO_RAD;
        return 0; // -360° = 0°
    }

    private float interpolateBoneRotationY(float time) {
        // 简化的bone Y轴旋转 - 摇摆动画
        // 分为4个周期，每个周期有类似的摇摆模式
        float cycleTime;

        if (time < 3.4167F) {
            cycleTime = time;
        } else if (time < 6.9167F) {
            cycleTime = time - 3.4167F;
        } else if (time < 10.0F) {
            cycleTime = time - 6.9167F;
        } else {
            cycleTime = time - 10.0F;
        }

        // 摇摆模式
        if (cycleTime < 0.3333F) return 0;
        if (cycleTime < 0.5833F) return lerp(0, 10F, (cycleTime - 0.3333F) / 0.25F) * DEG_TO_RAD;
        if (cycleTime < 0.75F) return lerp(10F, -10F, (cycleTime - 0.5833F) / 0.1667F) * DEG_TO_RAD;
        if (cycleTime < 0.9167F) return lerp(-10F, 12.5F, (cycleTime - 0.75F) / 0.1667F) * DEG_TO_RAD;
        if (cycleTime < 1.1667F) return lerp(12.5F, -7.5F, (cycleTime - 0.9167F) / 0.25F) * DEG_TO_RAD;
        if (cycleTime < 1.375F) return lerp(-7.5F, 10F, (cycleTime - 1.1667F) / 0.2083F) * DEG_TO_RAD;
        if (cycleTime < 1.7083F) return lerp(10F, -12.5F, (cycleTime - 1.375F) / 0.3333F) * DEG_TO_RAD;
        if (cycleTime < 2.125F) return lerp(-12.5F, 10F, (cycleTime - 1.7083F) / 0.4167F) * DEG_TO_RAD;
        if (cycleTime < 2.4583F) return lerp(10F, 0F, (cycleTime - 2.125F) / 0.3333F) * DEG_TO_RAD;
        return 0;
    }

    private float[] interpolateArm2Rotation(float time) {
        // roboticArm1_2 旋转 - 返回 [x, y, z] 增量
        float cycleTime;

        if (time < 3.4167F) {
            cycleTime = time;
        } else if (time < 6.375F) {
            cycleTime = time - 3.4167F + 0.3333F;
        } else if (time < 10.0F) {
            cycleTime = time - 6.375F + 0.3333F;
        } else {
            cycleTime = time - 10.0F + 0.3333F;
        }

        if (cycleTime < 0.3333F) return new float[]{0, 0, 0};
        if (cycleTime < 0.5833F) return new float[]{0, 0, lerp(0, 7.5F, (cycleTime - 0.3333F) / 0.25F) * DEG_TO_RAD};
        if (cycleTime < 0.9167F) return new float[]{0, 0, 7.5F * DEG_TO_RAD};
        if (cycleTime < 1.1667F) return new float[]{
                lerp(0, -0.4857F, (cycleTime - 0.9167F) / 0.25F) * DEG_TO_RAD,
                lerp(0, 0.3899F, (cycleTime - 0.9167F) / 0.25F) * DEG_TO_RAD,
                lerp(7.5F, 14.9725F, (cycleTime - 0.9167F) / 0.25F) * DEG_TO_RAD
        };
        if (cycleTime < 1.375F) return new float[]{-0.4857F * DEG_TO_RAD, 0.3899F * DEG_TO_RAD, 14.9725F * DEG_TO_RAD};
        if (cycleTime < 1.7083F) return new float[]{
                lerp(-0.4857F, 0.9918F, (cycleTime - 1.375F) / 0.3333F) * DEG_TO_RAD,
                lerp(0.3899F, -0.6192F, (cycleTime - 1.375F) / 0.3333F) * DEG_TO_RAD,
                lerp(14.9725F, 7.6858F, (cycleTime - 1.375F) / 0.3333F) * DEG_TO_RAD
        };
        if (cycleTime < 2.125F) return new float[]{0.99F * DEG_TO_RAD, -0.62F * DEG_TO_RAD, 7.69F * DEG_TO_RAD};
        if (cycleTime < 2.4583F) return new float[]{
                lerp(0.99F, 0, (cycleTime - 2.125F) / 0.3333F) * DEG_TO_RAD,
                lerp(-0.62F, 0, (cycleTime - 2.125F) / 0.3333F) * DEG_TO_RAD,
                lerp(7.69F, 0, (cycleTime - 2.125F) / 0.3333F) * DEG_TO_RAD
        };
        return new float[]{0, 0, 0};
    }

    private float interpolateArm3RotationZ(float time) {
        // roboticArm1_3 Z轴旋转
        float cycleTime;

        if (time < 3.4167F) {
            cycleTime = time;
        } else if (time < 6.375F) {
            cycleTime = time - 3.4167F + 0.3333F;
        } else if (time < 10.0F) {
            cycleTime = time - 6.375F + 0.3333F;
        } else {
            cycleTime = time - 10.0F + 0.3333F;
        }

        if (cycleTime < 0.3333F) return lerp(0, -25F, cycleTime / 0.3333F) * DEG_TO_RAD;
        if (cycleTime < 0.9167F) return lerp(-25F, 0, (cycleTime - 0.3333F) / 0.5834F) * DEG_TO_RAD;
        if (cycleTime < 1.1667F) return lerp(0, -12.5F, (cycleTime - 0.9167F) / 0.25F) * DEG_TO_RAD;
        if (cycleTime < 1.375F) return -12.5F * DEG_TO_RAD;
        if (cycleTime < 1.7083F) return lerp(-12.5F, -22.5F, (cycleTime - 1.375F) / 0.3333F) * DEG_TO_RAD;
        if (cycleTime < 2.125F) return lerp(-22.5F, -15F, (cycleTime - 1.7083F) / 0.4167F) * DEG_TO_RAD;
        if (cycleTime < 2.4583F) return lerp(-15F, 0, (cycleTime - 2.125F) / 0.3333F) * DEG_TO_RAD;
        return 0;
    }

    private float interpolateArm4RotationZ(float time) {
        // roboticArm1_4 Z轴旋转
        float cycleTime;

        if (time < 3.4167F) {
            cycleTime = time;
        } else if (time < 6.375F) {
            cycleTime = time - 3.4167F + 0.3333F;
        } else if (time < 10.0F) {
            cycleTime = time - 6.375F + 0.3333F;
        } else {
            cycleTime = time - 10.0F + 0.3333F;
        }

        if (cycleTime < 0.3333F) return lerp(0, 12.5F, cycleTime / 0.3333F) * DEG_TO_RAD;
        if (cycleTime < 0.9167F) return lerp(12.5F, 0, (cycleTime - 0.3333F) / 0.5834F) * DEG_TO_RAD;
        if (cycleTime < 1.1667F) return lerp(0, 10F, (cycleTime - 0.9167F) / 0.25F) * DEG_TO_RAD;
        if (cycleTime < 1.375F) return 10F * DEG_TO_RAD;
        if (cycleTime < 1.7083F) return lerp(10F, 20F, (cycleTime - 1.375F) / 0.3333F) * DEG_TO_RAD;
        if (cycleTime < 2.125F) return lerp(20F, 12.5F, (cycleTime - 1.7083F) / 0.4167F) * DEG_TO_RAD;
        if (cycleTime < 2.4583F) return lerp(12.5F, 0, (cycleTime - 2.125F) / 0.3333F) * DEG_TO_RAD;
        return 0;
    }

    // ==================== 工具方法 ====================

    private static float lerp(float start, float end, float progress) {
        progress = MathHelper.clamp(progress, 0, 1);
        return start + (end - start) * progress;
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.rotateAngleX = x;
        model.rotateAngleY = y;
        model.rotateAngleZ = z;
    }
}