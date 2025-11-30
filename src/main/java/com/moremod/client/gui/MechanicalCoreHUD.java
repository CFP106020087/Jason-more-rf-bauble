package com.moremod.client.gui;

import com.moremod.client.KeyBindHandler;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.UpgradeType;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import com.moremod.system.humanity.AscensionRoute;
import com.moremod.config.MechanicalCoreHUDConfig;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import com.moremod.upgrades.WetnessSystem;
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
 * 机械核心HUD - Smart Holographic Edition (Final)
 * * 融合特性：
 * 1. 视觉：全息科技风格 (Holographic Tech)
 * 2. 布局：智能高度计算 + 锚点自适应 (Smart Anchor)
 * 3. 逻辑：万能 NBT 读取
 */
@SideOnly(Side.CLIENT)
public class MechanicalCoreHUD extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // 视觉常量
    private static final int MAIN_PANEL_WIDTH = 150; // 宽面板适配中文
    private static final int PANEL_PADDING = 5;
    private static final int BAR_HEIGHT = 4;
    private static final int MODULE_PANEL_OFFSET = 3;

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
                    while (energySamples.size() > SAMPLE_SIZE) energySamples.poll();
                    if (!energySamples.isEmpty()) {
                        int sum = 0;
                        for (int sample : energySamples) sum += sample;
                        currentNetFlow = sum * 20 / energySamples.size();
                    }
                    lastEnergy = currentEnergy;
                    lastUpdateTick = currentTick;
                }
            } catch (Exception e) { reset(); }
        }
        public static int getNetFlow() { return currentNetFlow; }
        public static void reset() { energySamples.clear(); lastEnergy = -1; currentNetFlow = 0; lastUpdateTick = 0; }
    }

    private static float animationTick = 0;

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) return;
        if (!KeyBindHandler.isHudVisible()) return;

        EntityPlayer player = mc.player;
        if (player == null || mc.world == null) return;

        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) {
            EnergyTracker.reset();
            return;
        }

        IEnergyStorage energy = ItemMechanicalCore.getEnergyStorage(coreStack);
        if (energy == null) return;

        EnergyTracker.update(energy.getEnergyStored());
        animationTick++;

        renderSmartDashboard(coreStack, energy, player);
    }

    /**
     * 智能渲染入口：先计算尺寸，再决定位置
     */
    private void renderSmartDashboard(ItemStack coreStack, IEnergyStorage energy, EntityPlayer player) {
        ScaledResolution resolution = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRenderer;

        // 1. 预计算高度 (Pre-calculate Height)
        // 只有先算出高度，才能在 BOTTOM 模式下正确向上推
        int mainHeight = calculateMainPanelHeight(coreStack, player);

        // 2. 准备模组列表
        List<String> moduleLines = new ArrayList<>();
        if (MechanicalCoreHUDConfig.showActiveUpgrades) {
            collectActiveUpgrades(moduleLines, coreStack, player);
        }

        int moduleWidth = 0;
        int moduleHeight = 0;
        if (!moduleLines.isEmpty()) {
            int maxW = 80;
            for (String line : moduleLines) {
                int w = fr.getStringWidth(line);
                if (w > maxW) maxW = w;
            }
            moduleWidth = maxW + (PANEL_PADDING * 2);

            int maxLines = MechanicalCoreHUDConfig.getCurrentMaxDisplayUpgrades();
            int displayCount = Math.min(moduleLines.size(), maxLines + (moduleLines.size() > maxLines ? 1 : 0));
            moduleHeight = (displayCount * 10) + (PANEL_PADDING * 2);
        }

        // 3. 坐标计算 (Smart Positioning)
        double scale = MechanicalCoreHUDConfig.scale;
        int screenW = (int)(resolution.getScaledWidth() / scale);
        int screenH = (int)(resolution.getScaledHeight() / scale);

        int xOffset = MechanicalCoreHUDConfig.xOffset;
        int yOffset = MechanicalCoreHUDConfig.yOffset;

        int mainX = 0, mainY = 0;
        int moduleX = 0, moduleY = 0;
        boolean anchorRight = false;

        switch (MechanicalCoreHUDConfig.position) {
            case TOP_LEFT:
                mainX = xOffset + 5;
                mainY = yOffset + 5;
                break;
            case TOP_RIGHT:
                mainX = screenW - MAIN_PANEL_WIDTH - xOffset - 5;
                mainY = yOffset + 5;
                anchorRight = true;
                break;
            case BOTTOM_LEFT:
                mainX = xOffset + 5;
                // 智能高度：屏幕底 - 偏移 - 面板高 = 顶端Y
                mainY = screenH - yOffset - mainHeight - 5;
                break;
            case BOTTOM_RIGHT:
                mainX = screenW - MAIN_PANEL_WIDTH - xOffset - 5;
                mainY = screenH - yOffset - mainHeight - 5;
                anchorRight = true;
                break;
            case TOP_MIDDLE:
                // 上中：水平居中，贴顶
                mainX = (screenW - MAIN_PANEL_WIDTH) / 2 + xOffset;
                mainY = yOffset + 5;
                break;
            case LEFT_MIDDLE:
                // 左中：贴左，垂直居中
                mainX = xOffset + 5;
                mainY = (screenH - mainHeight) / 2 + yOffset;
                break;
            case RIGHT_MIDDLE:
                // 右中：贴右，垂直居中
                mainX = screenW - MAIN_PANEL_WIDTH - xOffset - 5;
                mainY = (screenH - mainHeight) / 2 + yOffset;
                anchorRight = true;
                break;
            case CUSTOM:
                mainX = xOffset;
                mainY = yOffset;
                break;
        }

        // 计算模组面板位置
        if (anchorRight) {
            // 右对齐模式：模组面板在主面板左侧
            moduleX = mainX - MODULE_PANEL_OFFSET - moduleWidth;
        } else {
            // 左对齐模式：模组面板在主面板右侧
            moduleX = mainX + MAIN_PANEL_WIDTH + MODULE_PANEL_OFFSET;
        }

        // 模组面板垂直对齐：如果底部对齐，则底部对齐；否则顶部对齐
        if (MechanicalCoreHUDConfig.position == MechanicalCoreHUDConfig.HUDPosition.BOTTOM_LEFT ||
                MechanicalCoreHUDConfig.position == MechanicalCoreHUDConfig.HUDPosition.BOTTOM_RIGHT) {
            moduleY = mainY + mainHeight - moduleHeight; // 底对齐
        } else {
            moduleY = mainY; // 顶对齐
        }

        // 4. 开始渲染
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0);

        renderMainPanelContent(mainX, mainY, MAIN_PANEL_WIDTH, mainHeight, coreStack, energy, player, fr);

        if (!moduleLines.isEmpty()) {
            renderModulePanelContent(moduleX, moduleY, moduleWidth, moduleHeight, moduleLines, fr);
        }

        GlStateManager.popMatrix();
    }

    /**
     * 纯计算：主面板高度
     */
    private int calculateMainPanelHeight(ItemStack coreStack, EntityPlayer player) {
        // 基础: Padding + Title(11) + EnergyBar(6) + EnergyVal(8) + Padding
        int height = 35;

        float rejection = getClientRejectionLevel(coreStack);
        boolean transcended = getClientTranscendedStatus(coreStack);
        int wetness = WetnessSystem.getWetness(player);

        boolean shouldShowHumanitySystem = transcended && rejection <= 0;
        IHumanityData humanityData = HumanityCapabilityHandler.getData(player);
        boolean showHumanity = shouldShowHumanitySystem && humanityData != null;

        boolean showRejection = rejection > 0 || (!transcended && rejection > 0);

        height += 16; // 身份状态栏

        // 每个模块: Gap(2/4) + Title(10) + Bar(8) = ~20-22
        if (showRejection) height += 22;
        if (wetness > 0) height += 22;
        if (showHumanity) height += 22;
        if (MechanicalCoreHUDConfig.showEnergyFlow) height += 14;
        if (MechanicalCoreHUDConfig.showEfficiency) height += 14;

        height += 5; // 底部留白
        return height;
    }

    private void renderMainPanelContent(int x, int y, int width, int height, ItemStack coreStack, IEnergyStorage energy, EntityPlayer player, FontRenderer fr) {
        EnergyDepletionManager.EnergyStatus energyStatus = getLocalEnergyStatus(energy);

        // 绘制全息背景
        drawHoloPanel(x, y, width, height, energyStatus);

        int currentY = y + PANEL_PADDING;
        int contentWidth = width - (PANEL_PADDING * 2);
        int contentX = x + PANEL_PADDING;

        // [1] Title
        String title = TextFormatting.AQUA + "" + TextFormatting.BOLD + "CORE SYSTEM";
        if (energyStatus != EnergyDepletionManager.EnergyStatus.NORMAL) {
            title = energyStatus.color + "⚠ " + energyStatus.displayName;
        }
        fr.drawStringWithShadow(title, contentX, currentY, 0xFFFFFF);

        float energyPercent = (float)energy.getEnergyStored() / energy.getMaxEnergyStored();
        String percentText = (int)(energyPercent * 100) + "%";
        drawRightAlignedString(percentText, contentX + contentWidth, currentY, getEnergyColor(energyPercent), fr);

        currentY += 11;

        // [2] Energy Bar
        drawGlossyBar(contentX, currentY, contentWidth, BAR_HEIGHT, energyPercent, getEnergyColor(energyPercent));
        currentY += 6;

        String energyVal = formatEnergy(energy.getEnergyStored()) + " RF";
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.8, 0.8, 1);
        fr.drawString(TextFormatting.GRAY + energyVal, (int)(contentX / 0.8), (int)(currentY / 0.8), 0xAAAAAA);
        GlStateManager.popMatrix();

        currentY += 8;

        // [3] Status
        currentY += 4;
        boolean transcended = getClientTranscendedStatus(coreStack);
        IHumanityData humanityData = HumanityCapabilityHandler.getData(player);

        String statusText = "👤 凡人";
        TextFormatting statusColor = TextFormatting.GRAY;
        if (transcended) {
            statusText = "⚙ 机械飞升";
            statusColor = TextFormatting.LIGHT_PURPLE;
            if (humanityData != null && humanityData.getAscensionRoute() == AscensionRoute.BROKEN_GOD) {
                statusText = "۞ 破碎之神";
                statusColor = TextFormatting.DARK_PURPLE;
            } else if (humanityData != null && humanityData.getAscensionRoute() == AscensionRoute.SHAMBHALA) {
                statusText = "☀ 机巧香巴拉";
                statusColor = TextFormatting.AQUA;
            }
        }
        drawSeparator(contentX, currentY - 2, contentWidth);
        fr.drawStringWithShadow("状态: " + statusColor + statusText, contentX, currentY, 0xDDDDDD);
        currentY += 12;

        // [4] Wetness
        int wetness = WetnessSystem.getWetness(player);
        if (wetness > 0) {
            currentY += 2;
            String wetIcon = (wetness >= 80 ? TextFormatting.RED : (wetness >= 30 ? TextFormatting.YELLOW : TextFormatting.AQUA)) + "💧";
            fr.drawStringWithShadow(wetIcon + " 潮湿积聚", contentX, currentY, 0xFFFFFF);
            drawRightAlignedString(wetness + "%", contentX + contentWidth, currentY, 0xFFFFFF, fr);

            currentY += 10;
            int barColor = wetness >= 80 ? 0xFFFF5555 : 0xFF55FFFF;
            drawGlossyBar(contentX, currentY, contentWidth, BAR_HEIGHT, wetness / 100f, barColor);
            currentY += 10;
        }

        // [5] Rejection
        float rejection = getClientRejectionLevel(coreStack);
        boolean showRejection = rejection > 0 || (!transcended && rejection > 0);

        if (showRejection) {
            currentY += 2;
            String rejIcon = (rejection >= 60 ? TextFormatting.RED : TextFormatting.GOLD) + "⚠";
            fr.drawStringWithShadow(rejIcon + " 排异反应", contentX, currentY, 0xFFFFFF);
            drawRightAlignedString(String.format("%.1f%%", rejection), contentX + contentWidth, currentY, 0xFFFFFF, fr);

            currentY += 10;
            int rejColor = rejection >= 60 ? 0xFFFF0000 : 0xFFFFAA00;
            drawGlossyBar(contentX, currentY, contentWidth, BAR_HEIGHT, rejection / 100f, rejColor);
            currentY += 10;
        }

        // [6] Humanity
        boolean shouldShowHumanitySystem = transcended && rejection <= 0;
        boolean showHumanity = shouldShowHumanitySystem && humanityData != null;

        if (showHumanity) {
            currentY += 2;
            float hVal = humanityData.getHumanity();
            AscensionRoute route = humanityData.getAscensionRoute();
            int hColor = getHumanityBarColor(hVal, route);
            String hTitle;
            TextFormatting titleColor;
            if (route == AscensionRoute.BROKEN_GOD) {
                hTitle = "⚛ 神性";
                titleColor = TextFormatting.DARK_PURPLE;
            } else if (route == AscensionRoute.SHAMBHALA) {
                hTitle = "☀ 圆满";
                titleColor = TextFormatting.AQUA;
            } else {
                hTitle = "⚛ 人性";
                titleColor = TextFormatting.LIGHT_PURPLE;
            }

            fr.drawStringWithShadow(titleColor + hTitle, contentX, currentY, 0xFFFFFF);
            drawRightAlignedString((int)hVal + "%", contentX + contentWidth, currentY, 0xFFFFFF, fr);

            currentY += 10;
            drawGlossyBar(contentX, currentY, contentWidth, BAR_HEIGHT, hVal / 100f, hColor);
            currentY += 10;
        }

        // [7] Energy Flow
        if (MechanicalCoreHUDConfig.showEnergyFlow) {
            currentY += 4;
            int netFlow = EnergyTracker.getNetFlow();
            String arrow = netFlow >= 0 ? TextFormatting.GREEN + "▲" : TextFormatting.RED + "▼";
            String flowText = arrow + " " + Math.abs(netFlow) + " RF/t";
            fr.drawStringWithShadow(flowText, contentX, currentY, 0xFFFFFF);
            currentY += 12;
        }

        // [8] Efficiency
        if (MechanicalCoreHUDConfig.showEfficiency) {
            int effLvl = getTargetedUpgradeLevel(coreStack, UpgradeType.ENERGY_EFFICIENCY);
            if (effLvl > 0) {
                int effPct = effLvl * 15;
                TextFormatting color = effPct >= 60 ? TextFormatting.GOLD : TextFormatting.GREEN;
                fr.drawStringWithShadow(color + "⚡ 效率: -" + effPct + "%", contentX, currentY, 0xFFFFFF);
            }
        }
    }

    private void renderModulePanelContent(int x, int y, int width, int height, List<String> lines, FontRenderer fr) {
        // 滚动逻辑
        int maxLines = MechanicalCoreHUDConfig.getCurrentMaxDisplayUpgrades();
        int scrollOffset = KeyBindHandler.getScrollOffset();
        List<String> displayList = lines;

        if (lines.size() > maxLines) {
            if (scrollOffset >= lines.size()) {
                scrollOffset = 0;
                KeyBindHandler.resetScrollOffset();
            }
            int endIndex = Math.min(scrollOffset + maxLines, lines.size());
            displayList = new ArrayList<>(lines.subList(scrollOffset, endIndex));
            displayList.add(TextFormatting.DARK_GRAY + "...");
        }

        drawHoloPanel(x, y, width, height, EnergyDepletionManager.EnergyStatus.NORMAL);

        int currentY = y + PANEL_PADDING;
        int contentX = x + PANEL_PADDING;

        for (String line : displayList) {
            fr.drawStringWithShadow(line, contentX, currentY, 0xFFFFFF);
            currentY += 10;
        }
    }

    // ==================== 视觉核心 ====================

    private void drawHoloPanel(int x, int y, int width, int height, EnergyDepletionManager.EnergyStatus status) {
        // 背景渐变
        int colorTop = 0xCC051020;
        int colorBottom = 0xE6000000;
        drawGradientRect(x, y, x + width, y + height, colorTop, colorBottom);

        int borderColor = 0xFF00FFFF;
        if (status == EnergyDepletionManager.EnergyStatus.CRITICAL) borderColor = 0xFFFF0000;
        else if (status == EnergyDepletionManager.EnergyStatus.EMERGENCY) borderColor = 0xFFFF5500;
        else if (status == EnergyDepletionManager.EnergyStatus.POWER_SAVING) borderColor = 0xFFFFAA00;

        // 边框
        drawRect(x, y, x + 2, y + height, borderColor);
        drawRect(x, y, x + 1, y + height, 0x80FFFFFF);

        drawRect(x + 2, y, x + width, y + 1, borderColor);
        drawRect(x + 2, y + height - 1, x + width, y + height, borderColor);
        drawRect(x + width - 1, y, x + width, y + height - 5, borderColor);

        drawRect(x + width - 4, y, x + width, y + 4, borderColor);
        drawRect(x, y + height - 4, x + 4, y + height, borderColor);
    }

    private void drawGlossyBar(int x, int y, int width, int height, float percent, int color) {
        drawRect(x, y, x + width, y + height, 0xFF111111);
        if (percent > 0) {
            int fillW = (int)(width * percent);
            drawRect(x, y, x + fillW, y + height, color);
            drawRect(x, y, x + fillW, y + height / 2, 0x50FFFFFF);
            drawRect(x, y + height - 1, x + fillW, y + height, 0x40000000);
        }
    }

    private void drawSeparator(int x, int y, int width) {
        drawRect(x, y, x + width, y + 1, 0x4000AAAA);
    }

    private void drawRightAlignedString(String text, int x, int y, int color, FontRenderer fr) {
        fr.drawStringWithShadow(text, x - fr.getStringWidth(text), y, color);
    }

    // ==================== 逻辑方法 ====================

    private void collectActiveUpgrades(List<String> list, ItemStack coreStack, EntityPlayer player) {
        if (!coreStack.hasTagCompound()) return;
        NBTTagCompound nbt = coreStack.getTagCompound();

        if (nbt.getBoolean("HasUpgrade_FLIGHT_MODULE")) {
            String txt = TextFormatting.LIGHT_PURPLE + "✈ 飞行";
            if (nbt.getBoolean("FlightHoverMode")) txt += " [悬停]";
            if (nbt.getBoolean("FlightModuleEnabled")) list.add(txt);
            else list.add(TextFormatting.GRAY + "✈ 飞行 (关)");
        }

        for (UpgradeType type : UpgradeType.values()) {
            if (type == UpgradeType.FLIGHT_MODULE) continue;
            int lvl = getTargetedUpgradeLevel(coreStack, type);
            String id = type.name();
            if (lvl > 0 && !nbt.getBoolean("Disabled_" + id)) {
                String info = getSimpleUpgradeText(type, lvl, player, nbt);
                if (info != null) list.add(info);
            }
        }
    }

    private int getTargetedUpgradeLevel(ItemStack stack, UpgradeType type) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return 0;
        NBTTagCompound nbt = stack.getTagCompound();
        String rawName = type.name();
        String lowerKey = "upgrade_" + rawName.toLowerCase();
        if (nbt.hasKey(lowerKey)) return nbt.getInteger(lowerKey);
        String hasKey = "HasUpgrade_" + rawName;
        if (nbt.hasKey(hasKey)) return nbt.getBoolean(hasKey) ? 1 : 0;
        String upperKey = "upgrade_" + rawName;
        if (nbt.hasKey(upperKey)) return nbt.getInteger(upperKey);
        if (nbt.hasKey(rawName.toLowerCase())) return nbt.getInteger(rawName.toLowerCase());
        return 0;
    }

    private String getSimpleUpgradeText(UpgradeType type, int level, EntityPlayer player, NBTTagCompound nbt) {
        TextFormatting c = type.getColor();
        switch (type) {
            case SPEED_BOOST: if(player.isSprinting()) return c + "⚡ 加速"; break;
            case MOVEMENT_SPEED: return c + "⚡ 移速 " + level;
            case SHIELD_GENERATOR:
            case YELLOW_SHIELD:
                if(player.getAbsorptionAmount()>0) return c + "🛡 护盾 " + (int)player.getAbsorptionAmount();
                break;
            case FIRE_EXTINGUISH: if(player.isBurning()) return c + "🔥 灭火中"; break;
            case WATERPROOF_MODULE: if(player.isInWater()) return c + "💧 防水"; break;
            case ORE_VISION: return c + "⛏ 矿视";
            case STEALTH:
                if(player.isInvisible()) return c + "👻 隐形";
                break;
            case HUNGER_THIRST:
                if(player.getFoodStats().getFoodLevel()<20) return c + "🍖 补给";
                break;
            case KINETIC_GENERATOR:
                if(Math.abs(player.motionX) > 0.01 || Math.abs(player.motionZ) > 0.01) return c + "⚙ 动能";
                break;
            case SOLAR_GENERATOR:
                if(mc.world.isDaytime() && mc.world.canSeeSky(player.getPosition())) return c + "☀ 太阳能";
                break;
            case VOID_ENERGY:
                if (player.posY < 30 || player.dimension == 1) return c + "⚫ 虚空能";
                break;
            case REGENERATION:
            case HEALTH_REGEN:
                if(player.getHealth() < player.getMaxHealth()) return c + "❤ 再生";
                break;
            case DAMAGE_BOOST: return c + "⚔ 伤害+" + (level*25) + "%";
            case ATTACK_SPEED: return c + "⚔ 攻速+" + (level*20) + "%";
            case EXP_AMPLIFIER: return c + "✨ 经验+" + (level*50) + "%";
            case TEMPERATURE_CONTROL: return c + "🌡 温控";
            case THORNS: return c + "🌵 荆棘 " + level;
            default: return null;
        }
        return null;
    }

    private EnergyDepletionManager.EnergyStatus getLocalEnergyStatus(IEnergyStorage energy) {
        if (energy.getMaxEnergyStored() <= 0) return EnergyDepletionManager.EnergyStatus.NORMAL;
        float percent = (float) energy.getEnergyStored() / energy.getMaxEnergyStored();
        if (percent <= 0.05f) return EnergyDepletionManager.EnergyStatus.CRITICAL;
        if (percent <= 0.15f) return EnergyDepletionManager.EnergyStatus.EMERGENCY;
        if (percent <= 0.30f) return EnergyDepletionManager.EnergyStatus.POWER_SAVING;
        return EnergyDepletionManager.EnergyStatus.NORMAL;
    }

    private int calculateHudX(ScaledResolution resolution) { return MechanicalCoreHUDConfig.xOffset + 5; }
    private int calculateHudY(ScaledResolution resolution) { return MechanicalCoreHUDConfig.yOffset + 5; }
    private int getEnergyColor(float percent) { return MechanicalCoreHUDConfig.getEnergyColor(percent); }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) return String.format("%.1fM", energy / 1000000.0);
        if (energy >= 1000) return String.format("%.1fk", energy / 1000.0);
        return String.valueOf(energy);
    }

    private float getClientRejectionLevel(ItemStack stack) {
        if(stack.isEmpty()) return 0;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt.hasKey("rejection")) return nbt.getCompoundTag("rejection").getFloat("RejectionLevel");
        if (nbt.hasKey("RejectionLevel")) return nbt.getFloat("RejectionLevel");
        if (nbt.hasKey("Rejection")) return nbt.getFloat("Rejection");
        return 0;
    }

    private boolean getClientTranscendedStatus(ItemStack stack) {
        if(stack.isEmpty()) return false;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt.hasKey("rejection")) return nbt.getCompoundTag("rejection").getBoolean("RejectionTranscended");
        if (nbt.hasKey("RejectionTranscended")) return nbt.getBoolean("RejectionTranscended");
        return false;
    }

    private int getHumanityBarColor(float h, AscensionRoute r) {
        if (r == AscensionRoute.BROKEN_GOD) return 0xFF5500AA; // 破碎之神暗紫
        if (r == AscensionRoute.SHAMBHALA) return 0xFF00DDFF;  // 机巧香巴拉青金
        if (h < 25) return 0xFFAA0000;
        if (h < 50) return 0xFFAA00AA;
        return 0xFF00AAFF;
    }
}