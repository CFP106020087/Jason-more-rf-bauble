package com.moremod.item.herosword;

import com.moremod.combat.TrueDamageHelper;
import com.moremod.item.ItemHeroSword;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 勇者之剑 - 事件处理（修复版）
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class HeroSwordEventHandler {

    // ===== 击杀加经验 =====
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingDeath(LivingDeathEvent event) {
        DamageSource src = event.getSource();
        Entity trueSrc = src.getTrueSource();
        if (!(trueSrc instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) trueSrc;
        ItemStack main = player.getHeldItemMainhand();
        if (main.isEmpty() || !(main.getItem() instanceof ItemHeroSword)) return;

        if (player.world.isRemote) return;

        EntityLivingBase killed = event.getEntityLiving();
        boolean isBoss = HeroSwordStats.isBoss(killed);
        HeroSwordNBT.addKillExp(main, isBoss);
    }

    // ===== 玩家受击：宿命重担记录 =====
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;
        
        // 忽略极小伤害（避免反复触发）
        if (event.getAmount() < 0.5F) return;

        ItemStack main = player.getHeldItemMainhand();
        if (main.isEmpty() || !(main.getItem() instanceof ItemHeroSword)) return;

        long tick = player.world.getTotalWorldTime();
        HeroSwordNBT.addHitTaken(main, 1, tick);
    }

    // ===== 对敌伤害：巨像杀手 + 宿命重担倍率 =====
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onEntityHurt(LivingHurtEvent event) {
        DamageSource src = event.getSource();
        Entity trueSrc = src.getTrueSource();
        if (!(trueSrc instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) trueSrc;
        if (player.world.isRemote) return;

        ItemStack main = player.getHeldItemMainhand();
        if (main.isEmpty() || !(main.getItem() instanceof ItemHeroSword)) return;

        EntityLivingBase target = event.getEntityLiving();
        float damage = event.getAmount();
        if (damage <= 0.0F) return;

        // 标记战斗中
        HeroSwordNBT.markAttack(main, player.world.getTotalWorldTime());

        // 巨像杀手倍率
        float giantMult = HeroSwordStats.getGiantSlayerMultiplier(player, target, main);

        // 宿命重担倍率
        float burdenMult = HeroSwordStats.getFateBurdenMultiplier(main, player);

        float finalDamage = damage * giantMult * burdenMult;
        event.setAmount(finalDamage);
    }

    // ===== 真伤：宿命裁决（将全部伤害转换为真伤）=====
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityDamage(LivingDamageEvent event) {
        // 避免递归：如果已经在真伤处理中，跳过
        if (TrueDamageHelper.isInTrueDamageContext()) return;

        DamageSource src = event.getSource();
        Entity trueSrc = src.getTrueSource();
        if (!(trueSrc instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) trueSrc;
        if (player.world.isRemote) return;

        ItemStack main = player.getHeldItemMainhand();
        if (main.isEmpty() || !(main.getItem() instanceof ItemHeroSword)) return;

        EntityLivingBase target = event.getEntityLiving();
        if (target.isDead || target.getHealth() <= 0.0F) return;

        float baseDamage = event.getAmount();
        if (baseDamage <= 0.0F) return;

        // 取消原始伤害，改用 TrueDamageHelper 施加真伤
        event.setCanceled(true);

        // 通过 TrueDamageHelper 施加真伤
        TrueDamageHelper.applyWrappedTrueDamage(
                target,
                player,
                baseDamage,
                TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE
        );
    }

    // ===== 玩家Tick：宿命重担渐进式衰减（修复版）=====
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        ItemStack main = player.getHeldItemMainhand();
        if (main.isEmpty() || !(main.getItem() instanceof ItemHeroSword)) return;

        long tick = player.world.getTotalWorldTime();
        
        // 每秒执行一次衰减检查（而非每tick）
        if (tick % 20 != 0) return;
        
        HeroSwordNBT.decayHitsTaken(main, tick);
    }

    // ===== 自定义伤害源（用于真伤）=====
    public static class HeroSwordDamage extends EntityDamageSource {
        public HeroSwordDamage(Entity source) {
            super("heroSwordTrue", source);
            this.setDamageBypassesArmor();
            this.setDamageIsAbsolute();
        }
    }
}