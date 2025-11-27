package com.moremod.dimension;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * 私人维度天空渲染器 - 时空裂缝维度
 * 量子涨落版本 + 裂缝粒子效果
 */
@SideOnly(Side.CLIENT)
public class PersonalDimensionSkyRenderer extends IRenderHandler {

    // ===== 可调参数 =====
    private static final long   RIFT_SEED               = 20250831L;
    private static final float  SKY_RADIUS               = 100.0f;
    private static final int    STAR_COUNT               = 3000;    // 增加星星数量
    private static final float  STAR_BRIGHTNESS          = 0.9f;    // 提高星星亮度

    // 裂缝形状参数
    private static final int    RIFT_SEGMENTS            = 180;
    private static final float  RIFT_WIDTH_DEG_VOID      = 8.0f;
    private static final float  RIFT_WIDTH_DEG_AURA      = 16.0f;
    private static final float  RIFT_WIDTH_DEG_DISTORT   = 24.0f;

    // 粒子效果参数
    private static final int    PARTICLE_COUNT           = 500;     // 粒子数量
    private static final float  PARTICLE_SIZE            = 0.3f;    // 粒子基础大小
    private static final float  PARTICLE_SPEED           = 0.5f;    // 粒子运动速度
    private static final float  PARTICLE_DRIFT           = 2.0f;    // 粒子漂移范围
    private static final float  PARTICLE_BRIGHTNESS      = 0.8f;    // 粒子亮度

    // 量子涨落参数
    private static final int    QUANTUM_HARMONICS        = 5;
    private static final double QUANTUM_BASE_FREQ        = 1.618;
    private static final double QUANTUM_DECAY            = 0.618;
    private static final double QUANTUM_UNCERTAINTY      = 0.1;
    private static final double QUANTUM_COLLAPSE_PROB    = 0.005;
    private static final double QUANTUM_ENTANGLEMENT     = 0.2;
    private static final double QUANTUM_TUNNEL_STRENGTH  = 0.15;
    private static final double PLANCK_SCALE            = 0.001;

    // 修正颜色方案
    private static final float[] COL_BG_TOP   = {0.03f, 0.02f, 0.05f};
    private static final float[] COL_BG_MID   = {0.06f, 0.03f, 0.10f};
    private static final float[] COL_BG_BOT   = {0.12f, 0.05f, 0.18f};
    private static final float[] COL_STAR     = {0.8f, 0.85f, 1.0f};

    private static final float[] COL_VOID_CORE  = {0.0f, 0.0f, 0.0f, 0.95f};
    private static final float[] COL_VOID_EDGE  = {0.2f, 0.05f, 0.3f, 0.7f};
    private static final float[] COL_ENERGY_IN  = {0.4f, 0.2f, 0.8f, 0.5f};
    private static final float[] COL_ENERGY_MID = {0.6f, 0.3f, 0.9f, 0.3f};
    private static final float[] COL_ENERGY_OUT = {0.8f, 0.5f, 1.0f, 0.15f};
    private static final float[] COL_DISTORT    = {0.5f, 0.7f, 1.0f, 0.1f};
    private static final float[] COL_LIGHTNING  = {0.9f, 0.95f, 1.0f, 0.9f};
    private static final float[] COL_QUANTUM    = {1.0f, 1.0f, 1.0f, 0.6f};

    // 粒子颜色（紫白渐变）
    private static final float[][] COL_PARTICLES = {
            {0.9f, 0.8f, 1.0f, 0.9f},  // 亮紫白
            {0.7f, 0.5f, 1.0f, 0.7f},  // 紫色
            {0.5f, 0.3f, 0.9f, 0.5f},  // 深紫
            {1.0f, 0.9f, 1.0f, 0.8f},  // 近白
            {0.6f, 0.4f, 0.95f, 0.6f}  // 中紫
    };

    // 动画速度
    private static final float  STAR_DRIFT_SPEED         = 0.003f;  // 减慢星星漂移
    private static final float  VOID_PULSE_SPEED         = 0.2f;
    private static final float  ENERGY_FLOW_SPEED        = 1.0f;
    private static final float  DISTORT_WAVE_SPEED       = 0.5f;
    private static final float  LIGHTNING_CHANCE         = 0.003f;
    private static final float  LIGHTNING_DURATION       = 20f;
    private static final float  QUANTUM_FLUTTER_SPEED    = 2.0f;

