package com.moremod.client.render;

import com.moremod.item.ItemExplorerCompass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * 箱子高亮渲染器
 * 当玩家装备探险者罗盘且解锁箱子高亮功能时，渲染附近箱子的发光边框
 */
@Mod.EventBusSubscriber(Side.CLIENT)
@SideOnly(Side.CLIENT)
public class CompassChestRenderer {

    private static final int HIGHLIGHT_RADIUS = 32; // 高亮半径
    private static final float ANIMATION_SPEED = 0.05f; // 动画速度

    // ===== 性能优化：缓存箱子列表 =====
    private static List<BlockPos> cachedChests = new ArrayList<>();
    private static long lastScanTime = 0;
    private static final long SCAN_INTERVAL = 1000; // 1秒扫描一次，而不是每帧
    private static BlockPos lastPlayerPos = BlockPos.ORIGIN;

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        if (player == null) return;

        // 获取装备的罗盘
        ItemStack compass = ItemExplorerCompass.getEquippedCompass(player);
        if (compass.isEmpty()) return;

        // 检查是否解锁箱子高亮功能
        if (!ItemExplorerCompass.getHasChestHighlight(compass)) return;

        // ===== 性能优化：只在需要时重新扫描 =====
        long currentTime = System.currentTimeMillis();
        BlockPos currentPos = player.getPosition();

        // 玩家移动超过10格或时间间隔到了，才重新扫描
        boolean shouldRescan = currentTime - lastScanTime > SCAN_INTERVAL ||
                currentPos.getDistance(lastPlayerPos.getX(), lastPlayerPos.getY(), lastPlayerPos.getZ()) > 10;

        if (shouldRescan) {
            cachedChests = ItemExplorerCompass.getNearbyChests(player, HIGHLIGHT_RADIUS);
            lastScanTime = currentTime;
            lastPlayerPos = currentPos;
        }

        if (cachedChests.isEmpty()) return;

        // 设置渲染状态
        setupRenderState(player, event.getPartialTicks());

        // 渲染每个箱子的边框
        for (BlockPos chestPos : cachedChests) {
            renderChestHighlight(chestPos, player, event.getPartialTicks());
        }

        // 恢复渲染状态
        resetRenderState();
    }

    /**
     * 设置渲染状态
     */
    private static void setupRenderState(EntityPlayer player, float partialTicks) {
        // 获取玩家视角偏移
        double viewX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double viewY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double viewZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-viewX, -viewY, -viewZ);

        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.glLineWidth(2.0f);
    }

    /**
     * 恢复渲染状态
     */
    private static void resetRenderState() {
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 渲染单个箱子的高亮效果
     */
    private static void renderChestHighlight(BlockPos pos, EntityPlayer player, float partialTicks) {
        // 计算动画
        long time = System.currentTimeMillis();
        float pulse = (float) (Math.sin(time * ANIMATION_SPEED) * 0.5 + 0.5); // 0.0 - 1.0

        // 计算距离衰减（越远越暗）
        double distance = Math.sqrt(player.getDistanceSq(pos));
        float distanceFade = Math.max(0.2f, 1.0f - (float) (distance / HIGHLIGHT_RADIUS));

        // 颜色：金黄色，带脉动效果
        float alpha = 0.3f + pulse * 0.4f * distanceFade;
        float r = 1.0f;
        float g = 0.84f;
        float b = 0.0f;

        // 渲染发光边框
        AxisAlignedBB box = new AxisAlignedBB(pos).grow(0.002);
        renderBox(box, r, g, b, alpha);

        // 渲染填充（更透明）
        renderFilledBox(box, r, g, b, alpha * 0.15f);
    }

    /**
     * 渲染线框
     */
    private static void renderBox(AxisAlignedBB box, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // 底面
        buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();

        buffer.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();

        buffer.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();

        buffer.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();

        // 顶面
        buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();

        buffer.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();

        buffer.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();

        buffer.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();

        // 竖边
        buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();

        buffer.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();

        buffer.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();

        buffer.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();

        tessellator.draw();
    }

    /**
     * 渲染填充
     */
    private static void renderFilledBox(AxisAlignedBB box, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        // 下面
        buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();

        // 上面
        buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();

        // 北面
        buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();

        // 南面
        buffer.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();

        // 西面
        buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();

        // 东面
        buffer.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();

        tessellator.draw();
    }
}
