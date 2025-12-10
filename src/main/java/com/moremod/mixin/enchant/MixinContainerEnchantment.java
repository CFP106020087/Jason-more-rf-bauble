package com.moremod.mixin.enchant;

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
 * MoreMod - 附魔台强化 Mixin
 * 支援无限等级附魔、高级附魔池
 */
@Mixin(ContainerEnchantment.class)
public abstract class MixinContainerEnchantment {

    @Shadow private World world;
    @Shadow private BlockPos position;
    @Shadow public int[] enchantLevels;
    @Shadow public IInventory tableInventory;

    @Unique private static final float VANILLA_MAX_POWER = 15.0f;
    @Unique private int[] moremod$serverEnchantLevels = new int[3];

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

    @Unique
    private float moremod$getExtraPower() {
        return Math.max(0, moremod$calculateTotalPower() - VANILLA_MAX_POWER);
    }

    /** 修改附魔槽等级（突破30上限） */
    @Inject(method = {"onCraftMatrixChanged","func_75130_a"}, at = @At("RETURN"))
    private void moremod$boostLevels(IInventory inv, CallbackInfo ci) {
        if (world == null || world.isRemote) return;

        float extra = moremod$getExtraPower();
        if (extra <= 0) {
            System.arraycopy(enchantLevels, 0, moremod$serverEnchantLevels, 0, 3);
            return;
        }

        int bonus = (int)(extra * 2); // 能量换等级

        for (int i = 0; i < 3; i++) {
            if (enchantLevels[i] > 0) {
                float ratio = (i + 1) / 3.0f;
                enchantLevels[i] += (int)(bonus * ratio);
            }
        }
        System.arraycopy(enchantLevels, 0, moremod$serverEnchantLevels, 0, 3);
    }

    /** 修正点击时等级一致 */
    @Inject(method = {"enchantItem","func_75140_a"}, at = @At("HEAD"))
    private void moremod$syncLevel(EntityPlayer player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (world.isRemote) return;
        if (id < 0 || id >= 3) return;
        if (moremod$getExtraPower() <= 0) return;

        enchantLevels[id] = moremod$serverEnchantLevels[id];
    }

    /** 🔥按钮可点击逻辑修补 */
    @Inject(method = {"canEnchant","func_82869_a"}, at = @At("HEAD"), cancellable = true)
    private void moremod$unlockButton(EntityPlayer player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (moremod$getExtraPower() > 0 && id >= 0 && id < 3) {
            if (moremod$serverEnchantLevels[id] > 0) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    /** 🔥强制高等级附魔逻辑 */
    @Inject(method = {"enchantItem","func_75140_a"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void moremod$forceEnchant(EntityPlayer player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (world.isRemote) return;
        if (id < 0 || id >= 3) return;

        int level = moremod$serverEnchantLevels[id];
        if (level <= 0) return;

        ItemStack item = tableInventory.getStackInSlot(0);
        if (item.isEmpty()) return;

        if (player.experienceLevel < level) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        player.addExperienceLevel(-level);

        List<EnchantmentData> enchList =
                EnchantmentHelper.buildEnchantmentList(player.getRNG(), item, level, false);

        if (enchList == null || enchList.isEmpty()) {
            enchList = new ArrayList<>();

            for (Enchantment ench : Enchantment.REGISTRY) {
                if (ench != null && ench.canApply(item)) {
                    int newLvl = Math.max(1, Math.min(ench.getMaxLevel(), level / 30));
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
