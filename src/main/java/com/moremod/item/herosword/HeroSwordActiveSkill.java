package com.moremod.item.herosword;

import com.moremod.combat.TrueDamageHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

/**
 * 终局审判 - 主动处决技能（修复版）
 * 
 * 纯处决，无削血，能力完全与成长挂钩
 * 修复了EntityLivingBase方法调用错误
 */
public class HeroSwordActiveSkill {

    /**
     * 施放终局审判
     */
    public static boolean tryCast(World world, EntityPlayer player, ItemStack stack) {
        if (world.isRemote) return false;
        
        // 获取成长参数
        int level = HeroSwordNBT.getLevel(stack);

        // 检查冷却
        long now = world.getTotalWorldTime();
        long last = HeroSwordNBT.getSkillCooldown(stack);
        long cooldown = HeroSwordStats.getExecuteCooldown(stack);

        if (now - last < cooldown) {
            return false;
        }

        // 设置冷却
        HeroSwordNBT.setSkillCooldown(stack, now);

        // 获取技能参数（全部与成长挂钩）
        float range = HeroSwordStats.getExecuteRange(stack);
        float threshold = HeroSwordStats.getExecuteThreshold(stack);
        int maxTargets = HeroSwordStats.getMaxExecuteTargets(stack);
        
        // 查找可处决目标
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                player.getEntityBoundingBox().grow(range, 3, range),
                e -> canBeExecuted(e, player, threshold)
        );

        // 按优先级排序：Boss > 低血量 > 距离近
        targets.sort((a, b) -> {
            boolean aBoss = HeroSwordStats.isBoss(a);
            boolean bBoss = HeroSwordStats.isBoss(b);
            if (aBoss != bBoss) return bBoss ? 1 : -1;
            
            float aRatio = a.getHealth() / a.getMaxHealth();
            float bRatio = b.getHealth() / b.getMaxHealth();
            if (Math.abs(aRatio - bRatio) > 0.1F) {
                return Float.compare(aRatio, bRatio);
            }
            
            float aDist = a.getDistance(player);
            float bDist = b.getDistance(player);
            return Float.compare(aDist, bDist);
        });

        // 执行处决
        int executed = 0;
        int bossExecuted = 0;
        
        for (EntityLivingBase target : targets) {
            if (executed >= maxTargets) break;
            
            boolean isBoss = HeroSwordStats.isBoss(target);
            if (executeTarget(world, player, target, stack)) {
                executed++;
                if (isBoss) bossExecuted++;
            }
        }

        // 反馈
        if (executed > 0) {
            // 史诗音效
            float pitch = 1.0F + (executed * 0.1F);
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS,
                    0.7F + (executed * 0.05F), pitch);

            // 处决信息
            String prefix;
            if (bossExecuted > 0) {
                prefix = TextFormatting.DARK_PURPLE + "【终局审判·弑神】";
            } else if (executed >= 5) {
                prefix = TextFormatting.LIGHT_PURPLE + "【终局审判·屠戮】";
            } else if (executed >= 3) {
                prefix = TextFormatting.RED + "【终局审判·制裁】";
            } else {
                prefix = TextFormatting.GOLD + "【终局审判】";
            }
            
            player.sendMessage(new TextComponentString(
                prefix + TextFormatting.WHITE + " 处决 " + executed + " 个目标！"
            ));
            
