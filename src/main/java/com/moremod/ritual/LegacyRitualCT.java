package com.moremod.ritual;

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
 * 旧仪式系统 CraftTweaker 接口
 *
 * 允许修改 TileEntityRitualCore 中硬编码仪式的参数
 *
 * 仪式ID列表：
 * - curse_purification  (诅咒净化)
 * - enchant_transfer    (附魔转移)
 * - enchant_infusion    (注魔仪式)
 * - curse_creation      (诅咒创造)
 * - weapon_exp_boost    (武器经验加速)
 * - muramasa_boost      (村正攻击提升)
 * - fabric_enhance      (织印强化)
 * - soul_binding        (灵魂绑定-创建假玩家核心)
 * - duplication         (禁忌复制)
 * - embedding           (七圣遗物嵌入)
 * - unbreakable         (不可破坏)
 * - soulbound           (灵魂束缚-死亡不掉落)
 *
 * 使用示例：
 *
 * // 修改诅咒净化的持续时间（tick）
 * mods.moremod.LegacyRitual.setDuration("curse_purification", 400);
 *
 * // 修改注魔仪式的失败率（0.0-1.0）
 * mods.moremod.LegacyRitual.setFailChance("enchant_infusion", 0.5);
 *
 * // 修改不可破坏仪式的能量消耗
 * mods.moremod.LegacyRitual.setEnergy("unbreakable", 500000);
 *
 * // 修改仪式所需阶层（1-3）
 * mods.moremod.LegacyRitual.setTier("curse_purification", 1);
 *
 * // ========== 自定义仪式材料 ==========
 *
 * // 修改不可破坏仪式的材料（替换默认的下界之星×2+黑曜石×2+钻石×4）
 * mods.moremod.LegacyRitual.setPedestalItems("unbreakable", [
 *     <minecraft:nether_star>,
 *     <minecraft:nether_star>,
 *     <minecraft:diamond> * 6
 * ]);
 *
 * // 修改灵魂束缚仪式的材料（替换默认的末影珍珠×4+恶魂之泪×2+金块×2）
 * mods.moremod.LegacyRitual.setPedestalItems("soulbound", [
 *     <minecraft:ender_pearl> * 4,
 *     <minecraft:ghast_tear> * 2,
 *     <minecraft:emerald_block> * 2
 * ]);
 *
 * // 修改注魔仪式的材料（默认需要3本附魔书）
 * mods.moremod.LegacyRitual.setPedestalItems("enchant_infusion", [
 *     <minecraft:enchanted_book> * 5
 * ]);
 *
 * // 修改诅咒创造仪式的材料（默认需要墨囊+腐肉/蜘蛛眼）
 * mods.moremod.LegacyRitual.setPedestalItems("curse_creation", [
 *     <minecraft:dye:0>,
 *     <minecraft:bone> * 2
 * ]);
 *
 * // 修改武器经验加速仪式的材料（默认需要经验瓶/附魔书/绿宝石）
 * mods.moremod.LegacyRitual.setPedestalItems("weapon_exp_boost", [
 *     <minecraft:experience_bottle> * 4
 * ]);
 *
 * // 修改村正攻击提升仪式的材料（默认需要凋零骷髅头/烈焰粉/下界之星）
 * mods.moremod.LegacyRitual.setPedestalItems("muramasa_boost", [
 *     <minecraft:blaze_powder> * 8
 * ]);
 *
 * // 修改诅咒净化仪式的材料（默认需要金苹果/圣水）
 * mods.moremod.LegacyRitual.setPedestalItems("curse_purification", [
 *     <minecraft:golden_apple> * 2
 * ]);
 *
 * // 修改附魔转移仪式的材料（默认需要青金石/龙息）
 * mods.moremod.LegacyRitual.setPedestalItems("enchant_transfer", [
 *     <minecraft:diamond> * 4
 * ]);
 *
 * // 修改织印强化仪式的材料（默认需要龙息/末影之眼/下界之星等）
 * mods.moremod.LegacyRitual.setPedestalItems("fabric_enhance", [
 *     <minecraft:ender_eye> * 4
 * ]);
 *
 * // ========== 删除/清除原有配方 ==========
 *
 * // 清除不可破坏仪式的默认材料（完全移除原有配方）
 * mods.moremod.LegacyRitual.clearMaterials("unbreakable");
 * // 然后设置新的自定义材料
 * mods.moremod.LegacyRitual.setPedestalItems("unbreakable", [
 *     <minecraft:gold_block> * 4,
 *     <minecraft:diamond> * 8
 * ]);
 *
 * // 禁用某个仪式
 * mods.moremod.LegacyRitual.disable("duplication");
 *
 * // 启用某个仪式
 * mods.moremod.LegacyRitual.enable("duplication");
 *
 * // 重置仪式到默认值（包括材料）
 * mods.moremod.LegacyRitual.reset("enchant_infusion");
 *
 * // 打印所有仪式配置
 * mods.moremod.LegacyRitual.printAll();
 */
