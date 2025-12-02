package com.moremod.client.gui;

import com.moremod.config.BrokenGodConfig;
import com.moremod.config.HumanityConfig;
import com.moremod.config.ShambhalaConfig;
import com.moremod.logic.NarrativeLogicHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@SideOnly(Side.CLIENT)
public class StoryOverlayRenderer {

    private static final Map<PlayerNarrativeState, ResourceLocation> OVERLAYS = new HashMap<>();
    static {
        OVERLAYS.put(PlayerNarrativeState.HUMAN_LOW, new ResourceLocation("minecraft", "textures/misc/vignette.png"));
    }

    private final Random rand = new Random();
    private int shaderProgram = -1;
    private int shambhalaShaderProgram = -1;

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
    //  香巴拉着色器 - 金色边缘渐变
    // ==================================================
    private static final String SHAMBHALA_FRAGMENT_SHADER =
            "#version 120\n" +
                    "uniform float time;" +
                    "uniform vec2 resolution;" +
                    "void main() {" +
                    "    vec2 uv = gl_TexCoord[0].st;" +
                    "    vec2 center = vec2(0.5, 0.5);" +
                    "    float aspect = resolution.x / resolution.y;" +
                    "    vec2 centeredUV = (uv - center) * vec2(aspect, 1.0);" +
                    "    float dist = length(centeredUV);" +
                    // 边缘到中心的渐变 (中心透明，边缘金色)
                    "    float vignette = smoothstep(0.3, 1.0, dist);" +
                    // 金色基础色
                    "    vec3 goldColor = vec3(1.0, 0.85, 0.4);" +
                    // 微弱的呼吸效果
                    "    float breath = 0.8 + 0.2 * sin(time * 0.5);" +
                    // Alpha: 中心完全透明，边缘半透明金色
                    "    float alpha = vignette * 0.4 * breath;" +
                    "    gl_FragColor = vec4(goldColor, alpha);" +
                    "}";

    // ==================================================
    //  着色器代码 (GLSL) - 全域数字视觉版
    // ==================================================
    private static final String VERTEX_SHADER =
            "#version 120\n" +
                    "void main() {" +
                    "    gl_TexCoord[0] = gl_MultiTexCoord0;" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "#version 120\n" +
                    "uniform float time;" +
                    "uniform float intensity;" +
                    "uniform vec3 baseColor;" +
                    "uniform vec2 resolution;" +
                    "uniform int isHurt;" +

                    // 随机噪点函数
                    "float random(vec2 st) {" +
                    "    return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);" +
                    "}" +

                    "void main() {" +
                    "    vec2 uv = gl_TexCoord[0].st;" +
                    "    vec2 center = vec2(0.5, 0.5);" +
                    "    float aspect = resolution.x / resolution.y;" +
                    "    vec2 centeredUV = (uv - center) * vec2(aspect, 1.0);" +
                    "    float dist = length(centeredUV);" +

                    // ========== 1. 基础晕影 (不再完全挖空中心) ==========
                    // 边缘是 1.0，中心是 0.0，但是曲线更平滑
                    "    float vignette = smoothstep(0.4, 1.2, dist);" +

                    // ========== 2. 全局扫描线 (Global Scanline) ==========
                    // 贯穿整个屏幕的横条纹，模拟旧显示器或数据流
                    "    float scanline = sin(uv.y * 400.0 + time * 5.0) * 0.03;" +

                    // ========== 3. 动态噪点 (Global Noise) ==========
                    // 全屏微弱的噪点，让画面变得"粗糙"
                    "    float noise = (random(uv + time) - 0.5) * 0.05;" +

                    // ========== 4. 颜色合成 ==========
                    // 基础色 (baseColor) 加上一点点噪点和扫描线
                    // 这里的关键：即使在中心，也有颜色！
                    "    vec3 finalColor = baseColor + vec3(noise);" +

                    // 受伤时：整体变红
                    "    if (isHurt > 0) {" +
                    "        finalColor = mix(finalColor, vec3(1.0, 0.2, 0.2), 0.5);" +
                    "    }" +

                    // ========== 5. Alpha 计算 (关键！) ==========
                    // 基础透明度：中心 0.15 (淡淡的一层)，边缘 0.9 (深色)
                    // 这样整个画面都被"统一"在同一种色调下
                    "    float baseAlpha = 0.15 + vignette * 0.75;" +