    // 显示列表
    private int starList = -1;
    private int voidCoreList = -1;
    private int energyLayersList = -1;
    private int distortionList = -1;
    private int lightningList = -1;
    private int particleList = -1;  // 新增粒子显示列表

    private boolean built = false;
    private float lastLightningTime = 0;
    private float lastQuantumCollapseTime = 0;

    // 量子态缓存
    private QuantumState[] quantumStates = null;
    private double[] waveFunction = null;
    private RiftMesh cachedMesh = null;  // 缓存裂缝网格用于粒子生成

    // 粒子系统
    private Particle[] particles = null;

    // 量子态结构
    private static class QuantumState {
        double amplitude;
        double phase;
        double frequency;
        Vec3d position;
        boolean collapsed;
        double entanglementPhase;

        QuantumState(Random rng) {
            amplitude = 0.5 + rng.nextDouble() * 0.5;
            phase = rng.nextDouble() * Math.PI * 2;
            frequency = QUANTUM_BASE_FREQ * (0.5 + rng.nextDouble() * 2);
            position = new Vec3d(0, 0, 0);
            collapsed = false;
            entanglementPhase = rng.nextDouble() * Math.PI * 2;
        }
    }

    // 粒子结构
    private static class Particle {
        Vec3d position;
        Vec3d velocity;
        float size;
        float brightness;
        float life;
        float maxLife;
        int colorIndex;
        float phase;  // 用于动画

        Particle(Vec3d pos, Random rng) {
            position = pos;
            velocity = new Vec3d(
                    (rng.nextDouble() - 0.5) * 0.02,
                    (rng.nextDouble() - 0.5) * 0.02,
                    (rng.nextDouble() - 0.5) * 0.02
            );
            size = PARTICLE_SIZE * (0.5f + rng.nextFloat());
            brightness = PARTICLE_BRIGHTNESS * (0.7f + rng.nextFloat() * 0.3f);
            maxLife = 100 + rng.nextFloat() * 100;
            life = maxLife;
            colorIndex = rng.nextInt(COL_PARTICLES.length);
            phase = rng.nextFloat() * (float)Math.PI * 2;
        }

        void update(float deltaTime) {
            // 更新位置
            position = position.add(velocity.scale(deltaTime * PARTICLE_SPEED));

            // 添加随机漂移
            position = position.add(new Vec3d(
                    Math.sin(phase) * 0.001,
                    Math.cos(phase * 1.3) * 0.001,
                    Math.sin(phase * 0.7) * 0.001
            ));

            // 更新生命值
            life -= deltaTime;

            // 更新相位
            phase += deltaTime * 0.1f;
        }

        boolean isAlive() {
            return life > 0;
        }

        float getAlpha() {
            return (life / maxLife) * brightness;
        }
    }

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        if (!built) {
            buildDisplayLists();
            built = true;
        }

        final Tessellator tess = Tessellator.getInstance();
        final BufferBuilder buf = tess.getBuffer();
        float time = world.getTotalWorldTime() + partialTicks;

        // 准备渲染状态
        GlStateManager.disableTexture2D();
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // 1. 渲染天空背景
        renderDarkGradientSphere(tess, buf);

        // 2. 空间碎片（增强版星星）
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

        GlStateManager.color(
                COL_DISTORT[0],
                COL_DISTORT[1],
                COL_DISTORT[2],
                Math.max(0, Math.min(1, distortAlpha))
        );
        GlStateManager.pushMatrix();
        GlStateManager.rotate(time * 0.01f, 0, 1, 0);
        if (distortionList >= 0) {
            GlStateManager.callList(distortionList);
        }
        GlStateManager.popMatrix();

        // 4. 能量层
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        float energyPulse = MathHelper.sin(time * ENERGY_FLOW_SPEED * 0.1f);
        float quantumModulation = (float)(calculateQuantumFluctuation(time) * 0.3);

        // 外层能量
        GlStateManager.pushMatrix();
        GlStateManager.rotate(time * 0.02f, 0, 1, 0);
        float outerAlpha = COL_ENERGY_OUT[3] * (0.7f + 0.3f * energyPulse) * (1.0f + quantumModulation);
        GlStateManager.color(
                COL_ENERGY_OUT[0],
                COL_ENERGY_OUT[1],
                COL_ENERGY_OUT[2],
                Math.max(0, Math.min(1, outerAlpha))
        );
        if (energyLayersList >= 0) {
            GlStateManager.callList(energyLayersList);
        }
        GlStateManager.popMatrix();

