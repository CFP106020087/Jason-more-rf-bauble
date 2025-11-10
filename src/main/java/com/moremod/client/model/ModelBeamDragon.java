// src/main/java/com/moremod/client/model/ModelBeamDragon.java
package com.moremod.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/** 立体龙（方块风）——配套 dragon_model.png UV 展开 */
public class ModelBeamDragon extends ModelBase {

    // 根节点
    private final ModelRenderer headRoot;
    private final ModelRenderer bodyRoot;
    private final ModelRenderer tailRoot;

    // 方便动画的数组
    private final ModelRenderer[] neckSeg = new ModelRenderer[3];
    private final ModelRenderer[] bodySeg = new ModelRenderer[5];
    private final ModelRenderer[] tailSeg = new ModelRenderer[6];
    private final ModelRenderer[] spines  = new ModelRenderer[8];

    public ModelBeamDragon() {
        this.textureWidth = 256;
        this.textureHeight = 256;

        // === 头部 ===
        headRoot = new ModelRenderer(this);
        headRoot.setRotationPoint(0F, -2F, -12F);

        // head 8×6×8  → 展开  (U=32, V=22)，占位 (0,0)
        ModelRenderer head = new ModelRenderer(this, 0, 0);
        head.addBox(-4, -3, -8, 8, 6, 8, 0F);
        headRoot.addChild(head);

        // jaw 6×2×6 → 展开 (24×14)，占位 (0,24)
        ModelRenderer jaw = new ModelRenderer(this, 0, 24);
        jaw.setRotationPoint(0, 3F, 0F); // 相对 headRoot
        jaw.addBox(-3, 0, -14, 6, 2, 6, 0F);
        headRoot.addChild(jaw);

        // horns 2×4×2 → 展开 (8×8)，(40,0)/(48,0)
        ModelRenderer hornL = new ModelRenderer(this, 40, 0);
        hornL.setRotationPoint( 3F, -3F, -2F);
        hornL.addBox(-1, -4, -1, 2, 4, 2, 0F);
        hornL.rotateAngleX = (float)Math.toRadians(-30);
        hornL.rotateAngleZ = (float)Math.toRadians(+15);
        headRoot.addChild(hornL);

        ModelRenderer hornR = new ModelRenderer(this, 48, 0);
        hornR.setRotationPoint(-3F, -3F, -2F);
        hornR.addBox(-1, -4, -1, 2, 4, 2, 0F);
        hornR.rotateAngleX = (float)Math.toRadians(-30);
        hornR.rotateAngleZ = (float)Math.toRadians(-15);
        headRoot.addChild(hornR);

        // whiskers 8×1×1 → 展开 (18×3)，(64,0/4/8/12)
        for (int i=0;i<4;i++) {
            int tv = 0 + i*4;
            boolean upper = i<2;
            float side = (i%2==0) ? +1F : -1F;

            ModelRenderer w = new ModelRenderer(this, 64, tv);
            w.setRotationPoint(side*3F, upper?-1F:2F, -13F);
            w.addBox(0, -0.5F, -0.5F, 8, 1, 1, 0F);
            w.rotateAngleY = (float)Math.toRadians(side*30);
            w.rotateAngleZ = (float)Math.toRadians(side*(upper?-10:10));
            headRoot.addChild(w);
        }

        // === 身体根 ===
        bodyRoot = new ModelRenderer(this);
        bodyRoot.setRotationPoint(0F, 0F, -4F);

        // 颈部 3 段（5×5×4 → 4×4×4 → 4×4×4）
        // 占位：(0,40)(20,40)(38,40)
        neckSeg[0] = new ModelRenderer(this, 0, 40); // 5×5×4
        neckSeg[0].addBox(-2, -2, -2, 5, 5, 4, 0F); // center 对齐
        neckSeg[0].setRotationPoint(0, -1F, -4F);
        bodyRoot.addChild(neckSeg[0]);

        neckSeg[1] = new ModelRenderer(this, 20, 40);
        neckSeg[1].addBox(-2, -2, -2, 4, 4, 4, 0F);
        neckSeg[1].setRotationPoint(0, 0F, 2.5F);
        neckSeg[0].addChild(neckSeg[1]);

        neckSeg[2] = new ModelRenderer(this, 38, 40);
        neckSeg[2].addBox(-2, -2, -2, 4, 4, 4, 0F);
        neckSeg[2].setRotationPoint(0, 0F, 3.0F);
        neckSeg[1].addChild(neckSeg[2]);

        // 身体 5 段（6×5×4 → 6×5×4 → 5×5×4 → 5×4×4 → 4×4×4）
        int[][] bodyUV = {{0,56},{22,56},{44,56},{64,56},{84,56}};
        int[][] bodyWHd = {{6,5,4},{6,5,4},{5,5,4},{5,4,4},{4,4,4}};

        ModelRenderer last = neckSeg[2];
        for (int i=0;i<5;i++) {
            int w=bodyWHd[i][0], h=bodyWHd[i][1], d=bodyWHd[i][2];
            ModelRenderer seg = new ModelRenderer(this, bodyUV[i][0], bodyUV[i][1]);
            seg.addBox(-w/2, -h/2, -d/2, w, h, d, 0F);
            seg.setRotationPoint(0F, 0F, 3.5F);
            last.addChild(seg);
            bodySeg[i] = seg;
            last = seg;
        }

        // 背脊刺（1×3×1）沿着 body 链
        for (int i=0;i<8;i++) {
            ModelRenderer sp = new ModelRenderer(this, 112, i*6);
            sp.addBox(-0.5F, -3F, -0.5F, 1, 3, 1, 0F);
            sp.setRotationPoint(0F, - (i<4?3F:2.5F), (i<4? i*1.5F : (i-4)*1.5F));
            sp.rotateAngleX = (float)Math.toRadians(-10);
            bodySeg[Math.min(i/2, 4)].addChild(sp); // 分配到前几段
            spines[i] = sp;
        }

        // === 尾巴根 ===
        tailRoot = new ModelRenderer(this);
        tailRoot.setRotationPoint(0F, 0F, 0F);
        last.addChild(tailRoot);

        // 尾巴 6 段（4×4×4 → 3×3×4 → 3×3×4 → 2×2×4 → 2×2×4 → 1×1×4）
        int[][] tailUV = {{0,  72},{18, 72},{36,72},{54,72},{72,72},{90,72}};
        int[][] tailWHd= {{4,4,4},{3,3,4},{3,3,4},{2,2,4},{2,2,4},{1,1,4}};

        ModelRenderer prev = tailRoot;
        for (int i=0;i<6;i++) {
            int w=tailWHd[i][0], h=tailWHd[i][1], d=tailWHd[i][2];
            ModelRenderer seg = new ModelRenderer(this, tailUV[i][0], tailUV[i][1]);
            seg.addBox(-w/2, -h/2, -d/2, w, h, d, 0F);
            seg.setRotationPoint(0F, 0F, 3.5F);
            prev.addChild(seg);
            tailSeg[i] = seg;
            prev = seg;
        }
    }

