package com.moremod.event;

import com.moremod.core.CurseDeathHook;
import com.moremod.item.curse.ItemGluttonousPhalanx;
import com.moremod.item.curse.ItemVoidGaze;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 七咒饰品事件处理器
 * 处理七咒之戒联动饰品的各种效果
 *
 * 注意：荆棘碎片和怨念结晶的效果在各自的类中处理
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

    // 注意：荆棘碎片的事件处理在 ItemThornShard 类内部
    // 注意：怨念结晶的真伤光环在 ItemCrystallizedResentment.onWornTick 中处理
}
