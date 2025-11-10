// ModelBeamPhoenix.java
package com.moremod.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * 凤凰剑气3D模型
 * 基于设计图：展翅飞翔的凤凰形态
 * 包含：头部+冠羽、身体、双翼（多层羽毛）、长尾羽
 */
public class ModelBeamPhoenix extends ModelBase {
    
    // 头部
    private ModelRenderer head;
    private ModelRenderer[] crest; // 冠羽 (3根)
    
    // 身体
    private ModelRenderer body;
    private ModelRenderer neck;
    
    // 翅膀 - 左翼
    private ModelRenderer leftWingUpper;   // 上层羽毛
    private ModelRenderer leftWingMiddle;  // 中层羽毛
    private ModelRenderer leftWingLower;   // 下层羽毛
    private ModelRenderer[] leftFeathers;  // 翼尖羽毛 (5根)
    
    // 翅膀 - 右翼
    private ModelRenderer rightWingUpper;
    private ModelRenderer rightWingMiddle;
    private ModelRenderer rightWingLower;
    private ModelRenderer[] rightFeathers;
    
    // 尾羽 (4根长尾羽)
    private ModelRenderer[] tailFeathers;
    
    public ModelBeamPhoenix() {
        this.textureWidth = 128;
        this.textureHeight = 128;
        
        // ====== 头部 ======
        // 主头部 - 圆形
        head = new ModelRenderer(this, 0, 0);
        head.addBox(-2.5F, -2.5F, -3F, 5, 5, 5);
        head.setRotationPoint(0F, -2F, -6F);
        
        // 冠羽 - 3根火焰状羽毛
        crest = new ModelRenderer[3];
        for (int i = 0; i < 3; i++) {
            crest[i] = new ModelRenderer(this, 0, 12 + i * 6);
            float angle = (i - 1) * 20F; // -20, 0, 20度
            float xOffset = (float) Math.sin(Math.toRadians(angle)) * 1.5F;
            
            // 火焰状，向上渐细
            crest[i].addBox(-0.5F, -6F, -0.5F, 1, 6, 1);
            crest[i].setRotationPoint(xOffset, -2.5F, -4F);
            crest[i].rotateAngleX = (float) Math.toRadians(-30);
            crest[i].rotateAngleZ = (float) Math.toRadians(angle);
        }
        
        // ====== 颈部和身体 ======
        neck = new ModelRenderer(this, 20, 0);
        neck.addBox(-1.5F, -1.5F, -3F, 3, 3, 4);
        neck.setRotationPoint(0F, -1F, -3F);
        neck.rotateAngleX = (float) Math.toRadians(-15);
        
        body = new ModelRenderer(this, 20, 10);
        body.addBox(-3F, -2.5F, -2F, 6, 5, 8);
        body.setRotationPoint(0F, 0F, 0F);
        
        // ====== 左翼 ======
        // 上层羽毛 - 最长
        leftWingUpper = new ModelRenderer(this, 40, 0);
        leftWingUpper.addBox(0F, -2F, -1F, 14, 4, 1);
        leftWingUpper.setRotationPoint(3F, -1F, 0F);
        leftWingUpper.rotateAngleZ = (float) Math.toRadians(30);
        leftWingUpper.rotateAngleY = (float) Math.toRadians(-20);
        
        // 中层羽毛
        leftWingMiddle = new ModelRenderer(this, 40, 8);
        leftWingMiddle.addBox(0F, -1.5F, -0.5F, 12, 3, 1);
        leftWingMiddle.setRotationPoint(3F, 0F, 1F);
        leftWingMiddle.rotateAngleZ = (float) Math.toRadians(25);
        leftWingMiddle.rotateAngleY = (float) Math.toRadians(-15);
        
        // 下层羽毛
        leftWingLower = new ModelRenderer(this, 40, 14);
        leftWingLower.addBox(0F, -1F, -0.5F, 10, 2, 1);
        leftWingLower.setRotationPoint(3F, 1F, 2F);
        leftWingLower.rotateAngleZ = (float) Math.toRadians(20);
        leftWingLower.rotateAngleY = (float) Math.toRadians(-10);
        
        // 翼尖分离羽毛 - 5根
        leftFeathers = new ModelRenderer[5];
        for (int i = 0; i < 5; i++) {
            leftFeathers[i] = new ModelRenderer(this, 40, 20 + i * 3);
            leftFeathers[i].addBox(0F, -0.5F, -0.25F, 8, 1, 1);
            
            float yOffset = -1F + i * 0.8F;
            float angle = 30F + i * 5F;
            
            leftFeathers[i].setRotationPoint(16F, yOffset, 0F);
            leftFeathers[i].rotateAngleZ = (float) Math.toRadians(angle);
        }
        
        // ====== 右翼（镜像左翼）======
        rightWingUpper = new ModelRenderer(this, 40, 0);
        rightWingUpper.mirror = true;
        rightWingUpper.addBox(-14F, -2F, -1F, 14, 4, 1);
        rightWingUpper.setRotationPoint(-3F, -1F, 0F);
        rightWingUpper.rotateAngleZ = (float) Math.toRadians(-30);
        rightWingUpper.rotateAngleY = (float) Math.toRadians(20);
        
        rightWingMiddle = new ModelRenderer(this, 40, 8);
        rightWingMiddle.mirror = true;
        rightWingMiddle.addBox(-12F, -1.5F, -0.5F, 12, 3, 1);
        rightWingMiddle.setRotationPoint(-3F, 0F, 1F);
        rightWingMiddle.rotateAngleZ = (float) Math.toRadians(-25);
        rightWingMiddle.rotateAngleY = (float) Math.toRadians(15);
        
        rightWingLower = new ModelRenderer(this, 40, 14);
        rightWingLower.mirror = true;
        rightWingLower.addBox(-10F, -1F, -0.5F, 10, 2, 1);
        rightWingLower.setRotationPoint(-3F, 1F, 2F);
        rightWingLower.rotateAngleZ = (float) Math.toRadians(-20);
        rightWingLower.rotateAngleY = (float) Math.toRadians(10);
        
        rightFeathers = new ModelRenderer[5];
        for (int i = 0; i < 5; i++) {
            rightFeathers[i] = new ModelRenderer(this, 40, 20 + i * 3);
            rightFeathers[i].mirror = true;
            rightFeathers[i].addBox(-8F, -0.5F, -0.25F, 8, 1, 1);
            
            float yOffset = -1F + i * 0.8F;
            float angle = -30F - i * 5F;
            
            rightFeathers[i].setRotationPoint(-16F, yOffset, 0F);
            rightFeathers[i].rotateAngleZ = (float) Math.toRadians(angle);
        }
        
        // ====== 尾羽 - 4根流动长尾羽 ======
        tailFeathers = new ModelRenderer[4];
        for (int i = 0; i < 4; i++) {
            tailFeathers[i] = new ModelRenderer(this, 0, 40 + i * 10);
            
            // 尾羽长度递减
            float length = 20F - i * 2F;
            tailFeathers[i].addBox(-1F, 0F, 0F, 2, 1, (int)length);
            
            // 位置：从身体后部散开
            float xOffset = (i % 2 == 0 ? -1F : 1F) * (i / 2 + 0.5F);
            float yOffset = 2F + i * 0.3F;
            
            tailFeathers[i].setRotationPoint(xOffset, yOffset, 6F);
            tailFeathers[i].rotateAngleX = (float) Math.toRadians(15 + i * 5);
        }
    }
    
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount,
                      float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        
        // 渲染所有部件
        head.render(scale);
        for (ModelRenderer c : crest) {
            c.render(scale);
        }
        
