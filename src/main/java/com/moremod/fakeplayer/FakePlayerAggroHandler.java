package com.moremod.fakeplayer;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 假玩家仇恨取消处理器
 *
 * 防止生物将假玩家设为攻击目标，避免游戏卡死
 * 类似于 ImmortalAmulet 的仇恨取消机制
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class FakePlayerAggroHandler {

    /**
     * 阻止生物锁定假玩家
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        // 检查目标是否是假玩家
        if (event.getTarget() instanceof FakePlayer) {
            EntityLivingBase attacker = event.getEntityLiving();

            // 清除攻击目标
            if (attacker instanceof EntityLiving) {
                EntityLiving living = (EntityLiving) attacker;
                living.setAttackTarget(null);

                // 清除复仇目标
                if (attacker.getRevengeTarget() instanceof FakePlayer) {
                    attacker.setRevengeTarget(null);
                }
            }
        }
    }

    /**
     * 清除指定范围内所有生物对假玩家的仇恨
     * 可被 TileEntityFakePlayerActivator 调用
     */
    public static void clearAggroInArea(FakePlayer fakePlayer, double radius) {
        if (fakePlayer == null || fakePlayer.world == null) return;

        fakePlayer.world.getEntitiesWithinAABB(
            EntityLiving.class,
            fakePlayer.getEntityBoundingBox().grow(radius),
            e -> e != null && e.isEntityAlive()
        ).forEach(entity -> {
            // 清除以假玩家为目标的仇恨
            if (entity.getAttackTarget() instanceof FakePlayer) {
                entity.setAttackTarget(null);
            }
            if (entity.getRevengeTarget() instanceof FakePlayer) {
                entity.setRevengeTarget(null);
            }
        });
    }
}
