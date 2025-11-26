package com.moremod.client.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class TestSwordRenderer extends TileEntityItemStackRenderer {
    
    private static int callCount = 0;
    
    @Override
    public void renderByItem(ItemStack stack) {
        callCount++;
        
        // 每10次調用輸出一次，確認被調用
        if (callCount % 10 == 0) {
            System.out.println("[TEST] renderByItem called! Count: " + callCount);
        }
        
        // 保存狀態
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        
        // 完全重置所有狀態
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        
        // 設置明亮的顏色
        GlStateManager.color(1.0F, 0.0F, 1.0F, 1.0F); // 亮紫色
        
        // 不做任何變換，直接在原點畫一個大方塊
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.getBuffer();
        
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        
        // 畫一個很大的方塊（從 -1 到 1）
        // 前面
        buffer.pos(-1, -1, -1).endVertex();
        buffer.pos(1, -1, -1).endVertex();
        buffer.pos(1, 1, -1).endVertex();
        buffer.pos(-1, 1, -1).endVertex();
        
        // 後面
        buffer.pos(-1, -1, 1).endVertex();
        buffer.pos(-1, 1, 1).endVertex();
        buffer.pos(1, 1, 1).endVertex();
        buffer.pos(1, -1, 1).endVertex();
        
        // 上面
        buffer.pos(-1, 1, -1).endVertex();
        buffer.pos(1, 1, -1).endVertex();
        buffer.pos(1, 1, 1).endVertex();
        buffer.pos(-1, 1, 1).endVertex();
        
        // 下面
        buffer.pos(-1, -1, -1).endVertex();
        buffer.pos(-1, -1, 1).endVertex();
        buffer.pos(1, -1, 1).endVertex();
        buffer.pos(1, -1, -1).endVertex();
        
        // 左面
        buffer.pos(-1, -1, -1).endVertex();
        buffer.pos(-1, 1, -1).endVertex();
        buffer.pos(-1, 1, 1).endVertex();
        buffer.pos(-1, -1, 1).endVertex();
        
        // 右面
        buffer.pos(1, -1, -1).endVertex();
        buffer.pos(1, -1, 1).endVertex();
        buffer.pos(1, 1, 1).endVertex();
        buffer.pos(1, 1, -1).endVertex();
        
        tess.draw();
        
        // 恢復狀態
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
        
        System.out.println("[TEST] Render complete - should see PURPLE box!");
    }
}