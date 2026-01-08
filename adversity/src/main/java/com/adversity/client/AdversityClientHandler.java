package com.adversity.client;

import com.adversity.Adversity;
import com.adversity.affix.AffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 客户端事件处理器 - 处理渲染等客户端逻辑
 */
@SideOnly(Side.CLIENT)
public class AdversityClientHandler {

    // 渲染距离
    private static final double RENDER_DISTANCE = 32.0;

    /**
     * 渲染实体名称后显示词条信息
     */
    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Post<EntityLiving> event) {
        if (!(event.getEntity() instanceof EntityLiving)) return;

        EntityLiving entity = (EntityLiving) event.getEntity();
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);

        if (cap == null || cap.getTier() <= 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        double distance = mc.player.getDistanceSq(entity);
        if (distance > RENDER_DISTANCE * RENDER_DISTANCE) return;

        // 渲染等级和词条信息
        renderAffixInfo(entity, cap, event.getX(), event.getY(), event.getZ());
    }

    /**
     * 渲染词条信息
     */
    private void renderAffixInfo(EntityLiving entity, IAdversityCapability cap, double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fontRenderer = mc.fontRenderer;

        // 构建显示文本
        StringBuilder text = new StringBuilder();

        // 等级显示
        TextFormatting tierColor = getTierColor(cap.getTier());
        text.append(tierColor).append("[T").append(cap.getTier()).append("]");

        // 词条显示（简化版，只显示数量）
        int affixCount = cap.getAffixCount();
        if (affixCount > 0) {
            text.append(" ").append(TextFormatting.GRAY).append(affixCount).append(" Affixes");
        }

        String displayText = text.toString();

        // 计算渲染位置（在实体上方）
        float height = entity.height + 0.5f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + height + 0.3, z);
        GlStateManager.glNormal3f(0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * mc.getRenderManager().playerViewX, 1.0f, 0.0f, 0.0f);
        GlStateManager.scale(-0.025f, -0.025f, 0.025f);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );

        int textWidth = fontRenderer.getStringWidth(displayText);

        // 绘制背景
        int bgColor = 0x40000000;
        fontRenderer.drawString(displayText, -textWidth / 2, 0, 0xFFFFFF);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    /**
     * 根据等级获取颜色
     */
    private TextFormatting getTierColor(int tier) {
        switch (tier) {
            case 1: return TextFormatting.GREEN;
            case 2: return TextFormatting.BLUE;
            case 3: return TextFormatting.LIGHT_PURPLE;
            case 4: return TextFormatting.GOLD;
            default: return TextFormatting.WHITE;
        }
    }
}
