package com.moremod.accessorybox.unlock.rules;

import com.moremod.accessorybox.unlock.rules.progress.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解锁规则解析器
 * 将配置字符串解析为可执行的规则对象
 */
public class UnlockRuleParser {

    /**
     * 解析所有规则
     */
    public static List<UnlockRule> parseAll() {
        String[] ruleStrings = UnlockRulesConfig.getActiveRules();
        List<UnlockRule> rules = new ArrayList<>();
        
        for (int i = 0; i < ruleStrings.length; i++) {
            try {
                UnlockRule rule = parse(ruleStrings[i]);
                if (rule != null) {
                    rules.add(rule);
                }
            } catch (Exception e) {
                System.err.println("[UnlockRuleParser] 解析规则失败 [" + i + "]: " + ruleStrings[i]);
                e.printStackTrace();
            }
        }
        
        if (UnlockRulesConfig.debugMode) {
            System.out.println("[UnlockRuleParser] 成功解析 " + rules.size() + " 条规则");
        }
        
        return rules;
    }

    /**
     * 解析单条规则
     * 格式: "槽位类型:索引|条件类型|参数1|参数2|...|选项"
     */
    public static UnlockRule parse(String ruleString) {
        if (ruleString == null || ruleString.trim().isEmpty()) {
            return null;
        }

        String[] parts = ruleString.split("\\|");
        if (parts.length < 3) {
            System.err.println("[UnlockRuleParser] 规则格式错误: " + ruleString);
            return null;
        }

        // 解析槽位
        String slotPart = parts[0].trim();
        SlotTarget target = parseSlotTarget(slotPart);
        if (target == null) {
            System.err.println("[UnlockRuleParser] 槽位解析失败: " + slotPart);
            return null;
        }

        // 解析条件类型
        String conditionType = parts[1].trim().toLowerCase();

        // 解析参数
        String[] params = new String[parts.length - 2];
        System.arraycopy(parts, 2, params, 0, parts.length - 2);

        // 创建条件
        UnlockCondition condition = createCondition(conditionType, params);
        if (condition == null) {
            System.err.println("[UnlockRuleParser] 无法创建条件: " + conditionType);
            return null;
        }

        return new UnlockRule(target, condition);
    }

    /**
     * 解析槽位目标
     * 格式: "TYPE:INDEX" 例如 "RING:0"
     */
    private static SlotTarget parseSlotTarget(String slotString) {
        String[] parts = slotString.split(":");
        if (parts.length != 2) {
            return null;
        }

        String type = parts[0].trim().toUpperCase();
        int index;
        try {
            index = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            return null;
        }

        return new SlotTarget(type, index);
    }

    /**
     * 根据类型和参数创建条件对象
     */
    private static UnlockCondition createCondition(String type, String[] params) {
        switch (type) {
            case "item_consume":
                return parseItemConsume(params);
            
            case "item_use":
                return parseItemUse(params);
            
            case "item_pickup":
                return parseItemPickup(params);
            
            case "item_crafting":
            case "crafting":
                return parseItemCrafting(params);
            
            case "kill_entity":
                return parseKillEntity(params);
            
            case "wear_bauble":
                return parseWearBauble(params);
            
            case "advancement":
                return parseAdvancement(params);
            
            case "level":
                return parseLevel(params);
            
            case "dimension":
                return parseDimension(params);
            
            case "inventory_item":
                return parseInventoryItem(params);
            
            case "equipped_item":
                return parseEquippedItem(params);
            
            default:
                System.err.println("[UnlockRuleParser] 未知条件类型: " + type);
                return null;
        }
    }

    // ==================== 各种条件的解析方法 ====================

    private static UnlockCondition parseItemConsume(String[] params) {
        if (params.length < 1) return null;
        String itemId = params[0].trim();
        int count = params.length > 1 ? parseIntSafe(params[1], 1) : 1;
        return new ItemConsumeCondition(itemId, count);
    }

    private static UnlockCondition parseItemUse(String[] params) {
        if (params.length < 1) return null;
        String itemId = params[0].trim();
        return new ItemUseCondition(itemId);
    }

    private static UnlockCondition parseItemPickup(String[] params) {
        if (params.length < 1) return null;
        String itemId = params[0].trim();
        int count = params.length > 1 ? parseIntSafe(params[1], 1) : 1;
        return new ItemPickupCondition(itemId, count);
    }

    private static UnlockCondition parseItemCrafting(String[] params) {
        if (params.length < 1) return null;
        String itemId = params[0].trim();
        int count = params.length > 1 ? parseIntSafe(params[1], 1) : 1;
        return new ItemCraftingCondition(itemId, count);
    }

    private static UnlockCondition parseKillEntity(String[] params) {
        if (params.length < 1) return null;
        String entityId = params[0].trim();
        int count = params.length > 1 ? parseIntSafe(params[1], 1) : 1;
        return new KillEntityCondition(entityId, count);
    }

    private static UnlockCondition parseWearBauble(String[] params) {
        if (params.length < 1) return null;
        String baubleId = params[0].trim();
        boolean temporary = params.length > 1 && params[1].trim().equalsIgnoreCase("temporary");
        return new WearBaubleCondition(baubleId, temporary);
    }

    private static UnlockCondition parseAdvancement(String[] params) {
        if (params.length < 1) return null;
        String advancementId = params[0].trim();
        return new AdvancementCondition(advancementId);
    }

    private static UnlockCondition parseLevel(String[] params) {
        if (params.length < 1) return null;
        int level = parseIntSafe(params[0], 1);
        return new LevelCondition(level);
    }

    private static UnlockCondition parseDimension(String[] params) {
        if (params.length < 1) return null;
        int dimensionId = parseIntSafe(params[0], 0);
        return new DimensionCondition(dimensionId);
    }

    private static UnlockCondition parseInventoryItem(String[] params) {
        if (params.length < 1) return null;
        String itemId = params[0].trim();
        int count = params.length > 1 ? parseIntSafe(params[1], 1) : 1;
        return new InventoryItemCondition(itemId, count);
    }

    private static UnlockCondition parseEquippedItem(String[] params) {
        if (params.length < 1) return null;
        String itemId = params[0].trim();
        String slot = params.length > 1 ? params[1].trim() : null;
        return new EquippedItemCondition(itemId, slot);
    }

    // ==================== 辅助方法 ====================

    private static int parseIntSafe(String str, int defaultValue) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 根据规则重新组织为 Map<槽位, List<规则>>
     */
    public static Map<Integer, List<UnlockRule>> groupBySlot(List<UnlockRule> rules) {
        Map<Integer, List<UnlockRule>> grouped = new HashMap<>();
        
        for (UnlockRule rule : rules) {
            int slotId = rule.getTarget().getSlotId();
            grouped.computeIfAbsent(slotId, k -> new ArrayList<>()).add(rule);
        }
        
        return grouped;
    }
}
