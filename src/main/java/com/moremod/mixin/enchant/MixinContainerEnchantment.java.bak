package com.moremod.mixin.enchant;

import com.moremod.config.ModConfig;
import com.moremod.sponsor.item.ZhuxianSword;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemEnchantedBook;
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
import java.util.Random;

/**
 * MoreMod - 附魔台强化 Mixin v3.0
 *
 * 修复内容：
 * - 修复客户端/服务端显示不同步问题
 * - 修复附魔提示与实际附魔结果不一致问题（使用正确的xpSeed）
 * - 补充青金石消耗逻辑
 * - 经验消耗上限可配置（默认30级）
 * - 附魔等级上限99级
 * - 普通书架（15个以内）维持原版逻辑，特殊书架才能突破30级
 */
@Mixin(ContainerEnchantment.class)
public abstract class MixinContainerEnchantment {

    @Shadow private World world;
    @Shadow private BlockPos position;
    @Shadow public int[] enchantLevels;
    @Shadow public int[] enchantClue;
    @Shadow public int[] worldClue;
    @Shadow public int xpSeed;
    @Shadow public IInventory tableInventory;

    @Unique private static final float VANILLA_MAX_POWER = 15.0f;
    @Unique private int[] moremod$serverEnchantLevels = new int[3];

    // ==================== 配置读取 ====================

    @Unique
    private boolean moremod$isEnabled() {
        return ModConfig.enchanting.enabled;
    }

    @Unique
    private int moremod$getMaxExpCost() {
        return ModConfig.enchanting.maxExpCost;
    }

    @Unique
    private int moremod$getMaxEnchantLevel() {
        return ModConfig.enchanting.maxEnchantLevel;
    }

    @Unique
    private double moremod$getSpecialMultiplier() {
        return ModConfig.enchanting.specialBookshelfMultiplier;
    }

    // ==================== 能量计算 ====================

