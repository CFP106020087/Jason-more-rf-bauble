package com.moremod.mixin.enchant;

import com.moremod.block.BlockEnchantingBooster;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
 * - 魔法书柜提供额外的 enchantBonus
 * - 在附魔等级计算完成后，根据魔法书柜的额外加成提升等级
 * - 最高允许60级附魔
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
    private static final int VANILLA_MAX_LEVEL = 30;

    @Unique
    private static final int BOOSTED_MAX_LEVEL = 60;

    /**
     * 在原版计算完附魔等级后，根据魔法书柜提升等级
     */
    @Inject(method = "onCraftMatrixChanged", at = @At("RETURN"))
    private void moremod$boostEnchantLevels(IInventory inventoryIn, CallbackInfo ci) {
        if (world == null || position == null) return;

        // 计算魔法书柜提供的额外加成
        float totalBoosterBonus = moremod$calculateBoosterBonus();

        if (totalBoosterBonus <= 0) return;

        // 计算额外等级加成 (每1.0 bonus = 2级)
        int bonusLevels = (int) (totalBoosterBonus * 2);

        System.out.println("[MoreMod] 附魔增强: 检测到魔法书柜加成 = " + totalBoosterBonus + ", 额外等级 = " + bonusLevels);

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
                    System.out.println("[MoreMod] 附魔槽 " + (i+1) + ": " + originalLevel + " -> " + newLevel);
                }
            }
        }
    }

    /**
     * 计算附近魔法书柜提供的额外加成
     * 注意: 只计算 BlockEnchantingBooster 的额外加成
     * 不包括普通书架 (普通书架由原版处理)
     */
    @Unique
    private float moremod$calculateBoosterBonus() {
        if (world == null || position == null) return 0;

        float totalBonus = 0;

        // 扫描附魔台周围 (与原版相同的范围)
        for (int dz = -1; dz <= 1; ++dz) {
            for (int dx = -1; dx <= 1; ++dx) {
                // 跳过中心和紧邻附魔台的位置
                if ((dz != 0 || dx != 0) && moremod$isAirOrEmpty(position.add(dx, 0, dz)) && moremod$isAirOrEmpty(position.add(dx, 1, dz))) {
                    // 检查第一圈外的方块 (距离2格)
                    totalBonus += moremod$getBoosterBonus(position.add(dx * 2, 0, dz * 2));
                    totalBonus += moremod$getBoosterBonus(position.add(dx * 2, 1, dz * 2));

                    // 检查对角线位置
                    if (dx != 0 && dz != 0) {
                        totalBonus += moremod$getBoosterBonus(position.add(dx * 2, 0, dz));
                        totalBonus += moremod$getBoosterBonus(position.add(dx * 2, 1, dz));
                        totalBonus += moremod$getBoosterBonus(position.add(dx, 0, dz * 2));
                        totalBonus += moremod$getBoosterBonus(position.add(dx, 1, dz * 2));
                    }
                }
            }
        }

        return totalBonus;
    }

    /**
     * 检查位置是否为空气或可通过
     */
    @Unique
    private boolean moremod$isAirOrEmpty(BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().isAir(state, world, pos) ||
               !state.getMaterial().blocksMovement();
    }

    /**
     * 获取指定位置的魔法书柜加成
     * 只返回超过普通书架(1.0)的额外部分
     */
    @Unique
    private float moremod$getBoosterBonus(BlockPos pos) {
        IBlockState state = world.getBlockState(pos);

        if (state.getBlock() instanceof BlockEnchantingBooster) {
            float bonus = state.getValue(BlockEnchantingBooster.TYPE).getEnchantBonus();
            // 返回超过1.0的部分 (因为1.0已经被原版计算过了)
            // 如果书柜类型bonus <= 1.0，则不提供额外加成
            return Math.max(0, bonus - 1.0f);
        }

        return 0;
    }
}
