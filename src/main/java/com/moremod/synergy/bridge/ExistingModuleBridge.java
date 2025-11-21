package com.moremod.synergy.bridge;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.synergy.api.IInstalledModuleView;
import com.moremod.synergy.api.IModuleProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 现有模块系统的适配桥接器
 *
 * 说明：
 * - 将现有的 ItemMechanicalCore 模块数据转换为 Synergy 系统可识别的 IInstalledModuleView
 * - 这是 Synergy 系统与现有模块系统之间的**唯一**桥接点
 * - 不修改任何现有逻辑，只读取数据
 *
 * 重要：
 * - 如果未来要移除 Synergy 包，只需删除此类在主 mod 初始化中的调用即可
 * - 此类不依赖 Synergy 的其他类，只实现接口
 */
public class ExistingModuleBridge implements IModuleProvider {

    private static final ExistingModuleBridge INSTANCE = new ExistingModuleBridge();

    private ExistingModuleBridge() {
        // 私有构造器
    }

    public static ExistingModuleBridge getInstance() {
        return INSTANCE;
    }

    @Override
    public List<IInstalledModuleView> getInstalledModules(EntityPlayer player) {
        if (player == null) {
            return Collections.emptyList();
        }

        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) {
            return Collections.emptyList();
        }

        return getInstalledModules(coreStack);
    }

    @Override
    public List<IInstalledModuleView> getInstalledModules(ItemStack coreStack) {
        if (coreStack.isEmpty()) {
            return Collections.emptyList();
        }

        List<IInstalledModuleView> modules = new ArrayList<>();

        // 从 ItemMechanicalCore 获取所有已安装的模块 ID
        // 注意：这里使用 ItemMechanicalCore 的公共 API，不访问内部细节
        List<String> installedIds = ItemMechanicalCore.getInstalledUpgradeIds(coreStack);

        for (String moduleId : installedIds) {
            int level = ItemMechanicalCore.getEffectiveUpgradeLevel(coreStack, moduleId);

            if (level > 0) {
                // 创建模块视图
                modules.add(new InstalledModuleView(coreStack, moduleId, level));
            }
        }

        return modules;
    }

    @Override
    public boolean hasActiveModule(EntityPlayer player, String moduleId) {
        if (player == null || moduleId == null) {
            return false;
        }

        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) {
            return false;
        }

        return ItemMechanicalCore.isUpgradeActive(coreStack, moduleId);
    }

    @Override
    public int getModuleLevel(EntityPlayer player, String moduleId) {
        if (player == null || moduleId == null) {
            return 0;
        }

        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) {
            return 0;
        }

        return ItemMechanicalCore.getEffectiveUpgradeLevel(coreStack, moduleId);
    }

    /**
     * 已安装模块视图实现
     *
     * 说明：
     * - 简单的 DTO，只存储快照数据
     * - 不保留对 ItemStack 的引用，避免内存泄漏
     */
    private static class InstalledModuleView implements IInstalledModuleView {

        private final String moduleId;
        private final int level;
        private final boolean active;
        private final String displayName;

        public InstalledModuleView(ItemStack coreStack, String moduleId, int level) {
            this.moduleId = moduleId;
            this.level = level;
            this.active = ItemMechanicalCore.isUpgradeActive(coreStack, moduleId);

            // 尝试获取显示名称（如果可能）
            // 这里只使用 moduleId，未来可以扩展为从 ModuleRegistry 获取
            this.displayName = formatDisplayName(moduleId);
        }

        @Override
        public String getModuleId() {
            return moduleId;
        }

        @Override
        public int getLevel() {
            return level;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        /**
         * 简单的显示名称格式化
         *
         * @param moduleId 模块 ID
         * @return 格式化后的显示名称
         */
        private static String formatDisplayName(String moduleId) {
            if (moduleId == null) return "Unknown";

            // 将 MAGIC_ABSORB 转换为 Magic Absorb
            String[] parts = moduleId.split("_");
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append(" ");
                String part = parts[i].toLowerCase();
                if (part.length() > 0) {
                    sb.append(Character.toUpperCase(part.charAt(0)));
                    if (part.length() > 1) {
                        sb.append(part.substring(1));
                    }
                }
            }

            return sb.toString();
        }

        @Override
        public String toString() {
            return String.format("Module[%s Lv.%d %s]",
                    moduleId, level, active ? "Active" : "Inactive");
        }
    }
}
