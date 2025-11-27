package com.moremod.client.render;

import com.moremod.system.humanity.AscensionRoute;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Map;

/**
 * 机械化叠加渲染层
 * Mechanical Overlay Layer
 *
 * 根据玩家人性值渲染机械化效果：
 * - 人性值 > 75%: 无效果
 * - 人性值 50-75%: 淡淡的电路纹理
 * - 人性值 25-50%: 明显的机械线条
 * - 人性值 < 25%: 强烈的机械化外观 + 发光
 * - 破碎之神: 完全机械化 + 脉冲发光
 */
@SideOnly(Side.CLIENT)
public class LayerMechanicalOverlay implements LayerRenderer<AbstractClientPlayer> {

    private final RenderPlayer renderPlayer;

    public LayerMechanicalOverlay(RenderPlayer renderPlayer) {
        this.renderPlayer = renderPlayer;
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {

        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        float humanity = data.getHumanity();
        AscensionRoute route = data.getAscensionRoute();
        boolean isBrokenGod = route == AscensionRoute.BROKEN_GOD;

        // 人性值 > 75 且非破碎之神时不渲染
        if (humanity > 75 && !isBrokenGod) return;

        // 计算机械化程度 (0 = 无, 1 = 完全机械化)
        float mechanization;
        if (isBrokenGod) {
            mechanization = 1.0f;
        } else {
            // 75 -> 0, 0 -> 1
            mechanization = Math.min(1.0f, (75 - humanity) / 75f);
        }

        // 渲染机械叠加
        renderMechanicalOverlay(player, mechanization, isBrokenGod, ageInTicks, partialTicks,
                limbSwing, limbSwingAmount, netHeadYaw, headPitch, scale);
    }

    private void renderMechanicalOverlay(AbstractClientPlayer player, float mechanization, boolean isBrokenGod,
                                         float ageInTicks, float partialTicks, float limbSwing, float limbSwingAmount,
                                         float netHeadYaw, float headPitch, float scale) {

        GlStateManager.pushMatrix();

        // 启用混合
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();

        // 调整位置（跟随玩家模型）
        if (player.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }

        // 计算脉冲效果
        float pulse = 1.0f;
        if (isBrokenGod) {
            pulse = 0.7f + 0.3f * (float) Math.sin(ageInTicks * 0.15);
        } else if (mechanization > 0.5f) {
            pulse = 0.8f + 0.2f * (float) Math.sin(ageInTicks * 0.1);
        }

        // 基础颜色：从暗灰色过渡到青色
        float r, g, b;
        if (isBrokenGod) {
            // 破碎之神：金色/橙色机械
            r = 1.0f;
            g = 0.7f + 0.3f * pulse;
            b = 0.2f;
        } else if (mechanization > 0.6f) {
            // 高机械化：青色
            r = 0.3f;
            g = 0.8f * pulse;
            b = 1.0f * pulse;
        } else {
            // 低机械化：灰色
            r = 0.5f;
            g = 0.5f;
            b = 0.6f;
        }

        float alpha = mechanization * 0.8f * pulse;

        // 渲染电路线条
        renderCircuitLines(player, r, g, b, alpha, mechanization, ageInTicks, scale);

        // 高机械化时添加发光节点
        if (mechanization > 0.3f) {
            renderGlowingNodes(player, r, g, b, alpha * 1.5f, mechanization, ageInTicks, scale);
        }

        // 破碎之神额外效果：能量场
        if (isBrokenGod) {
            renderEnergyField(player, ageInTicks, scale);
        }

        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 渲染电路线条
     */
    private void renderCircuitLines(AbstractClientPlayer player, float r, float g, float b, float alpha,
                                    float mechanization, float ageInTicks, float scale) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.glLineWidth(1.5f + mechanization * 1.5f);

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // 身体中心线
        addLine(buffer, 0, 0.3, 0.13, 0, 0.7, 0.13, r, g, b, alpha);
        addLine(buffer, 0, 0.7, 0.13, 0, 1.0, 0.13, r, g, b, alpha * 0.8f);

        // 胸部横线
        addLine(buffer, -0.25, 0.85, 0.14, 0.25, 0.85, 0.14, r, g, b, alpha);
        addLine(buffer, -0.2, 0.65, 0.14, 0.2, 0.65, 0.14, r, g, b, alpha * 0.7f);

        // 肩部连接
        addLine(buffer, -0.25, 0.85, 0.13, -0.35, 0.85, 0, r, g, b, alpha);
        addLine(buffer, 0.25, 0.85, 0.13, 0.35, 0.85, 0, r, g, b, alpha);

        // 手臂线条
        addLine(buffer, -0.35, 0.85, 0, -0.35, 0.5, 0, r, g, b, alpha * 0.9f);
        addLine(buffer, -0.35, 0.5, 0, -0.35, 0.2, 0, r, g, b, alpha * 0.7f);
        addLine(buffer, 0.35, 0.85, 0, 0.35, 0.5, 0, r, g, b, alpha * 0.9f);
        addLine(buffer, 0.35, 0.5, 0, 0.35, 0.2, 0, r, g, b, alpha * 0.7f);

        // 腿部线条
        addLine(buffer, -0.1, 0.3, 0.12, -0.1, 0, 0.12, r, g, b, alpha * 0.8f);
        addLine(buffer, 0.1, 0.3, 0.12, 0.1, 0, 0.12, r, g, b, alpha * 0.8f);

        // 背部线条
        addLine(buffer, 0, 0.4, -0.13, 0, 0.8, -0.13, r, g, b, alpha * 0.6f);
        addLine(buffer, -0.15, 0.6, -0.14, 0.15, 0.6, -0.14, r, g, b, alpha * 0.5f);

        // 高机械化时添加更多细节
        if (mechanization > 0.5f) {
            // 头部电路
            addLine(buffer, -0.15, 1.4, 0.12, 0.15, 1.4, 0.12, r, g, b, alpha * 0.6f);
            addLine(buffer, 0, 1.3, 0.13, 0, 1.5, 0.13, r, g, b, alpha * 0.5f);

            // 对角线连接
            addLine(buffer, -0.2, 0.85, 0.13, -0.1, 0.5, 0.13, r, g, b, alpha * 0.4f);
            addLine(buffer, 0.2, 0.85, 0.13, 0.1, 0.5, 0.13, r, g, b, alpha * 0.4f);
        }

        tessellator.draw();
    }

    /**
     * 渲染发光节点
     */
    private void renderGlowingNodes(AbstractClientPlayer player, float r, float g, float b, float alpha,
                                    float mechanization, float ageInTicks, float scale) {
        GlStateManager.glPointSize(3.0f + mechanization * 4.0f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);

        // 关节节点
        float nodeAlpha = Math.min(1.0f, alpha * 1.2f);

        // 肩部
        addPoint(buffer, -0.35, 0.85, 0, r, g, b, nodeAlpha);
        addPoint(buffer, 0.35, 0.85, 0, r, g, b, nodeAlpha);

        // 肘部
        addPoint(buffer, -0.35, 0.5, 0, r, g, b, nodeAlpha * 0.8f);
        addPoint(buffer, 0.35, 0.5, 0, r, g, b, nodeAlpha * 0.8f);

        // 胸部中心
        addPoint(buffer, 0, 0.75, 0.14, r, g, b, nodeAlpha);

        // 腰部
        addPoint(buffer, 0, 0.35, 0.13, r, g, b, nodeAlpha * 0.7f);

        // 髋部
        addPoint(buffer, -0.1, 0.3, 0.12, r, g, b, nodeAlpha * 0.6f);
        addPoint(buffer, 0.1, 0.3, 0.12, r, g, b, nodeAlpha * 0.6f);

        if (mechanization > 0.6f) {
            // 头部节点
            addPoint(buffer, 0, 1.4, 0.13, r, g, b, nodeAlpha * 0.5f);
            addPoint(buffer, -0.12, 1.4, 0.1, r, g, b, nodeAlpha * 0.4f);
            addPoint(buffer, 0.12, 1.4, 0.1, r, g, b, nodeAlpha * 0.4f);
        }

        tessellator.draw();
    }

    /**
     * 破碎之神能量场效果
     */
    private void renderEnergyField(AbstractClientPlayer player, float ageInTicks, float scale) {
        GlStateManager.pushMatrix();

        // 旋转的能量环
        float rotation = (ageInTicks % 360) * 2;
        GlStateManager.translate(0, 0.6, 0);
        GlStateManager.rotate(rotation, 0, 1, 0);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.glLineWidth(1.0f);
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);

        // 绘制能量环
        float radius = 0.4f;
        float alpha = 0.3f + 0.2f * (float) Math.sin(ageInTicks * 0.2);
        int segments = 16;

        for (int i = 0; i < segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            float y = 0.05f * (float) Math.sin(angle * 3 + ageInTicks * 0.1);

            buffer.pos(x, y, z).color(1.0f, 0.8f, 0.3f, alpha).endVertex();
        }

        tessellator.draw();

        GlStateManager.popMatrix();
    }

    private void addLine(BufferBuilder buffer, double x1, double y1, double z1,
                         double x2, double y2, double z2, float r, float g, float b, float a) {
        buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
    }

    private void addPoint(BufferBuilder buffer, double x, double y, double z, float r, float g, float b, float a) {
        buffer.pos(x, y, z).color(r, g, b, a).endVertex();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }

    /**
     * 注册渲染层
     */
    public static void register() {
        try {
            Map<String, RenderPlayer> skinMap = Minecraft.getMinecraft().getRenderManager().getSkinMap();

            RenderPlayer defaultRender = skinMap.get("default");
            if (defaultRender != null) {
                defaultRender.addLayer(new LayerMechanicalOverlay(defaultRender));
            }

            RenderPlayer slimRender = skinMap.get("slim");
            if (slimRender != null) {
                slimRender.addLayer(new LayerMechanicalOverlay(slimRender));
            }

            System.out.println("[moremod] Mechanical Overlay Layer registered!");
        } catch (Throwable e) {
            System.err.println("[moremod] Failed to register Mechanical Overlay Layer: " + e.getMessage());
        }
    }
}
