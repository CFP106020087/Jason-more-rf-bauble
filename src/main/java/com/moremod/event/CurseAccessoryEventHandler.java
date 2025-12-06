package com.moremod.event;

import com.moremod.core.CurseDeathHook;
import com.moremod.item.curse.ItemGluttonousPhalanx;
import com.moremod.item.curse.ItemThornShard;
import com.moremod.item.curse.ItemVoidGaze;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 七咒饰品事件处理器
 * 处理七咒之戒联动饰品的各种效果
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class CurseAccessoryEventHandler {

    // ========== 虚无之眸：经验获取 +10% ==========

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerPickupXp(PlayerPickupXpEvent event) {
        if (event.isCanceled()) return;

        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world.isRemote) return;

        // 检查是否佩戴虚无之眸
        if (!ItemVoidGaze.isWearing(player)) return;

        // 检查是否佩戴七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        EntityXPOrb orb = event.getOrb();
        if (orb == null) return;

        // 计算加成经验
        int originalXp = orb.xpValue;
        int bonusXp = (int) Math.ceil(originalXp * ItemVoidGaze.getXpBonus());

        if (bonusXp > 0) {
            orb.xpValue = originalXp + bonusXp;
        }
    }

    // ========== 饕餮指骨：掠夺等级 +2 ==========

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLootingLevel(LootingLevelEvent event) {
        if (!(event.getDamageSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getDamageSource().getTrueSource();
        if (player.world.isRemote) return;

        // 检查是否佩戴饕餮指骨
        if (!ItemGluttonousPhalanx.isWearing(player)) return;

        // 检查是否佩戴七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 增加掠夺等级
        event.setLootingLevel(event.getLootingLevel() + ItemGluttonousPhalanx.getLootingBonus());
    }

    // ========== 荆棘碎片：受伤时反弹伤害 ==========

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (player.world.isRemote) return;

        // 检查是否佩戴七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        EntityLivingBase attacker = null;
        if (event.getSource().getTrueSource() instanceof EntityLivingBase) {
            attacker = (EntityLivingBase) event.getSource().getTrueSource();
        }

        // 荆棘碎片：反弹伤害
        if (ItemThornShard.isWearing(player) && attacker != null && attacker != player) {
            ItemThornShard.reflectDamage(player, attacker, event.getAmount());
        }
    }

    // ========== 荆棘碎片：攻击时自伤 ==========

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerAttack(LivingAttackEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        // 检查是否佩戴荆棘碎片
        if (!ItemThornShard.isWearing(player)) return;

        // 检查是否佩戴七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 攻击时自伤
        ItemThornShard.applySelfDamage(player, event.getAmount());
    }
}
