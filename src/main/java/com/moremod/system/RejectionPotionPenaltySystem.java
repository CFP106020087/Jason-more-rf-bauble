package com.moremod.system;

import com.moremod.config.FleshRejectionConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

/**
 * 正面药水惩罚系统 - 简化版
 * 
 * 职责：
 * 1. 压制：排异值高时限制正面药水数量
 * 2. 反馈：每秒根据正面药水数量增加排异
 * 
 * 注意：药水阻挡逻辑已移至 FleshRejectionPotionHandler
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class RejectionPotionPenaltySystem {

    private static final PotionCategoryClassifier classifier = new PotionCategoryClassifier();
    
    // 处理间隔：每秒一次
    private static final Map<UUID, Integer> processCooldown = new HashMap<>();
    private static final int PROCESS_INTERVAL = 20;
    
    /**
     * 玩家tick - 压制正面药水 + 排异增长
     */
    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;
        if (!FleshRejectionConfig.enablePotionRejection) return;
        if (!FleshRejectionSystem.hasMechanicalCore(player)) return;

        UUID playerId = player.getUniqueID();
        
        // 冷却控制：每秒处理一次
        int cooldown = processCooldown.getOrDefault(playerId, 0);
        if (cooldown > 0) {
            processCooldown.put(playerId, cooldown - 1);
            return;
        }
        processCooldown.put(playerId, PROCESS_INTERVAL);
        
        // 突破状态免疫
        if (FleshRejectionSystem.hasTranscended(player)) return;

        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        float maxRejection = FleshRejectionConfig.maxRejection;

        // 计算允许的正面药水数量
        int maxAllowedPositive = computeMaxAllowedPositive(rejection, maxRejection);

        // 收集正面药水
        List<PotionEffect> positiveEffects = new ArrayList<>();
        for (PotionEffect eff : new ArrayList<>(player.getActivePotionEffects())) {
            if (classifier.isPositive(eff.getPotion())) {
                positiveEffects.add(eff);
            }
        }

        // 压制：超过限制则移除
        if (positiveEffects.size() > maxAllowedPositive) {
            // 按持续时间排序，优先移除短时效果
            positiveEffects.sort(Comparator.comparingInt(PotionEffect::getDuration));
            
            Set<Potion> toRemove = new HashSet<>();
            for (int i = maxAllowedPositive; i < positiveEffects.size(); i++) {
                toRemove.add(positiveEffects.get(i).getPotion());
            }
            
            // 批量移除
            for (Potion potion : toRemove) {
                player.removePotionEffect(potion);
            }
            
            // 提示（低频）
            if (player.ticksExisted % 60 == 0) {
                player.sendStatusMessage(
                    new TextComponentString("§c⚠ 排异压制了 " + toRemove.size() + " 个效果"),
                    true
                );
            }
        }

        // 根据正面药水增加排异
        if (maxAllowedPositive > 0) {
            int activeCount = Math.min(positiveEffects.size(), maxAllowedPositive);
            if (activeCount > 0) {
                float growth = (float) (activeCount * FleshRejectionConfig.potionRejectionGain);
                FleshRejectionSystem.setRejectionLevel(player, rejection + growth);
            }
        }
    }

    /**
     * 计算允许的正面药水数量
     */
    private static int computeMaxAllowedPositive(float rejection, float maxRejection) {
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
     * 正面药水分类器
     */
    public static class PotionCategoryClassifier {
        private final Map<Potion, Boolean> cache = new HashMap<>();

        public boolean isPositive(Potion potion) {
            return cache.computeIfAbsent(potion, this::checkPositive);
        }

        private boolean checkPositive(Potion potion) {
            // Vanilla正面效果
            if (potion == net.minecraft.init.MobEffects.STRENGTH) return true;
            if (potion == net.minecraft.init.MobEffects.SPEED) return true;
            if (potion == net.minecraft.init.MobEffects.REGENERATION) return true;
            if (potion == net.minecraft.init.MobEffects.ABSORPTION) return true;
            if (potion == net.minecraft.init.MobEffects.HASTE) return true;
            if (potion == net.minecraft.init.MobEffects.LUCK) return true;
            if (potion == net.minecraft.init.MobEffects.RESISTANCE) return true;
            if (potion == net.minecraft.init.MobEffects.FIRE_RESISTANCE) return true;
            if (potion == net.minecraft.init.MobEffects.WATER_BREATHING) return true;
            if (potion == net.minecraft.init.MobEffects.NIGHT_VISION) return true;
            if (potion == net.minecraft.init.MobEffects.INVISIBILITY) return true;
            if (potion == net.minecraft.init.MobEffects.JUMP_BOOST) return true;
            if (potion == net.minecraft.init.MobEffects.HEALTH_BOOST) return true;
            if (potion == net.minecraft.init.MobEffects.SATURATION) return true;

            return false;
        }
    }
    
    /**
     * 清理玩家数据
     */
    public static void cleanupPlayer(UUID playerId) {
        processCooldown.remove(playerId);
    }
}