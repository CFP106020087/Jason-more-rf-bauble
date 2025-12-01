package com.moremod.dimension;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * 私人维度天空渲染器 - 时空裂缝维度 (Erika 终极优化版)
 * 特性：
 * 1. 修复了天空只渲染一半的 Bug (关闭背面剔除)
 * 2. 增加了末地传送门风格的深邃视差效果
 * 3. 优化了粒子系统 (对象池重用)
 */
@SideOnly(Side.CLIENT)
public class PersonalDimensionSkyRenderer extends IRenderHandler {

    // ===== 资源引用 =====
    private static final ResourceLocation END_PORTAL_TEXTURE = new ResourceLocation("textures/entity/end_portal.png");
    private static final ResourceLocation END_SKY_TEXTURE = new ResourceLocation("textures/environment/end_sky.png");

    // ===== 可调参数 =====
    private static final long   RIFT_SEED               = 20250831L;
    private static final float  SKY_RADIUS               = 100.0f;
    private static final int    STAR_COUNT               = 3000;
    private static final float  STAR_BRIGHTNESS          = 0.9f;

    // 裂缝形状参数
    private static final int    RIFT_SEGMENTS            = 180;
    private static final float  RIFT_WIDTH_DEG_VOID      = 8.0f;
    private static final float  RIFT_WIDTH_DEG_AURA      = 16.0f;
    private static final float  RIFT_WIDTH_DEG_DISTORT   = 24.0f;

    // 粒子效果参数
    private static final int    PARTICLE_COUNT           = 500;
    private static final float  PARTICLE_SIZE            = 0.3f;
    private static final float  PARTICLE_SPEED           = 0.5f;
    private static final float  PARTICLE_DRIFT           = 2.0f;
    private static final float  PARTICLE_BRIGHTNESS      = 0.8f;

    // 量子涨落参数
    private static final int    QUANTUM_HARMONICS        = 5;
    private static final double QUANTUM_BASE_FREQ        = 1.618;
    private static final double QUANTUM_DECAY            = 0.618;
    private static final double QUANTUM_UNCERTAINTY      = 0.1;
    private static final double PLANCK_SCALE             = 0.001;

    // 颜色方案
    private static final float[] COL_BG_TOP   = {0.03f, 0.02f, 0.05f};
    private static final float[] COL_BG_MID   = {0.06f, 0.03f, 0.10f};
    private static final float[] COL_BG_BOT   = {0.12f, 0.05f, 0.18f};
    private static final float[] COL_STAR     = {0.8f, 0.85f, 1.0f};

    // 裂缝颜色
    private static final float[] COL_ENERGY_IN  = {0.4f, 0.2f, 0.8f, 0.5f};
    private static final float[] COL_ENERGY_MID = {0.6f, 0.3f, 0.9f, 0.3f};
    private static final float[] COL_ENERGY_OUT = {0.8f, 0.5f, 1.0f, 0.15f};
    private static final float[] COL_DISTORT    = {0.5f, 0.7f, 1.0f, 0.1f};
    private static final float[] COL_LIGHTNING  = {0.9f, 0.95f, 1.0f, 0.9f};

    // 粒子颜色（紫白渐变）
    private static final float[][] COL_PARTICLES = {
            {0.9f, 0.8f, 1.0f, 0.9f},
            {0.7f, 0.5f, 1.0f, 0.7f},
            {0.5f, 0.3f, 0.9f, 0.5f},
            {1.0f, 0.9f, 1.0f, 0.8f},
            {0.6f, 0.4f, 0.95f, 0.6f}
    };

    // 动画速度
    private static final float  STAR_DRIFT_SPEED         = 0.003f;
    private static final float  ENERGY_FLOW_SPEED        = 1.0f;
    private static final float  DISTORT_WAVE_SPEED       = 0.5f;
    private static final float  LIGHTNING_CHANCE         = 0.003f;
    private static final float  LIGHTNING_DURATION       = 20f;
    private static final float  QUANTUM_FLUTTER_SPEED    = 2.0f;