            // 显示成长提示
            if (level < 100 && player.world.rand.nextFloat() < 0.3F) {
                player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "（Lv." + level + 
                    " 处决上限:" + maxTargets + 
                    " 阈值:" + (int)(threshold*100) + "%）"
                ));
            }

            // 华丽特效
            if (world instanceof WorldServer) {
                spawnJudgmentEffect((WorldServer)world, player, range, executed, bossExecuted > 0);
            }
            
            // 成就奖励
            if (executed >= 5 || bossExecuted > 0) {
                HeroSwordNBT.addKillExp(stack, true);
            }
            
        } else {
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "没有符合审判条件的目标（需<" + 
                (int)(threshold * 100) + "%血量，范围" + (int)range + "格）"
            ));
        }

        return true;
    }

    /**
     * 判断目标是否可被处决（修复版）
     */
    private static boolean canBeExecuted(EntityLivingBase entity, EntityPlayer player, float threshold) {
        if (entity == null || entity == player) return false;
        if (entity.isDead || entity.getHealth() <= 0) return false;
        
        // 敌对生物直接可处决
        if (entity instanceof IMob) {
            float ratio = entity.getHealth() / entity.getMaxHealth();
            return ratio <= threshold;
        }
        
        // 中立生物需要有仇恨
        // 检查复仇目标
        if (entity.getRevengeTarget() == player) {
            float ratio = entity.getHealth() / entity.getMaxHealth();
            return ratio <= threshold;
        }
        
        // 如果是EntityLiving（大部分生物），检查攻击目标
        if (entity instanceof EntityLiving) {
            EntityLiving living = (EntityLiving) entity;
            if (living.getAttackTarget() == player) {
                float ratio = entity.getHealth() / entity.getMaxHealth();
                return ratio <= threshold;
            }
        }
        
        // 默认不处决（友好生物、其他玩家等）
        return false;
    }

    /**
     * 执行单体处决
     */
    private static boolean executeTarget(World world, EntityPlayer player,
                                        EntityLivingBase target, ItemStack stack) {
        // 使用包装的死亡链处决
        TrueDamageHelper.triggerVanillaDeathChain(target);

        if (target.isDead) {
            // 单体特效
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                
                // 处决印记
                for (int i = 0; i < 3; i++) {
                    ws.spawnParticle(
                        EnumParticleTypes.SPELL_MOB_AMBIENT,
                        target.posX, target.posY + target.height * (i+1)/3, target.posZ,
                        5, 0.2, 0.2, 0.2, 0.01
                    );
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 生成审判特效
     */
    private static void spawnJudgmentEffect(WorldServer world, EntityPlayer player, 
                                           float range, int count, boolean hadBoss) {
        // 审判之环
        for (int i = 0; i < 360; i += (hadBoss ? 10 : 20)) {
            double rad = Math.toRadians(i);
            double x = player.posX + Math.cos(rad) * range;
            double z = player.posZ + Math.sin(rad) * range;
            
            world.spawnParticle(
                hadBoss ? EnumParticleTypes.SPELL_WITCH : EnumParticleTypes.SPELL_MOB,
                x, player.posY, z,
                1, 0, 0.5, 0, 0.05
            );
        }
        
        // 天降审判
        if (count >= 3) {
            for (int i = 0; i < count * 3; i++) {
                double x = player.posX + (world.rand.nextDouble() - 0.5) * range * 2;
                double z = player.posZ + (world.rand.nextDouble() - 0.5) * range * 2;
                double y = player.posY + 5 + world.rand.nextDouble() * 3;
                
                world.spawnParticle(
                    EnumParticleTypes.SPELL_MOB_AMBIENT,
                    x, y, z,
                    1, 0, -0.5, 0.0, 0
                );
            }
        }
    }

    /**
     * 终局审判伤害源
     */

    
    // ==================== 兼容旧版本的方法 ====================
    
    /**
     * 获取处决阈值（兼容旧代码）
     * @deprecated 使用 HeroSwordStats.getExecuteThreshold(stack)
     */
    @Deprecated
    public static float getExecuteThreshold(int level) {
        // 简单映射
        if (level < 10) return 0.20F;
        if (level < 20) return 0.25F;
        if (level < 30) return 0.30F;
        if (level < 40) return 0.35F;
        if (level < 50) return 0.40F;
        return 0.45F;
    }
    
    /**
     * 获取处决伤害比例（已废弃）
     * @deprecated 现在直接处决，不需要比例
     */
    @Deprecated
    public static float getExecutePercent(int level) {
        return 1.0F;
    }
    
    /**
     * 获取冷却时间（兼容旧代码）
     * @deprecated 使用 HeroSwordStats.getExecuteCooldown(stack)
     */
    @Deprecated
    public static long getCooldownTicks(int level) {
        int seconds = Math.max(5, 20 - (level / 10));
        return seconds * 20L;
    }
}