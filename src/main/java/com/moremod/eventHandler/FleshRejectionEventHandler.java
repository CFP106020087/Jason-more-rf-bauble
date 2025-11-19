package com.moremod.eventHandler;

import com.moremod.config.FleshRejectionConfig;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.system.FleshRejectionSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class FleshRejectionEventHandler {

    /**
     * 玩家Tick - 更新排异系统
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!FleshRejectionConfig.enableRejectionSystem) return;
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;

        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (!core.isEmpty()) {
            FleshRejectionSystem.updateRejection(player, core);
        }

        // ✅ 修复：传入 core 参数，让方法自己检查
        handleInvulnerabilityReduction(player, core);
    }

    /**
     * 缩短无敌帧
     */
    private static void handleInvulnerabilityReduction(EntityPlayer player, ItemStack core) {
        if (!FleshRejectionConfig.enableInvulnerabilityReduction) return;

        // ✅ 修复：检查核心
        if (core.isEmpty()) return;

        if (FleshRejectionSystem.hasTranscended(player)) return;

        if (player.hurtTime >= player.maxHurtTime - 1 && player.hurtTime <= player.maxHurtTime) {
            float rejection = FleshRejectionSystem.getRejectionLevel(player);

            if (rejection < FleshRejectionConfig.invulnerabilityReductionStart) return;

            // 计算缩减比例
            float reductionProgress = (rejection - FleshRejectionConfig.invulnerabilityReductionStart) /
                    (FleshRejectionConfig.maxRejection - FleshRejectionConfig.invulnerabilityReductionStart);

            float multiplier = 1.0f - reductionProgress * (1.0f - (float)FleshRejectionConfig.minInvulnerabilityRatio);
            multiplier = MathHelper.clamp(multiplier, (float)FleshRejectionConfig.minInvulnerabilityRatio, 1.0f);

            int expected = player.maxHurtResistantTime;
            if (player.hurtResistantTime >= expected - 2) {
                player.hurtResistantTime = (int)(expected * multiplier);
            }
        }
    }

    /**
     * 攻击失误
     */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!FleshRejectionConfig.enableAttackMiss) return;

        EntityPlayer player = event.getEntityPlayer();

        // ✅ 修复：检查核心
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;

        if (FleshRejectionSystem.hasTranscended(player)) return;

        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        if (rejection < FleshRejectionConfig.attackMissStart) return;

        // 计算失误概率
        float missProgress = (rejection - FleshRejectionConfig.attackMissStart) /
                (FleshRejectionConfig.maxRejection - FleshRejectionConfig.attackMissStart);
        float missChance = (float)FleshRejectionConfig.maxMissChance * missProgress;

        if (player.world.rand.nextFloat() < missChance) {
            event.setCanceled(true);
            player.swingArm(EnumHand.MAIN_HAND);

            // 检查是否触发自伤
            if (FleshRejectionConfig.enableAttackSelfDamage &&
                    rejection >= FleshRejectionConfig.selfDamageStart &&
                    player.world.rand.nextFloat() < FleshRejectionConfig.selfDamageChance) {

                player.attackEntityFrom(
                        new DamageSource("rejection").setDamageBypassesArmor().setDamageIsAbsolute(),
                        (float)FleshRejectionConfig.selfDamageAmount
                );

                if (!player.world.isRemote) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.DARK_RED + "⚠ 神經錯亂！"
                    ), true);
                }
            } else {
                if (!player.world.isRemote) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.RED + "⚡ 肌肉失序"
                    ), true);
                }
            }
        }
    }

    /**
     * 受伤时触发出血
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!FleshRejectionConfig.enableBleeding) return;
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // ✅ 修复：检查核心
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;

        if (FleshRejectionSystem.hasTranscended(player)) return;

        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        if (rejection < FleshRejectionConfig.invulnerabilityReductionStart) return;

        // 计算出血概率
        float bleedChance = (float)(FleshRejectionConfig.bleedingBaseChance +
                (rejection - FleshRejectionConfig.invulnerabilityReductionStart) *
                        FleshRejectionConfig.bleedingChanceGrowth);

        if (player.world.rand.nextFloat() < bleedChance) {
            FleshRejectionSystem.triggerBleeding(player, FleshRejectionConfig.bleedingDuration);

            if (!player.world.isRemote) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "💉 血液溶解"
                ), true);
            }
        }
    }

    /**
     * ✅ 玩家死亡处理（修复版）
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        EntityPlayer oldPlayer = event.getOriginal();
        EntityPlayer newPlayer = event.getEntityPlayer();

        // 调用系统的死亡处理方法
        FleshRejectionSystem.handlePlayerDeath(oldPlayer, newPlayer);
    }
}
