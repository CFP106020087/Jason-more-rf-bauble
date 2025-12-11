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
     * 计算能量超载加成
     * 当提供的能量超过所需能量时，每超过100%给予额外成功率加成
     * @param totalEnergy 所有基座的总能量
     * @return 超载加成 (0.0 - 0.5)，最多50%额外成功率
     */
    public float getOverloadBonus(int totalEnergy) {
        int requiredEnergy = energyPerPedestal * getPedestalCount();
        if (requiredEnergy <= 0) return 0;

        float energyRatio = (float) totalEnergy / requiredEnergy;
        if (energyRatio <= 1.0f) return 0; // 能量不足，无加成

        // 每超过100%能量，给予10%成功率加成，最多50%
        float overloadPercent = (energyRatio - 1.0f);
        return Math.min(0.5f, overloadPercent * 0.1f);
    }

    /**
     * 计算最终失败率（考虑祭坛加成 + 能量超载）
     */
    public float getOverloadAdjustedFailChance(AltarTier tier, int totalEnergy) {
        float tierAdjusted = getAdjustedFailChance(tier);
        float overloadBonus = getOverloadBonus(totalEnergy);
        return Math.max(0, tierAdjusted - overloadBonus);
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
