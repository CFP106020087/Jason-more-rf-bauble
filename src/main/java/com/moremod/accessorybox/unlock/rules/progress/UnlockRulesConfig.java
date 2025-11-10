package com.moremod.accessorybox.unlock.rules.progress;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 槽位解锁规则配置 - 完全可配置版本
 * 所有规则都通过字符串在配置文件中定义
 */
@Config(modid = "moremod", name = "moremod/unlock_rules")
public class UnlockRulesConfig {

    @Config.Comment({
            "=== 槽位解锁规则配置 ===",
            "",
            "规则格式: \"槽位类型:索引|条件类型|参数1|参数2|...|选项\"",
            "",
            "【槽位类型】",
            "  AMULET   - 项链",
            "  RING     - 戒指",
            "  BELT     - 腰带",
            "  HEAD     - 头部",
            "  BODY     - 身体",
            "  CHARM    - 挂饰",
            "  TRINKET  - 万能",
            "",
            "【索引】额外槽位的索引，从0开始",
            "  例如: RING:0 表示第1个额外戒指, RING:1 表示第2个额外戒指",
            "",
            "========================================",
            "【条件类型及参数】",
            "========================================",
            "",
            "1. item_consume - 消耗/吃掉物品",
            "   格式: \"槽位|item_consume|物品ID|数量(可选)\"",
            "   示例:",
            "     \"RING:0|item_consume|minecraft:golden_apple\" - 吃金苹果",
            "     \"RING:1|item_consume|minecraft:golden_apple:1\" - 吃附魔金苹果",
            "     \"AMULET:0|item_consume|minecraft:cooked_beef|64\" - 吃64个牛排",
            "",
            "2. item_use - 使用物品(右键)",
            "   格式: \"槽位|item_use|物品ID\"",
            "   示例:",
            "     \"BELT:0|item_use|minecraft:ender_pearl\" - 使用末影珍珠",
            "     \"HEAD:0|item_use|minecraft:flint_and_steel\" - 使用打火石",
            "",
            "3. item_pickup - 拾取物品",
            "   格式: \"槽位|item_pickup|物品ID|数量(可选)\"",
            "   示例:",
            "     \"TRINKET:0|item_pickup|minecraft:nether_star\" - 拾取下界之星",
            "     \"CHARM:0|item_pickup|minecraft:diamond|32\" - 拾取32个钻石",
            "",
            "4. item_crafting - 合成物品",
            "   格式: \"槽位|item_crafting|物品ID|数量(可选)\"",
            "   示例:",
            "     \"RING:2|item_crafting|minecraft:diamond_sword\" - 合成钻石剑",
            "     \"BELT:0|item_crafting|minecraft:enchanting_table\" - 合成附魔台",
            "",
            "5. kill_entity - 击杀实体",
            "   格式: \"槽位|kill_entity|实体ID|数量(可选)\"",
            "   示例:",
            "     \"TRINKET:0|kill_entity|minecraft:ender_dragon\" - 击杀末影龙",
            "     \"TRINKET:1|kill_entity|minecraft:wither\" - 击杀凋灵",
            "     \"RING:3|kill_entity|minecraft:zombie|100\" - 击杀100个僵尸",
            "     \"AMULET:1|kill_entity|minecraft:blaze|50\" - 击杀50个烈焰人",
            "",
            "6. wear_bauble - 佩戴指定饰品",
            "   格式: \"槽位|wear_bauble|饰品ID|temporary(可选)\"",
            "   示例:",
            "     \"AMULET:0|wear_bauble|baubles:ring_overclocking\" - 佩戴超频戒指",
            "     \"RING:0|wear_bauble|baubles:ring_overclocking|temporary\" - 临时解锁(摘下重新锁定)",
            "     \"HEAD:0|wear_bauble|thaumcraft:baubles:2\" - 佩戴神秘帽(带meta值)",
            "",
            "7. advancement - 获得进度/成就",
            "   格式: \"槽位|advancement|进度ID\"",
            "   示例:",
            "     \"RING:1|advancement|minecraft:story/mine_diamond\" - 获得钻石",
            "     \"BELT:0|advancement|minecraft:story/enchant_item\" - 附魔物品",
            "     \"TRINKET:2|advancement|minecraft:nether/all_potions\" - 获得所有药水效果",
            "     \"HEAD:0|advancement|minecraft:end/dragon_egg\" - 获得龙蛋",
            "",
            "8. level - 等级要求",
            "   格式: \"槽位|level|等级\"",
            "   示例:",
            "     \"RING:2|level|10\" - 等级达到10级",
            "     \"AMULET:1|level|25\" - 等级达到25级",
            "     \"TRINKET:3|level|50\" - 等级达到50级",
            "",
            "9. dimension - 进入指定维度",
            "   格式: \"槽位|dimension|维度ID\"",
            "   示例:",
            "     \"HEAD:0|dimension|1\" - 进入末地(维度1)",
            "     \"BODY:0|dimension|-1\" - 进入下界(维度-1)",
            "     \"CHARM:0|dimension|7\" - 进入暮色森林(维度7)",
            "",
            "10. inventory_item - 背包中持有物品",
            "    格式: \"槽位|inventory_item|物品ID|数量(可选)\"",
            "    示例:",
            "      \"BELT:1|inventory_item|minecraft:diamond|16\" - 拥有16个钻石",
            "      \"AMULET:2|inventory_item|minecraft:emerald_block\" - 拥有绿宝石块",
            "",
            "11. equipped_item - 装备指定物品",
            "    格式: \"槽位|equipped_item|物品ID|槽位(可选)\"",
            "    示例:",
            "      \"HEAD:1|equipped_item|minecraft:diamond_helmet\" - 装备钻石头盔",
            "      \"BODY:1|equipped_item|minecraft:elytra|chest\" - 装备鞘翅",
            "",
            "========================================",
            "【选项说明】",
            "========================================",
            "",
            "  temporary - 临时解锁，条件不满足时重新锁定",
            "              主要用于 wear_bauble 条件",
            "              摘下饰品后槽位会被锁定，物品行为见下方配置",
            "",
            "========================================",
            "【配置示例】",
            "========================================",
            "",
            "// 吃金苹果解锁第1个额外戒指",
            "\"RING:0|item_consume|minecraft:golden_apple\"",
            "",
            "// 吃附魔金苹果解锁第2个额外戒指", 
            "\"RING:1|item_consume|minecraft:golden_apple:1\"",
            "",
            "// 击杀末影龙解锁第1个万能槽位",
            "\"TRINKET:0|kill_entity|minecraft:ender_dragon\"",
            "",
            "// 击杀凋灵解锁第2个万能槽位",
            "\"TRINKET:1|kill_entity|minecraft:wither\"",
            "",
            "// 佩戴超频戒指时临时解锁项链(摘下重新锁定)",
            "\"AMULET:0|wear_bauble|baubles:ring_overclocking|temporary\"",
            "",
            "// 10级解锁第3个戒指",
            "\"RING:2|level|10\"",
            "",
            "// 进入末地解锁头部槽位",
            "\"HEAD:0|dimension|1\"",
            "",
            "// 获得钻石成就解锁第4个戒指",
            "\"RING:3|advancement|minecraft:story/mine_diamond\"",
            "",
            "// 合成钻石剑解锁腰带",
            "\"BELT:0|item_crafting|minecraft:diamond_sword\"",
            "",
            "// 拾取下界之星解锁挂饰",
            "\"CHARM:0|item_pickup|minecraft:nether_star\"",
            "",
            "// 击杀100个僵尸解锁第5个戒指",
            "\"RING:4|kill_entity|minecraft:zombie|100\"",
            "",
            "========================================",
            "修改后需要重启游戏生效"
    })
    @Config.Name("解锁规则列表 | Unlock Rules")
    @Config.RequiresMcRestart
    public static String[] unlockRules = {
            // 默认示例规则 - 可以根据需要修改或删除
            
            // 戒指系列 - 食物主题
            "RING:0|item_consume|minecraft:golden_apple",
            "RING:1|item_consume|minecraft:golden_apple:1",
            "RING:2|level|10",
            
            // 项链系列 - 成就主题
            "AMULET:0|advancement|minecraft:story/mine_diamond",
            
            // 万能系列 - Boss主题
            "TRINKET:0|kill_entity|minecraft:ender_dragon",
            "TRINKET:1|kill_entity|minecraft:wither",
            
            // 临时解锁示例
            "AMULET:1|wear_bauble|baubles:ring_overclocking|temporary"
    };

