package com.moremod.item;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/**
 * 預測模式系統 - 真正的行為預測
 * 這是一個可選的進階功能，讓預測模式不只是增加增益倍率
 * 而是真正"預測"玩家的下一步操作
 */
public class PredictionSystem {
    
    // ===== 行為模式記錄 =====
    private static class ActionPattern {
        long timestamp;
        ActionType type;
        Map<String, Object> context; // 上下文信息
        
        ActionPattern(ActionType type) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.context = new HashMap<>();
        }
    }
    
    private enum ActionType {
        COMBAT,      // 戰鬥
        MINING,      // 挖礦
        BUILDING,    // 建築
        MOVING,      // 移動
        CRAFTING     // 合成
    }
    
    // ===== 玩家行為歷史 =====
    private static final Map<UUID, List<ActionPattern>> actionHistory = new HashMap<>();
    private static final int MAX_HISTORY_SIZE = 100; // 只保留最近 100 個動作
    
    // ===== 預測緩存 =====
    private static final Map<UUID, ActionType> predictedNextAction = new HashMap<>();
    private static final Map<UUID, Long> lastPredictionTime = new HashMap<>();
    
    /**
     * 記錄玩家動作
     */
    public static void recordAction(EntityPlayer player, ActionType type) {
        if (!shouldTrack(player)) return;
        
        UUID uuid = player.getUniqueID();
        List<ActionPattern> history = actionHistory.computeIfAbsent(uuid, k -> new ArrayList<>());
        
        ActionPattern pattern = new ActionPattern(type);
        // 添加上下文信息
        pattern.context.put("health", player.getHealth());
        pattern.context.put("time", player.world.getWorldTime());
        pattern.context.put("dimension", player.dimension);
        pattern.context.put("posX", (int)player.posX);
        pattern.context.put("posY", (int)player.posY);
        pattern.context.put("posZ", (int)player.posZ);
        
        history.add(pattern);
        
        // 限制歷史大小
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
        
        // 觸發預測更新
        updatePrediction(player);
    }
    
    /**
     * 更新預測
     */
    private static void updatePrediction(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        List<ActionPattern> history = actionHistory.get(uuid);
        
        if (history == null || history.size() < 10) {
            return; // 數據不足，無法預測
        }
        
        // 簡單的預測算法：基於最近動作的頻率
        Map<ActionType, Integer> recentCounts = new HashMap<>();
        int recentWindowSize = Math.min(20, history.size());
        
        for (int i = history.size() - recentWindowSize; i < history.size(); i++) {
            ActionType type = history.get(i).type;
            recentCounts.put(type, recentCounts.getOrDefault(type, 0) + 1);
        }
        
        // 找出最頻繁的動作
        ActionType predicted = null;
        int maxCount = 0;
        for (Map.Entry<ActionType, Integer> entry : recentCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                predicted = entry.getKey();
            }
        }
        
        // 進階預測：檢測週期性模式
        ActionType periodicPrediction = detectPeriodicPattern(history);
        if (periodicPrediction != null) {
            predicted = periodicPrediction;
        }
        
        predictedNextAction.put(uuid, predicted);
        lastPredictionTime.put(uuid, System.currentTimeMillis());
    }
    
    /**
     * 檢測週期性模式
     * 例如：玩家總是「挖礦 -> 建築 -> 挖礦 -> 建築」
     */
    private static ActionType detectPeriodicPattern(List<ActionPattern> history) {
        if (history.size() < 30) return null;
        
        // 檢查最近 30 個動作是否有規律
        List<ActionType> recent = new ArrayList<>();
        for (int i = history.size() - 30; i < history.size(); i++) {
            recent.add(history.get(i).type);
        }
        
        // 嘗試找出長度為 2-5 的重複模式
        for (int patternLength = 2; patternLength <= 5; patternLength++) {
            if (isRepeatingPattern(recent, patternLength)) {
                // 預測下一個
                int currentPos = recent.size() % patternLength;
                return recent.get(currentPos);
            }
        }
        
        return null;
    }
    
    /**
     * 檢查是否為重複模式
     */
    private static boolean isRepeatingPattern(List<ActionType> actions, int patternLength) {
        if (actions.size() < patternLength * 2) return false;
        
        List<ActionType> pattern = actions.subList(0, patternLength);
        
        for (int i = patternLength; i < actions.size(); i += patternLength) {
            int end = Math.min(i + patternLength, actions.size());
            List<ActionType> segment = actions.subList(i, end);
            
            if (segment.size() == patternLength && !segment.equals(pattern)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 獲取預測的下一個動作
     */
    public static ActionType getPredictedAction(EntityPlayer player) {
        return predictedNextAction.get(player.getUniqueID());
    }
    
    /**
     * 根據預測提前啟動相關模組（需要與機械核心整合）
     */
    public static void applyPredictiveBonus(EntityPlayer player) {
        if (!shouldTrack(player)) return;
        
        ActionType predicted = getPredictedAction(player);
        if (predicted == null) return;
        
        // 這裡可以與機械核心整合，提前啟動對應模組
        // 例如：預測到戰鬥，提前啟動護盾模組
        
        switch (predicted) {
            case COMBAT:
                // 提前啟動戰鬥相關模組
                // 可以調用機械核心的 API 或發送信號
                applyPredictiveCombatBonus(player);
                break;
            case MINING:
                applyPredictiveMiningBonus(player);
                break;
            case BUILDING:
                applyPredictiveBuildingBonus(player);
                break;
        }
    }
    
    private static void applyPredictiveCombatBonus(EntityPlayer player) {
        // 示例：提供短暫的防禦增益
        player.addPotionEffect(
            new net.minecraft.potion.PotionEffect(
                net.minecraft.init.MobEffects.RESISTANCE,
                40, // 2 秒
                0,
                false,
                false
            )
        );
    }
    
    private static void applyPredictiveMiningBonus(EntityPlayer player) {
        // 示例：提供短暫的急迫效果
        player.addPotionEffect(
            new net.minecraft.potion.PotionEffect(
                net.minecraft.init.MobEffects.HASTE,
                40,
                0,
                false,
                false
            )
        );
    }
    
    private static void applyPredictiveBuildingBonus(EntityPlayer player) {
        // 建築增益可以通過其他方式實現
    }
    
    /**
     * 檢查是否應該追蹤該玩家
     */
    private static boolean shouldTrack(EntityPlayer player) {
        if (player == null || player.world.isRemote) return false;
        
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return false;
        
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemBehaviorAnalysisChip) {
                // 檢查是否已解鎖預測模式
                if (stack.hasTagCompound()) {
                    return stack.getTagCompound().getBoolean("PredictMode");
                }
            }
        }
        return false;
    }
    
    /**
     * 清理數據
     */
    public static void cleanup(UUID uuid) {
        actionHistory.remove(uuid);
        predictedNextAction.remove(uuid);
        lastPredictionTime.remove(uuid);
    }
    
    // ===== 事件處理器整合 =====
    
    /**
     * 這個類需要註冊到事件總線
     * 在主模組初始化時調用：
     * MinecraftForge.EVENT_BUS.register(PredictionSystem.EventHandler.class);
     */
    public static class EventHandler {
        
        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            EntityPlayer player = event.player;
            
            // 每秒應用一次預測獎勵
            if (player.world.getTotalWorldTime() % 20 == 0) {
                applyPredictiveBonus(player);
            }
        }
        
        @SubscribeEvent
        public void onBlockBreak(BlockEvent.BreakEvent event) {
            recordAction(event.getPlayer(), ActionType.MINING);
        }
        
        @SubscribeEvent
        public void onBlockPlace(BlockEvent.PlaceEvent event) {
            recordAction(event.getPlayer(), ActionType.BUILDING);
        }
        
        @SubscribeEvent
        public void onLivingHurt(LivingHurtEvent event) {
            if (event.getSource().getTrueSource() instanceof EntityPlayer) {
                recordAction((EntityPlayer) event.getSource().getTrueSource(), ActionType.COMBAT);
            }
        }
    }
    
    // ===== 調試工具 =====
    
    /**
     * 獲取玩家的行為統計（用於調試）
     */
    public static String getDebugInfo(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        List<ActionPattern> history = actionHistory.get(uuid);
        
        if (history == null || history.isEmpty()) {
            return "無數據";
        }
        
        Map<ActionType, Integer> counts = new HashMap<>();
        for (ActionPattern pattern : history) {
            counts.put(pattern.type, counts.getOrDefault(pattern.type, 0) + 1);
        }
        
        ActionType predicted = predictedNextAction.get(uuid);
        
        StringBuilder sb = new StringBuilder();
        sb.append("行為歷史（最近 ").append(history.size()).append(" 個動作）\n");
        for (Map.Entry<ActionType, Integer> entry : counts.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("預測下一動作: ").append(predicted != null ? predicted : "無");
        
        return sb.toString();
    }
}