        // 中层能量
        GlStateManager.pushMatrix();
        GlStateManager.rotate(-time * 0.015f, 0, 1, 0);
        float midAlpha = COL_ENERGY_MID[3] * (0.6f + 0.4f * MathHelper.cos(time * ENERGY_FLOW_SPEED * 0.15f));
        GlStateManager.color(
                COL_ENERGY_MID[0],
                COL_ENERGY_MID[1],
                COL_ENERGY_MID[2],
                Math.max(0, Math.min(1, midAlpha))
        );
        GlStateManager.scale(0.95f, 1f, 0.95f);
        if (energyLayersList >= 0) {
            GlStateManager.callList(energyLayersList);
        }
        GlStateManager.popMatrix();

        // 内层能量
        GlStateManager.pushMatrix();
        GlStateManager.rotate(time * 0.025f, 0, 1, 0);
        float innerAlpha = COL_ENERGY_IN[3] * (0.5f + 0.5f * energyPulse);
        GlStateManager.color(
                COL_ENERGY_IN[0],
                COL_ENERGY_IN[1],
                COL_ENERGY_IN[2],
                Math.max(0, Math.min(1, innerAlpha))
        );
        GlStateManager.scale(0.9f, 1f, 0.9f);
        if (energyLayersList >= 0) {
            GlStateManager.callList(energyLayersList);
        }
        GlStateManager.popMatrix();

        // 5. 虚空核心
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        float voidPulse = 0.9f + 0.1f * MathHelper.sin(time * VOID_PULSE_SPEED * 0.1f);
        GlStateManager.color(1f, 1f, 1f, voidPulse);
        if (voidCoreList >= 0) {
            GlStateManager.callList(voidCoreList);
        }

        // 6. 裂缝边缘粒子效果（新增）
        renderRiftParticles(time, world);

        // 7. 闪电效果
        if (world.rand.nextFloat() < LIGHTNING_CHANCE || Math.abs(time - lastLightningTime) < LIGHTNING_DURATION) {
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

        // 恢复状态
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableFog();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
    }

    // 渲染裂缝粒子效果
    private void renderRiftParticles(float time, WorldClient world) {
        if (particles == null || cachedMesh == null) return;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        Random rng = world.rand;

        // 更新和重生粒子
        for (int i = 0; i < particles.length; i++) {
            particles[i].update(1.0f);

            // 如果粒子死亡，在裂缝边缘重生
            if (!particles[i].isAlive()) {
                Vec3d newPos = getRandomRiftEdgePosition(rng);
                particles[i] = new Particle(newPos, rng);
            }
        }

        // 渲染粒子
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableCull();

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        for (Particle p : particles) {
            float[] color = COL_PARTICLES[p.colorIndex];
            float alpha = p.getAlpha();

            // 粒子脉动
            float pulse = 1.0f + 0.3f * MathHelper.sin(time * 0.1f + p.phase);
            float size = p.size * pulse;

            // 粒子位置
            Vec3d pos = p.position.scale(SKY_RADIUS);

            // 面向相机的billboard粒子
            Vec3d camPos = Minecraft.getMinecraft().player.getPositionEyes(1.0f);
            Vec3d toCam = camPos.subtract(pos).normalize();
            Vec3d right = toCam.crossProduct(new Vec3d(0, 1, 0)).normalize().scale(size);
            Vec3d up = toCam.crossProduct(right).normalize().scale(size);

            // 绘制粒子四边形
            buf.pos(pos.x - right.x - up.x, pos.y - right.y - up.y, pos.z - right.z - up.z)
                    .color(color[0], color[1], color[2], alpha).endVertex();
            buf.pos(pos.x + right.x - up.x, pos.y + right.y - up.y, pos.z + right.z - up.z)
                    .color(color[0], color[1], color[2], alpha).endVertex();
            buf.pos(pos.x + right.x + up.x, pos.y + right.y + up.y, pos.z + right.z + up.z)
                    .color(color[0], color[1], color[2], alpha).endVertex();
            buf.pos(pos.x - right.x + up.x, pos.y - right.y + up.y, pos.z - right.z + up.z)
                    .color(color[0], color[1], color[2], alpha).endVertex();
        }

        tess.draw();

        GlStateManager.enableCull();
    }