    @Unique
    private float moremod$calculateTotalPower() {
        if (world == null || position == null) return 0;
        float power = 0;

        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dz != 0 || dx != 0)
                        && world.isAirBlock(position.add(dx, 0, dz))
                        && world.isAirBlock(position.add(dx, 1, dz))) {

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

    @Unique
    private int moremod$calculateEnchantLevel(int baseLevel, int slotIndex) {
        float extraPower = moremod$getExtraPower();
        int finalLevel = baseLevel;

        if (extraPower > 0) {
            int bonus = (int) (extraPower * moremod$getSpecialMultiplier());
            float ratio = (slotIndex + 1) / 3.0f;
            finalLevel = baseLevel + (int) (bonus * ratio);
        }

        return Math.min(finalLevel, moremod$getMaxEnchantLevel());
    }

    // ==================== 核心注入 ====================

    /**
     * 修改附魔槽等级
     * 关键修复：客户端和服务端都执行相同逻辑，保证显示一致
     */
    @Inject(method = {"onCraftMatrixChanged", "func_75130_a"}, at = @At("RETURN"))
    private void moremod$boostLevels(IInventory inv, CallbackInfo ci) {
        if (world == null) return;
        if (!moremod$isEnabled()) {
            if (!world.isRemote) {
                System.arraycopy(enchantLevels, 0, moremod$serverEnchantLevels, 0, 3);
            }
            return;
        }

        float extraPower = moremod$getExtraPower();

        // 客户端和服务端都执行相同的修改逻辑，保证显示一致
        if (extraPower > 0) {
            for (int i = 0; i < 3; i++) {
                if (enchantLevels[i] > 0) {
                    enchantLevels[i] = moremod$calculateEnchantLevel(enchantLevels[i], i);
                }
            }

            // 同时更新附魔提示（使用正确的seed重新计算）
            moremod$updateEnchantClues();
        }

        // 只在服务端保存用于验证
        if (!world.isRemote) {
            System.arraycopy(enchantLevels, 0, moremod$serverEnchantLevels, 0, 3);
        }
    }

    /**
     * 更新附魔提示，使用正确的等级
     */
    @Unique
    private void moremod$updateEnchantClues() {
        ItemStack item = tableInventory.getStackInSlot(0);
        if (item.isEmpty()) return;

        for (int i = 0; i < 3; i++) {
            if (enchantLevels[i] > 0) {
                Random rand = new Random(xpSeed + i);
                List<EnchantmentData> list = EnchantmentHelper.buildEnchantmentList(rand, item, enchantLevels[i], false);

                if (list != null && !list.isEmpty()) {
                    EnchantmentData data = list.get(rand.nextInt(list.size()));
                    enchantClue[i] = Enchantment.getEnchantmentID(data.enchantment);
                    worldClue[i] = data.enchantmentLevel;
                } else {
                    enchantClue[i] = -1;
                    worldClue[i] = -1;
                }
            } else {
                enchantClue[i] = -1;
                worldClue[i] = -1;
            }
        }
    }

    /**
     * 修正点击时等级一致
     */
    @Inject(method = {"enchantItem", "func_75140_a"}, at = @At("HEAD"))
    private void moremod$syncLevel(EntityPlayer player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (world.isRemote) return;
        if (id < 0 || id >= 3) return;
        if (!moremod$isEnabled()) return;

        enchantLevels[id] = moremod$serverEnchantLevels[id];
    }

    /**
     * 按钮可点击逻辑修补
     */
    @Inject(method = {"canEnchant", "func_82869_a"}, at = @At("HEAD"), cancellable = true)
    private void moremod$unlockButton(EntityPlayer player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!moremod$isEnabled()) return;

        float extraPower = moremod$getExtraPower();
        if (extraPower > 0 && id >= 0 && id < 3) {
            if (moremod$serverEnchantLevels[id] > 0) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    /**
     * 强制高等级附魔逻辑
     * 关键修复：使用xpSeed保证附魔结果与提示一致
     */
    @Inject(method = {"enchantItem", "func_75140_a"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void moremod$forceEnchant(EntityPlayer player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (world.isRemote) return;
        if (id < 0 || id >= 3) return;
        if (!moremod$isEnabled()) return;

        int enchantLevel = moremod$serverEnchantLevels[id];
        if (enchantLevel <= 30) return; // 30级以下走原版逻辑

        ItemStack item = tableInventory.getStackInSlot(0);
        ItemStack lapis = tableInventory.getStackInSlot(1);

        if (item.isEmpty()) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        // 经验消耗固定为 1/2/3 级
        int expCost = id + 1;

        // 检查经验
        if (player.experienceLevel < expCost && !player.capabilities.isCreativeMode) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        // 检查青金石
        if ((lapis.isEmpty() || lapis.getItem() != Items.DYE || lapis.getMetadata() != 4 || lapis.getCount() < expCost)
                && !player.capabilities.isCreativeMode) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        // 扣除经验
        if (!player.capabilities.isCreativeMode) {
            player.addExperienceLevel(-expCost);
        }

        // 消耗青金石
        if (!player.capabilities.isCreativeMode) {
            lapis.shrink(expCost);
            if (lapis.isEmpty()) {
                tableInventory.setInventorySlotContents(1, ItemStack.EMPTY);
            }
        }

        int effectiveLevel = Math.min(enchantLevel, moremod$getMaxEnchantLevel());

        // 关键修复：使用和提示相同的seed，保证结果一致
        Random rand = new Random(xpSeed + id);
        List<EnchantmentData> enchList = EnchantmentHelper.buildEnchantmentList(rand, item, effectiveLevel, false);

        // Fallback：如果原版算法返回空，手动生成
        if (enchList == null || enchList.isEmpty()) {
            enchList = moremod$generateFallbackEnchantments(item, effectiveLevel);
        }

        // 应用附魔
        boolean isBook = item.getItem() == Items.BOOK;
        if (isBook) {
            item = new ItemStack(Items.ENCHANTED_BOOK);
            tableInventory.setInventorySlotContents(0, item);
        }

        for (EnchantmentData data : enchList) {
            if (isBook) {
                ItemEnchantedBook.addEnchantment(item, data);            } else {
                item.addEnchantment(data.enchantment, data.enchantmentLevel);
            }
        }

        // 重置seed（原版行为，下次附魔会不同）
        xpSeed = player.getRNG().nextInt();

        // 标记物品栏变化
        tableInventory.markDirty();

        // 播放音效
        world.playEvent(1030, position, 0);

        cir.setReturnValue(true);
        cir.cancel();
    }

    /**
     * Fallback附魔生成
     */
    @Unique
    private List<EnchantmentData> moremod$generateFallbackEnchantments(ItemStack item, int level) {
        List<EnchantmentData> result = new ArrayList<>();

        for (Enchantment ench : Enchantment.REGISTRY) {
            if (ench != null && ench.canApply(item)) {
                // 根据等级计算附魔等级，每30级提升1级
                int enchLvl = Math.max(1, Math.min(ench.getMaxLevel(), level / 30));
                result.add(new EnchantmentData(ench, enchLvl));
            }
        }

        return result;
    }

    /**
     * 诛仙剑-为天地立心：附魔消耗减少20%
     */
    @Inject(method = {"enchantItem", "func_75140_a"}, at = @At("RETURN"))
    private void moremod$zhuxianEnchantXpReduce(EntityPlayer player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (world.isRemote) return;
        if (!cir.getReturnValue()) return;
        if (id < 0 || id >= 3) return;

        try {
            if (ZhuxianSword.isPlayerSkillActive(player, ZhuxianSword.NBT_SKILL_TIANXIN)) {
                if (player.getHeldItemOffhand().isEmpty()) {
                    int baseCost = id + 1;
                    int refund = Math.max(1, (int) (baseCost * 0.2f));
                    int xpToAdd = refund * 17;
                    player.addExperience(xpToAdd);
                }
            }
        } catch (Throwable ignored) {
            // ZhuxianSword可能未加载
        }
    }
}