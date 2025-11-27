package com.moremod.event;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.combat.TrueDamageHelper;
import com.moremod.config.BrokenRelicConfig;
import com.moremod.item.broken.*;
import com.moremod.moremod;
import com.moremod.system.ascension.BrokenGodHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 破碎终局饰品事件处理器
 * Broken Endgame Relic Event Handler
 *
 * 处理所有破碎饰品的战斗效果
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class BrokenRelicEventHandler {

    // ========== 伤害处理（攻击方） ==========

    /**
     * 处理玩家造成的伤害
     * 优先级 LOWEST 确保在所有其他修改之后
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDealDamage(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        // 必须是破碎之神才能触发效果
        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // 防止真伤处理递归
        EntityLivingBase target = event.getEntityLiving();
        if (TrueDamageHelper.isProcessingTrueDamage(target)) return;

        float baseDamage = event.getAmount();
        float modifiedDamage = baseDamage;
        float trueDamageToApply = 0;

        // ========== 破碎_眼: 必定暴击 + 护甲穿透 ==========
        if (hasBrokenEye(player)) {
            // 暴击伤害
            modifiedDamage *= ItemBrokenEye.getCritMultiplier();

            // 护甲穿透（增加伤害来模拟）
            modifiedDamage = TrueDamageHelper.calculateArmorBypassDamage(
                    modifiedDamage,
                    ItemBrokenEye.getArmorIgnoreRatio()
            );
        }

        // ========== 破碎_终结: 伤害放大 + 追加真伤 ==========
        if (hasBrokenTerminus(player)) {
            modifiedDamage *= ItemBrokenTerminus.getDamageMultiplier();
            trueDamageToApply += baseDamage * ItemBrokenTerminus.getTrueDamageRatio();
        }

        // 应用修改后的伤害
        event.setAmount(modifiedDamage);

        // ========== 破碎_手: 幻象打击真伤 ==========
        if (hasBrokenHand(player)) {
            trueDamageToApply += modifiedDamage * ItemBrokenHand.getPhantomDamageRatio();
        }

        // ========== 破碎_投影: 幻象分身 / 斩杀 ==========
        if (hasBrokenProjection(player)) {
            if (ItemBrokenProjection.canExecute(target)) {
                // 斩杀执行
                TrueDamageHelper.applyExecuteDamage(target, player);
                // 斩杀后不需要其他真伤
                trueDamageToApply = 0;
            } else {
                // 幻象分身
                trueDamageToApply += modifiedDamage * ItemBrokenProjection.getTwinDamageRatio();
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

        // ========== 破碎_心核: 生命汲取 ==========
        if (hasBrokenHeart(player)) {
            // 使用最终伤害（包括真伤）计算吸血
            float totalDamage = modifiedDamage + trueDamageToApply;
            ItemBrokenHeart.applyLifesteal(player, totalDamage);
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

        // ========== 破碎_枷锁: 伤害减免 ==========
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

    private static boolean hasBrokenEye(EntityPlayer player) {
        return hasBrokenItem(player, ItemBrokenEye.class);
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