@ModOnly("crafttweaker")
@ZenClass("mods.moremod.LegacyRitual")
@ZenRegister
public class LegacyRitualCT {

    /**
     * 设置仪式持续时间
     * @param ritualId 仪式ID
     * @param ticks 持续时间（tick，20tick=1秒）
     */
    @ZenMethod
    public static void setDuration(String ritualId, int ticks) {
        if (ticks <= 0) {
            CraftTweakerAPI.logError("[LegacyRitual] Duration must be positive: " + ticks);
            return;
        }
        CraftTweakerAPI.apply(new SetDurationAction(ritualId, ticks));
    }

    /**
     * 设置仪式失败率
     * @param ritualId 仪式ID
     * @param chance 失败率（0.0-1.0，如0.1表示10%）
     */
    @ZenMethod
    public static void setFailChance(String ritualId, float chance) {
        if (chance < 0 || chance > 1) {
            CraftTweakerAPI.logError("[LegacyRitual] Fail chance must be 0.0-1.0: " + chance);
            return;
        }
        CraftTweakerAPI.apply(new SetFailChanceAction(ritualId, chance));
    }

    /**
     * 设置仪式能量消耗（每基座）
     * @param ritualId 仪式ID
     * @param energy RF能量值
     */
    @ZenMethod
    public static void setEnergy(String ritualId, int energy) {
        if (energy < 0) {
            CraftTweakerAPI.logError("[LegacyRitual] Energy must be non-negative: " + energy);
            return;
        }
        CraftTweakerAPI.apply(new SetEnergyAction(ritualId, energy));
    }

    /**
     * 设置仪式所需祭坛阶层
     * @param ritualId 仪式ID
     * @param tier 阶层（1=基础, 2=进阶, 3=大师）
     */
    @ZenMethod
    public static void setTier(String ritualId, int tier) {
        if (tier < 1 || tier > 3) {
            CraftTweakerAPI.logError("[LegacyRitual] Tier must be 1-3: " + tier);
            return;
        }
        CraftTweakerAPI.apply(new SetTierAction(ritualId, tier));
    }

    /**
     * 设置仪式基座材料
     * @param ritualId 仪式ID
     * @param items 材料数组（1-8个）
     */
    @ZenMethod
    public static void setPedestalItems(String ritualId, IItemStack[] items) {
        if (items == null || items.length == 0 || items.length > 8) {
            CraftTweakerAPI.logError("[LegacyRitual] Pedestal items must be 1-8 items");
            return;
        }
        CraftTweakerAPI.apply(new SetPedestalItemsAction(ritualId, items));
    }

    /**
     * 清除仪式的默认材料配置
     * 清除后，仪式将不检查材料（除非使用 setPedestalItems 设置新材料）
     * 这允许你完全移除原有配方，然后定义自己的新配方
     *
     * 使用示例：
     * // 先清除不可破坏仪式的默认材料
     * mods.moremod.LegacyRitual.clearMaterials("unbreakable");
     * // 然后设置新材料
     * mods.moremod.LegacyRitual.setPedestalItems("unbreakable", [
     *     <minecraft:gold_block> * 4
     * ]);
     *
     * @param ritualId 仪式ID
     */
    @ZenMethod
    public static void clearMaterials(String ritualId) {
        CraftTweakerAPI.apply(new ClearMaterialsAction(ritualId));
    }

    /**
     * 禁用仪式
     * @param ritualId 仪式ID
     */
    @ZenMethod
    public static void disable(String ritualId) {
        CraftTweakerAPI.apply(new DisableAction(ritualId));
    }

    /**
     * 启用仪式
     * @param ritualId 仪式ID
     */
    @ZenMethod
    public static void enable(String ritualId) {
        CraftTweakerAPI.apply(new EnableAction(ritualId));
    }

    /**
     * 重置仪式到默认值
     * @param ritualId 仪式ID
     */
    @ZenMethod
    public static void reset(String ritualId) {
        CraftTweakerAPI.apply(new ResetAction(ritualId));
    }

    /**
     * 重置所有仪式到默认值
     */
    @ZenMethod
    public static void resetAll() {
        CraftTweakerAPI.apply(new ResetAllAction());
    }

