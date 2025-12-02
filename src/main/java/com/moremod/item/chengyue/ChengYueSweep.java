package com.moremod.item.chengyue;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

/**
 * 澄月 - 范围攻击系统
 * 
 * 取代初期的月华斩技能，作为被动范围伤害
 * 机制：
 * - 攻击主目标时，对周围敌人造成削弱的范围伤害
 * - 继承武器的所有附魔加成
 * - 随等级提升，削弱乘数降低（范围伤害更高）
 * - 使用自定义伤害源防止递归循环
 */
public class ChengYueSweep {
    
    /**
     * 自定义横扫伤害源
     * 用于标识范围攻击，防止递归触发
     */
    public static class ChengYueSweepDamage extends EntityDamageSource {
        public ChengYueSweepDamage(EntityPlayer player) {
            super("chengyue_sweep", player);
        }
        
        /**
         * 检查是否为澄月的范围伤害
         */
        public static boolean isSweepDamage(DamageSource source) {
            return source instanceof ChengYueSweepDamage;
        }
    }
    
    // ==================== 范围攻击参数（强化版）====================

    /**
     * 获取范围攻击半径（格）（强化版）
     * 公式：4.0 + level×0.25
     *
     * Level 0:  4.0格（原2.5格）
     * Level 10: 6.5格（原4.0格）
     * Level 30: 11.5格（原7.0格）
     * Level 50: 16.5格
     */
    public static float getSweepRange(int level) {
        return 4.0f + level * 0.25f;
    }

    /**
     * 获取伤害削弱乘数（强化版）
     * 公式：0.45 + level×0.02
     *
     * Level 0:  45%伤害（原30%）
     * Level 10: 65%伤害（原45%）
     * Level 20: 85%伤害（原60%）
     * Level 30: 100%伤害（原75%）
     */
    public static float getDamageMultiplier(int level) {
        float base = 0.45f + level * 0.02f;
        return Math.min(1.0f, base); // 上限100%（原80%）
    }

    /**
     * 获取击退强度（强化版，更早解锁）
     *
     * Level 0-4:   0.2击退（原无）
     * Level 5-14:  0.4击退（原0.3）
     * Level 15-24: 0.6击退（原0.5）
     * Level 25+:   0.9击退（原0.7）
     */
    public static float getKnockbackStrength(int level) {
        if (level < 5) return 0.2f;
        if (level < 15) return 0.4f;
        if (level < 25) return 0.6f;
        return 0.9f;
    }

    /**
     * 是否触发减速效果（强化版，更早解锁）
     * 10级以上：减速I，持续4秒（原15级，3秒）
     * 20级以上：减速II，持续4秒（原25级，3秒）
     * 35级以上：减速III，持续5秒（新增）
     */
    public static int getSlownessLevel(int level) {
        if (level >= 35) return 3;
        if (level >= 20) return 2;
        if (level >= 10) return 1;
        return 0;
    }

    /**
     * 获取减速持续时间（tick）
     */
    public static int getSlownessDuration(int level) {
        if (level >= 35) return 100; // 5秒
        return 80; // 4秒
    }

    /**
     * 获取最大目标数量（新增）
     * 限制范围攻击影响的最大目标数
     */
    public static int getMaxTargets(int level) {
        // 基础5个 + 每10级+2个，上限20个
        return Math.min(20, 5 + (level / 10) * 2);
    }
    
    // ==================== 范围攻击执行 ====================
    
