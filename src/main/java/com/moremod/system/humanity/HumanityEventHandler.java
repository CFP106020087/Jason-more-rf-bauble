package com.moremod.system.humanity;

import com.moremod.config.HumanityConfig;
import com.moremod.moremod;
import com.moremod.system.FleshRejectionSystem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 人性值事件处理器
 * Humanity Event Handler
 *
 * 处理所有与人性值系统相关的游戏事件
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class HumanityEventHandler {

    // ========== 玩家Tick事件 ==========

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 检查是否需要激活人性值系统
        checkSystemActivation(player);

        // 更新人性值
        HumanitySpectrumSystem.updateHumanity(player);

        // 应用异常场效果
        HumanitySpectrumSystem.applyAnomalyFieldEffect(player);

        // 同步系统
        if (player instanceof EntityPlayerMP) {
            HumanitySpectrumSystem.tickSyncSystem((EntityPlayerMP) player);
            FleshRejectionSystem.tickSyncSystem((EntityPlayerMP) player);
        }
    }

    /**
     * 检查是否需要激活人性值系统
     * 当排异系统突破时自动激活
     */
    private static void checkSystemActivation(EntityPlayer player) {
        if (!HumanityConfig.enableHumanitySystem) return;

        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || data.isSystemActive()) return;

        // 检查排异系统是否已突破
        if (FleshRejectionSystem.hasTranscended(player)) {
            HumanitySpectrumSystem.activateSystem(player);
        }
    }

    // ========== 伤害事件处理 ==========

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 玩家造成伤害
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            handlePlayerAttack(event);
        }

        // 玩家受到伤害
        if (event.getEntityLiving() instanceof EntityPlayer) {
            handlePlayerDamaged(event);
        }
    }

    /**
     * 处理玩家攻击
     */
    private static void handlePlayerAttack(LivingHurtEvent event) {
        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLivingBase target = event.getEntityLiving();

        if (!HumanitySpectrumSystem.isSystemActive(player)) return;

        // 标记战斗状态
        HumanitySpectrumSystem.markCombat(player);

        float humanity = HumanitySpectrumSystem.getHumanity(player);
        float damageMultiplier = 1.0f;

        // 高人性：猎人协议
        if (humanity >= 40f) {
            damageMultiplier *= HumanitySpectrumSystem.calculateHunterProtocolDamageMultiplier(player, target);

            // 检查暴击
            if (HumanitySpectrumSystem.checkHunterProtocolCrit(player, target)) {
                damageMultiplier += 0.5f;
                // 暴击反馈
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        net.minecraft.init.SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                        net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 1.2f);
            }
        }

        // 低人性：异常协议
        if (humanity < 50f) {
            damageMultiplier *= HumanitySpectrumSystem.calculateAnomalyProtocolDamageMultiplier(player);

            // 检查畸变脉冲
            if (HumanitySpectrumSystem.checkDistortionPulse(player)) {
                HumanitySpectrumSystem.triggerDistortionPulse(player);
            }
        }

        // 崩解状态：攻击力翻倍
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data != null && data.isDissolutionActive()) {
            damageMultiplier *= 2.0f;

            // 反噬
            float backlash = player.getMaxHealth() * 0.05f;
            player.attackEntityFrom(net.minecraft.util.DamageSource.OUT_OF_WORLD, backlash);
        }

        event.setAmount(event.getAmount() * damageMultiplier);
    }

    /**
     * 处理玩家受伤
     */
    private static void handlePlayerDamaged(LivingHurtEvent event) {
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (!HumanitySpectrumSystem.isSystemActive(player)) return;

        // 标记战斗状态
        HumanitySpectrumSystem.markCombat(player);

        // 检查量子叠加（灰域致命伤害时）
        if (HumanitySpectrumSystem.checkQuantumCollapse(player, event.getAmount())) {
            event.setCanceled(true);
        }
    }

    // ========== 实体死亡事件 ==========

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        // 玩家击杀实体
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            EntityLivingBase target = event.getEntityLiving();

            if (!HumanitySpectrumSystem.isSystemActive(player)) return;

            // 处理样本掉落
            HumanitySpectrumSystem.handleEntityKill(player, target);

            // 击杀被动生物消耗人性
            HumanitySpectrumSystem.onKillPassiveMob(player, target);
        }
    }

    // ========== 治疗效果 ==========

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!HumanitySpectrumSystem.isSystemActive(player)) return;

        float humanity = HumanitySpectrumSystem.getHumanity(player);

        // 低人性：治疗效果降低
        if (humanity < 50f) {
            float reduction = (float) HumanityConfig.lowHumanityHealingReduction;
            event.setAmount(event.getAmount() * (1f - reduction));
        }
    }

    // ========== 睡眠事件 ==========

    @SubscribeEvent
    public static void onPlayerSleep(PlayerSleepInBedEvent event) {
        // 睡眠开始时不做特殊处理
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        if (!HumanitySpectrumSystem.isSystemActive(player)) return;

        // 检查是否是正常睡眠（不是被打断）
        if (!event.shouldSetSpawn()) return;

        // 睡眠恢复人性值
        HumanitySpectrumSystem.onPlayerSleep(player);
    }

    // ========== 交互事件 ==========

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote) return;

        EntityPlayer player = event.getEntityPlayer();
        if (!HumanitySpectrumSystem.isSystemActive(player)) return;

        // 与村民交互限制
        if (event.getTarget() instanceof EntityVillager) {
            float humanity = HumanitySpectrumSystem.getHumanity(player);

            if (humanity < HumanityConfig.npcInteractionThreshold) {
                event.setCanceled(true);
                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        "\u00a78\u00a7o村民无法理解你的存在..."), true);

                // 村民恐惧反应
                EntityVillager villager = (EntityVillager) event.getTarget();
                makeVillagerFlee(villager, player);
            }
        }

        // 喂养动物
        if (event.getTarget() instanceof EntityAnimal) {
            EntityAnimal animal = (EntityAnimal) event.getTarget();
            if (animal.isBreedingItem(event.getItemStack())) {
                HumanitySpectrumSystem.onFeedAnimal(player);
            }
        }
    }

    /**
     * 让村民逃跑
     */
    private static void makeVillagerFlee(EntityVillager villager, EntityPlayer player) {
        // 简单实现：给村民一个向远离玩家方向的推力
        double dx = villager.posX - player.posX;
        double dz = villager.posZ - player.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > 0) {
            villager.motionX += (dx / dist) * 0.5;
            villager.motionZ += (dz / dist) * 0.5;
        }

        // 恐惧粒子
        if (villager.world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer world = (net.minecraft.world.WorldServer) villager.world;
            world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.VILLAGER_ANGRY,
                    villager.posX, villager.posY + villager.getEyeHeight(), villager.posZ,
                    3, 0.2, 0.2, 0.2, 0
            );
        }
    }

    // ========== 玩家退出清理 ==========

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HumanitySpectrumSystem.cleanupPlayer(event.player.getUniqueID());
        FleshRejectionSystem.cleanupPlayer(event.player.getUniqueID());
    }
}
