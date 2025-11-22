package com.moremod.synergy.effect;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.upgrades.WetnessSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 湿度成瘾效果 - 脱水戒断症状
 *
 * 机制：
 * - 当湿度降至0%时触发"脱水震颤"
 * - 效果：挖掘疲劳IV + 缓慢II
 * - 每5秒强制消耗500 RF
 * - 持续到重新淋雨（湿度>0）
 */
public class MoistureAddictionEffect implements ISynergyEffect {

    private static final int ENERGY_DRAIN = 500; // 每5秒消耗500 RF
    private static final int DRAIN_INTERVAL = 100; // 每100 tick (5秒) 消耗一次

    @Override
    public String getEffectId() {
        return "moisture_addiction";
    }

    @Override
    @SubscribeEvent
    public void onPlayerTick(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        // 获取湿度
        int wetness = WetnessSystem.getWetness(player);

        if (wetness == 0) {
            // 湿度为0，触发脱水震颤
            applyWithdrawalSymptoms(player);
        } else {
            // 湿度恢复，检查是否需要清除症状
            checkRecovery(player, wetness);
        }
    }

    /**
     * 施加脱水震颤症状
     */
    private void applyWithdrawalSymptoms(EntityPlayer player) {
        // 施加挖掘疲劳IV (几乎无法挖掘)
        if (!player.isPotionActive(MobEffects.MINING_FATIGUE) ||
            player.getActivePotionEffect(MobEffects.MINING_FATIGUE).getAmplifier() < 4) {

            player.addPotionEffect(new PotionEffect(
                MobEffects.MINING_FATIGUE, 100, 4, false, true
            ));
        }

        // 施加缓慢II
        if (!player.isPotionActive(MobEffects.SLOWNESS) ||
            player.getActivePotionEffect(MobEffects.SLOWNESS).getAmplifier() < 2) {

            player.addPotionEffect(new PotionEffect(
                MobEffects.SLOWNESS, 100, 2, false, true
            ));
        }

        // 每5秒消耗能量
        if (player.ticksExisted % DRAIN_INTERVAL == 0) {
            ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
            if (!core.isEmpty()) {
                boolean consumed = ItemMechanicalCore.consumeEnergy(core, ENERGY_DRAIN);

                if (consumed) {
                    player.sendStatusMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "💀 脱水震颤：系统渴求湿度！(-" + ENERGY_DRAIN + " RF)"
                    ), true);
                } else {
                    // 能量不足时，额外警告
                    player.sendStatusMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "💀 能量耗尽！快找水源！"
                    ), true);
                }
            }
        }

        // 每20秒提示一次如何恢复
        if (player.ticksExisted % 400 == 0) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "⚠ 脱水震颤症状：需要淋雨或用水桶淋湿自己才能恢复"
            ));
        }
    }

    /**
     * 检查恢复情况
     */
    private void checkRecovery(EntityPlayer player, int wetness) {
        // 如果湿度刚从0恢复到>0，通知玩家
        if (wetness > 0 && wetness <= 10) {
            // 只在刚恢复时提示一次（湿度10%以内）
            if (player.ticksExisted % 20 == 0) {
                player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 湿度恢复，脱水症状缓解中..."
                ), true);
            }
        }

        // 湿度>=20%时，完全恢复
        if (wetness >= 20) {
            // 移除负面效果
            if (player.isPotionActive(MobEffects.MINING_FATIGUE)) {
                PotionEffect effect = player.getActivePotionEffect(MobEffects.MINING_FATIGUE);
                if (effect != null && effect.getAmplifier() == 4) {
                    player.removePotionEffect(MobEffects.MINING_FATIGUE);
                }
            }

            if (player.isPotionActive(MobEffects.SLOWNESS)) {
                PotionEffect effect = player.getActivePotionEffect(MobEffects.SLOWNESS);
                if (effect != null && effect.getAmplifier() == 2) {
                    player.removePotionEffect(MobEffects.SLOWNESS);
                }
            }
        }
    }

    /**
     * 提供给玩家的恢复提示
     */
    public static void showRecoveryHint(EntityPlayer player) {
        player.sendMessage(new TextComponentString(
            TextFormatting.YELLOW + "━━━ 如何恢复湿度 ━━━"
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "1. 在雨中站立"
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "2. 对自己使用水桶（右键）"
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "3. 建造洒水装置/人工雨室"
        ));
    }
}
