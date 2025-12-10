package com.moremod.mixin.enchant;

import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 突破附魔等级上限
 * 当附魔台周围有魔法书柜(BlockEnchantingBooster)时，允许附魔等级超过30
 *
 * 原版机制:
 * - 普通书架提供 1.0 power (通过 getEnchantPowerBonus)
 * - 最多计算15个书架 = 15 power = 30级附魔
 * - 原版会 Math.min(power, 15) 限制上限
 *
 * 我们的修改:
 * - 魔法书柜提供额外的 enchantBonus (通过 getEnchantPowerBonus)
 * - 使用 ForgeHooks.getEnchantPower() 获取总能量
 * - 如果总能量超过15，根据超出部分提升附魔等级
 * - 最高允许60级附魔
 *
 * 参考: Apotheosis 模组的 EnchantmentUtils.getPower() 实现
 */
@Mixin(ContainerEnchantment.class)
public abstract class MixinContainerEnchantment {

    @Shadow
    private World world;

    @Shadow
    private BlockPos position;

    @Shadow
    public int[] enchantLevels;

    @Unique
    private static final float VANILLA_MAX_POWER = 15.0f;

    @Unique
    private static final int BOOSTED_MAX_LEVEL = 60;

    /**
     * 在原版计算完附魔等级后，根据额外能量提升等级
     * onCraftMatrixChanged -> func_75130_a
     *
     * 使用两个方法名兼容开发环境(MCP)和生产环境(SRG)
     */
    @Inject(method = {"onCraftMatrixChanged", "func_75130_a"}, at = @At("RETURN"), require = 0)
    private void moremod$boostEnchantLevels(IInventory inventoryIn, CallbackInfo ci) {
        if (world == null || position == null) return;

        // 计算总能量和超出原版上限的额外能量
        float totalPower = moremod$calculateTotalPower();
        float extraPower = moremod$calculateExtraPower();

        // 调试输出：总是打印（帮助调试mixin是否加载）
        System.out.println("[MoreMod-Enchant] Mixin触发! 总能量=" + totalPower + ", 额外能量=" + extraPower);

        if (extraPower <= 0) return;

        // 计算额外等级加成 (每1.0额外能量 = 2级)
        // 这与原版的 power * 2 = max level 公式一致
        int bonusLevels = (int) (extraPower * 2);

        System.out.println("[MoreMod-Enchant] 附魔增强: 额外能量=" + extraPower + ", 额外等级=" + bonusLevels);

        // 检查是否有物品在附魔台中
        boolean hasItem = false;
        for (int i = 0; i < 3; i++) {
            if (enchantLevels[i] > 0) {
                hasItem = true;
                break;
            }
        }

        if (!hasItem) return;

        // 提升附魔等级
        for (int i = 0; i < 3; i++) {
            if (enchantLevels[i] > 0) {
                int originalLevel = enchantLevels[i];

                // 按槽位比例提升 (第1槽30%, 第2槽60%, 第3槽100%)
                float ratio = (i + 1) / 3.0f;
                int addedLevels = (int) (bonusLevels * ratio);

                // 计算新等级，限制在最大值
                int newLevel = Math.min(originalLevel + addedLevels, BOOSTED_MAX_LEVEL);
                enchantLevels[i] = newLevel;

                if (newLevel != originalLevel) {
                    System.out.println("[MoreMod-Enchant] 附魔槽 " + (i+1) + ": " + originalLevel + " -> " + newLevel);
                }
            }
        }
    }

    /**
     * 使用 ForgeHooks.getEnchantPower() 计算总能量
     * 参考 Apotheosis 的 EnchantmentUtils.getPower() 实现
     *
     * @return 总能量值（未被原版15上限截断的真实值）
     */
    @Unique
    private float moremod$calculateTotalPower() {
        if (world == null || position == null) return 0;

        float power = 0;

        // 使用与原版和 Apotheosis 相同的扫描模式
        for (int deltaZ = -1; deltaZ <= 1; ++deltaZ) {
            for (int deltaX = -1; deltaX <= 1; ++deltaX) {
                if ((deltaZ != 0 || deltaX != 0) &&
                    world.isAirBlock(position.add(deltaX, 0, deltaZ)) &&
                    world.isAirBlock(position.add(deltaX, 1, deltaZ))) {

                    // 使用 ForgeHooks.getEnchantPower 获取能量
                    power += ForgeHooks.getEnchantPower(world, position.add(deltaX * 2, 0, deltaZ * 2));
                    power += ForgeHooks.getEnchantPower(world, position.add(deltaX * 2, 1, deltaZ * 2));

                    if (deltaX != 0 && deltaZ != 0) {
                        power += ForgeHooks.getEnchantPower(world, position.add(deltaX * 2, 0, deltaZ));
                        power += ForgeHooks.getEnchantPower(world, position.add(deltaX * 2, 1, deltaZ));
                        power += ForgeHooks.getEnchantPower(world, position.add(deltaX, 0, deltaZ * 2));
                        power += ForgeHooks.getEnchantPower(world, position.add(deltaX, 1, deltaZ * 2));
                    }
                }
            }
        }

        return power;
    }

    /**
     * 计算超出原版上限的额外能量
     * 只有当总能量超过15时才返回正值
     */
    @Unique
    private float moremod$calculateExtraPower() {
        float totalPower = moremod$calculateTotalPower();
        // 只返回超过原版15上限的部分
        return Math.max(0, totalPower - VANILLA_MAX_POWER);
    }
}
