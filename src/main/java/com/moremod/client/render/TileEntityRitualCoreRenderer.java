package com.moremod.client.render;

import com.moremod.tile.TileEntityRitualCore;
import com.moremod.tile.TileEntityPedestal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class TileEntityRitualCoreRenderer extends TileEntitySpecialRenderer<TileEntityRitualCore> {

    @Override
    public void render(TileEntityRitualCore tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (tile == null) return;

        // 1. 渲染懸浮物品 (包含核心與產物)
        renderFloatingItems(tile, x, y, z, partialTicks);

        // 2. 渲染能量雷射 (僅在激活且有能量時)
        if (tile.isActive()) {
            renderRitualBeams(tile, x, y, z, partialTicks);
        }
    }

    private void renderFloatingItems(TileEntityRitualCore tile, double x, double y, double z, float partialTicks) {
        ItemStack input = tile.getInv().getStackInSlot(0);
        ItemStack output = tile.getInv().getStackInSlot(1);

        // 如果沒有物品，直接跳過
        if (input.isEmpty() && output.isEmpty()) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 1.25, z + 0.5);

        // 使用 TileEntity 內的變量進行平滑插值旋轉 (解決 TPS 低落時的卡頓)
        float currentRot = tile.clientRotation;
        float lastRot = tile.lastClientRotation;
        float smoothRot = lastRot + (currentRot - lastRot) * partialTicks;

        // 浮動波形
        double time = Minecraft.getSystemTime() / 800D;
        double bob = Math.sin(time % (2 * Math.PI)) * 0.05;
        GlStateManager.translate(0, bob, 0);

        // 渲染輸入物品 (核心)
        if (!input.isEmpty()) {
            GlStateManager.pushMatrix();
            GlStateManager.rotate(smoothRot, 0, 1, 0);
            renderSingleItem(input, 0.5F);
            GlStateManager.popMatrix();
        }

        // 渲染輸出物品 (如果有，反向旋轉並稍微縮小)
        if (!output.isEmpty()) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0.25, 0); // 輸出物品稍微高一點
            GlStateManager.rotate(-smoothRot * 1.5F, 0, 1, 0); // 反向快速旋轉
            renderSingleItem(output, 0.4F);
            GlStateManager.popMatrix();
        }

        GlStateManager.popMatrix();
    }

    private void renderSingleItem(ItemStack stack, float scale) {
        if (stack.getItem() instanceof ItemBlock) {
            scale *= 1.2F; // 方塊通常看起來比較小，放大一點
        }
        GlStateManager.scale(scale, scale, scale);

        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
        RenderHelper.disableStandardItemLighting();
    }

    private void renderRitualBeams(TileEntityRitualCore tile, double x, double y, double z, float partialTicks) {
        // --- 視覺參數計算 ---
        // 使用系統時間確保動畫流暢，不受 TPS 影響
        float time = (Minecraft.getSystemTime() % 2400) / 2400.0F;
        // 脈衝函數：0.0 ~ 1.0，用於控制顏色混合和寬度
        float pulse = (float)(Math.sin(time * Math.PI * 4) * 0.5 + 0.5);

        // 顏色定義：Pale Electrum (金) & Causal Cyan (青)
        // 當 pulse 高時偏向金色，低時偏向青色
        float r = 0.0F + pulse * 0.8F;  // 青色無紅 -> 金色高紅
        float g = 0.8F + pulse * 0.1F;  // 始終保持高綠
        float b = 0.9F - pulse * 0.5F;  // 青色高藍 -> 金色低藍
        float a = 0.4F + pulse * 0.3F;  // 透明度波動

        // --- GL 狀態設置 (極致優化) ---
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 1.25 + (Math.sin(time * 10) * 0.02), z + 0.5); // 起點：核心懸浮物中心

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        // Additive Blending (疊加發光模式)
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.disableCull(); // 雙面渲染
        GlStateManager.depthMask(false); // 不寫入深度，避免遮擋

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 開始批次繪製 - 使用 GL_QUADS 繪製十字交叉平面
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        // 遍歷所有可能的基座位置
        for (BlockPos offset : tile.getPedestalOffsets()) {
            // 快速檢查：只有當該位置真的有基座且有物品時才畫線
            // 使用 getWorld().getTileEntity 是安全的，因為我們只檢查 8 個位置
            TileEntity targetTe = tile.getWorld().getTileEntity(tile.getPos().add(offset));
            if (targetTe instanceof TileEntityPedestal && !((TileEntityPedestal) targetTe).isEmpty()) {

                // 終點計算 (相對於核心中心的偏移)
                // 基座高度假設 +0.8 (物品懸浮位置) - 1.25 (核心起點高度)
                double destX = offset.getX();
                double destY = offset.getY() + 0.8D - 1.25D;
                double destZ = offset.getZ();

                addLaserQuad(buffer, destX, destY, destZ, pulse, r, g, b, a);
            }
        }

        tessellator.draw();

        // 恢復 GL 狀態
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA); // 恢復默認混合
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private void addLaserQuad(BufferBuilder buffer, double dx, double dy, double dz, float pulse, float r, float g, float b, float a) {
        // 雷射寬度隨脈衝變化
        double width = 0.05D + pulse * 0.03D;

        // 幾何計算：十字交叉平面 (Crossed Planes)
        // 這樣無論從哪個角度看，雷射都有「體積感」

        // 平面 1：水平展開 (XZ平面寬度)
        // 注意：這裡簡化了法線計算，假設雷射不會垂直向上射
        // 對於水平輻射狀的儀式結構，這是最高效的算法

        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001) return; // 防止除以零

        // 計算垂直於路徑的水平向量
        double pX = -dz / length * width;
        double pZ = dx / length * width;

        // 繪製水平切面 (Horizontal Plane)
        buffer.pos(dx + pX, dy, dz + pZ).color(r, g, b, a).endVertex();
        buffer.pos(dx - pX, dy, dz - pZ).color(r, g, b, a).endVertex();
        buffer.pos(-pX, 0, -pZ).color(r, g, b, a).endVertex();
        buffer.pos(pX, 0, pZ).color(r, g, b, a).endVertex();

        // 繪製垂直切面 (Vertical Plane)
        // 簡單地在 Y 軸上展開
        buffer.pos(dx, dy + width, dz).color(r, g, b, a).endVertex();
        buffer.pos(dx, dy - width, dz).color(r, g, b, a).endVertex();
        buffer.pos(0, -width, 0).color(r, g, b, a).endVertex();
        buffer.pos(0, width, 0).color(r, g, b, a).endVertex();

        // 可選：核心光暈 (在起點加一個小方塊增強發光感)
        float coreA = a * 0.5F;
        double coreSize = width * 2.0;
        buffer.pos(coreSize, coreSize, 0).color(r, g, b, coreA).endVertex();
        buffer.pos(-coreSize, coreSize, 0).color(r, g, b, coreA).endVertex();
        buffer.pos(-coreSize, -coreSize, 0).color(r, g, b, coreA).endVertex();
        buffer.pos(coreSize, -coreSize, 0).color(r, g, b, coreA).endVertex();
    }
}