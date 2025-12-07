package com.moremod.fabric.handler;

import com.moremod.fabric.data.UpdatedFabricPlayerData;
import com.moremod.fabric.data.UpdatedFabricPlayerData.FabricType;
import com.moremod.fabric.system.FabricWeavingSystem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 基础织布效果处理器
 * 处理便宜版织布的简单效果
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class BasicFabricHandler {

    // 属性修改器UUID
    private static final UUID RESILIENT_ARMOR_UUID = UUID.fromString("a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d");
    private static final UUID RESILIENT_KNOCKBACK_UUID = UUID.fromString("b2c3d4e5-f6a7-5b6c-9d0e-1f2a3b4c5d6e");
    private static final UUID VITAL_HEALTH_UUID = UUID.fromString("c3d4e5f6-a7b8-6c7d-0e1f-2a3b4c5d6e7f");
    private static final UUID LIGHT_SPEED_UUID = UUID.fromString("d4e5f6a7-b8c9-7d8e-1f2a-3b4c5d6e7f8a");
    private static final UUID PREDATOR_DAMAGE_UUID = UUID.fromString("e5f6a7b8-c9d0-8e9f-2a3b-4c5d6e7f8a9b");

    // 玩家上次的织布件数缓存
    private static final Map<UUID, Map<FabricType, Integer>> lastFabricCounts = new HashMap<>();

    /**
     * 玩家Tick事件 - 处理属性加成和被动效果
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 每20tick更新一次属性（1秒）
        if (player.ticksExisted % 20 != 0) return;

        // 统计各类织布件数
        int resilientCount = FabricWeavingSystem.countPlayerFabric(player, FabricType.RESILIENT);
        int vitalCount = FabricWeavingSystem.countPlayerFabric(player, FabricType.VITAL);
        int lightCount = FabricWeavingSystem.countPlayerFabric(player, FabricType.LIGHT);
        int predatorCount = FabricWeavingSystem.countPlayerFabric(player, FabricType.PREDATOR);
        int siphonCount = FabricWeavingSystem.countPlayerFabric(player, FabricType.SIPHON);

        // 更新属性
        updateResilientAttributes(player, resilientCount);
        updateVitalAttributes(player, vitalCount);
        updateLightAttributes(player, lightCount);
        updatePredatorAttributes(player, predatorCount);

        // 活力丝线被动回血（4件套，每5秒回1血）
        if (vitalCount >= 4 && player.ticksExisted % 100 == 0) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(1.0F);
            }
        }
    }

    /**
     * 坚韧纤维：+2护甲/件，4件套+10%击退抗性
     */
    private static void updateResilientAttributes(EntityPlayer player, int count) {
        IAttributeInstance armorAttr = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        IAttributeInstance knockbackAttr = player.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);

        // 移除旧修改器
        armorAttr.removeModifier(RESILIENT_ARMOR_UUID);
        knockbackAttr.removeModifier(RESILIENT_KNOCKBACK_UUID);

        if (count > 0) {
            // +2护甲/件
            armorAttr.applyModifier(new AttributeModifier(
                    RESILIENT_ARMOR_UUID, "Resilient Fiber Armor", count * 2.0, 0));

            // 4件套+10%击退抗性
            if (count >= 4) {
                knockbackAttr.applyModifier(new AttributeModifier(
                        RESILIENT_KNOCKBACK_UUID, "Resilient Fiber Knockback", 0.1, 0));
            }
        }
    }

    /**
     * 活力丝线：+1最大生命/件
     */
    private static void updateVitalAttributes(EntityPlayer player, int count) {
        IAttributeInstance healthAttr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);

        // 移除旧修改器
        healthAttr.removeModifier(VITAL_HEALTH_UUID);

        if (count > 0) {
            // +1最大生命/件
            healthAttr.applyModifier(new AttributeModifier(
                    VITAL_HEALTH_UUID, "Vital Thread Health", count * 1.0, 0));
        }
    }

    /**
     * 轻盈织物：+5%移动速度/件
     */
    private static void updateLightAttributes(EntityPlayer player, int count) {
        IAttributeInstance speedAttr = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);

        // 移除旧修改器
        speedAttr.removeModifier(LIGHT_SPEED_UUID);

        if (count > 0) {
            // +5%移动速度/件（使用乘法修改器）
            speedAttr.applyModifier(new AttributeModifier(
                    LIGHT_SPEED_UUID, "Light Weave Speed", count * 0.05, 2));
        }
    }

    /**
     * 掠食者布料：+5%攻击伤害/件
     */
    private static void updatePredatorAttributes(EntityPlayer player, int count) {
        IAttributeInstance damageAttr = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);

        // 移除旧修改器
        damageAttr.removeModifier(PREDATOR_DAMAGE_UUID);

        if (count > 0) {
            // +5%攻击伤害/件（使用乘法修改器）
            damageAttr.applyModifier(new AttributeModifier(
                    PREDATOR_DAMAGE_UUID, "Predator Cloth Damage", count * 0.05, 2));
        }
    }

    /**
     * 轻盈织物：4件套减少25%摔落伤害
     */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        int lightCount = FabricWeavingSystem.countPlayerFabric(player, FabricType.LIGHT);
        if (lightCount >= 4) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.75F);
        }
    }

    /**
     * 掠食者布料：4件套10%机率造成流血
     */
    @SubscribeEvent
    public static void onAttack(LivingHurtEvent event) {
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            int predatorCount = FabricWeavingSystem.countPlayerFabric(player, FabricType.PREDATOR);

            // 4件套10%机率造成流血（凋零效果模拟）
            if (predatorCount >= 4 && player.world.rand.nextFloat() < 0.1F) {
                EntityLivingBase target = event.getEntityLiving();
                target.addPotionEffect(new PotionEffect(MobEffects.WITHER, 60, 0)); // 3秒凋零
            }
        }
    }

    /**
     * 吸魂织带：击杀回复1饥饿值，4件套10%额外经验
     */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            int siphonCount = FabricWeavingSystem.countPlayerFabric(player, FabricType.SIPHON);

            if (siphonCount > 0) {
                // 回复饥饿值
                player.getFoodStats().addStats(1, 0.5F);

                // 4件套10%机率额外经验
                if (siphonCount >= 4 && player.world.rand.nextFloat() < 0.1F) {
                    EntityLivingBase victim = event.getEntityLiving();
                    if (!(victim instanceof EntityPlayer)) {
                        // 生成额外经验球
                        int bonusXP = 5 + player.world.rand.nextInt(10);
                        victim.world.spawnEntity(new net.minecraft.entity.item.EntityXPOrb(
                                victim.world, victim.posX, victim.posY, victim.posZ, bonusXP));
                    }
                }
            }
        }
    }
}
