package com.moremod.mixin.enchant;

import com.moremod.config.ModConfig;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
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

import java.util.ArrayList;
import java.util.List;

/**
 * MoreMod - 附魔台强化 Mixin v2.0
 *
 * 改进：
 * - 经验消耗上限可配置（默认30级，不再吃所有等级）
 * - 附魔等级上限99级
 * - 普通书架（15个以内）也能触发超过30级附魔
 */
@Mixin(ContainerEnchantment.class)
public abstract class MixinContainerEnchantment {

    @Shadow private World world;
    @Shadow private BlockPos position;
    @Shadow public int[] enchantLevels;
    @Shadow public IInventory tableInventory;

    @Unique private static final float VANILLA_MAX_POWER = 15.0f;
    @Unique private int[] moremod$serverEnchantLevels = new int[3];

    /** 从配置获取是否启用 */
    @Unique
    private boolean moremod$isEnabled() {
        return ModConfig.enchanting.enabled;
    }

    /** 从配置获取经验消耗上限 */
    @Unique
    private int moremod$getMaxExpCost() {
        return ModConfig.enchanting.maxExpCost;
    }

    /** 从配置获取附魔等级上限 */
    @Unique
    private int moremod$getMaxEnchantLevel() {
        return ModConfig.enchanting.maxEnchantLevel;
    }

    /** 从配置获取特殊书架倍率 */
    @Unique
    private double moremod$getSpecialMultiplier() {
        return ModConfig.enchanting.specialBookshelfMultiplier;
    }

    /** 核心能量计算逻辑 */
    @Unique
    private float moremod$calculateTotalPower() {
        if (world == null || position == null) return 0;
        float power = 0;

        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dz != 0 || dx != 0)
                        && world.isAirBlock(position.add(dx,0,dz))
                        && world.isAirBlock(position.add(dx,1,dz))) {

                    power += ForgeHooks.getEnchantPower(world, position.add(dx * 2, 0, dz * 2));
                    power += ForgeHooks.getEnchantPower(world, position.add(dx * 2, 1, dz * 2));

                    if (dx != 0 && dz != 0) {
                        power += ForgeHooks.getEnchantPower(world, position.add(dx * 2, 0, dz));
                        power += ForgeHooks.getEnchantPower(world, position.add(dx * 2, 1, dz));
                        power += ForgeHooks.getEnchantPower(world, position.add(dx, 0, dz * 2));
                        power += ForgeHooks.getEnchantPower(world, position.add(dx, 1, dz * 2));
                    }
                }
            }
        }
        return power;
    }

    /** 获取超出普通书架的额外能量 */
    @Unique
    private float moremod$getExtraPower() {
        return Math.max(0, moremod$calculateTotalPower() - VANILLA_MAX_POWER);
    }

    /**
     * 计算最终附魔等级
     * 普通书架(<=15)无法突破30级
     * 只有特殊书架(额外能量>0)才能突破30级上限
     */
    @Unique
    private int moremod$calculateEnchantLevel(int baseLevel, int slotIndex) {
        float extraPower = moremod$getExtraPower();

        int finalLevel = baseLevel;

        // 只有特殊书架(额外能量>0)才能突破30级
        if (extraPower > 0) {
            // 特殊书架提升：每点额外能量增加 multiplier 级
            int bonus = (int)(extraPower * moremod$getSpecialMultiplier());
            float ratio = (slotIndex + 1) / 3.0f;
            finalLevel = baseLevel + (int)(bonus * ratio);
        }
        // 普通书架(<=15)维持原版逻辑，不突破30级

        // 应用等级上限
        return Math.min(finalLevel, moremod$getMaxEnchantLevel());
    }

    /** 修改附魔槽等级（只有特殊书架能突破30上限） */
    @Inject(method = {"onCraftMatrixChanged","func_75130_a"}, at = @At("RETURN"))
    private void moremod$boostLevels(IInventory inv, CallbackInfo ci) {
        if (world == null || world.isRemote) return;
        if (!moremod$isEnabled()) {
            System.arraycopy(enchantLevels, 0, moremod$serverEnchantLevels, 0, 3);
            return;
        }

        float extraPower = moremod$getExtraPower();

        // 只有特殊书架(额外能量>0)才能突破30级
        if (extraPower > 0) {
            for (int i = 0; i < 3; i++) {
                if (enchantLevels[i] > 0) {
                    enchantLevels[i] = moremod$calculateEnchantLevel(enchantLevels[i], i);
                }
            }
        }

        System.arraycopy(enchantLevels, 0, moremod$serverEnchantLevels, 0, 3);
    }

    /** 修正点击时等级一致 */
    @Inject(method = {"enchantItem","func_75140_a"}, at = @At("HEAD"))
    private void moremod$syncLevel(EntityPlayer player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (world.isRemote) return;
        if (id < 0 || id >= 3) return;
        if (!moremod$isEnabled()) return;

        enchantLevels[id] = moremod$serverEnchantLevels[id];
    }

    /** 按钮可点击逻辑修补（只有特殊书架时才需要修补） */
    @Inject(method = {"canEnchant","func_82869_a"}, at = @At("HEAD"), cancellable = true)
    private void moremod$unlockButton(EntityPlayer player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!moremod$isEnabled()) return;

        float extraPower = moremod$getExtraPower();
        // 只有特殊书架(额外能量>0)时才需要修补按钮逻辑
        if (extraPower > 0 && id >= 0 && id < 3) {
            if (moremod$serverEnchantLevels[id] > 0) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    /** 强制高等级附魔逻辑 - 经验消耗有上限 */
    @Inject(method = {"enchantItem","func_75140_a"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void moremod$forceEnchant(EntityPlayer player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (world.isRemote) return;
        if (id < 0 || id >= 3) return;
        if (!moremod$isEnabled()) return;

        int enchantLevel = moremod$serverEnchantLevels[id];
        if (enchantLevel <= 30) return; // 30级以下走原版逻辑

        ItemStack item = tableInventory.getStackInSlot(0);
        if (item.isEmpty()) return;

        // 经验消耗上限：不再吃掉所有等级
        int expCost = Math.min(enchantLevel, moremod$getMaxExpCost());

        if (player.experienceLevel < expCost) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        // 只扣除上限内的经验
        player.addExperienceLevel(-expCost);

        // 附魔等级上限
        int effectiveLevel = Math.min(enchantLevel, moremod$getMaxEnchantLevel());

        List<EnchantmentData> enchList =
                EnchantmentHelper.buildEnchantmentList(player.getRNG(), item, effectiveLevel, false);

        if (enchList == null || enchList.isEmpty()) {
            enchList = new ArrayList<>();

            for (Enchantment ench : Enchantment.REGISTRY) {
                if (ench != null && ench.canApply(item)) {
                    int newLvl = Math.max(1, Math.min(ench.getMaxLevel(), effectiveLevel / 30));
                    enchList.add(new EnchantmentData(ench, newLvl));
                }
            }
        }

        for (EnchantmentData data : enchList) {
            item.addEnchantment(data.enchantment, data.enchantmentLevel);
        }

        world.playEvent(1030, position, 0);
        cir.setReturnValue(true);
        cir.cancel();
    }
}
