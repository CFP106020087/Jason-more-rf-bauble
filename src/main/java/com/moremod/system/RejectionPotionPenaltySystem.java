package com.moremod.system;

import com.moremod.config.FleshRejectionConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.Potion;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 正面藥水效果 → 提升排異
 * 排異值越高 → 玩家可保留的正面藥水越少（平滑壓制）
 */
public class RejectionPotionPenaltySystem {

    private static final PotionCategoryClassifier classifier = new PotionCategoryClassifier();

    /**
     * 玩家每 tick 檢查正面藥水壓制 + 排異增長
     */
    @SubscribeEvent
    public void onPlayerTick(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        if (!FleshRejectionConfig.enablePotionRejection) return;

        // ✅ 修复：检查是否佩戴机械核心
        // 未佩戴核心的玩家不应该被药水增加排异值或被压制正面效果
        if (!FleshRejectionSystem.hasMechanicalCore(player)) return;

        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        float maxRejection = FleshRejectionConfig.maxRejection;

        // 計算玩家最多可擁有的正面藥水數量
        int maxAllowedPositive = computeMaxAllowedPositive(rejection, maxRejection);

        // 判斷哪些屬於「正面藥水」
        List<PotionEffect> positiveEffects = new ArrayList<>();
        for (PotionEffect eff : player.getActivePotionEffects()) {
            if (classifier.isPositive(eff.getPotion())) {
                positiveEffects.add(eff);
            }
        }

        // ① 壓制：超過可擁有的數量 → 自動移除後面的藥水
        if (positiveEffects.size() > maxAllowedPositive) {
            for (int i = maxAllowedPositive; i < positiveEffects.size(); i++) {
                PotionEffect removed = positiveEffects.get(i);
                player.removePotionEffect(removed.getPotion());
            }
        }

        // ② 每秒根據正面藥水增加排異
        if (player.ticksExisted % 20 == 0 && maxAllowedPositive > 0) {
            int activePositiveCount = Math.min(positiveEffects.size(), maxAllowedPositive);
            if (activePositiveCount > 0) {
                float growth = (float) (activePositiveCount * FleshRejectionConfig.potionRejectionGain);
                FleshRejectionSystem.setRejectionLevel(player, rejection + growth);
            }
        }
    }

    /**
     * 正面藥水壓制公式（平滑）：
     * rejection = start → maxAllowedPositive N
     * rejection = max   → 0
     */
    private int computeMaxAllowedPositive(float rejection, float maxRejection) {
        int start = FleshRejectionConfig.potionSuppressionStart;
        int endValue = FleshRejectionConfig.potionSuppressionEndValue;
        int baseMax = FleshRejectionConfig.maxPositiveEffects;

        if (rejection <= start) return baseMax;
        if (rejection >= maxRejection) return endValue;

        float ratio = (rejection - start) / (maxRejection - start);
        float suppressed = baseMax - (ratio * (baseMax - endValue));
        return Math.max(endValue, Math.round(suppressed));
    }

    /**
     * 允許區分正面與負面藥水
     */
    public static class PotionCategoryClassifier {

        public boolean isPositive(Potion potion) {
            // vanilla 常見正面效果
            if (potion == net.minecraft.init.MobEffects.STRENGTH) return true;
            if (potion == net.minecraft.init.MobEffects.SPEED) return true;
            if (potion == net.minecraft.init.MobEffects.REGENERATION) return true;
            if (potion == net.minecraft.init.MobEffects.ABSORPTION) return true;
            if (potion == net.minecraft.init.MobEffects.HASTE) return true;
            if (potion == net.minecraft.init.MobEffects.LUCK) return true;
            if (potion == net.minecraft.init.MobEffects.RESISTANCE) return true;
            if (potion == net.minecraft.init.MobEffects.FIRE_RESISTANCE) return true;
            if (potion == net.minecraft.init.MobEffects.WATER_BREATHING) return true;

            // 閣下可以自行擴充

            return false;
        }
    }
}
