package com.moremod.client.render.fx;

import com.moremod.entity.fx.EntityPlayerLaserBeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RenderPlayerLaserBeam extends Render<EntityPlayerLaserBeam> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("moremod", "textures/effects/laser_beam.png");

    public RenderPlayerLaserBeam(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0F;
    }

    @Override
    public boolean shouldRender(EntityPlayerLaserBeam entity, ICamera camera, double camX, double camY, double camZ) {
        return true;  // 始终渲染，跳过视锥检查
    }

    @Override
    public void doRender(EntityPlayerLaserBeam beam, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        if (beam.getOwner() == null) return;

        Vec3d start = beam.getStartPos();
        Vec3d end = beam.getActualEndPos();

        double sx = start.x - renderManager.viewerPosX;
        double sy = start.y - renderManager.viewerPosY;
        double sz = start.z - renderManager.viewerPosZ;
        double ex = end.x - renderManager.viewerPosX;
        double ey = end.y - renderManager.viewerPosY;
        double ez = end.z - renderManager.viewerPosZ;

        double distance = start.distanceTo(end);
        if (distance < 0.1) return;

        // LOD系统 - 根据距离调整细节
        double viewDistance = Minecraft.getMinecraft().player.getDistance(beam.getOwner());
        int segments;
        int particleChance;
        float alphaMultiplier = 1.0F;

        if (viewDistance < 30) {
            segments = 16;  // 近距离：完整细节
            particleChance = 3;
        } else if (viewDistance < 60) {
            segments = 12;  // 中距离：降低细节
            particleChance = 6;
            alphaMultiplier = 0.9F;
        } else if (viewDistance < 100) {
            segments = 8;   // 远距离：低细节
            particleChance = 10;
            alphaMultiplier = 0.8F;
        } else {
            segments = 6;   // 超远距离：最低细节
            particleChance = 20;
            alphaMultiplier = 0.7F;
        }

        Vec3d direction = end.subtract(start).normalize();
        Vec3d up = new Vec3d(0, 1, 0);
        if (Math.abs(direction.y) > 0.99) {
            up = new Vec3d(1, 0, 0);
        }
        Vec3d right = direction.crossProduct(up).normalize();
        up = right.crossProduct(direction).normalize();

        // OpenGL状态
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        bindTexture(TEXTURE);

        float time = (Minecraft.getSystemTime() % 1000L) / 1000.0F;
        float pulse = (float)(Math.sin(Minecraft.getSystemTime() * 0.003) * 0.05 + 1.0);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 外层光晕
        if (viewDistance < 100) {  // 远距离不渲染光晕
            renderBeamLayer(buffer, sx, sy, sz, ex, ey, ez, right, up,
                    1.2F * pulse, time * 0.5F, distance, segments,
                    0.6F, 0.0F, 0.8F, 0.15F * alphaMultiplier);
        }

        // 主激光束
        renderBeamLayer(buffer, sx, sy, sz, ex, ey, ez, right, up,
                0.5F * pulse, time, distance, segments,
                0.7F, 0.1F, 0.9F, 0.6F * alphaMultiplier);

        // 核心亮光
        renderBeamLayer(buffer, sx, sy, sz, ex, ey, ez, right, up,
                0.2F, time * 2, distance, Math.max(6, segments / 2),
                0.9F, 0.5F, 1.0F, 0.9F * alphaMultiplier);

        // 粒子效果（根据LOD调整）
        if (beam.world.rand.nextInt(particleChance) == 0 && viewDistance < 80) {
            spawnParticles(start, end, beam, viewDistance);
        }

        // 恢复OpenGL状态
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private void renderBeamLayer(BufferBuilder buffer,
                                 double sx, double sy, double sz,
                                 double ex, double ey, double ez,
                                 Vec3d right, Vec3d up,
                                 float width, float texOffset, double distance,
                                 int segments,
                                 float r, float g, float b, float alpha) {

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (int i = 0; i < segments; i++) {
            double angle1 = (Math.PI * 2 * i) / segments;
            double angle2 = (Math.PI * 2 * (i + 1)) / segments;

            Vec3d offset1 = right.scale(Math.cos(angle1) * width)
                    .add(up.scale(Math.sin(angle1) * width));
            Vec3d offset2 = right.scale(Math.cos(angle2) * width)
                    .add(up.scale(Math.sin(angle2) * width));

            float u1 = (float)i / segments;
            float u2 = (float)(i + 1) / segments;
            float v1 = texOffset;
            float v2 = texOffset + (float)distance * 0.1F;

            buffer.pos(sx + offset1.x, sy + offset1.y, sz + offset1.z)
                    .tex(u1, v1).color(r, g, b, alpha).endVertex();
            buffer.pos(sx + offset2.x, sy + offset2.y, sz + offset2.z)
                    .tex(u2, v1).color(r, g, b, alpha).endVertex();
            buffer.pos(ex + offset2.x, ey + offset2.y, ez + offset2.z)
                    .tex(u2, v2).color(r, g, b, alpha * 0.7F).endVertex();
            buffer.pos(ex + offset1.x, ey + offset1.y, ez + offset1.z)
                    .tex(u1, v2).color(r, g, b, alpha * 0.7F).endVertex();
        }

        Tessellator.getInstance().draw();
    }

    private void spawnParticles(Vec3d start, Vec3d end, EntityPlayerLaserBeam beam, double viewDistance) {
        if (!beam.world.isRemote) return;

        // 根据视距调整粒子数量
        int maxParticles = viewDistance < 30 ? 10 : viewDistance < 60 ? 5 : 3;
        int particleCount = Math.min(maxParticles, (int)(start.distanceTo(end) / 10) + 1);

        for (int i = 0; i < particleCount; i++) {
            double t = beam.world.rand.nextDouble();
            double px = start.x + (end.x - start.x) * t;
            double py = start.y + (end.y - start.y) * t;
            double pz = start.z + (end.z - start.z) * t;

            beam.world.spawnParticle(EnumParticleTypes.PORTAL, px, py, pz, 0, 0, 0);
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityPlayerLaserBeam entity) {
        return TEXTURE;
    }
}