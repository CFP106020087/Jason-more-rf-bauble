package com.moremod.mixin.enchant;

import com.moremod.block.BlockEnchantingBooster;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * 突破附魔等级上限
 * 当附魔台周围有魔法书柜时，允许附魔等级超过30
 *
 * 原版上限: 30级 (15个书架 × 2 = 30)
 * 突破后: 根据魔法书柜的增益值计算更高上限
 */
@Mixin(ContainerEnchantment.class)
public abstract class MixinContainerEnchantment {

    @Shadow
    private World world;

    @Shadow
    private BlockPos position;

    @Shadow
    private Random rand;

    @Unique
    private static final int VANILLA_MAX = 30;

    @Unique
    private static final int BOOSTED_MAX = 60; // 最高允许60级附魔

    /**
     * 计算附近魔法书柜提供的额外加成
     * 魔法书柜的 enchantBonus 会额外增加附魔上限
     */
    @Unique
    private int moremod$calculateBoosterBonus() {
        if (world == null || position == null) return 0;

        float totalBoosterBonus = 0;

        // 扫描附魔台周围 2 格范围
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    // 跳过附魔台正上方和正中
                    if ((dx != 0 || dz != 0) || dy != 0) {
                        BlockPos checkPos = position.add(dx, dy, dz);
                        IBlockState state = world.getBlockState(checkPos);

                        if (state.getBlock() instanceof BlockEnchantingBooster) {
                            // 获取书柜类型的增益值
                            float bonus = state.getValue(BlockEnchantingBooster.TYPE).getEnchantBonus();
                            totalBoosterBonus += bonus;
                        }
                    }
                }
            }
        }

        // 将浮点增益转换为整数加成
        // 每1.0点增益 = 2级附魔上限提升
        return (int) (totalBoosterBonus * 2);
    }

    /**
     * 修改附魔等级上限
     * 在计算附魔选项时，根据魔法书柜提升上限
     */
    @Inject(method = "updateRepairOutput", at = @At("HEAD"), remap = false, require = 0)
    private void moremod$onUpdateEnchantLevels(CallbackInfo ci) {
        // 此方法可能不存在于1.12.2，使用其他注入点
    }

    /**
     * 在添加监听器时计算加成
     * 这是确保在GUI打开时计算的一个时机
     */
    @Inject(method = "onCraftMatrixChanged", at = @At("TAIL"))
    private void moremod$afterCraftMatrixChanged(net.minecraft.inventory.IInventory inventoryIn, CallbackInfo ci) {
        // 此注入在物品放入附魔台后触发
        // 附魔等级计算已经在原方法中完成
        // 我们需要在计算后修改结果

        ContainerEnchantment self = (ContainerEnchantment)(Object)this;
        int boosterBonus = moremod$calculateBoosterBonus();

        if (boosterBonus > 0) {
            // 获取当前计算的等级
            int[] levels = self.enchantLevels;

            // 如果任何槽位达到了30级上限，尝试提升
            for (int i = 0; i < 3; i++) {
                if (levels[i] >= 30) {
                    // 计算新的上限
                    int newMax = Math.min(VANILLA_MAX + boosterBonus, BOOSTED_MAX);

                    // 按比例提升（第三槽位获得最大提升）
                    float ratio = (i + 1) / 3.0f;
                    int additionalLevels = (int) ((newMax - VANILLA_MAX) * ratio);

                    levels[i] = Math.min(levels[i] + additionalLevels, newMax);
                }
            }

            // 更新显示
            self.detectAndSendChanges();
        }
    }
}
