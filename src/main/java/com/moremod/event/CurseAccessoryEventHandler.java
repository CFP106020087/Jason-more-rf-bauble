package com.moremod.event;

import com.moremod.core.CurseDeathHook;
import com.moremod.item.curse.ItemVoidGaze;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 七咒饰品事件处理器
 * 处理虚无之眸的经验获取加成等效果
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class CurseAccessoryEventHandler {

    /**
     * 虚无之眸经验获取加成
     * 佩戴虚无之眸时，经验获取+10%
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerPickupXp(PlayerPickupXpEvent event) {
        if (event.isCanceled()) return;

        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world.isRemote) return;

        // 检查是否佩戴虚无之眸
        if (!ItemVoidGaze.isWearing(player)) return;

        // 检查是否佩戴七咒之戒（联动效果需要）
        if (!CurseDeathHook.hasCursedRing(player)) return;

        EntityXPOrb orb = event.getOrb();
        if (orb == null) return;

        // 计算加成经验
        int originalXp = orb.xpValue;
        int bonusXp = (int) Math.ceil(originalXp * ItemVoidGaze.getXpBonus());

        if (bonusXp > 0) {
            // 直接增加经验值到经验球
            orb.xpValue = originalXp + bonusXp;
        }
    }
}