    /**
     * 执行范围攻击
     * 
     * @param player 玩家
     * @param mainTarget 主要目标（被直接攻击的实体）
     * @param baseDamage 对主目标造成的伤害（已包含附魔加成）
     * @param weapon 使用的武器
     * @param level 澄月等级
     */
    public static void performSweepAttack(
        EntityPlayer player,
        EntityLivingBase mainTarget,
        float baseDamage,
        ItemStack weapon,
        int level
    ) {
        if (player.world.isRemote) return;
        
        // 获取范围和伤害倍率
        float range = getSweepRange(level);
        float damageMultiplier = getDamageMultiplier(level);
        
        // 计算范围伤害
        float sweepDamage = baseDamage * damageMultiplier;
        
        // 获取范围内的所有敌人
        AxisAlignedBB searchBox = new AxisAlignedBB(
            mainTarget.posX - range, mainTarget.posY - 1, mainTarget.posZ - range,
            mainTarget.posX + range, mainTarget.posY + 3, mainTarget.posZ + range
        );
        
        List<EntityLivingBase> targets = player.world.getEntitiesWithinAABB(
            EntityLivingBase.class,
            searchBox,
            entity -> entity != player &&
                     entity != mainTarget &&
                     !entity.isOnSameTeam(player) &&
                     entity.isEntityAlive()
        );

        // 获取最大目标数量
        int maxTargets = getMaxTargets(level);

        // 按距离排序，优先攻击近的
        targets.sort((a, b) -> Float.compare(
            (float) a.getDistanceSq(mainTarget),
            (float) b.getDistanceSq(mainTarget)
        ));

        // 对每个目标造成伤害
        int hitCount = 0;
        for (EntityLivingBase victim : targets) {
            if (hitCount >= maxTargets) break;

            // 使用自定义伤害源，防止递归
            boolean damaged = victim.attackEntityFrom(
                new ChengYueSweepDamage(player),
                sweepDamage
            );

            if (damaged) {
                hitCount++;

                // 应用击退效果
                float kbStrength = getKnockbackStrength(level);
                if (kbStrength > 0) {
                    applyKnockback(player, victim, kbStrength);
                }

                // 应用减速效果
                int slownessLevel = getSlownessLevel(level);
                if (slownessLevel > 0) {
                    int duration = getSlownessDuration(level);
                    victim.addPotionEffect(new PotionEffect(
                        MobEffects.SLOWNESS,
                        duration,
                        slownessLevel - 1 // 药水等级从0开始
                    ));
                }
            }
        }
        
        // 粒子特效
        if (hitCount > 0) {
            spawnSweepParticles(player, mainTarget, range, level);
        }
        
        // 音效
        if (hitCount > 0) {
            player.world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS,
                0.8f,
                1.0f + (level * 0.01f) // 音调随等级微调
            );
        }
    }
    
    /**
     * 应用击退效果
     */
    private static void applyKnockback(EntityPlayer player, EntityLivingBase victim, float strength) {
        double kbX = victim.posX - player.posX;
        double kbZ = victim.posZ - player.posZ;
        double distance = Math.sqrt(kbX * kbX + kbZ * kbZ);
        
        if (distance > 0.01) { // 防止除以0
            victim.knockBack(player, strength, -(kbX / distance), -(kbZ / distance));
        }
    }
    
    /**
     * 生成横扫粒子特效
     */
    private static void spawnSweepParticles(EntityPlayer player, EntityLivingBase target, float range, int level) {
        // 环形粒子（等级越高越密集）
        int particleCount = 12 + level * 2;
        
        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double offsetX = Math.cos(angle) * range * 0.8;
            double offsetZ = Math.sin(angle) * range * 0.8;
            
            double posX = target.posX + offsetX;
            double posY = target.posY + target.height / 2;
            double posZ = target.posZ + offsetZ;
            
            // 横扫特效
            player.world.spawnParticle(
                EnumParticleTypes.SWEEP_ATTACK,
                posX, posY, posZ,
                0, 0, 0
            );
            
            // 额外的魔法粒子（15级以上）
            if (level >= 15) {
                player.world.spawnParticle(
                    EnumParticleTypes.ENCHANTMENT_TABLE,
                    posX, posY, posZ,
                    (offsetX / range) * 0.2,
                    0.1,
                    (offsetZ / range) * 0.2
                );
            }
        }
        
        // 中心爆炸特效（25级以上）
        if (level >= 25) {
            for (int i = 0; i < 10; i++) {
                double vx = (player.world.rand.nextDouble() - 0.5) * 0.3;
                double vy = player.world.rand.nextDouble() * 0.3;
                double vz = (player.world.rand.nextDouble() - 0.5) * 0.3;
                
                player.world.spawnParticle(
                    EnumParticleTypes.CRIT,
                    target.posX, target.posY + target.height / 2, target.posZ,
                    vx, vy, vz
                );
            }
        }
    }
    
    // ==================== 显示信息 ====================
    
    /**
     * 获取范围攻击描述
     */
    public static String getSweepDescription(int level) {
        float range = getSweepRange(level);
        float damagePercent = getDamageMultiplier(level) * 100;
        float kbStrength = getKnockbackStrength(level);
        int slownessLevel = getSlownessLevel(level);
        int maxTargets = getMaxTargets(level);

        StringBuilder sb = new StringBuilder();
        sb.append("§6【范围攻击·强化版】\n");
        sb.append("§7范围: §f").append(String.format("%.1f", range)).append("格\n");
        sb.append("§7伤害: §c").append(String.format("%.0f%%", damagePercent)).append(" 主伤害\n");
        sb.append("§7目标上限: §f").append(maxTargets).append("个\n");

        if (kbStrength > 0) {
            sb.append("§7击退: §e").append(String.format("%.1f", kbStrength)).append("\n");
        }

        if (slownessLevel > 0) {
            int duration = getSlownessDuration(level) / 20;
            sb.append("§7减速: §b").append("Lv.").append(slownessLevel).append(" (").append(duration).append("秒)\n");
        }

        return sb.toString();
    }

    /**
     * 获取简短描述（用于Tooltip）
     */
    public static String getSweepTooltip(int level) {
        float range = getSweepRange(level);
        float damagePercent = getDamageMultiplier(level) * 100;
        int maxTargets = getMaxTargets(level);

        return String.format("§6范围攻击: §f%.1f格 / §c%.0f%%伤害 / §e%d目标", range, damagePercent, maxTargets);
    }
}
