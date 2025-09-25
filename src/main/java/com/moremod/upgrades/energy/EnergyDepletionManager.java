package com.moremod.upgrades.energy;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.util.UpgradeKeys;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.IEnergyStorage;
import com.moremod.event.EnergyPunishmentSystem;

/**
 * 能量耗尽管理系统（统一键名 + 惩罚锁 + 防刷屏 + 接入惩罚系统）
 */
public class EnergyDepletionManager {

    // ====== 能量状态 ======
    public enum EnergyStatus {
        NORMAL(0.30f, "正常运行",  TextFormatting.GREEN,     "✓"),
        POWER_SAVING(0.15f, "省电模式", TextFormatting.YELLOW,    "⚡"),
        EMERGENCY(0.05f, "紧急模式",  TextFormatting.RED,       "⚠"),
        CRITICAL(0.00f, "生命支持",  TextFormatting.DARK_RED,  "💀");

        public final float threshold;
        public final String displayName;
        public final TextFormatting color;
        public final String icon;

        EnergyStatus(float threshold, String displayName, TextFormatting color, String icon) {
            this.threshold = threshold;
            this.displayName = displayName;
            this.color = color;
            this.icon = icon;
        }
    }

    // ====== 升级可用性判定（统一键名 + 锁 + 状态门控）======
    public static boolean isUpgradeActive(ItemStack stack, String upgradeId) {
        if (!ItemMechanicalCore.isMechanicalCore(stack)) return false;

        String cid = UpgradeKeys.foldAlias(upgradeId);
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(stack);

        // 手动禁用
        if (nbt.getBoolean(UpgradeKeys.kDisabled(cid))) return false;
        // 惩罚锁定
        if (nbt.getBoolean(UpgradeKeys.kLock(cid)))     return false;

        IEnergyStorage energy = ItemMechanicalCore.getEnergyStorage(stack);
        if (energy == null) return false;

        // 最低能量线
        int min = getMinimumEnergyForUpgrade(cid);
        if (energy.getEnergyStored() < min) return false;

        // 能量状态门控
        EnergyStatus status = getCurrentEnergyStatus(stack);
        switch (status) {
            case NORMAL:
                return true;
            case POWER_SAVING:
                return !isHighConsumptionUpgrade(cid);
            case EMERGENCY:
                return isImportantUpgrade(cid);
            case CRITICAL:
                return isEssentialUpgrade(cid);
            default:
                return false;
        }
    }

    // ====== 升级最低能量线（可按需调整）======
    private static int getMinimumEnergyForUpgrade(String cid) {
        switch (cid) {
            // 生存必需
            case "HEALTH_REGEN":
            case "REGENERATION":       return 0;
            case "FIRE_EXTINGUISH":    return 50;
            case "THORNS":             return 0;

            // 防护
            case "YELLOW_SHIELD":
            case "SHIELD_GENERATOR":   return 300;
            case "HUNGER_THIRST":      return 200;
            case "TEMPERATURE_CONTROL":return 100;

            // 战斗
            case "DAMAGE_BOOST":       return 400;
            case "ATTACK_SPEED":       return 200;
            case "RANGE_EXTENSION":    return 300;
            case "PURSUIT":            return 500;

            // 移动
            case "MOVEMENT_SPEED":
            case "SPEED_BOOST":        return 600;
            case "FLIGHT_MODULE":      return 800;

            // 特殊
            case "ORE_VISION":         return 1200;
            case "STEALTH":            return 1000;
            case "EXP_AMPLIFIER":      return 400;

            // 被动
            case "ENERGY_CAPACITY":
            case "ENERGY_EFFICIENCY":  return 0;
            case "ARMOR_ENHANCEMENT":  return 300;

            default:                   return 500;
        }
    }

    // ====== 高耗能 / 重要 / 必需 ======
    private static boolean isHighConsumptionUpgrade(String cid) {
        switch (cid) {
            case "ORE_VISION":
            case "STEALTH":
            case "FLIGHT_MODULE":
                return true;
            default:
                return false;
        }
    }

    private static boolean isImportantUpgrade(String cid) {
        switch (cid) {
            case "HEALTH_REGEN":
            case "REGENERATION":
            case "FIRE_EXTINGUISH":
            case "THORNS":
            case "YELLOW_SHIELD":
            case "SHIELD_GENERATOR":
            case "HUNGER_THIRST":
            case "TEMPERATURE_CONTROL":
            case "DAMAGE_BOOST":
            case "ATTACK_SPEED":
            case "ARMOR_ENHANCEMENT":
                return true;
            default:
                return false;
        }
    }

    private static boolean isEssentialUpgrade(String cid) {
        switch (cid) {
            case "HEALTH_REGEN":
            case "REGENERATION":
            case "FIRE_EXTINGUISH":
            case "THORNS":
            case "TEMPERATURE_CONTROL":
                return true;
            default:
                return false;
        }
    }

