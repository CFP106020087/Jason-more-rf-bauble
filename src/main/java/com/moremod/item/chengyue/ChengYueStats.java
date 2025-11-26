package com.moremod.item.chengyue;

import com.moremod.capability.ChengYueCapability;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 澄月 - 属性计算（攻击平衡版 + 生存最小修改版）
 *
 * 攻击向：
 *  - 使用你刚刚说「不错」的那套平衡公式（白值 + 月相 + 形态压缩）
 *
 * 生存向：
 *  - 没再额外削弱，使用你贴出来那版：
 *      吸血 ≤ 30%
 *      减伤 ≤ 50%
 *      闪避 ≤ 30%
 */
public class ChengYueStats {

    /**
     * 显示用预估伤害（不含连击）
     * 👉 保持你刚刚说 OK 的那套公式
     */
    public static double getDamage(ItemStack stack, EntityPlayer player) {
        ChengYueNBT.init(stack);

        int level = ChengYueNBT.getLevel(stack);
        int stage = ChengYueNBT.getStage(stack);

        // 白值（高阶神器，但不是核弹）
        double base = 7.0; // 接近钻石剑
        double levelBonus = 2.5 * Math.log(level + 1) / Math.log(2); // 等级缓慢成长
        double stageBonus = (stage - 1) * 3.0; // 阶位提供 0 / 3 / 6

        double total = base + levelBonus + stageBonus;

        World world = (player != null) ? player.world : null;
        if (world != null) {
            // 当前月相 & 记忆：先各算，再取较强的一个
            float phaseMult = ChengYueMoonPhase.getDamageMultiplier(world);
            float memoryMult = ChengYueMoonMemory.getDamageMultiplierWithMemory(stack, world);
            float moonMult = Math.max(phaseMult, memoryMult);

            // 形态倍率
            float formMult = 1.0f;
            ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);
            if (cap != null && ChengYueFormManager.isUnlocked(stack)) {
                int formIndex = cap.getCurrentForm();
                formMult = ChengYueMoonForm.values()[formIndex].getDamageMultiplier();
            }

            // 压缩叠乘：不让倍率无限乘爆
            float extra = 0.0f;
            extra += (moonMult - 1.0f) * 0.6f;
            extra += (formMult - 1.0f) * 0.6f;

            total *= (1.0 + extra);
        }

        return total;
    }

    /**
     * 攻击速度
     * 👉 与上一版相同（攻击平衡用）
     */
    public static double getAttackSpeed(ItemStack stack, World world) {
        ChengYueNBT.init(stack);

        int level = ChengYueNBT.getLevel(stack);
        double speed = 1.6 + level * 0.01; // 每级 +0.01

        if (world != null) {
            float moonBonus = ChengYueMoonPhase.getAttackSpeedBonus(world);
            float memoryBonus = ChengYueMoonMemory.getAttackSpeedBonusWithMemory(stack, world);
            // 攻速加成用加法，并稍微压缩
            speed += (moonBonus + memoryBonus) * 0.6f;
        }

        // cap：不要疯狂连砍
        return Math.min(3.0, speed);
    }

    /**
     * 暴击率
     * 👉 与上一版相同（攻向平衡）
     */
    public static float getCritChance(ItemStack stack, World world) {
        ChengYueNBT.init(stack);

        int level = ChengYueNBT.getLevel(stack);
        float critChance = 0.10f + level * 0.005f; // 10% + 0.5%/lvl

        if (world != null) {
            float phaseBonus = ChengYueMoonPhase.getCritChanceBonus(world);
            float memoryBonus = ChengYueMoonMemory.getCritChanceBonusWithMemory(stack, world);
            critChance += (phaseBonus + memoryBonus) * 0.6f;
        }

        return Math.min(0.5f, critChance); // 上限 50%
    }

    /**
     * 暴击伤害
     * 👉 与上一版相同（攻向平衡）
     */
    public static float getCritDamage(ItemStack stack, World world) {
        ChengYueNBT.init(stack);

        int stage = ChengYueNBT.getStage(stack);
        // 基础暴伤：1.5x 起步，每阶 +0.2
        float critDamage = 1.5f + stage * 0.2f;

        if (world != null) {
            float phaseMult = ChengYueMoonPhase.getCritDamageMultiplier(world);
            float memoryMult = ChengYueMoonMemory.getCritDamageMultiplierWithMemory(stack, world);

            // 暴伤用“额外加成”的方式压缩
            float extra = 0.0f;
            extra += (phaseMult - 1.0f) * 0.5f;
            extra += (memoryMult - 1.0f) * 0.5f;

            critDamage *= (1.0f + extra);
        }

        return Math.min(2.2f, critDamage); // cap 2.2x
    }

    // ==================== 生存向（按你原本设计，不额外削弱） ====================

    /**
     * 吸血比例
     * 原本设计：
     *   5% 起步，每级 +0.2%，吃月相记忆倍率，最高 30%
     */
    public static float getLifeSteal(ItemStack stack, World world) {
        ChengYueNBT.init(stack);

        int level = ChengYueNBT.getLevel(stack);
        float lifeSteal = 0.05f + level * 0.002f;  // 5% + 0.2%/level

        if (world != null) {
            // 使用月相记忆倍率（我们改过 MoonMemory：没记忆时返回 1.0，不会负面）
            float moonMult = ChengYueMoonMemory.getLifeStealMultiplierWithMemory(stack, world);
            lifeSteal *= moonMult;
        }

        // 保留你原本 cap = 30%
        return Math.min(0.3f, lifeSteal);
    }

    /**
     * 减伤比例
     * 原本设计：
     *   每阶 5%，吃月相记忆减伤加成，最高 50%
     */
    public static float getDamageReduction(ItemStack stack, World world) {
        ChengYueNBT.init(stack);

        int stage = ChengYueNBT.getStage(stack);
        float reduction = stage * 0.05f; // 每阶 5%

        if (world != null) {
            reduction += ChengYueMoonMemory.getDamageReductionBonusWithMemory(stack, world);
        }

        // 保留你原本 cap = 50%
        return Math.min(0.5f, reduction);
    }

    /**
     * 闪避率
     * 原本设计：
     *   10 级后开始给，每级 +0.2%，吃月相记忆闪避加成，最高 30%
     */
    public static float getDodgeChance(ItemStack stack, World world) {
        ChengYueNBT.init(stack);

        int level = ChengYueNBT.getLevel(stack);
        float dodge = level < 10 ? 0.0f : (level - 10) * 0.002f; // 从 10 级开始，每级 +0.2%

        if (world != null) {
            dodge += ChengYueMoonMemory.getDodgeChanceBonusWithMemory(stack, world);
        }

        // 保留你原本 cap = 30%
        return Math.min(0.3f, dodge);
    }
}
