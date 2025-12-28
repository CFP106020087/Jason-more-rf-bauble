package com.moremod.mixin;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.util.ElementUtil;
import com.moremod.event.MagicDamageAbsorbHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PotionCore 魔法加成拦截器（指数连击版）
 *
 * 功能：
 * 1. 元素武器 + 魔力吸收模块 → 中和魔力加成，转化为余灼
 * 2. 元素武器（无模块） → 中和魔力加成，不叠余灼
 * 3. 普通武器 + 魔力吸收模块 → 吸收法伤倍率偏移，叠加余灼与连击标记
 *
 * 连击机制：
 * - 余灼越高，触发概率越大（最高80%）
 * - 连击伤害指数增长（1.30^n，最高6倍）
 * - 20层余灼触发爆心（强制6连击）
 */
@Pseudo
@Mixin(targets = "com.tmtravlr.potioncore.PotionCoreEventHandler", remap = false)
public class MixinPotionCoreMagicDamage {

    /* ───────────────────────────────────────────────
     * ThreadLocal：记录攻击环境
     * ─────────────────────────────────────────────── */
    private static final ThreadLocal<Boolean> TL_IS_ELEMENTAL =
            ThreadLocal.withInitial(() -> false);

    private static final ThreadLocal<Integer> TL_MODULE_LEVEL =
            ThreadLocal.withInitial(() -> 0);

    private static final ThreadLocal<Float> TL_DAMAGE_BEFORE =
            ThreadLocal.withInitial(() -> 0f);

    // 吸收效率：每等级吸收 50% 的法伤偏移
    private static final float ABSORB_RATE_PER_LEVEL = 0.50f;

    /* ───────────────────────────────────────────────
     * HEAD：记录原伤害与战斗环境
     * ─────────────────────────────────────────────── */
    @Inject(method = "onLivingHurt", at = @At("HEAD"), remap = false, require = 0)
    private static void beforePotionCore(LivingHurtEvent event, CallbackInfo ci) {

        TL_IS_ELEMENTAL.set(false);
        TL_MODULE_LEVEL.set(0);
        TL_DAMAGE_BEFORE.set(event.getAmount());

        // 只关心「魔法伤害」
        if (!event.getSource().isMagicDamage()) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        ItemStack weapon = player.getHeldItemMainhand();
        if (weapon.isEmpty()) return;

        // ───────────────────────────
        // ① 检测元素武器
        // ───────────────────────────
        boolean isElemental = ElementUtil.isElementalWeapon(weapon) || ElementUtil.hasElementConversion(weapon);
        TL_IS_ELEMENTAL.set(isElemental);

        // ───────────────────────────
        // ② 检测魔力吸收模块
        // ───────────────────────────
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (!core.isEmpty()) {
            int level = ItemMechanicalCoreExtended.getUpgradeLevel(core, "MAGIC_ABSORB");
            if (level > 0) {
                TL_MODULE_LEVEL.set(level);
            }
        }
    }

    /* ───────────────────────────────────────────────
     * RETURN：处理法伤偏移、叠加余灼、标记连击
     * ─────────────────────────────────────────────── */
    @Inject(method = "onLivingHurt", at = @At("RETURN"), remap = false, require = 0)
    private static void afterPotionCore(LivingHurtEvent event, CallbackInfo ci) {

        try {
            boolean isElemental = TL_IS_ELEMENTAL.get();
            int moduleLevel = TL_MODULE_LEVEL.get();

            if (!isElemental && moduleLevel <= 0) return;
            if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

            float before = TL_DAMAGE_BEFORE.get();
            float after = event.getAmount();

            // PotionCore 的魔法放大倍率
            double magicMult = (before > 0) ? (after / before) : 1.0;

            /* ───────────────────────────
             * ① 元素武器（无模块）→ 仅中和魔力
             * ─────────────────────────── */
            if (isElemental && moduleLevel <= 0) {
                // 元素武器无模块：恢复原伤害，不叠余灼
                event.setAmount(before);
                return;
            }

            /* ───────────────────────────
             * ② 魔力吸收模块
             * ─────────────────────────── */
            if (moduleLevel > 0) {

                // 无魔力增幅 → 清空余灼，恢复原伤害
                if (magicMult <= 1.00001) {
                    MagicDamageAbsorbHandler.clearScorchTags(player);
                    event.setAmount(before);
                    return;
                }

                // 魔力偏移量（真实法伤倍率 - 1）
                double extraMagic = magicMult - 1.0;

                // 恢复原伤害（模块不吃法伤增益）
                event.setAmount(before);

                // ───────────────────────────
                // 余灼叠层
                // ───────────────────────────
                double absorbed = extraMagic * (ABSORB_RATE_PER_LEVEL * moduleLevel);
                int gain = 1 + (int) Math.floor(absorbed);

                int scorch = MagicDamageAbsorbHandler.getScorch(player);
                scorch += gain;

                if (scorch > MagicDamageAbsorbHandler.MAX_SCORCH) {
                    scorch = MagicDamageAbsorbHandler.MAX_SCORCH;
                }

                MagicDamageAbsorbHandler.setScorch(player, scorch);
                MagicDamageAbsorbHandler.setLastHitTime(player, player.world.getTotalWorldTime());

                // ───────────────────────────
                // 🔥 标记连击概率（提高触发率）
                // ───────────────────────────
                // 每层余灼提供 6% 概率，最高 80%
                float chainChance = Math.min(scorch * 0.06f, 0.80f);

                NBTTagCompound temp = player.getEntityData();
                temp.setFloat("mm_chain_chance", chainChance);

                // ───────────────────────────
                // 🔥 标记爆心连击（>=20层）
                // ───────────────────────────
                if (scorch >= MagicDamageAbsorbHandler.BURST_THRESHOLD) {
                    temp.setInteger("mm_burst_chains", 1); // 标记爆心
                    MagicDamageAbsorbHandler.setScorch(player, 0); // 清空余灼
                }
            }

        } finally {
            // cleanup
            TL_IS_ELEMENTAL.remove();
            TL_MODULE_LEVEL.remove();
            TL_DAMAGE_BEFORE.remove();
        }
    }
}