    // 获取裂缝边缘的随机位置
    private Vec3d getRandomRiftEdgePosition(Random rng) {
        if (cachedMesh == null) return new Vec3d(0, 0, 0);

        int index = rng.nextInt(cachedMesh.centerLine.length);

        // 随机选择左边缘或右边缘
        Vec3d edgePos;
        if (rng.nextBoolean()) {
            // 在能量层边缘生成
            edgePos = cachedMesh.energyLeft[index];
        } else {
            edgePos = cachedMesh.energyRight[index];
        }

        // 添加一些随机偏移
        double offset = PARTICLE_DRIFT * 0.01;
        return edgePos.add(
                (rng.nextDouble() - 0.5) * offset,
                (rng.nextDouble() - 0.5) * offset,
                (rng.nextDouble() - 0.5) * offset
        ).normalize();
    }

    private void buildDisplayLists() {
        Random rng = new Random(RIFT_SEED);

        // 初始化量子态
        initializeQuantumStates(rng);

        // 初始化粒子系统
        initializeParticles(rng);

        // 构建显示列表
        starList = GLAllocation.generateDisplayLists(1);
        voidCoreList = GLAllocation.generateDisplayLists(1);
        energyLayersList = GLAllocation.generateDisplayLists(1);
        distortionList = GLAllocation.generateDisplayLists(1);
        lightningList = GLAllocation.generateDisplayLists(1);

        // 构建增强版星星
        GlStateManager.glNewList(starList, GL11.GL_COMPILE);
        renderEnhancedStars();
        GlStateManager.glEndList();

        // 构建裂缝网格
        RiftMesh mesh = buildQuantumRiftMesh(rng);
        cachedMesh = mesh;  // 缓存网格用于粒子生成

        // 虚空核心
        GlStateManager.glNewList(voidCoreList, GL11.GL_COMPILE);
        renderVoidCore(mesh);
        GlStateManager.glEndList();

        // 能量层
        GlStateManager.glNewList(energyLayersList, GL11.GL_COMPILE);
        renderEnergyLayer(mesh);
        GlStateManager.glEndList();

        // 扭曲层
        GlStateManager.glNewList(distortionList, GL11.GL_COMPILE);
        renderDistortionLayer(mesh);
        GlStateManager.glEndList();

        rebuildLightning();
    }

