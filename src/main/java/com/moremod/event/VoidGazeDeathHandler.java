package com.moremod.event;

import com.moremod.core.CurseDeathHook;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 虚无之眸死亡保护事件处理器
 * 使用 Forge 事件系统实现死亡保护（不依赖 ASM）
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class VoidGazeDeathHandler {

    /**
     * 在伤害应用前检测致命伤害
     * 使用 HIGHEST 优先级确保在其他处理器之前执行
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof EntityPlayer)) return;
        if (event.getEntity().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntity();
        float damage = event.getAmount();
        float currentHealth = player.getHealth();

        // 检测致命伤害（当前血量 <= 伤害量）
        if (currentHealth <= damage) {
            // 检查是否有虚无之眸和七咒之戒
            if (!CurseDeathHook.hasVoidGaze(player)) return;
            if (!CurseDeathHook.hasCursedRing(player)) return;

            // 尝试用虚无之眸阻止死亡
            if (CurseDeathHook.tryPreventDeath(player, event.getSource())) {
                // 成功阻止，取消伤害
                event.setCanceled(true);
            }
        }
    }

    /**
     * 最终防线：在死亡事件触发时再次检查
     * 使用 HIGHEST 优先级确保在其他处理器之前执行
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof EntityPlayer)) return;
        if (event.getEntity().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntity();

        // 检查是否有虚无之眸和七咒之戒
        if (!CurseDeathHook.hasVoidGaze(player)) return;
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 最终防线：尝试阻止死亡
        if (CurseDeathHook.tryPreventDeath(player, event.getSource())) {
            event.setCanceled(true);
            // 确保玩家血量恢复
            if (player.getHealth() <= 0) {
                player.setHealth(4.0f);
            }
        }
    }
}