    @Override
    public void render(Entity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch, float f5) {
        setRotationAngles(limbSwing, limbSwingAmount, age, netHeadYaw, headPitch, f5, e);
        headRoot.render(f5);
        bodyRoot.render(f5); // 包括 neck/body/尾部链
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch, float f5, Entity e) {
        super.setRotationAngles(limbSwing, limbSwingAmount, age, netHeadYaw, headPitch, f5, e);

        float t = age * 0.1F;

        // 头部轻摆
        headRoot.rotateAngleY = (float)Math.sin(t*0.5F)*0.2F;
        headRoot.rotateAngleX = (float)Math.cos(t*0.3F)*0.1F;

        // 颈部波动（链式衰减）
        for (int i=0;i<neckSeg.length;i++) {
            float off = i*0.35F;
            neckSeg[i].rotateAngleY = (float)Math.sin(t+off)*0.15F;
            neckSeg[i].rotateAngleX = (float)Math.cos(t*0.8F+off)*0.08F;
        }

        // 身体蛇形
        for (int i=0;i<bodySeg.length;i++) {
            float off = i*0.45F;
            bodySeg[i].rotateAngleY = (float)Math.sin(t+off)*0.22F;
            bodySeg[i].rotateAngleX = (float)Math.cos(t*0.7F+off)*0.08F;
        }

        // 尾巴更大幅度
        for (int i=0;i<tailSeg.length;i++) {
            float off = i*0.55F;
            float amp = 0.28F + i*0.05F;
            tailSeg[i].rotateAngleY = (float)Math.sin(t+off)*amp;
            tailSeg[i].rotateAngleX = (float)Math.cos(t*0.6F+off)*0.12F;
        }
    }
}
