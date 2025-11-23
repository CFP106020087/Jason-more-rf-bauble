package com.moremod.upgrades;

import com.moremod.item.ItemMechanicalCore;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 能量效率管理器 (Minecraft 1.12.2)
 * 负责计算和应用能量效率加成
 *
 * v2.0 - 静默版本，无刷屏提示
 */
public class EnergyEfficiencyManager {

    // 调试模式
    private static final boolean DEBUG_MODE = false;

    /**
     * 获取玩家的能量效率倍率
     * @param player 玩家实体
     * @return 效率倍率 (0.0 - 1.0，越小消耗越少)
     */
    public static double getEfficiencyMultiplier(EntityPlayer player) {
        ItemStack coreStack = findMechanicalCore(player);
        if (coreStack.isEmpty()) {
            return 1.0; // 无核心，无加成
        }

        // ✅ Set player context for upgrade reads
        ItemMechanicalCore.setPlayerContext(player);
        int level;
        try {
            level = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.ENERGY_EFFICIENCY);
        } finally {
            ItemMechanicalCore.clearPlayerContext();
        }

        // 根据等级计算效率
        switch (level) {
            case 0: return 1.00;
            case 1: return 0.85;
            case 2: return 0.70;
            case 3: return 0.55;
            case 4: return 0.40;
            case 5: return 0.25;
            default:
                if (level > 5) {
                    return Math.max(0.10, 0.25 - (level - 5) * 0.05);
                }
                return 1.00;
        }
    }

    /**
     * 获取能量减免百分比（用于显示）
     * @param player 玩家实体
     * @return 减免百分比 (0-90)
     */
    public static int getEfficiencyPercentage(EntityPlayer player) {
        double multiplier = getEfficiencyMultiplier(player);
        return (int)((1.0 - multiplier) * 100);
    }

    /**
     * 计算实际能量消耗
     * @param player 玩家实体
     * @param originalCost 原始消耗
     * @return 实际消耗（应用效率后）
     */
    public static int calculateActualCost(EntityPlayer player, int originalCost) {
        double multiplier = getEfficiencyMultiplier(player);
        int actualCost = (int)(originalCost * multiplier);

        // 确保至少消耗1点能量（如果原本要消耗的话）
        if (originalCost > 0 && actualCost == 0) {
            actualCost = 1;
        }

        return actualCost;
    }

    /**
     * 计算节省的能量（仅用于统计）
     * @param player 玩家实体
     * @param originalCost 原始消耗
     * @return 节省的能量值
     */
    public static int calculateSavedEnergy(EntityPlayer player, int originalCost) {
        int actualCost = calculateActualCost(player, originalCost);
        return originalCost - actualCost;
    }

    /**
     * 记录能量节省（静默版本，不显示消息）
     * 这个方法替代了原来的 showEfficiencySaving
     * @param player 玩家实体
     * @param originalCost 原始消耗
     * @param actualCost 实际消耗
     */
    public static void recordEfficiencySaving(EntityPlayer player, int originalCost, int actualCost) {
        if (originalCost <= actualCost) return;

        ItemStack coreStack = findMechanicalCore(player);
        if (coreStack.isEmpty()) return;

        // 只记录到 NBT，不显示任何消息
        NBTTagCompound nbt = coreStack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            coreStack.setTagCompound(nbt);
        }

        int saved = originalCost - actualCost;
        long totalSaved = nbt.getLong("TotalEnergySaved");
        nbt.setLong("TotalEnergySaved", totalSaved + saved);

        // 记录本次会话节省
        int sessionSaved = nbt.getInteger("SessionEnergySaved");
        nbt.setInteger("SessionEnergySaved", sessionSaved + saved);

        // 调试日志
        if (DEBUG_MODE) {
            System.out.println("[EnergyEfficiency] 节省了 " + saved + " RF (总计: " + (totalSaved + saved) + " RF)");
        }
    }

    /**
     * 兼容旧代码的方法（静默处理）
     * @deprecated 使用 recordEfficiencySaving 代替
     */
    @Deprecated
    public static void showEfficiencySaving(EntityPlayer player, int originalCost, int actualCost) {
        // 静默记录，不显示消息
        recordEfficiencySaving(player, originalCost, actualCost);
    }

    /**
     * 查找玩家装备的机械核心
     * @param player 玩家实体
     * @return 机械核心物品栈，如果没有则返回空
     */
    private static ItemStack findMechanicalCore(EntityPlayer player) {
        // 检查 Baubles 饰品栏
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (ItemMechanicalCore.isMechanicalCore(stack)) {
                    return stack;
                }
            }
        }

        // 检查主副手
        ItemStack mainHand = player.getHeldItemMainhand();
        if (ItemMechanicalCore.isMechanicalCore(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = player.getHeldItemOffhand();
        if (ItemMechanicalCore.isMechanicalCore(offHand)) {
            return offHand;
        }

        // 检查物品栏
        for (ItemStack stack : player.inventory.mainInventory) {
            if (ItemMechanicalCore.isMechanicalCore(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * 获取能量效率等级
     * @param player 玩家实体
     * @return 效率等级
     */
    public static int getEfficiencyLevel(EntityPlayer player) {
        ItemStack coreStack = findMechanicalCore(player);
        if (coreStack.isEmpty()) {
            return 0;
        }

        // ✅ Set player context for upgrade reads
        ItemMechanicalCore.setPlayerContext(player);
        try {
            return ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.ENERGY_EFFICIENCY);
        } finally {
            ItemMechanicalCore.clearPlayerContext();
        }
    }

    /**
     * 检查是否有能量效率升级
     * @param player 玩家实体
     * @return 是否有效率升级
     */
    public static boolean hasEfficiencyUpgrade(EntityPlayer player) {
        return getEfficiencyLevel(player) > 0;
    }

    /**
     * 应用能量效率（静默处理，无提示）
     * @param player 玩家实体
     * @param coreStack 机械核心物品栈
     */
    public static void applyPassiveEffects(EntityPlayer player, ItemStack coreStack) {
        // 静默应用效率效果，不显示任何提示
        // 效率计算已经在其他方法中处理
        // 这个方法保留用于未来可能的扩展
    }

    /**
     * 获取效率信息字符串（供GUI或命令使用）
     * @param player 玩家实体
     * @return 格式化的效率信息
     */
    public static String getEfficiencyInfo(EntityPlayer player) {
        int level = getEfficiencyLevel(player);
        if (level <= 0) {
            return "无能量效率升级";
        }

        int percentage = getEfficiencyPercentage(player);
        return String.format("能量效率 Lv.%d (减少 %d%% 消耗)", level, percentage);
    }

    /**
     * 直接从ItemStack计算效率倍率（不需要玩家实体）
     * @param coreStack 机械核心物品栈
     * @return 效率倍率
     */
    public static double getEfficiencyMultiplierFromStack(ItemStack coreStack) {
        if (coreStack.isEmpty() || !ItemMechanicalCore.isMechanicalCore(coreStack)) {
            return 1.0;
        }

        int level = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.ENERGY_EFFICIENCY);

        switch (level) {
            case 0: return 1.00;
            case 1: return 0.85;
            case 2: return 0.70;
            case 3: return 0.55;
            case 4: return 0.40;
            case 5: return 0.25;
            default:
                if (level > 5) {
                    return Math.max(0.10, 0.25 - (level - 5) * 0.05);
                }
                return 1.00;
        }
    }

    /**
     * 获取总节省的能量（从NBT读取）
     * @param player 玩家实体
     * @return 总节省的能量值
     */
    public static long getTotalEnergySaved(EntityPlayer player) {
        ItemStack coreStack = findMechanicalCore(player);
        if (coreStack.isEmpty() || !coreStack.hasTagCompound()) {
            return 0;
        }
        return coreStack.getTagCompound().getLong("TotalEnergySaved");
    }

    /**
     * 获取本次会话节省的能量
     * @param player 玩家实体
     * @return 本次会话节省的能量值
     */
    public static int getSessionEnergySaved(EntityPlayer player) {
        ItemStack coreStack = findMechanicalCore(player);
        if (coreStack.isEmpty() || !coreStack.hasTagCompound()) {
            return 0;
        }
        return coreStack.getTagCompound().getInteger("SessionEnergySaved");
    }

    /**
     * 重置会话节省统计
     * @param player 玩家实体
     */
    public static void resetSessionSaved(EntityPlayer player) {
        ItemStack coreStack = findMechanicalCore(player);
        if (!coreStack.isEmpty() && coreStack.hasTagCompound()) {
            coreStack.getTagCompound().setInteger("SessionEnergySaved", 0);
        }
    }
}