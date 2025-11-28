package com.moremod.event;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.combat.TrueDamageHelper;
import com.moremod.item.broken.*;
import com.moremod.moremod;
import com.moremod.system.ascension.BrokenGodHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 破碎终局饰品事件处理器
 * Broken Endgame Relic Event Handler
 *
 * 处理所有破碎饰品的战斗效果
 *
 * 设计说明：
 * - LivingHurtEvent LOWEST: 应用伤害倍率（暴击×3、狂战士、终结×2）
 * - LivingDamageEvent LOWEST: 基于最终伤害计算真伤（投影100%真伤）
 * - AttackEntityEvent: 重置攻击冷却（手）
 * - LivingDeathEvent: 击杀效果（终结）
 * - 护甲粉碎: 由ItemBrokenArm的onWornTick光环处理
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class BrokenRelicEventHandler {

    // ========== 伤害处理 第一阶段：伤害倍率 ==========

    /**
     * 处理玩家造成的伤害 - 应用伤害倍率
     * 优先级 LOWEST 确保在其他修改之后
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDealDamage(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        // 必须是破碎之神才能触发效果
        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // 如果是我们的真伤伤害源，不要再叠加效果（防止递归）
        if (TrueDamageHelper.isTrueDamageSource(event.getSource())) return;
        if (TrueDamageHelper.isInTrueDamageContext()) return;

        EntityLivingBase target = event.getEntityLiving();
        if (TrueDamageHelper.isProcessingTrueDamage(target)) return;

        float modifiedDamage = event.getAmount();

        // ========== 破碎_臂: 伤害×2 + 必定暴击×3 ==========
        // 护甲粉碎效果由ItemBrokenArm的光环处理（周围敌人护甲归零）
        if (hasBrokenArm(player)) {
            // 基础伤害倍率 ×2
            modifiedDamage *= ItemBrokenArm.getDamageMultiplier();
            // 暴击伤害 ×3
            modifiedDamage *= ItemBrokenArm.getCritMultiplier();
        }

        // ========== 破碎_心核: 狂战士（血量越低伤害越高，最高×5） ==========
        if (hasBrokenHeart(player)) {
            float berserkerMultiplier = ItemBrokenHeart.getBerserkerMultiplier(player);
            modifiedDamage *= berserkerMultiplier;
        }

        // ========== 破碎_终结: 伤害×2 ==========
        if (hasBrokenTerminus(player)) {
            modifiedDamage *= ItemBrokenTerminus.getDamageMultiplier();
        }

        // 应用修改后的伤害
        event.setAmount(modifiedDamage);
    }

    // ========== 伤害处理 第二阶段：真伤（基于最终伤害） ==========

    /**
     * 处理玩家造成的最终伤害 - 应用真伤
     * 优先级 LOWEST 确保获取到所有增伤后的最终数值
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDealFinalDamage(LivingDamageEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        // 必须是破碎之神才能触发效果
        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // 如果是我们的真伤伤害源，不要再叠加效果（防止递归）
        if (TrueDamageHelper.isTrueDamageSource(event.getSource())) return;
        if (TrueDamageHelper.isInTrueDamageContext()) return;

        EntityLivingBase target = event.getEntityLiving();
        if (TrueDamageHelper.isProcessingTrueDamage(target)) return;

        // 获取最终伤害数值（经过所有护甲、附魔、增伤计算后）
        float finalDamage = event.getAmount();
        float trueDamageToApply = 0;

        // ========== 破碎_投影: 幻影打击 / 斩杀执行 ==========
        if (hasBrokenProjection(player)) {
            if (ItemBrokenProjection.canExecute(target)) {
                // 斩杀执行 - 目标 <50% HP 直接击杀
                TrueDamageHelper.applyExecuteDamage(target, player);
                // 斩杀后不需要其他真伤
                trueDamageToApply = 0;
            } else {
                // 幻影打击 - 100%额外真伤
                trueDamageToApply += finalDamage * ItemBrokenProjection.getTrueDamageRatio();
            }
        }

        // 应用真伤
        if (trueDamageToApply > 0) {
            TrueDamageHelper.applyWrappedTrueDamage(
                    target,
                    player,
                    trueDamageToApply,
                    TrueDamageHelper.TrueDamageFlag.PHANTOM_TWIN
            );
        }

        // ========== 破碎_心核: 生命汲取（100%吸血） ==========
        if (hasBrokenHeart(player)) {
            // 使用最终伤害（包括真伤）计算吸血
            float totalDamage = finalDamage + trueDamageToApply;
            ItemBrokenHeart.applyLifesteal(player, totalDamage);
        }
    }

    // ========== 攻击事件：重置冷却 ==========

    /**
     * 攻击后重置冷却（破碎_手）
     */
    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // ========== 破碎_手: 攻击后重置冷却 ==========
        if (hasBrokenHand(player) && ItemBrokenHand.shouldResetCooldown()) {
            // 重置攻击冷却
            player.resetCooldown();
        }
    }

    // ========== 击杀事件：终结效果 ==========

    /**
     * 击杀敌人时触发终结效果
     */
    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // ========== 破碎_终结: 死亡收割 ==========
        if (hasBrokenTerminus(player)) {
            ItemBrokenTerminus.applyKillEffect(player);
        }
    }

    // ========== 伤害处理（受击方） ==========

    /**
     * 处理玩家受到的伤害
     * 优先级 HIGH 在伤害计算早期减免
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerTakeDamage(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        if (!BrokenGodHandler.isBrokenGod(player)) return;

        float damage = event.getAmount();

        // ========== 破碎_枷锁: 伤害减免50% ==========
        if (hasBrokenShackles(player)) {
            float reduction = ItemBrokenShackles.getDamageReduction();
            damage *= (1.0f - reduction);
        }

        event.setAmount(damage);
    }

    // ========== 饰品检测工具方法 ==========

    private static boolean hasBrokenHeart(EntityPlayer player) {
        return hasBrokenItem(player, ItemBrokenHeart.class);
    }

    private static boolean hasBrokenArm(EntityPlayer player) {
        return hasBrokenItem(player, ItemBrokenArm.class);
    }

    private static boolean hasBrokenHand(EntityPlayer player) {
        return hasBrokenItem(player, ItemBrokenHand.class);
    }

    private static boolean hasBrokenShackles(EntityPlayer player) {
        return hasBrokenItem(player, ItemBrokenShackles.class);
    }

    private static boolean hasBrokenProjection(EntityPlayer player) {
        return hasBrokenItem(player, ItemBrokenProjection.class);
    }

    private static boolean hasBrokenTerminus(EntityPlayer player) {
        return hasBrokenItem(player, ItemBrokenTerminus.class);
    }

    /**
     * 检查玩家是否装备了指定的破碎饰品
     */
    private static boolean hasBrokenItem(EntityPlayer player, Class<?> itemClass) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return false;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) {
                return true;
            }
        }
        return false;
    }
}
