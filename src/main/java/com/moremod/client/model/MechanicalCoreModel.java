package com.moremod.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 机械核心3D模型
 * Made with Blockbench 4.12.5
 * Exported for Minecraft version 1.7 - 1.12
 */
@SideOnly(Side.CLIENT)
public class MechanicalCoreModel extends ModelBase {
    private final ModelRenderer bone;
    private final ModelRenderer cube_r1;
    private final ModelRenderer cube_r2;
    private final ModelRenderer cube_r3;
    private final ModelRenderer cube_r4;
    private final ModelRenderer cube_r5;
    private final ModelRenderer cube_r6;
    private final ModelRenderer bone2;
    private final ModelRenderer cube_r7;
    private final ModelRenderer cube_r8;
    private final ModelRenderer cube_r9;
    private final ModelRenderer cube_r10;
    private final ModelRenderer cube_r11;
    private final ModelRenderer bone3;
    private final ModelRenderer cube_r12;
    private final ModelRenderer cube_r13;
    private final ModelRenderer cube_r14;
    private final ModelRenderer cube_r15;
    private final ModelRenderer cube_r16;

    // 动画变量
    private float animationTick = 0;
    private boolean isActive = false;
    private boolean useKeyframeAnimation = true; // 使用关键帧动画

    public MechanicalCoreModel() {
        textureWidth = 64;
        textureHeight = 64;

        bone = new ModelRenderer(this);
        bone.setRotationPoint(0.0F, 24.0F, 0.0F);
        bone.cubeList.add(new ModelBox(bone, 0, 18, -4.0F, -1.0F, -5.5F, 8, 1, 1, 0.0F, false));
        bone.cubeList.add(new ModelBox(bone, 18, 2, -4.0F, -0.5F, -6.5F, 8, 0, 1, 0.0F, false));
        bone.cubeList.add(new ModelBox(bone, 18, 4, -4.0F, -0.5F, 5.5F, 8, 0, 1, 0.0F, false));
        bone.cubeList.add(new ModelBox(bone, 18, 0, -4.0F, -1.0F, 4.5F, 8, 1, 1, 0.0F, false));
        bone.cubeList.add(new ModelBox(bone, 0, 9, -5.5F, -1.0F, -4.0F, 1, 1, 8, 0.0F, false));
        bone.cubeList.add(new ModelBox(bone, 0, 0, 4.5F, -1.0F, -4.0F, 1, 1, 8, 0.0F, false));

        cube_r1 = new ModelRenderer(this);
        cube_r1.setRotationPoint(4.0F, 0.0F, 4.0F);
        bone.addChild(cube_r1);
        setRotationAngle(cube_r1, 0.0F, -0.7854F, 0.0F);
        cube_r1.cubeList.add(new ModelBox(cube_r1, 6, 20, 0.0F, -1.0F, -1.0F, 1, 1, 2, 0.0F, false));

        cube_r2 = new ModelRenderer(this);
        cube_r2.setRotationPoint(-4.0F, 0.0F, 4.0F);
        bone.addChild(cube_r2);
        setRotationAngle(cube_r2, 0.0F, -0.7854F, 0.0F);
        cube_r2.cubeList.add(new ModelBox(cube_r2, 14, 24, -1.0F, -1.0F, 0.0F, 2, 1, 1, 0.0F, false));

        cube_r3 = new ModelRenderer(this);
        cube_r3.setRotationPoint(-4.0F, 0.0F, -4.0F);
        bone.addChild(cube_r3);
        setRotationAngle(cube_r3, 0.0F, -0.7854F, 0.0F);
        cube_r3.cubeList.add(new ModelBox(cube_r3, 0, 20, -1.0F, -1.0F, -1.0F, 1, 1, 2, 0.0F, false));

        cube_r4 = new ModelRenderer(this);
        cube_r4.setRotationPoint(4.0F, 0.0F, -4.0F);
        bone.addChild(cube_r4);
        setRotationAngle(cube_r4, 0.0F, -0.7854F, 0.0F);
        cube_r4.cubeList.add(new ModelBox(cube_r4, 8, 24, -1.0F, -1.0F, -1.0F, 2, 1, 1, 0.0F, false));

        cube_r5 = new ModelRenderer(this);
        cube_r5.setRotationPoint(-15.5F, -0.5F, 0.0F);
        bone.addChild(cube_r5);
        setRotationAngle(cube_r5, 0.0F, -1.5708F, 0.0F);
        cube_r5.cubeList.add(new ModelBox(cube_r5, 18, 5, -4.0F, 0.0F, -10.0F, 8, 0, 1, 0.0F, false));

        cube_r6 = new ModelRenderer(this);
        cube_r6.setRotationPoint(-3.5F, -0.5F, 0.0F);
        bone.addChild(cube_r6);
        setRotationAngle(cube_r6, 0.0F, -1.5708F, 0.0F);
        cube_r6.cubeList.add(new ModelBox(cube_r6, 18, 3, -4.0F, 0.0F, -10.0F, 8, 0, 1, 0.0F, false));

        bone2 = new ModelRenderer(this);
        bone2.setRotationPoint(0.0F, 0.0F, 0.0F);
        bone.addChild(bone2);
        bone2.cubeList.add(new ModelBox(bone2, 0, 26, -5.0F, -0.5F, -0.5F, 2, 0, 1, 0.0F, false));
        bone2.cubeList.add(new ModelBox(bone2, 20, 25, 3.0F, -0.5F, -0.5F, 2, 0, 1, 0.0F, false));

        cube_r7 = new ModelRenderer(this);
        cube_r7.setRotationPoint(-9.5F, -0.5F, 0.0F);
        bone2.addChild(cube_r7);
        setRotationAngle(cube_r7, 0.0F, -1.5708F, 0.0F);
        cube_r7.cubeList.add(new ModelBox(cube_r7, 20, 24, 2.5F, 0.0F, -10.0F, 2, 0, 1, 0.0F, false));
        cube_r7.cubeList.add(new ModelBox(cube_r7, 0, 25, -5.0F, 0.0F, -10.0F, 2, 0, 1, 0.0F, false));

        cube_r8 = new ModelRenderer(this);
        cube_r8.setRotationPoint(12.25F, -0.5F, 1.25F);
        bone2.addChild(cube_r8);
        setRotationAngle(cube_r8, 0.0F, 0.7854F, 0.0F);
        cube_r8.cubeList.add(new ModelBox(cube_r8, 8, 23, -5.0F, 0.0F, -10.0F, 3, 0, 1, 0.0F, false));

        cube_r9 = new ModelRenderer(this);
        cube_r9.setRotationPoint(-1.0F, -0.5F, 12.5F);
        bone2.addChild(cube_r9);
        setRotationAngle(cube_r9, 0.0F, -0.7854F, 0.0F);
        cube_r9.cubeList.add(new ModelBox(cube_r9, 16, 23, -5.0F, 0.0F, -10.0F, 3, 0, 1, 0.0F, false));

        cube_r10 = new ModelRenderer(this);
        cube_r10.setRotationPoint(-7.5F, -0.5F, 6.0F);
        bone2.addChild(cube_r10);
        setRotationAngle(cube_r10, 0.0F, -0.7854F, 0.0F);
        cube_r10.cubeList.add(new ModelBox(cube_r10, 0, 24, -5.0F, 0.0F, -10.0F, 3, 0, 1, 0.0F, false));

        cube_r11 = new ModelRenderer(this);
        cube_r11.setRotationPoint(6.0F, -0.5F, 7.5F);
        bone2.addChild(cube_r11);
        setRotationAngle(cube_r11, 0.0F, 0.7854F, 0.0F);
        cube_r11.cubeList.add(new ModelBox(cube_r11, 0, 23, -5.0F, 0.0F, -10.0F, 3, 0, 1, 0.0F, false));

        bone3 = new ModelRenderer(this);
        bone3.setRotationPoint(0.0F, 24.0F, 5.5F);
        bone3.cubeList.add(new ModelBox(bone3, 18, 18, -2.0F, -1.0F, -8.5F, 4, 1, 1, 0.0F, false));
        bone3.cubeList.add(new ModelBox(bone3, 18, 11, 2.0F, -1.0F, -7.5F, 1, 1, 4, 0.0F, false));
        bone3.cubeList.add(new ModelBox(bone3, 18, 6, -3.0F, -1.0F, -7.5F, 1, 1, 4, 0.0F, false));
        bone3.cubeList.add(new ModelBox(bone3, 18, 16, -2.0F, -1.0F, -3.5F, 4, 1, 1, 0.0F, false));
        bone3.cubeList.add(new ModelBox(bone3, 0, 0, -3.0F, -0.5F, -5.5F, 8, 0, 0, 0.0F, false));
        bone3.cubeList.add(new ModelBox(bone3, 0, 0, -3.0F, -0.5F, -5.5F, 8, 0, 0, 0.0F, false));
        bone3.cubeList.add(new ModelBox(bone3, 0, 0, -3.0F, -0.5F, -5.5F, 8, 0, 0, 0.0F, false));

        cube_r12 = new ModelRenderer(this);
        cube_r12.setRotationPoint(-9.5F, -0.5F, -5.5F);
        bone3.addChild(cube_r12);
        setRotationAngle(cube_r12, 0.0F, -1.5708F, 0.0F);
        cube_r12.cubeList.add(new ModelBox(cube_r12, 0, 0, -5.0F, 0.0F, -9.0F, 10, 0, 0, 0.0F, false));

        cube_r13 = new ModelRenderer(this);
        cube_r13.setRotationPoint(1.5F, 0.0F, -4.0F);
        bone3.addChild(cube_r13);
        setRotationAngle(cube_r13, 0.0F, -0.7854F, 0.0F);
        cube_r13.cubeList.add(new ModelBox(cube_r13, 12, 20, 0.0F, -1.0F, -1.0F, 1, 1, 2, 0.0F, false));

        cube_r14 = new ModelRenderer(this);
        cube_r14.setRotationPoint(-1.5F, 0.0F, -4.0F);
        bone3.addChild(cube_r14);
        setRotationAngle(cube_r14, 0.0F, -0.7854F, 0.0F);
        cube_r14.cubeList.add(new ModelBox(cube_r14, 24, 20, -1.0F, -1.0F, 0.0F, 2, 1, 1, 0.0F, false));

        cube_r15 = new ModelRenderer(this);
        cube_r15.setRotationPoint(-1.5F, 0.0F, -7.0F);
        bone3.addChild(cube_r15);
        setRotationAngle(cube_r15, 0.0F, -0.7854F, 0.0F);
        cube_r15.cubeList.add(new ModelBox(cube_r15, 18, 20, -1.0F, -1.0F, -1.0F, 1, 1, 2, 0.0F, false));

        cube_r16 = new ModelRenderer(this);
        cube_r16.setRotationPoint(1.5F, 0.0F, -7.0F);
        bone3.addChild(cube_r16);
        setRotationAngle(cube_r16, 0.0F, -0.7854F, 0.0F);
        cube_r16.cubeList.add(new ModelBox(cube_r16, 24, 22, -1.0F, -1.0F, -1.0F, 2, 1, 1, 0.0F, false));
    }