    @Config.Comment({
            "启用解锁规则系统",
            "false = 禁用，所有槽位按默认解锁配置"
    })
    @Config.Name("启用规则系统 | Enable Rule System")
    @Config.RequiresMcRestart
    public static boolean enableRuleSystem = true;

    @Config.Comment({
            "多条规则指向同一槽位时的逻辑",
            "true  - OR模式: 满足任意一条规则即可解锁",
            "false - AND模式: 必须满足所有规则才能解锁"
    })
    @Config.Name("规则OR模式 | Rule OR Mode")
    public static boolean ruleOrMode = true;

    @Config.Comment({
            "临时解锁的槽位在条件失效时，槽位中物品的处理方式",
            "drop      - 掉落到地上",
            "inventory - 尝试放入背包，背包满则掉落",
            "keep      - 保持在槽位(下次打开GUI时无法访问，需重新满足条件)"
    })
    @Config.Name("临时槽位失效行为 | Temporary Slot Behavior")
    public static String temporarySlotBehavior = "inventory";

    @Config.Comment({
            "规则检查间隔(tick)",
            "用于实时检查临时解锁条件",
            "建议: 20-100 (1-5秒)",
            "过低会影响性能"
    })
    @Config.Name("检查间隔 | Check Interval")
    @Config.RangeInt(min = 1, max = 600)
    public static int checkInterval = 40;

