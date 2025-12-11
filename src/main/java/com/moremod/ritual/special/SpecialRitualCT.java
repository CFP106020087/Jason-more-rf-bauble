package com.moremod.ritual.special;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * 特殊仪式 CraftTweaker 集成
 *
 * 允许通过 CraftTweaker 脚本修改特殊仪式的参数
 *
 * 使用示例：
 *
 * // 修改灵魂束缚仪式的持续时间
 * mods.moremod.SpecialRitual.setDuration("soulbound", 600);
 *
 * // 修改失败率
 * mods.moremod.SpecialRitual.setFailChance("soulbound", 0.05);
 *
 * // 修改能量消耗
 * mods.moremod.SpecialRitual.setEnergy("soulbound", 100000);
 *
 * // 修改基座材料
 * mods.moremod.SpecialRitual.setPedestalItems("soulbound", [
 *     <minecraft:ender_pearl> * 2,
 *     <minecraft:diamond> * 6
 * ]);
 *
 * // 禁用仪式
 * mods.moremod.SpecialRitual.disable("unbreakable");
 *
 * // 启用仪式
 * mods.moremod.SpecialRitual.enable("unbreakable");
 *
 * // 打印所有已注册的仪式
 * mods.moremod.SpecialRitual.printAll();
 */
@ModOnly("crafttweaker")
@ZenClass("mods.moremod.SpecialRitual")
@ZenRegister
public class SpecialRitualCT {

    /**
     * 设置仪式的持续时间
     * @param ritualId 仪式ID (如 "soulbound", "unbreakable")
     * @param ticks 持续时间（tick，20tick=1秒）
     */
    @ZenMethod
    public static void setDuration(String ritualId, int ticks) {
        if (ticks <= 0) {
            CraftTweakerAPI.logError("Duration must be positive, got: " + ticks);
            return;
        }
        CraftTweakerAPI.apply(new SetDurationAction(ritualId, ticks));
    }

    /**
     * 设置仪式的失败率
     * @param ritualId 仪式ID
     * @param chance 失败率 (0.0-1.0, 如 0.1 = 10%)
     */
    @ZenMethod
    public static void setFailChance(String ritualId, float chance) {
        if (chance < 0 || chance > 1) {
            CraftTweakerAPI.logError("Fail chance must be between 0.0 and 1.0, got: " + chance);
            return;
        }
        CraftTweakerAPI.apply(new SetFailChanceAction(ritualId, chance));
    }

    /**
     * 设置仪式的能量消耗（每基座）
     * @param ritualId 仪式ID
     * @param energy 能量值（RF）
     */
    @ZenMethod
    public static void setEnergy(String ritualId, int energy) {
        if (energy < 0) {
            CraftTweakerAPI.logError("Energy must be non-negative, got: " + energy);
            return;
        }
        CraftTweakerAPI.apply(new SetEnergyAction(ritualId, energy));
    }

    /**
     * 设置仪式的基座材料
     * @param ritualId 仪式ID
     * @param items 基座物品数组（1-8个）
     */
    @ZenMethod
    public static void setPedestalItems(String ritualId, IItemStack[] items) {
        if (items == null || items.length == 0) {
            CraftTweakerAPI.logError("Pedestal items cannot be empty");
            return;
        }
        if (items.length > 8) {
            CraftTweakerAPI.logError("Maximum 8 pedestal items allowed, got: " + items.length);
            return;
        }
        CraftTweakerAPI.apply(new SetPedestalItemsAction(ritualId, items));
    }

    /**
     * 禁用指定仪式
     * @param ritualId 仪式ID
     */
    @ZenMethod
    public static void disable(String ritualId) {
        CraftTweakerAPI.apply(new DisableRitualAction(ritualId));
    }

    /**
     * 启用指定仪式
     * @param ritualId 仪式ID
     */
    @ZenMethod
    public static void enable(String ritualId) {
        CraftTweakerAPI.apply(new EnableRitualAction(ritualId));
    }

    /**
     * 清除仪式的所有自定义覆盖，恢复默认值
     * @param ritualId 仪式ID
     */
    @ZenMethod
    public static void resetToDefault(String ritualId) {
        CraftTweakerAPI.apply(new ResetRitualAction(ritualId));
    }

    /**
     * 打印所有已注册的仪式（调试用）
     */
    @ZenMethod
    public static void printAll() {
        CraftTweakerAPI.logInfo("=== Special Rituals ===");
        for (ISpecialRitual ritual : SpecialRitualRegistry.getAll()) {
            String status = SpecialRitualRegistry.isEnabled(ritual.getId()) ? "ENABLED" : "DISABLED";
            CraftTweakerAPI.logInfo(String.format("[%s] %s (%s) - Tier %d, %d ticks, %.0f%% fail",
                    status, ritual.getId(), ritual.getDisplayName(),
                    ritual.getRequiredTier(),
                    SpecialRitualRegistry.getEffectiveDuration(ritual),
                    SpecialRitualRegistry.getEffectiveFailChance(ritual) * 100));
        }
        CraftTweakerAPI.logInfo("======================");
    }

