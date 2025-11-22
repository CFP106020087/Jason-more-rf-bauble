package com.moremod.upgrades.platform;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * 模块平台 - 核心入口
 *
 * 功能：
 * - 封装所有底层逻辑（NBT、tick、事件、生命周期）
 * - 提供简洁的 API 供外部调用
 * - 自动管理模块状态和数据持久化
 *
 * 设计：
 * - 单例模式
 * - 所有复杂逻辑都隐藏在内部
 * - 新模块开发者只需调用简单方法
 *
 * 使用示例：
 * <pre>
 * // 在机械核心的 onWornTick 中调用
 * ModulePlatform.getInstance().tickAllModules(player, coreStack);
 *
 * // 在事件处理器中调用
 * ModulePlatform.getInstance().handleEvent(event, player, coreStack);
 *
 * // 在装备时调用
 * ModulePlatform.getInstance().onCoreEquipped(player, coreStack);
 *
 * // 在卸载时调用
 * ModulePlatform.getInstance().onCoreUnequipped(player, coreStack);
 * </pre>
 */
public class ModulePlatform {

    private static final ModulePlatform INSTANCE = new ModulePlatform();

    private boolean initialized = false;

    private ModulePlatform() {}

    public static ModulePlatform getInstance() {
        return INSTANCE;
    }

    // ===== 初始化 =====

