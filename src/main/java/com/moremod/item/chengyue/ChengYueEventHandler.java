package com.moremod.item.chengyue;

import com.moremod.capability.ChengYueCapability;
import com.moremod.item.ItemSwordChengYue;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 澄月 - 完整事件处理（平衡版）
 */
public class ChengYueEventHandler {

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onLivingDeath(LivingDeathEvent event) {
        EntityLivingBase killed = event.getEntityLiving();
        DamageSource source = event.getSource();

        if (!(source.getTrueSource() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer killer = (EntityPlayer) source.getTrueSource();
        ItemStack mainHand = killer.getHeldItemMainhand();

        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof ItemSwordChengYue)) {
            return;
        }

        if (killer.world.isRemote) {
            return;
        }

        ChengYueNBT.init(mainHand);
        long killCount = ChengYueNBT.getKillCount(mainHand);
        ChengYueNBT.setKillCount(mainHand, killCount + 1);

        boolean isBoss = killed.getMaxHealth() >= 100.0f;
        if (isBoss) {
            int bossKills = ChengYueNBT.getBossKills(mainHand);
            ChengYueNBT.setBossKills(mainHand, bossKills + 1);
        }

        long expGain = isBoss ? 500 : 50;
        ChengYueLevel.addExp(mainHand, killer, expGain);

        if (ChengYueLunarPower.isUnlocked(mainHand)) {
            ChengYueCapability cap = killer.getCapability(ChengYueCapability.CAPABILITY, null);
            if (cap != null) {
                ChengYueLunarPower.onKill(cap, isBoss);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        ItemStack mainHand = player.getHeldItemMainhand();

        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof ItemSwordChengYue)) {
            return;
        }

        if (player.world.isRemote) {
            return;
        }

        float damage = event.getAmount();
        DamageSource source = event.getSource();

        if (ChengYueSurvival.tryDodge(player, mainHand, source)) {
            event.setCanceled(true);
            return;
        }

        damage = ChengYueSurvival.applyDamageReduction(damage, mainHand, player.world);
        ChengYueSurvival.tryActivateAegis(player, mainHand, damage, source);
        damage = ChengYueSurvival.applyAegisReduction(player, mainHand, damage, source);

        event.setAmount(damage);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onEntityHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();

        if (ChengYueSweep.ChengYueSweepDamage.isSweepDamage(source)) {
            return;
        }

        if (!(source.getTrueSource() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer attacker = (EntityPlayer) source.getTrueSource();
        ItemStack mainHand = attacker.getHeldItemMainhand();

        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof ItemSwordChengYue)) {
            return;
        }

        if (attacker.world.isRemote) {
            return;
        }

        EntityLivingBase target = event.getEntityLiving();
        float damage = event.getAmount();

        // 月殇易伤（先标记）
        ChengYueMoonAffliction.applyAffliction(target, attacker, mainHand);
        float afflictionMult = ChengYueMoonAffliction.getDamageMultiplier(target);

        ChengYueCapability cap = attacker.getCapability(ChengYueCapability.CAPABILITY, null);

        // === 收集各种倍率 ===
        float comboMult = 1.0f;
        float formMult = 1.0f;
        float moonMult = 1.0f;

        if (cap != null) {
            int combo = cap.getCombo();
            if (combo > 0) {
                comboMult = ChengYueCombo.getComboMultiplier(combo);
            }
        }

        if (ChengYueFormManager.isUnlocked(mainHand) && cap != null) {
            int formIndex = cap.getCurrentForm();
            ChengYueMoonForm form = ChengYueMoonForm.values()[formIndex];
            formMult = form.getDamageMultiplier();
        }

        // 当前月相 & 记忆：取较强的一种，然后交给压缩模型处理
        float phaseMult = ChengYueMoonPhase.getDamageMultiplier(attacker.world);
        float memoryMult = ChengYueMoonMemory.getDamageMultiplierWithMemory(mainHand, attacker.world);
        moonMult = Math.max(phaseMult, memoryMult);

        // === 统一伤害加权叠加 ===
        float extra = 0.0f;

        // 易伤效果：权重 0.4
        extra += (afflictionMult - 1.0f) * 0.4f;

        // 连击：权重 0.4
        extra += (comboMult - 1.0f) * 0.4f;

        // 形态：权重 0.6
        extra += (formMult - 1.0f) * 0.6f;

        // 月相：权重 0.6
        extra += (moonMult - 1.0f) * 0.6f;

        float finalMult = 1.0f + extra;
        if (finalMult < 0.0f) {
            finalMult = 0.0f;
        }

        damage *= finalMult;

        event.setAmount(damage);

        // === 更新连击 & 月华 ===
        if (cap != null) {
            cap.addCombo();
            cap.setLastHitTime(attacker.world.getTotalWorldTime());

            if (ChengYueLunarPower.isUnlocked(mainHand)) {
                ChengYueLunarPower.onHit(cap);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityDamage(LivingDamageEvent event) {
        DamageSource source = event.getSource();

        if (ChengYueSweep.ChengYueSweepDamage.isSweepDamage(source)) {
            return;
        }

        if (!(source.getTrueSource() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer attacker = (EntityPlayer) source.getTrueSource();
        ItemStack mainHand = attacker.getHeldItemMainhand();

        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof ItemSwordChengYue)) {
            return;
        }

        if (attacker.world.isRemote) {
            return;
        }

        EntityLivingBase target = event.getEntityLiving();
        float finalDamage = event.getAmount();

        int level = ChengYueNBT.getLevel(mainHand);
        ChengYueSweep.performSweepAttack(
                attacker,
                target,
                finalDamage,
                mainHand,
                level
        );

        ChengYueSurvival.applyLifeSteal(attacker, mainHand, finalDamage);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        EntityPlayer player = event.player;
        if (player.world.isRemote) {
            return;
        }

        ItemStack mainHand = player.getHeldItemMainhand();

        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof ItemSwordChengYue)) {
            return;
        }

        ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);
        if (cap == null) {
            return;
        }

        // === 连击超时：更严格的时间窗口 ===
        if (player.ticksExisted % 5 == 0) {
            int combo = cap.getCombo();
            if (combo > 0) {
                long lastHit = cap.getLastHitTime();
                long currentTime = player.world.getTotalWorldTime();

                long timeout = 30; // 1.5 秒
                if (ChengYueMoonPhase.getCurrentPhase(player.world) == 5) {
                    timeout += 20; // 娥眉月额外 1 秒（总 2.5 秒）
                }

                if (currentTime - lastHit > timeout) {
                    cap.resetCombo();
                }
            }
        }

        // === 月华 & 月相记忆 ===
        if (player.ticksExisted % 20 == 0) {
            if (ChengYueLunarPower.isUnlocked(mainHand)) {
                ChengYueLunarPower.tickRegen(mainHand, cap, player);
            }

            if (player.ticksExisted % 200 == 0) {
                boolean updated = ChengYueMoonMemory.checkAndUpdateMemory(mainHand, player.world);

                if (updated) {
                    ChengYueMoonMemory.notifyMemoryUpdate(player, mainHand, player.world);

                    if (ChengYueFormManager.isUnlocked(mainHand)) {
                        ChengYueFormManager.updateAutoForm(mainHand, player, player.world);
                    }
                }
            }
        }

        // Aegis 粒子
        if (player.ticksExisted % 5 == 0) {
            long currentTime = player.world.getTotalWorldTime();
            if (ChengYueSurvival.isAegisActive(mainHand, currentTime)) {
                ChengYueSurvival.spawnAegisParticles(player);
            }
        }
    }
}