    // ==================== Action 类 ====================

    private static class SetDurationAction implements IAction {
        private final String ritualId;
        private final int duration;

        public SetDurationAction(String ritualId, int duration) {
            this.ritualId = ritualId;
            this.duration = duration;
        }

        @Override
        public void apply() {
            if (!SpecialRitualRegistry.exists(ritualId)) {
                CraftTweakerAPI.logError("Unknown ritual: " + ritualId);
                return;
            }
            SpecialRitualRegistry.RitualOverrides overrides = SpecialRitualRegistry.getOverride(ritualId);
            if (overrides == null) {
                overrides = new SpecialRitualRegistry.RitualOverrides();
            }
            overrides.setDuration(duration);
            SpecialRitualRegistry.setOverride(ritualId, overrides);
        }

        @Override
        public String describe() {
            return "Setting duration of " + ritualId + " to " + duration + " ticks";
        }
    }

    private static class SetFailChanceAction implements IAction {
        private final String ritualId;
        private final float chance;

        public SetFailChanceAction(String ritualId, float chance) {
            this.ritualId = ritualId;
            this.chance = chance;
        }

        @Override
        public void apply() {
            if (!SpecialRitualRegistry.exists(ritualId)) {
                CraftTweakerAPI.logError("Unknown ritual: " + ritualId);
                return;
            }
            SpecialRitualRegistry.RitualOverrides overrides = SpecialRitualRegistry.getOverride(ritualId);
            if (overrides == null) {
                overrides = new SpecialRitualRegistry.RitualOverrides();
            }
            overrides.setFailChance(chance);
            SpecialRitualRegistry.setOverride(ritualId, overrides);
        }

        @Override
        public String describe() {
            return "Setting fail chance of " + ritualId + " to " + (chance * 100) + "%";
        }
    }

    private static class SetEnergyAction implements IAction {
        private final String ritualId;
        private final int energy;

        public SetEnergyAction(String ritualId, int energy) {
            this.ritualId = ritualId;
            this.energy = energy;
        }

        @Override
        public void apply() {
            if (!SpecialRitualRegistry.exists(ritualId)) {
                CraftTweakerAPI.logError("Unknown ritual: " + ritualId);
                return;
            }
            SpecialRitualRegistry.RitualOverrides overrides = SpecialRitualRegistry.getOverride(ritualId);
            if (overrides == null) {
                overrides = new SpecialRitualRegistry.RitualOverrides();
            }
            overrides.setEnergyPerPedestal(energy);
            SpecialRitualRegistry.setOverride(ritualId, overrides);
        }

        @Override
        public String describe() {
            return "Setting energy of " + ritualId + " to " + energy + " RF/pedestal";
        }
    }

    private static class SetPedestalItemsAction implements IAction {
        private final String ritualId;
        private final IItemStack[] items;

        public SetPedestalItemsAction(String ritualId, IItemStack[] items) {
            this.ritualId = ritualId;
            this.items = items;
        }

        @Override
        public void apply() {
            if (!SpecialRitualRegistry.exists(ritualId)) {
                CraftTweakerAPI.logError("Unknown ritual: " + ritualId);
                return;
            }
            List<ItemStack> mcItems = new ArrayList<>();
            for (IItemStack item : items) {
                if (item != null) {
                    mcItems.add(CraftTweakerMC.getItemStack(item));
                }
            }
            SpecialRitualRegistry.RitualOverrides overrides = SpecialRitualRegistry.getOverride(ritualId);
            if (overrides == null) {
                overrides = new SpecialRitualRegistry.RitualOverrides();
            }
            overrides.setPedestalItems(mcItems);
            SpecialRitualRegistry.setOverride(ritualId, overrides);
        }

        @Override
        public String describe() {
            return "Setting pedestal items of " + ritualId;
        }
    }

    private static class DisableRitualAction implements IAction {
        private final String ritualId;

        public DisableRitualAction(String ritualId) {
            this.ritualId = ritualId;
        }

        @Override
        public void apply() {
            if (!SpecialRitualRegistry.exists(ritualId)) {
                CraftTweakerAPI.logWarning("Ritual not found: " + ritualId);
            }
            SpecialRitualRegistry.disable(ritualId);
        }

        @Override
        public String describe() {
            return "Disabling ritual: " + ritualId;
        }
    }

    private static class EnableRitualAction implements IAction {
        private final String ritualId;

        public EnableRitualAction(String ritualId) {
            this.ritualId = ritualId;
        }

        @Override
        public void apply() {
            SpecialRitualRegistry.enable(ritualId);
        }

        @Override
        public String describe() {
            return "Enabling ritual: " + ritualId;
        }
    }

    private static class ResetRitualAction implements IAction {
        private final String ritualId;

        public ResetRitualAction(String ritualId) {
            this.ritualId = ritualId;
        }

        @Override
        public void apply() {
            SpecialRitualRegistry.clearOverride(ritualId);
        }

        @Override
        public String describe() {
            return "Resetting ritual to default: " + ritualId;
        }
    }
}