    /**
     * 设置模型激活状态
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }

    /**
     * 更新动画 - 支持关键帧动画和程序化动画
     */
    public void updateAnimation(float partialTicks) {
        // 更新动画时间
        if (isActive) {
            animationTick += partialTicks * 0.05F; // 调整速度
        } else {
            animationTick += partialTicks * 0.02F; // 待机时更慢
        }

        // 如果启用关键帧动画，使用 Blockbench 导出的动画
        if (useKeyframeAnimation) {
            try {
                // 尝试使用关键帧动画系统
                Class<?> animClass = Class.forName("com.moremod.client.animation.KeyframeAnimationSystem");
                java.lang.reflect.Method method = animClass.getMethod("applyMechanicalCoreAnimation",
                        ModelRenderer.class, ModelRenderer.class, ModelRenderer.class, float.class, float.class);

                float speed = isActive ? 1.0F : 0.5F;
                method.invoke(null, bone, bone2, bone3, animationTick * speed, partialTicks);

            } catch (Exception e) {
                // 如果关键帧系统不可用，使用程序化动画
                useProgrammaticAnimation(partialTicks);
            }
        } else {
            // 使用原来的程序化动画
            useProgrammaticAnimation(partialTicks);
        }
    }

    /**
     * 程序化动画（备用）
     */
    private void useProgrammaticAnimation(float partialTicks) {
        if (isActive) {
            // 主环旋转
            float rotation = (animationTick * 2) % 360;
            bone.rotateAngleY = (float) Math.toRadians(rotation);

            // 上下浮动
            float bobOffset = (float) Math.sin(animationTick * 0.1) * 0.05F;
            bone.offsetY = bobOffset;

            // bone3 倾斜摆动
            float tilt = (float) Math.sin(animationTick * 0.05) * 5;
            bone3.rotateAngleZ = (float) Math.toRadians(tilt);

            // bone2 反向旋转
            bone2.rotateAngleY = (float) Math.toRadians(-rotation * 0.5);
        } else {
            // 待机动画
            float slowRotation = (animationTick) % 360;
            bone.rotateAngleY = (float) Math.toRadians(slowRotation);
            bone3.rotateAngleY = (float) Math.toRadians(-slowRotation * 0.3);
        }
    }

