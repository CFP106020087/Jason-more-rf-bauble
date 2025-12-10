package com.moremod.mixin.enchant;

import net.minecraft.entity.player.EntityPlayer;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
 * - 无上限！
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

    // 服务端计算的真实附魔等级缓存
    @Unique
    private int[] moremod$serverEnchantLevels = new int[3];

    /**
     * 在原版计算完附魔等级后，计算增强等级但不修改显示值
     * onCraftMatrixChanged -> func_75130_a
     *
     * 关键设计：
     * - enchantLevels[] 保持原版值（用于显示和经验等级检查）
     * - moremod$serverEnchantLevels[] 存储增强后的值（用于实际附魔计算）
     *
     * 这样玩家只需30级经验就能附魔，但实际效果按增强等级计算
     */
    @Inject(method = {"onCraftMatrixChanged", "func_75130_a"}, at = @At("RETURN"), require = 0)
    private void moremod$boostEnchantLevels(IInventory inventoryIn, CallbackInfo ci) {
        if (world == null || position == null) return;

        // 只在服务端处理
        if (world.isRemote) return;

        // 计算总能量和超出原版上限的额外能量
        float totalPower = moremod$calculateTotalPower();
        float extraPower = moremod$calculateExtraPower();

        // 调试输出
        System.out.println("[MoreMod-Enchant] Mixin触发! 总能量=" + totalPower + ", 额外能量=" + extraPower);

        if (extraPower <= 0) {
            // 没有额外能量，增强等级等于显示等级
            System.arraycopy(enchantLevels, 0, moremod$serverEnchantLevels, 0, 3);
            return;
        }

        // 计算额外等级加成 (每1.0额外能量 = 2级)
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

        if (!hasItem) {
            System.arraycopy(enchantLevels, 0, moremod$serverEnchantLevels, 0, 3);
            return;
        }

        // 计算增强等级（不修改enchantLevels，保持原版显示值）
        for (int i = 0; i < 3; i++) {
            if (enchantLevels[i] > 0) {
                int originalLevel = enchantLevels[i];

                // 按槽位比例提升 (第1槽30%, 第2槽60%, 第3槽100%)
                float ratio = (i + 1) / 3.0f;
                int addedLevels = (int) (bonusLevels * ratio);

                // 计算增强等级（无上限）
                int boostedLevel = originalLevel + addedLevels;
                moremod$serverEnchantLevels[i] = boostedLevel;

                if (boostedLevel != originalLevel) {
                    System.out.println("[MoreMod-Enchant] 附魔槽 " + (i+1) + ": 显示=" + originalLevel + ", 实际=" + boostedLevel);
                }
            } else {
                moremod$serverEnchantLevels[i] = 0;
            }
        }
    }

    /**
     * 拦截 enchantItem 方法，在附魔计算前临时使用增强等级
     *
     * 原版流程：
     * 1. 检查 player.experienceLevel >= enchantLevels[id] （使用显示等级，玩家只需30级）
     * 2. 调用 EnchantmentHelper.buildEnchantmentList(stack, enchantLevels[id]) （我们要让它使用增强等级）
     * 3. 应用附魔
     *
     * 我们的策略：在HEAD临时替换等级，让附魔计算使用增强值
     */
    @Inject(method = {"enchantItem", "func_75140_a"}, at = @At("HEAD"), require = 0)
    private void moremod$onEnchantItemHead(EntityPlayer player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (world == null || world.isRemote) return;
        if (id < 0 || id >= 3) return;

        // 输出调试信息
        System.out.println("[MoreMod-Enchant] enchantItem开始! 槽位=" + id +
            ", 显示等级=" + enchantLevels[id] +
            ", 增强等级=" + moremod$serverEnchantLevels[id]);

        // 如果有增强等级，临时替换（让附魔计算使用增强值）
        if (moremod$serverEnchantLevels[id] > enchantLevels[id]) {
            System.out.println("[MoreMod-Enchant] 临时使用增强等级: " + enchantLevels[id] + " -> " + moremod$serverEnchantLevels[id]);
            enchantLevels[id] = moremod$serverEnchantLevels[id];
        }
    }

    /**
     * 在 enchantItem 方法结束后清理增强等级
     * 虽然物品已经附魔完成，但保险起见恢复原值
     */
    @Inject(method = {"enchantItem", "func_75140_a"}, at = @At("RETURN"), require = 0)
    private void moremod$onEnchantItemReturn(EntityPlayer player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (world == null || world.isRemote) return;
        if (id < 0 || id >= 3) return;

        // 重置增强等级缓存（下次放入物品时会重新计算）
        moremod$serverEnchantLevels[id] = 0;

        System.out.println("[MoreMod-Enchant] enchantItem完成! 结果=" + cir.getReturnValue());
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
