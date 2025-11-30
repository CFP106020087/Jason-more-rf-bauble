package com.moremod.system.ascension;

import com.moremod.config.ShambhalaConfig;
import com.moremod.item.shambhala.ItemShambhalaVeil;
import com.moremod.moremod;
import com.moremod.system.humanity.AscensionRoute;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 香巴拉事件处理器
 * Shambhala Event Handler
 *
 * 处理所有香巴拉相关的游戏事件：
 * - 能量护盾（伤害吸收）
 * - 反伤
 * - 死亡拦截
 * - 伤害输出削弱
 * - 高人性时间累计
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class ShambhalaEventHandler {

    private static final Logger LOGGER = LogManager.getLogger("moremod");

    // ========== 玩家Tick处理 ==========

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        // 未升格玩家：累计高人性时间
        if (data.getAscensionRoute() == AscensionRoute.NONE) {
            float humanity = data.getHumanity();
            if (humanity >= ShambhalaConfig.highHumanityThreshold) {
                data.addHighHumanityTicks(1);
            }
            return;
        }

        // 香巴拉玩家：固定人性值为100
        if (!ShambhalaHandler.isShambhala(player)) return;

        if (data.getHumanity() < 100) {
            data.setHumanity(100);
        }

        // 血量锁定检查
        ShambhalaHandler.tryLockHealth(player);
    }

    // ========== 伤害处理（防御核心） ==========

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 香巴拉受伤：能量护盾吸收
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();

            if (ShambhalaHandler.isShambhala(player)) {
                float originalDamage = event.getAmount();

                // 尝试用能量吸收伤害
                float remainingDamage = ShambhalaHandler.tryAbsorbDamage(player, originalDamage);

                if (remainingDamage <= 0) {
                    event.setCanceled(true);
                    return;
                }

                event.setAmount(remainingDamage);

                // 触发反伤
                if (event.getSource().getTrueSource() != null) {
                    ShambhalaHandler.reflectDamage(player, event.getSource().getTrueSource(), originalDamage);
                }
            }
        }

        // 香巴拉造成伤害：削弱输出
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer attacker = (EntityPlayer) event.getSource().getTrueSource();

            if (ShambhalaHandler.isShambhala(attacker)) {
                float reduction = (float) ShambhalaConfig.damageOutputReduction;
                event.setAmount(event.getAmount() * (1 - reduction));
            }
        }
    }

    // ========== 死亡拦截 ==========

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (ShambhalaHandler.isShambhala(player)) {
            // 检查是否有能量
            if (ShambhalaHandler.getCurrentEnergy(player) > 0) {
                // 拦截死亡
                event.setCanceled(true);

                // 锁定血量
                player.setHealth((float) ShambhalaConfig.coreHealthLock);

                // 消耗大量能量作为代价
                ShambhalaHandler.consumeEnergy(player, ShambhalaConfig.energyPerDamage * 10);

                player.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "[香巴拉] " +
                        TextFormatting.WHITE + "永恒齿轮庇佑...死亡被拒绝"
                ));

                LOGGER.info("[Shambhala] Intercepted death for player {}", player.getName());
            }
        }
    }

    // ========== 怪物侦测距离减少 ==========

    @SubscribeEvent
    public static void onLivingSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (!(event.getTarget() instanceof EntityPlayer)) return;
        if (!(event.getEntityLiving() instanceof EntityMob)) return;

        EntityPlayer target = (EntityPlayer) event.getTarget();
        EntityMob attacker = (EntityMob) event.getEntityLiving();

        if (!ShambhalaHandler.isShambhala(target)) return;

        // 减少侦测距离
        double normalRange = attacker.getEntityAttribute(
                net.minecraft.entity.SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        double reducedRange = normalRange * (1 - ShambhalaConfig.veilDetectionReduction);
        double distance = attacker.getDistance(target);

        // 如果超出减少后的范围，取消目标
        if (distance > reducedRange) {
            attacker.setAttackTarget(null);
        }

        // 潜行时完全隐身
        if (ShambhalaConfig.veilSneakInvisible && target.isSneaking()) {
            attacker.setAttackTarget(null);
        }
    }

    // ========== 玩家登录/退出 ==========

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data != null && data.getAscensionRoute() == AscensionRoute.SHAMBHALA) {
            ShambhalaHandler.registerShambhala(player);
            LOGGER.info("[Shambhala] Registered returning Shambhala player: {}", player.getName());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        ShambhalaHandler.cleanupPlayer(event.player.getUniqueID());
    }

    // ========== 世界卸载（防止跨存档数据污染） ==========

    @SubscribeEvent
    public static void onWorldUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) {
            ShambhalaHandler.clearAllState();
            LOGGER.info("[Shambhala] Cleared all static state on world unload");
        }
    }

    // ========== 玩家克隆（死亡重生） ==========

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 香巴拉状态通过 IHumanityData 的 copyFrom 保留
    }
}
