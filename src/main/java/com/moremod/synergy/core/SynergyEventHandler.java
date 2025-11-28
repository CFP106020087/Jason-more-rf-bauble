package com.moremod.synergy.core;

import com.moremod.synergy.bridge.ExistingModuleBridge;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Set;

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

    // ==================== 能量维护成本设置 ====================

    /** 能量维护消耗间隔（tick），每5秒消耗一次 */
    private static final int ENERGY_DRAIN_INTERVAL = 100; // 5秒

    /**
     * 每个激活的 Synergy 的基础能量成本（RF/5秒）
     * 对比模块消耗：护甲强化 10 RF/s, 护盾 20 RF/s, 飞行 25 RF/s
     * 单个 Synergy 约等于一个中等模块的消耗
     */
    private static final int BASE_ENERGY_COST_PER_SYNERGY = 25;

    /**
     * 每个额外 Synergy 的递增成本（RF/5秒）
     * 激活越多 Synergy，边际成本越高，鼓励玩家精选组合
     */
    private static final int SCALING_COST_PER_SYNERGY = 5;

    /** 能量不足时是否自动停用 Synergy */
    private static final boolean AUTO_DEACTIVATE_ON_LOW_ENERGY = true;

    /** 低能量警告阈值（百分比） */
    private static final float LOW_ENERGY_WARNING_THRESHOLD = 10.0f;

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
     * 每5秒处理一次能量维护消耗
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务端 END 阶段处理
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        if (!manager.isInitialized() || !manager.isEnabled()) return;

        long worldTime = event.player.world.getTotalWorldTime();

        // 每秒触发一次（每 20 tick）处理 Synergy TICK 事件
        if (worldTime % TICK_INTERVAL == 0) {
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

        // 每5秒处理一次 Synergy 能量维护消耗
        if (worldTime % ENERGY_DRAIN_INTERVAL == 0) {
            processSynergyEnergyMaintenance(event.player);
        }
    }

    /**
     * 处理 Synergy 系统的能量维护消耗
     *
     * 公式：totalCost = synergyCount * (BASE_COST + synergyCount * SCALING_COST)
     * 示例：
     * - 1 synergy:  1 * (25 + 1*5)  =  30 RF/5s =   6 RF/s
     * - 3 synergies: 3 * (25 + 3*5)  = 120 RF/5s =  24 RF/s
     * - 5 synergies: 5 * (25 + 5*5)  = 250 RF/5s =  50 RF/s
     * - 10 synergies: 10 * (25 + 10*5) = 750 RF/5s = 150 RF/s
     *
     * 对比模块消耗：护甲强化 10 RF/s, 护盾 20 RF/s, 飞行 25 RF/s
     *
     * @param player 玩家
     */
    private void processSynergyEnergyMaintenance(EntityPlayer player) {
        try {
            // 获取玩家已激活的 Synergy
            Set<String> activeSynergies = manager.getActivatedSynergiesForPlayer(player);
            int synergyCount = activeSynergies.size();

            // 无激活的 Synergy，无需消耗
            if (synergyCount == 0) return;

            // 计算能量消耗：递增式成本
            int totalCost = synergyCount * (BASE_ENERGY_COST_PER_SYNERGY + synergyCount * SCALING_COST_PER_SYNERGY);

            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
            int currentEnergy = bridge.getCurrentEnergy(player);
            float energyPercent = bridge.getEnergyPercent(player);

            // 检查能量是否足够
            if (currentEnergy < totalCost) {
                // 能量不足
                if (AUTO_DEACTIVATE_ON_LOW_ENERGY) {
                    // 自动停用所有 Synergy
                    manager.deactivateAllSynergiesForPlayer(player);

                    // 通知玩家
                    player.sendStatusMessage(
                            new TextComponentString(TextFormatting.RED + "[Synergy] " +
                                    TextFormatting.YELLOW + "能量不足，所有协同效应已停用"),
                            true);

                    if (manager.isDebugMode()) {
                        System.out.println("[Synergy] Deactivated all synergies for " + player.getName() +
                                " due to insufficient energy (" + currentEnergy + "/" + totalCost + " RF)");
                    }
                }
                return;
            }

            // 消耗能量
            bridge.consumeEnergy(player, totalCost);

            // 低能量警告
            float newEnergyPercent = bridge.getEnergyPercent(player);
            if (newEnergyPercent <= LOW_ENERGY_WARNING_THRESHOLD && energyPercent > LOW_ENERGY_WARNING_THRESHOLD) {
                // 刚刚跌破警告阈值，提示玩家
                player.sendStatusMessage(
                        new TextComponentString(TextFormatting.GOLD + "[Synergy] " +
                                TextFormatting.YELLOW + "能量低于 " + (int) LOW_ENERGY_WARNING_THRESHOLD + "%，协同效应消耗中..."),
                        true);
            }

            if (manager.isDebugMode()) {
                System.out.println("[Synergy] Energy maintenance: " + player.getName() +
                        " | Synergies: " + synergyCount +
                        " | Cost: " + totalCost + " RF" +
                        " | Remaining: " + bridge.getCurrentEnergy(player) + " RF");
            }

        } catch (Exception e) {
            if (manager.isDebugMode()) {
                System.err.println("[Synergy] Error in energy maintenance: " + e.getMessage());
            }
        }
    }

    /**
     * 计算 Synergy 能量维护成本（供外部查询）
     *
     * @param synergyCount 激活的 Synergy 数量
     * @return 每5秒的能量消耗
     */
    public static int calculateMaintenanceCost(int synergyCount) {
        if (synergyCount <= 0) return 0;
        return synergyCount * (BASE_ENERGY_COST_PER_SYNERGY + synergyCount * SCALING_COST_PER_SYNERGY);
    }

    /**
     * 获取能量消耗间隔（tick）
     */
    public static int getEnergyDrainInterval() {
        return ENERGY_DRAIN_INTERVAL;
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
     * 玩家登出事件 - 清理激活的 Synergy
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player.world.isRemote) return;

        // 清理玩家的激活数据
        manager.cleanupPlayer(event.player.getUniqueID());
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
