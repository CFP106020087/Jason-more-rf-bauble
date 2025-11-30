package com.moremod.client.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 香巴拉：永恒圆满 (Shambhala Ascension Overlay)
 *
 * 与破碎之神形成对偶：
 * - 破碎之神：感官消逝、记忆抹除、冰冷机械、神性降临
 * - 香巴拉：感官升华、和谐共振、温暖机械、永恒圆满
 *
 * 核心概念：与世独立、不受侵扰、完美造物、永恒、圆满
 * 色调：金色、青色、暖白
 */
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class ShambhalaAscensionOverlay extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();

    // ==================== 时间控制 ====================
    private static boolean isAnimating = false;
    private static int animationTick = 0;

    // 30秒 = 600 ticks
    private static final int TOTAL_DURATION = 600;
    private static final int FADE_OUT_DURATION = 40;

    // 阶段划分
    private static final int PHASE1_END = 80;        // 4s - 觉醒：温暖扩散
    private static final int PHASE2_END = 200;       // 6s - 绽放：机械之花
    private static final int PHASE3_END = 360;       // 8s - 涅磐：曼荼罗
    private static final int PHASE4_END = TOTAL_DURATION; // 12s - 圆满：永恒守护

    // Phase 4 子阶段
    private static final int PHASE4_BLOOM_END = 420;   // 3s - 莲花绽放
    private static final int PHASE4_RADIATE_END = 520; // 5s - 光辉放射
    private static final int PHASE4_PEACE_END = 560;   // 2s - 寂静
    // 560-600: 2s - 归于平静

    // ==================== 全局状态 ====================
    private static float globalFadeAlpha = 1.0f;

    // ==================== Phase 1 状态 ====================
    private static float warmthSpread = 0;
    private static float goldenGlow = 0;
    private static final List<WarmthParticle> warmthParticles = new ArrayList<>();

    // ==================== Phase 2 状态 ====================
    private static final List<MemoryFragment> harmonyFragmentsList = new ArrayList<>();
    private static final List<GoldenGear> goldenGears = new ArrayList<>();
    private static float gearHarmony = 0;
    private static float harmonyAura = 0;

    // "此刻，永恒" 状态
    private static float vowProgress = 0;
    private static float[] vowCharGlow = new float[5];
    private static boolean vowComplete = false;

    // ==================== Phase 3 状态 ====================
    private static float mandalaRotation = 0;
    private static float mandalaScale = 0;
    private static float enlightenmentProgress = 0;
    private static final List<String> enlightenmentLogs = new ArrayList<>();
    private static int lastLogTick = 0;

    // ==================== Phase 4 状态 ====================
    private static float lotusBloom = 0;
    private static float[] petalAngles = new float[8];
    private static float[] petalExtend = new float[8];
    private static float radianceIntensity = 0;
    private static float[] holyRingRadii = new float[5];
    private static float[] holyRingAngles = new float[5];
    private static final List<GoldenMote> goldenMotes = new ArrayList<>();

    // 心跳系统（与破碎之神相反——越来越强）
    private static float heartbeatPhase = 0;
    private static float heartbeatStrength = 0.3f;

    // 最终光辉
    private static float finalRadiance = 0;
    private static float peaceProgress = 0;

    // ==================== 剧本 ====================
    enum LineType { HUMAN, SYSTEM, ENLIGHTENED, PEACE }
    private static final List<MonologueLine> SCRIPT = new ArrayList<>();

    private static final String[] SACRED_SYMBOLS = {
            "☸", "✿", "❀", "◎", "○", "◇", "△", "☯", "卍", "ॐ",
            "⚙", "⟡", "✦", "✧", "❋", "✾", "❁", "✤", "✥", "❃"
    };

    private static final String[] HARMONY_FRAGMENTS = {
            "和谐", "永恒", "圆满", "光辉", "美丽", "理想", "完美", "人性"
    };

    private static final String VOW_TEXT = "此刻，永恒";

    static {
        SCRIPT.add(new MonologueLine(5, 50, LineType.HUMAN, "光芒... 如此温暖..."));
        SCRIPT.add(new MonologueLine(60, 30, LineType.SYSTEM, "[ 齿轮共振: 和谐 ]"));
        SCRIPT.add(new MonologueLine(100, 40, LineType.SYSTEM, ">>> 完美造物协议启动"));
        SCRIPT.add(new MonologueLine(200, 35, LineType.SYSTEM, ">>> 人性核心: 圆满"));
        SCRIPT.add(new MonologueLine(370, 40, LineType.ENLIGHTENED, ""));
        SCRIPT.add(new MonologueLine(550, 50, LineType.PEACE, "香巴拉"));
    }

    // ==================== 公共接口 ====================

    public static void startAnimation() {
        isAnimating = true;
        animationTick = 0;

        // 重置所有状态
        globalFadeAlpha = 1.0f;
        warmthSpread = 0;
        goldenGlow = 0;
        gearHarmony = 0;
        harmonyAura = 0;
        vowProgress = 0;
        vowComplete = false;
        mandalaRotation = 0;
        mandalaScale = 0;
        enlightenmentProgress = 0;
        lastLogTick = 0;
        lotusBloom = 0;
        radianceIntensity = 0;
        heartbeatPhase = 0;
        heartbeatStrength = 0.3f;
        finalRadiance = 0;
        peaceProgress = 0;

        warmthParticles.clear();
        harmonyFragmentsList.clear();
        goldenGears.clear();
        enlightenmentLogs.clear();
        goldenMotes.clear();

        for (int i = 0; i < 5; i++) {
            vowCharGlow[i] = 0;
        }

        for (int i = 0; i < 8; i++) {
            petalAngles[i] = i * 45;
            petalExtend[i] = 0;
        }

        for (int i = 0; i < holyRingAngles.length; i++) {
            holyRingAngles[i] = random.nextFloat() * 360;
            holyRingRadii[i] = 0;
        }
    }

    public static boolean isAnimating() {
        return isAnimating;
    }

    // ==================== TICK 逻辑 ====================

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isAnimating) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.isGamePaused()) return;

        animationTick++;

        if (animationTick > TOTAL_DURATION) {
            isAnimating = false;
            animationTick = 0;
            return;
        }

        // 全局心跳（贯穿全程，越来越强）
        tickHeartbeat();

        if (animationTick <= PHASE1_END) {
            tickPhase1();
        } else if (animationTick <= PHASE2_END) {
            tickPhase2();
        } else if (animationTick <= PHASE3_END) {
            tickPhase3();
        } else {
            tickPhase4();
        }
    }

    private static void tickHeartbeat() {
        float globalProgress = (float) animationTick / TOTAL_DURATION;
        heartbeatStrength = 0.3f + globalProgress * 0.7f; // 从0.3到1.0

        heartbeatPhase += 0.05f * (1.0f + globalProgress * 0.5f);
        if (heartbeatPhase > 1.0f) heartbeatPhase -= 1.0f;
    }

    private static void tickPhase1() {
        float progress = (float) animationTick / PHASE1_END;
        warmthSpread = easeOutQuart(progress);
        goldenGlow = warmthSpread * 0.6f;

        // 生成温暖粒子
        if (random.nextFloat() < 0.2f * progress) {
            ScaledResolution res = new ScaledResolution(mc);
            WarmthParticle particle = new WarmthParticle();
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float dist = res.getScaledWidth() * 0.3f * (1 - progress);
            particle.x = res.getScaledWidth() / 2.0f + (float) Math.cos(angle) * dist;
            particle.y = res.getScaledHeight() / 2.0f + (float) Math.sin(angle) * dist;
            particle.size = 5 + random.nextFloat() * 15;
            particle.life = 1.0f;
            particle.vx = (res.getScaledWidth() / 2.0f - particle.x) * 0.02f;
            particle.vy = (res.getScaledHeight() / 2.0f - particle.y) * 0.02f;
            warmthParticles.add(particle);
        }

        Iterator<WarmthParticle> iter = warmthParticles.iterator();
        while (iter.hasNext()) {
            WarmthParticle p = iter.next();
            p.x += p.vx;
            p.y += p.vy;
            p.life -= 0.015f;
            p.size *= 1.02f;
            if (p.life <= 0) iter.remove();
        }
        while (warmthParticles.size() > 30) warmthParticles.remove(0);
    }

    private static void tickPhase2() {
        float progress = (float) (animationTick - PHASE1_END) / (PHASE2_END - PHASE1_END);
        gearHarmony = easeInOutQuad(progress);
        harmonyAura = progress;

        // 生成金色齿轮
        if (random.nextFloat() < 0.08f && goldenGears.size() < 12) {
            ScaledResolution res = new ScaledResolution(mc);
            GoldenGear gear = new GoldenGear();
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float dist = 50 + random.nextFloat() * 100;
            gear.x = res.getScaledWidth() / 2.0f + (float) Math.cos(angle) * dist;
            gear.y = res.getScaledHeight() / 2.0f + (float) Math.sin(angle) * dist;
            gear.radius = 15 + random.nextFloat() * 25;
            gear.teeth = 6 + random.nextInt(6);
            gear.rotation = random.nextFloat() * 360;
            gear.rotationSpeed = (random.nextFloat() - 0.5f) * 2;
            gear.alpha = 0;
            gear.targetAlpha = 0.6f + random.nextFloat() * 0.3f;
            goldenGears.add(gear);
        }

        for (GoldenGear gear : goldenGears) {
            gear.rotation += gear.rotationSpeed;
            gear.alpha += (gear.targetAlpha - gear.alpha) * 0.05f;
        }

        // 生成和谐碎片
        if (random.nextFloat() < 0.05f && harmonyFragmentsList.size() < 8) {
            ScaledResolution res = new ScaledResolution(mc);
            MemoryFragment mem = new MemoryFragment();
            mem.x = random.nextFloat() * res.getScaledWidth();
            mem.y = random.nextFloat() * res.getScaledHeight();
            mem.text = HARMONY_FRAGMENTS[random.nextInt(HARMONY_FRAGMENTS.length)];
            mem.life = 1.0f;
            mem.protected_ = true;
            mem.glowPhase = random.nextFloat();
            harmonyFragmentsList.add(mem);
        }

        Iterator<MemoryFragment> memIter = harmonyFragmentsList.iterator();
        while (memIter.hasNext()) {
            MemoryFragment mem = memIter.next();
            mem.glowPhase += 0.05f;
            mem.life -= 0.005f;
            if (mem.life <= 0) memIter.remove();
        }

        // "此刻，永恒" 核心文字
        if (progress > 0.3f && progress < 0.9f) {
            vowProgress = (progress - 0.3f) / 0.6f;
            for (int i = 0; i < 5; i++) {
                float charProgress = Math.max(0, (vowProgress - i * 0.15f) / 0.3f);
                vowCharGlow[i] = Math.min(1.0f, charProgress);
            }
        } else if (progress >= 0.9f) {
            vowComplete = true;
            for (int i = 0; i < 5; i++) {
                vowCharGlow[i] = 1.0f;
            }
        }
    }

    private static void tickPhase3() {
        float progress = (float) (animationTick - PHASE2_END) / (PHASE3_END - PHASE2_END);
        mandalaRotation += 0.5f + progress * 1.5f;
        mandalaScale = easeOutQuart(Math.min(progress * 1.5f, 1.0f));
        enlightenmentProgress = progress;

        // 启蒙日志
        int tickInterval = progress > 0.7f ? 2 : 4;
        if (animationTick - lastLogTick >= tickInterval) {
            enlightenmentLogs.add(generateEnlightenmentLog(progress));
            lastLogTick = animationTick;
            while (enlightenmentLogs.size() > 12) enlightenmentLogs.remove(0);
        }

        // 圣环展开
        for (int i = 0; i < holyRingRadii.length; i++) {
            float ringProgress = Math.max(0, (progress - i * 0.1f) / 0.5f);
            float targetRadius = 40 + i * 35;
            holyRingRadii[i] += (targetRadius * Math.min(ringProgress, 1.0f) - holyRingRadii[i]) * 0.1f;
            holyRingAngles[i] += (i % 2 == 0 ? 0.5f : -0.5f) * (1 + progress);
        }
    }

    private static void tickPhase4() {
        int phase4Tick = animationTick - PHASE3_END;

        if (animationTick <= PHASE4_BLOOM_END) {
            // 莲花绽放
            float bloomProgress = (float) phase4Tick / (PHASE4_BLOOM_END - PHASE3_END);
            lotusBloom = easeOutQuart(bloomProgress);

            for (int i = 0; i < 8; i++) {
                float petalDelay = i * 0.08f;
                float petalProgress = Math.max(0, (bloomProgress - petalDelay) / 0.6f);
                petalExtend[i] = easeOutBack(Math.min(petalProgress, 1.0f));
            }

            radianceIntensity = bloomProgress * 0.5f;

        } else if (animationTick <= PHASE4_RADIATE_END) {
            // 光辉放射
            float radiateProgress = (float) (animationTick - PHASE4_BLOOM_END) / (PHASE4_RADIATE_END - PHASE4_BLOOM_END);
            radianceIntensity = 0.5f + radiateProgress * 0.5f;

            // 生成金色光点
            if (random.nextFloat() < 0.15f * radianceIntensity) {
                ScaledResolution res = new ScaledResolution(mc);
                GoldenMote mote = new GoldenMote();
                float angle = random.nextFloat() * (float) Math.PI * 2;
                float dist = 30 + random.nextFloat() * 50;
                mote.x = res.getScaledWidth() / 2.0f + (float) Math.cos(angle) * dist;
                mote.y = res.getScaledHeight() / 2.0f + (float) Math.sin(angle) * dist;
                mote.vx = (float) Math.cos(angle) * (2 + random.nextFloat() * 3);
                mote.vy = (float) Math.sin(angle) * (2 + random.nextFloat() * 3);
                mote.life = 1.0f;
                mote.size = 2 + random.nextFloat() * 4;
                goldenMotes.add(mote);
            }

            // 圣环脉动
            float pulse = (float) Math.sin(animationTick * 0.1f) * 0.1f;
            for (int i = 0; i < holyRingRadii.length; i++) {
                holyRingRadii[i] *= (1.0f + pulse * (i + 1) * 0.02f);
                holyRingAngles[i] += (i % 2 == 0 ? 1.0f : -1.0f);
            }

        } else if (animationTick <= PHASE4_PEACE_END) {
            // 寂静
            float peaceStartProgress = (float) (animationTick - PHASE4_RADIATE_END) / (PHASE4_PEACE_END - PHASE4_RADIATE_END);
            finalRadiance = easeInQuad(peaceStartProgress);

        } else {
            // 归于平静
            float endProgress = (float) (animationTick - PHASE4_PEACE_END) / (PHASE4_END - PHASE4_PEACE_END);
            peaceProgress = endProgress;
            finalRadiance = 1.0f - endProgress * 0.7f;

            // 所有元素慢慢消散
            radianceIntensity *= 0.98f;
            for (int i = 0; i < holyRingRadii.length; i++) {
                holyRingRadii[i] *= 0.99f;
            }
        }

        // 更新金色光点
        Iterator<GoldenMote> moteIter = goldenMotes.iterator();
        while (moteIter.hasNext()) {
            GoldenMote mote = moteIter.next();
            mote.x += mote.vx;
            mote.y += mote.vy;
            mote.vx *= 0.98f;
            mote.vy *= 0.98f;
            mote.life -= 0.015f;
            if (mote.life <= 0) moteIter.remove();
        }
        while (goldenMotes.size() > 60) goldenMotes.remove(0);

        // 全局淡出
        int fadeStartTick = TOTAL_DURATION - FADE_OUT_DURATION;
        if (animationTick > fadeStartTick) {
            float fadeProgress = (float) (animationTick - fadeStartTick) / (float) FADE_OUT_DURATION;
            globalFadeAlpha = 1.0f - easeInQuad(fadeProgress);
        } else {
            globalFadeAlpha = 1.0f;
        }
    }

    // ==================== 渲染逻辑 ====================

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isAnimating) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (animationTick > TOTAL_DURATION) return;

        ScaledResolution res = event.getResolution();
        int w = res.getScaledWidth();
        int h = res.getScaledHeight();

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 800);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableAlpha();

        renderAtmosphere(w, h);

        if (animationTick <= PHASE1_END) {
            renderPhase1(w, h);
        } else if (animationTick <= PHASE2_END) {
            renderPhase2(w, h);
        } else if (animationTick <= PHASE3_END) {
            renderPhase3(w, h);
        } else {
            renderPhase4(w, h);
        }

        // 全局心跳效果
        renderHeartbeat(w, h);

        renderMonologue(w, h);

        // 最终淡出
        if (globalFadeAlpha < 1.0f) {
            float curtainAlpha = 1.0f - globalFadeAlpha;
            // 淡出到金色而非黑色
            int goldCurtain = applyAlpha(0x1A1408, curtainAlpha);
            Gui.drawRect(0, 0, w, h, goldCurtain);
        }

        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderAtmosphere(int w, int h) {
        int color;
        if (animationTick <= PHASE1_END) {
            float progress = (float) animationTick / PHASE1_END;
            // 从正常到温暖金色
            int r = (int) (10 + progress * 30);
            int g = (int) (10 + progress * 20);
            int b = (int) (15 + progress * 5);
            color = 0xFF000000 | (r << 16) | (g << 8) | b;
        } else if (animationTick <= PHASE2_END) {
            color = 0xFF1E1408; // 温暖深棕
        } else if (animationTick <= PHASE3_END) {
            color = 0xFF0A0A12; // 深蓝黑
        } else if (animationTick <= PHASE4_PEACE_END) {
            // 逐渐明亮
            float progress = (float) (animationTick - PHASE3_END) / (PHASE4_PEACE_END - PHASE3_END);
            int brightness = (int) (10 + progress * 20);
            color = 0xFF000000 | (brightness << 16) | (brightness << 8) | (brightness + 5);
        } else {
            // 归于平静 - 淡金色
            int gold = (int) (30 + peaceProgress * 20);
            color = 0xFF000000 | (gold << 16) | ((int)(gold * 0.9f) << 8) | (int)(gold * 0.6f);
        }
        Gui.drawRect(0, 0, w, h, color);
    }

    // ==================== Phase 1 渲染 ====================

    private static void renderPhase1(int w, int h) {
        float centerX = w / 2.0f;
        float centerY = h / 2.0f;

        // 中心温暖光晕
        float glowRadius = warmthSpread * 200;
        int warmColor = applyAlpha(0xFFD080, goldenGlow * 0.4f);
        drawSoftCircle(centerX, centerY, glowRadius, warmColor);

        // 内核光点
        int coreColor = applyAlpha(0xFFFFE0, goldenGlow * 0.8f);
        drawSoftCircle(centerX, centerY, 20 + warmthSpread * 30, coreColor);

        // 温暖粒子
        for (WarmthParticle p : warmthParticles) {
            int pColor = applyAlpha(0xFFD700, p.life * 0.6f);
            drawSoftCircle(p.x, p.y, p.size, pColor);
        }

        // 边缘温暖渐晕
        if (warmthSpread > 0.5f) {
            float vignetteAlpha = (warmthSpread - 0.5f) * 0.3f;
            drawWarmVignette(w, h, vignetteAlpha);
        }
    }

    // ==================== Phase 2 渲染 ====================

    private static void renderPhase2(int w, int h) {
        float centerX = w / 2.0f;
        float centerY = h / 2.0f;

        // 和谐光环
        if (harmonyAura > 0.1f) {
            float auraRadius = 80 + harmonyAura * 60;
            int auraColor = applyAlpha(0x80CFFF, harmonyAura * 0.2f);
            drawSoftCircle(centerX, centerY, auraRadius, auraColor);
        }

        // 金色齿轮
        for (GoldenGear gear : goldenGears) {
            renderGoldenGear(gear);
        }

        // 和谐碎片
        for (MemoryFragment mem : harmonyFragmentsList) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(mem.x, mem.y, 0);

            float glow = 0.5f + 0.5f * (float) Math.sin(mem.glowPhase);
            int textColor = applyAlpha(0xFFD700, mem.life * (0.6f + glow * 0.4f));

            // 光晕
            int circleColor = applyAlpha(0x80CFFF, mem.life * glow * 0.3f);
            float textWidth = mc.fontRenderer.getStringWidth(mem.text);
            drawSoftCircle(textWidth / 2, 4, textWidth * 0.8f, circleColor);

            mc.fontRenderer.drawString(mem.text, 0, 0, textColor, false);
            GlStateManager.popMatrix();
        }

        // "此刻，永恒" 核心文字
        if (vowProgress > 0) {
            renderVow(w, h);
        }
    }

    private static void renderVow(int w, int h) {
        GlStateManager.pushMatrix();

        float centerX = w / 2.0f;
        float centerY = h / 2.0f + 40;
        float scale = 2.5f;

        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(scale, scale, 1);

        String text = VOW_TEXT;
        float totalWidth = 0;
        float[] charWidths = new float[5];
        for (int i = 0; i < 5; i++) {
            charWidths[i] = mc.fontRenderer.getCharWidth(text.charAt(i));
            totalWidth += charWidths[i];
        }

        float x = -totalWidth / 2;
        for (int i = 0; i < 5; i++) {
            float charGlow = vowCharGlow[i];

            // 基础颜色渐变（从灰到金）
            int r = (int) (100 + charGlow * 155);
            int g = (int) (100 + charGlow * 115);
            int b = (int) (100 - charGlow * 50);
            int baseColor = (r << 16) | (g << 8) | b;

            float alpha = 0.3f + charGlow * 0.7f;
            int color = applyAlpha(baseColor, alpha);

            // 发光效果
            if (charGlow > 0.5f) {
                int glowColor = applyAlpha(0xFFD700, (charGlow - 0.5f) * 0.4f);
                drawSoftCircle(x + charWidths[i] / 2, 0, 15 * charGlow, glowColor);
            }

            mc.fontRenderer.drawString(String.valueOf(text.charAt(i)), x, -4, color, false);
            x += charWidths[i];
        }

        // 完成时的光环
        if (vowComplete) {
            int completeGlow = applyAlpha(0xFFD700, 0.3f + 0.1f * (float) Math.sin(animationTick * 0.1f));
            drawSoftCircle(0, 0, totalWidth * scale * 0.8f, completeGlow);
        }

        GlStateManager.popMatrix();
    }

    private static void renderGoldenGear(GoldenGear gear) {
        if (gear.alpha < 0.01f) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(gear.x, gear.y, 0);
        GlStateManager.rotate(gear.rotation, 0, 0, 1);

        int gearColor = applyAlpha(0xFFD700, gear.alpha * 0.7f);
        drawGearShape(0, 0, gear.radius, gear.teeth, gearColor);

        GlStateManager.popMatrix();
    }

    // ==================== Phase 3 渲染 ====================

    private static void renderPhase3(int w, int h) {
        float centerX = w / 2.0f;
        float centerY = h / 2.0f;

        // 曼荼罗
        if (mandalaScale > 0.1f) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(centerX, centerY, 0);
            GlStateManager.rotate(mandalaRotation, 0, 0, 1);
            GlStateManager.scale(mandalaScale, mandalaScale, 1);
            renderMandala();
            GlStateManager.popMatrix();
        }

        // 圣环
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0);
        renderHolyRings();
        GlStateManager.popMatrix();

        // 启蒙日志
        renderEnlightenmentLogs(w, h);

        // 进度条
        if (enlightenmentProgress > 0.1f) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(centerX, centerY + 80, 0);

            String title = "ACHIEVING ETERNAL PERFECTION...";
            int titleW = mc.fontRenderer.getStringWidth(title);
            mc.fontRenderer.drawString(title, -titleW / 2, -20, 0xFFFFD700, false);

            int barW = 100;
            int filledW = (int) (barW * 2 * enlightenmentProgress);
            Gui.drawRect(-barW, 0, barW, 6, 0xFF333333);
            Gui.drawRect(-barW, 0, -barW + filledW, 6, 0xFFFFD700);

            String percent = String.format("%d%%", (int) (enlightenmentProgress * 100));
            int percentW = mc.fontRenderer.getStringWidth(percent);
            mc.fontRenderer.drawString(percent, -percentW / 2, 12, 0xFFCCAA00, false);

            GlStateManager.popMatrix();
        }
    }

    private static void renderMandala() {
        float alpha = mandalaScale * 0.5f;

        // 多层同心圆
        for (int i = 1; i <= 4; i++) {
            float radius = i * 30;
            int sides = 8 + i * 4;
            int color = applyAlpha(0xFFD700, alpha * (1.0f - i * 0.15f));
            drawPolygonOutline(0, 0, radius, sides, 1.5f, color);
        }

        // 放射线
        int rays = 16;
        for (int i = 0; i < rays; i++) {
            float angle = (float) (Math.PI * 2 * i / rays);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            int lineColor = applyAlpha(0xFFE080, alpha * 0.4f);
            drawLine(cos * 20, sin * 20, cos * 120, sin * 120, 1.0f, lineColor);
        }

        // 中心符号
        if (mandalaScale > 0.7f) {
            String symbol = "☸";
            int symbolColor = applyAlpha(0xFFFFE0, (mandalaScale - 0.7f) * 3);
            int sw = mc.fontRenderer.getStringWidth(symbol);
            mc.fontRenderer.drawString(symbol, -sw / 2, -4, symbolColor, false);
        }
    }

    private static void renderHolyRings() {
        int baseColor = 0x80CFFF;

        for (int i = 0; i < 5; i++) {
            if (holyRingRadii[i] < 1) continue;

            GlStateManager.pushMatrix();
            GlStateManager.rotate(holyRingAngles[i], 0, 0, 1);

            float radius = holyRingRadii[i];
            float alpha = 0.3f + 0.1f * i;

            // 交替金色和青色
            int color = i % 2 == 0 ?
                    applyAlpha(0xFFD700, alpha) :
                    applyAlpha(baseColor, alpha);

            int sides = 60;
            if (i == 0) sides = 8;
            else if (i == 1) sides = 6;
            else if (i == 2) sides = 12;

            drawPolygonOutline(0, 0, radius, sides, 2.0f, color);

            GlStateManager.popMatrix();
        }
    }

    private static void renderEnlightenmentLogs(int w, int h) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5f, 0.5f, 1.0f);

        int startX = 10;
        int startY = 10;

        for (int i = 0; i < enlightenmentLogs.size(); i++) {
            String log = enlightenmentLogs.get(i);
            float alpha = (float) i / enlightenmentLogs.size();
            int color;

            if (log.contains("[HARMONY")) color = applyAlpha(0xFFD700, alpha);       // 金色 - 和谐
            else if (log.contains("[PERFECT")) color = applyAlpha(0x80CFFF, alpha);  // 青色 - 完美
            else if (log.contains("[ETERNAL")) color = applyAlpha(0xFFE080, alpha);  // 暖金 - 永恒
            else if (log.contains("[COMPLETE")) color = applyAlpha(0x90EE90, alpha); // 绿色 - 完成
            else color = applyAlpha(0xCCCCCC, alpha);

            mc.fontRenderer.drawString(log, startX, startY + i * 10, color, false);
        }
        GlStateManager.popMatrix();
    }

    // ==================== Phase 4 渲染 ====================

    private static void renderPhase4(int w, int h) {
        float centerX = w / 2.0f;
        float centerY = h / 2.0f;

        // 背景光辉
        if (radianceIntensity > 0.1f) {
            renderDivineRadiance(w, h, centerX, centerY);
        }

        // 圣环
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0);
        renderHolyRings();
        GlStateManager.popMatrix();

        // 莲花
        if (lotusBloom > 0.1f) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(centerX, centerY, 0);
            renderLotus();
            GlStateManager.popMatrix();
        }

        // 金色光点
        for (GoldenMote mote : goldenMotes) {
            int moteColor = applyAlpha(0xFFD700, mote.life * 0.8f);
            drawSoftCircle(mote.x, mote.y, mote.size, moteColor);
        }

        // 最终光辉
        if (finalRadiance > 0.01f) {
            int radianceColor = applyAlpha(0xFFFAE0, finalRadiance * 0.6f);
            Gui.drawRect(0, 0, w, h, radianceColor);
        }

        // 宁静效果
        if (peaceProgress > 0) {
            int peaceColor = applyAlpha(0xFFFFFF, peaceProgress * 0.2f);
            drawSoftCircle(centerX, centerY, 100 + peaceProgress * 200, peaceColor);
        }
    }

    private static void renderLotus() {
        // 莲花花瓣
        for (int i = 0; i < 8; i++) {
            if (petalExtend[i] < 0.1f) continue;

            GlStateManager.pushMatrix();
            GlStateManager.rotate(petalAngles[i], 0, 0, 1);

            float extend = petalExtend[i];
            float petalLength = 40 * extend;
            float petalWidth = 15 * extend;

            // 花瓣颜色（金色渐变）
            float hue = i / 8.0f;
            int r = (int) (255 - hue * 30);
            int g = (int) (200 + hue * 20);
            int b = (int) (80 - hue * 40);
            int petalColor = applyAlpha((r << 16) | (g << 8) | b, 0.7f * extend);

            drawPetal(0, -10, petalLength, petalWidth, petalColor);

            GlStateManager.popMatrix();
        }

        // 莲花中心
        int coreColor = applyAlpha(0xFFFFE0, lotusBloom * 0.9f);
        drawSoftCircle(0, 0, 15 * lotusBloom, coreColor);

        // 中心符号
        if (lotusBloom > 0.8f) {
            float symbolAlpha = (lotusBloom - 0.8f) * 5;
            String symbol = "❀";
            int symbolColor = applyAlpha(0xFFFFFF, symbolAlpha);
            int sw = mc.fontRenderer.getStringWidth(symbol);
            mc.fontRenderer.drawString(symbol, -sw / 2, -4, symbolColor, false);
        }
    }

    private static void renderDivineRadiance(int w, int h, float centerX, float centerY) {
        int rays = 12;
        float baseAlpha = radianceIntensity * 0.12f;

        GlStateManager.disableTexture2D();

        for (int i = 0; i < rays; i++) {
            float angle = (float) (Math.PI * 2 * i / rays) + animationTick * 0.002f;
            float rayLength = Math.max(w, h) * 0.7f;
            float rayWidth = 30 + radianceIntensity * 20;

            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            // 温暖的金色光线
            int color = applyAlpha(0xFFD080, baseAlpha * (0.7f + random.nextFloat() * 0.3f));
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = ((color >> 24) & 0xFF) / 255.0f;

            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(centerX, centerY, 0).color(r, g, b, a).endVertex();
            buffer.pos(centerX + cos * rayLength - sin * rayWidth,
                    centerY + sin * rayLength + cos * rayWidth, 0).color(r, g, b, 0).endVertex();
            buffer.pos(centerX + cos * rayLength + sin * rayWidth,
                    centerY + sin * rayLength - cos * rayWidth, 0).color(r, g, b, 0).endVertex();
            Tessellator.getInstance().draw();
        }

        GlStateManager.enableTexture2D();
    }

    // ==================== 心跳渲染 ====================

    private static void renderHeartbeat(int w, int h) {
        float centerX = w / 2.0f;
        float centerY = h / 2.0f;

        float beat = 0;
        if (heartbeatPhase < 0.1f) {
            beat = heartbeatPhase / 0.1f;
        } else if (heartbeatPhase < 0.3f) {
            beat = 1.0f - (heartbeatPhase - 0.1f) / 0.2f;
        }
        beat *= heartbeatStrength;

        if (beat > 0.05f) {
            // 温暖的心跳光环（与破碎之神的冷白不同）
            float pulseSize = 10 + beat * 30;
            int warmPulse = applyAlpha(0xFFD080, beat * 0.3f);
            drawSoftCircle(centerX, centerY, pulseSize, warmPulse);

            // 边缘脉动
            if (beat > 0.3f) {
                int edgeColor = applyAlpha(0xFFD700, beat * 0.1f);
                renderEdgePulse(w, h, edgeColor);
            }
        }
    }

    private static void renderEdgePulse(int w, int h, int color) {
        int thickness = 2;
        Gui.drawRect(0, 0, w, thickness, color);
        Gui.drawRect(0, h - thickness, w, h, color);
        Gui.drawRect(0, 0, thickness, h, color);
        Gui.drawRect(w - thickness, 0, w, h, color);
    }

    // ==================== 独白渲染 ====================

    private static void renderMonologue(int w, int h) {
        for (MonologueLine line : SCRIPT) {
            if (animationTick >= line.start && animationTick < line.start + line.duration) {
                float lineProgress = (float) (animationTick - line.start) / line.duration;

                float alpha = 1.0f;
                if (lineProgress < 0.15f) alpha = lineProgress / 0.15f;
                else if (lineProgress > 0.85f) alpha = (1.0f - lineProgress) / 0.15f;

                GlStateManager.pushMatrix();

                float baseY = h / 2.0f + 70;
                float scale = 1.0f;

                if (line.type == LineType.HUMAN) {
                    scale = 1.5f;
                } else if (line.type == LineType.ENLIGHTENED) {
                    scale = 1.2f;
                } else if (line.type == LineType.PEACE) {
                    scale = 2.0f;
                    baseY = h - 40;
                }

                GlStateManager.translate(w / 2.0f, baseY, 0);
                GlStateManager.scale(scale, scale, 1);

                String textToDraw;
                int color;

                switch (line.type) {
                    case SYSTEM:
                        color = 0x80CFFF;
                        textToDraw = line.text;
                        break;
                    case ENLIGHTENED:
                        color = 0xFFD700;
                        if (line.text.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < 7; i++) {
                                sb.append(SACRED_SYMBOLS[(animationTick / 3 + i) % SACRED_SYMBOLS.length]);
                                sb.append(" ");
                            }
                            textToDraw = sb.toString();
                        } else {
                            textToDraw = line.text;
                        }
                        break;
                    case PEACE:
                        color = 0xFFFAE0;
                        textToDraw = line.text;
                        break;
                    case HUMAN:
                    default:
                        color = 0xFFE0A0;
                        textToDraw = line.text;
                        break;
                }

                if (!textToDraw.isEmpty()) {
                    int strW = mc.fontRenderer.getStringWidth(textToDraw);
                    mc.fontRenderer.drawString(textToDraw, -strW / 2.0f, 0,
                            applyAlpha(color, alpha), false);
                }

                GlStateManager.popMatrix();
            }
        }
    }

    // ==================== 工具方法 ====================

    private static String generateEnlightenmentLog(float progress) {
        if (progress < 0.25f) {
            return String.format("[HARMONY] GEAR_RESONANCE_%04X ... ALIGNED", random.nextInt(0xFFFF));
        } else if (progress < 0.5f) {
            return String.format("[PERFECT] LENS_ARRAY_%02X ... FOCUSED", random.nextInt(256));
        } else if (progress < 0.8f) {
            return String.format("[ETERNAL] HUMANITY_CORE ... %.1f%% PERFECTION", 90 + progress * 10);
        } else {
            String[] finalLogs = {
                    "[COMPLETE] PERFECT_CREATION ... REALIZED",
                    "[COMPLETE] ETERNAL_HARMONY ... ACHIEVED",
                    "[COMPLETE] NIRVANA_PROTOCOL ... READY",
                    "[COMPLETE] SHAMBHALA ... AWAKENED"
            };
            return finalLogs[random.nextInt(finalLogs.length)];
        }
    }

    private static void drawSoftCircle(float x, float y, float radius, int color) {
        if (radius < 0.5f) return;
        GlStateManager.disableTexture2D();

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y, 0).color(r, g, b, a).endVertex();

        int segments = Math.max(16, (int) (radius * 1.5f));
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            buffer.pos(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius, 0)
                    .color(r, g, b, 0).endVertex();
        }
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
    }

    private static void drawWarmVignette(int w, int h, float alpha) {
        int color = applyAlpha(0xFFD080, alpha);
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        GlStateManager.disableTexture2D();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        // 四角渐晕
        int size = Math.min(w, h) / 3;

        // 左上
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(0, 0, 0).color(r, g, b, a).endVertex();
        buffer.pos(size, 0, 0).color(r, g, b, 0).endVertex();
        buffer.pos(0, size, 0).color(r, g, b, 0).endVertex();
        Tessellator.getInstance().draw();

        // 右上
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(w, 0, 0).color(r, g, b, a).endVertex();
        buffer.pos(w - size, 0, 0).color(r, g, b, 0).endVertex();
        buffer.pos(w, size, 0).color(r, g, b, 0).endVertex();
        Tessellator.getInstance().draw();

        // 左下
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(0, h, 0).color(r, g, b, a).endVertex();
        buffer.pos(size, h, 0).color(r, g, b, 0).endVertex();
        buffer.pos(0, h - size, 0).color(r, g, b, 0).endVertex();
        Tessellator.getInstance().draw();

        // 右下
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(w, h, 0).color(r, g, b, a).endVertex();
        buffer.pos(w - size, h, 0).color(r, g, b, 0).endVertex();
        buffer.pos(w, h - size, 0).color(r, g, b, 0).endVertex();
        Tessellator.getInstance().draw();

        GlStateManager.enableTexture2D();
    }

    private static void drawGearShape(float x, float y, float radius, int teeth, int color) {
        GlStateManager.disableTexture2D();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        // 齿轮外圈
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        int segments = teeth * 4;
        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            float toothRadius = (i % 4 < 2) ? radius : radius * 0.75f;
            buffer.pos(x + Math.cos(angle) * toothRadius, y + Math.sin(angle) * toothRadius, 0)
                    .color(r, g, b, a).endVertex();
        }
        Tessellator.getInstance().draw();

        // 中心孔
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        float holeRadius = radius * 0.3f;
        for (int i = 0; i <= 16; i++) {
            double angle = (Math.PI * 2 * i) / 16;
            buffer.pos(x + Math.cos(angle) * holeRadius, y + Math.sin(angle) * holeRadius, 0)
                    .color(r, g, b, a).endVertex();
        }
        Tessellator.getInstance().draw();

        GlStateManager.enableTexture2D();
    }

    private static void drawPolygonOutline(float x, float y, float radius, int sides, float thickness, int color) {
        GlStateManager.disableTexture2D();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        GL11.glLineWidth(thickness);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < sides; i++) {
            double angle = (Math.PI * 2 * i / sides) - Math.PI / 2;
            buffer.pos(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius, 0)
                    .color(r, g, b, a).endVertex();
        }
        Tessellator.getInstance().draw();

        GL11.glLineWidth(1.0f);
        GlStateManager.enableTexture2D();
    }

    private static void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        GlStateManager.disableTexture2D();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        GL11.glLineWidth(thickness);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        Tessellator.getInstance().draw();

        GL11.glLineWidth(1.0f);
        GlStateManager.enableTexture2D();
    }

    private static void drawPetal(float x, float y, float length, float width, int color) {
        GlStateManager.disableTexture2D();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);

        // 花瓣形状（椭圆形）
        buffer.pos(x, y, 0).color(r, g, b, a).endVertex();
        int segments = 12;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * i / segments;
            float px = x + (float) Math.sin(angle) * width;
            float py = y - (float) Math.cos(angle) * length;
            buffer.pos(px, py, 0).color(r, g, b, a * 0.5f).endVertex();
        }

        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
    }

    private static int applyAlpha(int color, float alpha) {
        int a = MathHelper.clamp((int) (alpha * 255), 0, 255);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private static float easeInQuad(float t) {
        return t * t;
    }

    private static float easeOutQuart(float t) {
        return 1 - (float) Math.pow(1 - t, 4);
    }

    private static float easeInOutQuad(float t) {
        return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    // ==================== 内部类 ====================

    private static class MonologueLine {
        int start, duration;
        LineType type;
        String text;

        MonologueLine(int s, int d, LineType type, String t) {
            this.start = s;
            this.duration = d;
            this.type = type;
            this.text = t;
        }
    }

    private static class WarmthParticle {
        float x, y, size, life, vx, vy;
    }

    private static class GoldenGear {
        float x, y, radius, rotation, rotationSpeed, alpha, targetAlpha;
        int teeth;
    }

    private static class MemoryFragment {
        float x, y, life, glowPhase;
        String text;
        boolean protected_;
    }

    private static class GoldenMote {
        float x, y, vx, vy, life, size;
    }
}
