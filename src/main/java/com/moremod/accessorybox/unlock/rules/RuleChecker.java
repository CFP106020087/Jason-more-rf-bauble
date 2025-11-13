package com.moremod.accessorybox.unlock.rules;

import com.moremod.accessorybox.unlock.SlotUnlockManager;
import com.moremod.accessorybox.unlock.rules.progress.ProgressTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/**
 * 规则检查器（修正版）
 * 
 * ⭐ 关键变化：
 * 1. 区分永久解锁和临时解锁
 * 2. 临时解锁失效时自动处理槽位物品
 * 3. 定期检查所有规则
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class RuleChecker {

    // 所有解锁规则
    private static List<UnlockRule> allRules = new ArrayList<>();
    
    // 按槽位分组的规则: SlotID -> List<Rule>
    private static Map<Integer, List<UnlockRule>> rulesBySlot = new HashMap<>();
    
    // 玩家检查计时器: UUID -> tick计数
    private static final Map<UUID, Integer> playerTickers = new HashMap<>();

    /**
     * 系统初始化 - 解析所有规则
     */
    public static void initialize() {
        if (!UnlockRulesConfig.enableRuleSystem) {
            System.out.println("[RuleChecker] 规则系统已禁用");
            return;
        }

        System.out.println("[RuleChecker] 初始化解锁规则系统...");
        
        // 解析规则
        allRules = UnlockRuleParser.parseAll();
        rulesBySlot = UnlockRuleParser.groupBySlot(allRules);
        
        System.out.println("[RuleChecker] 加载了 " + allRules.size() + " 条规则");
        System.out.println("[RuleChecker] 涉及 " + rulesBySlot.size() + " 个槽位");
        
        if (UnlockRulesConfig.debugMode) {
            for (Map.Entry<Integer, List<UnlockRule>> entry : rulesBySlot.entrySet()) {
                System.out.println("[RuleChecker]   槽位 " + entry.getKey() + ": " 
                    + entry.getValue().size() + " 条规则");
            }
        }
    }

    /**
     * 玩家登录 - 加载进度并立即检查
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!UnlockRulesConfig.enableRuleSystem) return;
        
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        // 加载进度数据
        ProgressTracker.loadFromPlayer(player);
        
        // 立即检查一次
        checkAndUnlockSlots(player);
        
        if (UnlockRulesConfig.debugMode) {
            System.out.println("[RuleChecker] 玩家登录，已检查规则: " + player.getName());
        }
    }

    /**
     * 玩家登出 - 清理数据
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.player.getUniqueID();
        playerTickers.remove(uuid);
        ProgressTracker.onPlayerLogout(uuid);
    }

    /**
     * 玩家Tick事件 - 定期检查
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!UnlockRulesConfig.enableRuleSystem) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        UUID uuid = player.getUniqueID();

        // 增加计数器
        int ticks = playerTickers.getOrDefault(uuid, 0) + 1;

        // 达到检查间隔时执行检查
        if (ticks >= UnlockRulesConfig.checkInterval) {
            if (UnlockRulesConfig.debugMode) {
                System.out.println("[RuleChecker] 定期检查玩家: " + player.getName() + " (间隔: " + UnlockRulesConfig.checkInterval + " ticks)");
            }
            checkAndUnlockSlots(player);
            ticks = 0;
        }

        playerTickers.put(uuid, ticks);
    }

    /**
     * ⭐ 核心方法：检查并解锁槽位
     */
    public static void checkAndUnlockSlots(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) return;
        
        Set<Integer> currentTempUnlocks = new HashSet<>();
        boolean anyPermanentChange = false;

        // 遍历所有槽位的规则
        for (Map.Entry<Integer, List<UnlockRule>> entry : rulesBySlot.entrySet()) {
            int slotId = entry.getKey();
            List<UnlockRule> rules = entry.getValue();
            
            // 检查槽位的所有规则
            RuleCheckResult result = checkSlotRules(player, rules);
            
            // ⭐ 处理永久解锁
            if (result.shouldPermanentUnlock) {
                boolean unlocked = SlotUnlockManager.getInstance()
                    .unlockSlotPermanent(player, slotId);
                
                if (unlocked) {
                    anyPermanentChange = true;
                    
                    if (UnlockRulesConfig.debugMode) {
                        System.out.println("[RuleChecker] 永久解锁槽位 " + slotId + 
                            " for " + player.getName());
                    }
                    
                    // 发送提示消息（可选）
                    sendUnlockMessage(player, slotId, false);
                }
            }
            
            // ⭐ 收集临时解锁
            if (result.shouldTemporaryUnlock) {
                currentTempUnlocks.add(slotId);
            }
        }
        
        // ⭐ 更新临时解锁状态（会自动处理失效的槽位）
        if (UnlockRulesConfig.debugMode) {
            System.out.println("[RuleChecker] 当前临时解锁槽位: " + currentTempUnlocks);
        }
        SlotUnlockManager.getInstance().updateTemporaryUnlocks(player, currentTempUnlocks);
    }

    /**
     * 检查槽位的所有规则
     * @return 检查结果（是否应该永久解锁、是否应该临时解锁）
     */
    private static RuleCheckResult checkSlotRules(EntityPlayer player, List<UnlockRule> rules) {
        if (rules.isEmpty()) {
            return new RuleCheckResult(false, false);
        }
        
        boolean hasPermanent = false;
        boolean permSatisfied = false;
        boolean hasTemporary = false;
        boolean tempSatisfied = false;
        
        for (UnlockRule rule : rules) {
            UnlockCondition condition = rule.getCondition();
            boolean satisfied = condition.check(player);
            
            if (condition.isTemporary()) {
                hasTemporary = true;
                if (satisfied) {
                    tempSatisfied = true;
                }
            } else {
                hasPermanent = true;
                if (satisfied) {
                    permSatisfied = true;
                }
            }
        }
        
        // OR模式: 满足任意条件即可
        if (UnlockRulesConfig.ruleOrMode) {
            return new RuleCheckResult(permSatisfied, tempSatisfied);
        }
        
        // AND模式: 必须满足所有类型的条件
        boolean perm = !hasPermanent || permSatisfied;
        boolean temp = !hasTemporary || tempSatisfied;
        return new RuleCheckResult(perm && hasPermanent, temp && hasTemporary);
    }

    /**
     * 规则检查结果
     */
    private static class RuleCheckResult {
        final boolean shouldPermanentUnlock;
        final boolean shouldTemporaryUnlock;
        
        RuleCheckResult(boolean permanent, boolean temporary) {
            this.shouldPermanentUnlock = permanent;
            this.shouldTemporaryUnlock = temporary;
        }
    }

    /**
     * 发送解锁提示消息（可选）
     */
    private static void sendUnlockMessage(EntityPlayer player, int slotId, boolean temporary) {
        // 可以发送聊天消息提示玩家
        // player.sendMessage(new TextComponentString("槽位已解锁！"));
    }

    /**
     * 强制检查指定玩家
     */
    public static void forceCheck(EntityPlayer player) {
        checkAndUnlockSlots(player);
    }

    /**
     * 清除所有缓存
     */
    public static void clearCache() {
        allRules.clear();
        rulesBySlot.clear();
        playerTickers.clear();
    }

    /**
     * 重新加载规则
     */
    public static void reload() {
        clearCache();
        initialize();
    }
}
