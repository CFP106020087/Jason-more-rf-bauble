package com.moremod.ritual;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import java.util.ArrayList;
import java.util.List;

public class RitualInfusionRecipe {
    public Ingredient core;
    public ItemStack output;
    public int time;
    public int energyPerPedestal;
    public float failChance;
    public List<Ingredient> pedestalItems;
    public int requiredTier;  // 所需祭坛阶层 (1-3)

    // 原有构造器（默认一阶）
    public RitualInfusionRecipe(Ingredient core, List<Ingredient> pedestalItems, ItemStack output,
                                int time, int energyPerPedestal, float failChance) {
        this(core, pedestalItems, output, time, energyPerPedestal, failChance, 1);
    }

    // 新构造器（指定阶层）
    public RitualInfusionRecipe(Ingredient core, List<Ingredient> pedestalItems, ItemStack output,
                                int time, int energyPerPedestal, float failChance, int requiredTier) {
        this.core = core;
        this.pedestalItems = pedestalItems;
        this.output = output;
        this.time = time;
        this.energyPerPedestal = energyPerPedestal;
        this.failChance = failChance;
        this.requiredTier = Math.max(1, Math.min(3, requiredTier)); // 限制在1-3
    }

    public boolean matchPedestalStacks(List<ItemStack> stacks) {
        if (stacks.size() < pedestalItems.size()) return false;

        List<ItemStack> available = new ArrayList<>(stacks);
        for (Ingredient required : pedestalItems) {
            boolean found = false;
            for (int i = 0; i < available.size(); i++) {
                if (required.apply(available.get(i))) {
                    available.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    /**
     * 检查祭坛阶层是否满足配方要求
     */
    public boolean canCraftAtTier(AltarTier tier) {
        return tier.getLevel() >= this.requiredTier;
    }

    /**
     * 计算实际失败率（考虑祭坛加成）
     */
    public float getAdjustedFailChance(AltarTier tier) {
        float adjusted = failChance - tier.getSuccessBonus();
        return Math.max(0, adjusted); // 不能为负
    }

    /**
     * 计算能量超载加成的失败率
     * @param tier 祭坛阶层
     * @param totalEnergyAvailable 所有基座的总可用能量
     * @return 调整后的失败率（考虑祭坛和超载加成）
     */
    public float getOverloadAdjustedFailChance(AltarTier tier, int totalEnergyAvailable) {
        float baseAdjusted = getAdjustedFailChance(tier);

        // 计算配方需要的总能量
        int requiredEnergy = energyPerPedestal * getPedestalCount();
        if (requiredEnergy <= 0) return baseAdjusted;

        // 计算超载比例 (超出100%的部分)
        float overloadRatio = (float)(totalEnergyAvailable - requiredEnergy) / requiredEnergy;
        if (overloadRatio <= 0) return baseAdjusted;

        // 超载加成：每超载100%能量（2倍需求）降低10%失败率
        // 最大超载300%（4倍能量）可降低30%失败率
        float maxOverloadBonus = 0.30f; // 最大30%加成
        float overloadBonus = Math.min(overloadRatio * 0.10f, maxOverloadBonus);

        return Math.max(0, baseAdjusted - overloadBonus);
    }

    /**
     * 计算超载提供的成功率加成百分比
     * @param totalEnergyAvailable 所有基座的总可用能量
     * @return 超载提供的成功率加成 (0.0 - 0.30)
     */
    public float getOverloadBonus(int totalEnergyAvailable) {
        int requiredEnergy = energyPerPedestal * getPedestalCount();
        if (requiredEnergy <= 0) return 0;

        float overloadRatio = (float)(totalEnergyAvailable - requiredEnergy) / requiredEnergy;
        if (overloadRatio <= 0) return 0;

        // 最大30%加成
        return Math.min(overloadRatio * 0.10f, 0.30f);
    }

    public Ingredient getCore() { return core; }
    public ItemStack getOutput() { return output.copy(); }
    public int getTime() { return time; }
    public int getEnergyPerPedestal() { return energyPerPedestal; }
    public float getFailChance() { return failChance; }
    public int getPedestalCount() { return pedestalItems.size(); }
    public List<Ingredient> getPedestalItems() { return pedestalItems; }
    public int getRequiredTier() { return requiredTier; }
}
