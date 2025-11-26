package com.moremod.synergy.core;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Synergy 事件处理器
 *
 * 监听 Forge 事件，并将事件转发给 SynergyManager 处理。
 * 这个类是完全独立的，不修改任何现有事件处理器。
 *
 * 注册方式：
 * MinecraftForge.EVENT_BUS.register(new SynergyEventHandler());
 *
 * 事件优先级说明：
 * - 使用 LOWEST 优先级，确保在其他模块处理完后再触发 Synergy
 * - 这样 Synergy 效果是"额外的"，不会干扰原有逻辑
 */
public class SynergyEventHandler {

    private final SynergyManager manager = SynergyManager.getInstance();

    // Tick 计数器，用于控制 tick synergy 的触发频率
    private static final int TICK_INTERVAL = 20; // 每秒触发一次

    /**
     * Synergy 系统专用的伤害源 - 用于识别并跳过我们自己造成的伤害，防止递归循环
     */
    public static final String SYNERGY_DAMAGE_TYPE = "synergy_bonus";

    /**
     * 创建一个 Synergy 专用的伤害源
     * @param player 造成伤害的玩家
     * @return 带有 synergy 标记的伤害源
     */
    public static DamageSource causeSynergyDamage(EntityPlayer player) {
        return new DamageSource(SYNERGY_DAMAGE_TYPE).setDamageBypassesArmor();
    }

    /**
     * 检查伤害源是否是 Synergy 系统造成的
     */
    public static boolean isSynergyDamage(DamageSource source) {
        return SYNERGY_DAMAGE_TYPE.equals(source.getDamageType());
    }

    /**
     * 玩家 Tick 事件
     *
     * 每秒检测一次 TICK 类型的 Synergy
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务端 END 阶段处理
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        // 每秒触发一次（每 20 tick）
        if (event.player.world.getTotalWorldTime() % TICK_INTERVAL != 0) return;

        if (!manager.isInitialized() || !manager.isEnabled()) return;

        try {
            manager.processEvent(
                    event.player,
                    SynergyEventType.TICK
            );
        } catch (Exception e) {
            if (manager.isDebugMode()) {
                System.err.println("[Synergy] Error in tick event: " + e.getMessage());
            }
        }
    }

    /**
     * 玩家攻击事件（造成伤害前）
     *
     * 优先级：LOWEST - 在所有其他处理器之后执行
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingAttack(LivingAttackEvent event) {
        // 跳过 Synergy 系统造成的伤害，防止递归循环
        if (isSynergyDamage(event.getSource())) return;

        // 检查攻击者是否为玩家
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        if (event.getSource().getTrueSource().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

        if (!manager.isInitialized() || !manager.isEnabled()) return;

        try {
            EntityLivingBase target = event.getEntityLiving();
            float damage = event.getAmount();

            manager.processEvent(
                    player,
                    SynergyEventType.ATTACK,
                    event,
                    target,
                    damage
            );
        } catch (Exception e) {
            if (manager.isDebugMode()) {
                System.err.println("[Synergy] Error in attack event: " + e.getMessage());
            }
        }
    }

    /**
     * 玩家造成伤害事件
     *
     * 优先级：LOWEST - 在伤害计算完成后触发
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingHurtByPlayer(LivingHurtEvent event) {
        // 跳过 Synergy 系统造成的伤害，防止递归循环
        if (isSynergyDamage(event.getSource())) return;

        // 检查攻击者是否为玩家
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        if (event.getSource().getTrueSource().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

        if (!manager.isInitialized() || !manager.isEnabled()) return;

        try {
            EntityLivingBase target = event.getEntityLiving();
            float damage = event.getAmount();

            // 处理普通攻击 Synergy
            int triggered = manager.processEvent(
                    player,
                    SynergyEventType.ATTACK,
                    event,
                    target,
                    damage
            );

            // 检测是否暴击（Minecraft 原版暴击判定）
            // 暴击条件：玩家在下落中、不在水中、不骑乘、不使用梯子
            if (player.fallDistance > 0.0F && !player.onGround && !player.isOnLadder()
                    && !player.isInWater() && !player.isRiding()) {
                manager.processEvent(
                        player,
                        SynergyEventType.CRITICAL_HIT,
                        event,
                        target,
                        damage
                );
            }

        } catch (Exception e) {
            if (manager.isDebugMode()) {
                System.err.println("[Synergy] Error in hurt event (attacker): " + e.getMessage());
            }
        }
    }

    /**
     * 玩家受伤事件
     *
     * 优先级：LOWEST - 在伤害计算完成后触发
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerHurt(LivingHurtEvent event) {
        // 检查受害者是否为玩家
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (!manager.isInitialized() || !manager.isEnabled()) return;

        try {
            EntityLivingBase attacker = null;
            if (event.getSource().getTrueSource() instanceof EntityLivingBase) {
                attacker = (EntityLivingBase) event.getSource().getTrueSource();
            }

            float damage = event.getAmount();

            // 判断伤害类型
            SynergyEventType eventType = SynergyEventType.HURT;

            // 检测环境伤害
            if (isEnvironmentalDamage(event.getSource())) {
                eventType = SynergyEventType.ENVIRONMENTAL_DAMAGE;
            }

            manager.processEvent(
                    player,
                    eventType,
                    event,
                    attacker,
                    damage
            );

        } catch (Exception e) {
            if (manager.isDebugMode()) {
                System.err.println("[Synergy] Error in hurt event (victim): " + e.getMessage());
            }
        }
    }

    /**
     * 玩家击杀实体事件
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDeath(LivingDeathEvent event) {
        // 检查击杀者是否为玩家
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        if (event.getSource().getTrueSource().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

        if (!manager.isInitialized() || !manager.isEnabled()) return;

        try {
            EntityLivingBase killed = event.getEntityLiving();

            manager.processEvent(
                    player,
                    SynergyEventType.KILL,
                    event,
                    killed,
                    0
            );

        } catch (Exception e) {
            if (manager.isDebugMode()) {
                System.err.println("[Synergy] Error in death event: " + e.getMessage());
            }
        }
    }

    /**
     * 玩家死亡事件
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        // 检查死亡者是否为玩家
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (!manager.isInitialized() || !manager.isEnabled()) return;

        try {
            EntityLivingBase killer = null;
            if (event.getSource().getTrueSource() instanceof EntityLivingBase) {
                killer = (EntityLivingBase) event.getSource().getTrueSource();
            }

            manager.processEvent(
                    player,
                    SynergyEventType.DEATH,
                    event,
                    killer,
                    0
            );

        } catch (Exception e) {
            if (manager.isDebugMode()) {
                System.err.println("[Synergy] Error in player death event: " + e.getMessage());
            }
        }
    }

    /**
     * 判断是否为环境伤害
     */
    private boolean isEnvironmentalDamage(net.minecraft.util.DamageSource source) {
        String type = source.getDamageType();
        return "inFire".equals(type)
                || "onFire".equals(type)
                || "lava".equals(type)
                || "drown".equals(type)
                || "fall".equals(type)
                || "inWall".equals(type)
                || "cactus".equals(type)
                || "lightningBolt".equals(type)
                || "hotFloor".equals(type)
                || "freeze".equals(type);
    }
}
