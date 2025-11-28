package com.moremod.client.gui;

import com.moremod.client.KeyBindHandler;
import com.moremod.config.FleshRejectionConfig;
import com.moremod.config.HumanityConfig;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.system.FleshRejectionSystem;
import com.moremod.system.humanity.AscensionRoute;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.*;

@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class SmartRejectionGuide extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // 详细信息显示状态
    private static boolean showingDetailedStatus = false;
    private static int detailDisplayTicks = 0;
    private static final int DETAIL_DISPLAY_DURATION = 200; // 10秒

    // 引导信息类
    private static class GuideInfo {
        String title;
        List<String> tips;
        int displayTicks;
        int priority;

        GuideInfo(String title, int ticks, int priority, String... tips) {
            this.title = title;
            this.tips = Arrays.asList(tips);
            this.displayTicks = ticks;
            this.priority = priority;
        }
    }

    // 状态跟踪
    private static GuideInfo currentGuide = null;
    private static GuideInfo pendingGuide = null; // 缓存的待显示guide
    private static int remainingTicks = 0;
    private static float lastRejection = -1;
    private static float lastAdaptation = -1;
    private static float lastHumanity = -1;
    private static boolean wasInHumanitySystem = false; // 追踪是否在人性值系统中
    private static Set<String> shownMilestones = new HashSet<>();
    private static boolean hadCore = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.player == null) return;

        // 详细状态倒计时
        if (detailDisplayTicks > 0) {
            detailDisplayTicks--;
            if (detailDisplayTicks == 0) {
                showingDetailedStatus = false;
                // 恢复缓存的guide
                if (pendingGuide != null) {
                    currentGuide = pendingGuide;
                    remainingTicks = pendingGuide.displayTicks;
                    pendingGuide = null;
                }
            }
        }

        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(mc.player);
        boolean hasCore = !core.isEmpty();

        if (!hasCore) {
            if (hadCore) {
                // 刚卸下核心
                lastRejection = -1;
                lastAdaptation = -1;
                currentGuide = null;
                pendingGuide = null;
                hadCore = false;
            }
            return;
        }

        // 使用FleshRejectionSystem API获取数据
        float rejection = FleshRejectionSystem.getRejectionLevel(mc.player);
        float adaptation = FleshRejectionSystem.getAdaptationLevel(mc.player);
        boolean transcended = FleshRejectionSystem.hasTranscended(mc.player);

        // 获取人性值数据
        IHumanityData humanityData = HumanityCapabilityHandler.getData(mc.player);
        float humanity = humanityData != null ? humanityData.getHumanity() : 75f;
        boolean humanitySystemActive = humanityData != null && humanityData.isSystemActive()
                && transcended && rejection <= 0;

        // 首次装备检测
        if (!hadCore) {
            hadCore = true;
            showFirstEquipGuide();
        }

        // 检测系统切换
        if (humanitySystemActive != wasInHumanitySystem) {
            if (humanitySystemActive) {
                // 进入人性值系统
                showEnterHumanitySystemGuide(humanity);
            } else if (wasInHumanitySystem) {
                // 离开人性值系统（排异重新激活）
                showLeaveHumanitySystemGuide();
            }
            wasInHumanitySystem = humanitySystemActive;
        }

        // 检测破碎之神升格
        AscensionRoute route = humanityData != null ? humanityData.getAscensionRoute() : AscensionRoute.NONE;
        if (route == AscensionRoute.BROKEN_GOD && !shownMilestones.contains("brokenGod")) {
            shownMilestones.add("brokenGod");
            showBrokenGodAscensionGuide();
        }

        // 只有在不显示详细信息时才检查里程碑
        if (!showingDetailedStatus) {
            if (humanitySystemActive) {
                // 人性值系统里程碑
                checkHumanityMilestones(humanity);
            } else {
                // 排异系统里程碑
                checkMilestones(rejection, adaptation, transcended);
            }
        }

        // 更新记录
        lastRejection = rejection;
        lastAdaptation = adaptation;
        lastHumanity = humanity;

        // Guide倒计时（只在不显示详细信息时）
        if (remainingTicks > 0 && !showingDetailedStatus) {
            remainingTicks--;
            if (remainingTicks == 0) {
                currentGuide = null;
            }
        }
    }

    // ========== 人性值系统引导 ==========

    private static void showEnterHumanitySystemGuide(float humanity) {
        showGuide(new GuideInfo(
                "§d✦ 人性值系统已激活", 300, 10,
                "§7你已完成人机融合，进入人性值阶段",
                String.format("§7当前人性值: §f%.0f%%", humanity),
                "§b高人性(>60%): §7猎人协议，精准打击",
                "§5低人性(<40%): §7异常协议，存在扭曲",
                "§e灰域(40-60%): §7不稳定，量子叠加"
        ), true);
    }

    private static void showLeaveHumanitySystemGuide() {
        showGuide(new GuideInfo(
                "§4⚠ 人性值系统已关闭", 200, 10,
                "§c排异反应重新激活！",
                "§7人机融合状态中断...",
                "§7需要重新达成突破条件"
        ), true);
    }

    private static void showBrokenGodAscensionGuide() {
        String keyName = getKeyDisplayName();
        showGuide(new GuideInfo(
                "§5◈ 你已成为破碎之神 ◈", 400, 10,
                "§7你的人性已完全消散...",
                "§6基础: §f关机模式、暴击×2、真伤、畸变脉冲",
                "§d遗物: §f攻速×3、生命偷取、处决、时停",
                "§c代价: §7药水无效、NPC无视、链结站禁用",
                "§b按 [" + keyName + "] 查看完整能力列表"
        ), true);
    }

    private static void checkHumanityMilestones(float humanity) {
        // 高人性里程碑
        if (humanity >= 80 && !shownMilestones.contains("hum80")) {
            shownMilestones.add("hum80");
            showGuide(new GuideInfo(
                    "§a◈ 高度人性", 180, 5,
                    String.format("§7人性值: §a%.0f%%", humanity),
                    "§a效果: §7猎人协议全面激活",
                    "§a效果: §7生物档案槽位最大化",
                    "§7保持人类行为维持人性"
            ), false);
        }
        else if (humanity >= 60 && humanity < 80 && !shownMilestones.contains("hum60")) {
            shownMilestones.add("hum60");
            showGuide(new GuideInfo(
                    "§b◈ 稳定人性", 160, 4,
                    String.format("§7人性值: §b%.0f%%", humanity),
                    "§b效果: §7猎人协议可用",
                    "§7可使用生物档案系统"
            ), false);
        }
        // 灰域警告
        else if (humanity >= 40 && humanity < 60 && !shownMilestones.contains("humGrey")) {
            shownMilestones.add("humGrey");
            showGuide(new GuideInfo(
                    "§e⚡ 进入灰域", 200, 6,
                    String.format("§7人性值: §e%.0f%%", humanity),
                    "§e警告: §7存在状态不稳定",
                    "§e效果: §7量子叠加可能触发",
                    "§e效果: §7致命伤害时可能坍缩"
            ), false);
        }
        // 低人性里程碑
        else if (humanity >= 25 && humanity < 40 && !shownMilestones.contains("hum25")) {
            shownMilestones.add("hum25");
            showGuide(new GuideInfo(
                    "§5◈ 低人性状态", 200, 6,
                    String.format("§7人性值: §5%.0f%%", humanity),
                    "§5效果: §7异常协议激活",
                    "§5效果: §7异常场开始影响周围",
                    "§c警告: §7治疗效果降低"
            ), false);
        }
        else if (humanity >= 10 && humanity < 25 && !shownMilestones.contains("hum10")) {
            shownMilestones.add("hum10");
            showGuide(new GuideInfo(
                    "§4⚠ 极低人性", 200, 7,
                    String.format("§7人性值: §4%.0f%%", humanity),
                    "§4效果: §7异常协议强化",
                    "§4效果: §7畸变脉冲可能触发",
                    "§c危险: §7接近存在崩解边缘"
            ), true);
        }
        else if (humanity <= 5 && !shownMilestones.contains("humCritical")) {
            shownMilestones.add("humCritical");
            showGuide(new GuideInfo(
                    "§4⚠⚠ 存在崩解警告", 240, 10,
                    String.format("§c人性值: §4%.1f%%", humanity),
                    "§4危险: §c即将触发存在崩解！",
                    "§4崩解中必须存活60秒",
                    "§6立即进行人类活动！"
            ), true);
        }
    }

    private static void showFirstEquipGuide() {
        String keyName = getKeyDisplayName();
        showGuide(new GuideInfo(
                "§6⚙ 机械核心激活", 240, 10,
                "§e排异系统已启动",
                "§7排异值会随时间缓慢增长",
                "§c饥饿、受伤、药水§7会加速排异",
                "§a睡眠和急救物品§7可缓解排异",
                "§b按 [" + keyName + "] 键查看详细状态"
        ), true);
    }

    private static void checkMilestones(float rejection, float adaptation, boolean transcended) {
        // 排异值里程碑
        if (rejection >= 20 && !shownMilestones.contains("rej20")) {
            shownMilestones.add("rej20");
            showGuide(new GuideInfo(
                    "§e⚠ 轻度排异反应", 180, 5,
                    String.format("§7当前排异: §e%.1f%%", rejection),
                    "§c效果：§7药水效果轻微减弱",
                    "§a建议：§7保持饱食度满格",
                    "§a建议：§7定期睡眠休息"
            ), false);
        }
        else if (rejection >= 40 && !shownMilestones.contains("rej40")) {
            shownMilestones.add("rej40");
            showGuide(new GuideInfo(
                    "§6⚡ 中度排异反应", 200, 6,
                    String.format("§7当前排异: §6%.1f%%", rejection),
                    "§c新效果：§7视野出现血色暗角",
                    "§c新效果：§7开始听到心跳声",
                    "§c警告：§7攻击可能失误",
                    "§e需要：§7考虑使用急救物品"
            ), false);
        }
        else if (rejection >= 60 && !shownMilestones.contains("rej60")) {
            shownMilestones.add("rej60");
            showGuide(new GuideInfo(
                    "§c⚠ 严重排异反应", 200, 7,
                    String.format("§7当前排异: §c%.1f%%", rejection),
                    "§c危险：§7药水容量受限",
                    "§c危险：§7部分药水失效",
                    "§c危险：§7无敌帧缩短",
                    "§6急需：§7使用强效急救包！"
            ), true);
        }
        else if (rejection >= 80 && !shownMilestones.contains("rej80")) {
            shownMilestones.add("rej80");
            showGuide(new GuideInfo(
                    "§4⚠⚠ 排异临界状态", 240, 9,
                    String.format("§c当前排异: §4%.1f%%", rejection),
                    "§4致命：§c正面药水完全无效",
                    "§4致命：§c严重出血风险",
                    "§4致命：§c神经严重错乱",
                    "§e立即使用急救包或校准器！"
            ), true);
        }

        // 90%+的危急警告
        if (rejection >= 90 && !shownMilestones.contains("critical_" + (int)rejection)) {
            shownMilestones.add("critical_" + (int)rejection);
            showGuide(new GuideInfo(
                    "§4⚠ 极限排异！", 100, 10,
                    String.format("§c排异值: §4%.1f%%", rejection),
                    "§4战斗能力严重受损！",
                    "§c无法正常治疗！"
            ), true);
        }

        // 95%+的特殊警告
        if (rejection >= 95 && !shownMilestones.contains("extreme_" + (int)rejection)) {
            shownMilestones.add("extreme_" + (int)rejection);
            showGuide(new GuideInfo(
                    "§4⚠ 血肉完全排斥机械！", 120, 10,
                    String.format("§4排异值: %.1f%%", rejection),
                    "§c你几乎无法战斗",
                    "§c任何治疗都会失效",
                    "§6必须立即降低排异！"
            ), true);
        }

        // 适应度里程碑
        if (adaptation >= 25 && !shownMilestones.contains("adapt25")) {
            shownMilestones.add("adapt25");
            showGuide(new GuideInfo(
                    "§a◈ 初步适应", 160, 4,
                    String.format("§7适应度: §a%.0f%%", adaptation),
                    "§a效果：§7排异增长开始减缓",
                    "§7继续安装模组提升适应"
            ), false);
        }
        else if (adaptation >= 50 && !shownMilestones.contains("adapt50")) {
            shownMilestones.add("adapt50");
            showGuide(new GuideInfo(
                    "§b◈ 稳定融合", 160, 4,
                    String.format("§7适应度: §b%.0f%%", adaptation),
                    "§b效果：§7排异反应明显减弱",
                    "§7距离突破还需50%适应度"
            ), false);
        }
        else if (transcended && !shownMilestones.contains("transcend")) {
            shownMilestones.add("transcend");
            showGuide(new GuideInfo(
                    "§6✨ 血肉超越成功！", 300, 10,
                    "§6恭喜！你已突破血肉极限",
                    "§a永久免疫排异反应",
                    "§e获得完美人机融合"
            ), true);
        }
    }

    private static void showGuide(GuideInfo guide, boolean force) {
        // 如果正在显示详细信息，缓存guide
        if (showingDetailedStatus) {
            if (pendingGuide == null || guide.priority > pendingGuide.priority) {
                pendingGuide = guide;
            }
            return;
        }

        if (currentGuide != null && !force) {
            if (guide.priority <= currentGuide.priority) {
                return;
            }
        }
        currentGuide = guide;
        remainingTicks = guide.displayTicks;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.EXPERIENCE) return;
        if (mc.player == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int baseY = 50;

        // 优先渲染详细状态
        if (showingDetailedStatus) {
            renderDetailedStatus(centerX, baseY);
        }
        // 否则渲染guide
        else if (currentGuide != null) {
            renderGuide(centerX, baseY);
        }
    }

    private static void renderGuide(int centerX, int y) {
        FontRenderer fr = mc.fontRenderer;

        // 淡入淡出
        float alpha = 1.0f;
        if (remainingTicks < 20) {
            alpha = remainingTicks / 20f;
        } else if (remainingTicks > currentGuide.displayTicks - 20) {
            alpha = (currentGuide.displayTicks - remainingTicks) / 20f;
        }

        int alphaInt = (int)(alpha * 255);

        // 渲染标题
        int titleWidth = fr.getStringWidth(currentGuide.title);
        drawRect(centerX - titleWidth/2 - 5, y - 2,
                centerX + titleWidth/2 + 5, y + 11,
                (int)(alpha * 200) << 24);
        fr.drawStringWithShadow(currentGuide.title,
                centerX - titleWidth/2, y,
                0xFFFFFF | (alphaInt << 24));

        // 渲染提示
        y += 16;
        for (String tip : currentGuide.tips) {
            int tipWidth = fr.getStringWidth(tip);
            drawRect(centerX - tipWidth/2 - 3, y - 1,
                    centerX + tipWidth/2 + 3, y + 10,
                    (int)(alpha * 150) << 24);
            fr.drawStringWithShadow(tip,
                    centerX - tipWidth/2, y,
                    0xFFFFFF | (alphaInt << 24));
            y += 12;
        }
    }

    private static String getKeyDisplayName() {
        if (KeyBindHandler.rejectionStatusKey != null) {
            String keyName = KeyBindHandler.rejectionStatusKey.getDisplayName();
            if (keyName != null && !keyName.isEmpty()) {
                return keyName;
            }
            // 如果没有显示名称，获取按键码
            int keyCode = KeyBindHandler.rejectionStatusKey.getKeyCode();
            if (keyCode > 0) {
                return Keyboard.getKeyName(keyCode);
            }
        }
        return "K"; // 默认值
    }

    private static void renderDetailedStatus(int centerX, int y) {
        FleshRejectionSystem.RejectionStatus rejStatus = FleshRejectionSystem.getStatus(mc.player);
        if (rejStatus == null) return;

        // 检查是否在人性值系统中
        IHumanityData humanityData = HumanityCapabilityHandler.getData(mc.player);
        boolean inHumanitySystem = humanityData != null && humanityData.isSystemActive()
                && rejStatus.transcended && rejStatus.rejection <= 0;
        boolean isBrokenGod = humanityData != null && humanityData.getAscensionRoute() == AscensionRoute.BROKEN_GOD;

        FontRenderer fr = mc.fontRenderer;

        // 淡入淡出
        float alpha = 1.0f;
        if (detailDisplayTicks < 20) {
            alpha = detailDisplayTicks / 20f;
        } else if (detailDisplayTicks > DETAIL_DISPLAY_DURATION - 20) {
            alpha = (DETAIL_DISPLAY_DURATION - detailDisplayTicks) / 20f;
        }
        int alphaInt = (int)(alpha * 255);
        int bgAlpha = (int)(alpha * 200);

        if (isBrokenGod) {
            // ========== 破碎之神详细状态 ==========
            y = renderBrokenGodDetailedStatus(centerX, y, humanityData, fr, alphaInt, bgAlpha);
        } else if (inHumanitySystem) {
            // ========== 人性值系统详细状态 ==========
            y = renderHumanityDetailedStatus(centerX, y, humanityData, fr, alphaInt, bgAlpha);
        } else {
            // ========== 排异系统详细状态 ==========
            y = renderRejectionDetailedStatus(centerX, y, rejStatus, fr, alphaInt, bgAlpha);
        }

        // 底部提示
        y += 4;
        String keyName = getKeyDisplayName();
        String hint = "§7按 [" + keyName + "] 关闭";
        int hintWidth = fr.getStringWidth(hint);
        fr.drawStringWithShadow(hint, centerX - hintWidth/2, y,
                0x777777 | (alphaInt << 24));
    }

    // ========== 排异系统详细状态渲染 ==========
    private static int renderRejectionDetailedStatus(int centerX, int y,
            FleshRejectionSystem.RejectionStatus status, FontRenderer fr, int alphaInt, int bgAlpha) {

        // 标题
        String title = "§6⚙ 血肉排异详细状态";
        int titleWidth = fr.getStringWidth(title);
        drawRect(centerX - titleWidth/2 - 8, y - 2,
                centerX + titleWidth/2 + 8, y + 11,
                bgAlpha << 24);
        fr.drawStringWithShadow(title, centerX - titleWidth/2, y,
                0xFFD700 | (alphaInt << 24));
        y += 16;

        // 分隔线
        String separator = "§7═════════════════";
        int sepWidth = fr.getStringWidth(separator);
        fr.drawStringWithShadow(separator, centerX - sepWidth/2, y,
                0x777777 | (alphaInt << 24));
        y += 12;

        // 排异值
        TextFormatting rejColor = getColorForRejection(status.rejection);
        String rejText = rejColor + String.format("排异: %.1f%%", status.rejection) +
                " §7(+" + String.format("%.2f", status.growthRate) + "/s)";
        int rejWidth = fr.getStringWidth(rejText);
        drawRect(centerX - rejWidth/2 - 5, y - 1,
                centerX + rejWidth/2 + 5, y + 10,
                (bgAlpha - 50) << 24);
        fr.drawStringWithShadow(rejText, centerX - rejWidth/2, y,
                0xFFFFFF | (alphaInt << 24));
        y += 12;

        // 适应度
        TextFormatting adaptColor = getColorForAdaptation(status.adaptation);
        String adaptText = adaptColor + String.format("适应度: %.0f%%", status.adaptation);
        int adaptWidth = fr.getStringWidth(adaptText);
        drawRect(centerX - adaptWidth/2 - 5, y - 1,
                centerX + adaptWidth/2 + 5, y + 10,
                (bgAlpha - 50) << 24);
        fr.drawStringWithShadow(adaptText, centerX - adaptWidth/2, y,
                0xFFFFFF | (alphaInt << 24));
        y += 12;

        // 模组状态
        String modText = "§7模组运行: §f" + status.running + "/" + status.installed;
        int modWidth = fr.getStringWidth(modText);
        drawRect(centerX - modWidth/2 - 5, y - 1,
                centerX + modWidth/2 + 5, y + 10,
                (bgAlpha - 50) << 24);
        fr.drawStringWithShadow(modText, centerX - modWidth/2, y,
                0xFFFFFF | (alphaInt << 24));
        y += 14;

        // 特殊状态
        if (status.transcended) {
            String transText = "§6✨ 已突破血肉极限";
            int transWidth = fr.getStringWidth(transText);
            drawRect(centerX - transWidth/2 - 5, y - 1,
                    centerX + transWidth/2 + 5, y + 10,
                    (bgAlpha - 50) << 24);
            fr.drawStringWithShadow(transText, centerX - transWidth/2, y,
                    0xFFD700 | (alphaInt << 24));
            y += 12;
        }

        if (status.bleeding > 0) {
            String bleedText = "§c💉 出血中: " + (status.bleeding/20) + "秒";
            int bleedWidth = fr.getStringWidth(bleedText);
            drawRect(centerX - bleedWidth/2 - 5, y - 1,
                    centerX + bleedWidth/2 + 5, y + 10,
                    (bgAlpha - 50) << 24);
            fr.drawStringWithShadow(bleedText, centerX - bleedWidth/2, y,
                    0xFF4444 | (alphaInt << 24));
            y += 12;
        }

        // 负面效果标题
        y += 4;
        String debuffTitle = "§c【当前负面效果】";
        int debuffTitleWidth = fr.getStringWidth(debuffTitle);
        drawRect(centerX - debuffTitleWidth/2 - 5, y - 1,
                centerX + debuffTitleWidth/2 + 5, y + 10,
                bgAlpha << 24);
        fr.drawStringWithShadow(debuffTitle, centerX - debuffTitleWidth/2, y,
                0xFF8888 | (alphaInt << 24));
        y += 12;

        // 负面效果列表
        List<String> debuffs = getDebuffsForRejection(status.rejection);

        for (String debuff : debuffs) {
            String debuffText;
            int color = 0xFF6666;

            if (debuff.contains("无敌帧")) {
                debuffText = "§e⚡ " + debuff;
                color = 0xFFFF66;
            } else if (debuff.contains("FA")) {
                debuffText = "§d✖ " + debuff;
                color = 0xFF66FF;
            } else if (debuff.contains("崩溃") || debuff.contains("不兼容")) {
                debuffText = "§4" + debuff;
                color = 0x880000;
            } else {
                debuffText = "§c• " + debuff;
            }

            int debuffWidth = fr.getStringWidth(debuffText);
            drawRect(centerX - debuffWidth/2 - 3, y - 1,
                    centerX + debuffWidth/2 + 3, y + 9,
                    (bgAlpha - 100) << 24);
            fr.drawStringWithShadow(debuffText, centerX - debuffWidth/2, y,
                    color | (alphaInt << 24));
            y += 10;
        }

        return y;
    }

    // ========== 人性值系统详细状态渲染 ==========
    private static int renderHumanityDetailedStatus(int centerX, int y,
            IHumanityData data, FontRenderer fr, int alphaInt, int bgAlpha) {

        float humanity = data.getHumanity();

        // 标题 - 根据人性值区间显示不同标题
        String title;
        int titleColor;
        if (humanity >= 60) {
            title = "§b✦ 人性值系统 - 猎人协议";
            titleColor = 0x55FFFF;
        } else if (humanity >= 40) {
            title = "§e⚡ 人性值系统 - 灰域状态";
            titleColor = 0xFFFF55;
        } else {
            title = "§5◈ 人性值系统 - 异常协议";
            titleColor = 0xAA00AA;
        }

        int titleWidth = fr.getStringWidth(title);
        drawRect(centerX - titleWidth/2 - 8, y - 2,
                centerX + titleWidth/2 + 8, y + 11,
                bgAlpha << 24);
        fr.drawStringWithShadow(title, centerX - titleWidth/2, y,
                titleColor | (alphaInt << 24));
        y += 16;

        // 分隔线
        String separator = "§7═════════════════";
        int sepWidth = fr.getStringWidth(separator);
        fr.drawStringWithShadow(separator, centerX - sepWidth/2, y,
                0x777777 | (alphaInt << 24));
        y += 12;

        // 人性值
        TextFormatting humColor = getColorForHumanity(humanity);
        String humText = humColor + String.format("人性值: %.1f%%", humanity);
        int humWidth = fr.getStringWidth(humText);
        drawRect(centerX - humWidth/2 - 5, y - 1,
                centerX + humWidth/2 + 5, y + 10,
                (bgAlpha - 50) << 24);
        fr.drawStringWithShadow(humText, centerX - humWidth/2, y,
                0xFFFFFF | (alphaInt << 24));
        y += 12;

        // 当前区间
        String zoneText = getHumanityZoneText(humanity);
        int zoneWidth = fr.getStringWidth(zoneText);
        drawRect(centerX - zoneWidth/2 - 5, y - 1,
                centerX + zoneWidth/2 + 5, y + 10,
                (bgAlpha - 50) << 24);
        fr.drawStringWithShadow(zoneText, centerX - zoneWidth/2, y,
                0xFFFFFF | (alphaInt << 24));
        y += 12;

        // 档案槽位
        int maxSlots = data.getMaxActiveProfiles();
        int activeSlots = data.getActiveProfiles().size();
        String slotText = "§7生物档案槽位: §f" + activeSlots + "/" + maxSlots;
        int slotWidth = fr.getStringWidth(slotText);
        drawRect(centerX - slotWidth/2 - 5, y - 1,
                centerX + slotWidth/2 + 5, y + 10,
                (bgAlpha - 50) << 24);
        fr.drawStringWithShadow(slotText, centerX - slotWidth/2, y,
                0xFFFFFF | (alphaInt << 24));
        y += 14;

        // 崩解状态
        if (data.isDissolutionActive()) {
            int dissolveSecs = data.getDissolutionTicks() / 20;
            String dissolveText = "§4⚠ 存在崩解中: " + dissolveSecs + "秒";
            int dissolveWidth = fr.getStringWidth(dissolveText);
            drawRect(centerX - dissolveWidth/2 - 5, y - 1,
                    centerX + dissolveWidth/2 + 5, y + 10,
                    (bgAlpha - 50) << 24);
            fr.drawStringWithShadow(dissolveText, centerX - dissolveWidth/2, y,
                    0xFF4444 | (alphaInt << 24));
            y += 12;
        }

        // 当前效果标题
        y += 4;
        String effectTitle = humanity >= 50 ? "§a【当前增益效果】" : "§5【当前异常效果】";
        int effectTitleWidth = fr.getStringWidth(effectTitle);
        drawRect(centerX - effectTitleWidth/2 - 5, y - 1,
                centerX + effectTitleWidth/2 + 5, y + 10,
                bgAlpha << 24);
        fr.drawStringWithShadow(effectTitle, centerX - effectTitleWidth/2, y,
                (humanity >= 50 ? 0x88FF88 : 0xAA55AA) | (alphaInt << 24));
        y += 12;

        // 效果列表
        List<String> effects = getEffectsForHumanity(humanity);
        for (String effect : effects) {
            int effectWidth = fr.getStringWidth(effect);
            drawRect(centerX - effectWidth/2 - 3, y - 1,
                    centerX + effectWidth/2 + 3, y + 9,
                    (bgAlpha - 100) << 24);
            fr.drawStringWithShadow(effect, centerX - effectWidth/2, y,
                    0xFFFFFF | (alphaInt << 24));
            y += 10;
        }

        // 如何改变人性值标题
        y += 6;
        String changeTitle = humanity >= 50 ? "§7【如何降低人性值】" : "§7【如何提升人性值】";
        int changeTitleWidth = fr.getStringWidth(changeTitle);
        drawRect(centerX - changeTitleWidth/2 - 5, y - 1,
                centerX + changeTitleWidth/2 + 5, y + 10,
                bgAlpha << 24);
        fr.drawStringWithShadow(changeTitle, centerX - changeTitleWidth/2, y,
                0x888888 | (alphaInt << 24));
        y += 12;

        // 改变方法列表
        List<String> changeTips = humanity >= 50 ?
                getHumanityDecreaseTips() : getHumanityIncreaseTips();
        for (String tip : changeTips) {
            int tipWidth = fr.getStringWidth(tip);
            drawRect(centerX - tipWidth/2 - 3, y - 1,
                    centerX + tipWidth/2 + 3, y + 9,
                    (bgAlpha - 100) << 24);
            fr.drawStringWithShadow(tip, centerX - tipWidth/2, y,
                    0xAAAAAA | (alphaInt << 24));
            y += 10;
        }

        return y;
    }

    // ========== 破碎之神详细状态渲染 ==========
    private static int renderBrokenGodDetailedStatus(int centerX, int y,
            IHumanityData data, FontRenderer fr, int alphaInt, int bgAlpha) {

        // 标题
        String title = "§5◈ 破碎之神 ◈";
        int titleWidth = fr.getStringWidth(title);
        drawRect(centerX - titleWidth/2 - 8, y - 2,
                centerX + titleWidth/2 + 8, y + 11,
                bgAlpha << 24);
        fr.drawStringWithShadow(title, centerX - titleWidth/2, y,
                0xAA55FF | (alphaInt << 24));
        y += 16;

        // 分隔线
        String separator = "§7═════════════════════════";
        int sepWidth = fr.getStringWidth(separator);
        fr.drawStringWithShadow(separator, centerX - sepWidth/2, y,
                0x777777 | (alphaInt << 24));
        y += 12;

        // 人性值（固定为0）
        String humText = "§5人性值: §40% §7(永久锁定)";
        int humWidth = fr.getStringWidth(humText);
        drawRect(centerX - humWidth/2 - 5, y - 1,
                centerX + humWidth/2 + 5, y + 10,
                (bgAlpha - 50) << 24);
        fr.drawStringWithShadow(humText, centerX - humWidth/2, y,
                0xFFFFFF | (alphaInt << 24));
        y += 14;

        // ========== 基础能力 ==========
        y = renderBrokenGodSection(centerX, y, fr, alphaInt, bgAlpha,
                "§6【基础能力】", 0xFFAA00, new String[] {
            "§6• §f关机模式: §7濒死时进入20秒无敌",
            "§6• §f暴击伤害×2: §7所有攻击",
            "§6• §f真实伤害: §7部分伤害无视护甲",
            "§6• §f无视无敌帧: §7连续攻击无间隔",
            "§6• §f畸变脉冲: §7受重击自动反击"
        });

        // ========== 破碎_手 ==========
        y = renderBrokenGodSection(centerX, y, fr, alphaInt, bgAlpha,
                "§d【破碎_手】", 0xDD88FF, new String[] {
            "§d• §f攻击速度×3",
            "§d• §f近战伤害+100%",
            "§d• §f攻击后冷却重置"
        });

        // ========== 破碎_心核 ==========
        y = renderBrokenGodSection(centerX, y, fr, alphaInt, bgAlpha,
                "§c【破碎_心核】", 0xFF5555, new String[] {
            "§c• §f最大生命压缩至10HP",
            "§c• §f100%生命偷取",
            "§c• §f溢出治疗→吸收之心(最多20)",
            "§c• §f狂战士: 1HP时伤害×5",
            "§c• §f免疫凋零/中毒/流血"
        });

        // ========== 破碎_臂 ==========
        y = renderBrokenGodSection(centerX, y, fr, alphaInt, bgAlpha,
                "§b【破碎_臂】", 0x55FFFF, new String[] {
            "§b• §f100%暴击率",
            "§b• §f暴击伤害×3",
            "§b• §f攻击距离+3格",
            "§b• §f护甲粉碎场: 10格内敌人护甲归零"
        });

        // ========== 破碎_枷锁 ==========
        y = renderBrokenGodSection(centerX, y, fr, alphaInt, bgAlpha,
                "§e【破碎_枷锁】", 0xFFFF55, new String[] {
            "§e• §f时停领域: 10格内敌人无法移动",
            "§e• §f受到伤害-50%",
            "§e• §c自身移速-30%"
        });

        // ========== 破碎_投影 ==========
        y = renderBrokenGodSection(centerX, y, fr, alphaInt, bgAlpha,
                "§5【破碎_投影】", 0xAA00AA, new String[] {
            "§5• §f幻影打击: +100%真实伤害",
            "§5• §f攻击无视敌人无敌帧",
            "§5• §f处决: <50%血量直接击杀"
        });

        // ========== 破碎_终结 ==========
        y = renderBrokenGodSection(centerX, y, fr, alphaInt, bgAlpha,
                "§4【破碎_终结】", 0xAA0000, new String[] {
            "§4• §f所有伤害×2",
            "§4• §f击杀回复5HP",
            "§4• §f击杀获得10吸收之心"
        });

        // ========== 失去的能力 ==========
        y += 4;
        y = renderBrokenGodSection(centerX, y, fr, alphaInt, bgAlpha,
                "§8【代价】", 0x888888, new String[] {
            "§8• §7药水完全无效",
            "§8• §7无法使用链结站",
            "§8• §7猎人协议失效",
            "§8• §7NPC完全无视你"
        });

        return y;
    }

    // 渲染破碎之神详细状态的一个区块
    private static int renderBrokenGodSection(int centerX, int y, FontRenderer fr,
            int alphaInt, int bgAlpha, String title, int titleColor, String[] lines) {
        // 标题
        int titleWidth = fr.getStringWidth(title);
        drawRect(centerX - titleWidth/2 - 5, y - 1,
                centerX + titleWidth/2 + 5, y + 10,
                bgAlpha << 24);
        fr.drawStringWithShadow(title, centerX - titleWidth/2, y,
                titleColor | (alphaInt << 24));
        y += 11;

        // 内容
        for (String line : lines) {
            int lineWidth = fr.getStringWidth(line);
            drawRect(centerX - lineWidth/2 - 3, y - 1,
                    centerX + lineWidth/2 + 3, y + 9,
                    (bgAlpha - 100) << 24);
            fr.drawStringWithShadow(line, centerX - lineWidth/2, y,
                    0xFFFFFF | (alphaInt << 24));
            y += 10;
        }
        y += 4;
        return y;
    }

    // 获取人性值区间文本
    private static String getHumanityZoneText(float humanity) {
        if (humanity >= 80) return "§a区间: 高度人性 (猎人协议完全激活)";
        if (humanity >= 60) return "§b区间: 稳定人性 (猎人协议可用)";
        if (humanity >= 40) return "§e区间: 灰域 (量子叠加不稳定)";
        if (humanity >= 25) return "§5区间: 低人性 (异常协议激活)";
        if (humanity >= 10) return "§4区间: 极低人性 (异常协议强化)";
        return "§4区间: 临界崩解 (存在不稳定)";
    }

    // 获取人性值对应的效果
    private static List<String> getEffectsForHumanity(float humanity) {
        List<String> effects = new ArrayList<>();

        if (humanity >= 60) {
            // 高人性效果
            effects.add("§a• 猎人协议: 已分析生物伤害+");
            int slots = (int)(humanity / 10f);
            effects.add("§a• 生物档案槽位: " + slots);
            if (humanity >= 80) {
                effects.add("§a• 未知敌人惩罚: 最小化");
            }
        } else if (humanity >= 40) {
            // 灰域效果
            effects.add("§e• 量子叠加: 致命伤害时可能坍缩");
            effects.add("§e• 协议混合: 部分猎人+部分异常");
            effects.add("§7• 异常场: 间歇激活");
        } else {
            // 低人性效果
            float anomalyBonus = humanity <= 10 ? 60 : (humanity <= 25 ? 40 : 20);
            effects.add("§5• 异常伤害加成: +" + (int)anomalyBonus + "%");
            float radius = (50f - humanity) / 10f;
            effects.add("§5• 异常场半径: " + String.format("%.1f", radius) + "格");
            if (humanity <= 25) {
                effects.add("§5• 凋零光环: 激活");
            }
            if (humanity <= 10) {
                effects.add("§4• 畸变脉冲: 可能触发");
            }
            effects.add("§c• 治疗效果: 降低");
        }

        if (effects.isEmpty()) {
            effects.add("§7无特殊效果");
        }

        return effects;
    }

    // 获取提升人性的方法
    private static List<String> getHumanityIncreaseTips() {
        List<String> tips = new ArrayList<>();
        tips.add("§7• 睡眠休息 (最大恢复到75%)");
        tips.add("§7• 食用熟食和复杂料理");
        tips.add("§7• 与村民交易");
        tips.add("§7• 收获作物");
        tips.add("§7• 喂养动物");
        tips.add("§7• 站在阳光下 (主世界)");
        return tips;
    }

    // 获取降低人性的方法
    private static List<String> getHumanityDecreaseTips() {
        List<String> tips = new ArrayList<>();
        tips.add("§7• 击杀村民 (大量消耗)");
        tips.add("§7• 击杀被动动物");
        tips.add("§7• 熬夜不睡觉");
        tips.add("§7• 进入异常维度 (下界/末地)");
        tips.add("§7• 低人性时持续战斗");
        return tips;
    }

    // 获取人性值颜色
    private static TextFormatting getColorForHumanity(float humanity) {
        if (humanity >= 80) return TextFormatting.GREEN;
        if (humanity >= 60) return TextFormatting.AQUA;
        if (humanity >= 40) return TextFormatting.YELLOW;
        if (humanity >= 25) return TextFormatting.LIGHT_PURPLE;
        if (humanity >= 10) return TextFormatting.DARK_PURPLE;
        return TextFormatting.DARK_RED;
    }

    // 获取排异值对应的负面效果
    private static List<String> getDebuffsForRejection(float rejection) {
        List<String> debuffs = new ArrayList<>();

        if (rejection >= 20) {
            debuffs.add("药水效果-20%");
        }
        if (rejection >= 40) {
            debuffs.add("视野暗角");
            debuffs.add("心跳声干扰");
            int missRate = (int)((rejection - 40) * 0.5);
            if (missRate > 0) {
                debuffs.add("攻击失误率+" + missRate + "%");
            }
        }
        if (rejection >= 60) {
            debuffs.add("药水容量严重受限");
            debuffs.add("部分药水直接失效");
            // 无敌帧缩短
            int invulReduction = (int)((rejection - 60) / 40f * 50);
            debuffs.add("无敌帧-" + invulReduction + "%");
            // FirstAid治疗失效
            int healFailRate = (int)((rejection - 60) * 1.875);
            debuffs.add("FA部位治疗失败" + healFailRate + "%");
        }
        if (rejection >= 80) {
            debuffs.add("正面药水完全无效");
            debuffs.add("高概率自伤");
            debuffs.add("神经严重错乱");
            debuffs.add("受伤必定出血");
        }
        if (rejection >= 90) {
            debuffs.add("⚠ 极度虚弱状态");
            debuffs.add("攻击失误率极高");
            debuffs.add("几乎无法治疗");
        }
        if (rejection >= 95) {
            debuffs.add("⚠⚠ 濒临崩溃！");
            debuffs.add("战斗能力基本丧失");
        }
        if (rejection >= 99) {
            debuffs.add("☠ 血肉机械完全不兼容");
        }

        if (debuffs.isEmpty()) {
            debuffs.add("无");
        }

        return debuffs;
    }

    // 颜色辅助方法
    private static TextFormatting getColorForRejection(float rejection) {
        if (rejection >= 80) return TextFormatting.DARK_RED;
        if (rejection >= 60) return TextFormatting.RED;
        if (rejection >= 40) return TextFormatting.GOLD;
        if (rejection >= 20) return TextFormatting.YELLOW;
        return TextFormatting.GREEN;
    }

    private static TextFormatting getColorForAdaptation(float adaptation) {
        if (adaptation >= 75) return TextFormatting.GREEN;
        if (adaptation >= 50) return TextFormatting.AQUA;
        if (adaptation >= 25) return TextFormatting.YELLOW;
        return TextFormatting.RED;
    }

    // 公开方法，供KeyBindHandler调用
    public static void showDetailedStatus() {
        if (mc.player == null) return;

        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(mc.player);
        if (core.isEmpty()) {
            mc.player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "未装备机械核心"), true);
            return;
        }

        // 切换显示状态
        showingDetailedStatus = !showingDetailedStatus;
        if (showingDetailedStatus) {
            detailDisplayTicks = DETAIL_DISPLAY_DURATION;
            // 保存当前guide
            if (currentGuide != null) {
                pendingGuide = currentGuide;
                currentGuide = null;
                remainingTicks = 0;
            }

            // 可选：显示开启提示
            if (FleshRejectionConfig.debugMode) {
                String keyName = getKeyDisplayName();
                mc.player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GRAY + "详细状态已开启 (再按[" + keyName + "]关闭)"), true);
            }
        } else {
            // 恢复缓存的guide
            if (pendingGuide != null) {
                currentGuide = pendingGuide;
                remainingTicks = pendingGuide.displayTicks;
                pendingGuide = null;
            }
        }
    }
}