    /**
     * 打印所有仪式配置（调试用）
     */
    @ZenMethod
    public static void printAll() {
        CraftTweakerAPI.logInfo("=== Legacy Ritual Configuration ===");
        for (String id : LegacyRitualConfig.getAllRitualIds()) {
            String status = LegacyRitualConfig.isEnabled(id) ? "ON" : "OFF";
            CraftTweakerAPI.logInfo(String.format("[%s] %s: %d ticks, %.0f%% fail, %d RF, Tier %d",
                    status, id,
                    LegacyRitualConfig.getDuration(id),
                    LegacyRitualConfig.getFailChance(id) * 100,
                    LegacyRitualConfig.getEnergyPerPedestal(id),
                    LegacyRitualConfig.getRequiredTier(id)));
        }
        CraftTweakerAPI.logInfo("===================================");
    }

    // ==================== Action 实现 ====================

    private static class SetDurationAction implements IAction {
        private final String ritualId;
        private final int duration;

        SetDurationAction(String ritualId, int duration) {
            this.ritualId = ritualId;
            this.duration = duration;
        }

        @Override
        public void apply() {
            LegacyRitualConfig.setDuration(ritualId, duration);
        }

        @Override
        public String describe() {
            return "[LegacyRitual] Set " + ritualId + " duration to " + duration + " ticks";
        }
    }

    private static class SetFailChanceAction implements IAction {
        private final String ritualId;
        private final float chance;

        SetFailChanceAction(String ritualId, float chance) {
            this.ritualId = ritualId;
            this.chance = chance;
        }

        @Override
        public void apply() {
            LegacyRitualConfig.setFailChance(ritualId, chance);
        }

        @Override
        public String describe() {
            return "[LegacyRitual] Set " + ritualId + " fail chance to " + (chance * 100) + "%";
        }
    }

    private static class SetEnergyAction implements IAction {
        private final String ritualId;
        private final int energy;

        SetEnergyAction(String ritualId, int energy) {
            this.ritualId = ritualId;
            this.energy = energy;
        }

        @Override
        public void apply() {
            LegacyRitualConfig.setEnergyPerPedestal(ritualId, energy);
        }

        @Override
        public String describe() {
            return "[LegacyRitual] Set " + ritualId + " energy to " + energy + " RF";
        }
    }

    private static class SetTierAction implements IAction {
        private final String ritualId;
        private final int tier;

        SetTierAction(String ritualId, int tier) {
            this.ritualId = ritualId;
            this.tier = tier;
        }

        @Override
        public void apply() {
            LegacyRitualConfig.setRequiredTier(ritualId, tier);
        }

        @Override
        public String describe() {
            return "[LegacyRitual] Set " + ritualId + " required tier to " + tier;
        }
    }

    private static class SetPedestalItemsAction implements IAction {
        private final String ritualId;
        private final IItemStack[] items;

        SetPedestalItemsAction(String ritualId, IItemStack[] items) {
            this.ritualId = ritualId;
            this.items = items;
        }

        @Override
        public void apply() {
            List<ItemStack> mcItems = new ArrayList<>();
            for (IItemStack item : items) {
                if (item != null) {
                    mcItems.add(CraftTweakerMC.getItemStack(item));
                }
            }
            LegacyRitualConfig.setPedestalItems(ritualId, mcItems);
        }

        @Override
        public String describe() {
            return "[LegacyRitual] Set " + ritualId + " pedestal items";
        }
    }

    private static class ClearMaterialsAction implements IAction {
        private final String ritualId;

        ClearMaterialsAction(String ritualId) {
            this.ritualId = ritualId;
        }

        @Override
        public void apply() {
            LegacyRitualConfig.clearMaterials(ritualId);
        }

        @Override
        public String describe() {
            return "[LegacyRitual] Clear " + ritualId + " materials (removing default recipe)";
        }
    }

    private static class DisableAction implements IAction {
        private final String ritualId;

        DisableAction(String ritualId) {
            this.ritualId = ritualId;
        }

        @Override
        public void apply() {
            LegacyRitualConfig.disable(ritualId);
        }

        @Override
        public String describe() {
            return "[LegacyRitual] Disable " + ritualId;
        }
    }

    private static class EnableAction implements IAction {
        private final String ritualId;

        EnableAction(String ritualId) {
            this.ritualId = ritualId;
        }

        @Override
        public void apply() {
            LegacyRitualConfig.enable(ritualId);
        }

        @Override
        public String describe() {
            return "[LegacyRitual] Enable " + ritualId;
        }
    }

    private static class ResetAction implements IAction {
        private final String ritualId;

        ResetAction(String ritualId) {
            this.ritualId = ritualId;
        }

        @Override
        public void apply() {
            LegacyRitualConfig.reset(ritualId);
        }

        @Override
        public String describe() {
            return "[LegacyRitual] Reset " + ritualId + " to default";
        }
    }

    private static class ResetAllAction implements IAction {
        @Override
        public void apply() {
            LegacyRitualConfig.resetAll();
        }

        @Override
        public String describe() {
            return "[LegacyRitual] Reset all rituals to default";
        }
    }
}
