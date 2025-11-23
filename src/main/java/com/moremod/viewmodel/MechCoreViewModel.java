package com.moremod.viewmodel;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.IMechCoreModule;
import com.moremod.capability.module.ModuleContainer;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.upgrades.ModuleRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Mechanical Core ViewModel
 *
 * 职责：
 *  - 从 Capability 读取数据
 *  - 提供格式化的数据给 GUI
 *  - 分离业务逻辑与 GUI 逻辑
 *
 * 设计模式：MVVM（Model-View-ViewModel）
 */
public class MechCoreViewModel {

    private final EntityPlayer player;
    private final IMechCoreData data;

    public MechCoreViewModel(EntityPlayer player) {
        this.player = player;
        this.data = player.getCapability(IMechCoreData.CAPABILITY, null);

        if (data == null) {
            throw new IllegalStateException("Player does not have MechCoreData capability");
        }
    }

    // ────────────────────────────────────────────────────────────
    // 能量系统
    // ────────────────────────────────────────────────────────────

    public int getEnergy() {
        return data.getEnergy();
    }

    public int getMaxEnergy() {
        return data.getMaxEnergy();
    }

    public float getEnergyPercentage() {
        if (data.getMaxEnergy() == 0) return 0;
        return (float) data.getEnergy() / data.getMaxEnergy();
    }

    public String getEnergyText() {
        return formatEnergy(data.getEnergy()) + " / " + formatEnergy(data.getMaxEnergy());
    }

    public String getEnergyPercentageText() {
        return String.format("%.1f%%", getEnergyPercentage() * 100);
    }

    public TextFormatting getEnergyColor() {
        float percentage = getEnergyPercentage();
        if (percentage >= 0.7f) return TextFormatting.GREEN;
        if (percentage >= 0.3f) return TextFormatting.YELLOW;
        if (percentage >= 0.1f) return TextFormatting.RED;
        return TextFormatting.DARK_RED;
    }

    // ────────────────────────────────────────────────────────────
    // 模块系统
    // ────────────────────────────────────────────────────────────

    public List<ModuleInfo> getAllModules() {
        List<ModuleInfo> modules = new ArrayList<>();
        ModuleContainer container = data.getModuleContainer();

        for (String moduleId : container.getAllModules()) {
            modules.add(new ModuleInfo(
                moduleId,
                container.getLevel(moduleId),
                getModuleMaxLevel(moduleId),
                container.isActive(moduleId)
            ));
        }

        return modules;
    }

    public List<ModuleInfo> getActiveModules() {
        List<ModuleInfo> modules = new ArrayList<>();
        ModuleContainer container = data.getModuleContainer();

        for (String moduleId : container.getActiveModules()) {
            modules.add(new ModuleInfo(
                moduleId,
                container.getLevel(moduleId),
                getModuleMaxLevel(moduleId),
                true
            ));
        }

        return modules;
    }

    public ModuleInfo getModule(String moduleId) {
        ModuleContainer container = data.getModuleContainer();

        return new ModuleInfo(
            moduleId,
            container.getLevel(moduleId),
            getModuleMaxLevel(moduleId),
            container.isActive(moduleId)
        );
    }

    // ────────────────────────────────────────────────────────────
    // 模块信息类
    // ────────────────────────────────────────────────────────────

    public static class ModuleInfo {
        private final String id;
        private final int level;
        private final int maxLevel;
        private final boolean active;

        public ModuleInfo(String id, int level, int maxLevel, boolean active) {
            this.id = id;
            this.level = level;
            this.maxLevel = maxLevel;
            this.active = active;
        }

        public String getId() {
            return id;
        }

        public int getLevel() {
            return level;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        public boolean isActive() {
            return active;
        }

        public String getDisplayName() {
            try {
                ItemMechanicalCoreExtended.UpgradeInfo info =
                    ItemMechanicalCoreExtended.getUpgradeInfo(id);
                if (info != null && info.displayName != null) {
                    return info.displayName;
                }
            } catch (Exception ignored) {}

            // Fallback: 格式化 ID
            return id.replace('_', ' ').toLowerCase();
        }

        public TextFormatting getColor() {
            if (!active) return TextFormatting.GRAY;
            if (level >= maxLevel) return TextFormatting.GOLD;
            if (level > 0) return TextFormatting.GREEN;
            return TextFormatting.GRAY;
        }

        public String getLevelText() {
            return "Lv." + level + "/" + maxLevel;
        }

        public String getStatusText() {
            if (!active) return "已停用";
            if (level == 0) return "未激活";
            return "运行中";
        }

        public TextFormatting getStatusColor() {
            if (!active) return TextFormatting.RED;
            if (level == 0) return TextFormatting.GRAY;
            return TextFormatting.GREEN;
        }
    }

    // ────────────────────────────────────────────────────────────
    // 辅助方法
    // ────────────────────────────────────────────────────────────

    /**
     * 获取模块的最大等级
     */
    private int getModuleMaxLevel(String moduleId) {
        IMechCoreModule module = ModuleRegistry.getNew(moduleId);
        if (module != null) {
            return module.getMaxLevel();
        }
        // 默认最大等级为 5
        return 5;
    }

    private String formatEnergy(int energy) {
        if (energy >= 1_000_000) {
            return String.format("%.1fM", energy / 1_000_000.0);
        } else if (energy >= 1_000) {
            return String.format("%.1fk", energy / 1_000.0);
        } else {
            return String.valueOf(energy);
        }
    }

    // ────────────────────────────────────────────────────────────
    // 数据访问
    // ────────────────────────────────────────────────────────────

    public IMechCoreData getData() {
        return data;
    }

    public EntityPlayer getPlayer() {
        return player;
    }
}
