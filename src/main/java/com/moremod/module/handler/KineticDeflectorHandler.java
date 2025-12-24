package com.moremod.module.handler;

import com.moremod.module.effect.EventContext;
import com.moremod.module.effect.IModuleEventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.WorldServer;

/**
 * 动能偏导护盾处理器 (KINETIC_DEFLECTOR) - 实现“弹反”机制。
 */
public class KineticDeflectorHandler implements IModuleEventHandler {

    // 判定窗口 (ticks) [Lv0, Lv1(20t/1s), Lv2(30t/1.5s), Lv3(40t/2s)]
    private static final int[] PARRY_WINDOW = {0, 20, 30, 40};
    private static final int ENERGY_COST = 1000;

    @Override
    public float onPlayerHurt(EventContext ctx, DamageSource source, float damage) {
        // 只在服务端处理
        if (ctx.player.world.isRemote) return damage;

        EntityPlayer player = ctx.player;
        int level = ctx.level;

        if (level <= 0 || level >= PARRY_WINDOW.length) return damage;

        // 1. 检查玩家是否正在使用物品
        if (!player.isHandActive()) return damage;

        // 2. 【关键】健壮性检查：确保动作是格挡 (EnumAction.BLOCK)
        ItemStack activeStack = player.getActiveItemStack();
        if (activeStack.isEmpty() || activeStack.getItemUseAction() != EnumAction.BLOCK) {
             // 只允许真正的格挡动作触发
             return damage;
        }

        // 3. 检查能量
        if (ctx.getEnergy() < ENERGY_COST) return damage;

        // 4. 判断格挡时机
        int activeDuration;
        try {
            // 计算格挡持续时间 (单位: ticks) = 最大使用时间 - 剩余使用时间
            int maxDuration = activeStack.getMaxItemUseDuration();
            int remainingDuration = player.getItemInUseCount();
            activeDuration = maxDuration - remainingDuration;
        } catch (Exception e) {
            // 防止某些模组物品实现异常导致崩溃
            return damage;
        }

        // 判定是否在窗口期内 (刚开始格挡)
        if (activeDuration > 0 && activeDuration <= PARRY_WINDOW[level]) {
            // 完美偏导触发！
            if (ctx.consumeEnergy(ENERGY_COST)) {
                handleParryEffect(ctx, source, level);
                playFeedback(player);
                // 免疫本次攻击
                return 0;
            }
        }

        return damage;
    }

    /**
     * 处理偏导成功的反制效果
     */
    private void handleParryEffect(EventContext ctx, DamageSource source, int level) {
        EntityPlayer player = ctx.player;

        // 1. 反射弹射物
        if (source.isProjectile() && source.getImmediateSource() != null) {
            Entity projectile = source.getImmediateSource();

            // 反转速度并加速
            projectile.motionX *= -1.5;
            projectile.motionY *= 1.1; // 稍微抬升
            projectile.motionZ *= -1.5;
            projectile.velocityChanged = true; // 通知客户端更新

            // 更新拥有者并设置为暴击
            if (projectile instanceof EntityArrow) {
                ((EntityArrow) projectile).shootingEntity = player;
                ((EntityArrow) projectile).setIsCritical(true);
            }
        }
        // 2. 强力击退近战攻击者
        else if (source.getTrueSource() instanceof EntityLivingBase) {
            EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();

            // 计算击退强度: 1.5, 2.0, 2.5
            double strength = 1.0 + level * 0.5;

            // 执行击退
            attacker.knockBack(player, (float) strength,
                attacker.posX - player.posX,
                attacker.posZ - player.posZ);

            // Lv3 奖励: 施加虚弱
            if (level >= 3) {
                attacker.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 60, 0)); // 虚弱 I, 3秒
            }
        }
    }

    /**
     * 播放音效和粒子效果反馈
     */
    private void playFeedback(EntityPlayer player) {
        // 播放成功音效 (高音调的铁砧声，模拟“叮”声)
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.4F, 1.8F);

        // 粒子效果: 魔法爆击火花
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.CRIT_MAGIC,
                player.posX, player.posY + player.height / 2.0, player.posZ,
                25, // 数量
                0.5, 0.5, 0.5, // 扩散范围 (dx, dy, dz)
                0.1 // 速度
            );
        }
    }

    @Override
    public int getPassiveEnergyCost() {
        return 0; // 主动消耗
    }
}