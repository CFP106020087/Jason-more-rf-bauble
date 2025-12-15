package com.moremod.mixin.brewing;

import com.moremod.item.ItemAlchemistStone;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 炼药师的术石 - 炼药台增强 (v2.5 修复版)
 * 修复了 CustomPotionEffects 无法正确写入 NBT 的问题
 */
@Mixin(TileEntityBrewingStand.class)
public abstract class MixinTileEntityBrewingStand {

    @Shadow
    private NonNullList<ItemStack> brewingItemStacks;

    @Shadow
    private int fuel;

    @Unique
    private static final int SLOT_INGREDIENT = 3;
    @Unique
    private static final int MAX_EXTRA_AMPLIFIER = 10; // 开放到 10 级
    @Unique
    private static final int MAX_DURATION_TICKS = 20 * 60 * 60; // 1小时

    // ======================== canBrew ========================

    @Inject(method = {"canBrew", "func_145934_k"}, at = @At("RETURN"), cancellable = true, remap = true)
    private void moremod$allowEnhancedBrewing(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (this.fuel <= 0) return;

        TileEntityBrewingStand te = (TileEntityBrewingStand) (Object) this;
        World world = te.getWorld();
        BlockPos pos = te.getPos();

        if (world == null || pos == null || this.brewingItemStacks.isEmpty()) return;
        if (!moremod$hasAlchemistStoneNearby(world, pos)) return;

        ItemStack ingredient = brewingItemStacks.get(SLOT_INGREDIENT);
        if (ingredient.isEmpty()) return;

        boolean glowstone = ingredient.getItem() == Items.GLOWSTONE_DUST;
        boolean redstone = ingredient.getItem() == Items.REDSTONE;
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
        TileEntityBrewingStand te = (TileEntityBrewingStand) (Object) this;
        World world = te.getWorld();
        BlockPos pos = te.getPos();

        if (world == null || pos == null || world.isRemote) return;
        if (!moremod$hasAlchemistStoneNearby(world, pos)) return;

        ItemStack ingredient = brewingItemStacks.get(SLOT_INGREDIENT);
        if (ingredient.isEmpty()) return;

        boolean glowstone = ingredient.getItem() == Items.GLOWSTONE_DUST;
        boolean redstone = ingredient.getItem() == Items.REDSTONE;
        if (!glowstone && !redstone) return;

        boolean didBrew = false;

        for (int i = 0; i < 3; i++) {
            ItemStack potion = brewingItemStacks.get(i);
            if (potion.isEmpty()) continue;

            // 优先原版配方
            if (BrewingRecipeRegistry.hasOutput(potion, ingredient)) continue;

            // 使用 copy 确保触发更新
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
     * 【核心逻辑】全量合并读取策略
     * 读取所有效果（原版+自定义），并放入 Map 去重，保留最强的。
     * 这样能确保我们是在"真实等级"的基础上进行强化。
     */
    @Unique
    private List<PotionEffect> moremod$getCleanEffects(ItemStack stack) {
        List<PotionEffect> all = PotionUtils.getEffectsFromStack(stack);
        Map<Potion, PotionEffect> merged = new LinkedHashMap<>();

        for (PotionEffect e : all) {
            Potion type = e.getPotion();
            if (merged.containsKey(type)) {
                PotionEffect existing = merged.get(type);
                // 保留等级更高或时间更长的
                if (e.getAmplifier() > existing.getAmplifier() ||
                        (e.getAmplifier() == existing.getAmplifier() && e.getDuration() > existing.getDuration())) {
                    merged.put(type, e);
                }
            } else {
                merged.put(type, e);
            }
        }
        return new ArrayList<>(merged.values());
    }

    @Unique
    private void moremod$enhanceAmplifier(ItemStack stack) {
        List<PotionEffect> current = moremod$getCleanEffects(stack);
        List<PotionEffect> next = new ArrayList<>();

        for (PotionEffect e : current) {
            int amp = Math.min(e.getAmplifier() + 1, MAX_EXTRA_AMPLIFIER);
            next.add(new PotionEffect(e.getPotion(), e.getDuration(), amp, e.getIsAmbient(), e.doesShowParticles()));
        }
        moremod$rewritePotionNBT(stack, next);
    }

    @Unique
    private void moremod$extendDuration(ItemStack stack) {
        List<PotionEffect> current = moremod$getCleanEffects(stack);
        List<PotionEffect> next = new ArrayList<>();

        for (PotionEffect e : current) {
            int dur = Math.min((int) (e.getDuration() * 1.5), MAX_DURATION_TICKS);
            next.add(new PotionEffect(e.getPotion(), dur, e.getAmplifier(), e.getIsAmbient(), e.doesShowParticles()));
        }
        moremod$rewritePotionNBT(stack, next);
    }

    /**
     * 【终极修复】重写 NBT：改ID、写效果、上色、改名
     * 修复：使用手动 NBT 写入代替不存在的 PotionUtils.addCustomPotionEffectToList
     */
    @Unique
    private void moremod$rewritePotionNBT(ItemStack stack, List<PotionEffect> effects) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();

        // 1. 手动写入 CustomPotionEffects（这是关键修复！）
        NBTTagList effectList = new NBTTagList();
        for (PotionEffect effect : effects) {
            effectList.appendTag(effect.writeCustomPotionEffectToNBT(new NBTTagCompound()));
        }
        tag.setTag("CustomPotionEffects", effectList);

        // 2. 将 ID 设为 "awkward" (粗制的药水)
        // 这步至关重要！它消除了原版 ID (如 strength_II) 对名字的强制控制
        // 使用 awkward (粗制) 比 water (水瓶) 更安全，不会被判定为水瓶
        tag.setString("Potion", "minecraft:awkward");

        // 3. 计算并写入正确的颜色 (防止变回粗制药水的蓝色)
        int color = PotionUtils.getPotionColorFromEffectList(effects);
        tag.setInteger("CustomPotionColor", color);

        stack.setTagCompound(tag);

        // 4. 手动修正名字 (例如: "力量药水 III")
        // 如果不加这步，名字会变成 "粗制的药水"，虽然效果是对的
        if (!effects.isEmpty()) {
            PotionEffect main = effects.get(0);
            String name = I18n.translateToLocal(main.getEffectName());
            String roman = moremod$getRomanNumeral(main.getAmplifier() + 1);

            // 使用白色(§f)来显示自定义名字
            stack.setStackDisplayName("§f" + name + " " + roman);
        }
    }

    /**
     * 罗马数字转换 (支持 1-11)
     */
    @Unique
    private String moremod$getRomanNumeral(int level) {
        switch (level) {
            case 0:
                return "";
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            case 6:
                return "VI";
            case 7:
                return "VII";
            case 8:
                return "VIII";
            case 9:
                return "IX";
            case 10:
                return "X";
            case 11:
                return "XI";
            default:
                return String.valueOf(level);
        }
    }
}