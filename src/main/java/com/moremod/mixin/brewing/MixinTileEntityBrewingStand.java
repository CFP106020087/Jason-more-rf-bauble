package com.moremod.mixin.brewing;

import com.moremod.item.ItemAlchemistStone;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 炼药师的术石 - 炼药台Mixin
 *
 * 当附近有佩戴炼药师术石的玩家时：
 * - 允许荧光石连续使用，最多增加5级
 * - 允许红石连续使用，无限延长时间
 */
@Mixin(TileEntityBrewingStand.class)
public abstract class MixinTileEntityBrewingStand {

    @Shadow
    private NonNullList<ItemStack> brewingItemStacks;

    @Shadow
    private int[] brewingItemStackRawIds;

    @Unique
    private static final int SLOT_INGREDIENT = 3;
    @Unique
    private static final int SLOT_FUEL = 4;
    @Unique
    private static final int MAX_EXTRA_AMPLIFIER = 5;
    @Unique
    private static final int MAX_DURATION_TICKS = 72000; // 1小时

    /**
     * 在 canBrew 方法返回前注入
     * 如果原版返回false但有炼药师术石玩家在附近，检查是否可以进行增强炼制
     */
    @Inject(method = "canBrew", at = @At("RETURN"), cancellable = true)
    private void moremod$allowEnhancedBrewing(CallbackInfoReturnable<Boolean> cir) {
        // 如果原版已经允许，不需要干预
        if (cir.getReturnValue()) return;

        TileEntityBrewingStand te = (TileEntityBrewingStand) (Object) this;
        World world = te.getWorld();
        BlockPos pos = te.getPos();

        if (world == null || pos == null) return;

        // 检查附近是否有佩戴炼药师术石的玩家
        if (!moremod$hasAlchemistStonePlayerNearby(world, pos)) return;

        // 获取材料
        ItemStack ingredient = brewingItemStacks.get(SLOT_INGREDIENT);
        if (ingredient.isEmpty()) return;

        boolean isGlowstone = ingredient.getItem() == Items.GLOWSTONE_DUST;
        boolean isRedstone = ingredient.getItem() == Items.REDSTONE;

        if (!isGlowstone && !isRedstone) return;

        // 检查是否有可增强的药水
        for (int i = 0; i < 3; i++) {
            ItemStack potionStack = brewingItemStacks.get(i);
            if (potionStack.isEmpty()) continue;

            if (isGlowstone && moremod$canEnhanceAmplifier(potionStack)) {
                cir.setReturnValue(true);
                return;
            }
            if (isRedstone && moremod$canExtendDuration(potionStack)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    /**
     * 在 brewPotions 方法中注入，处理增强炼制
     */
    @Inject(method = "brewPotions", at = @At("HEAD"), cancellable = true)
    private void moremod$handleEnhancedBrewing(CallbackInfo ci) {
        TileEntityBrewingStand te = (TileEntityBrewingStand) (Object) this;
        World world = te.getWorld();
        BlockPos pos = te.getPos();

        if (world == null || pos == null) return;

        // 检查附近是否有佩戴炼药师术石的玩家
        if (!moremod$hasAlchemistStonePlayerNearby(world, pos)) return;

        ItemStack ingredient = brewingItemStacks.get(SLOT_INGREDIENT);
        if (ingredient.isEmpty()) return;

        boolean isGlowstone = ingredient.getItem() == Items.GLOWSTONE_DUST;
        boolean isRedstone = ingredient.getItem() == Items.REDSTONE;

        if (!isGlowstone && !isRedstone) return;

        boolean didBrew = false;

        // 处理每个药水槽
        for (int i = 0; i < 3; i++) {
            ItemStack potionStack = brewingItemStacks.get(i);
            if (potionStack.isEmpty()) continue;

            // 检查是否已经有标准配方
            if (BrewingRecipeRegistry.hasOutput(potionStack, ingredient)) {
                // 让原版处理
                continue;
            }

            if (isGlowstone && moremod$canEnhanceAmplifier(potionStack)) {
                moremod$enhanceAmplifier(potionStack);
                didBrew = true;
            } else if (isRedstone && moremod$canExtendDuration(potionStack)) {
                moremod$extendDuration(potionStack);
                didBrew = true;
            }
        }

        if (didBrew) {
            // 消耗材料
            ingredient.shrink(1);

            // 播放音效
            world.playEvent(1035, pos, 0);

            // 取消原版处理
            ci.cancel();
        }
    }

    /**
     * 检查附近是否有佩戴炼药师术石的玩家
     */
    @Unique
    private boolean moremod$hasAlchemistStonePlayerNearby(World world, BlockPos pos) {
        double range = 5.0;
        AxisAlignedBB searchBox = new AxisAlignedBB(
            pos.getX() - range, pos.getY() - range, pos.getZ() - range,
            pos.getX() + range + 1, pos.getY() + range + 1, pos.getZ() + range + 1
        );

        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, searchBox);
        for (EntityPlayer player : players) {
            if (ItemAlchemistStone.isWearing(player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查药水是否可以提升等级
     */
    @Unique
    private boolean moremod$canEnhanceAmplifier(ItemStack potionStack) {
        // 检查是否是药水
        if (potionStack.getItem() != Items.POTIONITEM &&
            potionStack.getItem() != Items.SPLASH_POTION &&
            potionStack.getItem() != Items.LINGERING_POTION) {
            return false;
        }

        // 获取药水效果
        List<PotionEffect> effects = PotionUtils.getEffectsFromStack(potionStack);
        if (effects.isEmpty()) return false;

        // 检查是否已达到最大等级
        for (PotionEffect effect : effects) {
            int currentLevel = effect.getAmplifier();
            // 原版最大是1（Level II），我们允许到 1 + MAX_EXTRA_AMPLIFIER
            if (currentLevel < 1 + MAX_EXTRA_AMPLIFIER) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查药水是否可以延长时间
     */
    @Unique
    private boolean moremod$canExtendDuration(ItemStack potionStack) {
        // 检查是否是药水
        if (potionStack.getItem() != Items.POTIONITEM &&
            potionStack.getItem() != Items.SPLASH_POTION &&
            potionStack.getItem() != Items.LINGERING_POTION) {
            return false;
        }

        // 获取药水效果
        List<PotionEffect> effects = PotionUtils.getEffectsFromStack(potionStack);
        if (effects.isEmpty()) return false;

        // 检查是否已达到最大时间
        for (PotionEffect effect : effects) {
            if (effect.getDuration() < MAX_DURATION_TICKS) {
                return true;
            }
        }
        return false;
    }

    /**
     * 增强药水等级
     */
    @Unique
    private void moremod$enhanceAmplifier(ItemStack potionStack) {
        List<PotionEffect> effects = PotionUtils.getEffectsFromStack(potionStack);
        if (effects.isEmpty()) return;

        List<PotionEffect> newEffects = new ArrayList<>();
        for (PotionEffect effect : effects) {
            int newAmplifier = Math.min(effect.getAmplifier() + 1, 1 + MAX_EXTRA_AMPLIFIER);
            newEffects.add(new PotionEffect(
                effect.getPotion(),
                effect.getDuration(),
                newAmplifier,
                effect.getIsAmbient(),
                effect.doesShowParticles()
            ));
        }

        // 更新药水NBT
        moremod$setCustomPotionEffects(potionStack, newEffects);
    }

    /**
     * 延长药水时间
     */
    @Unique
    private void moremod$extendDuration(ItemStack potionStack) {
        List<PotionEffect> effects = PotionUtils.getEffectsFromStack(potionStack);
        if (effects.isEmpty()) return;

        List<PotionEffect> newEffects = new ArrayList<>();
        for (PotionEffect effect : effects) {
            // 每次延长50%，最多到1小时
            int newDuration = Math.min((int)(effect.getDuration() * 1.5), MAX_DURATION_TICKS);
            newEffects.add(new PotionEffect(
                effect.getPotion(),
                newDuration,
                effect.getAmplifier(),
                effect.getIsAmbient(),
                effect.doesShowParticles()
            ));
        }

        // 更新药水NBT
        moremod$setCustomPotionEffects(potionStack, newEffects);
    }

    /**
     * 设置自定义药水效果
     */
    @Unique
    private void moremod$setCustomPotionEffects(ItemStack potionStack, List<PotionEffect> effects) {
        NBTTagCompound tag = potionStack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            potionStack.setTagCompound(tag);
        }

        // 清除原有的 Potion 类型（改为使用自定义效果）
        // 保留 Potion 标签以便显示正确的颜色

        // 写入自定义效果
        PotionUtils.addCustomPotionEffectToList(tag, effects);
    }
}
