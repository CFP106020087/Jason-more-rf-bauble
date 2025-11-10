// src/main/java/com/moremod/client/render/RenderSwordBeamAdvanced.java
package com.moremod.client.render;

import com.moremod.entity.EntitySwordBeam;
import com.moremod.client.model.ModelSwordBeam;
import com.moremod.client.model.ModelBeamCrescent;
import com.moremod.client.model.ModelBeamSpiral;
import com.moremod.client.model.ModelBeamDragon;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/** 高级3D剑气渲染器（细节层=Alpha、发光层=Additive） */
public class RenderSwordBeam extends Render<EntitySwordBeam> {

    private static final ResourceLocation TEX_NORMAL  = new ResourceLocation("moremod","textures/entity/beam_normal.png");
    private static final ResourceLocation TEX_SPIRAL  = new ResourceLocation("moremod","textures/entity/beam_spiral.png");
    private static final ResourceLocation TEX_CRES    = new ResourceLocation("moremod","textures/entity/beam_crescent.png");
    private static final ResourceLocation TEX_CROSS   = new ResourceLocation("moremod","textures/entity/beam_cross.png");
    private static final ResourceLocation TEX_DRAGON  = new ResourceLocation("moremod","textures/entity/dragon_model.png"); // ← 注意：模型“皮”贴图
    private static final ResourceLocation TEX_PHOENIX = new ResourceLocation("moremod","textures/entity/beam_phoenix.png");

    private final Map<EntitySwordBeam.BeamType, ModelBase> models = new HashMap<>();
    private final ModelBase defaultModel = new ModelSwordBeam();

    public RenderSwordBeam(RenderManager mgr) {
        super(mgr);
        models.put(EntitySwordBeam.BeamType.NORMAL,  new ModelSwordBeam());
        models.put(EntitySwordBeam.BeamType.SPIRAL,  new ModelBeamSpiral());
        models.put(EntitySwordBeam.BeamType.CRESCENT,new ModelBeamCrescent());
        models.put(EntitySwordBeam.BeamType.CROSS,   new ModelSwordBeam());
        models.put(EntitySwordBeam.BeamType.DRAGON,  new ModelBeamDragon()); // 立体龙
        models.put(EntitySwordBeam.BeamType.PHOENIX, new ModelSwordBeam());
    }

    @Override
    public void doRender(EntitySwordBeam e, double x, double y, double z, float yaw, float pt) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        float ry = e.prevRotationYaw   + (e.rotationYaw   - e.prevRotationYaw)   * pt;
        float rp = e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * pt;
        GlStateManager.rotate(ry - 90.0F, 0,1,0);
        GlStateManager.rotate(rp,        0,0,1);

        float baseScale = getBaseScale(e) * e.getScale(); // 世界尺度
        float r = e.getRed(), g = e.getGreen(), b = e.getBlue();
        float a = 1.0F - e.getLifeProgress();

        // 绑定贴图
        bindEntityTexture(e);

        // === 细节层（写深度 + Alpha） ===
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.depthMask(true);

        GlStateManager.pushMatrix();
        GlStateManager.scale(baseScale, baseScale, baseScale);
        GlStateManager.color(r, g, b, a);
        getModel(e).render(e, 0,0, e.ticksExisted, 0,0, 0.0625F);
        GlStateManager.popMatrix();

        // === 发光层（不写深度 + Additive） ===
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        float[] S = {1.25f, 1.55f, 1.9f};
        float[] Af= {0.55f, 0.35f, 0.18f};
        float[] Cf= {0.90f, 0.75f, 0.60f};

        for (int i=0;i<S.length;i++) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(baseScale*S[i], baseScale*S[i], baseScale*S[i]);
            GlStateManager.color(r*Cf[i], g*Cf[i], b*Cf[i], a*Af[i]);
            getModel(e).render(e, 0,0, e.ticksExisted, 0,0, 0.0625F);
            GlStateManager.popMatrix();
        }

        // 收尾
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();

        GlStateManager.popMatrix();
        super.doRender(e, x, y, z, yaw, pt);
    }

    private ModelBase getModel(EntitySwordBeam e) {
        ModelBase m = models.get(e.getBeamType());
        return m != null ? m : defaultModel;
    }

    private float getBaseScale(EntitySwordBeam e) {
        switch (e.getBeamType()) {
            case CRESCENT: return 1.5F;
            case CROSS:    return 1.2F;
            case DRAGON:   return 1.8F; // 立体龙稍大
            case PHOENIX:  return 1.6F;
            case SPIRAL:   return 1.3F;
            default:       return 1.0F;
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySwordBeam e) {
        switch (e.getBeamType()) {
            case SPIRAL:   return TEX_SPIRAL;
            case CRESCENT: return TEX_CRES;
            case CROSS:    return TEX_CROSS;
            case DRAGON:   return TEX_DRAGON;    // ← 立体龙用“模型皮”
            case PHOENIX:  return TEX_PHOENIX;
            default:       return TEX_NORMAL;
        }
    }
}
