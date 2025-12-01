package com.moremod.client.gui;

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
    private void renderShambhalaEffects(int w, int h, float time, EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 1. 金色边缘渐变（使用Shader）
        if (OpenGlHelper.shadersSupported && shambhalaShaderProgram != -1) {
            ARBShaderObjects.glUseProgramObjectARB(shambhalaShaderProgram);

            int timeLoc = ARBShaderObjects.glGetUniformLocationARB(shambhalaShaderProgram, "time");
            ARBShaderObjects.glUniform1fARB(timeLoc, time);

            int resLoc = ARBShaderObjects.glGetUniformLocationARB(shambhalaShaderProgram, "resolution");
            ARBShaderObjects.glUniform2fARB(resLoc, (float) w, (float) h);

            mc.getTextureManager().bindTexture(OVERLAYS.get(PlayerNarrativeState.HUMAN_LOW));
            drawFullScreenQuad(w, h, false);

            ARBShaderObjects.glUseProgramObjectARB(0);
        } else {
            // Shader不支持时的备用：简单的金色晕影
            ResourceLocation tex = OVERLAYS.get(PlayerNarrativeState.HUMAN_LOW);
            if (tex != null) {
                mc.getTextureManager().bindTexture(tex);
                float breath = 0.7f + 0.1f * (float) Math.sin(time * 0.5f);
                GlStateManager.color(1.0f, 0.85f, 0.4f, 0.35f * breath);
                drawFullScreenQuad(w, h, true);
            }
        }

        // 2. 更新并渲染金色齿轮
        updateGears(w, h, time);
        renderGears(w, h, time);
    }

    /**
     * 更新齿轮状态 - 随机生成新齿轮
     */
    private void updateGears(int w, int h, float time) {
        // 偶尔生成新齿轮 (约每2秒一个)
        if (rand.nextFloat() < 0.01f) {
            for (int i = 0; i < MAX_GEARS; i++) {
                if (!gearActive[i]) {
                    gearX[i] = rand.nextFloat() * w;
                    gearY[i] = rand.nextFloat() * h;
                    gearSize[i] = 30 + rand.nextFloat() * 60; // 30-90像素
                    gearRotation[i] = rand.nextFloat() * 360;
                    gearAlpha[i] = 0.0f;
                    gearLifetime[i] = 3.0f + rand.nextFloat() * 3.0f; // 3-6秒
                    gearActive[i] = true;
                    break;
                }
            }
        }

        // 更新齿轮状态
        float deltaTime = 0.05f; // 假设约20fps
        for (int i = 0; i < MAX_GEARS; i++) {
            if (gearActive[i]) {
                gearLifetime[i] -= deltaTime;
                gearRotation[i] += deltaTime * 30; // 慢速旋转

                // 淡入淡出
                if (gearLifetime[i] > 2.0f) {
                    // 淡入阶段
                    gearAlpha[i] = Math.min(0.6f, gearAlpha[i] + deltaTime * 0.5f);
                } else if (gearLifetime[i] < 1.0f) {
                    // 淡出阶段
                    gearAlpha[i] = Math.max(0.0f, gearAlpha[i] - deltaTime * 0.5f);
                }

                // 生命周期结束
                if (gearLifetime[i] <= 0) {
                    gearActive[i] = false;
                }
            }
        }
    }

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
    private void drawGear(float x, float y, float size, float rotation, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int teeth = 12; // 齿轮齿数
        float innerRadius = size * 0.5f;
        float outerRadius = size * 0.7f;
        float toothHeight = size * 0.15f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.rotate(rotation, 0, 0, 1);

        // 金色: RGB(255, 215, 100) = (1.0, 0.84, 0.39)
        float r = 1.0f, g = 0.84f, b = 0.39f;

        // 绘制齿轮主体（圆环）
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int t = 0; t <= 72; t++) {
            float angle = (float) (t * Math.PI * 2 / 72);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            buffer.pos(cos * innerRadius * 0.6f, sin * innerRadius * 0.6f, -90).color(r, g, b, alpha * 0.3f).endVertex();
            buffer.pos(cos * innerRadius, sin * innerRadius, -90).color(r, g, b, alpha).endVertex();
        }
        tessellator.draw();

        // 绘制齿轮齿
        for (int t = 0; t < teeth; t++) {
            float angle1 = (float) (t * Math.PI * 2 / teeth);
            float angle2 = (float) ((t + 0.3) * Math.PI * 2 / teeth);
            float angle3 = (float) ((t + 0.7) * Math.PI * 2 / teeth);
            float angle4 = (float) ((t + 1) * Math.PI * 2 / teeth);

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            // 齿的内边
            buffer.pos(Math.cos(angle1) * outerRadius, Math.sin(angle1) * outerRadius, -90).color(r, g, b, alpha).endVertex();
            buffer.pos(Math.cos(angle2) * (outerRadius + toothHeight), Math.sin(angle2) * (outerRadius + toothHeight), -90).color(r, g, b, alpha * 0.8f).endVertex();
            buffer.pos(Math.cos(angle3) * (outerRadius + toothHeight), Math.sin(angle3) * (outerRadius + toothHeight), -90).color(r, g, b, alpha * 0.8f).endVertex();
            buffer.pos(Math.cos(angle4) * outerRadius, Math.sin(angle4) * outerRadius, -90).color(r, g, b, alpha).endVertex();
            tessellator.draw();
        }

        // 中心圆点
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(0, 0, -90).color(r, g, b, alpha).endVertex();
        for (int t = 0; t <= 24; t++) {
            float angle = (float) (t * Math.PI * 2 / 24);
            buffer.pos(Math.cos(angle) * innerRadius * 0.3f, Math.sin(angle) * innerRadius * 0.3f, -90).color(r, g, b, alpha * 0.5f).endVertex();
        }
        tessellator.draw();

        GlStateManager.popMatrix();
    }

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