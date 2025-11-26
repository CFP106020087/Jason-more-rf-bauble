package com.moremod.client.gui;

import com.moremod.client.KeyBindHandler;
import com.moremod.config.FleshRejectionConfig;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.system.FleshRejectionSystem;
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

        // 首次装备检测
        if (!hadCore) {
            hadCore = true;
            showFirstEquipGuide();
        }

        // 只有在不显示详细信息时才检查里程碑
        if (!showingDetailedStatus) {
            checkMilestones(rejection, adaptation, transcended);
        }

        // 更新记录
        lastRejection = rejection;
        lastAdaptation = adaptation;

        // Guide倒计时（只在不显示详细信息时）
        if (remainingTicks > 0 && !showingDetailedStatus) {
            remainingTicks--;
            if (remainingTicks == 0) {
                currentGuide = null;
            }
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
        FleshRejectionSystem.RejectionStatus status = FleshRejectionSystem.getStatus(mc.player);
        if (status == null) return;

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
            int color = 0xFF6666; // 默认红色

            // 对重要效果使用不同颜色
            if (debuff.contains("无敌帧")) {
                debuffText = "§e⚡ " + debuff; // 黄色闪电
                color = 0xFFFF66;
            } else if (debuff.contains("FA")) {
                debuffText = "§d✖ " + debuff; // 紫色X
                color = 0xFF66FF;
            } else if (debuff.contains("崩溃") || debuff.contains("不兼容")) {
                debuffText = "§4" + debuff; // 深红
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

        // 底部提示
        y += 4;
        String keyName = getKeyDisplayName();
        String hint = "§7按 [" + keyName + "] 关闭";
        int hintWidth = fr.getStringWidth(hint);
        fr.drawStringWithShadow(hint, centerX - hintWidth/2, y,
                0x777777 | (alphaInt << 24));
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