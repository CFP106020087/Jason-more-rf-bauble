package com.moremod.item.sawblade;

import com.moremod.combat.TrueDamageHelper;
import com.moremod.item.sawblade.potion.PotionBloodEuphoria;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

/**
 * 处决收割 - 主动技能
 * 
 * AOE处决低血量+出血目标
 * 每次击杀延长技能时间（连杀机制）
 */
public class SawBladeActiveSkill {
    
    /**
     * 尝试施放处决收割
     */
    public static boolean tryCast(World world, EntityPlayer player, ItemStack stack) {
        if (world.isRemote) return false;
        
        // 检查冷却
        long now = world.getTotalWorldTime();
        long last = SawBladeNBT.getSkillCooldown(stack);
        long cooldown = SawBladeStats.getExecuteCooldown(stack);
        
        if (now - last < cooldown) {
            long remaining = (cooldown - (now - last)) / 20;
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.GRAY + "冷却中... " + remaining + "秒"
            ), true);
            return false;
        }
        
        // 设置冷却
        SawBladeNBT.setSkillCooldown(stack, now);
        
        // 获取参数
        float range = SawBladeStats.getExecuteRange(stack);
        float threshold = SawBladeStats.getExecuteThreshold(stack);
        int maxTargets = SawBladeStats.getExecuteMaxTargets(stack);
        
        // 查找目标
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(
            EntityLivingBase.class,
            player.getEntityBoundingBox().grow(range, 3, range),
            e -> canExecute(e, player, threshold)
        );
        
        if (targets.isEmpty()) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "没有可处决的目标（需<" + 
                (int)(threshold * 100) + "%血量或高出血值）"
            ));
            return false;
        }
        
        // 排序：Boss > 低血量 > 高出血 > 距离近
        targets.sort((a, b) -> {
            boolean aBoss = SawBladeStats.isBoss(a);
            boolean bBoss = SawBladeStats.isBoss(b);
            if (aBoss != bBoss) return bBoss ? 1 : -1;
            
            float aHP = a.getHealth() / a.getMaxHealth();
            float bHP = b.getHealth() / b.getMaxHealth();
            if (Math.abs(aHP - bHP) > 0.1f) return Float.compare(aHP, bHP);
            
            float aBleed = a.getEntityData().getFloat("moremod_bleed_buildup");
            float bBleed = b.getEntityData().getFloat("moremod_bleed_buildup");
            if (Math.abs(aBleed - bBleed) > 20.0f) return Float.compare(bBleed, aBleed);
            
            return Float.compare(a.getDistance(player), b.getDistance(player));
        });
        
        // 执行处决
        int executed = 0;
        int bossExecuted = 0;
        long chainTime = now;
        int chainExtension = SawBladeStats.getExecuteChainExtension(stack);
        
        for (EntityLivingBase target : targets) {
            // 连杀机制：每击杀延长时间
            if (world.getTotalWorldTime() - chainTime > chainExtension * 20L) {
                break;  // 超时，停止连杀
            }
            
            if (executed >= maxTargets) break;
            
            boolean isBoss = SawBladeStats.isBoss(target);
            if (executeTarget(world, player, target, stack)) {
                executed++;
                if (isBoss) bossExecuted++;
                chainTime = world.getTotalWorldTime();  // 刷新连杀时间
                
                // 触发鲜血欢愉
                PotionBloodEuphoria.applyEffect(player, stack);
            }
        }
        
        // 反馈
        if (executed > 0) {
            String prefix = getExecutePrefix(executed, bossExecuted);
            player.sendMessage(new TextComponentString(
                prefix + TextFormatting.WHITE + " 处决 " + executed + " 个目标！"
            ));
            
            // 音效
            float pitch = 1.0f + (executed * 0.1f);
            world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS,
                0.6f + (executed * 0.05f), pitch);
            
            // 特效
            if (world instanceof WorldServer) {
                spawnExecuteEffect((WorldServer)world, player, range, executed, bossExecuted > 0);
            }
            
            // 统计
            for (int i = 0; i < executed; i++) {
                SawBladeNBT.addExecute(stack);
            }
        }
        
        return true;
    }
    
    /**
     * 判断是否可处决
     */
    private static boolean canExecute(EntityLivingBase entity, EntityPlayer player, float threshold) {
        if (entity == null || entity == player) return false;
        if (entity.isDead || entity.getHealth() <= 0) return false;
        if (!(entity instanceof IMob)) return false;
        
        // 条件1：低血量
        float hpRatio = entity.getHealth() / entity.getMaxHealth();
        if (hpRatio <= threshold) return true;
        
        // 条件2：高出血值（>70）
        float bleed = entity.getEntityData().getFloat("moremod_bleed_buildup");
        return bleed > 70.0f;
    }
    
    /**
     * 执行单体处决
     */
    private static boolean executeTarget(World world, EntityPlayer player,
                                        EntityLivingBase target, ItemStack stack) {
        // 使用包装的死亡链处决
        TrueDamageHelper.triggerVanillaDeathChain(target);

        if (target.isDead) {
            // 特效
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int i = 0; i < 5; i++) {
                    ws.spawnParticle(
                        EnumParticleTypes.SWEEP_ATTACK,
                        target.posX, target.posY + target.height * 0.5, target.posZ,
                        3, 0.3, 0.3, 0.3, 0.0
                    );
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * 获取处决前缀
     */
    private static String getExecutePrefix(int count, int bossCount) {
        if (bossCount > 0) {
            return TextFormatting.DARK_PURPLE + "【屠神收割】";
        } else if (count >= 5) {
            return TextFormatting.RED + "【大收割】";
        } else if (count >= 3) {
            return TextFormatting.GOLD + "【收割】";
        } else {
            return TextFormatting.YELLOW + "【处决】";
        }
    }
    
    /**
     * 生成处决特效
     */
    private static void spawnExecuteEffect(WorldServer world, EntityPlayer player, 
                                          float range, int count, boolean hadBoss) {
        // 冲击波
        for (int i = 0; i < 360; i += 15) {
            double rad = Math.toRadians(i);
            double x = player.posX + Math.cos(rad) * range;
            double z = player.posZ + Math.sin(rad) * range;
            
            world.spawnParticle(
                hadBoss ? EnumParticleTypes.SPELL_WITCH : EnumParticleTypes.CRIT,
                x, player.posY, z,
                1, 0, 0.5, 0, 0.1
            );
        }
        
        // 上升粒子
        if (count >= 3) {
            for (int i = 0; i < count * 5; i++) {
                double x = player.posX + (world.rand.nextDouble() - 0.5) * range * 2;
                double z = player.posZ + (world.rand.nextDouble() - 0.5) * range * 2;
                double y = player.posY;
                
                world.spawnParticle(
                    EnumParticleTypes.REDSTONE,
                    x, y, z,
                    1, 0, 1, 0, 0.0
                );
            }
        }
    }
}