    /**
     * 切换动画模式
     */
    public void setUseKeyframeAnimation(boolean use) {
        this.useKeyframeAnimation = use;
        if (!use) {
            // 重置动画状态
            resetAnimation();
        }
    }

    /**
     * 重置动画状态
     */
    private void resetAnimation() {
        bone.rotateAngleX = 0;
        bone.rotateAngleY = 0;
        bone.rotateAngleZ = 0;
        bone.offsetX = 0;
        bone.offsetY = 0;
        bone.offsetZ = 0;

        if (bone3 != null) {
            bone3.rotateAngleX = 0;
            bone3.rotateAngleY = 0;
            bone3.rotateAngleZ = 0;
            bone3.offsetX = 0;
            bone3.offsetY = 0;
            bone3.offsetZ = 0;
        }
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        // 缩放模型使其适合作为头部装备
        float scale = 1.0F;

        // 保存矩阵状态
        net.minecraft.client.renderer.GlStateManager.pushMatrix();

        // 缩放
        net.minecraft.client.renderer.GlStateManager.scale(scale, scale, scale);

        // 调整位置使其正确显示在头部上方（向上移动）
        // 降低位置，让它更贴近头部
        net.minecraft.client.renderer.GlStateManager.translate(0.0F, -2.1F, 0.0F);  // 从 -2.8F 改为 -1.8F

        // 渲染模型
        bone.render(f5);
        bone3.render(f5);

        // 恢复矩阵状态
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }

    /**
     * 直接渲染方法（用于物品栏显示）
     */
    public void renderForItem(float scale) {
        // 应用旋转以便更好地展示
        bone.rotateAngleY = (float) Math.toRadians(45);
        bone.rotateAngleX = (float) Math.toRadians(10);

        bone.render(scale);
        bone3.render(scale);
    }

    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }

    /**
     * 设置模型的旋转和位置（用于装备时）
     */
    public void setLivingAnimations(EntityLivingBase entityIn, float limbSwing, float limbSwingAmount, float partialTickTime) {
        super.setLivingAnimations(entityIn, limbSwing, limbSwingAmount, partialTickTime);
        updateAnimation(partialTickTime);
    }
}