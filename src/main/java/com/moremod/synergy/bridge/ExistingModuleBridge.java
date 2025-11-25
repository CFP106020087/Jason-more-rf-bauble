package com.moremod.synergy.bridge;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.ItemMechanicalCoreExtended.UpgradeCategory;
import com.moremod.item.ItemMechanicalCoreExtended.UpgradeInfo;
import com.moremod.synergy.api.IInstalledModuleView;
import com.moremod.synergy.api.IModuleProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 现有模块系统的适配器
 *
 * 这是 Synergy 系统与 MoreMod 现有模块系统之间的桥梁。
 * 它通过调用 ItemMechanicalCore 和 ItemMechanicalCoreExtended 的公共 API
 * 来获取模块信息，而不修改现有系统的任何代码。
 *
 * 设计原则：
 * - 只读访问：不修改任何模块状态
 * - 零侵入：不需要修改现有类
 * - 防御性编程：处理所有可能的 null 和异常情况
 */
public class ExistingModuleBridge implements IModuleProvider {

    private static final ExistingModuleBridge INSTANCE = new ExistingModuleBridge();

    public static ExistingModuleBridge getInstance() {
        return INSTANCE;
    }

    private ExistingModuleBridge() {
        // 私有构造器，单例模式
    }

    /**
     * 获取玩家的机械核心
     *
     * 使用 ItemMechanicalCoreExtended.getCoreFromPlayer() 或
     * ItemMechanicalCore.getCoreFromPlayer() 来获取
     */
    @Override
    @Nullable
    public ItemStack getMechanicalCore(@Nonnull EntityPlayer player) {
        try {
            // 优先使用 Extended 的方法
            ItemStack core = ItemMechanicalCoreExtended.getCoreFromPlayer(player);
            if (core != null && !core.isEmpty()) {
                return core;
            }

            // 备选：使用主核心的方法
            core = ItemMechanicalCore.getCoreFromPlayer(player);
            return (core != null && !core.isEmpty()) ? core : null;
        } catch (Exception e) {
            System.err.println("[SynergyBridge] Error getting mechanical core: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取玩家所有已安装的模块视图
     */
    @Override
    @Nonnull
    public List<IInstalledModuleView> getInstalledModules(@Nonnull EntityPlayer player) {
        ItemStack core = getMechanicalCore(player);
        if (core == null || core.isEmpty()) {
            return new ArrayList<>();
        }
        return getInstalledModules(core);
    }

    /**
     * 获取指定机械核心中所有已安装的模块视图
     *
     * 使用 ItemMechanicalCoreExtended.getInstalledUpgradeIds() 获取模块列表，
     * 然后为每个模块创建 IInstalledModuleView。
     */
    @Override
    @Nonnull
    public List<IInstalledModuleView> getInstalledModules(@Nonnull ItemStack coreStack) {
        List<IInstalledModuleView> result = new ArrayList<>();

        if (coreStack == null || coreStack.isEmpty()) {
            return result;
        }

        try {
            // 使用 ItemMechanicalCoreExtended 获取已安装的模块 ID 列表
            List<String> installedIds = ItemMechanicalCoreExtended.getInstalledUpgradeIds(coreStack);

            for (String moduleId : installedIds) {
                IInstalledModuleView view = createModuleView(coreStack, moduleId);
                if (view != null) {
                    result.add(view);
                }
            }
        } catch (Exception e) {
            System.err.println("[SynergyBridge] Error getting installed modules: " + e.getMessage());
        }

        return result;
    }

    /**
     * 为指定模块创建视图
     *
     * 使用现有 API：
     * - ItemMechanicalCoreExtended.getUpgradeLevel()
     * - ItemMechanicalCoreExtended.isUpgradeActive()
     * - ItemMechanicalCoreExtended.getUpgradeInfo()
     */
    @Nullable
    private IInstalledModuleView createModuleView(ItemStack coreStack, String moduleId) {
        try {
            // 获取等级
            int level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, moduleId);

            // 获取激活状态
            boolean active = ItemMechanicalCoreExtended.isUpgradeActive(coreStack, moduleId);

            // 获取模块信息（显示名称、类别等）
            UpgradeInfo info = ItemMechanicalCoreExtended.getUpgradeInfo(moduleId);

            String displayName = moduleId;
            int maxLevel = 5;
            String category = "UNKNOWN";

            if (info != null) {
                displayName = info.displayName;
                maxLevel = info.maxLevel;
                category = categoryToString(info.category);
            }

            return InstalledModuleViewImpl.builder(moduleId.toUpperCase())
                    .level(level)
                    .active(active)
                    .displayName(displayName)
                    .maxLevel(maxLevel)
                    .category(category)
                    .build();

        } catch (Exception e) {
            System.err.println("[SynergyBridge] Error creating view for module " + moduleId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * 将 UpgradeCategory 转换为字符串
     */
    private String categoryToString(UpgradeCategory category) {
        if (category == null) return "UNKNOWN";
        return category.name();
    }

    // ==================== 辅助方法（供 Synergy 效果使用） ====================

    /**
     * 获取指定模块的等级（便捷方法）
     *
     * @param player 玩家
     * @param moduleId 模块ID
     * @return 模块等级，未安装返回 0
     */
    public int getModuleLevel(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        ItemStack core = getMechanicalCore(player);
        if (core == null || core.isEmpty()) {
            return 0;
        }
        return ItemMechanicalCoreExtended.getUpgradeLevel(core, moduleId);
    }

    /**
     * 检查模块是否激活（便捷方法）
     *
     * @param player 玩家
     * @param moduleId 模块ID
     * @return true 如果模块激活
     */
    public boolean isModuleActive(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        ItemStack core = getMechanicalCore(player);
        if (core == null || core.isEmpty()) {
            return false;
        }
        return ItemMechanicalCoreExtended.isUpgradeActive(core, moduleId);
    }

    /**
     * 消耗能量（供 Synergy 效果使用）
     *
     * @param player 玩家
     * @param amount 能量数量
     * @return true 如果成功消耗
     */
    public boolean consumeEnergy(@Nonnull EntityPlayer player, int amount) {
        ItemStack core = getMechanicalCore(player);
        if (core == null || core.isEmpty()) {
            return false;
        }
        try {
            return ItemMechanicalCore.consumeEnergy(core, amount);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 添加能量（供 Synergy 效果使用）
     *
     * @param player 玩家
     * @param amount 能量数量
     */
    public void addEnergy(@Nonnull EntityPlayer player, int amount) {
        ItemStack core = getMechanicalCore(player);
        if (core == null || core.isEmpty()) {
            return;
        }
        try {
            ItemMechanicalCore.addEnergy(core, amount);
        } catch (Exception e) {
            System.err.println("[SynergyBridge] Error adding energy: " + e.getMessage());
        }
    }

    /**
     * 获取当前能量
     *
     * @param player 玩家
     * @return 当前能量值
     */
    public int getCurrentEnergy(@Nonnull EntityPlayer player) {
        ItemStack core = getMechanicalCore(player);
        if (core == null || core.isEmpty()) {
            return 0;
        }
        try {
            IEnergyStorage storage = ItemMechanicalCore.getEnergyStorage(core);
            return storage != null ? storage.getEnergyStored() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取最大能量
     *
     * @param player 玩家
     * @return 最大能量值
     */
    public int getMaxEnergy(@Nonnull EntityPlayer player) {
        ItemStack core = getMechanicalCore(player);
        if (core == null || core.isEmpty()) {
            return 0;
        }
        try {
            IEnergyStorage storage = ItemMechanicalCore.getEnergyStorage(core);
            return storage != null ? storage.getMaxEnergyStored() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
