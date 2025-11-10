// ModelSwordBeam.java
package com.moremod.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * 剑气的3D模型
 * 使用多个立方体/面片组成立体的剑气形状
 */
public class ModelSwordBeam extends ModelBase {
    
    // 模型部件
    private ModelRenderer core;           // 核心
    private ModelRenderer blade1;         // 刀刃1
    private ModelRenderer blade2;         // 刀刃2
    private ModelRenderer blade3;         // 刀刃3（交叉）
    private ModelRenderer blade4;         // 刀刃4（交叉）
    private ModelRenderer glowOuter;      // 外层光晕
    
    public ModelSwordBeam() {
        this.textureWidth = 128;
        this.textureHeight = 128;
        
        // 核心 - 中心实心部分
        core = new ModelRenderer(this, 0, 0);
        core.addBox(-2F, -2F, -2F, 4, 4, 4);
        core.setRotationPoint(0F, 0F, 0F);
        
        // 刀刃1 - 主刀刃（前后方向）
        blade1 = new ModelRenderer(this, 0, 20);
        blade1.addBox(-8F, -0.5F, -0.5F, 16, 1, 1);
        blade1.setRotationPoint(0F, 0F, 0F);
        
        // 刀刃2 - 主刀刃的竖直部分
        blade2 = new ModelRenderer(this, 0, 30);
        blade2.addBox(-0.5F, -8F, -0.5F, 1, 16, 1);
        blade2.setRotationPoint(0F, 0F, 0F);
        
        // 刀刃3 - 交叉刀刃（45度）
        blade3 = new ModelRenderer(this, 0, 40);
        blade3.addBox(-8F, -0.5F, -0.5F, 16, 1, 1);
        blade3.setRotationPoint(0F, 0F, 0F);
        blade3.rotateAngleY = (float) Math.toRadians(45);
        
        // 刀刃4 - 交叉刀刃（135度）
        blade4 = new ModelRenderer(this, 0, 50);
        blade4.addBox(-8F, -0.5F, -0.5F, 16, 1, 1);
        blade4.setRotationPoint(0F, 0F, 0F);
        blade4.rotateAngleY = (float) Math.toRadians(135);
        
        // 外层光晕 - 更大更透明
        glowOuter = new ModelRenderer(this, 0, 60);
        glowOuter.addBox(-10F, -10F, -1F, 20, 20, 2);
        glowOuter.setRotationPoint(0F, 0F, 0F);
    }
    
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, 
                      float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        
        // 渲染所有部件
        core.render(scale);
        blade1.render(scale);
        blade2.render(scale);
        blade3.render(scale);
        blade4.render(scale);
        glowOuter.render(scale);
    }
    
    /**
     * 设置旋转和动画
     */
    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        
        // 添加旋转动画
        float rotation = ageInTicks * 0.2F;
        
        // 核心慢速旋转
        core.rotateAngleZ = rotation * 0.5F;
        
        // 刀刃快速旋转
        blade1.rotateAngleZ = rotation;
        blade2.rotateAngleZ = rotation;
        blade3.rotateAngleY = (float) Math.toRadians(45) + rotation;
        blade4.rotateAngleY = (float) Math.toRadians(135) + rotation;
        
        // 光晕脉动
        float pulse = (float) Math.sin(ageInTicks * 0.3) * 0.1F + 1.0F;
        glowOuter.rotateAngleZ = rotation * 0.3F;
    }
}