    // 初始化粒子系统
    private void initializeParticles(Random rng) {
        particles = new Particle[PARTICLE_COUNT];

        // 如果网格还没有创建，先创建一个临时的
        if (cachedMesh == null) {
            cachedMesh = buildQuantumRiftMesh(rng);
        }

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Vec3d pos = getRandomRiftEdgePosition(rng);
            particles[i] = new Particle(pos, rng);
            // 随机化初始生命值，让粒子不同步
            particles[i].life = rng.nextFloat() * particles[i].maxLife;
        }
    }

    // 渲染增强版星星
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

            // 避免裂缝区域，但不要太严格
            double riftDist = Math.abs(x);
            if (riftDist < 0.15 && Math.abs(y) < 0.3) continue;

            double radius = SKY_RADIUS * (0.85 + rand.nextFloat() * 0.15);
            double X = x * radius;
            double Y = y * radius;
            double Z = z * radius;

            // 变化的星星大小
            double size = 0.1 + rand.nextFloat() * 0.4;

            // 一些星星更亮
            if (rand.nextFloat() < 0.1) {
                size *= 1.5;
            }

            // 创建星星的十字形状（更像真实的星星）
            if (rand.nextFloat() < 0.3) {
                // 十字星
                double s = size;
                // 横线
                buf.pos(X - s * 2, Y, Z).endVertex();
                buf.pos(X + s * 2, Y, Z).endVertex();
                buf.pos(X + s * 2, Y + s * 0.3, Z).endVertex();
                buf.pos(X - s * 2, Y + s * 0.3, Z).endVertex();
                // 竖线
                buf.pos(X, Y - s * 2, Z).endVertex();
                buf.pos(X + s * 0.3, Y - s * 2, Z).endVertex();
                buf.pos(X + s * 0.3, Y + s * 2, Z).endVertex();
                buf.pos(X, Y + s * 2, Z).endVertex();
            } else {
                // 普通方形星星
                buf.pos(X - size, Y - size, Z).endVertex();
                buf.pos(X + size, Y - size, Z).endVertex();
                buf.pos(X + size, Y + size, Z).endVertex();
                buf.pos(X - size, Y + size, Z).endVertex();
            }
        }

        tess.draw();
    }

    // 初始化量子态
    private void initializeQuantumStates(Random rng) {
        int stateCount = RIFT_SEGMENTS + 1;
        quantumStates = new QuantumState[stateCount];
        waveFunction = new double[stateCount];

        for (int i = 0; i < stateCount; i++) {
            quantumStates[i] = new QuantumState(rng);
            waveFunction[i] = rng.nextGaussian() * 0.5;
        }

        normalizeWaveFunction();
    }

    // 归一化波函数
    private void normalizeWaveFunction() {
        double sum = 0;
        for (double psi : waveFunction) {
            sum += psi * psi;
        }
        if (sum > 0) {
            double norm = Math.sqrt(sum);
            for (int i = 0; i < waveFunction.length; i++) {
                waveFunction[i] /= norm;
            }
        }
    }

    // 计算量子涨落
    private double calculateQuantumFluctuation(float time) {
        double fluctuation = 0;

        for (int n = 0; n < QUANTUM_HARMONICS; n++) {
            double freq = QUANTUM_BASE_FREQ * Math.pow(1.5, n);
            double amp = Math.pow(QUANTUM_DECAY, n);
            double phase = n * Math.PI / QUANTUM_HARMONICS;

            fluctuation += amp * Math.sin(freq * time * 0.01 + phase);
            fluctuation += amp * 0.5 * Math.cos(freq * time * 0.005) * PLANCK_SCALE;
        }

        return fluctuation * QUANTUM_UNCERTAINTY;
    }

    // 构建量子裂缝网格
    private RiftMesh buildQuantumRiftMesh(Random rng) {
        int n = RIFT_SEGMENTS + 1;
        RiftMesh mesh = new RiftMesh(n);

        for (int i = 0; i < n; i++) {
            double t = i / (double)(n - 1);
            double angle = t * Math.PI * 2;

            // 基础路径
            Vec3d basePos = new Vec3d(
                    Math.cos(angle),
                    0,
                    Math.sin(angle) * 0.3
            );

            // 量子涨落
            double waveFunctionContribution = waveFunction[i] * QUANTUM_UNCERTAINTY;

            double quantumHarmonics = 0;
            for (int h = 0; h < QUANTUM_HARMONICS; h++) {
                double freq = QUANTUM_BASE_FREQ * Math.pow(1.618, h);
                double amp = Math.pow(QUANTUM_DECAY, h);
                double phase = quantumStates[i].phase + h * Math.PI / QUANTUM_HARMONICS;

                quantumHarmonics += amp * Math.sin(angle * freq + phase);
            }

            // 简化的量子效应
            double quantumY = waveFunctionContribution + quantumHarmonics * 0.1;
            double quantumZ = quantumHarmonics * 0.05;

            Vec3d center = new Vec3d(
                    basePos.x,
                    quantumY,
                    basePos.z + quantumZ
            ).normalize();

            mesh.centerLine[i] = center;

            // 计算裂缝宽度
            double widthFactor = 1.0 - Math.abs(t - 0.5) * 0.3;
            widthFactor *= (1.0 + Math.abs(waveFunctionContribution));

            // 计算垂直方向
            Vec3d up = new Vec3d(0, 1, 0);
            Vec3d right = center.crossProduct(up).normalize();

            // 各层宽度
            double voidW = Math.toRadians(RIFT_WIDTH_DEG_VOID * widthFactor);
            double energyW = Math.toRadians(RIFT_WIDTH_DEG_AURA * widthFactor);
            double distortW = Math.toRadians(RIFT_WIDTH_DEG_DISTORT * widthFactor);

            // 设置边界
            mesh.voidLeft[i] = rotateAroundAxis(center, right, voidW).normalize();
            mesh.voidRight[i] = rotateAroundAxis(center, right, -voidW).normalize();

            mesh.energyLeft[i] = rotateAroundAxis(center, right, energyW).normalize();
            mesh.energyRight[i] = rotateAroundAxis(center, right, -energyW).normalize();

            mesh.distortLeft[i] = rotateAroundAxis(center, right, distortW).normalize();
            mesh.distortRight[i] = rotateAroundAxis(center, right, -distortW).normalize();
        }

        // 平滑处理
        smoothMesh(mesh, 1);

        return mesh;
    }

    // 平滑网格
    private void smoothMesh(RiftMesh mesh, int iterations) {
        for (int iter = 0; iter < iterations; iter++) {
            Vec3d[] smoothed = new Vec3d[mesh.centerLine.length];

            for (int i = 0; i < mesh.centerLine.length; i++) {
                Vec3d prev = mesh.centerLine[(i - 1 + mesh.centerLine.length) % mesh.centerLine.length];
                Vec3d curr = mesh.centerLine[i];
                Vec3d next = mesh.centerLine[(i + 1) % mesh.centerLine.length];

                smoothed[i] = prev.scale(0.2)
                        .add(curr.scale(0.6))
                        .add(next.scale(0.2))
                        .normalize();
            }

            System.arraycopy(smoothed, 0, mesh.centerLine, 0, mesh.centerLine.length);
        }
    }

    // [其余渲染方法保持不变...]

    private void renderDarkGradientSphere(Tessellator tess, BufferBuilder buf) {
        final int latSegments = 12;
        final int lonSegments = 24;

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
    }

    private float[] getGradientColor(double y) {
        if (y > 0.5) {
            return COL_BG_TOP;
        } else if (y > 0) {
            float t = (float)(y * 2);
            return new float[] {
                    COL_BG_MID[0] * (1-t) + COL_BG_TOP[0] * t,
                    COL_BG_MID[1] * (1-t) + COL_BG_TOP[1] * t,
                    COL_BG_MID[2] * (1-t) + COL_BG_TOP[2] * t
            };
        } else {
            float t = (float)((y + 1) / 2);
            return new float[] {
                    COL_BG_BOT[0] * (1-t) + COL_BG_MID[0] * t,
                    COL_BG_BOT[1] * (1-t) + COL_BG_MID[1] * t,
                    COL_BG_BOT[2] * (1-t) + COL_BG_MID[2] * t
            };
        }
    }

    private static class RiftMesh {
        final Vec3d[] centerLine;
        final Vec3d[] voidLeft, voidRight;
        final Vec3d[] energyLeft, energyRight;
        final Vec3d[] distortLeft, distortRight;

        RiftMesh(int n) {
            centerLine = new Vec3d[n];
            voidLeft = new Vec3d[n];
            voidRight = new Vec3d[n];
            energyLeft = new Vec3d[n];
            energyRight = new Vec3d[n];
            distortLeft = new Vec3d[n];
            distortRight = new Vec3d[n];
        }
    }

    private void renderVoidCore(RiftMesh mesh) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        GlStateManager.disableCull();

        buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < mesh.centerLine.length; i++) {
            Vec3d L = mesh.voidLeft[i].scale(SKY_RADIUS);
            Vec3d R = mesh.voidRight[i].scale(SKY_RADIUS);

            float t = i / (float)(mesh.centerLine.length - 1);
            float alpha = (1.0f - Math.abs(t - 0.5f) * 0.3f) * COL_VOID_EDGE[3];

            buf.pos(L.x, L.y, L.z).color(COL_VOID_EDGE[0], COL_VOID_EDGE[1], COL_VOID_EDGE[2], alpha).endVertex();
            buf.pos(R.x, R.y, R.z).color(COL_VOID_EDGE[0], COL_VOID_EDGE[1], COL_VOID_EDGE[2], alpha).endVertex();
        }
        tess.draw();

        buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < mesh.centerLine.length; i++) {
            Vec3d L = mesh.voidLeft[i].scale(SKY_RADIUS * 0.6);
            Vec3d R = mesh.voidRight[i].scale(SKY_RADIUS * 0.6);

            float t = i / (float)(mesh.centerLine.length - 1);
            float alpha = (1.0f - Math.abs(t - 0.5f) * 0.2f) * COL_VOID_CORE[3];

            buf.pos(L.x, L.y, L.z).color(COL_VOID_CORE[0], COL_VOID_CORE[1], COL_VOID_CORE[2], alpha).endVertex();
            buf.pos(R.x, R.y, R.z).color(COL_VOID_CORE[0], COL_VOID_CORE[1], COL_VOID_CORE[2], alpha).endVertex();
        }
        tess.draw();

        GlStateManager.enableCull();
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

    private Vec3d rotateAroundAxis(Vec3d v, Vec3d axis, double rad) {
        axis = axis.normalize();
        double c = Math.cos(rad), s = Math.sin(rad);
        double dot = v.dotProduct(axis);
        Vec3d cross = axis.crossProduct(v);
        return v.scale(c).add(axis.scale(dot * (1 - c))).add(cross.scale(s));
    }
}