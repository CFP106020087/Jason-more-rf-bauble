package com.moremod.system.ascension;

import com.moremod.config.BrokenGodConfig;
import com.moremod.moremod;
import com.moremod.system.humanity.AscensionRoute;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

/**
 * 破碎之神事件处理器
 * Broken God Event Handler
 *
 * 处理所有破碎之神相关的游戏事件：
 * - 战斗能力（暴击、真伤、无视无敌帧）
 * - 扭曲脉冲
 * - 停机模式
 * - 死亡拦截
 * - 怪物侦测距离减少
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class BrokenGodEventHandler {

    // ========== 玩家Tick处理 ==========

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // 更新停机模式
        BrokenGodHandler.tickShutdown(player);

        // 更新扭曲脉冲冷却
        BrokenGodHandler.tickPulseCooldown(player);

        // 固定人性值为0
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data != null && data.getHumanity() > 0) {
            data.setHumanity(0);
        }

        // 停机期间禁止移动
        if (BrokenGodHandler.isInShutdown(player)) {
            player.motionX = 0;
            player.motionZ = 0;
            player.motionY = Math.max(player.motionY, -0.1);
        }
    }

    // ========== 攻击处理 ==========

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        // 破碎之神受伤时的停机检测
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();

            if (BrokenGodHandler.isBrokenGod(player)) {
                // 停机期间无敌
                if (BrokenGodHandler.isInShutdown(player)) {
                    if (BrokenGodConfig.invulnerableDuringShutdown) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }

        // 破碎之神攻击时的效果
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer attacker = (EntityPlayer) event.getSource().getTrueSource();

            if (BrokenGodHandler.isBrokenGod(attacker)) {
                EntityLivingBase target = event.getEntityLiving();

                // 无视无敌帧
                if (BrokenGodConfig.bypassInvulnerability) {
                    target.hurtResistantTime = 0;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 破碎之神造成伤害
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer attacker = (EntityPlayer) event.getSource().getTrueSource();

            if (BrokenGodHandler.isBrokenGod(attacker)) {
                // 100% 暴击
                float damage = event.getAmount();
                damage *= BrokenGodConfig.critDamage;
                event.setAmount(damage);

                // 真实伤害（绕过护甲）
                if (BrokenGodConfig.trueDamage) {
                    // 我们通过设置伤害源为 OUT_OF_WORLD 类型来绕过护甲
                    // 但这会改变伤害类型，所以我们直接增加伤害来补偿护甲减免
                    EntityLivingBase target = event.getEntityLiving();
                    float armor = target.getTotalArmorValue();
                    float armorToughness = (float) target.getEntityAttribute(
                            net.minecraft.entity.SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue();
                    // 简化处理：增加护甲减免的伤害
                    float armorReduction = armor / 25f; // 大约护甲减免比例
                    event.setAmount(damage * (1 + armorReduction * 0.5f));
                }

                // 敌人困惑
                if (Math.random() < BrokenGodConfig.confusionChance) {
                    EntityLivingBase target = event.getEntityLiving();
                    target.addPotionEffect(new PotionEffect(MobEffects.NAUSEA,
                            BrokenGodConfig.confusionDuration, 0));
                    target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS,
                            BrokenGodConfig.confusionDuration, 1));

                    // 视觉效果
                    if (attacker.world instanceof WorldServer) {
                        WorldServer world = (WorldServer) attacker.world;
                        world.spawnParticle(EnumParticleTypes.PORTAL,
                                target.posX, target.posY + target.height / 2, target.posZ,
                                10, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }
        }

        // 破碎之神受到伤害 - 触发扭曲脉冲
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();

            if (BrokenGodHandler.isBrokenGod(player)) {
                float damage = event.getAmount();

                // 扭曲脉冲触发条件
                if (damage >= BrokenGodConfig.pulseTriggerDamage &&
                    BrokenGodHandler.canUseDistortionPulse(player)) {

                    triggerDistortionPulse(player);
                    BrokenGodHandler.triggerPulseCooldown(player);
                }

                // 检查是否需要进入停机模式
                float healthAfterDamage = player.getHealth() - damage;
                if (healthAfterDamage <= 0) {
                    // 拦截死亡，进入停机模式
                    event.setCanceled(true);
                    BrokenGodHandler.enterShutdown(player);
                }
            }
        }
    }

    // ========== 死亡拦截 ==========

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (BrokenGodHandler.isBrokenGod(player)) {
            // 拦截死亡
            event.setCanceled(true);

            // 如果还没在停机状态，进入停机
            if (!BrokenGodHandler.isInShutdown(player)) {
                BrokenGodHandler.enterShutdown(player);
            }

        }
    }

    // ========== 扭曲脉冲 ==========

    private static void triggerDistortionPulse(EntityPlayer player) {
        if (player.world.isRemote) return;

        double range = BrokenGodConfig.pulseRange;
        AxisAlignedBB aabb = new AxisAlignedBB(
                player.posX - range, player.posY - range, player.posZ - range,
                player.posX + range, player.posY + range, player.posZ + range
        );

        // 获取范围内所有实体
        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, aabb,
                e -> e != player && e.isEntityAlive()
        );

        for (EntityLivingBase entity : entities) {
            double dist = entity.getDistance(player);
            if (dist > range) continue;

            // 击退
            double knockbackStrength = 2.0 * (1 - dist / range);
            double dx = entity.posX - player.posX;
            double dz = entity.posZ - player.posZ;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0) {
                entity.motionX += (dx / len) * knockbackStrength;
                entity.motionY += 0.3;
                entity.motionZ += (dz / len) * knockbackStrength;
                entity.velocityChanged = true;
            }

            // 真实伤害
            entity.attackEntityFrom(DamageSource.OUT_OF_WORLD, (float) BrokenGodConfig.pulseDamage);

            // 短盲
            entity.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS,
                    BrokenGodConfig.pulseBlindnessDuration, 0));
        }

        // 消息
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "[扭曲脉冲] " +
                TextFormatting.GRAY + "释放异常能量波"
        ));

        // 音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 1.0f, 0.5f);

        // 粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            for (int i = 0; i < 50; i++) {
                double angle = (i / 50.0) * Math.PI * 2;
                for (double r = 1; r <= range; r += 2) {
                    double x = player.posX + Math.cos(angle) * r;
                    double z = player.posZ + Math.sin(angle) * r;
                    world.spawnParticle(EnumParticleTypes.PORTAL, x, player.posY + 0.5, z,
                            1, 0, 0, 0, 0.1);
                }
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

        if (!BrokenGodHandler.isBrokenGod(target)) return;

        // 减少侦测距离
        double normalRange = attacker.getEntityAttribute(
                net.minecraft.entity.SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        double reducedRange = normalRange * (1 - BrokenGodConfig.detectionReduction);
        double distance = attacker.getDistance(target);

        // 如果超出减少后的范围，取消目标
        if (distance > reducedRange) {
            attacker.setAttackTarget(null);
        }
    }

    // ========== 玩家退出清理 ==========

    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        BrokenGodHandler.cleanupPlayer(event.player.getUniqueID());
    }

    // ========== 玩家克隆（死亡重生） ==========

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 破碎之神状态通过 IHumanityData 的 copyFrom 保留
        // 这里不需要额外处理
    }
}
