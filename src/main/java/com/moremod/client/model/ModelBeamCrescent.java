// ModelBeamCrescent.java
package com.moremod.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * 月牙形剑气模型
 */
public class ModelBeamCrescent extends ModelBase {
    
    private ModelRenderer[] crescentParts;
    private ModelRenderer core;
    
    public ModelBeamCrescent() {
        this.textureWidth = 128;
        this.textureHeight = 128;
        
        // 核心
        core = new ModelRenderer(this, 0, 0);
        core.addBox(-2F, -2F, -1F, 4, 4, 2);
        core.setRotationPoint(0F, 0F, 0F);
        
        // 创建月牙形状 - 使用多个小方块拼接成弧形
        crescentParts = new ModelRenderer[12];
        
        for (int i = 0; i < crescentParts.length; i++) {
            crescentParts[i] = new ModelRenderer(this, 20, i * 8);
            
            // 计算弧形位置
            float angle = (float) (Math.PI * (i / (float) crescentParts.length - 0.5));
            float radius = 12F;
            float x = (float) Math.cos(angle) * radius;
            float y = (float) Math.sin(angle) * radius;
            
            // 添加方块
            crescentParts[i].addBox(-1F, -1F, -0.5F, 2, 2, 1);
            crescentParts[i].setRotationPoint(x, y, 0F);
            
            // 旋转使其切线方向
            crescentParts[i].rotateAngleZ = angle + (float) Math.PI / 2;
        }
    }
    
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount,
                      float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        core.render(scale);
        
        for (ModelRenderer part : crescentParts) {
            part.render(scale);
        }
    }
    
    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale, Entity entity) {
        // 月牙旋转
        float rotation = ageInTicks * 0.15F;
        core.rotateAngleZ = rotation;
        
        for (ModelRenderer part : crescentParts) {
            part.rotateAngleY = rotation * 0.5F;
        }
    }
}
