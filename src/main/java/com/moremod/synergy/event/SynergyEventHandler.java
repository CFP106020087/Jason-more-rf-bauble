package com.moremod.synergy.event;

import com.moremod.synergy.core.SynergyManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Synergy 事件处理器
 *
 * 说明：
 * - 监听关键事件，触发 Synergy 检测与效果应用
 * - 完全独立于现有的 Manager（Combat/Survival/Auxiliary/Energy）
 * - 不修改任何现有事件处理逻辑
 *
 * 重要：
 * - 这是 Synergy 系统唯一需要注册到事件总线的类
 * - 如果要移除 Synergy 包，只需在主 mod 类中移除此类的注册即可
 */
@Mod.EventBusSubscriber
public class SynergyEventHandler {

    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("synergy.debug", "false"));

    /**
     * 玩家 Tick 事件 - 处理被动 Synergy
     *
     * 优先级：NORMAL（不干扰其他系统）
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务端、Phase.END 时处理
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }

        EntityPlayer player = event.player;

        // 每秒处理一次被动 Synergy（减少性能消耗）
        if (player.world.getTotalWorldTime() % 20 == 0) {
            try {
                SynergyManager.getInstance().processSynergiesInTick(player);
            } catch (Exception e) {
                if (DEBUG) {
                    System.err.println("[SynergyEventHandler] Error in player tick:");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 玩家攻击实体事件
     *
     * 优先级：NORMAL（在原系统处理之后）
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onAttackEntity(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world.isRemote) {
            return;
        }

        try {
            SynergyManager.getInstance().processSynergiesInEvent(player, event);
        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("[SynergyEventHandler] Error in attack entity event:");
                e.printStackTrace();
            }
        }
    }

    /**
     * 生物受伤事件 - HIGH 优先级
     *
     * 说明：
     * - 在这里处理伤害修改类 Synergy
     * - HIGH 优先级确保在大部分其他 Mod 之后执行
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 只处理玩家造成的伤害
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) {
            return;
        }

        try {
            SynergyManager.getInstance().processSynergiesInEvent(player, event);
        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("[SynergyEventHandler] Error in living hurt event:");
                e.printStackTrace();
            }
        }
    }

    /**
     * 生物受攻击事件
     *
     * 优先级：NORMAL
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingAttack(LivingAttackEvent event) {
        // 只处理玩家造成的攻击
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) {
            return;
        }

        try {
            SynergyManager.getInstance().processSynergiesInEvent(player, event);
        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("[SynergyEventHandler] Error in living attack event:");
                e.printStackTrace();
            }
        }
    }

    /**
     * 玩家受伤事件 - 处理玩家作为受害者的 Synergy
     *
     * 优先级：NORMAL
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerHurt(LivingHurtEvent event) {
        // 只处理玩家受伤
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) {
            return;
        }

        try {
            SynergyManager.getInstance().processSynergiesInEvent(player, event);
        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("[SynergyEventHandler] Error in player hurt event:");
                e.printStackTrace();
            }
        }
    }

    /**
     * 注：可以根据需要添加更多事件监听器，例如：
     * - LivingDeathEvent（击杀时触发）
     * - PlayerPickupXpEvent（获取经验时触发）
     * - BlockEvent.BreakEvent（破坏方块时触发）
     * - 等等
     *
     * 只需确保优先级合理，不干扰现有系统即可
     */
}