        neck.render(scale);
        body.render(scale);
        
        // 左翼
        leftWingUpper.render(scale);
        leftWingMiddle.render(scale);
        leftWingLower.render(scale);
        for (ModelRenderer f : leftFeathers) {
            f.render(scale);
        }
        
        // 右翼
        rightWingUpper.render(scale);
        rightWingMiddle.render(scale);
        rightWingLower.render(scale);
        for (ModelRenderer f : rightFeathers) {
            f.render(scale);
        }
        
        // 尾羽
        for (ModelRenderer t : tailFeathers) {
            t.render(scale);
        }
    }
    
    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        
        float time = ageInTicks * 0.15F;
        
        // 头部和冠羽轻微摆动
        head.rotateAngleY = (float) Math.sin(time * 0.5) * 0.1F;
        
        for (int i = 0; i < crest.length; i++) {
            float offset = i * 0.3F;
            crest[i].rotateAngleZ += (float) Math.sin(time + offset) * 0.15F;
        }
        
        // 翅膀扇动 - 上下摆动
        float wingFlap = (float) Math.sin(time * 2) * 0.4F;
        
        leftWingUpper.rotateAngleZ = (float) Math.toRadians(30) + wingFlap;
        leftWingMiddle.rotateAngleZ = (float) Math.toRadians(25) + wingFlap * 0.8F;
        leftWingLower.rotateAngleZ = (float) Math.toRadians(20) + wingFlap * 0.6F;
        
        rightWingUpper.rotateAngleZ = (float) Math.toRadians(-30) - wingFlap;
        rightWingMiddle.rotateAngleZ = (float) Math.toRadians(-25) - wingFlap * 0.8F;
        rightWingLower.rotateAngleZ = (float) Math.toRadians(-20) - wingFlap * 0.6F;
        
        // 翼尖羽毛独立波动
        for (int i = 0; i < leftFeathers.length; i++) {
            float featherOffset = i * 0.2F;
            float featherWave = (float) Math.sin(time * 2 + featherOffset) * 0.2F;
            leftFeathers[i].rotateAngleZ = (float) Math.toRadians(30 + i * 5) + featherWave;
            rightFeathers[i].rotateAngleZ = (float) Math.toRadians(-30 - i * 5) - featherWave;
        }
        
        // 尾羽流动 - 波浪状运动
        for (int i = 0; i < tailFeathers.length; i++) {
            float tailOffset = i * 0.4F;
            float tailWave = (float) Math.sin(time * 0.8 + tailOffset) * 0.3F;
            
            tailFeathers[i].rotateAngleX = (float) Math.toRadians(15 + i * 5) + tailWave;
            tailFeathers[i].rotateAngleY = (float) Math.sin(time * 0.6 + tailOffset) * 0.2F;
        }
        
        // 身体轻微上下起伏
        body.rotateAngleX = (float) Math.sin(time) * 0.05F;
    }
}
