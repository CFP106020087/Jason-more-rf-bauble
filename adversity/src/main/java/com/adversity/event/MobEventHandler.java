package com.adversity.event;

import com.adversity.affix.AffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.config.AdversityConfig;
import com.adversity.difficulty.DifficultyManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 怪物事件处理器 - 处理生成、攻击、受伤、死亡等事件
 */
public class MobEventHandler {

    /**
     * 实体加入世界时处理
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote) return;
        if (!(event.getEntity() instanceof EntityLiving)) return;

        EntityLiving entity = (EntityLiving) event.getEntity();

        // 排除玩家
        if (entity instanceof EntityPlayer) return;

        // 检查配置 - 是否应该处理此实体
        if (!AdversityConfig.shouldProcess(entity)) return;

        // 找最近的玩家
        EntityPlayer nearestPlayer = event.getWorld().getClosestPlayerToEntity(entity, 128);

        // 处理生成
        DifficultyManager.processSpawnedEntity(entity, nearestPlayer);
    }

    /**
     * 实体更新时处理（tick 词条效果）
     */
    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (event.getEntityLiving().world.isRemote) return;
        if (!(event.getEntityLiving() instanceof EntityLiving)) return;

        EntityLiving entity = (EntityLiving) event.getEntityLiving();
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);

        if (cap == null || cap.getAffixCount() == 0) return;

        // 处理每个词条的 tick
        for (AffixData data : cap.getAllAffixData()) {
            if (data.isActive()) {
                // 处理冷却
                data.decrementCooldown();
                data.incrementTick();

                // 调用词条的 tick 方法
                data.getAffix().onTick(entity, data);
            }
        }
    }

    /**
     * 实体攻击时处理
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote) return;

        DamageSource source = event.getSource();
        float damage = event.getAmount();

        // 检查攻击者是否有词条
        if (source.getTrueSource() instanceof EntityLiving) {
            EntityLiving attacker = (EntityLiving) source.getTrueSource();
            IAdversityCapability attackerCap = CapabilityHandler.getCapability(attacker);

            if (attackerCap != null && attackerCap.getAffixCount() > 0) {
                // 应用伤害倍率
                damage *= attackerCap.getDamageMultiplier();

                // 处理攻击型词条
                for (AffixData data : attackerCap.getAllAffixData()) {
                    if (data.isActive() && data.getCooldown() <= 0) {
                        damage = data.getAffix().onAttack(attacker, event.getEntityLiving(), damage, data);
                    }
                }
            }
        }

        // 检查被攻击者是否有词条
        if (event.getEntityLiving() instanceof EntityLiving) {
            EntityLiving target = (EntityLiving) event.getEntityLiving();
            IAdversityCapability targetCap = CapabilityHandler.getCapability(target);

            if (targetCap != null && targetCap.getAffixCount() > 0) {
                // 处理防御型词条
                for (AffixData data : targetCap.getAllAffixData()) {
                    if (data.isActive() && data.getCooldown() <= 0) {
                        damage = data.getAffix().onHurt(target, source, damage, data);
                    }
                }
            }
        }

        // 设置最终伤害
        event.setAmount(damage);
    }

    /**
     * 实体死亡时处理
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntityLiving().world.isRemote) return;
        if (!(event.getEntityLiving() instanceof EntityLiving)) return;

        EntityLiving entity = (EntityLiving) event.getEntityLiving();
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);

        if (cap == null || cap.getAffixCount() == 0) return;

        // 调用每个词条的死亡处理
        for (AffixData data : cap.getAllAffixData()) {
            data.getAffix().onDeath(entity, event.getSource(), data);
        }
    }
}