    // ====== 状态计算 ======
    public static EnergyStatus getCurrentEnergyStatus(ItemStack stack) {
        IEnergyStorage e = ItemMechanicalCore.getEnergyStorage(stack);
        if (e == null || e.getMaxEnergyStored() == 0) return EnergyStatus.CRITICAL;

        float p = (float) e.getEnergyStored() / e.getMaxEnergyStored();
        if (p >= EnergyStatus.NORMAL.threshold)        return EnergyStatus.NORMAL;
        else if (p >= EnergyStatus.POWER_SAVING.threshold) return EnergyStatus.POWER_SAVING;
        else if (p >= EnergyStatus.EMERGENCY.threshold)    return EnergyStatus.EMERGENCY;
        else return EnergyStatus.CRITICAL;
    }

    // ====== 主循环（仅状态变化提示 + 接入惩罚系统）======
    public static void handleEnergyDepletion(ItemStack stack, EntityPlayer player) {
        if (!ItemMechanicalCore.isMechanicalCore(stack)) return;

        EnergyStatus cur = getCurrentEnergyStatus(stack);
        EnergyStatus prev = getPreviousEnergyStatus(stack);

        if (cur != prev) {
            executeStatusTransition(stack, player, prev, cur);
            setPreviousEnergyStatus(stack, cur);
        }

        // 在低能量状态下，按惩罚系统节流规则执行
        if (cur == EnergyStatus.EMERGENCY || cur == EnergyStatus.CRITICAL) {
            EnergyPunishmentSystem.tick(stack, player, cur);
        }

        // 生命支持下的低频粒子（不刷屏）
        if (cur == EnergyStatus.CRITICAL && player.world.getTotalWorldTime() % 200 == 0) {
            for (int i = 0; i < 3; i++) {
                player.world.spawnParticle(
                        EnumParticleTypes.REDSTONE,
                        player.posX + (player.world.rand.nextDouble() - 0.5) * 2,
                        player.posY + player.world.rand.nextDouble() * 2,
                        player.posZ + (player.world.rand.nextDouble() - 0.5) * 2,
                        1.0, 0.0, 0.0
                );
            }
        }
    }

