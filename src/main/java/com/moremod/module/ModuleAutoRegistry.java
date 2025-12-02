package com.moremod.module;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.UpgradeType;
import com.moremod.item.upgrades.ItemUpgradeComponent;
import com.moremod.module.handler.AreaMiningBoostHandler;
import com.moremod.module.handler.RangedDamageBoostHandler;
import net.minecraft.item.Item;
import net.minecraft.util.text.TextFormatting;

import java.util.*;

/**
 * 模块自动注册系统
 *
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                           自动化注册流程                                       ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  使用方法:                                                                    ║
 * ║                                                                              ║
 * ║  1. 在 ModuleAutoRegistry.registerAllModules() 中定义模块:                    ║
 * ║                                                                              ║
 * ║     ModuleDefinition.builder("MAGIC_ABSORB")                                 ║
 * ║         .displayName("魔力吸收")                                              ║
 * ║         .color(TextFormatting.DARK_PURPLE)                                   ║
 * ║         .category(Category.COMBAT)                                           ║
 * ║         .maxLevel(3)                                                         ║
 * ║         .levelDescriptions(lv -> switch(lv) { ... })                         ║
 * ║         .register();                                                         ║
 * ║                                                                              ║
 * ║  2. 在 UpgradeType 枚举中添加对应值 (目前仍需手动)                              ║
 * ║                                                                              ║
 * ║  3. 实现效果 (事件处理器/Mixin)                                                ║
 * ║                                                                              ║
 * ║  自动完成:                                                                    ║
 * ║  ✓ ItemMechanicalCoreExtended 注册                                           ║
 * ║  ✓ 物品实例创建                                                               ║
 * ║  ✓ Forge Registry 注册                                                       ║
 * ║  ✓ EXTENDED_UPGRADE_IDS 填充                                                 ║
 * ║  ✓ 语言文件 fallback (显示名称)                                               ║
 * ║                                                                              ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
public class ModuleAutoRegistry {

    // 所有注册的模块定义
    private static final Map<String, ModuleDefinition> DEFINITIONS = new LinkedHashMap<>();

    // 生成的物品实例 (ID -> Level -> Item)
    private static final Map<String, Map<Integer, ItemUpgradeComponent>> ITEMS = new LinkedHashMap<>();

    // 【优化】有 handler 的模块缓存 (避免每次遍历时检查 hasHandler)
    private static List<ModuleDefinition> HANDLER_MODULES_CACHE = null;

    // 是否已初始化
    private static boolean initialized = false;

    /**
     * 注册模块定义
     */
    public static void register(ModuleDefinition definition) {
        DEFINITIONS.put(definition.id, definition);
        // 清除缓存以便重新生成
        HANDLER_MODULES_CACHE = null;
        System.out.println("[ModuleAutoRegistry] 注册模块定义: " + definition.id);
    }

    /**
     * 初始化 - 在 Forge 注册事件之前调用
     * 创建所有物品实例并注册到 ItemMechanicalCoreExtended
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        System.out.println("[ModuleAutoRegistry] ========== 开始自动注册 ==========");

        // 先注册所有模块定义
        registerAllModules();

        // 处理每个模块
        for (ModuleDefinition def : DEFINITIONS.values()) {
            // 1. 注册到 ItemMechanicalCoreExtended
            registerToMechanicalCoreExtended(def);

            // 2. 创建物品实例
            createItemInstances(def);
        }

        // 刷新 ItemMechanicalCore 的扩展模块ID缓存
        try {
            ItemMechanicalCore.refreshExtendedUpgradeIds();
            System.out.println("[ModuleAutoRegistry] 已刷新 ItemMechanicalCore 扩展ID缓存");
        } catch (Throwable e) {
            System.err.println("[ModuleAutoRegistry] 刷新缓存失败: " + e.getMessage());
        }

        System.out.println("[ModuleAutoRegistry] ========== 注册完成: " + DEFINITIONS.size() + " 个模块 ==========");
    }

    /**
     * 在这里定义所有自动注册的模块
     * 新增模块只需在这里添加定义即可
     */
    private static void registerAllModules() {
        // ===== 示例：魔力吸收模块 =====
        // 注意：这个模块已经在 UpgradeItemsExtended 中手动定义了
        // 这里只是展示如何使用自动注册系统
        // 实际使用时，选择其中一种方式即可

        /*
        ModuleDefinition.builder("MAGIC_ABSORB")
            .displayName("魔力吸收")
            .color(TextFormatting.DARK_PURPLE)
            .category(ModuleDefinition.Category.COMBAT)
            .maxLevel(3)
            .levelDescriptions(lv -> {
                switch (lv) {
                    case 1: return new String[]{
                        TextFormatting.DARK_PURPLE + "魔力吸收 I",
                        TextFormatting.GRAY + "将魔力吸收升级至 Lv.1",
                        "",
                        TextFormatting.YELLOW + "▶ 吸收少量法伤",
                        TextFormatting.GRAY + "并转化为物理力量",
                        TextFormatting.RED + "叠加少量余灼"
                    };
                    case 2: return new String[]{
                        TextFormatting.DARK_PURPLE + "魔力吸收 II",
                        TextFormatting.GRAY + "将魔力吸收升级至 Lv.2",
                        "",
                        TextFormatting.YELLOW + "▶ 更高法伤吸收率",
                        TextFormatting.GOLD + "▶ 更快余灼累积"
                    };
                    case 3: return new String[]{
                        TextFormatting.DARK_PURPLE + "✦ 魔力吸收 III ✦",
                        TextFormatting.GRAY + "将魔力吸收升级至最高等级",
                        "",
                        TextFormatting.YELLOW + "▶ 强化吸收倍率",
                        TextFormatting.RED + "▶ 余灼满载触发『魔力爆心』"
                    };
                    default: return new String[]{};
                }
            })
            .register();
        */

        // ===== 在这里添加新模块 =====

        // 范围挖掘模块 (Vein Mining) - 使用自动注册 + 事件处理器
        ModuleDefinition.builder("AREA_MINING_BOOST")
            .displayName("范围挖掘")
            .color(TextFormatting.YELLOW)
            .category(ModuleDefinition.Category.AUXILIARY)
            .maxLevel(3)
            .levelDescriptions(lv -> {
                switch (lv) {
                    case 1: return new String[]{
                        TextFormatting.YELLOW + "范围挖掘 I",
                        TextFormatting.GRAY + "将范围挖掘升级至 Lv.1",
                        "",
                        TextFormatting.YELLOW + "▶ 连锁数量: 8个方块",
                        TextFormatting.AQUA + "▶ 能耗: 50 RF/方块",
                        TextFormatting.GRAY + "自动挖掘相邻同类型方块",
                        TextFormatting.DARK_GRAY + "基础连锁挖掘"
                    };
                    case 2: return new String[]{
                        TextFormatting.YELLOW + "范围挖掘 II",
                        TextFormatting.GRAY + "将范围挖掘升级至 Lv.2",
                        "",
                        TextFormatting.YELLOW + "▶ 连锁数量: 16个方块",
                        TextFormatting.AQUA + "▶ 能耗: 50 RF/方块",
                        TextFormatting.BLUE + "高效连锁系统"
                    };
                    case 3: return new String[]{
                        TextFormatting.YELLOW + "✦ 范围挖掘 III ✦",
                        TextFormatting.GRAY + "将范围挖掘升级至最高等级",
                        "",
                        TextFormatting.YELLOW + "▶ 连锁数量: 32个方块",
                        TextFormatting.AQUA + "▶ 能耗: 50 RF/方块",
                        TextFormatting.LIGHT_PURPLE + "矿脉粉碎",
                        TextFormatting.RED + "已达最高等级"
                    };
                    default: return new String[]{};
                }
            })
            .handler(new AreaMiningBoostHandler())
            .register();

        // 远程伤害增幅模块 - 增加弓箭等远程武器伤害
        ModuleDefinition.builder("RANGED_DAMAGE_BOOST")
            .displayName("远程伤害增幅")
            .color(TextFormatting.GOLD)
            .category(ModuleDefinition.Category.COMBAT)
            .maxLevel(3)
            .levelDescriptions(lv -> {
                switch (lv) {
                    case 1: return new String[]{
                        TextFormatting.GOLD + "远程增幅 I",
                        TextFormatting.GRAY + "将远程伤害增幅升级至 Lv.1",
                        "",
                        TextFormatting.YELLOW + "▶ 远程伤害: +15%",
                        TextFormatting.AQUA + "▶ 适用: 弓箭、投掷物",
                        TextFormatting.DARK_GRAY + "基础远程增幅"
                    };
                    case 2: return new String[]{
                        TextFormatting.GOLD + "远程增幅 II",
                        TextFormatting.GRAY + "将远程伤害增幅升级至 Lv.2",
                        "",
                        TextFormatting.YELLOW + "▶ 远程伤害: +30%",
                        TextFormatting.BLUE + "强化远程增幅"
                    };
                    case 3: return new String[]{
                        TextFormatting.GOLD + "✦ 远程增幅 III ✦",
                        TextFormatting.GRAY + "将远程伤害增幅升级至最高等级",
                        "",
                        TextFormatting.YELLOW + "▶ 远程伤害: +50%",
                        TextFormatting.LIGHT_PURPLE + "精准射击大师",
                        TextFormatting.RED + "已达最高等级"
                    };
                    default: return new String[]{};
                }
            })
            .handler(new RangedDamageBoostHandler())
            .register();
    }

    /**
     * 注册到 ItemMechanicalCoreExtended
     */
    private static void registerToMechanicalCoreExtended(ModuleDefinition def) {
        try {
            // 转换 Category
            ItemMechanicalCoreExtended.UpgradeCategory category;
            switch (def.category) {
                case SURVIVAL: category = ItemMechanicalCoreExtended.UpgradeCategory.SURVIVAL; break;
                case AUXILIARY: category = ItemMechanicalCoreExtended.UpgradeCategory.AUXILIARY; break;
                case COMBAT: category = ItemMechanicalCoreExtended.UpgradeCategory.COMBAT; break;
                case ENERGY: category = ItemMechanicalCoreExtended.UpgradeCategory.ENERGY; break;
                default: category = ItemMechanicalCoreExtended.UpgradeCategory.AUXILIARY;
            }

            ItemMechanicalCoreExtended.register(
                def.id,
                def.displayName,
                def.color,
                def.maxLevel,
                category
            );
            System.out.println("[ModuleAutoRegistry] -> ItemMechanicalCoreExtended: " + def.id);
        } catch (Exception e) {
            System.err.println("[ModuleAutoRegistry] 注册到 ItemMechanicalCoreExtended 失败: " + def.id);
            e.printStackTrace();
        }
    }

    /**
     * 创建物品实例
     */
    private static void createItemInstances(ModuleDefinition def) {
        Map<Integer, ItemUpgradeComponent> levelItems = new LinkedHashMap<>();

        for (int lv = 1; lv <= def.maxLevel; lv++) {
            try {
                // 尝试获取 UpgradeType
                UpgradeType upgradeType = null;
                try {
                    upgradeType = UpgradeType.valueOf(def.id);
                } catch (IllegalArgumentException e) {
                    // UpgradeType 不存在，使用字符串构造函数
                    System.out.println("[ModuleAutoRegistry] 警告: UpgradeType." + def.id + " 不存在，请手动添加到枚举");
                }

                ItemUpgradeComponent item;
                if (upgradeType != null) {
                    item = new ItemUpgradeComponent(upgradeType, def.getDescriptions(lv), lv);
                } else {
                    // 使用字符串ID构造
                    item = new ItemUpgradeComponent(def.id, def.getDescriptions(lv), lv);
                }

                item.setRegistryName("moremod", def.getRegistryName(lv));
                item.setTranslationKey(def.getRegistryName(lv));
                item.setCreativeTab(moremodCreativeTab.moremod_TAB);
                item.setMaxStackSize(def.getStackSize(lv));

                levelItems.put(lv, item);
                System.out.println("[ModuleAutoRegistry] -> 创建物品: " + def.getRegistryName(lv));

            } catch (Exception e) {
                System.err.println("[ModuleAutoRegistry] 创建物品失败: " + def.getRegistryName(lv));
                e.printStackTrace();
            }
        }

        ITEMS.put(def.id, levelItems);
    }

    // ========== 获取方法 ==========

    /**
     * 获取所有自动注册的物品 (用于 Forge Registry)
     */
    public static Item[] getAllItems() {
        List<Item> allItems = new ArrayList<>();
        for (Map<Integer, ItemUpgradeComponent> levelMap : ITEMS.values()) {
            allItems.addAll(levelMap.values());
        }
        return allItems.toArray(new Item[0]);
    }

    /**
     * 获取所有模块ID (用于 EXTENDED_UPGRADE_IDS)
     */
    public static String[] getAllModuleIds() {
        return DEFINITIONS.keySet().toArray(new String[0]);
    }

    /**
     * 获取指定模块的物品
     */
    public static ItemUpgradeComponent getItem(String moduleId, int level) {
        Map<Integer, ItemUpgradeComponent> levelMap = ITEMS.get(moduleId.toUpperCase());
        return levelMap != null ? levelMap.get(level) : null;
    }

    /**
     * 获取模块定义
     */
    public static ModuleDefinition getDefinition(String moduleId) {
        return DEFINITIONS.get(moduleId.toUpperCase());
    }

    /**
     * 获取所有模块定义
     */
    public static Collection<ModuleDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    /**
     * 【优化】获取所有有 handler 的模块定义
     * 使用缓存避免每次遍历时检查 hasHandler()
     */
    public static List<ModuleDefinition> getHandlerModules() {
        if (HANDLER_MODULES_CACHE == null) {
            HANDLER_MODULES_CACHE = new ArrayList<>();
            for (ModuleDefinition def : DEFINITIONS.values()) {
                if (def.hasHandler()) {
                    HANDLER_MODULES_CACHE.add(def);
                }
            }
        }
        return HANDLER_MODULES_CACHE;
    }

    /**
     * 生成语言文件条目 (可用于调试或生成)
     */
    public static Map<String, String> generateLangEntries() {
        Map<String, String> entries = new LinkedHashMap<>();
        for (ModuleDefinition def : DEFINITIONS.values()) {
            for (int lv = 1; lv <= def.maxLevel; lv++) {
                entries.put(def.getLangKey(lv), def.displayName + "lv" + lv + " 升级组件");
            }
        }
        return entries;
    }

    /**
     * 打印所有语言文件条目 (方便复制到 .lang 文件)
     */
    public static void printLangEntries() {
        System.out.println("===== 语言文件条目 =====");
        for (Map.Entry<String, String> entry : generateLangEntries().entrySet()) {
            System.out.println(entry.getKey() + "=" + entry.getValue());
        }
        System.out.println("========================");
    }
}
