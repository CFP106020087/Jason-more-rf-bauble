package com.moremod.client.gui;

import com.moremod.client.KeyBindHandler;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.UpgradeType;
import com.moremod.upgrades.WaterproofUpgrade;
import com.moremod.upgrades.WetnessSystem;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import com.moremod.system.humanity.AscensionRoute;
import com.moremod.config.MechanicalCoreHUDConfig;
import com.moremod.config.FleshRejectionConfig;
import com.moremod.upgrades.energy.EnergyDepletionManager;
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

import static com.moremod.config.EnergyBalanceConfig.ExtendedUpgrades.NIGHT_VISION;

/**
 * 机械核心HUD - Cyber Dashboard Edition (Compilation Fixed)
 * * 修复：移除了不存在的 UpgradeType 枚举引用 (AUTO_FEEDER, OXYGEN_SUPPLY 等)
 * * 修复：清理了未使用的 import
 */
@SideOnly(Side.CLIENT)
public class MechanicalCoreHUD extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // 布局常量
    private static final int PANEL_PADDING = 4;
    private static final int BAR_HEIGHT = 3;
    private static final int MODULE_PANEL_OFFSET = 2;

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
                        for (int sample : energySamples) sum += sample;
                        currentNetFlow = sum * 20 / energySamples.size();
                    }
                    lastEnergy = currentEnergy;
                    lastUpdateTick = currentTick;
                }
            } catch (Exception e) {
                reset();
            }
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

        renderDashboard(coreStack, energy, player);
    }

    private void renderDashboard(ItemStack coreStack, IEnergyStorage energy, EntityPlayer player) {
        ScaledResolution resolution = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRenderer;

        int startX = calculateHudX(resolution);
        int startY = calculateHudY(resolution);

        GlStateManager.pushMatrix();
        GlStateManager.scale(MechanicalCoreHUDConfig.scale, MechanicalCoreHUDConfig.scale, 1.0);
        startX = (int)(startX / MechanicalCoreHUDConfig.scale);
        startY = (int)(startY / MechanicalCoreHUDConfig.scale);

        int mainPanelWidth = 110;

        List<String> rightPanelLines = new ArrayList<>();
        if (MechanicalCoreHUDConfig.showActiveUpgrades) {
            collectActiveUpgrades(rightPanelLines, coreStack, player);
        }

        renderMainPanel(startX, startY, mainPanelWidth, coreStack, energy, player, fr);

        if (!rightPanelLines.isEmpty()) {
            int rightPanelX = startX + mainPanelWidth + MODULE_PANEL_OFFSET;
            renderModulePanel(rightPanelX, startY, rightPanelLines, fr);
        }

        GlStateManager.popMatrix();
    }

    private int renderMainPanel(int x, int y, int width, ItemStack coreStack, IEnergyStorage energy, EntityPlayer player, FontRenderer fr) {
        int height = 28;

        EnergyDepletionManager.EnergyStatus energyStatus = getLocalEnergyStatus(energy);

        float rejection = getClientRejectionLevel(coreStack);
        boolean transcended = getClientTranscendedStatus(coreStack);
        boolean showRejection = !transcended && rejection > 0;

        int wetness = WetnessSystem.getWetness(player);
        boolean showWetness = wetness > 0;

        boolean shouldShowHumanitySystem = transcended && rejection <= 0;
        IHumanityData humanityData = HumanityCapabilityHandler.getData(player);
        boolean showHumanity = shouldShowHumanitySystem && humanityData != null;

        if (showRejection) height += 18;
        if (showWetness) height += 18;
        if (showHumanity) height += 18;
        if (MechanicalCoreHUDConfig.showEnergyFlow) height += 10;
        if (MechanicalCoreHUDConfig.showEfficiency) height += 10;
        if (transcended && !shouldShowHumanitySystem) height += 10;

        drawCyberPanel(x, y, width, height, energyStatus);

        int currentY = y + PANEL_PADDING;
        int contentWidth = width - (PANEL_PADDING * 2);
        int contentX = x + PANEL_PADDING;

        // Title
        String title = TextFormatting.AQUA + "◆ 核心状态";
        if (energyStatus != EnergyDepletionManager.EnergyStatus.NORMAL) {
            title = energyStatus.color + "⚠ " + energyStatus.displayName;
        }
        fr.drawStringWithShadow(title, contentX, currentY, 0xFFFFFF);

        // Energy %
        float energyPercent = (float)energy.getEnergyStored() / energy.getMaxEnergyStored();
        String percentText = (int)(energyPercent * 100) + "%";
        drawRightAlignedString(percentText, contentX + contentWidth, currentY, getEnergyColor(energyPercent), fr);

        currentY += 10;

        // Energy Bar
        drawStyledBar(contentX, currentY, contentWidth, BAR_HEIGHT, energyPercent, getEnergyColor(energyPercent));
        currentY += 5;

        // Energy Val
        String energyVal = formatEnergy(energy.getEnergyStored()) + " RF";
        fr.drawStringWithShadow(TextFormatting.GRAY + energyVal, contentX, currentY, 0xAAAAAA);
        currentY += 10;

        // Wetness
        if (showWetness) {
            currentY += 2;
            String wetIcon = (wetness >= 80 ? TextFormatting.RED : (wetness >= 30 ? TextFormatting.YELLOW : TextFormatting.AQUA)) + "💧";
            fr.drawStringWithShadow(wetIcon + " 潮湿", contentX, currentY, 0xFFFFFF);
            drawRightAlignedString(wetness + "%", contentX + contentWidth, currentY, 0xFFFFFF, fr);

            currentY += 10;
            int barColor = wetness >= 80 ? 0xFFFF5555 : 0xFF55FFFF;
            drawStyledBar(contentX, currentY, contentWidth, BAR_HEIGHT, wetness / 100f, barColor);
            currentY += 6;
        }

        // Rejection
        if (showRejection) {
            currentY += 2;
            String rejIcon = (rejection >= 60 ? TextFormatting.RED : TextFormatting.GOLD) + "⚠";
            fr.drawStringWithShadow(rejIcon + " 排异反应", contentX, currentY, 0xFFFFFF);
            drawRightAlignedString(String.format("%.1f%%", rejection), contentX + contentWidth, currentY, 0xFFFFFF, fr);

            currentY += 10;
            int rejColor = rejection >= 60 ? 0xFFFF0000 : 0xFFFFAA00;
            drawStyledBar(contentX, currentY, contentWidth, BAR_HEIGHT, rejection / 100f, rejColor);
            currentY += 6;
        } else if (transcended && !shouldShowHumanitySystem) {
            currentY += 2;
            fr.drawStringWithShadow(TextFormatting.GREEN + "✓ 机体适应完成", contentX, currentY, 0xFFFFFF);
            currentY += 10;
        }

        // Humanity
        if (showHumanity) {
            currentY += 2;
            float hVal = humanityData.getHumanity();
            int hColor = getHumanityBarColor(hVal, humanityData.getAscensionRoute());

            fr.drawStringWithShadow(TextFormatting.LIGHT_PURPLE + "⚛ 人性锚点", contentX, currentY, 0xFFFFFF);
            drawRightAlignedString((int)hVal + "%", contentX + contentWidth, currentY, 0xFFFFFF, fr);

            currentY += 10;
            drawStyledBar(contentX, currentY, contentWidth, BAR_HEIGHT, hVal / 100f, hColor);
            currentY += 6;
        }

        // Energy Flow
        if (MechanicalCoreHUDConfig.showEnergyFlow) {
            currentY += 2;
            int netFlow = EnergyTracker.getNetFlow();
            String arrow = netFlow >= 0 ? TextFormatting.GREEN + "▲" : TextFormatting.RED + "▼";
            String flowText = arrow + " " + Math.abs(netFlow) + " RF/t";
            fr.drawStringWithShadow(flowText, contentX, currentY, 0xFFFFFF);
            currentY += 10;
        }

        // Efficiency
        if (MechanicalCoreHUDConfig.showEfficiency) {
            int efficiencyLevel = getLocalUpgradeLevel(coreStack, UpgradeType.ENERGY_EFFICIENCY);
            if (efficiencyLevel > 0) {
                int efficiencyPercent = efficiencyLevel * 15;
                TextFormatting color = TextFormatting.GREEN;
                if (efficiencyPercent >= 60) color = TextFormatting.GOLD;
                fr.drawStringWithShadow(color + "⚡ 效率: -" + efficiencyPercent + "%", contentX, currentY, 0xFFFFFF);
                currentY += 10;
            }
        }

        return height;
    }

    private void renderModulePanel(int x, int y, List<String> lines, FontRenderer fr) {
        if (lines.isEmpty()) return;

        int maxWidth = 80;
        for (String line : lines) {
            int w = fr.getStringWidth(line);
            if (w > maxWidth) maxWidth = w;
        }
        int width = maxWidth + (PANEL_PADDING * 2);

        int maxLines = MechanicalCoreHUDConfig.getCurrentMaxDisplayUpgrades();
        int scrollOffset = KeyBindHandler.getScrollOffset();

        if (lines.size() > maxLines) {
            if (scrollOffset >= lines.size()) {
                scrollOffset = 0;
                KeyBindHandler.resetScrollOffset();
            }
            int endIndex = Math.min(scrollOffset + maxLines, lines.size());
            List<String> subList = new ArrayList<>(lines.subList(scrollOffset, endIndex));
            subList.add(TextFormatting.DARK_GRAY + "... (" + (scrollOffset+1) + "-" + endIndex + ")");
            lines = subList;
        }

        int height = (lines.size() * 10) + (PANEL_PADDING * 2);

        drawCyberPanel(x, y, width, height, EnergyDepletionManager.EnergyStatus.NORMAL);

        int currentY = y + PANEL_PADDING;
        int contentX = x + PANEL_PADDING;

        for (String line : lines) {
            fr.drawStringWithShadow(line, contentX, currentY, 0xFFFFFF);
            currentY += 10;
        }
    }

    private void drawCyberPanel(int x, int y, int width, int height, EnergyDepletionManager.EnergyStatus status) {
        drawRect(x, y, x + width, y + height, 0xCC000000);

        int borderColor = 0xFF00AAAA;
        if (status == EnergyDepletionManager.EnergyStatus.CRITICAL) borderColor = 0xFFFF0000;
        else if (status == EnergyDepletionManager.EnergyStatus.EMERGENCY) borderColor = 0xFFFF5500;
        else if (status == EnergyDepletionManager.EnergyStatus.POWER_SAVING) borderColor = 0xFFFFAA00;

        drawRect(x, y, x + 2, y + height, borderColor);
        drawRect(x + 2, y, x + width, y + 1, borderColor);
        drawRect(x + 2, y + height - 1, x + width, y + height, borderColor);
        drawRect(x + width - 1, y, x + width, y + height, borderColor);
        drawRect(x + width - 5, y, x + width, y + 5, borderColor);
    }

    private void drawStyledBar(int x, int y, int width, int height, float percent, int color) {
        drawRect(x, y, x + width, y + height, 0xFF222222);
        if (percent > 0) {
            int fillW = (int)(width * percent);
            drawRect(x, y, x + fillW, y + height, color);
            drawRect(x, y, x + fillW, y + 1, 0x40FFFFFF);
        }
    }

    private void collectActiveUpgrades(List<String> list, ItemStack coreStack, EntityPlayer player) {
        NBTTagCompound nbt = coreStack.getTagCompound();
        if (nbt == null) nbt = new NBTTagCompound();

        if (nbt.getBoolean("FlightModuleEnabled") && !nbt.getBoolean("Disabled_FLIGHT_MODULE")) {
            String txt = TextFormatting.LIGHT_PURPLE + "✈ 飞行";
            if (nbt.getBoolean("FlightHoverMode")) txt += " [悬停]";
            list.add(txt);
        }

        for (UpgradeType type : UpgradeType.values()) {
            if (type == UpgradeType.FLIGHT_MODULE) continue;

            int lvl = getLocalUpgradeLevel(coreStack, type);
            String id = type.name();

            if (lvl > 0 && !nbt.getBoolean("Disabled_" + id)) {
                String info = getSimpleUpgradeText(type, lvl, player, nbt);
                if (info != null) list.add(info);
            }
        }
    }

    private String getSimpleUpgradeText(UpgradeType type, int level, EntityPlayer player, NBTTagCompound nbt) {
        TextFormatting c = type.getColor();
        switch (type) {
            case SPEED_BOOST: if(player.isSprinting()) return c + "⚡ 加速"; break;
            case MOVEMENT_SPEED: if(player.isSprinting()) return c + "⚡ 移速"; break;
            case SHIELD_GENERATOR:
            case YELLOW_SHIELD:
                if(player.getAbsorptionAmount()>0) return c + "🛡 护盾 " + (int)player.getAbsorptionAmount();
                break;
            case FIRE_EXTINGUISH: if(player.isBurning()) return c + "🔥 灭火中"; break;
            case WATERPROOF_MODULE: if(player.isInWater()) return c + "💧 防水"; break;
            case ORE_VISION: if(nbt.getBoolean("OreVisionActive")) return c + "⛏ 矿视"; break;
            case STEALTH:
                if(player.isInvisible() || nbt.getBoolean("StealthActive")) return c + "👻 隐形";
                break;
            case HUNGER_THIRST:
                if(player.getFoodStats().getFoodLevel()<20) return c + "🍖 补给";
                break;
            case KINETIC_GENERATOR:
                if(Math.abs(player.motionX) > 0.01 || Math.abs(player.motionZ) > 0.01) return c + "⚙ 动能发电";
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

    // ========== 修复后的本地数据获取方法 ==========

    private int getLocalUpgradeLevel(ItemStack stack, UpgradeType type) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return 0;
        NBTTagCompound nbt = stack.getTagCompound();

        String key = "Upgrade_" + type.name();
        if (nbt.hasKey(key)) return nbt.getInteger(key);

        if (nbt.hasKey("Upgrades")) {
            NBTTagCompound upgrades = nbt.getCompoundTag("Upgrades");
            if (upgrades.hasKey(type.name())) return upgrades.getInteger(type.name());
        }

        return 0;
    }

    private EnergyDepletionManager.EnergyStatus getLocalEnergyStatus(IEnergyStorage energy) {
        if (energy.getMaxEnergyStored() <= 0) return EnergyDepletionManager.EnergyStatus.NORMAL;
        float percent = (float) energy.getEnergyStored() / energy.getMaxEnergyStored();

        if (percent <= 0.05f) return EnergyDepletionManager.EnergyStatus.CRITICAL;
        if (percent <= 0.15f) return EnergyDepletionManager.EnergyStatus.EMERGENCY;
        if (percent <= 0.30f) return EnergyDepletionManager.EnergyStatus.POWER_SAVING;
        return EnergyDepletionManager.EnergyStatus.NORMAL;
    }

    private int calculateHudX(ScaledResolution resolution) {
        return MechanicalCoreHUDConfig.xOffset + 5;
    }

    private int calculateHudY(ScaledResolution resolution) {
        return MechanicalCoreHUDConfig.yOffset + 5;
    }

    private int getEnergyColor(float percent) {
        return MechanicalCoreHUDConfig.getEnergyColor(percent);
    }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) return String.format("%.1fM", energy / 1000000.0);
        if (energy >= 1000) return String.format("%.1fk", energy / 1000.0);
        return String.valueOf(energy);
    }

    private void drawRightAlignedString(String text, int x, int y, int color, FontRenderer fr) {
        fr.drawStringWithShadow(text, x - fr.getStringWidth(text), y, color);
    }

    // NBT 安全读取
    private float getClientRejectionLevel(ItemStack stack) {
        if(stack.isEmpty()) return 0;
        return stack.getOrCreateSubCompound("rejection").getFloat("RejectionLevel");
    }

    private boolean getClientTranscendedStatus(ItemStack stack) {
        if(stack.isEmpty()) return false;
        return stack.getOrCreateSubCompound("rejection").getBoolean("RejectionTranscended");
    }

    private RejectionDisplayInfo getClientRejectionInfo(ItemStack stack) {
        return new RejectionDisplayInfo();
    }

    private static class RejectionDisplayInfo {
        float growthRate = 0;
    }

    private int getHumanityBarColor(float h, AscensionRoute r) {
        if (r == AscensionRoute.MEKHANE_SYNTHETIC) return 0xFFDD88FF;
        if (h < 25) return 0xFFAA0000;
        if (h < 50) return 0xFFAA00AA;
        return 0xFF00AAFF;
    }
}