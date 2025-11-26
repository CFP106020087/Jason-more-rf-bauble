package com.moremod.client.gui;

import com.moremod.client.KeyBindHandler;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.UpgradeType;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import com.moremod.upgrades.WaterproofUpgrade;
import com.moremod.upgrades.WetnessSystem;
import com.moremod.system.FleshRejectionSystem;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import com.moremod.system.humanity.AscensionRoute;
import com.moremod.config.MechanicalCoreHUDConfig;
import com.moremod.config.FleshRejectionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

/**
 * 机械核心HUD - 完整升级显示版本
 * 修改版：使用 KeyBindHandler 管理所有按键功能
 * 修复版：排异值从核心NBT读取，解决客户端不显示问题
 */
@SideOnly(Side.CLIENT)
public class MechanicalCoreHUD extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // 能量流追踪
    private static class EnergyTracker {
        private static final int SAMPLE_SIZE = 20;
        private static final Queue<Integer> energySamples = new LinkedList<>();
        private static int lastEnergy = -1;
        private static long lastUpdateTick = 0;
        private static int currentNetFlow = 0;

        public static void update(int currentEnergy) {
            try {
                long currentTick = mc.world != null ? mc.world.getTotalWorldTime() : 0;

                if (lastEnergy == -1) {
                    lastEnergy = currentEnergy;
                    lastUpdateTick = currentTick;
                    return;
                }

                if (currentTick > lastUpdateTick) {
                    int energyDiff = currentEnergy - lastEnergy;
                    energySamples.offer(energyDiff);

                    while (energySamples.size() > SAMPLE_SIZE) {
                        energySamples.poll();
                    }

                    if (!energySamples.isEmpty()) {
                        int sum = 0;
                        for (int sample : energySamples) {
                            sum += sample;
                        }
                        currentNetFlow = sum * 20 / energySamples.size();
                    }

                    lastEnergy = currentEnergy;
                    lastUpdateTick = currentTick;
                }
            } catch (Exception e) {
                reset();
            }
        }

        public static int getNetFlow() {
            return currentNetFlow;
        }

        public static void reset() {
            energySamples.clear();
            lastEnergy = -1;
            currentNetFlow = 0;
            lastUpdateTick = 0;
        }
    }

    // 动画相关
    private static float animationTick = 0;
    private static float pulseAnimation = 0;
    private static boolean expanding = true;

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        try {
            if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) {
                return;
            }

            // 检查HUD是否可见（由KeyBindHandler控制）
            if (!KeyBindHandler.isHudVisible()) {
                return;
            }

            EntityPlayer player = mc.player;
            if (player == null || mc.world == null) return;

            ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
            if (coreStack.isEmpty()) {
                EnergyTracker.reset();
                return;
            }

            IEnergyStorage energy = ItemMechanicalCore.getEnergyStorage(coreStack);
            if (energy == null) return;

            // 更新能量追踪
            EnergyTracker.update(energy.getEnergyStored());

            // 更新动画
            if (MechanicalCoreHUDConfig.shouldUseAnimations()) {
                updateAnimations();
            }

            // 渲染HUD
            renderMechanicalCoreHUD(coreStack, energy);

        } catch (Exception e) {
            System.err.println("[MechanicalCoreHUD] 渲染时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 渲染机械核心HUD
     */
    private void renderMechanicalCoreHUD(ItemStack coreStack, IEnergyStorage energy) {
        try {
            ScaledResolution resolution = new ScaledResolution(mc);
            FontRenderer fontRenderer = mc.fontRenderer;

            // 计算HUD位置
            int hudX = calculateHudX(resolution);
            int hudY = calculateHudY(resolution);
            int hudWidth = 120;

            int currentEnergy = energy.getEnergyStored();
            int maxEnergy = energy.getMaxEnergyStored();
            float energyPercent = maxEnergy > 0 ? (float) currentEnergy / maxEnergy : 0;

            // 应用缩放
            GlStateManager.pushMatrix();
            GlStateManager.scale(MechanicalCoreHUDConfig.scale, MechanicalCoreHUDConfig.scale, 1.0);

            hudX = (int)(hudX / MechanicalCoreHUDConfig.scale);
            hudY = (int)(hudY / MechanicalCoreHUDConfig.scale);

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO
            );

            // 渲染背景
            renderBackground(hudX, hudY, hudWidth, energyPercent);

            // 渲染标题
            EnergyDepletionManager.EnergyStatus energyStatus = ItemMechanicalCore.getEnergyStatus(coreStack);
            String title = getStatusIcon(energyStatus) + " " + TextFormatting.DARK_AQUA + "机械核心";
            fontRenderer.drawStringWithShadow(title, hudX + 5, hudY + 5, 0xFFFFFF);

            // 渲染能量条
            int barY = hudY + 18;
            renderEnergyBar(hudX + 5, barY, hudWidth - 10, 6, energyPercent);

            // 渲染能量数值
            String energyText = formatEnergy(currentEnergy) + " / " + formatEnergy(maxEnergy) + " RF";
            fontRenderer.drawStringWithShadow(energyText, hudX + 5, barY + 9, getEnergyColor(energyPercent));

            // 渲染百分比
            String percentText = String.format("%.1f%%", energyPercent * 100);
            TextFormatting percentColor = getPercentColor(energyPercent);
            fontRenderer.drawStringWithShadow(
                    percentColor + percentText,
                    hudX + hudWidth - fontRenderer.getStringWidth(percentText) - 5,
                    barY + 9,
                    0xFFFFFF
            );

            int currentY = barY + 20;

            // 显示能量状态
            if (energyStatus != EnergyDepletionManager.EnergyStatus.NORMAL) {
                String statusText = energyStatus.color + energyStatus.displayName;
                fontRenderer.drawStringWithShadow(statusText, hudX + 5, currentY, 0xFFFFFF);
                currentY += 12;
            }

            // ========== 排异值显示（客户端从核心NBT读取）==========
            float rejection = getClientRejectionLevel(coreStack);
            boolean transcended = getClientTranscendedStatus(coreStack);

            // ========== 修改后的潮湿值显示 ==========
            EntityPlayer player = mc.player;
            int wetness = WetnessSystem.getWetness(player);

            if (wetness > 0) {
                currentY += 2;

                int wetnessBarY = currentY;
                int wetnessBarWidth = hudWidth - 10;
                int wetnessBarHeight = 4;

                drawRect(hudX + 5, wetnessBarY, hudX + 5 + wetnessBarWidth, wetnessBarY + wetnessBarHeight, 0xFF000000);

                TextFormatting wetnessColor;
                int barColor;
                if (wetness >= 80) {
                    wetnessColor = TextFormatting.RED;
                    barColor = 0xFFFF0000;
                } else if (wetness >= 60) {
                    wetnessColor = TextFormatting.GOLD;
                    barColor = 0xFFFFAA00;
                } else if (wetness >= 30) {
                    wetnessColor = TextFormatting.YELLOW;
                    barColor = 0xFFFFFF00;
                } else {
                    wetnessColor = TextFormatting.AQUA;
                    barColor = 0xFF00AAFF;
                }

                int malfunctionLevel = WetnessSystem.getMalfunctionLevel(player);
                if (malfunctionLevel > 0 && animationTick % 20 < 10) {
                    barColor = 0xFFDC143C;
                }

                int fillWidth = (int)((float)wetness / 100f * wetnessBarWidth);
                drawRect(hudX + 5, wetnessBarY, hudX + 5 + fillWidth, wetnessBarY + wetnessBarHeight, barColor);

                // ✅ 第一行：只显示潮湿度数值
                String wetnessText = "💧 潮濕度: " + wetness + "%";
                fontRenderer.drawStringWithShadow(
                        wetnessColor + wetnessText,
                        hudX + 5,
                        wetnessBarY + 6,
                        0xFFFFFF
                );

                currentY = wetnessBarY + 16;

                // ✅ 第二行：显示状态（防护 + 故障）
                int waterproofLevel = WaterproofUpgrade.getEffectiveWaterproofLevel(coreStack);
                String statusLine = "";
                TextFormatting statusColor = TextFormatting.GRAY;

                if (waterproofLevel >= 2) {
                    statusLine = "  [完全防護]";
                    statusColor = TextFormatting.GREEN;
                } else if (waterproofLevel == 1) {
                    statusLine = "  [部分防護]";
                    statusColor = TextFormatting.YELLOW;
                }

                if (malfunctionLevel > 0) {
                    statusLine += TextFormatting.RED + " [故障LV" + (malfunctionLevel + 1) + "]";
                }

                if (!statusLine.isEmpty()) {
                    fontRenderer.drawStringWithShadow(statusColor + statusLine, hudX + 5, currentY, 0xFFFFFF);
                    currentY += 10;
                }

                // 雨中警告
                if (player.world.isRaining() &&
                        player.world.canSeeSky(player.getPosition()) &&
                        player.world.getPrecipitationHeight(player.getPosition()).getY() <= player.posY) {

                    String rainWarning;
                    TextFormatting rainColor;

                    if (player.world.isThundering()) {
                        rainWarning = "⛈ 雷雨中";
                        rainColor = TextFormatting.DARK_PURPLE;
                        if (waterproofLevel < 2) {
                            rainWarning += " - 快速受潮";
                        } else {
                            rainWarning += " - 已防護";
                        }
                    } else {
                        rainWarning = "☔ 雨中";
                        rainColor = TextFormatting.BLUE;
                        if (waterproofLevel < 2) {
                            rainWarning += " - 受潮中";
                        } else {
                            rainWarning += " - 已防護";
                        }
                    }

                    fontRenderer.drawStringWithShadow(rainColor + rainWarning, hudX + 5, currentY, 0xFFFFFF);
                    currentY += 10;
                }
            }
            // ========== 潮湿值显示结束 ==========

            // ========== 修改后的排异值显示（从核心NBT读取）==========
            if (!transcended && rejection > 0) {
                currentY += 2;

                int rejectionBarY = currentY;
                int rejectionBarWidth = hudWidth - 10;
                int rejectionBarHeight = 4;

                drawRect(hudX + 5, rejectionBarY, hudX + 5 + rejectionBarWidth, rejectionBarY + rejectionBarHeight, 0xFF000000);

                TextFormatting rejectionColor;
                int barColor;
                if (rejection >= 80) {
                    rejectionColor = TextFormatting.DARK_RED;
                    barColor = 0xFFDC143C;
                } else if (rejection >= 60) {
                    rejectionColor = TextFormatting.RED;
                    barColor = 0xFFFF4444;
                } else if (rejection >= 40) {
                    rejectionColor = TextFormatting.GOLD;
                    barColor = 0xFFFFAA00;
                } else if (rejection >= 20) {
                    rejectionColor = TextFormatting.YELLOW;
                    barColor = 0xFFFFFF00;
                } else {
                    rejectionColor = TextFormatting.GREEN;
                    barColor = 0xFF88FF88;
                }

                if (rejection >= 80 && animationTick % 20 < 10) {
                    barColor = 0xFFFF0000;
                }

                int fillWidth = (int)(rejection / 100f * rejectionBarWidth);
                drawRect(hudX + 5, rejectionBarY, hudX + 5 + fillWidth, rejectionBarY + rejectionBarHeight, barColor);

                RejectionDisplayInfo rInfo = getClientRejectionInfo(coreStack);

                // ✅ 第一行：只显示排异值和增长速率
                String rejectionText = "⚠ 排異: " + String.format("%.1f%%", rejection);

                if (rInfo != null && rInfo.growthRate > 0) {
                    rejectionText += String.format(" (+%.2f/s)", rInfo.growthRate);
                }

                fontRenderer.drawStringWithShadow(
                        rejectionColor + rejectionText,
                        hudX + 5,
                        rejectionBarY + 6,
                        0xFFFFFF
                );

                currentY = rejectionBarY + 16;

                // ✅ 第二行：显示适应度进度（从核心读取）
                float adaptation = getClientAdaptationLevel(coreStack);
                if (adaptation > 0) {
                    String adaptationText = "  [適應: " + (int)adaptation + "/120]";
                    TextFormatting adaptColor = adaptation >= 120 ? TextFormatting.GREEN : TextFormatting.AQUA;

                    fontRenderer.drawStringWithShadow(
                            adaptColor + adaptationText,
                            hudX + 5,
                            currentY,
                            0xFFFFFF
                    );
                    currentY += 10;
                }

                // 使用 KeyBindHandler 判断是否显示详细信息（从核心读取）
                if (KeyBindHandler.shouldShowDetailedInfo() && rInfo != null) {
                    String detailText = TextFormatting.GRAY + "  運行: " + rInfo.running + "/" + rInfo.installed + " 模組";

                    if (adaptation >= 120) {
                        detailText += TextFormatting.GREEN + " [可突破]";
                    } else if (rInfo.hasSynchronizer) {
                        detailText += TextFormatting.AQUA + " [神經同步]";
                    }

                    fontRenderer.drawStringWithShadow(detailText, hudX + 5, currentY, 0xAAAAAA);
                    currentY += 10;
                }

                // 高排异警告
                if (rejection >= 80) {
                    String warningText = TextFormatting.DARK_RED + "💀 嚴重排異！";
                    if (rejection >= 90) {
                        warningText += " 立即處理";
                    }
                    fontRenderer.drawStringWithShadow(warningText, hudX + 5, currentY, 0xFFFFFF);
                    currentY += 10;
                }
            }

            // 如果已突破，显示状态
            if (transcended) {
                currentY += 2;
                String transcendedText = TextFormatting.AQUA + "✓ 血肉已適應機械化";
                fontRenderer.drawStringWithShadow(transcendedText, hudX + 5, currentY, 0xFFFFFF);
                currentY += 12;
            }
            // ========== 排异值显示结束 ==========

            // ========== 人性值显示 ==========
            // 只有在人性系统激活时才显示（排异期间不显示）
            IHumanityData humanityData = HumanityCapabilityHandler.getData(player);
            if (humanityData != null && humanityData.isSystemActive()) {
                // 再次确认：如果排异值 > 0 或未突破，不显示人性值
                // isSystemActive() 应该已经处理了这个，但为了保险起见再检查一次
                if (transcended && rejection <= 0) {
                    currentY += 2;
                    currentY = renderHumanityInfo(hudX, currentY, hudWidth, humanityData, fontRenderer);
                }
            }
            // ========== 人性值显示结束 ==========

            // 渲染实时能量流
            if (MechanicalCoreHUDConfig.showEnergyFlow) {
                currentY = renderRealTimeEnergyFlow(hudX + 5, currentY, coreStack, fontRenderer);
            }

            // 渲染效率信息
            if (MechanicalCoreHUDConfig.showEfficiency) {
                currentY = renderEfficiencyInfo(hudX + 5, currentY, coreStack, fontRenderer);
            }

            // 渲染活跃升级（完整版）
            if (MechanicalCoreHUDConfig.showActiveUpgrades) {
                currentY = renderAllActiveUpgrades(hudX + 5, currentY, coreStack, fontRenderer, energyStatus);
            }

            // 渲染警告
            if (MechanicalCoreHUDConfig.showWarnings && energyPercent < 0.1f && energyPercent > 0) {
                renderWarning(hudX, currentY, "⚠ 能量严重不足！", fontRenderer);
            }

            GlStateManager.disableBlend();
            GlStateManager.popMatrix();

        } catch (Exception e) {
            GlStateManager.popMatrix();
            System.err.println("[MechanicalCoreHUD] 渲染HUD时出错: " + e.getMessage());
        }
    }

    /**
     * 渲染所有活跃升级 - 完整版本（包含所有升级类型）
     */
    private int renderAllActiveUpgrades(int x, int y, ItemStack coreStack, FontRenderer font,
                                        EnergyDepletionManager.EnergyStatus energyStatus) {
        List<String> activeUpgrades = new ArrayList<>();

        try {
            NBTTagCompound nbt = coreStack.getTagCompound();
            EntityPlayer player = mc.player;

            if (nbt == null) {
                nbt = new NBTTagCompound();
            }

            boolean canUseAllFeatures = (energyStatus == EnergyDepletionManager.EnergyStatus.NORMAL);
            boolean canUseImportantFeatures = (energyStatus == EnergyDepletionManager.EnergyStatus.NORMAL ||
                    energyStatus == EnergyDepletionManager.EnergyStatus.POWER_SAVING);
            boolean canUseEssentialFeatures = (energyStatus != EnergyDepletionManager.EnergyStatus.CRITICAL);

            for (UpgradeType type : UpgradeType.values()) {
                String upgradeId = type.name();
                int level = getUpgradeLevel(coreStack, upgradeId);

                if (level > 0 && !nbt.getBoolean("Disabled_" + upgradeId)) {
                    String upgradeText = checkAndGetUpgradeStatus(type, level, nbt, player, energyStatus,
                            canUseAllFeatures, canUseImportantFeatures, canUseEssentialFeatures);

                    if (upgradeText != null && !upgradeText.isEmpty()) {
                        activeUpgrades.add(upgradeText);
                    }
                }
            }

            if (canUseImportantFeatures &&
                    !nbt.getBoolean("Disabled_FLIGHT_MODULE") &&
                    nbt.getBoolean("FlightModuleEnabled") &&
                    player.capabilities.isFlying) {
                String flightText = TextFormatting.LIGHT_PURPLE + "✈ 飞行";
                if (nbt.getBoolean("FlightHoverMode")) {
                    flightText += " (悬停)";
                }
                int flightLevel = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE);
                if (flightLevel >= 3) {
                    ItemMechanicalCore.SpeedMode speedMode = ItemMechanicalCore.getSpeedMode(coreStack);
                    if (speedMode != ItemMechanicalCore.SpeedMode.NORMAL) {
                        flightText += " " + speedMode.getName();
                    }
                }
                if (!activeUpgrades.contains(flightText)) {
                    activeUpgrades.add(flightText);
                }
            }

            if (energyStatus == EnergyDepletionManager.EnergyStatus.EMERGENCY) {
                activeUpgrades.add(TextFormatting.RED + "⚠ 紧急模式");
            } else if (energyStatus == EnergyDepletionManager.EnergyStatus.CRITICAL) {
                activeUpgrades.add(TextFormatting.DARK_RED + "💀 生命支持模式");
            }

            int disabledCount = countDisabledUpgrades(nbt);
            if (disabledCount > 0) {
                activeUpgrades.add(TextFormatting.DARK_GRAY + "(" + disabledCount + " 已禁用)");
            }

        } catch (Exception e) {
            activeUpgrades.add(TextFormatting.RED + "错误：无法读取升级");
        }

        return renderUpgradeListWithScroll(x, y, activeUpgrades, font);
    }

    private int renderUpgradeListWithScroll(int x, int y, List<String> activeUpgrades, FontRenderer font) {
        try {
            if (activeUpgrades.isEmpty()) {
                font.drawStringWithShadow(
                        TextFormatting.DARK_GRAY + "无活跃功能",
                        x, y, 0x666666
                );
                return y + 12;
            }

            int maxDisplay = MechanicalCoreHUDConfig.getCurrentMaxDisplayUpgrades();
            int scrollOffset = KeyBindHandler.getScrollOffset();
            List<String> displayUpgrades;

            if (activeUpgrades.size() > maxDisplay) {
                if (scrollOffset >= activeUpgrades.size()) {
                    scrollOffset = 0;
                    KeyBindHandler.resetScrollOffset();
                }

                int startIndex = scrollOffset;
                int endIndex = Math.min(startIndex + maxDisplay - 1, activeUpgrades.size());
                displayUpgrades = new ArrayList<>(activeUpgrades.subList(startIndex, endIndex));

                if (MechanicalCoreHUDConfig.showScrollHints) {
                    String scrollHint = TextFormatting.GRAY + String.format("[%d-%d/%d]",
                            startIndex + 1, endIndex, activeUpgrades.size());
                    displayUpgrades.add(scrollHint);
                }
            } else {
                displayUpgrades = activeUpgrades;
            }

            int shown = 0;
            for (String upgrade : displayUpgrades) {
                font.drawStringWithShadow(upgrade, x, y + shown * 10, 0xFFFFFF);
                shown++;
            }

            return y + (shown * 10) + 5;

        } catch (Exception e) {
            return y + 12;
        }
    }

    private int renderRealTimeEnergyFlow(int x, int y, ItemStack coreStack, FontRenderer font) {
        try {
            int netFlow = EnergyTracker.getNetFlow();
            EnergyFlowDetails details = calculateDetailedEnergyFlow(coreStack);

            String flowText;
            if (netFlow > 0) {
                flowText = TextFormatting.GREEN + "▲ +" + netFlow + " RF/s";
            } else if (netFlow < 0) {
                flowText = TextFormatting.RED + "▼ " + netFlow + " RF/s";

                IEnergyStorage energy = ItemMechanicalCore.getEnergyStorage(coreStack);
                if (energy != null && energy.getEnergyStored() > 0 && netFlow < 0) {
                    int seconds = energy.getEnergyStored() / Math.abs(netFlow);
                    if (seconds < 3600) {
                        int minutes = seconds / 60;
                        int secs = seconds % 60;
                        flowText += String.format(" (%d:%02d)", minutes, secs);
                    } else {
                        int hours = seconds / 3600;
                        int minutes = (seconds % 3600) / 60;
                        flowText += String.format(" (%dh%dm)", hours, minutes);
                    }
                }
            } else {
                flowText = TextFormatting.YELLOW + "— 平衡";
            }

            font.drawStringWithShadow(flowText, x, y, 0xFFFFFF);

            if (KeyBindHandler.shouldShowDetailedInfo()) {
                font.drawStringWithShadow(
                        TextFormatting.GRAY + "产: +" + details.generation + " 耗: -" + details.consumption,
                        x, y + 10, 0xAAAAAA
                );
                return y + 20;
            }

            return y + 12;
        } catch (Exception e) {
            return y;
        }
    }

    private String checkAndGetUpgradeStatus(UpgradeType type, int level, NBTTagCompound nbt,
                                            EntityPlayer player, EnergyDepletionManager.EnergyStatus energyStatus,
                                            boolean canUseAllFeatures, boolean canUseImportantFeatures,
                                            boolean canUseEssentialFeatures) {

        switch (type) {
            case ENERGY_CAPACITY:
                if (MechanicalCoreHUDConfig.showPassiveEffects) {
                    return type.getColor() + "⚡ 容量+" + (level * 50000) + "RF";
                }
                break;

            case ENERGY_EFFICIENCY:
                if (level > 0) {
                    int efficiency = level * 15;
                    return type.getColor() + "⚡ 效率-" + efficiency + "%";
                }
                break;

            case ARMOR_ENHANCEMENT:
                if (MechanicalCoreHUDConfig.showPassiveEffects) {
                    return type.getColor() + "🛡 护甲+" + level;
                }
                break;

            case SPEED_BOOST:
            case MOVEMENT_SPEED:
                if (canUseImportantFeatures && (player.motionX != 0 || player.motionZ != 0)) {
                    String speedText = type.getColor() + "⚡ 速度";
                    if (player.isSprinting()) {
                        speedText += " (疾跑)";
                    }
                    return speedText;
                }
                break;

            case REGENERATION:
            case HEALTH_REGEN:
                if (canUseEssentialFeatures && player.getHealth() < player.getMaxHealth()) {
                    return type.getColor() + "❤ 恢复中";
                }
                break;

            case SHIELD_GENERATOR:
            case YELLOW_SHIELD:
                if (canUseEssentialFeatures && player.getAbsorptionAmount() > 0) {
                    float shield = player.getAbsorptionAmount();
                    return type.getColor() + "💛 护盾 " + String.format("%.1f", shield);
                }
                break;

            case TEMPERATURE_CONTROL:
                if (canUseImportantFeatures && isInExtremeTemperature(player)) {
                    return type.getColor() + "🌡 温控激活";
                }
                break;

            case SURVIVAL_PACKAGE:
                if (canUseEssentialFeatures) {
                    return type.getColor() + "🎒 生存套装";
                }
                break;

            case HUNGER_THIRST:
                if (player.getFoodStats().getFoodLevel() < 20) {
                    return type.getColor() + "🍖 饱食度管理";
                }
                break;

            case THORNS:
                if (MechanicalCoreHUDConfig.showCombatInfo && player.getLastAttackedEntityTime() < 100) {
                    return type.getColor() + "🌵 反伤 " + (level * 2);
                }
                break;

            case FIRE_EXTINGUISH:
                if (player.isBurning()) {
                    return type.getColor() + "💧 自动灭火";
                }
                break;

            case WATERPROOF_MODULE:
                if (player.isInWater() || player.isWet()) {
                    return type.getColor() + "💧 防水模块";
                }
                break;

            case ORE_VISION:
                if (canUseAllFeatures && nbt.getBoolean("OreVisionActive")) {
                    return type.getColor() + "⛏ 矿物透视";
                }
                break;

            case STEALTH:
                if (canUseAllFeatures &&
                        (player.getEntityData().getBoolean("MechanicalCoreStealth") || player.isInvisible())) {
                    return type.getColor() + "👁 隐身";
                }
                break;

            case EXP_AMPLIFIER:
                if (MechanicalCoreHUDConfig.showPassiveEffects && player.experienceTotal > 0) {
                    return type.getColor() + "✨ 经验+" + (level * 50) + "%";
                }
                break;

            case DAMAGE_BOOST:
                if (canUseImportantFeatures && MechanicalCoreHUDConfig.showCombatInfo &&
                        player.getLastAttackedEntityTime() < 100) {
                    return type.getColor() + "⚔ 伤害+" + (level * 25) + "%";
                }
                break;

            case ATTACK_SPEED:
                if (canUseImportantFeatures && MechanicalCoreHUDConfig.showCombatInfo &&
                        player.getLastAttackedEntityTime() < 100) {
                    return type.getColor() + "⚔ 攻速+" + (level * 20) + "%";
                }
                break;

            case RANGE_EXTENSION:
                if (canUseImportantFeatures && MechanicalCoreHUDConfig.showCombatInfo) {
                    return type.getColor() + "↔ 范围+" + level;
                }
                break;

            case PURSUIT:
                if (canUseImportantFeatures && player.isSprinting()) {
                    return type.getColor() + "➡ 追击模式";
                }
                break;

            case KINETIC_GENERATOR:
                if (MechanicalCoreHUDConfig.showGenerators &&
                        (player.motionX != 0 || player.motionZ != 0)) {
                    return type.getColor() + "⚙ 动能发电";
                }
                break;

            case SOLAR_GENERATOR:
                if (MechanicalCoreHUDConfig.showGenerators &&
                        mc.world.isDaytime() &&
                        mc.world.canSeeSky(player.getPosition())) {
                    return type.getColor() + "☀ 太阳能";
                }
                break;

            case VOID_ENERGY:
                if (MechanicalCoreHUDConfig.showGenerators) {
                    if (player.dimension == 1 || player.posY < 30) {
                        return type.getColor() + "⚫ 虚空能量";
                    }
                }
                break;

            case COMBAT_CHARGER:
                if (player.getLastAttackedEntityTime() < 100) {
                    return type.getColor() + "⚡ 战斗充能";
                }
                break;
        }

        return null;
    }

    private EnergyFlowDetails calculateDetailedEnergyFlow(ItemStack coreStack) {
        EnergyFlowDetails details = new EnergyFlowDetails();

        try {
            EntityPlayer player = mc.player;
            if (player == null) return details;

            NBTTagCompound nbt = coreStack.getTagCompound();
            if (nbt == null) nbt = new NBTTagCompound();

            int kineticLevel = getUpgradeLevel(coreStack, "KINETIC_GENERATOR");
            if (kineticLevel > 0 && !nbt.getBoolean("Disabled_KINETIC_GENERATOR")) {
                double speed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
                if (speed > 0.05) {
                    int baseGen = 20 + (kineticLevel - 1) * 15;
                    float multiplier = 1.0f;
                    if (player.isSprinting()) multiplier = 1.5f;
                    if (player.capabilities.isFlying) multiplier = 2.0f;
                    details.generation += (int)(baseGen * speed * 20 * multiplier);
                }
            }

            int solarLevel = getUpgradeLevel(coreStack, "SOLAR_GENERATOR");
            if (solarLevel > 0 &&
                    !nbt.getBoolean("Disabled_SOLAR_GENERATOR") &&
                    mc.world.isDaytime() &&
                    mc.world.canSeeSky(player.getPosition())) {
                details.generation += 40 + (solarLevel - 1) * 30;
            }

            int voidLevel = getUpgradeLevel(coreStack, "VOID_ENERGY");
            if (voidLevel > 0 && !nbt.getBoolean("Disabled_VOID_ENERGY")) {
                if (player.dimension == 1) {
                    details.generation += 80 + (voidLevel - 1) * 60;
                } else if (player.posY < 30) {
                    details.generation += 30 + (voidLevel - 1) * 20;
                }
            }

            int combatLevel = getUpgradeLevel(coreStack, "COMBAT_CHARGER");
            if (combatLevel > 0 &&
                    !nbt.getBoolean("Disabled_COMBAT_CHARGER") &&
                    player.getLastAttackedEntityTime() < 100) {
                details.generation += 50 + (combatLevel - 1) * 25;
            }

            int totalUpgrades = getTotalActiveUpgradeLevel(coreStack);
            if (totalUpgrades > 0) {
                details.consumption += 5 + totalUpgrades;
            }

            if (!nbt.getBoolean("Disabled_FLIGHT_MODULE") &&
                    nbt.getBoolean("FlightModuleEnabled") &&
                    player.capabilities.isFlying) {
                int flightLevel = getUpgradeLevel(coreStack, "FLIGHT_MODULE");
                int baseCost = 50 + (flightLevel - 1) * 30;
                if (nbt.getBoolean("FlightHoverMode")) baseCost *= 2;
                details.consumption += baseCost;
            }

            if (player.getAbsorptionAmount() > 0) {
                if (!nbt.getBoolean("Disabled_YELLOW_SHIELD") ||
                        !nbt.getBoolean("Disabled_SHIELD_GENERATOR")) {
                    details.consumption += 10;
                }
            }

            if (!nbt.getBoolean("Disabled_STEALTH") &&
                    (nbt.getBoolean("StealthActive") || player.getEntityData().getBoolean("MechanicalCoreStealth"))) {
                details.consumption += 25;
            }

            if (!nbt.getBoolean("Disabled_ORE_VISION") &&
                    nbt.getBoolean("OreVisionActive")) {
                details.consumption += 10;
            }

            if (player.getHealth() < player.getMaxHealth()) {
                if (!nbt.getBoolean("Disabled_REGENERATION") ||
                        !nbt.getBoolean("Disabled_HEALTH_REGEN")) {
                    details.consumption += 5;
                }
            }

            if (!nbt.getBoolean("Disabled_TEMPERATURE_CONTROL") &&
                    isInExtremeTemperature(player)) {
                details.consumption += 8;
            }

            if (!nbt.getBoolean("Disabled_WATERPROOF_MODULE") &&
                    (player.isInWater() || player.isWet())) {
                details.consumption += 3;
            }

            if (!nbt.getBoolean("Disabled_ENERGY_EFFICIENCY")) {
                int efficiencyLevel = getUpgradeLevel(coreStack, "ENERGY_EFFICIENCY");
                if (efficiencyLevel > 0) {
                    int efficiencyPercent = efficiencyLevel * 15;
                    details.consumption = (int)(details.consumption * (1.0 - efficiencyPercent / 100.0));
                }
            }

        } catch (Exception e) {
        }

        return details;
    }

    private static class EnergyFlowDetails {
        int generation = 0;
        int consumption = 0;
    }

    private int getUpgradeLevel(ItemStack stack, String upgradeId) {
        try {
            int level = ItemMechanicalCoreExtended.getUpgradeLevel(stack, upgradeId);
            if (level > 0) return level;

            try {
                ItemMechanicalCore.UpgradeType type = ItemMechanicalCore.UpgradeType.valueOf(upgradeId);
                return ItemMechanicalCore.getUpgradeLevel(stack, type);
            } catch (Exception e) {
            }

            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int getTotalActiveUpgradeLevel(ItemStack stack) {
        try {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt == null) return 0;

            int total = 0;

            for (UpgradeType type : UpgradeType.values()) {
                String key = type.name();
                if (!nbt.getBoolean("Disabled_" + key)) {
                    total += getUpgradeLevel(stack, key);
                }
            }

            return total;
        } catch (Exception e) {
            return 0;
        }
    }

    private int countDisabledUpgrades(NBTTagCompound nbt) {
        if (nbt == null) return 0;

        int count = 0;
        for (String key : nbt.getKeySet()) {
            if (key.startsWith("Disabled_") && nbt.getBoolean(key)) {
                count++;
            }
        }
        return count;
    }

    private boolean isInExtremeTemperature(EntityPlayer player) {
        try {
            float temp = player.world.getBiome(player.getPosition()).getTemperature(player.getPosition());
            return temp > 1.5f || temp < 0.2f || player.isInLava() ||
                    player.world.provider.isNether() || player.world.provider.getDimension() == -1;
        } catch (Exception e) {
            return false;
        }
    }

    private String getStatusIcon(EnergyDepletionManager.EnergyStatus status) {
        switch (status) {
            case NORMAL:
                return TextFormatting.GREEN + "✓";
            case POWER_SAVING:
                return TextFormatting.YELLOW + "⚡";
            case EMERGENCY:
                return TextFormatting.RED + "⚠";
            case CRITICAL:
                return TextFormatting.DARK_RED + "💀";
            default:
                return TextFormatting.AQUA + "⚙";
        }
    }

    private int calculateHudX(ScaledResolution resolution) {
        switch (MechanicalCoreHUDConfig.position) {
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                return resolution.getScaledWidth() - 130 - MechanicalCoreHUDConfig.xOffset;
            case CUSTOM:
                return MechanicalCoreHUDConfig.xOffset;
            case TOP_LEFT:
            case BOTTOM_LEFT:
            default:
                return MechanicalCoreHUDConfig.xOffset;
        }
    }

    private int calculateHudY(ScaledResolution resolution) {
        switch (MechanicalCoreHUDConfig.position) {
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                return resolution.getScaledHeight() - 150 - MechanicalCoreHUDConfig.yOffset;
            case CUSTOM:
                return MechanicalCoreHUDConfig.yOffset;
            case TOP_LEFT:
            case TOP_RIGHT:
            default:
                return MechanicalCoreHUDConfig.yOffset;
        }
    }

    private int renderEfficiencyInfo(int x, int y, ItemStack coreStack, FontRenderer font) {
        try {
            NBTTagCompound nbt = coreStack.getTagCompound();
            if (nbt != null && !nbt.getBoolean("Disabled_ENERGY_EFFICIENCY")) {
                int efficiencyLevel = getUpgradeLevel(coreStack, "ENERGY_EFFICIENCY");
                if (efficiencyLevel > 0) {
                    int efficiencyPercent = efficiencyLevel * 15;
                    TextFormatting color = TextFormatting.GREEN;
                    if (efficiencyPercent >= 60) color = TextFormatting.GOLD;
                    if (efficiencyPercent >= 75) color = TextFormatting.LIGHT_PURPLE;

                    font.drawStringWithShadow(color + "⚡ 效率: -" + efficiencyPercent + "%", x, y, 0xFFFFFF);
                    return y + 12;
                }
            }
        } catch (Exception e) {
        }
        return y;
    }

    private void renderWarning(int x, int y, String warning, FontRenderer font) {
        if (MechanicalCoreHUDConfig.shouldUseAnimations()) {
            float alpha = (float) Math.sin(animationTick * 0.2f) * 0.5f + 0.5f;
            GlStateManager.color(1.0f, 0.0f, 0.0f, alpha);
        }

        drawRect(x, y, x + 120, y + 15, 0x80FF0000);
        font.drawStringWithShadow(warning, x + 5, y + 3, 0xFFFF00);
    }

    private void updateAnimations() {
        animationTick++;

        if (expanding) {
            pulseAnimation += 0.05f;
            if (pulseAnimation >= 1.0f) {
                pulseAnimation = 1.0f;
                expanding = false;
            }
        } else {
            pulseAnimation -= 0.05f;
            if (pulseAnimation <= 0.0f) {
                pulseAnimation = 0.0f;
                expanding = true;
            }
        }
    }

    private void renderBackground(int x, int y, int width, float energyPercent) {
        float alpha = (float)MechanicalCoreHUDConfig.backgroundAlpha;
        float red, green, blue;

        if (energyPercent > 0.6f) {
            red = 0.1f; green = 0.2f; blue = 0.3f;
        } else if (energyPercent > 0.3f) {
            red = 0.3f; green = 0.25f; blue = 0.1f;
        } else {
            red = 0.3f;
            green = 0.1f;
            blue = 0.1f;

            if (MechanicalCoreHUDConfig.enablePulseEffect) {
                red += pulseAnimation * 0.1f;
            }
        }

        int height = 100;
        if (MechanicalCoreHUDConfig.showActiveUpgrades) {
            height += 50;
        }

        GlStateManager.color(red, green, blue, alpha);
        drawRect(x, y, x + width, y + height, 0x80000000);

        drawBorder(x, y, width, height, getEnergyColor(energyPercent));
        drawCornerDecorations(x, y, width, height, energyPercent);
    }

    private void renderEnergyBar(int x, int y, int width, int height, float percent) {
        drawRect(x, y, x + width, y + height, 0xFF000000);
        drawRect(x - 1, y - 1, x + width + 1, y, 0xFF444444);
        drawRect(x - 1, y + height, x + width + 1, y + height + 1, 0xFF444444);
        drawRect(x - 1, y, x, y + height, 0xFF444444);
        drawRect(x + width, y, x + width + 1, y + height, 0xFF444444);

        if (percent > 0) {
            int barWidth = (int) (width * percent);
            int color1 = getEnergyColor(percent);
            int color2 = getDarkerColor(color1);
            drawGradientRect(x, y, x + barWidth, y + height, color1, color2);

            if (MechanicalCoreHUDConfig.enableEnergyBarShimmer && MechanicalCoreHUDConfig.shouldUseAnimations() && percent > 0.1f) {
                float shimmer = (float) Math.sin(animationTick * 0.1f) * 0.3f + 0.7f;
                GlStateManager.color(1.0f, 1.0f, 1.0f, shimmer * 0.3f);
                drawRect(x, y, x + barWidth, y + 1, 0x80FFFFFF);
            }
        }
    }

    private int getEnergyColor(float percent) {
        return MechanicalCoreHUDConfig.getEnergyColor(percent);
    }

    private int getDarkerColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return 0xFF000000 | ((r * 3 / 4) << 16) | ((g * 3 / 4) << 8) | (b * 3 / 4);
    }

    private TextFormatting getPercentColor(float percent) {
        if (percent > 0.6f) return TextFormatting.GREEN;
        if (percent > 0.3f) return TextFormatting.YELLOW;
        if (percent > 0.1f) return TextFormatting.GOLD;
        return TextFormatting.RED;
    }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.1fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fk", energy / 1000.0);
        }
        return String.valueOf(energy);
    }

    private void drawBorder(int x, int y, int width, int height, int color) {
        drawRect(x, y, x + width, y + 1, color);
        drawRect(x, y + height - 1, x + width, y + height, color);
        drawRect(x, y, x + 1, y + height, color);
        drawRect(x + width - 1, y, x + width, y + height, color);
    }

    private void drawCornerDecorations(int x, int y, int width, int height, float energyPercent) {
        int color = getEnergyColor(energyPercent);
        int cornerSize = 5;

        drawRect(x, y, x + cornerSize, y + 1, color);
        drawRect(x, y, x + 1, y + cornerSize, color);
        drawRect(x + width - cornerSize, y, x + width, y + 1, color);
        drawRect(x + width - 1, y, x + width, y + cornerSize, color);
        drawRect(x, y + height - 1, x + cornerSize, y + height, color);
        drawRect(x, y + height - cornerSize, x + 1, y + height, color);
        drawRect(x + width - cornerSize, y + height - 1, x + width, y + height, color);
        drawRect(x + width - 1, y + height - cornerSize, x + width, y + height, color);
    }

    // ========== 人性值渲染方法 ==========

    /**
     * 渲染人性值信息
     */
    private int renderHumanityInfo(int hudX, int currentY, int hudWidth, IHumanityData data, FontRenderer fontRenderer) {
        float humanity = data.getHumanity();
        AscensionRoute route = data.getAscensionRoute();

        // 人性值进度条
        int barY = currentY;
        int barWidth = hudWidth - 10;
        int barHeight = 4;

        // 背景
        drawRect(hudX + 5, barY, hudX + 5 + barWidth, barY + barHeight, 0xFF000000);

        // 获取颜色
        int barColor = getHumanityBarColor(humanity, route);
        TextFormatting textColor = getHumanityTextColor(humanity, route);

        // 低人性脉冲效果
        if (humanity < 25f && animationTick % 20 < 10) {
            barColor = 0xFF8800AA;
        }

        // 绘制进度条
        int fillWidth = (int)(humanity / 100f * barWidth);
        drawRect(hudX + 5, barY, hudX + 5 + fillWidth, barY + barHeight, barColor);

        currentY = barY + 6;

        // 第一行：人性值 + 状态
        String humanityText = "⚛ 人性: " + String.format("%.0f%%", humanity);
        String statusLabel = getHumanityStatusLabel(humanity, route);

        fontRenderer.drawStringWithShadow(textColor + humanityText + " " + statusLabel,
                hudX + 5, currentY, 0xFFFFFF);
        currentY += 10;

        // 升格路线显示
        if (route != AscensionRoute.NONE) {
            String routeText = route == AscensionRoute.MEKHANE_SYNTHETIC ?
                    TextFormatting.LIGHT_PURPLE + "  [Mekhane合成人]" :
                    TextFormatting.DARK_PURPLE + "  [破碎之神]";
            fontRenderer.drawStringWithShadow(routeText, hudX + 5, currentY, 0xFFFFFF);
            currentY += 10;
        }

        // 崩解状态警告
        if (data.isDissolutionActive()) {
            int seconds = data.getDissolutionTicks() / 20;
            String warningText = TextFormatting.DARK_RED + "💀 崩解中! " + TextFormatting.RED + seconds + "s";
            if (animationTick % 10 < 5) {
                fontRenderer.drawStringWithShadow(warningText, hudX + 5, currentY, 0xFFFFFF);
            }
            currentY += 10;
        }

        // 存在锚定标记
        if (data.isExistenceAnchored(mc.world.getTotalWorldTime())) {
            String anchorText = TextFormatting.AQUA + "  [存在锚定]";
            fontRenderer.drawStringWithShadow(anchorText, hudX + 5, currentY, 0xFFFFFF);
            currentY += 10;
        }

        // 分析进度
        net.minecraft.util.ResourceLocation analyzing = data.getAnalyzingEntity();
        if (analyzing != null) {
            int progress = data.getAnalysisProgress();
            String analysisText = TextFormatting.GREEN + "  分析: " + TextFormatting.WHITE +
                    analyzing.getPath() + " " + TextFormatting.YELLOW + progress + "%";
            fontRenderer.drawStringWithShadow(analysisText, hudX + 5, currentY, 0xFFFFFF);
            currentY += 10;
        }

        return currentY;
    }

    /**
     * 获取人性值进度条颜色
     */
    private int getHumanityBarColor(float humanity, AscensionRoute route) {
        if (route == AscensionRoute.MEKHANE_SYNTHETIC) {
            return 0xFFDD88FF;  // 浅紫
        }
        if (route == AscensionRoute.BROKEN_GOD) {
            return 0xFF8800AA;  // 暗紫
        }

        if (humanity >= 80f) return 0xFFAADDFF;  // 蓝白
        if (humanity >= 60f) return 0xFFBBDDEE;  // 浅蓝
        if (humanity >= 40f) return 0xFFEEBBFF;  // 浅紫
        if (humanity >= 25f) return 0xFFDD88FF;  // 紫
        if (humanity >= 10f) return 0xFFAA44DD;  // 深紫
        return 0xFF8800AA;  // 暗紫
    }

    /**
     * 获取人性值文字颜色
     */
    private TextFormatting getHumanityTextColor(float humanity, AscensionRoute route) {
        if (route == AscensionRoute.MEKHANE_SYNTHETIC) return TextFormatting.LIGHT_PURPLE;
        if (route == AscensionRoute.BROKEN_GOD) return TextFormatting.DARK_PURPLE;

        if (humanity >= 80f) return TextFormatting.AQUA;
        if (humanity >= 60f) return TextFormatting.WHITE;
        if (humanity >= 40f) return TextFormatting.LIGHT_PURPLE;
        if (humanity >= 25f) return TextFormatting.DARK_PURPLE;
        return TextFormatting.DARK_RED;
    }

    /**
     * 获取人性值状态标签
     */
    private String getHumanityStatusLabel(float humanity, AscensionRoute route) {
        if (route == AscensionRoute.MEKHANE_SYNTHETIC) {
            return TextFormatting.LIGHT_PURPLE + "[协同完美]";
        }
        if (route == AscensionRoute.BROKEN_GOD) {
            return TextFormatting.DARK_PURPLE + "[超越人性]";
        }

        if (humanity >= 80f) return TextFormatting.AQUA + "[猎人协议]";
        if (humanity >= 60f) return TextFormatting.WHITE + "[稳定]";
        if (humanity >= 40f) return TextFormatting.LIGHT_PURPLE + "[灰域]";
        if (humanity >= 25f) return TextFormatting.DARK_PURPLE + "[异常协议]";
        if (humanity >= 10f) return TextFormatting.RED + "[深度异化]";
        return TextFormatting.DARK_RED + "[临界崩解]";
    }

    // ========== 以下是新增的辅助方法：客户端从核心NBT读取排异数据 ==========

    /**
     * 客户端安全地从核心读取排异值
     */
    private float getClientRejectionLevel(ItemStack coreStack) {
        if (coreStack.isEmpty()) return 0f;
        NBTTagCompound rejectionData = coreStack.getOrCreateSubCompound("rejection");
        return rejectionData.getFloat("RejectionLevel");
    }

    /**
     * 客户端安全地从核心读取适应度
     */
    private float getClientAdaptationLevel(ItemStack coreStack) {
        if (coreStack.isEmpty()) return 0f;
        NBTTagCompound rejectionData = coreStack.getOrCreateSubCompound("rejection");
        return rejectionData.getFloat("AdaptationLevel");
    }

    /**
     * 客户端安全地从核心读取突破状态
     */
    private boolean getClientTranscendedStatus(ItemStack coreStack) {
        if (coreStack.isEmpty()) return false;
        NBTTagCompound rejectionData = coreStack.getOrCreateSubCompound("rejection");
        return rejectionData.getBoolean("RejectionTranscended");
    }

    /**
     * 客户端获取排异状态摘要（用于详细信息显示）
     */
    private RejectionDisplayInfo getClientRejectionInfo(ItemStack coreStack) {
        if (coreStack.isEmpty()) return null;

        RejectionDisplayInfo info = new RejectionDisplayInfo();
        info.rejection = getClientRejectionLevel(coreStack);
        info.adaptation = getClientAdaptationLevel(coreStack);
        info.transcended = getClientTranscendedStatus(coreStack);

        // 从核心计算模组数量
        info.installed = FleshRejectionSystem.getTotalInstalledModules(coreStack);
        info.running = FleshRejectionSystem.getRunningModuleCount(coreStack);
        info.hasSynchronizer = FleshRejectionSystem.hasNeuralSynchronizer(coreStack);

        // 计算增长速率
        try {
            info.growthRate = (float) (info.running * FleshRejectionConfig.rejectionGrowthRate);
        } catch (Exception e) {
            info.growthRate = 0f;
        }

        return info;
    }

    /**
     * 排异显示信息数据类
     */
    private static class RejectionDisplayInfo {
        float rejection;
        float adaptation;
        boolean transcended;
        int installed;
        int running;
        boolean hasSynchronizer;
        float growthRate;
    }
}