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
 * 破碎之神：神性降临 (V9 - Divine Forge / 锻神之炉)
 *
 * 配色方案：机械神性
 * - 核心：炽白 → 橙黄 (烧红的金属)
 * - 光晕：琥珀/黄铜色
 * - 圣环：青铜 / 脉动时亮铜
 * - 冲击波：橙白火花
 * - 色散：橙/青通道分离 (热能 vs 冷金属)
 * - 余韵：暖灰铜锈色调
 */
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class BrokenGodAscensionOverlay extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();

    // ==================== 时间控制 ====================
    private static boolean isAnimating = false;
    private static int animationTick = 0;

    private static final int TOTAL_DURATION = 680;
    private static final int FADE_OUT_DURATION = 40;

    private static final int PHASE1_END = 70;
    private static final int PHASE2_END = 190;
    private static final int PHASE3_END = 350;
    private static final int PHASE3_5_END = 410;
    private static final int PHASE4_END = TOTAL_DURATION;

    private static final int PHASE4_BURST_END = 450;
    private static final int PHASE4_PULSE_END = 570;
    private static final int PHASE4_LIGHT_END = 620;

    // ==================== 全局状态 ====================
    private static float globalFadeAlpha = 1.0f;

    // ==================== Phase 1 状态 ====================
    private static float senseDecay = 0;
    private static float colorDrain = 0;
    private static final List<SensoryGhost> sensoryGhosts = new ArrayList<>();

    // ==================== Phase 2 状态 ====================
    private static final List<MemoryShard> memoryShards = new ArrayList<>();
    private static final List<FormatBlock> formatBlocks = new ArrayList<>();
    private static int formatTargetLine = 0;
    private static int formatCurrentLine = 0;
    private static int formatBurstCooldown = 0;
    private static float formatScreenShake = 0;

    private static float lastWordsProgress = 0;
    private static float[] lastWordsCharAlpha = new float[5];
    private static boolean[] lastWordsCharGlitch = new boolean[5];
    private static int[] lastWordsCharGlitchTimer = new int[5];

    // ==================== Phase 3 状态 ====================
    private static final List<String> biosLogs = new ArrayList<>();
    private static int lastLogTick = 0;

    // ==================== Phase 3.5 状态 ====================
    private static float lastHeartbeatPhase = 0;
    private static float lastHeartbeatStrength = 1.0f;
    private static float lastHeartbeatInterval = 1.0f;

    // ==================== Phase 4 状态 ====================
    private static float singularitySize = 0;
    private static float shockwaveRadius = 0;
    private static float shockwaveAlpha = 0;
    private static float divinityIntensity = 0;
    private static float[] holoRingRadii = new float[5];
    private static float[] holoRingTargetRadii = new float[5];
    private static float[] holoRingAngles = new float[5];
    private static float[] holoRingTilts = new float[5];
    private static final List<DivineSigil> divineSigils = new ArrayList<>();

    private static float pulsePhase = 0;
    private static float pulseInterval = 1.2f;
    private static float pulseIntensity = 0;
    private static float globalShake = 0;

    private static float lightburstAlpha = 0;
    private static float aftermathProgress = 0;
    private static float worldDesaturation = 0;

    // ==================== 色散控制 ====================
    private static float chromaticAberration = 0;
    private static float aberrationPulse = 0;

    // ==================== 剧本 ====================
    enum LineType { HUMAN, SYSTEM, DIVINE, VOID }
    private static final List<MonologueLine> SCRIPT = new ArrayList<>();

    // 机械神性符文 - 更多几何/齿轮感
    private static final String[] DIVINE_RUNES = {
            "◈", "◇", "○", "●", "◎", "◉", "△", "▽", "☆", "★",
            "⚙", "⚛", "⌬", "⏣", "⏢", "⎔", "⬡", "⬢", "⟐", "⍟"
    };

    private static final char[] GLITCH_CHARS = {'█', '▓', '▒', '░', '#', '?', '%', '&', '!', '0', '1', '@'};

    private static final String[] MEMORY_FRAGMENTS = {
            "母亲的声音", "第一次", "温暖", "笑容", "名字",
            "承诺", "眼泪", "拥抱", "告别", "曾经", "爱"
    };

    private static final String LAST_WORDS = "我不想忘记";

    static {
        SCRIPT.add(new MonologueLine(5, 40, LineType.HUMAN, "颜色... 在消失..."));
        SCRIPT.add(new MonologueLine(50, 25, LineType.SYSTEM, "[ 感官通道: 关闭中 ]"));
        SCRIPT.add(new MonologueLine(75, 35, LineType.SYSTEM, ">>> 开始覆写"));
        SCRIPT.add(new MonologueLine(170, 25, LineType.SYSTEM, ">>> 扇区清除: 完成"));
        SCRIPT.add(new MonologueLine(365, 35, LineType.VOID, ""));
        SCRIPT.add(new MonologueLine(640, 40, LineType.DIVINE, "MEKHANE"));
    }

    // ==================== 公共接口 ====================

    public static void startAnimation() {
        isAnimating = true;
        animationTick = 0;

        globalFadeAlpha = 1.0f;
        senseDecay = 0;
        colorDrain = 0;
        formatTargetLine = 0;
        formatCurrentLine = 0;
        formatBurstCooldown = 0;
        formatScreenShake = 0;
        lastWordsProgress = 0;
        lastLogTick = 0;
        lastHeartbeatPhase = 0;
        lastHeartbeatStrength = 1.0f;
        lastHeartbeatInterval = 1.0f;
        singularitySize = 0;
        shockwaveRadius = 0;
        shockwaveAlpha = 0;
        divinityIntensity = 0;
        pulsePhase = 0;
        pulseInterval = 1.2f;
        pulseIntensity = 0;
        globalShake = 0;
        lightburstAlpha = 0;
        aftermathProgress = 0;
        worldDesaturation = 0;
        chromaticAberration = 0;
        aberrationPulse = 0;

        sensoryGhosts.clear();
        memoryShards.clear();
        formatBlocks.clear();
        biosLogs.clear();
        divineSigils.clear();

        for (int i = 0; i < 5; i++) {
            lastWordsCharAlpha[i] = 1.0f;
            lastWordsCharGlitch[i] = false;
            lastWordsCharGlitchTimer[i] = 0;
        }

        for (int i = 0; i < holoRingAngles.length; i++) {
            holoRingAngles[i] = random.nextFloat() * 360;
            holoRingTilts[i] = (random.nextFloat() - 0.5f) * 60;
            holoRingRadii[i] = 0;
            holoRingTargetRadii[i] = 40 + i * 30;
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

        if (animationTick <= PHASE1_END) {
            tickPhase1();
        } else if (animationTick <= PHASE2_END) {
            tickPhase2();
        } else if (animationTick <= PHASE3_END) {
            tickPhase3();
        } else if (animationTick <= PHASE3_5_END) {
            tickPhase3_5();
        } else {
            tickPhase4();
        }
    }

    private static void tickPhase1() {
        float progress = (float) animationTick / PHASE1_END;
        senseDecay = easeInQuad(progress);
        colorDrain = senseDecay;

        if (random.nextFloat() < 0.15f && progress < 0.85f) {
            ScaledResolution res = new ScaledResolution(mc);
            SensoryGhost ghost = new SensoryGhost();
            ghost.x = random.nextFloat() * res.getScaledWidth();
            ghost.y = random.nextFloat() * res.getScaledHeight();
            ghost.size = 20 + random.nextFloat() * 40;
            ghost.life = 1.0f;
            ghost.drift = (random.nextFloat() - 0.5f) * 2;
            ghost.type = random.nextInt(3);
            sensoryGhosts.add(ghost);
        }

        Iterator<SensoryGhost> ghostIter = sensoryGhosts.iterator();
        while (ghostIter.hasNext()) {
            SensoryGhost ghost = ghostIter.next();
            ghost.life -= 0.02f;
            ghost.y -= 0.5f;
            ghost.x += ghost.drift * 0.3f;
            ghost.size *= 1.01f;
            if (ghost.life <= 0) ghostIter.remove();
        }
        while (sensoryGhosts.size() > 15) sensoryGhosts.remove(0);
    }

    private static void tickPhase2() {
        float progress = (float) (animationTick - PHASE1_END) / (PHASE2_END - PHASE1_END);
        ScaledResolution res = new ScaledResolution(mc);
        int h = res.getScaledHeight();
        int w = res.getScaledWidth();

        formatTargetLine = (int) (h * progress);

        if (formatBurstCooldown > 0) {
            formatBurstCooldown--;
        } else {
            if (formatCurrentLine < formatTargetLine) {
                int burstAmount = 3 + random.nextInt(5);
                int burstEnd = Math.min(formatCurrentLine + burstAmount, formatTargetLine);

                while (formatCurrentLine < burstEnd) {
                    FormatBlock block = new FormatBlock();
                    block.y = formatCurrentLine;
                    block.height = random.nextInt(3) + 2;
                    block.targetAlpha = 0.95f;
                    block.currentAlpha = 0.3f;
                    formatBlocks.add(block);
                    formatCurrentLine += block.height + random.nextInt(2);
                }

                formatBurstCooldown = 4 + random.nextInt(8);
                formatScreenShake = 0.5f;
            }
        }

        formatScreenShake *= 0.85f;

        for (FormatBlock block : formatBlocks) {
            block.currentAlpha += (block.targetAlpha - block.currentAlpha) * 0.15f;
        }
        while (formatBlocks.size() > 150) formatBlocks.remove(0);

        if (random.nextFloat() < 0.1f && progress < 0.85f) {
            MemoryShard shard = new MemoryShard();
            shard.x = random.nextFloat() * w;
            shard.y = formatCurrentLine + 30 + random.nextFloat() * 80;
            shard.text = MEMORY_FRAGMENTS[random.nextInt(MEMORY_FRAGMENTS.length)];
            shard.life = 1.0f;
            shard.vx = 0;
            shard.vy = 0;
            shard.state = ShardState.FLOATING;
            shard.pixelateProgress = 0;
            memoryShards.add(shard);
        }

        Iterator<MemoryShard> shardIter = memoryShards.iterator();
        while (shardIter.hasNext()) {
            MemoryShard shard = shardIter.next();
            float distToFormat = shard.y - formatCurrentLine;

            switch (shard.state) {
                case FLOATING:
                    shard.y += 0.2f;
                    shard.x += (random.nextFloat() - 0.5f) * 0.5f;
                    if (distToFormat < 60) shard.state = ShardState.FLEEING;
                    break;
                case FLEEING:
                    shard.vy = Math.min(shard.vy + 0.3f, 4.0f);
                    shard.y += shard.vy;
                    shard.x += (random.nextFloat() - 0.5f) * 2;
                    if (distToFormat > 100 || random.nextFloat() < 0.02f) shard.state = ShardState.CAPTURED;
                    break;
                case CAPTURED:
                    float pullStrength = 0.15f;
                    shard.vy -= pullStrength * distToFormat * 0.02f;
                    shard.vy *= 0.95f;
                    shard.y += shard.vy;
                    if (shard.y <= formatCurrentLine + 5) shard.state = ShardState.DISSOLVING;
                    break;
                case DISSOLVING:
                    shard.pixelateProgress += 0.08f;
                    shard.life -= 0.05f;
                    if (shard.life <= 0) { shardIter.remove(); continue; }
                    break;
            }

            if (shard.state == ShardState.FLOATING) shard.life -= 0.003f;
            if (shard.life <= 0) shardIter.remove();
        }
        while (memoryShards.size() > 25) memoryShards.remove(0);

        if (progress > 0.3f && progress < 0.95f) {
            lastWordsProgress = (progress - 0.3f) / 0.65f;
            for (int i = 0; i < 5; i++) {
                float glitchThreshold = 0.2f + (i * 0.12f) + (random.nextFloat() * 0.03f);
                if (lastWordsProgress > glitchThreshold && !lastWordsCharGlitch[i]) {
                    lastWordsCharGlitch[i] = true;
                }
                if (lastWordsCharGlitch[i]) {
                    lastWordsCharGlitchTimer[i]++;
                    lastWordsCharAlpha[i] -= 0.025f;
                    if (lastWordsCharAlpha[i] < 0) lastWordsCharAlpha[i] = 0;
                }
            }
        }
    }

    private static void tickPhase3() {
        float progress = (float) (animationTick - PHASE2_END) / (PHASE3_END - PHASE2_END);
        int tickInterval = progress > 0.7f ? 1 : (progress > 0.4f ? 2 : 3);
        if (animationTick - lastLogTick >= tickInterval) {
            biosLogs.add(generateBootLog(progress));
            lastLogTick = animationTick;
            while (biosLogs.size() > 18) biosLogs.remove(0);
        }
    }

    private static void tickPhase3_5() {
        float progress = (float) (animationTick - PHASE3_END) / (PHASE3_5_END - PHASE3_END);
        lastHeartbeatStrength = 1.0f - progress * 0.8f;
        lastHeartbeatInterval = 1.0f + progress * 1.5f;
        lastHeartbeatPhase += 0.05f / lastHeartbeatInterval;
        if (lastHeartbeatPhase > 1.0f) lastHeartbeatPhase -= 1.0f;
    }

    private static void tickPhase4() {
        int phase4Tick = animationTick - PHASE3_5_END;

        if (animationTick <= PHASE4_BURST_END) {
            float burstProgress = (float) phase4Tick / (PHASE4_BURST_END - PHASE3_5_END);

            singularitySize = burstProgress < 0.3f ? easeOutQuart(burstProgress / 0.3f) * 60 : 60 - (burstProgress - 0.3f) * 30;

            if (burstProgress > 0.15f && burstProgress < 0.5f) {
                float swProgress = (burstProgress - 0.15f) / 0.35f;
                shockwaveRadius = swProgress * 400;
                shockwaveAlpha = (1.0f - swProgress) * 1.0f;
            } else {
                shockwaveAlpha *= 0.8f;
            }

            if (burstProgress > 0.2f) {
                for (int i = 0; i < holoRingRadii.length; i++) {
                    float overshoot = (float) Math.sin((burstProgress - 0.2f) / 0.8f * Math.PI) * 0.2f;
                    float targetMult = Math.min((burstProgress - 0.2f) / 0.8f * 1.5f, 1.0f) + overshoot;
                    holoRingRadii[i] += (holoRingTargetRadii[i] * targetMult - holoRingRadii[i]) * 0.15f;
                }
            }

            globalShake = (1.0f - burstProgress) * 15.0f;
            divinityIntensity = easeOutQuart(burstProgress);
            chromaticAberration = burstProgress * 0.6f;

        } else if (animationTick <= PHASE4_PULSE_END) {
            float pulseStageProgress = (float) (animationTick - PHASE4_BURST_END) / (PHASE4_PULSE_END - PHASE4_BURST_END);

            pulseInterval = 1.2f - pulseStageProgress * 0.7f;
            pulsePhase += 0.05f / pulseInterval;
            if (pulsePhase > 1.0f) {
                pulsePhase -= 1.0f;
                globalShake = 3.0f + pulseStageProgress * 5.0f;
                aberrationPulse = 1.0f;
            }

            float beat = pulsePhase < 0.1f ? pulsePhase / 0.1f :
                    (pulsePhase < 0.2f ? 1.0f - (pulsePhase - 0.1f) / 0.1f : 0);
            pulseIntensity = beat * (0.5f + pulseStageProgress * 0.5f);

            for (int i = 0; i < holoRingRadii.length; i++) {
                float breathe = 1.0f + pulseIntensity * 0.15f;
                holoRingRadii[i] = holoRingTargetRadii[i] * breathe;
                holoRingAngles[i] += (i % 2 == 0 ? 1 : -1) * (0.5f + i * 0.3f);
            }

            globalShake *= 0.9f;
            divinityIntensity = 1.0f;
            singularitySize = 40 + pulseIntensity * 30;
            chromaticAberration = 0.6f + pulseStageProgress * 0.3f + aberrationPulse * 0.2f;
            aberrationPulse *= 0.85f;

        } else if (animationTick <= PHASE4_LIGHT_END) {
            float lightProgress = (float) (animationTick - PHASE4_PULSE_END) / (PHASE4_LIGHT_END - PHASE4_PULSE_END);

            pulsePhase += 0.15f;
            if (pulsePhase > 1.0f) {
                pulsePhase -= 1.0f;
                globalShake = 8.0f;
                aberrationPulse = 1.5f;
            }

            lightburstAlpha = lightProgress < 0.6f ? easeInQuad(lightProgress / 0.6f) : 1.0f;
            singularitySize = 60 + lightProgress * 600;
            globalShake = globalShake * 0.85f + random.nextFloat() * 5;
            chromaticAberration = 0.9f + lightProgress * 0.1f + aberrationPulse * 0.15f;
            aberrationPulse *= 0.8f;

        } else {
            float afterProgress = (float) (animationTick - PHASE4_LIGHT_END) / (PHASE4_END - PHASE4_LIGHT_END);
            aftermathProgress = afterProgress;

            lightburstAlpha = Math.max(0, 1.0f - afterProgress * 2.0f);
            worldDesaturation = easeOutQuart(Math.min(afterProgress * 1.5f, 1.0f));

            globalShake *= 0.8f;
            singularitySize *= 0.95f;
            pulseIntensity *= 0.9f;

            for (int i = 0; i < holoRingRadii.length; i++) {
                holoRingRadii[i] *= 0.98f;
            }

            divinityIntensity = 1.0f - afterProgress * 0.3f;
            chromaticAberration = (1.0f - afterProgress) * 0.7f;
        }

        // 全局淡出
        int fadeStartTick = TOTAL_DURATION - FADE_OUT_DURATION;
        if (animationTick > fadeStartTick) {
            float fadeProgress = (float) (animationTick - fadeStartTick) / (float) FADE_OUT_DURATION;
            globalFadeAlpha = 1.0f - easeInQuad(fadeProgress);
        } else {
            globalFadeAlpha = 1.0f;
        }

        // 生成神性符文
        if (animationTick > PHASE4_BURST_END && animationTick < PHASE4_LIGHT_END) {
            if (random.nextFloat() < 0.1f * divinityIntensity) {
                ScaledResolution res = new ScaledResolution(mc);
                DivineSigil sigil = new DivineSigil();
                float angle = random.nextFloat() * (float) Math.PI * 2;
                float dist = 50 + random.nextFloat() * 100;
                sigil.x = res.getScaledWidth() / 2.0f + (float) Math.cos(angle) * dist;
                sigil.y = res.getScaledHeight() / 2.0f + (float) Math.sin(angle) * dist;
                sigil.vx = (float) Math.cos(angle) * 2;
                sigil.vy = (float) Math.sin(angle) * 2;
                sigil.rune = DIVINE_RUNES[random.nextInt(DIVINE_RUNES.length)];
                sigil.life = 1.0f;
                sigil.scale = 1.0f + random.nextFloat() * 2.0f;
                divineSigils.add(sigil);
            }
        }

        Iterator<DivineSigil> sigilIter = divineSigils.iterator();
        while (sigilIter.hasNext()) {
            DivineSigil sigil = sigilIter.next();
            sigil.x += sigil.vx;
            sigil.y += sigil.vy;
            sigil.vx *= 0.98f;
            sigil.vy *= 0.98f;
            sigil.life -= 0.02f;
            if (sigil.life <= 0) sigilIter.remove();
        }
        while (divineSigils.size() > 40) divineSigils.remove(0);
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

        if (globalShake > 0.1f) {
            float sx = (random.nextFloat() - 0.5f) * globalShake;
            float sy = (random.nextFloat() - 0.5f) * globalShake;
            GlStateManager.translate(sx, sy, 0);
        }

        renderAtmosphere(w, h);

        if (animationTick <= PHASE1_END) {
            renderPhase1(w, h);
        } else if (animationTick <= PHASE2_END) {
            renderPhase2(w, h);
        } else if (animationTick <= PHASE3_END) {
            renderPhase3(w, h);
        } else if (animationTick <= PHASE3_5_END) {
            renderPhase3_5(w, h);
        } else {
            renderPhase4(w, h);
        }

        renderMonologue(w, h);

        if (globalFadeAlpha < 1.0f) {
            float curtainAlpha = 1.0f - globalFadeAlpha;
            Gui.drawRect(0, 0, w, h, applyAlpha(0x000000, curtainAlpha));
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
            color = applyAlpha(0x101010, progress * progress);
        } else if (animationTick <= PHASE2_END) {
            color = 0xFF030308;
        } else if (animationTick <= PHASE3_5_END) {
            color = 0xFF000000;
        } else if (animationTick <= PHASE4_LIGHT_END) {
            // 锻神之炉：微微的暖色调 (深红棕)
            int warmth = (int) (divinityIntensity * 12);
            color = 0xFF000000 | (warmth << 16) | ((warmth / 2) << 8);
        } else {
            // 余韵：暖灰，带铜锈色调
            int r = (int) (25 + worldDesaturation * 35);
            int g = (int) (20 + worldDesaturation * 25);
            int b = (int) (15 + worldDesaturation * 20);
            color = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        Gui.drawRect(0, 0, w, h, color);
    }

    // ==================== Phase 1 渲染 ====================

    private static void renderPhase1(int w, int h) {
        int grayOverlay = applyAlpha(0x808080, colorDrain * 0.6f);
        Gui.drawRect(0, 0, w, h, grayOverlay);

        for (SensoryGhost ghost : sensoryGhosts) {
            float alpha = ghost.life * 0.5f;
            int color;
            switch (ghost.type) {
                case 0: color = applyAlpha(0xFFFAE0, alpha); break;
                case 1: color = applyAlpha(0xE0F0FF, alpha * 0.7f); break;
                default: color = applyAlpha(0xFFE0E8, alpha * 0.5f); break;
            }
            drawSoftCircle(ghost.x, ghost.y, ghost.size, color);
        }

        float progress = (float) animationTick / PHASE1_END;
        if (progress > 0.7f) {
            float whiteout = (progress - 0.7f) / 0.3f;
            Gui.drawRect(0, 0, w, h, applyAlpha(0xFFFFFF, whiteout * 0.15f));
        }
    }

    // ==================== Phase 2 渲染 ====================

    private static void renderPhase2(int w, int h) {
        if (formatScreenShake > 0.05f) {
            GlStateManager.translate(
                    (random.nextFloat() - 0.5f) * formatScreenShake * 8,
                    (random.nextFloat() - 0.5f) * formatScreenShake * 8,
                    0
            );
        }

        for (FormatBlock block : formatBlocks) {
            int mainColor = applyAlpha(0x000000, block.currentAlpha);
            Gui.drawRect(0, block.y, w, block.y + block.height, mainColor);

            if (block.currentAlpha < block.targetAlpha * 0.95f) {
                float edgeAlpha = (block.targetAlpha - block.currentAlpha) * 4;
                Gui.drawRect(0, block.y + block.height - 1, w, block.y + block.height,
                        applyAlpha(0x00FFFF, edgeAlpha));
            }
        }

        for (MemoryShard shard : memoryShards) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(shard.x, shard.y, 0);

            int baseColor;
            float alpha = shard.life;

            switch (shard.state) {
                case FLEEING:
                    baseColor = 0xAA6666;
                    GlStateManager.rotate((random.nextFloat() - 0.5f) * 10, 0, 0, 1);
                    break;
                case CAPTURED:
                    baseColor = 0x666666;
                    break;
                case DISSOLVING:
                    baseColor = 0x444444;
                    alpha *= (1.0f - shard.pixelateProgress);
                    GlStateManager.scale(1.0f + shard.pixelateProgress * 0.5f,
                            1.0f - shard.pixelateProgress * 0.3f, 1);
                    break;
                default:
                    baseColor = 0x888888;
            }

            int color = applyAlpha(baseColor, alpha * 0.9f);

            if (shard.state == ShardState.DISSOLVING && shard.pixelateProgress > 0.3f) {
                renderPixelatedText(shard.text, color, shard.pixelateProgress);
            } else {
                mc.fontRenderer.drawString(shard.text, 0, 0, color, false);
            }

            GlStateManager.popMatrix();
        }

        float progress = (float) (animationTick - PHASE1_END) / (PHASE2_END - PHASE1_END);
        if (progress > 0.3f && progress < 0.95f) {
            renderLastWords(w, h);
        }
    }

    private static void renderLastWords(int w, int h) {
        GlStateManager.pushMatrix();
        float centerX = w / 2.0f;
        float centerY = h / 2.0f + 30;
        float scale = 2.5f;

        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(scale, scale, 1);

        String text = LAST_WORDS;
        float totalWidth = 0;
        float[] charWidths = new float[5];
        for (int i = 0; i < 5; i++) {
            charWidths[i] = mc.fontRenderer.getCharWidth(text.charAt(i));
            totalWidth += charWidths[i];
        }

        float x = -totalWidth / 2;
        for (int i = 0; i < 5; i++) {
            float charX = x;
            float charAlpha = lastWordsCharAlpha[i];

            if (charAlpha <= 0.01f) {
                x += charWidths[i] + 1;
                continue;
            }

            String displayChar;
            int color;

            if (lastWordsCharGlitch[i]) {
                color = applyAlpha(0x888888, charAlpha);
                if (random.nextFloat() < 0.7f) {
                    displayChar = String.valueOf(GLITCH_CHARS[random.nextInt(GLITCH_CHARS.length)]);
                } else {
                    displayChar = DIVINE_RUNES[random.nextInt(DIVINE_RUNES.length)];
                }
            } else {
                color = applyAlpha(0xDDDDDD, charAlpha);
                displayChar = String.valueOf(text.charAt(i));
                if (random.nextFloat() < 0.05f) {
                    charX += (random.nextFloat() - 0.5f);
                }
            }

            mc.fontRenderer.drawString(displayChar, charX, -4, color, false);
            x += charWidths[i] + 1;
        }

        GlStateManager.popMatrix();
    }

    private static void renderPixelatedText(String text, int color, float pixelateProgress) {
        int textWidth = mc.fontRenderer.getStringWidth(text);
        int blockSize = 2 + (int) (pixelateProgress * 3);

        GlStateManager.disableTexture2D();

        for (int x = 0; x < textWidth; x += blockSize) {
            for (int y = 0; y < 9; y += blockSize) {
                if (random.nextFloat() > pixelateProgress * 0.5f) {
                    float scatter = pixelateProgress * 10;
                    float sx = x + (random.nextFloat() - 0.5f) * scatter;
                    float sy = y + (random.nextFloat() - 0.5f) * scatter;
                    Gui.drawRect((int) sx, (int) sy, (int) sx + blockSize - 1, (int) sy + blockSize - 1, color);
                }
            }
        }
        GlStateManager.enableTexture2D();
    }

    // ==================== Phase 3 渲染 ====================

    private static void renderPhase3(int w, int h) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5f, 0.5f, 1.0f);

        int startX = 10;
        int startY = 10;

        for (int i = 0; i < biosLogs.size(); i++) {
            String log = biosLogs.get(i);
            float alpha = (float) i / biosLogs.size();
            int color = applyAlpha(0xFFFFFF, alpha);

            if (log.contains("[ERR")) color = applyAlpha(0xFF5555, alpha);
            else if (log.contains("[OK")) color = applyAlpha(0x55FF55, alpha);
            else if (log.contains("[WARN")) color = applyAlpha(0xFFFF55, alpha);
            else if (log.contains("[CRIT")) color = applyAlpha(0xFF8800, alpha);

            mc.fontRenderer.drawString(log, startX, startY + i * 10, color, false);
        }
        GlStateManager.popMatrix();

        float progress = (float) (animationTick - PHASE2_END) / (PHASE3_END - PHASE2_END);
        if (progress > 0.1f) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(w / 2.0f, h / 2.0f, 0);

            String title = "INSTALLING DIVINE KERNEL...";
            int titleW = mc.fontRenderer.getStringWidth(title);
            mc.fontRenderer.drawString(title, -titleW / 2, -20, 0xFFFFFF, false);

            int barW = 100;
            int filledW = (int) (barW * 2 * progress);
            Gui.drawRect(-barW, 0, barW, 6, 0xFF333333);
            Gui.drawRect(-barW, 0, -barW + filledW, 6, 0xFFFFFFFF);

            String percent = String.format("%d%%", (int) (progress * 100));
            int percentW = mc.fontRenderer.getStringWidth(percent);
            mc.fontRenderer.drawString(percent, -percentW / 2, 12, 0xFFAAAAAA, false);

            GlStateManager.popMatrix();
        }
    }

    // ==================== Phase 3.5 渲染 ====================

    private static void renderPhase3_5(int w, int h) {
        float centerX = w / 2.0f;
        float centerY = h / 2.0f;

        float beat = 0;
        if (lastHeartbeatPhase < 0.1f) {
            beat = lastHeartbeatPhase / 0.1f;
        } else if (lastHeartbeatPhase < 0.4f) {
            beat = 1.0f - (lastHeartbeatPhase - 0.1f) / 0.3f;
        }
        beat *= lastHeartbeatStrength;

        float baseSize = 1.5f + beat * 2.0f;
        float alpha = 0.4f + beat * 0.6f;
        alpha *= lastHeartbeatStrength;

        if (alpha > 0.05f) {
            int coreColor = applyAlpha(0xFFFFFF, alpha);
            drawFilledCircle(centerX, centerY, baseSize, coreColor);

            if (beat > 0.1f) {
                // 机械预兆：微微的琥珀色光晕
                int glowColor = applyAlpha(0xFFAA60, alpha * 0.3f * beat);
                drawSoftCircle(centerX, centerY, baseSize * 8, glowColor);
            }
        }

        if (random.nextFloat() < 0.05f * lastHeartbeatStrength) {
            int lineY = random.nextInt(h);
            int lineH = 1;
            int lineAlpha = random.nextInt(50) + 10;
            Gui.drawRect(0, lineY, w, lineY + lineH, (lineAlpha << 24) | 0xFFFFFF);
        }
    }

    // ==================== Phase 4 渲染: 锻神之炉 + 橙青色散 ====================

    private static void renderPhase4(int w, int h) {
        float centerX = w / 2.0f;
        float centerY = h / 2.0f;

        if (divinityIntensity > 0.1f && lightburstAlpha < 0.9f) {
            renderDivineRadiance(w, h, centerX, centerY);
        }

        if (shockwaveAlpha > 0.01f) {
            renderShockwave(centerX, centerY);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0);
        renderHolyRings();
        GlStateManager.popMatrix();

        for (DivineSigil sigil : divineSigils) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(sigil.x, sigil.y, 0);
            GlStateManager.scale(sigil.scale, sigil.scale, 1);
            float alpha = sigil.life * divinityIntensity * (1.0f - lightburstAlpha);
            int color = applyAlpha(0xDDDDDD, alpha);
            mc.fontRenderer.drawString(sigil.rune, 0, 0, color, false);
            GlStateManager.popMatrix();
        }

        if (singularitySize > 5) {
            float overexposure = Math.min(singularitySize / 100.0f, 1.0f);
            overexposure = overexposure * (0.5f + pulseIntensity * 0.5f);
            int coreColor = applyAlpha(0xFFFFFF, overexposure * 0.9f);
            drawSoftCircle(centerX, centerY, singularitySize * 0.8f, coreColor);

            int glowColor = applyAlpha(0xFFF8E0, overexposure * 0.4f);
            drawSoftCircle(centerX, centerY, singularitySize * 1.5f, glowColor);
        }

        if (lightburstAlpha > 0.01f) {
            Gui.drawRect(0, 0, w, h, applyAlpha(0xFFFFFF, lightburstAlpha));
        }

        if (aftermathProgress > 0) {
            int coldOverlay = applyAlpha(0x8090A0, worldDesaturation * 0.15f);
            Gui.drawRect(0, 0, w, h, coldOverlay);
        }
    }

    private static void renderDivineRadiance(int w, int h, float centerX, float centerY) {
        int rays = 16;
        float baseAlpha = divinityIntensity * 0.15f * (1.0f + pulseIntensity * 0.5f);

        GlStateManager.disableTexture2D();

        for (int i = 0; i < rays; i++) {
            float angle = (float) (Math.PI * 2 * i / rays) + animationTick * 0.003f;
            float rayLength = Math.max(w, h) * (0.6f + pulseIntensity * 0.3f);
            float rayWidth = 35 + pulseIntensity * 40;

            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            int color = applyAlpha(0xFFF8E0, baseAlpha * (0.6f + random.nextFloat() * 0.4f));
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

    private static void renderShockwave(float centerX, float centerY) {
        GlStateManager.disableTexture2D();

        float r = 1.0f, g = 1.0f, b = 1.0f;
        float a = shockwaveAlpha;

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        float innerRadius = shockwaveRadius - 5;
        float outerRadius = shockwaveRadius + 5;

        int segments = 64;
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            buffer.pos(centerX + cos * outerRadius, centerY + sin * outerRadius, 0)
                    .color(r, g, b, 0).endVertex();
            buffer.pos(centerX + cos * innerRadius, centerY + sin * innerRadius, 0)
                    .color(r, g, b, a).endVertex();
        }

        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
    }

    private static void renderHolyRings() {
        int baseColor = 0xE8E4D8;

        for (int i = 0; i < 5; i++) {
            if (holoRingRadii[i] < 1) continue;

            GlStateManager.pushMatrix();

            float tilt = holoRingTilts[i] * divinityIntensity;
            GlStateManager.rotate(tilt, 1, 0, 0);
            GlStateManager.rotate(holoRingAngles[i], 0, 0, 1);

            float radius = holoRingRadii[i];
            float alpha = divinityIntensity * (0.4f + 0.1f * i) * (1.0f - lightburstAlpha * 0.8f);
            int color = applyAlpha(baseColor, alpha);

            int sides = 60;
            if (i == 0) sides = 4;
            else if (i == 1) sides = 6;
            else if (i == 2) sides = 3;
            else if (i == 3) sides = 8;

            float width = 3.0f + i * 0.8f + pulseIntensity * 2;
            drawDivinePolygon(0, 0, radius, sides, width, color);

            GlStateManager.popMatrix();
        }

        if (divinityIntensity > 0.5f && lightburstAlpha < 0.8f) {
            float coreAlpha = (divinityIntensity - 0.5f) * 2 * (1.0f - lightburstAlpha);
            GlStateManager.pushMatrix();
            GlStateManager.rotate(animationTick * 0.5f, 0, 0, 1);

            float scale = 0.7f + pulseIntensity * 0.3f;
            GlStateManager.scale(scale, scale, 1);

            int frameColor = applyAlpha(0xCCCCCC, coreAlpha * 0.7f);
            drawDiamondFrame(25, frameColor);

            int crossColor = applyAlpha(0xFFFFFF, coreAlpha * 0.9f);
            drawCross(10, 2.5f, crossColor);

            GlStateManager.popMatrix();
        }
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

                float baseY = h / 2.0f + 60;
                float scale = 1.0f;
                float jitterX = 0, jitterY = 0;

                if (line.type == LineType.HUMAN) {
                    scale = 1.5f;
                    if (senseDecay > 0) {
                        jitterX = (random.nextFloat() - 0.5f) * senseDecay * 4;
                        jitterY = (random.nextFloat() - 0.5f) * senseDecay * 4;
                    }
                } else if (line.type == LineType.DIVINE) {
                    scale = 2.0f;
                    if (aftermathProgress > 0.3f) {
                        baseY = h - 30;
                        scale = 1.0f;
                    }
                } else if (line.type == LineType.VOID) {
                    scale = 1.0f;
                    alpha *= 0.5f;
                }

                GlStateManager.translate(w / 2.0f + jitterX, baseY + jitterY, 0);
                GlStateManager.scale(scale, scale, 1);

                String textToDraw;
                int color;

                switch (line.type) {
                    case SYSTEM:
                        color = 0x606060;
                        textToDraw = line.text;
                        break;
                    case DIVINE:
                        // 机械神性：金铜色文字
                        color = aftermathProgress > 0.3f ? 0x907050 : 0xFFD090;
                        if (line.text.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < 7; i++) {
                                sb.append(DIVINE_RUNES[(animationTick / 3 + i) % DIVINE_RUNES.length]);
                                sb.append(" ");
                            }
                            textToDraw = sb.toString();
                        } else {
                            textToDraw = line.text;
                        }
                        break;
                    case VOID:
                        color = 0x333333;
                        int dots = (animationTick / 20) % 4;
                        StringBuilder db = new StringBuilder();
                        for (int d = 0; d < dots; d++) db.append(".");
                        textToDraw = db.toString();
                        break;
                    case HUMAN:
                    default:
                        color = 0xEEEEEE;
                        textToDraw = line.text;
                        break;
                }

                // DIVINE 文字橙青色散
                if (line.type == LineType.DIVINE && chromaticAberration > 0.5f && !textToDraw.isEmpty()) {
                    int strW = mc.fontRenderer.getStringWidth(textToDraw);
                    float offset = chromaticAberration * 2;

                    // 橙色偏移
                    mc.fontRenderer.drawString(textToDraw, -strW / 2.0f + offset, 0,
                            applyAlpha(0xFF9060, alpha * 0.3f), false);
                    // 青色偏移
                    mc.fontRenderer.drawString(textToDraw, -strW / 2.0f - offset, 0,
                            applyAlpha(0x60C0D0, alpha * 0.25f), false);
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

    private static String generateBootLog(float progress) {
        String[] components = {"MEM", "CPU", "GPU", "IO", "NET", "VRAM", "LOGIC", "SOUL", "CORE", "GEAR", "PISTON"};
        String[] status = {"OK", "DONE", "PASS", "READY", "ENGAGED"};

        if (progress < 0.25f) {
            return String.format("[INIT] %s_BUS_%02X ... %s",
                    components[random.nextInt(components.length)],
                    random.nextInt(256),
                    status[random.nextInt(status.length)]);
        } else if (progress < 0.5f) {
            return String.format("[WARN] HUMANITY_SECTOR_%04X ... PURGED", random.nextInt(0xFFFF));
        } else if (progress < 0.8f) {
            return String.format("[CRIT] DIVINE_INJECT_%.1f%% ... PROCESSING", progress * 100);
        } else {
            String[] finalLogs = {
                    "[SYS] CONSCIOUSNESS_TRANSFER ... COMPLETE",
                    "[SYS] MORTAL_SHELL ... DISCARDED",
                    "[SYS] DIVINE_KERNEL ... LOADED",
                    "[SYS] MEKHANE_PROTOCOL ... ENGAGED",
                    "[SYS] COGWORK_SOUL ... ACTIVATED"
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

    private static void drawFilledCircle(float x, float y, float radius, int color) {
        if (radius < 0.5f) return;
        GlStateManager.disableTexture2D();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;
        GlStateManager.color(r, g, b, a);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        buffer.pos(x, y, 0).endVertex();
        int segments = Math.max(8, (int) (radius * 2));
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            buffer.pos(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius, 0).endVertex();
        }
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
    }

    private static void drawDivinePolygon(float x, float y, float radius, int sides, float thickness, int color) {
        if (sides < 3 || radius < 1) return;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        float halfThick = thickness / 2.0f;
        float innerRadius = radius - halfThick;
        float outerRadius = radius + halfThick;

        for (int i = 0; i <= sides; i++) {
            double angle = (Math.PI * 2 * i / sides) - Math.PI / 2;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            buffer.pos(x + cos * outerRadius, y + sin * outerRadius, 0).color(r, g, b, 0).endVertex();
            buffer.pos(x + cos * innerRadius, y + sin * innerRadius, 0).color(r, g, b, a).endVertex();
        }
        Tessellator.getInstance().draw();

        GL11.glLineWidth(1.0f);
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < sides; i++) {
            double angle = (Math.PI * 2 * i / sides) - Math.PI / 2;
            buffer.pos(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius, 0)
                    .color(1.0f, 1.0f, 1.0f, a * 0.8f).endVertex();
        }
        Tessellator.getInstance().draw();

        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
    }

    private static void drawDiamondFrame(float radius, int color) {
        GlStateManager.disableTexture2D();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(1.5f);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        buffer.pos(0, -radius, 0).endVertex();
        buffer.pos(radius, 0, 0).endVertex();
        buffer.pos(0, radius, 0).endVertex();
        buffer.pos(-radius, 0, 0).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
    }

    private static void drawCross(float size, float width, int color) {
        GlStateManager.disableTexture2D();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(width);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        buffer.pos(-size, 0, 0).endVertex();
        buffer.pos(size, 0, 0).endVertex();
        buffer.pos(0, -size, 0).endVertex();
        buffer.pos(0, size, 0).endVertex();
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

    private static class SensoryGhost {
        float x, y, size, life, drift;
        int type;
    }

    enum ShardState {FLOATING, FLEEING, CAPTURED, DISSOLVING}

    private static class MemoryShard {
        float x, y, vx, vy, life;
        String text;
        ShardState state;
        float pixelateProgress;
    }

    private static class FormatBlock {
        int y, height;
        float targetAlpha, currentAlpha;
    }

    private static class DivineSigil {
        float x, y, vx, vy, life, scale;
        String rune;
    }
}