    // 显示列表 ID
    private int starList = -1;
    // private int voidCoreList = -1; // 移除：因为视差效果需要动态渲染，不能用显示列表
    private int energyLayersList = -1;
    private int distortionList = -1;
    private int lightningList = -1;

    private boolean built = false;
    private float lastLightningTime = 0;

    // 缓存数据
    private QuantumState[] quantumStates = null;
    private double[] waveFunction = null;
    private RiftMesh cachedMesh = null;
    private Particle[] particles = null;

    // ==========================================
    //              主要渲染逻辑
    // ==========================================

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        if (!built) {
            buildDisplayLists();
            built = true;
        }

        final Tessellator tess = Tessellator.getInstance();
        final BufferBuilder buf = tess.getBuffer();
        float time = world.getTotalWorldTime() + partialTicks;

        // 准备全局渲染状态
        GlStateManager.disableTexture2D();
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false); // 天空通常不需要写入深度
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // 1. 渲染天空背景 (已修复：一半天空问题)
        renderDarkGradientSphere(tess, buf);

        // 2. 空间碎片（星星）
        GlStateManager.pushMatrix();
        GlStateManager.rotate(time * STAR_DRIFT_SPEED, 0.3f, 1f, 0.2f);
        float starPulse = 0.85f + 0.15f * MathHelper.sin(time * 0.02f);
        GlStateManager.color(
                COL_STAR[0] * STAR_BRIGHTNESS * starPulse,
                COL_STAR[1] * STAR_BRIGHTNESS * starPulse,
                COL_STAR[2] * STAR_BRIGHTNESS * starPulse,
                1.0f
        );
        if (starList >= 0) {
            GlStateManager.callList(starList);
        }
        GlStateManager.popMatrix();

        // 3. 空间扭曲层
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        float distortAlpha = 0.08f + 0.04f * MathHelper.sin(time * DISTORT_WAVE_SPEED * 0.1f);
        float quantumNoise = (float)(Math.sin(time * QUANTUM_FLUTTER_SPEED * 0.1f) * 0.05);
        distortAlpha += quantumNoise;

        GlStateManager.color(COL_DISTORT[0], COL_DISTORT[1], COL_DISTORT[2], Math.max(0, Math.min(1, distortAlpha)));
        GlStateManager.pushMatrix();
        GlStateManager.rotate(time * 0.01f, 0, 1, 0);
        if (distortionList >= 0) {
            GlStateManager.callList(distortionList);
        }
        GlStateManager.popMatrix();

        // 4. 能量层 (多层叠加)
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        float energyPulse = MathHelper.sin(time * ENERGY_FLOW_SPEED * 0.1f);
        float quantumModulation = (float)(calculateQuantumFluctuation(time) * 0.3);

        // 外层
        GlStateManager.pushMatrix();
        GlStateManager.rotate(time * 0.02f, 0, 1, 0);
        float outerAlpha = COL_ENERGY_OUT[3] * (0.7f + 0.3f * energyPulse) * (1.0f + quantumModulation);
        GlStateManager.color(COL_ENERGY_OUT[0], COL_ENERGY_OUT[1], COL_ENERGY_OUT[2], Math.max(0, Math.min(1, outerAlpha)));
        if (energyLayersList >= 0) GlStateManager.callList(energyLayersList);
        GlStateManager.popMatrix();

        // 中层
        GlStateManager.pushMatrix();
        GlStateManager.rotate(-time * 0.015f, 0, 1, 0);
        float midAlpha = COL_ENERGY_MID[3] * (0.6f + 0.4f * MathHelper.cos(time * ENERGY_FLOW_SPEED * 0.15f));
        GlStateManager.color(COL_ENERGY_MID[0], COL_ENERGY_MID[1], COL_ENERGY_MID[2], Math.max(0, Math.min(1, midAlpha)));
        GlStateManager.scale(0.95f, 1f, 0.95f);
        if (energyLayersList >= 0) GlStateManager.callList(energyLayersList);
        GlStateManager.popMatrix();

        // 内层
        GlStateManager.pushMatrix();
        GlStateManager.rotate(time * 0.025f, 0, 1, 0);
        float innerAlpha = COL_ENERGY_IN[3] * (0.5f + 0.5f * energyPulse);
        GlStateManager.color(COL_ENERGY_IN[0], COL_ENERGY_IN[1], COL_ENERGY_IN[2], Math.max(0, Math.min(1, innerAlpha)));
        GlStateManager.scale(0.9f, 1f, 0.9f);
        if (energyLayersList >= 0) GlStateManager.callList(energyLayersList);
        GlStateManager.popMatrix();

        // 5. 虚空核心 (✨ 全新升级：视差效果)
        // 核心必须动态渲染，不能用 DisplayList
        renderVoidCoreParallax(cachedMesh, time);

        // 6. 裂缝边缘粒子效果
        renderRiftParticles(time, world);

        // 7. 闪电效果
        handleLightning(time, world.rand);

        // 恢复状态
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableFog();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
    }

    // ==========================================
    //           渲染子方法 (修复与优化)
    // ==========================================

    /**
     * 修复：渲染渐变背景球
     * 关键修复：禁用剔除 (Cull Face)，否则只能看到一半
     */
    private void renderDarkGradientSphere(Tessellator tess, BufferBuilder buf) {
        GlStateManager.disableCull(); // 👈 关键修复
        GlStateManager.depthMask(false);

        final int latSegments = 18; // 稍微增加平滑度
        final int lonSegments = 32;

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        for (int lat = 0; lat < latSegments; lat++) {
            double lat0 = Math.PI * (-0.5 + (double) lat / latSegments);
            double lat1 = Math.PI * (-0.5 + (double) (lat + 1) / latSegments);

            for (int lon = 0; lon < lonSegments; lon++) {
                double lon0 = 2 * Math.PI * (double) lon / lonSegments;
                double lon1 = 2 * Math.PI * (double) (lon + 1) / lonSegments;

                double x0 = SKY_RADIUS * Math.cos(lat0) * Math.cos(lon0);
                double y0 = SKY_RADIUS * Math.sin(lat0);
                double z0 = SKY_RADIUS * Math.cos(lat0) * Math.sin(lon0);

                double x1 = SKY_RADIUS * Math.cos(lat0) * Math.cos(lon1);
                double y1 = SKY_RADIUS * Math.sin(lat0);
                double z1 = SKY_RADIUS * Math.cos(lat0) * Math.sin(lon1);

                double x2 = SKY_RADIUS * Math.cos(lat1) * Math.cos(lon1);
                double y2 = SKY_RADIUS * Math.sin(lat1);
                double z2 = SKY_RADIUS * Math.cos(lat1) * Math.sin(lon1);

                double x3 = SKY_RADIUS * Math.cos(lat1) * Math.cos(lon0);
                double y3 = SKY_RADIUS * Math.sin(lat1);
                double z3 = SKY_RADIUS * Math.cos(lat1) * Math.sin(lon0);

                float[] c0 = getGradientColor(y0 / SKY_RADIUS);
                float[] c1 = getGradientColor(y1 / SKY_RADIUS);
                float[] c2 = getGradientColor(y2 / SKY_RADIUS);
                float[] c3 = getGradientColor(y3 / SKY_RADIUS);

                buf.pos(x0, y0, z0).color(c0[0], c0[1], c0[2], 1f).endVertex();
                buf.pos(x1, y1, z1).color(c1[0], c1[1], c1[2], 1f).endVertex();
                buf.pos(x2, y2, z2).color(c2[0], c2[1], c2[2], 1f).endVertex();
                buf.pos(x3, y3, z3).color(c3[0], c3[1], c3[2], 1f).endVertex();
            }
        }
        tess.draw();
        GlStateManager.enableCull(); // 恢复默认状态
    }

    /**
     * 升级：虚空核心视差渲染
     * 使用末地传送门材质产生深邃感
     */
    private void renderVoidCoreParallax(RiftMesh mesh, float time) {
        if (mesh == null) return;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        // --- 第一层：纯黑底色 (遮挡背后的星星) ---
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < mesh.centerLine.length; i++) {
            Vec3d L = mesh.voidLeft[i].scale(SKY_RADIUS * 1.05);
            Vec3d R = mesh.voidRight[i].scale(SKY_RADIUS * 1.05);
            buf.pos(L.x, L.y, L.z).color(0f, 0f, 0f, 1f).endVertex();
            buf.pos(R.x, R.y, R.z).color(0f, 0f, 0f, 1f).endVertex();
        }
        tess.draw();

        // --- 第二层：末地视差效果 ---
        GlStateManager.enableTexture2D();
        Minecraft.getMinecraft().getTextureManager().bindTexture(END_PORTAL_TEXTURE);

        // 设置纹理矩阵
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.pushMatrix();
        float t = (float)(Minecraft.getSystemTime() % 100000L) / 100000.0F;
        GlStateManager.translate(t, t, t);
        GlStateManager.scale(16.0F, 16.0F, 16.0F);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);

        GlStateManager.blendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE); // 发光混合模式

        Random innerRand = new Random(31100L);

        // 多层叠加
        for(int layer = 0; layer < 4; layer++) {
            GlStateManager.pushMatrix();
            float scale = 1.0f - layer * 0.15f;
            GlStateManager.scale(scale, scale, scale);

            float r = 0.2F + innerRand.nextFloat() * 0.3F; // 暗紫
            float g = 0.1F + innerRand.nextFloat() * 0.1F;
            float b = 0.4F + innerRand.nextFloat() * 0.4F; // 深蓝
            float a = 0.5F / (layer + 1);

            buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < mesh.centerLine.length; i++) {
                Vec3d L = mesh.voidLeft[i].scale(SKY_RADIUS * 0.9);
                Vec3d R = mesh.voidRight[i].scale(SKY_RADIUS * 0.9);
                buf.pos(L.x, L.y, L.z).color(r, g, b, a).endVertex();
                buf.pos(R.x, R.y, R.z).color(r, g, b, a).endVertex();
            }
            tess.draw();
            GlStateManager.popMatrix();

            // 旋转纹理矩阵
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.rotate(15.0f, 0, 0, 1);
            GlStateManager.scale(0.8f, 0.8f, 0.8f);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        }

        // 还原
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);

        GlStateManager.disableTexture2D();
        GlStateManager.enableCull();
    }

    /**
     * 优化：粒子渲染与更新
     * 使用对象复用，避免 new Particle()
     */
    private void renderRiftParticles(float time, WorldClient world) {
        if (particles == null || cachedMesh == null) return;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        Random rng = world.rand;

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        Vec3d camPos = Minecraft.getMinecraft().player.getPositionEyes(1.0f);

        for (Particle p : particles) {
            // 更新逻辑
            p.update(1.0f);

            // 复活逻辑 (优化：不用 new)
            if (!p.isAlive()) {
                Vec3d newPos = getRandomRiftEdgePosition(rng);
                p.reset(newPos, rng);
            }

            // 渲染逻辑
            float[] color = COL_PARTICLES[p.colorIndex];
            float alpha = p.getAlpha();
            float pulse = 1.0f + 0.3f * MathHelper.sin(time * 0.1f + p.phase);
            float size = p.size * pulse;

            Vec3d pos = p.position.scale(SKY_RADIUS);

            // Billboard 计算
            Vec3d toCam = camPos.subtract(pos).normalize();
            Vec3d right = toCam.crossProduct(new Vec3d(0, 1, 0)).normalize().scale(size);
            Vec3d up = toCam.crossProduct(right).normalize().scale(size);

            buf.pos(pos.x - right.x - up.x, pos.y - right.y - up.y, pos.z - right.z - up.z).color(color[0], color[1], color[2], alpha).endVertex();
            buf.pos(pos.x + right.x - up.x, pos.y + right.y - up.y, pos.z + right.z - up.z).color(color[0], color[1], color[2], alpha).endVertex();
            buf.pos(pos.x + right.x + up.x, pos.y + right.y + up.y, pos.z + right.z + up.z).color(color[0], color[1], color[2], alpha).endVertex();
            buf.pos(pos.x - right.x + up.x, pos.y - right.y + up.y, pos.z - right.z + up.z).color(color[0], color[1], color[2], alpha).endVertex();
        }

        tess.draw();
        GlStateManager.enableCull();
    }

    private void handleLightning(float time, Random rand) {
        if (rand.nextFloat() < LIGHTNING_CHANCE || Math.abs(time - lastLightningTime) < LIGHTNING_DURATION) {
            if (Math.abs(time - lastLightningTime) > LIGHTNING_DURATION * 2) {
                lastLightningTime = time;
                rebuildLightning();
            }
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            float lightningBright = 1f - (Math.abs(time - lastLightningTime) / LIGHTNING_DURATION);
            lightningBright = Math.max(0, lightningBright);
            if (lightningBright > 0 && lightningList >= 0) {
                GlStateManager.color(
                        COL_LIGHTNING[0],
                        COL_LIGHTNING[1],
                        COL_LIGHTNING[2],
                        COL_LIGHTNING[3] * lightningBright * lightningBright
                );
                GlStateManager.callList(lightningList);
            }
        }
    }

    // ==========================================
    //           初始化与辅助方法
    // ==========================================

    private void buildDisplayLists() {
        Random rng = new Random(RIFT_SEED);
        initializeQuantumStates(rng);

        // 确保网格被构建
        cachedMesh = buildQuantumRiftMesh(rng);

        // 初始化粒子
        initializeParticles(rng);

        starList = GLAllocation.generateDisplayLists(1);
        energyLayersList = GLAllocation.generateDisplayLists(1);
        distortionList = GLAllocation.generateDisplayLists(1);
        lightningList = GLAllocation.generateDisplayLists(1);

        GlStateManager.glNewList(starList, GL11.GL_COMPILE);
        renderEnhancedStars();
        GlStateManager.glEndList();

        GlStateManager.glNewList(energyLayersList, GL11.GL_COMPILE);
        renderEnergyLayer(cachedMesh);
        GlStateManager.glEndList();

        GlStateManager.glNewList(distortionList, GL11.GL_COMPILE);
        renderDistortionLayer(cachedMesh);
        GlStateManager.glEndList();

        rebuildLightning();
    }

    private void initializeParticles(Random rng) {
        particles = new Particle[PARTICLE_COUNT];
        if (cachedMesh == null) cachedMesh = buildQuantumRiftMesh(rng);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Vec3d pos = getRandomRiftEdgePosition(rng);
            particles[i] = new Particle(pos, rng);
            particles[i].life = rng.nextFloat() * particles[i].maxLife;
        }
    }

    private Vec3d getRandomRiftEdgePosition(Random rng) {
        if (cachedMesh == null) return new Vec3d(0, 0, 0);
        int index = rng.nextInt(cachedMesh.centerLine.length);
        Vec3d edgePos = rng.nextBoolean() ? cachedMesh.energyLeft[index] : cachedMesh.energyRight[index];
        double offset = PARTICLE_DRIFT * 0.01;
        return edgePos.add((rng.nextDouble() - 0.5) * offset, (rng.nextDouble() - 0.5) * offset, (rng.nextDouble() - 0.5) * offset).normalize();
    }

    private void renderEnhancedStars() {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        Random rand = new Random(10842L);
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        for (int i = 0; i < STAR_COUNT; ++i) {
            double theta = rand.nextFloat() * Math.PI * 2;
            double phi = Math.acos(2 * rand.nextFloat() - 1);
            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.cos(phi);
            double z = Math.sin(phi) * Math.sin(theta);

            if (Math.abs(x) < 0.15 && Math.abs(y) < 0.3) continue; // 避开裂缝

            double radius = SKY_RADIUS * (0.85 + rand.nextFloat() * 0.15);
            double X = x * radius, Y = y * radius, Z = z * radius;
            double size = 0.1 + rand.nextFloat() * 0.4;
            if (rand.nextFloat() < 0.1) size *= 1.5;

            if (rand.nextFloat() < 0.3) { // 十字星
                double s = size;
                buf.pos(X - s * 2, Y, Z).endVertex(); buf.pos(X + s * 2, Y, Z).endVertex();
                buf.pos(X + s * 2, Y + s * 0.3, Z).endVertex(); buf.pos(X - s * 2, Y + s * 0.3, Z).endVertex();
                buf.pos(X, Y - s * 2, Z).endVertex(); buf.pos(X + s * 0.3, Y - s * 2, Z).endVertex();
                buf.pos(X + s * 0.3, Y + s * 2, Z).endVertex(); buf.pos(X, Y + s * 2, Z).endVertex();
            } else { // 方形星
                buf.pos(X - size, Y - size, Z).endVertex(); buf.pos(X + size, Y - size, Z).endVertex();
                buf.pos(X + size, Y + size, Z).endVertex(); buf.pos(X - size, Y + size, Z).endVertex();
            }
        }
        tess.draw();
    }

    private void renderEnergyLayer(RiftMesh mesh) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        GlStateManager.disableCull();
        buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < mesh.centerLine.length; i++) {
            Vec3d L = mesh.energyLeft[i].scale(SKY_RADIUS);
            Vec3d R = mesh.energyRight[i].scale(SKY_RADIUS);
            float t = i / (float)(mesh.centerLine.length - 1);
            float pulse = (float)(Math.sin(t * Math.PI * 6) * 0.3 + 0.7);
            float alpha = (1.0f - Math.abs(t - 0.5f) * 0.4f) * pulse * 0.4f;
            buf.pos(L.x, L.y, L.z).color(1f, 1f, 1f, alpha).endVertex();
            buf.pos(R.x, R.y, R.z).color(1f, 1f, 1f, alpha).endVertex();
        }
        tess.draw();
        GlStateManager.enableCull();
    }

    private void renderDistortionLayer(RiftMesh mesh) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        GlStateManager.disableCull();
        for (int pass = 0; pass < 2; pass++) {
            buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < mesh.centerLine.length; i++) {
                float offset = pass * 0.05f;
                Vec3d L = mesh.distortLeft[i].scale(SKY_RADIUS * (1 + offset));
                Vec3d R = mesh.distortRight[i].scale(SKY_RADIUS * (1 + offset));
                float t = i / (float)(mesh.centerLine.length - 1);
                float wave = (float)Math.sin(t * Math.PI * 8 + pass * Math.PI / 2);
                float alpha = (0.2f - pass * 0.08f) * (0.5f + wave * 0.5f);
                buf.pos(L.x, L.y, L.z).color(1f, 1f, 1f, alpha).endVertex();
                buf.pos(R.x, R.y, R.z).color(1f, 1f, 1f, alpha).endVertex();
            }
            tess.draw();
        }
        GlStateManager.enableCull();
    }

    private void rebuildLightning() {
        if (lightningList < 0) lightningList = GLAllocation.generateDisplayLists(1);
        GlStateManager.glNewList(lightningList, GL11.GL_COMPILE);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        Random rng = new Random(System.currentTimeMillis());
        GlStateManager.disableCull();

        for (int bolt = 0; bolt < 2 + rng.nextInt(2); bolt++) {
            buf.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            double startAngle = rng.nextDouble() * Math.PI * 2;
            double endAngle = startAngle + (rng.nextDouble() - 0.5) * Math.PI * 0.5;
            int segments = 15 + rng.nextInt(10);
            for (int i = 0; i <= segments; i++) {
                double t = i / (double)segments;
                double angle = startAngle + (endAngle - startAngle) * t;
                double jitter = (rng.nextDouble() - 0.5) * 0.1;
                double height = 0.2 + (rng.nextDouble() - 0.5) * 0.4 + jitter;
                double x = Math.cos(angle) * SKY_RADIUS * 0.8;
                double y = height * SKY_RADIUS * 0.4;
                double z = Math.sin(angle) * SKY_RADIUS * 0.8;
                float brightness = 1.0f - (float)t * 0.2f;
                buf.pos(x, y, z).color(brightness, brightness, brightness, 1f).endVertex();
            }
            tess.draw();
        }
        GlStateManager.enableCull();
        GlStateManager.glEndList();
    }

    // ==========================================
    //           数学与结构定义
    // ==========================================

    private float[] getGradientColor(double y) {
        if (y > 0.5) return COL_BG_TOP;
        else if (y > 0) {
            float t = (float)(y * 2);
            return new float[] { COL_BG_MID[0]*(1-t)+COL_BG_TOP[0]*t, COL_BG_MID[1]*(1-t)+COL_BG_TOP[1]*t, COL_BG_MID[2]*(1-t)+COL_BG_TOP[2]*t };
        } else {
            float t = (float)((y + 1) / 2);
            return new float[] { COL_BG_BOT[0]*(1-t)+COL_BG_MID[0]*t, COL_BG_BOT[1]*(1-t)+COL_BG_MID[1]*t, COL_BG_BOT[2]*(1-t)+COL_BG_MID[2]*t };
        }
    }

    private RiftMesh buildQuantumRiftMesh(Random rng) {
        int n = RIFT_SEGMENTS + 1;
        RiftMesh mesh = new RiftMesh(n);
        for (int i = 0; i < n; i++) {
            double t = i / (double)(n - 1);
            double angle = t * Math.PI * 2;
            Vec3d basePos = new Vec3d(Math.cos(angle), 0, Math.sin(angle) * 0.3);

            // 量子涨落计算
            double waveFn = waveFunction[i] * QUANTUM_UNCERTAINTY;
            double harm = 0;
            for (int h = 0; h < QUANTUM_HARMONICS; h++) {
                harm += Math.pow(QUANTUM_DECAY, h) * Math.sin(angle * QUANTUM_BASE_FREQ * Math.pow(1.618, h) + quantumStates[i].phase);
            }
            Vec3d center = new Vec3d(basePos.x, waveFn + harm * 0.1, basePos.z + harm * 0.05).normalize();
            mesh.centerLine[i] = center;

            // 构建各个带
            Vec3d up = new Vec3d(0, 1, 0);
            Vec3d right = center.crossProduct(up).normalize();
            double widthFactor = (1.0 - Math.abs(t - 0.5) * 0.3) * (1.0 + Math.abs(waveFn));

            double wVoid = Math.toRadians(RIFT_WIDTH_DEG_VOID * widthFactor);
            double wAura = Math.toRadians(RIFT_WIDTH_DEG_AURA * widthFactor);
            double wDist = Math.toRadians(RIFT_WIDTH_DEG_DISTORT * widthFactor);

            mesh.voidLeft[i] = rotateAroundAxis(center, right, wVoid).normalize();
            mesh.voidRight[i] = rotateAroundAxis(center, right, -wVoid).normalize();
            mesh.energyLeft[i] = rotateAroundAxis(center, right, wAura).normalize();
            mesh.energyRight[i] = rotateAroundAxis(center, right, -wAura).normalize();
            mesh.distortLeft[i] = rotateAroundAxis(center, right, wDist).normalize();
            mesh.distortRight[i] = rotateAroundAxis(center, right, -wDist).normalize();
        }
        smoothMesh(mesh, 1);
        return mesh;
    }

    private void smoothMesh(RiftMesh mesh, int iterations) {
        for (int iter = 0; iter < iterations; iter++) {
            Vec3d[] smoothed = new Vec3d[mesh.centerLine.length];
            for (int i = 0; i < mesh.centerLine.length; i++) {
                Vec3d prev = mesh.centerLine[(i - 1 + mesh.centerLine.length) % mesh.centerLine.length];
                Vec3d curr = mesh.centerLine[i];
                Vec3d next = mesh.centerLine[(i + 1) % mesh.centerLine.length];
                smoothed[i] = prev.scale(0.2).add(curr.scale(0.6)).add(next.scale(0.2)).normalize();
            }
            System.arraycopy(smoothed, 0, mesh.centerLine, 0, mesh.centerLine.length);
        }
    }

    private Vec3d rotateAroundAxis(Vec3d v, Vec3d axis, double rad) {
        axis = axis.normalize();
        double c = Math.cos(rad), s = Math.sin(rad);
        double dot = v.dotProduct(axis);
        Vec3d cross = axis.crossProduct(v);
        return v.scale(c).add(axis.scale(dot * (1 - c))).add(cross.scale(s));
    }

    private void initializeQuantumStates(Random rng) {
        int stateCount = RIFT_SEGMENTS + 1;
        quantumStates = new QuantumState[stateCount];
        waveFunction = new double[stateCount];
        for (int i = 0; i < stateCount; i++) {
            quantumStates[i] = new QuantumState(rng);
            waveFunction[i] = rng.nextGaussian() * 0.5;
        }
        double sum = 0; for (double v : waveFunction) sum += v*v;
        if (sum > 0) { double norm = Math.sqrt(sum); for (int i = 0; i < waveFunction.length; i++) waveFunction[i] /= norm; }
    }

    private double calculateQuantumFluctuation(float time) {
        double fluctuation = 0;
        for (int n = 0; n < QUANTUM_HARMONICS; n++) {
            fluctuation += Math.pow(QUANTUM_DECAY, n) * Math.sin(QUANTUM_BASE_FREQ * Math.pow(1.5, n) * time * 0.01 + n * Math.PI / QUANTUM_HARMONICS);
        }
        return fluctuation * QUANTUM_UNCERTAINTY;
    }

    // ==========================================
    //           内部类
    // ==========================================

    private static class QuantumState {
        double phase;
        QuantumState(Random rng) { phase = rng.nextDouble() * Math.PI * 2; }
    }

    private static class RiftMesh {
        final Vec3d[] centerLine, voidLeft, voidRight, energyLeft, energyRight, distortLeft, distortRight;
        RiftMesh(int n) {
            centerLine = new Vec3d[n]; voidLeft = new Vec3d[n]; voidRight = new Vec3d[n];
            energyLeft = new Vec3d[n]; energyRight = new Vec3d[n]; distortLeft = new Vec3d[n]; distortRight = new Vec3d[n];
        }
    }

    private static class Particle {
        Vec3d position;
        Vec3d velocity;
        float size, brightness, life, maxLife, phase;
        int colorIndex;

        Particle(Vec3d pos, Random rng) { reset(pos, rng); }

        void reset(Vec3d pos, Random rng) {
            position = pos;
            velocity = new Vec3d((rng.nextDouble()-0.5)*0.02, (rng.nextDouble()-0.5)*0.02, (rng.nextDouble()-0.5)*0.02);
            size = PARTICLE_SIZE * (0.5f + rng.nextFloat());
            brightness = PARTICLE_BRIGHTNESS * (0.7f + rng.nextFloat() * 0.3f);
            maxLife = 100 + rng.nextFloat() * 100;
            life = maxLife;
            colorIndex = rng.nextInt(COL_PARTICLES.length);
            phase = rng.nextFloat() * (float)Math.PI * 2;
        }

        void update(float deltaTime) {
            position = position.add(velocity.scale(deltaTime * PARTICLE_SPEED));
            position = position.add(new Vec3d(Math.sin(phase)*0.001, Math.cos(phase*1.3)*0.001, Math.sin(phase*0.7)*0.001));
            life -= deltaTime;
            phase += deltaTime * 0.1f;
        }

        boolean isAlive() { return life > 0; }
        float getAlpha() { return (life / maxLife) * brightness; }
    }
}