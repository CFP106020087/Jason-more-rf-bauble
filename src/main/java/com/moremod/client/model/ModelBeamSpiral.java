// ModelBeamSpiral.java
package com.moremod.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * 螺旋形剑气模型
 */
public class ModelBeamSpiral extends ModelBase {
    
    private ModelRenderer[] spiralParts;
    private ModelRenderer core;
    
    public ModelBeamSpiral() {
        this.textureWidth = 128;
        this.textureHeight = 128;
        
        core = new ModelRenderer(this, 0, 0);
        core.addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3);
        core.setRotationPoint(0F, 0F, 0F);
        
        // 创建螺旋 - 20个部件沿螺旋线排列
        spiralParts = new ModelRenderer[20];
        
        for (int i = 0; i < spiralParts.length; i++) {
            spiralParts[i] = new ModelRenderer(this, 20, i * 5);
            
            // 螺旋参数
            float t = i / (float) spiralParts.length;
            float angle = t * (float) Math.PI * 4; // 2圈螺旋
            float radius = 3F + t * 5F; // 半径递增
            float length = t * 20F - 10F; // 沿长度方向
            
            float x = (float) Math.cos(angle) * radius;
            float y = (float) Math.sin(angle) * radius;
            float z = length;
            
            spiralParts[i].addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1);
            spiralParts[i].setRotationPoint(x, y, z);
        }
    }
    
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount,
                      float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        core.render(scale);
        
        for (ModelRenderer part : spiralParts) {
            part.render(scale);
        }
    }
    
    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale, Entity entity) {
        // 螺旋旋转动画
        float rotation = ageInTicks * 0.3F;
        
        for (int i = 0; i < spiralParts.length; i++) {
            float offset = i / (float) spiralParts.length * (float) Math.PI * 2;
            spiralParts[i].rotateAngleZ = rotation + offset;
        }
    }
}