    /**
     * 初始化平台（在 Mod 加载时调用一次）
     */
    public void initialize() {
        if (initialized) {
            System.out.println("[ModulePlatform] 已经初始化，跳过");
            return;
        }

        System.out.println("[ModulePlatform] 开始初始化模块平台...");

        // 标记注册表初始化完成
        ModuleRegistry.getInstance().markInitialized();

        initialized = true;
        System.out.println("[ModulePlatform] 模块平台初始化完成");
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    // ===== 核心生命周期方法 =====

    /**
     * 当机械核心被装备时调用
     */
    public void onCoreEquipped(@Nonnull EntityPlayer player, @Nonnull ItemStack coreStack) {
        if (player.world.isRemote) {
            return;
        }

        try {
            // 加载所有模块状态
            Map<String, ModuleState> allStates = ModuleDataStorage.loadAllStates(coreStack);

            // 分发装备事件到所有模块
            ModuleDispatcher.dispatchEquip(player, coreStack, allStates);

            System.out.println("[ModulePlatform] 机械核心已装备，模块数: " + allStates.size());

        } catch (Throwable t) {
            System.err.println("[ModulePlatform] 装备处理失败");
            t.printStackTrace();
        }
    }

    /**
     * 当机械核心被卸载时调用
     */
    public void onCoreUnequipped(@Nonnull EntityPlayer player, @Nonnull ItemStack coreStack) {
        if (player.world.isRemote) {
            return;
        }

        try {
            // 加载所有模块状态
            Map<String, ModuleState> allStates = ModuleDataStorage.loadAllStates(coreStack);

            // 分发卸载事件到所有模块
            ModuleDispatcher.dispatchUnequip(player, coreStack, allStates);

            System.out.println("[ModulePlatform] 机械核心已卸载");

        } catch (Throwable t) {
            System.err.println("[ModulePlatform] 卸载处理失败");
            t.printStackTrace();
        }
    }

    /**
     * 每 tick 调用（在机械核心的 onWornTick 中调用）
     */
    public void tickAllModules(@Nonnull EntityPlayer player, @Nonnull ItemStack coreStack) {
        if (player.world.isRemote) {
            return;
        }

        try {
            // 加载所有模块状态
            Map<String, ModuleState> allStates = ModuleDataStorage.loadAllStates(coreStack);

            // 分发 tick 到所有激活的模块
            ModuleDispatcher.tickAllModules(player, coreStack, allStates);

            // 保存模块状态（如果有修改）
            ModuleDataStorage.saveAllStates(coreStack, allStates);

        } catch (Throwable t) {
            System.err.println("[ModulePlatform] Tick 处理失败");
            t.printStackTrace();
        }
    }

    /**
     * 处理事件（在事件处理器中调用）
     */
    public void handleEvent(@Nonnull Event event, @Nonnull EntityPlayer player, @Nonnull ItemStack coreStack) {
        try {
            // 加载所有模块状态
            Map<String, ModuleState> allStates = ModuleDataStorage.loadAllStates(coreStack);

            // 分发事件到所有激活的模块
            ModuleDispatcher.dispatchEvent(event, player, coreStack, allStates);

            // 保存模块状态（如果有修改）
            ModuleDataStorage.saveAllStates(coreStack, allStates);

        } catch (Throwable t) {
            System.err.println("[ModulePlatform] 事件处理失败: " + event.getClass().getSimpleName());
            t.printStackTrace();
        }
    }

    // ===== 模块状态管理 =====

    /**
     * 获取模块状态
     */
    @Nonnull
    public ModuleState getModuleState(@Nonnull ItemStack coreStack, @Nonnull String moduleId) {
        return ModuleDataStorage.loadState(coreStack, moduleId);
    }

    /**
     * 设置模块等级
     */
    public void setModuleLevel(@Nonnull ItemStack coreStack, @Nonnull String moduleId, int level) {
        ModuleState state = ModuleDataStorage.loadState(coreStack, moduleId);
        state.setLevel(level);
        ModuleDataStorage.saveState(coreStack, state);
    }

    /**
     * 获取模块等级
     */
    public int getModuleLevel(@Nonnull ItemStack coreStack, @Nonnull String moduleId) {
        return ModuleDataStorage.getModuleLevel(coreStack, moduleId);
    }

    /**
     * 检查模块是否激活
     */
    public boolean isModuleActive(@Nonnull ItemStack coreStack, @Nonnull String moduleId) {
        return ModuleDataStorage.isModuleActive(coreStack, moduleId);
    }

    /**
     * 暂停模块
     */
    public void pauseModule(@Nonnull ItemStack coreStack, @Nonnull String moduleId) {
        ModuleState state = ModuleDataStorage.loadState(coreStack, moduleId);
        state.setPaused(true);
        ModuleDataStorage.saveState(coreStack, state);
    }

    /**
     * 恢复模块
     */
    public void resumeModule(@Nonnull ItemStack coreStack, @Nonnull String moduleId) {
        ModuleState state = ModuleDataStorage.loadState(coreStack, moduleId);
        state.setPaused(false);
        ModuleDataStorage.saveState(coreStack, state);
    }

    /**
     * 禁用模块
     */
    public void disableModule(@Nonnull ItemStack coreStack, @Nonnull String moduleId) {
        ModuleState state = ModuleDataStorage.loadState(coreStack, moduleId);
        state.setDisabled(true);
        ModuleDataStorage.saveState(coreStack, state);
    }

    /**
     * 启用模块
     */
    public void enableModule(@Nonnull ItemStack coreStack, @Nonnull String moduleId) {
        ModuleState state = ModuleDataStorage.loadState(coreStack, moduleId);
        state.setDisabled(false);
        ModuleDataStorage.saveState(coreStack, state);
    }

    // ===== 批量操作 =====

    /**
     * 暂停所有模块
     */
    public void pauseAllModules(@Nonnull ItemStack coreStack) {
        Map<String, ModuleState> allStates = ModuleDataStorage.loadAllStates(coreStack);
        for (ModuleState state : allStates.values()) {
            state.setPaused(true);
        }
        ModuleDataStorage.saveAllStates(coreStack, allStates);
    }

    /**
     * 恢复所有模块
     */
    public void resumeAllModules(@Nonnull ItemStack coreStack) {
        Map<String, ModuleState> allStates = ModuleDataStorage.loadAllStates(coreStack);
        for (ModuleState state : allStates.values()) {
            state.setPaused(false);
        }
        ModuleDataStorage.saveAllStates(coreStack, allStates);
    }

    // ===== 工具方法 =====

    /**
     * 创建模块上下文
     */
    @Nonnull
    public ModuleContext createContext(@Nonnull EntityPlayer player,
                                      @Nonnull ItemStack coreStack,
                                      @Nonnull String moduleId) {
        ModuleState state = getModuleState(coreStack, moduleId);
        return new ModuleContext(player, coreStack, state);
    }

    @Override
    public String toString() {
        return String.format("ModulePlatform{initialized=%s, modules=%d}",
                initialized, ModuleRegistry.getInstance().getModuleCount());
    }
}
