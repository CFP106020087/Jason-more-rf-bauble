package com.moremod.mixin.brewing;

import com.moremod.item.ItemAlchemistStone;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList; // 记得引入这个！
import net.minecraft.potion.PotionEffect;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * 炼药师的术石 - 炼药台增强 (v2.3 编译修复版)
 * 修复了 PotionUtils 方法缺失的问题，采用手动 NBT 解析
 */
@Mixin(TileEntityBrewingStand.class)
public abstract class MixinTileEntityBrewingStand {

    @Shadow
    private NonNullList<ItemStack> brewingItemStacks;

    @Shadow
    private int fuel;

    @Unique private static final int SLOT_INGREDIENT = 3;
    @Unique private static final int MAX_EXTRA_AMPLIFIER = 10;
    @Unique private static final int MAX_DURATION_TICKS = 20 * 60 * 60;

    // ======================== canBrew ========================

    @Inject(method = {"canBrew", "func_145934_k"}, at = @At("RETURN"), cancellable = true, remap = true)
    private void moremod$allowEnhancedBrewing(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (this.fuel <= 0) return;

        TileEntityBrewingStand te = (TileEntityBrewingStand)(Object)this;
        World world = te.getWorld();
        BlockPos pos = te.getPos();

        if (world == null || pos == null || this.brewingItemStacks.isEmpty()) return;
        if (!moremod$hasAlchemistStoneNearby(world, pos)) return;

        ItemStack ingredient = brewingItemStacks.get(SLOT_INGREDIENT);
        if (ingredient.isEmpty()) return;

        boolean glowstone = ingredient.getItem() == Items.GLOWSTONE_DUST;
        boolean redstone  = ingredient.getItem() == Items.REDSTONE;
        if (!glowstone && !redstone) return;

        for (int i = 0; i < 3; i++) {
            ItemStack potion = brewingItemStacks.get(i);
            if (potion.isEmpty()) continue;

            if ((glowstone && moremod$canEnhanceAmplifier(potion)) ||
                    (redstone && moremod$canExtendDuration(potion))) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    // ======================== brewPotions ========================

    @Inject(method = {"brewPotions", "func_145940_l"}, at = @At("HEAD"), cancellable = true, remap = true)
    private void moremod$handleEnhancedBrewing(CallbackInfo ci) {
        TileEntityBrewingStand te = (TileEntityBrewingStand)(Object)this;
        World world = te.getWorld();
        BlockPos pos = te.getPos();

        if (world == null || pos == null || world.isRemote) return;
        if (!moremod$hasAlchemistStoneNearby(world, pos)) return;

        ItemStack ingredient = brewingItemStacks.get(SLOT_INGREDIENT);
        if (ingredient.isEmpty()) return;

        boolean glowstone = ingredient.getItem() == Items.GLOWSTONE_DUST;
        boolean redstone  = ingredient.getItem() == Items.REDSTONE;
        if (!glowstone && !redstone) return;

        boolean didBrew = false;

        for (int i = 0; i < 3; i++) {
            ItemStack potion = brewingItemStacks.get(i);
            if (potion.isEmpty()) continue;

            if (BrewingRecipeRegistry.hasOutput(potion, ingredient)) continue;

            ItemStack newStack = potion.copy();
            boolean changed = false;

            if (glowstone && moremod$canEnhanceAmplifier(newStack)) {
                moremod$enhanceAmplifier(newStack);
                changed = true;
            } else if (redstone && moremod$canExtendDuration(newStack)) {
                moremod$extendDuration(newStack);
                changed = true;
            }

            if (changed) {
                brewingItemStacks.set(i, newStack);
                didBrew = true;
            }
        }

        if (!didBrew) return;

        ingredient.shrink(1);
        this.fuel--;

        world.playEvent(1035, pos, 0);
        te.markDirty();
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);

        ci.cancel();
    }

    // ======================== 工具方法 ========================

    @Unique
    private boolean moremod$hasAlchemistStoneNearby(World world, BlockPos pos) {
        AxisAlignedBB box = new AxisAlignedBB(pos).grow(5.0);
        for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, box)) {
            if (ItemAlchemistStone.isWearing(player)) return true;
        }
        return false;
    }

    @Unique
    private boolean moremod$canEnhanceAmplifier(ItemStack stack) {
        List<PotionEffect> effects = PotionUtils.getEffectsFromStack(stack);
        if (effects.isEmpty()) return false;
        for (PotionEffect e : effects) {
            if (e.getAmplifier() < MAX_EXTRA_AMPLIFIER) return true;
        }
        return false;
    }

    @Unique
    private boolean moremod$canExtendDuration(ItemStack stack) {
        List<PotionEffect> effects = PotionUtils.getEffectsFromStack(stack);
        if (effects.isEmpty()) return false;
        for (PotionEffect e : effects) {
            if (e.getDuration() < MAX_DURATION_TICKS) return true;
        }
        return false;
    }

    /**
     * 【核心修复】手动读取自定义药水效果列表
     * 1.12.2 没有 getCustomPotionEffects，我们自己解析 NBT。
     */
    @Unique
    private List<PotionEffect> moremod$getSourceEffects(ItemStack stack) {
        // 如果 NBT 中已经有自定义效果列表 (ID=9 表示 List)
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("CustomPotionEffects", 9)) {
            List<PotionEffect> list = new ArrayList<>();
            // 获取列表 (TagID 10 = Compound)
            NBTTagList tagList = stack.getTagCompound().getTagList("CustomPotionEffects", 10);

            for (int i = 0; i < tagList.tagCount(); ++i) {
                NBTTagCompound tag = tagList.getCompoundTagAt(i);
                // 1.12.2 提供了这个静态方法来从 NBT 读取单个效果
                PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(tag);
                if (effect != null) {
                    list.add(effect);
                }
            }
            return list;
        } else {
            // 如果还没有自定义效果（第一次强化），则从 PotionUtils 读取基础效果
            return PotionUtils.getEffectsFromStack(stack);
        }
    }

    @Unique
    private void moremod$enhanceAmplifier(ItemStack stack) {
        List<PotionEffect> current = moremod$getSourceEffects(stack);
        List<PotionEffect> next = new ArrayList<>();

        for (PotionEffect e : current) {
            int amp = Math.min(e.getAmplifier() + 1, MAX_EXTRA_AMPLIFIER);
            next.add(new PotionEffect(e.getPotion(), e.getDuration(), amp, e.getIsAmbient(), e.doesShowParticles()));
        }
        moremod$writeCustomEffects(stack, next);
    }

    @Unique
    private void moremod$extendDuration(ItemStack stack) {
        List<PotionEffect> current = moremod$getSourceEffects(stack);
        List<PotionEffect> next = new ArrayList<>();

        for (PotionEffect e : current) {
            int dur = Math.min((int)(e.getDuration() * 1.5), MAX_DURATION_TICKS);
            next.add(new PotionEffect(e.getPotion(), dur, e.getAmplifier(), e.getIsAmbient(), e.doesShowParticles()));
        }
        moremod$writeCustomEffects(stack, next);
    }

    @Unique
    private void moremod$writeCustomEffects(ItemStack stack, List<PotionEffect> effects) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();

        tag.removeTag("CustomPotionEffects");
        PotionUtils.addCustomPotionEffectToList(tag, effects);

        stack.setTagCompound(tag);
    }
}