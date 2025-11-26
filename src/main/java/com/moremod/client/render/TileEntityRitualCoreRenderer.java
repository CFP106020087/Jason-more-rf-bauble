package com.moremod.client.render;

import com.moremod.tile.TileEntityRitualCore;
import com.moremod.tile.TileEntityPedestal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
@SideOnly(Side.CLIENT)

public class TileEntityRitualCoreRenderer extends TileEntitySpecialRenderer<TileEntityRitualCore> {

    @Override
    public void render(TileEntityRitualCore tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        if (tile == null) return;

        // 渲染物品
        renderItems(tile, x, y, z);

        // 只在有能量时渲染激光
        if (tile.isActive() && tile.hasEnoughEnergy()) {
            renderLaserBeams(tile, x, y, z, partialTicks);
        }
    }

    private void renderItems(TileEntityRitualCore tile, double x, double y, double z) {
        ItemStack coreStack = tile.getInv().getStackInSlot(0);
        ItemStack outputStack = tile.getInv().getStackInSlot(1);

        if (!coreStack.isEmpty()) {
            renderFloatingItem(coreStack, x, y + 1.25, z, 0.5F, 1.0F);
        }

        if (!outputStack.isEmpty()) {
            renderFloatingItem(outputStack, x, y + 1.25, z, 0.5F, -0.5F);
        }
    }

    private void renderFloatingItem(ItemStack stack, double x, double y, double z, float scale, float rotationSpeed) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x + 0.5F, (float)y, (float)z + 0.5F);

        if (rotationSpeed != 0) {
            double boop = Minecraft.getSystemTime() / 800D;
            GlStateManager.translate(0D, Math.sin(boop % (2 * Math.PI)) * 0.065, 0D);
            GlStateManager.rotate((float)(boop * 40D * rotationSpeed % 360), 0, 1, 0);
        }

        if (stack.getItem() instanceof ItemBlock) {
            scale *= 1.5F;
        }
        GlStateManager.scale(scale, scale, scale);

        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.GROUND);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();

        GlStateManager.popMatrix();
    }

    private void renderLaserBeams(TileEntityRitualCore tile, double x, double y, double z, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 1.0, z + 0.5);

        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        float time = (Minecraft.getSystemTime() % 1000) / 1000.0F;
        float pulse = (float)(Math.sin(time * Math.PI * 2) * 0.5 + 0.5);

        // 渲染所有活跃基座的激光
        for (BlockPos offset : tile.getPedestalOffsets()) {
            TileEntity te = tile.getWorld().getTileEntity(tile.getPos().add(offset));
            if (te instanceof TileEntityPedestal && !((TileEntityPedestal)te).isEmpty()) {
                drawLaserBeam(buffer, tessellator, offset.getX(), 0, offset.getZ(), pulse);
            }
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();

        GlStateManager.popMatrix();
    }

    private void drawLaserBeam(BufferBuilder buffer, Tessellator tessellator,
                               double fromX, double fromY, double fromZ, float pulse) {
        float beamWidth = 0.05F + pulse * 0.02F;
        float r = 0.5F + pulse * 0.3F;
        float g = 0.0F;
        float b = 0.8F + pulse * 0.2F;
        float a = 0.7F + pulse * 0.3F;

        double length = Math.sqrt(fromX * fromX + fromZ * fromZ);
        double dx = -fromX / length;
        double dz = -fromZ / length;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        buffer.pos(fromX - dz * beamWidth, fromY, fromZ + dx * beamWidth).color(r, g, b, a).endVertex();
        buffer.pos(fromX + dz * beamWidth, fromY, fromZ - dx * beamWidth).color(r, g, b, a).endVertex();
        buffer.pos(0 + dz * beamWidth, 0, 0 - dx * beamWidth).color(r, g, b, a).endVertex();
        buffer.pos(0 - dz * beamWidth, 0, 0 + dx * beamWidth).color(r, g, b, a).endVertex();

        tessellator.draw();
    }
}