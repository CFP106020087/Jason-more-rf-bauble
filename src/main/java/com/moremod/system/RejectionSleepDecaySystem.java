package com.moremod.system;

import com.moremod.config.FleshRejectionConfig;
import com.moremod.item.ItemMechanicalCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 睡眠/休息 → 排異自然下降
 * 睡眠中下降較快，醒來後可持續一段時間緩慢下降
 * 【修复】：只有佩戴机械核心的玩家才受影响
 */
public class RejectionSleepDecaySystem {

    // 記錄睡醒後持續自然下降的秒數
    private static final java.util.WeakHashMap<EntityPlayer, Integer> postSleepTicks = new java.util.WeakHashMap<>();

    /**
     * 玩家睡覺開始：立即進入睡眠下降狀態
     */
    @SubscribeEvent
    public void onSleep(PlayerSleepInBedEvent event) {
        if (!FleshRejectionConfig.enableSleepDecay) return;

        EntityPlayer player = event.getEntityPlayer();
        
        // ✅ 【修复】：检查是否佩戴机械核心
        if (!FleshRejectionSystem.hasMechanicalCore(player)) return;
        
        if (!player.world.isRemote) {
            player.sendMessage(new TextComponentString("§b你進入深度休息，血肉排異正在平穩下降…"));
        }
    }

    /**
     * 玩家睡醒：啟動「持續下降」
     */
    @SubscribeEvent
    public void onWake(PlayerWakeUpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || !FleshRejectionConfig.enableSleepDecay) return;

        // ✅ 【修复】：检查是否佩戴机械核心
        if (!FleshRejectionSystem.hasMechanicalCore(player)) return;

        if (FleshRejectionConfig.continueDecayAfterWake) {
            postSleepTicks.put(player, FleshRejectionConfig.postSleepDecayDuration * 20);
            player.sendMessage(new TextComponentString("§a你感到精神煥發，排異仍在逐漸下降…"));
        }
    }

    /**
     * 每 tick 檢查是否需要下降排異
     */
    @SubscribeEvent
    public void onTick(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        // ✅ 【修复】：检查是否佩戴机械核心
        // 未佩戴核心的玩家不应该有任何睡眠排异变化
        if (!FleshRejectionSystem.hasMechanicalCore(player)) {
            // 清理未佩戴核心玩家的睡眠恢复状态
            if (postSleepTicks.containsKey(player)) {
                postSleepTicks.remove(player);
            }
            return;
        }

        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        if (rejection <= 0) return;

        // 睡覺時
        if (player.isPlayerSleeping() && FleshRejectionConfig.enableSleepDecay) {
            if (player.ticksExisted % 20 == 0) {
                float decay = (float) FleshRejectionConfig.sleepDecayRate;
                FleshRejectionSystem.reduceRejection(player, decay);
            }
            return;
        }

        // 睡醒後持續自然下降
        if (postSleepTicks.containsKey(player)) {
            int ticksLeft = postSleepTicks.get(player);
            if (ticksLeft <= 0) {
                postSleepTicks.remove(player);
                return;
            }

            if (player.ticksExisted % 20 == 0) {
                float decay = (float) FleshRejectionConfig.postSleepDecayRate;
                FleshRejectionSystem.reduceRejection(player, decay);
            }

            postSleepTicks.put(player, ticksLeft - 1);
        }
    }
}