    // ====== 状态迁移（只在变化时提示一次）======
    private static void executeStatusTransition(ItemStack stack, EntityPlayer player,
                                                EnergyStatus from, EnergyStatus to) {
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(stack);

        // 清掉旧标记
        if (from != null) clearStatusFlags(nbt, from);

        switch (to) {
            case NORMAL:
                nbt.setBoolean("PowerSavingMode", false);
                nbt.setBoolean("EmergencyMode",   false);
                nbt.setBoolean("CriticalMode",    false);
                player.sendStatusMessage(new TextComponentString(
                        to.color + to.icon + " " + to.displayName + " - 所有系统已恢复"
                ), true);
                break;

            case POWER_SAVING:
                nbt.setBoolean("PowerSavingMode", true);
                IEnergyStorage e1 = ItemMechanicalCore.getEnergyStorage(stack);
                int pct1 = e1 == null ? 0 : (int)((float)e1.getEnergyStored() / e1.getMaxEnergyStored() * 100);
                player.sendStatusMessage(new TextComponentString(
                        to.color + to.icon + " " + to.displayName + " [" + pct1 + "%] - 高耗能功能已降低"
                ), true);
                break;

            case EMERGENCY:
                nbt.setBoolean("EmergencyMode", true);
                IEnergyStorage e2 = ItemMechanicalCore.getEnergyStorage(stack);
                int pct2 = e2 == null ? 0 : (int)((float)e2.getEnergyStored() / e2.getMaxEnergyStored() * 100);
                player.sendStatusMessage(new TextComponentString(
                        to.color + to.icon + " " + to.displayName + " [" + pct2 + "%] - 非必要系统已关闭"
                ), true);
                // 一次性警报音
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        net.minecraft.init.SoundEvents.BLOCK_NOTE_PLING,
                        net.minecraft.util.SoundCategory.PLAYERS, 0.8F, 0.5F);
                break;

            case CRITICAL:
                nbt.setBoolean("CriticalMode", true);
                if (!player.isCreative()) {
                    player.capabilities.allowFlying = false;
                    player.capabilities.isFlying = false;
                    player.sendPlayerAbilities();
                }
                IEnergyStorage e3 = ItemMechanicalCore.getEnergyStorage(stack);
                int pct3 = e3 == null ? 0 : (int)((float)e3.getEnergyStored() / e3.getMaxEnergyStored() * 100);
                player.sendStatusMessage(new TextComponentString(
                        to.color + to.icon + " " + to.displayName + " [" + pct3 + "%] - 仅保留生存系统！请立即充能！"
                ), true);
                // 严重警告音
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        net.minecraft.init.SoundEvents.ENTITY_WITHER_HURT,
                        net.minecraft.util.SoundCategory.PLAYERS, 0.5F, 2.0F);
                break;
        }
    }

    private static void clearStatusFlags(NBTTagCompound nbt, EnergyStatus s) {
        switch (s) {
            case POWER_SAVING: nbt.setBoolean("PowerSavingMode", false); break;
            case EMERGENCY:    nbt.setBoolean("EmergencyMode",   false); break;
            case CRITICAL:     nbt.setBoolean("CriticalMode",    false); break;
        }
    }

    private static EnergyStatus getPreviousEnergyStatus(ItemStack stack) {
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(stack);
        int ord = nbt.getInteger("PreviousEnergyStatus");
        if (ord >= 0 && ord < EnergyStatus.values().length) return EnergyStatus.values()[ord];
        return EnergyStatus.NORMAL;
    }
    private static void setPreviousEnergyStatus(ItemStack stack, EnergyStatus s) {
        UpgradeKeys.getOrCreate(stack).setInteger("PreviousEnergyStatus", s.ordinal());
    }

    // ====== 面板用：显示详细状态 ======
    public static void displayDetailedEnergyStatus(EntityPlayer player, ItemStack stack) {
        IEnergyStorage e = ItemMechanicalCore.getEnergyStorage(stack);
        if (e == null) return;
        EnergyStatus status = getCurrentEnergyStatus(stack);
        int cur = e.getEnergyStored();
        int max = e.getMaxEnergyStored();
        float pct = max <= 0 ? 0 : (float)cur / max * 100f;

        player.sendMessage(new TextComponentString(
                status.color + "⚡ 机械核心状态: " + status.icon + " " + status.displayName
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "能量: " + formatEnergy(cur) + " / " + formatEnergy(max)
                        + String.format(" (%.1f%%)", pct)
        ));

        // 系统概览（示例保留）
        String[] important = {"HEALTH_REGEN","YELLOW_SHIELD","DAMAGE_BOOST","MOVEMENT_SPEED","FLIGHT_MODULE","ORE_VISION","STEALTH"};
        player.sendMessage(new TextComponentString(TextFormatting.GRAY + "━━━ 系统状态 ━━━"));
        for (String id : important) {
            int lv = getUpgradeLevel(stack, id);
            if (lv > 0) {
                boolean active = isUpgradeActive(stack, id);
                TextFormatting c = active ? TextFormatting.GREEN : TextFormatting.RED;
                String ico = active ? "✓" : "✗";
                player.sendMessage(new TextComponentString(
                        c + ico + " " + getUpgradeDisplayName(id) + " Lv." + lv + (active ? "" : TextFormatting.GRAY + " (能量不足或锁定)")
                ));
            }
        }
    }

    // ====== 兼容读取/显示 ======
    private static int getUpgradeLevel(ItemStack stack, String id) {
        // 扩展
        int lv = ItemMechanicalCoreExtended.getUpgradeLevel(stack, id);
        if (lv > 0) return lv;
        // 基础
        for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
            if (t.getKey().equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)) {
                return ItemMechanicalCore.getUpgradeLevel(stack, t);
            }
        }
        // 规范键
        return UpgradeKeys.getLevel(stack, id);
    }

    private static String getUpgradeDisplayName(String id) {
        switch (UpgradeKeys.canon(id)) {
            case "HEALTH_REGEN":
            case "REGENERATION":     return "生命恢复";
            case "YELLOW_SHIELD":
            case "SHIELD_GENERATOR": return "护盾系统";
            case "DAMAGE_BOOST":     return "伤害增幅";
            case "MOVEMENT_SPEED":
            case "SPEED_BOOST":      return "移动速度";
            case "FLIGHT_MODULE":    return "飞行系统";
            case "ORE_VISION":       return "矿物透视";
            case "STEALTH":          return "隐身系统";
            case "ATTACK_SPEED":     return "攻击速度";
            case "RANGE_EXTENSION":  return "范围拓展";
            case "PURSUIT":          return "追击系统";
            default:                 return id;
        }
    }

    private static String formatEnergy(int e) {
        if (e >= 1_000_000) return String.format("%.1fM", e / 1_000_000.0);
        if (e >= 1_000)     return String.format("%.1fk", e / 1_000.0);
        return String.valueOf(e);
    }

    // ====== 供外部操作的便捷方法 ======
    public static boolean hasEnergyForOperation(ItemStack stack, String upgradeId, int extraCost) {
        if (!isUpgradeActive(stack, upgradeId)) return false;
        IEnergyStorage e = ItemMechanicalCore.getEnergyStorage(stack);
        if (e == null) return false;
        int need = getMinimumEnergyForUpgrade(UpgradeKeys.foldAlias(upgradeId)) + extraCost;
        return e.getEnergyStored() >= need;
    }

    public static void forceEmergencyMode(ItemStack stack, EntityPlayer player) {
        UpgradeKeys.getOrCreate(stack).setBoolean("ForceEmergencyMode", true);
        executeStatusTransition(stack, player, getCurrentEnergyStatus(stack), EnergyStatus.EMERGENCY);
        player.sendMessage(new TextComponentString(TextFormatting.RED + "⚠ 强制进入紧急模式！"));
    }
    public static void clearForceEmergencyMode(ItemStack stack) {
        UpgradeKeys.getOrCreate(stack).setBoolean("ForceEmergencyMode", false);
    }
}