                    // 加上扫描线带来的透明度波动
                    "    float finalAlpha = baseAlpha + scanline;" +

                    // 整体强度控制
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
        if (mc.player == null) return;

        PlayerNarrativeState state = NarrativeLogicHandler.determineState(mc.player);

        if (state == PlayerNarrativeState.HUMAN_HIGH || state == PlayerNarrativeState.NONE) return;

        // 检查配置：如果对应状态的视觉效果被禁用，直接返回
        if (state == PlayerNarrativeState.HUMAN_LOW && !HumanityConfig.enableVisualDistortion) return;
        if (state == PlayerNarrativeState.BROKEN_GOD && !BrokenGodConfig.enableVisualOverlay) return;
        if (state == PlayerNarrativeState.SHAMBHALA && !ShambhalaConfig.enableVisualOverlay) return;

        ScaledResolution resolution = event.getResolution();
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        float time = (mc.player.ticksExisted + event.getPartialTicks()) / 20.0f;

        if (shaderProgram == -1 && state == PlayerNarrativeState.BROKEN_GOD) {
            initShader();
        }
        if (shambhalaShaderProgram == -1 && state == PlayerNarrativeState.SHAMBHALA) {
            initShambhalaShader();
        }

        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);

        GlStateManager.pushMatrix();

        switch (state) {
            case HUMAN_LOW:
                renderLowHumanityEffects(width, height, time);
                break;

            case BROKEN_GOD:
                renderBrokenGodEffects(width, height, time, mc.player);
                break;

            case SHAMBHALA:
                renderShambhalaEffects(width, height, time, mc.player);
                break;
        }

        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderBrokenGodEffects(int w, int h, float time, EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        boolean isHurt = player.hurtTime > 0;
        boolean isRandomGlitch = rand.nextFloat() < 0.002f;
        boolean triggerTearing = isHurt || isRandomGlitch;

        // 強度：保持通透
        float intensity = isHurt ? 1.5f : 0.8f;

        // ==========================================
        // 🎨 配色選擇區 (在這裡切換神性顏色！)
        // ==========================================

        // 方案 A: 【蒼白聖金 (Pale Electrum)】 -> 神聖、高貴、非電子感 (推薦!)
        //float r = 1.0f; float g = 0.92f; float b = 0.75f;

        // 方案 B: 【氧化青銅 (Verdigris)】 -> 古老、銅鏽、神秘
         float r = 0.6f; float g = 1.0f; float b = 0.9f;

        // 方案 C: 【純淨白金 (Platinum)】 -> 極度理性、冷酷、幾乎黑白
         //float r = 0.95f; float g = 0.95f; float b = 0.98f;

        // ==========================================

        if (isHurt) {
            // 受傷時：變為警示紅 (保持不變，因為紅色代表危險是通用的)
            r = 1.0f; g = 0.3f; b = 0.2f;

            // BIOS 觸發
            BiosLogRenderer.triggerDamageBurst(3); // 瞬間插入 3 條
        }
        if (rand.nextFloat() < 0.0005f) {
            BiosLogRenderer.render(time); // 偶發
        }

        // 1. Shader 渲染
        if (OpenGlHelper.shadersSupported && shaderProgram != -1) {
            ARBShaderObjects.glUseProgramObjectARB(shaderProgram);

            int timeLoc = ARBShaderObjects.glGetUniformLocationARB(shaderProgram, "time");
            ARBShaderObjects.glUniform1fARB(timeLoc, time);
            int intensityLoc = ARBShaderObjects.glGetUniformLocationARB(shaderProgram, "intensity");
            ARBShaderObjects.glUniform1fARB(intensityLoc, intensity);

            int colorLoc = ARBShaderObjects.glGetUniformLocationARB(shaderProgram, "baseColor");
            // 傳入我們選定的神性顏色
            ARBShaderObjects.glUniform3fARB(colorLoc, r, g, b);

            int resLoc = ARBShaderObjects.glGetUniformLocationARB(shaderProgram, "resolution");
            ARBShaderObjects.glUniform2fARB(resLoc, (float) w, (float) h);
            int hurtLoc = ARBShaderObjects.glGetUniformLocationARB(shaderProgram, "isHurt");
            ARBShaderObjects.glUniform1iARB(hurtLoc, isHurt ? 1 : 0);

            // 綁定暈影貼圖傳遞 UV
            mc.getTextureManager().bindTexture(OVERLAYS.get(PlayerNarrativeState.HUMAN_LOW));
            drawFullScreenQuad(w, h, false);

            ARBShaderObjects.glUseProgramObjectARB(0);
        }

        // 2. 畫面撕裂 (Glitch Tearing)
        if (triggerTearing) {
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            int glitchCount = isHurt ? 8 : 2;
            float tearIntensity = isHurt ? 40.0f : 10.0f;

            GlStateManager.disableTexture2D();

            for (int i = 0; i < glitchCount; i++) {
                float yStart = rand.nextInt(h);
                float yHeight = rand.nextInt(30) + 5;
                float xOffset = (rand.nextFloat() - 0.5f) * tearIntensity;

                // 撕裂條顏色：跟隨主色調，但更亮一點
                // 這樣撕裂時就不會突然變藍，而是變成金光/白光閃爍
                GlStateManager.color(r, g, b, 0.6f);

                buffer.begin(7, DefaultVertexFormats.POSITION);
                buffer.pos(0 + xOffset, yStart + yHeight, -90).endVertex();
                buffer.pos(w + xOffset, yStart + yHeight, -90).endVertex();
                buffer.pos(w + xOffset, yStart, -90).endVertex();
                buffer.pos(0 + xOffset, yStart, -90).endVertex();
                tessellator.draw();
            }
            GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);
        }

        // 3. 邊緣噪點 (Digital Noise)
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

                // 噪點顏色：跟隨主色調，稍微提亮
                // 這樣噪點就是金粉/銀粉，而不是電子雜訊
                float noiseR = Math.min(1.0f, r + 0.2f);
                float noiseG = Math.min(1.0f, g + 0.2f);
                float noiseB = Math.min(1.0f, b + 0.2f);

                buffer.pos(nx, ny, -90).color(noiseR, noiseG, noiseB, alpha * val).endVertex();
            }
        }
        tessellator.draw();
        GL11.glPointSize(1.0f);
        GlStateManager.enableTexture2D();

        // 4. BIOS 日誌
        BiosLogRenderer.render(time);
    }

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
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(0, height, z).tex(0, 1).endVertex();
            buffer.pos(width, height, z).tex(1, 1).endVertex();
            buffer.pos(width, 0, z).tex(1, 0).endVertex();
            buffer.pos(0, 0, z).tex(0, 0).endVertex();
        }
        tessellator.draw();
    }

    private void initShader() {
        if (!OpenGlHelper.shadersSupported) {
            shaderProgram = -1;
            return;
        }

        try {
            int vertexShader = ARBShaderObjects.glCreateShaderObjectARB(OpenGlHelper.GL_VERTEX_SHADER);
            ARBShaderObjects.glShaderSourceARB(vertexShader, VERTEX_SHADER);
            ARBShaderObjects.glCompileShaderARB(vertexShader);

            if (ARBShaderObjects.glGetObjectParameteriARB(vertexShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE) {
                return;
            }

            int fragmentShader = ARBShaderObjects.glCreateShaderObjectARB(OpenGlHelper.GL_FRAGMENT_SHADER);
            ARBShaderObjects.glShaderSourceARB(fragmentShader, FRAGMENT_SHADER);
            ARBShaderObjects.glCompileShaderARB(fragmentShader);

            if (ARBShaderObjects.glGetObjectParameteriARB(fragmentShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE) {
                return;
            }

            shaderProgram = ARBShaderObjects.glCreateProgramObjectARB();
            ARBShaderObjects.glAttachObjectARB(shaderProgram, vertexShader);
            ARBShaderObjects.glAttachObjectARB(shaderProgram, fragmentShader);
            ARBShaderObjects.glLinkProgramARB(shaderProgram);

            if (ARBShaderObjects.glGetObjectParameteriARB(shaderProgram, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
                return;
            }

        } catch (Exception e) {
            shaderProgram = -1;
        }
    }

    // ==================================================
    //  低人性渲染 - 简单的暗角效果
    // ==================================================
    private void renderLowHumanityEffects(int w, int h, float time) {
        Minecraft mc = Minecraft.getMinecraft();
        ResourceLocation tex = OVERLAYS.get(PlayerNarrativeState.HUMAN_LOW);
        if (tex != null) {
            mc.getTextureManager().bindTexture(tex);
            // 呼吸效果
            float breath = 0.6f + 0.15f * (float) Math.sin(time * 1.5f);
            // 暗红色调
            GlStateManager.color(0.3f, 0.1f, 0.1f, breath);
            drawFullScreenQuad(w, h, true);
        }
    }

    // ==================================================
    //  香巴拉渲染 - 金色齿轮 + 金色边缘渐变
    // ==================================================
    // ==================================================
    //  Erica 修复版：解决齿轮隐形与闪烁问题
    // ==================================================

    // ==================================================
    //  Erica 最终修复版：绝对防御版齿轮渲染
    // ==================================================

    // ==================================================
    //  Erica 最终修正：使用 ShambhalaAscensionOverlay 原版齿轮
    // ==================================================

    private void renderShambhalaEffects(int w, int h, float time, EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();

        // 1. 画背景光晕 (保持不变)
        if (OpenGlHelper.shadersSupported && shambhalaShaderProgram != -1) {
            ARBShaderObjects.glUseProgramObjectARB(shambhalaShaderProgram);
            ARBShaderObjects.glUniform1fARB(ARBShaderObjects.glGetUniformLocationARB(shambhalaShaderProgram, "time"), time);
            ARBShaderObjects.glUniform2fARB(ARBShaderObjects.glGetUniformLocationARB(shambhalaShaderProgram, "resolution"), (float) w, (float) h);
            mc.getTextureManager().bindTexture(OVERLAYS.get(PlayerNarrativeState.HUMAN_LOW));
            drawFullScreenQuad(w, h, false);
            ARBShaderObjects.glUseProgramObjectARB(0);
        } else {
            ResourceLocation tex = OVERLAYS.get(PlayerNarrativeState.HUMAN_LOW);
            if (tex != null) {
                mc.getTextureManager().bindTexture(tex);
                float breath = 0.7f + 0.1f * (float) Math.sin(time * 0.5f);
                GlStateManager.color(1.0f, 0.85f, 0.4f, 0.35f * breath);
                drawFullScreenQuad(w, h, true);
            }
        }

        // 2. 状态准备
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.bindTexture(0);

        // 关键：为了还原动画里的效果，我们需要开启线宽，让线条更清晰
        GL11.glLineWidth(2.0F);

        // 3. 画齿轮
        updateGears(w, h, time);
        renderGears(w, h, time);

        // 4. 恢复状态
        GL11.glLineWidth(1.0F); // 恢复线宽
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
    }

    /**
     * 绘制单个齿轮 - 完美复刻 ShambhalaAscensionOverlay 的逻辑
     * (空心线框风格，带有方正的齿牙)
     */
    private void drawGear(float x, float y, float size, float rotation, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 这里的 radius 对应动画里的 radius
        float radius = size * 0.6f;
        int teeth = 12; // 保持 12 个齿
        double zLevel = 0.0D;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.rotate(rotation, 0, 0, 1);

        // 设置颜色：金色
        GlStateManager.color(1.0f, 0.84f, 0.39f, alpha);

        // --- 1. 齿轮外圈 (带齿) ---
        // 使用 GL_LINE_LOOP 画空心线框
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);

        // 逻辑源自 drawGearShape: segments = teeth * 4
        int segments = teeth * 4;
        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;

            // 动画里的齿形逻辑：前两个点是齿顶，后两个点是齿根
            // (i % 4 < 2) ? radius : radius * 0.75f
            float toothRadius = (i % 4 < 2) ? radius : radius * 0.75f;

            buffer.pos(Math.cos(angle) * toothRadius, Math.sin(angle) * toothRadius, zLevel).endVertex();
        }
        tessellator.draw();

        // --- 2. 中心孔 ---
        // 也是一个空心圆圈
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        float holeRadius = radius * 0.3f;
        for (int i = 0; i <= 32; i++) {
            double angle = (Math.PI * 2 * i) / 32;
            buffer.pos(Math.cos(angle) * holeRadius, Math.sin(angle) * holeRadius, zLevel).endVertex();
        }
        tessellator.draw();

        GlStateManager.popMatrix();
    }

    /**
     * 绘制单个齿轮 - 纯几何体版 (最稳！)
     */


    /**
     * 更新齿轮状态 - 提高生成率，拒绝空场
     */
    private void updateGears(int w, int h, float time) {
        // 统计活跃齿轮
        boolean hasActive = false;
        for(boolean active : gearActive) { if(active) hasActive = true; }

        // 关键修复 B：如果是空的，强制提高生成率到 100%，否则保持 5%
        // 这样你一进状态就能看到齿轮，不用干等
        float spawnChance = hasActive ? 0.05f : 1.0f;

        if (rand.nextFloat() < spawnChance) {
            for (int i = 0; i < MAX_GEARS; i++) {
                if (!gearActive[i]) {
                    // 随机位置优化：尽量往屏幕中间靠一点点，防止只露出一半
                    gearX[i] = rand.nextFloat() * w;
                    gearY[i] = rand.nextFloat() * h;

                    gearSize[i] = 30 + rand.nextFloat() * 60; // 大小适中
                    gearRotation[i] = rand.nextFloat() * 360;
                    gearAlpha[i] = 0.0f;
                    gearLifetime[i] = 3.0f + rand.nextFloat() * 3.0f; // 3-6秒
                    gearActive[i] = true;
                    break;
                }
            }
        }

        float deltaTime = 0.05f;
        for (int i = 0; i < MAX_GEARS; i++) {
            if (gearActive[i]) {
                gearLifetime[i] -= deltaTime;
                gearRotation[i] += deltaTime * 30; // 转动

                // 淡入淡出逻辑
                if (gearLifetime[i] > 2.0f) {
                    // 淡入稍微快一点，最大透明度设为 0.9，更亮！
                    gearAlpha[i] = Math.min(0.9f, gearAlpha[i] + deltaTime * 0.8f);
                } else if (gearLifetime[i] < 1.0f) {
                    gearAlpha[i] = Math.max(0.0f, gearAlpha[i] - deltaTime * 0.5f);
                }

                if (gearLifetime[i] <= 0) {
                    gearActive[i] = false;
                }
            }
        }
    }

    /**
     * 绘制单个齿轮 - 关键：Z轴分层
     */


    /**
     * 更新齿轮状态 - 随机生成新齿轮
     */

    /**
     * 渲染金色齿轮
     */
    private void renderGears(int w, int h, float time) {
        GlStateManager.disableTexture2D();

        for (int i = 0; i < MAX_GEARS; i++) {
            if (gearActive[i] && gearAlpha[i] > 0.01f) {
                drawGear(gearX[i], gearY[i], gearSize[i], gearRotation[i], gearAlpha[i]);
            }
        }

        GlStateManager.enableTexture2D();
    }

    /**
     * 绘制单个齿轮
     */

    /**
     * 初始化香巴拉着色器
     */
    private void initShambhalaShader() {
        if (!OpenGlHelper.shadersSupported) {
            shambhalaShaderProgram = -1;
            return;
        }

        try {
            int vertexShader = ARBShaderObjects.glCreateShaderObjectARB(OpenGlHelper.GL_VERTEX_SHADER);
            ARBShaderObjects.glShaderSourceARB(vertexShader, VERTEX_SHADER);
            ARBShaderObjects.glCompileShaderARB(vertexShader);

            if (ARBShaderObjects.glGetObjectParameteriARB(vertexShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE) {
                shambhalaShaderProgram = -1;
                return;
            }

            int fragmentShader = ARBShaderObjects.glCreateShaderObjectARB(OpenGlHelper.GL_FRAGMENT_SHADER);
            ARBShaderObjects.glShaderSourceARB(fragmentShader, SHAMBHALA_FRAGMENT_SHADER);
            ARBShaderObjects.glCompileShaderARB(fragmentShader);

            if (ARBShaderObjects.glGetObjectParameteriARB(fragmentShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE) {
                shambhalaShaderProgram = -1;
                return;
            }

            shambhalaShaderProgram = ARBShaderObjects.glCreateProgramObjectARB();
            ARBShaderObjects.glAttachObjectARB(shambhalaShaderProgram, vertexShader);
            ARBShaderObjects.glAttachObjectARB(shambhalaShaderProgram, fragmentShader);
            ARBShaderObjects.glLinkProgramARB(shambhalaShaderProgram);

            if (ARBShaderObjects.glGetObjectParameteriARB(shambhalaShaderProgram, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
                shambhalaShaderProgram = -1;
                return;
            }

        } catch (Exception e) {
            shambhalaShaderProgram = -1;
        }
    }
}