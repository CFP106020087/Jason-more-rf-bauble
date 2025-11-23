package com.moremod.system.visual;

import com.moremod.item.ItemMechanicalCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * 血液暗角效果
 * 从机械核心NBT读取排异值来决定渲染强度
 */
@SideOnly(Side.CLIENT)
public class BloodVignetteEffect {

    private static final ResourceLocation VIGNETTE =
            new ResourceLocation("moremod", "textures/gui/blood_vignette.png");

    // 调试开关
    private static final boolean DEBUG = false;

    /**
     * 从机械核心NBT读取排异值
     */
    private static float getRejectionFromCore(Minecraft mc) {
        if (mc.player == null) return 0;
        
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(mc.player);
        if (core.isEmpty()) return 0;
        
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null || !nbt.hasKey("rejection")) return 0;
        
        NBTTagCompound group = nbt.getCompoundTag("rejection");
        return group.getFloat("RejectionLevel");
    }

    public static void render(float rejection, RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        
        // 如果传入的排异值为0，尝试从核心读取
        if (rejection <= 0) {
            rejection = getRejectionFromCore(mc);
            if (rejection <= 0) return; // 确实没有排异，不渲染
        }

        // 排异不够，完全不渲染
        if (rejection < VisualConfig.VIGNETTE_START) {
            if (DEBUG && rejection > 0) {
                System.out.println("[暗角] 排异 " + rejection + "% < " + VisualConfig.VIGNETTE_START + "% - 不渲染");
            }
            return;
        }

        // 计算透明度
        float alpha = Math.min((rejection - VisualConfig.VIGNETTE_START)
                / (100f - VisualConfig.VIGNETTE_START), 1f);

        if (DEBUG) {
            System.out.println("[暗角] 排异 " + rejection + "% - Alpha: " + String.format("%.2f", alpha));
        }

        ScaledResolution res = new ScaledResolution(mc);
        int w = res.getScaledWidth();
        int h = res.getScaledHeight();

        // 保存当前GL状态
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        
        try {
            // 设置渲染状态
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO
            );
            
            // 根据排异程度调整颜色
            if (rejection >= 80) {
                // 高排异：深红色调
                GlStateManager.color(1f, 0.7f, 0.7f, alpha);
            } else if (rejection >= 60) {
                // 中排异：红色调
                GlStateManager.color(1f, 0.85f, 0.85f, alpha);
            } else {
                // 低排异：正常暗角
                GlStateManager.color(1f, 1f, 1f, alpha);
            }
            
            GlStateManager.disableAlpha();
            GlStateManager.disableLighting();

            // 绑定纹理
            mc.getTextureManager().bindTexture(VIGNETTE);

            // 使用 Tessellator 绘制全屏缩放的纹理
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(0, h, -90.0D).tex(0, 1).endVertex();
            buffer.pos(w, h, -90.0D).tex(1, 1).endVertex();
            buffer.pos(w, 0, -90.0D).tex(1, 0).endVertex();
            buffer.pos(0, 0, -90.0D).tex(0, 0).endVertex();
            tessellator.draw();
            
        } finally {
            // 确保恢复GL状态
            GlStateManager.enableLighting();
            GlStateManager.enableAlpha();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            
            // 恢复保存的状态
            GlStateManager.popAttrib();
            GlStateManager.popMatrix();
        }
    }
    
    /**
     * 测试渲染（用于调试）
     */
    public static void testRender(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        
        // 强制渲染50%透明度的暗角
        float testRejection = 70f; // 测试用排异值
        System.out.println("[暗角测试] 强制渲染，排异值: " + testRejection + "%");
        
        render(testRejection, event);
    }
}
