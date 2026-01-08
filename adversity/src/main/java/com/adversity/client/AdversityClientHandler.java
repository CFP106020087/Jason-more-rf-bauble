package com.adversity.client;

import com.adversity.affix.AffixRegistry;
import com.adversity.affix.IAffix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 客户端事件处理器 - 处理渲染等客户端逻辑
 * 使用从服务端同步过来的缓存数据进行渲染
 */
@SideOnly(Side.CLIENT)
public class AdversityClientHandler {

    // 渲染距离
    private static final double RENDER_DISTANCE = 32.0;

    // 血条尺寸
    private static final float HEALTH_BAR_WIDTH = 40.0f;
    private static final float HEALTH_BAR_HEIGHT = 4.0f;

    /**
     * 玩家切换维度时清除缓存
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            // 退出世界时清除缓存
            ClientAdversityCache.clearAll();
        }
    }

    /**
     * 渲染实体名称后显示词条信息和血条
     */
    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Post<EntityLiving> event) {
        if (!(event.getEntity() instanceof EntityLiving)) return;

        EntityLiving entity = (EntityLiving) event.getEntity();

        // 从客户端缓存获取数据
        ClientAdversityCache.CachedEntityData data = ClientAdversityCache.getEntityData(entity.getEntityId());
        if (data == null || data.tier <= 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        double distance = mc.player.getDistanceSq(entity);
        if (distance > RENDER_DISTANCE * RENDER_DISTANCE) return;

        // 渲染等级、词条信息和血条
        renderAffixInfo(entity, data, event.getX(), event.getY(), event.getZ());
    }

    /**
     * 渲染词条信息和血条
     */
    private void renderAffixInfo(EntityLiving entity, ClientAdversityCache.CachedEntityData data,
                                  double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fontRenderer = mc.fontRenderer;

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
        GlStateManager.disableTexture2D();

        // 渲染血条
        renderHealthBar(entity, data);

        GlStateManager.enableTexture2D();

        // 构建显示文本 - 等级
        StringBuilder tierText = new StringBuilder();
        TextFormatting tierColor = getTierColor(data.tier);
        tierText.append(tierColor).append("[T").append(data.tier).append("]");

        // 绘制等级文本
        String tierStr = tierText.toString();
        int tierWidth = fontRenderer.getStringWidth(tierStr);
        fontRenderer.drawString(tierStr, -tierWidth / 2, -12, 0xFFFFFF);

        // 绘制词条名称
        List<ResourceLocation> affixIds = data.affixIds;
        if (!affixIds.isEmpty()) {
            StringBuilder affixText = new StringBuilder();
            for (int i = 0; i < affixIds.size(); i++) {
                IAffix affix = AffixRegistry.getAffix(affixIds.get(i));
                if (affix != null) {
                    if (i > 0) affixText.append(" ");
                    affixText.append(getAffixColor(affix)).append(affix.getDisplayName());
                }
            }
            String affixStr = affixText.toString();
            int affixWidth = fontRenderer.getStringWidth(affixStr);
            fontRenderer.drawString(affixStr, -affixWidth / 2, 0, 0xFFFFFF);
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    /**
     * 渲染血条
     */
    private void renderHealthBar(EntityLiving entity, ClientAdversityCache.CachedEntityData data) {
        float healthPercent = entity.getHealth() / entity.getMaxHealth();
        healthPercent = Math.max(0, Math.min(1, healthPercent));

        float halfWidth = HEALTH_BAR_WIDTH / 2;
        float yOffset = -20;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 背景（黑色）
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(-halfWidth - 1, yOffset - 1, 0).color(0, 0, 0, 128).endVertex();
        buffer.pos(-halfWidth - 1, yOffset + HEALTH_BAR_HEIGHT + 1, 0).color(0, 0, 0, 128).endVertex();
        buffer.pos(halfWidth + 1, yOffset + HEALTH_BAR_HEIGHT + 1, 0).color(0, 0, 0, 128).endVertex();
        buffer.pos(halfWidth + 1, yOffset - 1, 0).color(0, 0, 0, 128).endVertex();
        tessellator.draw();

        // 血条背景（深红）
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(-halfWidth, yOffset, 0).color(64, 0, 0, 200).endVertex();
        buffer.pos(-halfWidth, yOffset + HEALTH_BAR_HEIGHT, 0).color(64, 0, 0, 200).endVertex();
        buffer.pos(halfWidth, yOffset + HEALTH_BAR_HEIGHT, 0).color(64, 0, 0, 200).endVertex();
        buffer.pos(halfWidth, yOffset, 0).color(64, 0, 0, 200).endVertex();
        tessellator.draw();

        // 当前血量（根据等级变色）
        int[] color = getTierHealthColor(data.tier);
        float currentWidth = HEALTH_BAR_WIDTH * healthPercent;
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(-halfWidth, yOffset, 0).color(color[0], color[1], color[2], 220).endVertex();
        buffer.pos(-halfWidth, yOffset + HEALTH_BAR_HEIGHT, 0).color(color[0], color[1], color[2], 220).endVertex();
        buffer.pos(-halfWidth + currentWidth, yOffset + HEALTH_BAR_HEIGHT, 0).color(color[0], color[1], color[2], 220).endVertex();
        buffer.pos(-halfWidth + currentWidth, yOffset, 0).color(color[0], color[1], color[2], 220).endVertex();
        tessellator.draw();
    }

    /**
     * 根据等级获取文字颜色
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

    /**
     * 根据等级获取血条颜色
     */
    private int[] getTierHealthColor(int tier) {
        switch (tier) {
            case 1: return new int[]{85, 255, 85};      // 绿色
            case 2: return new int[]{85, 85, 255};      // 蓝色
            case 3: return new int[]{255, 85, 255};     // 紫色
            case 4: return new int[]{255, 170, 0};      // 金色
            default: return new int[]{255, 85, 85};     // 红色
        }
    }

    /**
     * 根据词条类型获取颜色
     */
    private TextFormatting getAffixColor(IAffix affix) {
        switch (affix.getType()) {
            case OFFENSIVE:
                return TextFormatting.RED;
            case DEFENSIVE:
                return TextFormatting.AQUA;
            case UTILITY:
                return TextFormatting.YELLOW;
            case SPECIAL:
                return TextFormatting.LIGHT_PURPLE;
            default:
                return TextFormatting.GRAY;
        }
    }
}
