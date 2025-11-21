#priority 1000

/*
 * 宝石掉落配置 - 平衡版
 *
 * 使用方法：
 * 1. 将此文件复制到 Minecraft 实例的 scripts/ 目录
 * 2. 重启游戏或使用 /ct reload 重载
 * 3. 查看日志确认配置已加载
 */

import mods.moremod.GemLootRules;
import mods.moremod.GemLootConfig;

print("========================================");
print("      宝石掉落配置 - 平衡版");
print("========================================");

// ==========================================
// 第一步：加载预设规则
// ==========================================
GemLootRules.setupAllRules();
print("  ✓ 已配置预设规则（Champions、Infernal、Dragons等）");

// ==========================================
// 第二步：添加自定义原版生物规则
// ==========================================
GemLootRules.add("zombie", 5, 15, 1, 2, 0.03);
GemLootRules.add("skeleton", 5, 15, 1, 2, 0.03);
GemLootRules.add("creeper", 10, 20, 2, 3, 0.05);
GemLootRules.add("enderman", 20, 40, 2, 4, 0.08);
GemLootRules.add("blaze", 20, 40, 2, 3, 0.10);
GemLootRules.add("witch", 25, 45, 3, 4, 0.12);
GemLootRules.add("guardian", 30, 50, 3, 4, 0.15);
GemLootRules.add("elder_guardian", 60, 80, 4, 5, 0.40);
print("  ✓ 已配置 8 个自定义生物规则");

// ==========================================
// 第三步：设置默认规则
// ==========================================
GemLootRules.setDefault(1, 20, 1, 3, 0.02);
print("  ✓ 已设置默认掉落规则");

// ==========================================
// 第四步：设置全局配置（推荐配置）
// ==========================================
GemLootConfig.setFilterPeaceful(true);   // 友善生物不掉落
GemLootConfig.setMaxGemLevel(100);       // 最大宝石等级
GemLootConfig.setHealthBalance(true);    // 血量平衡
GemLootConfig.setDebug(false);           // 关闭调试模式

// ==========================================
// 配置完成
// ==========================================
print("  ✓ 怪物掉落规则配置完成");
print("========================================");
print("✅ 宝石掉落系统已就绪");
print("   - 预设规则：三级龙 Lv25-30, 三王 Lv100");
print("   - 自定义规则：8个原版生物");
print("   - 默认规则：Lv1-20 (2%掉落率)");
print("========================================");
