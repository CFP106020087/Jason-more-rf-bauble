package com.moremod.client.gui;

import com.moremod.config.BrokenGodConfig;
import com.moremod.config.HumanityConfig;
import com.moremod.config.ShambhalaConfig;
import com.moremod.system.logic.NarrativeLogicHandler;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class StoryOverlayRenderer {

    // ==================================================
    //  贴图资源
    // ==================================================
    private static final ResourceLocation HUMAN_LOW_TEX = new ResourceLocation("moremod", "textures/gui/black_vignette.png");
    private static final ResourceLocation VIGNETTE_TEX = new ResourceLocation("minecraft", "textures/misc/vignette.png");

    private final Random rand = new Random();

    // 着色器程序
    private int brokenGodShaderProgram = -1;

    // 香巴拉齿轮数据
    private static final int MAX_GEARS = 8;
    private final float[] gearX = new float[MAX_GEARS];
    private final float[] gearY = new float[MAX_GEARS];
    private final float[] gearSize = new float[MAX_GEARS];
    private final float[] gearRotation = new float[MAX_GEARS];
    private final float[] gearAlpha = new float[MAX_GEARS];
    private final float[] gearLifetime = new float[MAX_GEARS];
    private final boolean[] gearActive = new boolean[MAX_GEARS];

    // ==================================================
    //  着色器代码 - 破碎神明 (全域数字视觉)
    // ==================================================
    private static final String VERTEX_SHADER =
            "#version 120\n" +
                    "void main() {" +
                    "    gl_TexCoord[0] = gl_MultiTexCoord0;" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;" +
                    "}";

    private static final String BROKEN_GOD_FRAGMENT_SHADER =
            "#version 120\n" +
                    "uniform float time;" +
                    "uniform float intensity;" +
                    "uniform vec3 baseColor;" +
                    "uniform vec2 resolution;" +
                    "uniform int isHurt;" +

                    "float random(vec2 st) {" +
                    "    return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);" +
                    "}" +

                    "void main() {" +
                    "    vec2 uv = gl_TexCoord[0].st;" +
                    "    vec2 center = vec2(0.5, 0.5);" +
                    "    float aspect = resolution.x / resolution.y;" +
                    "    vec2 centeredUV = (uv - center) * vec2(aspect, 1.0);" +
                    "    float dist = length(centeredUV);" +
                    "    float vignette = smoothstep(0.4, 1.2, dist);" +
                    "    float scanline = sin(uv.y * 400.0 + time * 5.0) * 0.03;" +
                    "    float noise = (random(uv + time) - 0.5) * 0.05;" +
                    "    vec3 finalColor = baseColor + vec3(noise);" +
                    "    if (isHurt > 0) {" +
                    "        finalColor = mix(finalColor, vec3(1.0, 0.2, 0.2), 0.5);" +
                    "    }" +
                    "    float baseAlpha = 0.15 + vignette * 0.75;" +
                    "    float finalAlpha = baseAlpha + scanline;" +
                    "    finalAlpha = clamp(finalAlpha * intensity, 0.0, 0.95);" +
                    "    gl_FragColor = vec4(finalColor, finalAlpha);" +
                    "}";

    // ==================================================
    //  主渲染逻辑
    // ==================================================
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;

        PlayerNarrativeState state = NarrativeLogicHandler.determineState(player);
        if (state == PlayerNarrativeState.HUMAN_HIGH || state == PlayerNarrativeState.NONE) return;

        // 检查配置
        if (state == PlayerNarrativeState.HUMAN_LOW && !HumanityConfig.enableVisualDistortion) return;
        if (state == PlayerNarrativeState.BROKEN_GOD && !BrokenGodConfig.enableVisualOverlay) return;
        if (state == PlayerNarrativeState.SHAMBHALA && !ShambhalaConfig.enableVisualOverlay) return;

        ScaledResolution resolution = event.getResolution();
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        float time = (player.ticksExisted + event.getPartialTicks()) / 20.0f;

        // 懒加载着色器
        if (brokenGodShaderProgram == -1 && state == PlayerNarrativeState.BROKEN_GOD) {
            initBrokenGodShader();
        }

        // 准备 GL 状态
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // 分发渲染
        switch (state) {
            case HUMAN_LOW:
                renderLowHumanityEffects(width, height, time, player);
                break;
            case BROKEN_GOD:
                renderBrokenGodEffects(width, height, time, player);
                break;
            case SHAMBHALA:
                renderShambhalaEffects(width, height, time);
                break;
        }

        // 恢复 GL 状态
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    // ==================================================
    //  低人性渲染 - 黑色晕影 + 数值驱动
    // ==================================================
    private void renderLowHumanityEffects(int w, int h, float time, EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null) return;

        float currentHumanity = data.getHumanity();
        float startThreshold = 40.0f;
        float ratio = MathHelper.clamp((startThreshold - currentHumanity) / startThreshold, 0.0f, 1.0f);
        float baseAlpha = ratio * 0.95f;

        if (baseAlpha <= 0.01f) return;

        float breath = 0.95f + 0.05f * (float) Math.sin(time * 1.5f);

        mc.getTextureManager().bindTexture(HUMAN_LOW_TEX);
        GlStateManager.color(1.0f, 1.0f, 1.0f, baseAlpha * breath);
        drawFullScreenQuad(w, h, true);
    }

    // ==================================================
    //  破碎神明渲染 - 着色器 + 撕裂 + 噪点 + BIOS
    // ==================================================
    private void renderBrokenGodEffects(int w, int h, float time, EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        boolean isHurt = player.hurtTime > 0;
        boolean isRandomGlitch = rand.nextFloat() < 0.002f;
        boolean triggerTearing = isHurt || isRandomGlitch;

        float intensity = isHurt ? 1.5f : 0.8f;

        // 配色：氧化青铜 (Verdigris)
        float r = 0.6f;
        float g = 1.0f;
        float b = 0.9f;

        if (isHurt) {
            r = 1.0f;
            g = 0.3f;
            b = 0.2f;
            BiosLogRenderer.triggerDamageBurst(3);
        }
        if (rand.nextFloat() < 0.0005f) {
            BiosLogRenderer.render(time);
        }

        // 1. Shader 渲染
        if (OpenGlHelper.shadersSupported && brokenGodShaderProgram != -1) {
            ARBShaderObjects.glUseProgramObjectARB(brokenGodShaderProgram);

            int timeLoc = ARBShaderObjects.glGetUniformLocationARB(brokenGodShaderProgram, "time");
            ARBShaderObjects.glUniform1fARB(timeLoc, time);
            int intensityLoc = ARBShaderObjects.glGetUniformLocationARB(brokenGodShaderProgram, "intensity");
            ARBShaderObjects.glUniform1fARB(intensityLoc, intensity);
            int colorLoc = ARBShaderObjects.glGetUniformLocationARB(brokenGodShaderProgram, "baseColor");
            ARBShaderObjects.glUniform3fARB(colorLoc, r, g, b);
            int resLoc = ARBShaderObjects.glGetUniformLocationARB(brokenGodShaderProgram, "resolution");
            ARBShaderObjects.glUniform2fARB(resLoc, (float) w, (float) h);
            int hurtLoc = ARBShaderObjects.glGetUniformLocationARB(brokenGodShaderProgram, "isHurt");
            ARBShaderObjects.glUniform1iARB(hurtLoc, isHurt ? 1 : 0);

            mc.getTextureManager().bindTexture(VIGNETTE_TEX);
            drawFullScreenQuad(w, h, true);

            ARBShaderObjects.glUseProgramObjectARB(0);
        }

        // 2. 画面撕裂 (Glitch Tearing)
        if (triggerTearing) {
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            int glitchCount = isHurt ? 8 : 2;
            float tearIntensity = isHurt ? 40.0f : 10.0f;

            GlStateManager.disableTexture2D();

            for (int i = 0; i < glitchCount; i++) {
                float yStart = rand.nextInt(h);
                float yHeight = rand.nextInt(30) + 5;
                float xOffset = (rand.nextFloat() - 0.5f) * tearIntensity;

                GlStateManager.color(r, g, b, 0.6f);

                buffer.begin(7, DefaultVertexFormats.POSITION);
                buffer.pos(0 + xOffset, yStart + yHeight, -90).endVertex();
                buffer.pos(w + xOffset, yStart + yHeight, -90).endVertex();
                buffer.pos(w + xOffset, yStart, -90).endVertex();
                buffer.pos(0 + xOffset, yStart, -90).endVertex();
                tessellator.draw();
            }

            GlStateManager.enableTexture2D();
            GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);
        }

        // 3. 边缘噪点 (Digital Noise)
        GlStateManager.disableTexture2D();
        GL11.glPointSize(2.0f);
        buffer.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);

        int noiseCount = (w * h) / 10000;
        if (isHurt) noiseCount *= 3;

        for (int i = 0; i < noiseCount; i++) {
            float nx = rand.nextInt(w);
            float ny = rand.nextInt(h);

            float edgeX = Math.min(nx, w - nx) / (float) w;
            float edgeY = Math.min(ny, h - ny) / (float) h;
            float edgeFactor = Math.min(edgeX, edgeY);

            if (edgeFactor < 0.2f) {
                float val = rand.nextFloat();
                float alpha = (0.2f - edgeFactor) / 0.2f * 0.7f;

                float noiseR = Math.min(1.0f, r + 0.2f);
                float noiseG = Math.min(1.0f, g + 0.2f);
                float noiseB = Math.min(1.0f, b + 0.2f);

                buffer.pos(nx, ny, -90).color(noiseR, noiseG, noiseB, alpha * val).endVertex();
            }
        }
        tessellator.draw();
        GL11.glPointSize(1.0f);
        GlStateManager.enableTexture2D();

        // 4. BIOS 日志
        BiosLogRenderer.render(time);
    }

    // ==================================================
    //  香巴拉渲染 - 全屏淡金光 + 黑色空心齿轮
    // ==================================================
    // ==================================================
    //  【Erica 最终设计】香巴拉渲染 - 圣金背景 + 青色全息齿轮
    // ==================================================
    private void renderShambhalaEffects(int w, int h, float time) {
        // --- 1. 背景：全屏淡淡金光 (保持不变) ---
        GlStateManager.disableTexture2D();

        // 关闭 Alpha 测试以支持微弱透明度
        GlStateManager.disableAlpha();

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);

        // 呼吸效果：Alpha 0.01 ~ 0.15
        float breath = 0.08f + 0.07f * (float) Math.sin(time * 0.5f);

        // 背景色：温暖的圣金色
        GlStateManager.color(1.0f, 0.9f, 0.6f, breath);

        drawFullScreenQuad(w, h, false);

        GlStateManager.enableAlpha(); // 恢复

        // --- 2. 前景：因果青 (Causal Cyan) 全息齿轮 ---
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.bindTexture(0);

        // 开启混合模式：这里用 Additive 混合 (Src=ONE, Dst=ONE) 会让光效更亮更科幻！
        // 如果觉得太亮，可以改回普通的 (SRC_ALPHA, ONE_MINUS_SRC_ALPHA)
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        updateGears(w, h, time);

        for (int i = 0; i < MAX_GEARS; i++) {
            if (gearActive[i] && gearAlpha[i] > 0.01f) {
                drawCyanEnergyGear(gearX[i], gearY[i], gearSize[i], gearRotation[i], gearAlpha[i]);
            }
        }

        // 恢复状态
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * 绘制【因果青】全息能量齿轮
     * 技巧：画两次，一次宽线条做光晕，一次细线条做核心
     */
    private void drawCyanEnergyGear(float x, float y, float size, float rotation, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        float radiusOuter = size * 0.6f;
        float radiusInner = radiusOuter * 0.75f;
        float holeRadius = radiusOuter * 0.3f;
        int segments = 48; // 齿轮精度

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.rotate(rotation, 0, 0, 1);

        // === Pass 1: 光晕层 (宽，半透明青色) ===
        GL11.glLineWidth(4.0F); // 宽线条
        // 颜色：青色 (Cyan) 0.0, 1.0, 1.0
        // Alpha 降低一点，作为辉光
        GlStateManager.color(0.0f, 0.8f, 1.0f, alpha * 0.4f);

        drawGearLines(buffer, segments, radiusOuter, radiusInner, holeRadius);

        // === Pass 2: 核心层 (细，高亮偏白) ===
        GL11.glLineWidth(1.5F); // 细线条
        // 颜色：极亮青白 (接近白色)
        GlStateManager.color(0.8f, 1.0f, 1.0f, alpha * 0.9f);

        drawGearLines(buffer, segments, radiusOuter, radiusInner, holeRadius);

        GlStateManager.popMatrix();
    }

    // 辅助方法：只负责画线，不负责颜色
    private void drawGearLines(BufferBuilder buffer, int segments, float rOuter, float rInner, float rHole) {
        Tessellator tessellator = Tessellator.getInstance();

        // 外圈齿
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            // 齿形逻辑
            float r = (i % 4 < 2) ? rOuter : rInner;
            buffer.pos(Math.cos(angle) * r, Math.sin(angle) * r, 0).endVertex();
        }
        tessellator.draw();

        // 内圈孔
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        for (int i = 0; i <= 32; i++) {
            double angle = (Math.PI * 2 * i) / 32;
            buffer.pos(Math.cos(angle) * rHole, Math.sin(angle) * rHole, 0).endVertex();
        }
        tessellator.draw();
    }

    // ==================================================
    //  工具方法
    // ==================================================
    private void drawFullScreenQuad(int width, int height, boolean useTex) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        double z = -90.0D;

        if (useTex) {
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(0, height, z).tex(0, 1).endVertex();
            buffer.pos(width, height, z).tex(1, 1).endVertex();
            buffer.pos(width, 0, z).tex(1, 0).endVertex();
            buffer.pos(0, 0, z).tex(0, 0).endVertex();
        } else {
            buffer.begin(7, DefaultVertexFormats.POSITION);
            buffer.pos(0, height, z).endVertex();
            buffer.pos(width, height, z).endVertex();
            buffer.pos(width, 0, z).endVertex();
            buffer.pos(0, 0, z).endVertex();
        }
        tessellator.draw();
    }

    // ==================================================
    //  齿轮逻辑
    // ==================================================
    private void updateGears(int w, int h, float time) {
        float cx = w / 2.0f;
        float cy = h / 2.0f;
        float safeZoneRadius = Math.min(w, h) / 2.0f * 0.85f;

        int activeCount = 0;
        for (boolean b : gearActive) if (b) activeCount++;

        float spawnChance = (activeCount == 0) ? 1.0f : 0.02f;

        if (rand.nextFloat() < spawnChance) {
            for (int i = 0; i < MAX_GEARS; i++) {
                if (!gearActive[i]) {
                    boolean positionFound = false;
                    float newSize = 0, newX = 0, newY = 0;

                    for (int attempt = 0; attempt < 10; attempt++) {
                        newSize = 40 + rand.nextFloat() * 60;
                        double angle = rand.nextDouble() * Math.PI * 2;
                        float dist = safeZoneRadius + (rand.nextFloat() * 50) + (newSize * 0.4f);
                        newX = cx + (float) (Math.cos(angle) * dist);
                        newY = cy + (float) (Math.sin(angle) * dist);

                        boolean overlap = false;
                        for (int j = 0; j < MAX_GEARS; j++) {
                            if (gearActive[j] && i != j) {
                                float dx = newX - gearX[j];
                                float dy = newY - gearY[j];
                                if ((dx * dx + dy * dy) < Math.pow((newSize + gearSize[j]) * 0.6f, 2)) {
                                    overlap = true;
                                    break;
                                }
                            }
                        }
                        if (!overlap) {
                            positionFound = true;
                            break;
                        }
                    }

                    if (positionFound) {
                        gearX[i] = newX;
                        gearY[i] = newY;
                        gearSize[i] = newSize;
                        gearRotation[i] = rand.nextFloat() * 360;
                        gearAlpha[i] = 0.0f;
                        gearLifetime[i] = 5.0f + rand.nextFloat() * 5.0f;
                        gearActive[i] = true;
                    }
                    break;
                }
            }
        }

        float dt = 0.05f;
        for (int i = 0; i < MAX_GEARS; i++) {
            if (gearActive[i]) {
                gearLifetime[i] -= dt;
                gearRotation[i] += dt * ((i % 2 == 0 ? 1 : -1) * (15 + 100 / gearSize[i]));

                if (gearLifetime[i] > 4.0f) {
                    gearAlpha[i] = Math.min(0.85f, gearAlpha[i] + dt * 0.5f);
                } else if (gearLifetime[i] < 1.5f) {
                    gearAlpha[i] = Math.max(0.0f, gearAlpha[i] - dt * 0.5f);
                }

                if (gearLifetime[i] <= 0) {
                    gearActive[i] = false;
                }
            }
        }
    }

    // ==================================================
    //  着色器初始化
    // ==================================================
    private void initBrokenGodShader() {
        if (!OpenGlHelper.shadersSupported) {
            brokenGodShaderProgram = -1;
            return;
        }

        try {
            int vertexShader = ARBShaderObjects.glCreateShaderObjectARB(OpenGlHelper.GL_VERTEX_SHADER);
            ARBShaderObjects.glShaderSourceARB(vertexShader, VERTEX_SHADER);
            ARBShaderObjects.glCompileShaderARB(vertexShader);

            if (ARBShaderObjects.glGetObjectParameteriARB(vertexShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE) {
                brokenGodShaderProgram = -1;
                return;
            }

            int fragmentShader = ARBShaderObjects.glCreateShaderObjectARB(OpenGlHelper.GL_FRAGMENT_SHADER);
            ARBShaderObjects.glShaderSourceARB(fragmentShader, BROKEN_GOD_FRAGMENT_SHADER);
            ARBShaderObjects.glCompileShaderARB(fragmentShader);

            if (ARBShaderObjects.glGetObjectParameteriARB(fragmentShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE) {
                brokenGodShaderProgram = -1;
                return;
            }

            brokenGodShaderProgram = ARBShaderObjects.glCreateProgramObjectARB();
            ARBShaderObjects.glAttachObjectARB(brokenGodShaderProgram, vertexShader);
            ARBShaderObjects.glAttachObjectARB(brokenGodShaderProgram, fragmentShader);
            ARBShaderObjects.glLinkProgramARB(brokenGodShaderProgram);

            if (ARBShaderObjects.glGetObjectParameteriARB(brokenGodShaderProgram, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
                brokenGodShaderProgram = -1;
            }

        } catch (Exception e) {
            brokenGodShaderProgram = -1;
        }
    }
}