package com.moremod.upgrades.platform;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemMechanicalCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 核心模块物品辅助类
 *
 * 功能：
 * - 从玩家获取机械核心物品
 * - 获取核心上的模块信息
 * - 提供便捷的查询方法
 *
 * 设计：
 * - 无状态工具类（所有方法都是静态的）
 * - Null-Safe（所有方法都处理null情况）
 */
public class CoreModuleItemHelper {

    // ===== 获取核心物品 =====

    /**
     * 从玩家获取装备的机械核心
     */
    @Nonnull
    public static ItemStack getEquippedCore(@Nonnull EntityPlayer player) {
        return ItemMechanicalCore.findEquippedMechanicalCore(player);
    }

    /**
     * 检查玩家是否装备了机械核心
     */
    public static boolean hasEquippedCore(@Nonnull EntityPlayer player) {
        return !getEquippedCore(player).isEmpty();
    }

    /**
     * 从玩家的 Bauble 槽位获取机械核心
     */
    @Nullable
    public static ItemStack getCoreFromBaubles(@Nonnull EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles == null) {
                return null;
            }

            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (ItemMechanicalCore.isMechanicalCore(stack)) {
                    return stack;
                }
            }
        } catch (Throwable t) {
            System.err.println("[CoreModuleItemHelper] 获取 Bauble 核心失败: " + t.getMessage());
        }

        return null;
    }

    // ===== 模块状态查询 =====

    /**
     * 获取模块等级
     */
    public static int getModuleLevel(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        ItemStack core = getEquippedCore(player);
        if (core.isEmpty()) {
            return 0;
        }
        return ModuleDataStorage.getModuleLevel(core, moduleId);
    }

    /**
     * 检查模块是否激活
     */
    public static boolean isModuleActive(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        ItemStack core = getEquippedCore(player);
        if (core.isEmpty()) {
            return false;
        }
        return ModuleDataStorage.isModuleActive(core, moduleId);
    }

    /**
     * 检查模块是否已安装（等级 > 0 或曾经安装过）
     */
    public static boolean hasModule(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        ItemStack core = getEquippedCore(player);
        if (core.isEmpty()) {
            return false;
        }
        return ModuleDataStorage.hasModule(core, moduleId);
    }

    /**
     * 获取模块状态
     */
    @Nonnull
    public static ModuleState getModuleState(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        ItemStack core = getEquippedCore(player);
        if (core.isEmpty()) {
            return new ModuleState(moduleId);
        }
        return ModuleDataStorage.loadState(core, moduleId);
    }

    /**
     * 获取所有已安装模块的状态
     */
    @Nonnull
    public static Map<String, ModuleState> getAllModuleStates(@Nonnull EntityPlayer player) {
        ItemStack core = getEquippedCore(player);
        if (core.isEmpty()) {
            return new java.util.HashMap<>();
        }
        return ModuleDataStorage.loadAllStates(core);
    }

    /**
     * 获取所有激活的模块ID
     */
    @Nonnull
    public static List<String> getActiveModuleIds(@Nonnull EntityPlayer player) {
        List<String> activeModules = new ArrayList<>();
        Map<String, ModuleState> allStates = getAllModuleStates(player);

        for (Map.Entry<String, ModuleState> entry : allStates.entrySet()) {
            if (entry.getValue().isActive()) {
                activeModules.add(entry.getKey());
            }
        }

        return activeModules;
    }

    /**
     * 获取已安装模块数量
     */
    public static int getInstalledModuleCount(@Nonnull EntityPlayer player) {
        return getAllModuleStates(player).size();
    }

    /**
     * 获取激活模块数量
     */
    public static int getActiveModuleCount(@Nonnull EntityPlayer player) {
        int count = 0;
        for (ModuleState state : getAllModuleStates(player).values()) {
            if (state.isActive()) {
                count++;
            }
        }
        return count;
    }

    // ===== 模块操作 =====

    /**
     * 设置模块等级
     */
    public static void setModuleLevel(@Nonnull EntityPlayer player, @Nonnull String moduleId, int level) {
        ItemStack core = getEquippedCore(player);
        if (!core.isEmpty()) {
            ModulePlatform.getInstance().setModuleLevel(core, moduleId, level);
        }
    }

    /**
     * 暂停模块
     */
    public static void pauseModule(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        ItemStack core = getEquippedCore(player);
        if (!core.isEmpty()) {
            ModulePlatform.getInstance().pauseModule(core, moduleId);
        }
    }

    /**
     * 恢复模块
     */
    public static void resumeModule(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        ItemStack core = getEquippedCore(player);
        if (!core.isEmpty()) {
            ModulePlatform.getInstance().resumeModule(core, moduleId);
        }
    }

    /**
     * 禁用模块
     */
    public static void disableModule(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        ItemStack core = getEquippedCore(player);
        if (!core.isEmpty()) {
            ModulePlatform.getInstance().disableModule(core, moduleId);
        }
    }

    /**
     * 启用模块
     */
    public static void enableModule(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        ItemStack core = getEquippedCore(player);
        if (!core.isEmpty()) {
            ModulePlatform.getInstance().enableModule(core, moduleId);
        }
    }

    // ===== 创建上下文 =====

    /**
     * 为玩家创建模块上下文
     */
    @Nullable
    public static ModuleContext createContext(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        ItemStack core = getEquippedCore(player);
        if (core.isEmpty()) {
            return null;
        }
        return ModulePlatform.getInstance().createContext(player, core, moduleId);
    }

    // ===== 调试方法 =====

    /**
     * 打印玩家的所有模块状态（调试用）
     */
    public static void debugPrintModules(@Nonnull EntityPlayer player) {
        System.out.println("=== 玩家模块状态: " + player.getName() + " ===");
        Map<String, ModuleState> allStates = getAllModuleStates(player);

        if (allStates.isEmpty()) {
            System.out.println("  (无模块)");
            return;
        }

        for (Map.Entry<String, ModuleState> entry : allStates.entrySet()) {
            System.out.println("  " + entry.getValue());
        }
        System.out.println("=== 总计: " + allStates.size() + " 个模块 ===");
    }
}