    @Config.Comment({
            "调试模式",
            "true - 在控制台输出详细的规则检查日志",
            "false - 静默运行"
    })
    @Config.Name("调试模式 | Debug Mode")
    public static boolean debugMode = false;

    // ==================== 高级选项 ====================

    @Config.Comment({
            "是否保存玩家的解锁进度",
            "true  - 玩家的解锁状态持久化保存",
            "false - 每次登录重新检查条件"
    })
    @Config.Name("保存解锁进度 | Save Progress")
    public static boolean saveProgress = true;

    @Config.Comment({
            "击杀计数是否跨维度累计",
            "true  - 在任何维度击杀都会计数",
            "false - 只在主世界计数"
    })
    @Config.Name("跨维度计数 | Cross Dimension Count")
    public static boolean crossDimensionCount = true;

    @Config.Comment({
            "物品拾取计数是否包含背包中已有的",
            "true  - 检查时统计背包现有数量",
            "false - 只统计拾取动作"
    })
    @Config.Name("计入已有物品 | Count Existing Items")
    public static boolean countExistingItems = false;

    // ==================== 工具方法 ====================

    /**
     * 获取当前有效的解锁规则
     */
    public static String[] getActiveRules() {
        if (!enableRuleSystem) {
            return new String[0];
        }
        return unlockRules;
    }

    /**
     * 打印当前配置
     */
    public static void printConfig() {
        System.out.println("========================================");
        System.out.println("[UnlockRules] 槽位解锁规则配置");
        System.out.println("========================================");
        System.out.println("规则系统: " + (enableRuleSystem ? "启用" : "禁用"));
        
        if (enableRuleSystem) {
            System.out.println("规则模式: " + (ruleOrMode ? "OR(任意满足)" : "AND(全部满足)"));
            System.out.println("检查间隔: " + checkInterval + " ticks");
            System.out.println("临时槽位行为: " + temporarySlotBehavior);
            System.out.println("保存进度: " + (saveProgress ? "是" : "否"));
            System.out.println("调试模式: " + (debugMode ? "开启" : "关闭"));
            System.out.println("--------------------------------------");
            System.out.println("当前规则数量: " + unlockRules.length);
            
            if (debugMode && unlockRules.length > 0) {
                System.out.println("\n规则详情:");
                for (int i = 0; i < unlockRules.length; i++) {
                    System.out.println("  [" + (i + 1) + "] " + unlockRules[i]);
                }
            }
        }
        
        System.out.println("========================================");
    }

    /**
     * 验证规则格式
     */
    public static boolean validateRules() {
        boolean valid = true;
        
        for (int i = 0; i < unlockRules.length; i++) {
            String rule = unlockRules[i];
            if (rule == null || rule.trim().isEmpty()) {
                continue;
            }
            
            String[] parts = rule.split("\\|");
            if (parts.length < 3) {
                System.err.println("[UnlockRules] 规则格式错误 [" + i + "]: " + rule);
                System.err.println("[UnlockRules] 至少需要3个部分: 槽位|条件类型|参数");
                valid = false;
                continue;
            }
            
            // 验证槽位格式
            if (!parts[0].contains(":")) {
                System.err.println("[UnlockRules] 槽位格式错误 [" + i + "]: " + parts[0]);
                System.err.println("[UnlockRules] 应为: 类型:索引 (例如: RING:0)");
                valid = false;
            }
        }
        
        return valid;
    }

    @Mod.EventBusSubscriber(modid = "moremod")
    public static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("moremod")) {
                ConfigManager.sync("moremod", Config.Type.INSTANCE);
                
                // 验证并打印配置
                if (validateRules()) {
                    printConfig();
                } else {
                    System.err.println("[UnlockRules] 配置验证失败，请检查规则格式！");
                }
            }
        }
    }
}
