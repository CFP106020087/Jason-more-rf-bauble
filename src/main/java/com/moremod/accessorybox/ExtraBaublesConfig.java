package com.moremod.accessorybox;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 动态分配的额外饰品槽位配置
 * 每种类型独立配置，总和最多42个
 */
@Config(modid = "moremod", name = "moremod/extra_baubles")
public class ExtraBaublesConfig {

    @Config.Comment({
            "=== 额外槽位配置 ===",
            "每种类型独立配置，总和不能超过 42",
            "修改后需要重启游戏"
    })
    @Config.Name("--- 配置说明 ---")
    public static String CONFIG_INFO = "请确保所有额外槽位总和 ≤ 42";

    @Config.Comment({
            "额外项链槽位数量",
            "Extra AMULET slots"
    })
    @Config.Name("额外项链 | Extra Amulets")
    @Config.RangeInt(min = 0, max = 42)
    @Config.RequiresMcRestart
    public static int extraAmulets = 0;

    @Config.Comment({
            "额外戒指槽位数量",
            "Extra RING slots"
    })
    @Config.Name("额外戒指 | Extra Rings")
    @Config.RangeInt(min = 0, max = 42)
    @Config.RequiresMcRestart
    public static int extraRings = 0;

    @Config.Comment({
            "额外腰带槽位数量",
            "Extra BELT slots"
    })
    @Config.Name("额外腰带 | Extra Belts")
    @Config.RangeInt(min = 0, max = 42)
    @Config.RequiresMcRestart
    public static int extraBelts = 0;

    @Config.Comment({
            "额外头部槽位数量",
            "Extra HEAD slots"
    })
    @Config.Name("额外头部 | Extra Heads")
    @Config.RangeInt(min = 0, max = 42)
    @Config.RequiresMcRestart
    public static int extraHeads = 0;

    @Config.Comment({
            "额外身体槽位数量",
            "Extra BODY slots"
    })
    @Config.Name("额外身体 | Extra Bodies")
    @Config.RangeInt(min = 0, max = 42)
    @Config.RequiresMcRestart
    public static int extraBodies = 0;

    @Config.Comment({
            "额外挂饰槽位数量",
            "Extra CHARM slots"
    })
    @Config.Name("额外挂饰 | Extra Charms")
    @Config.RangeInt(min = 0, max = 42)
    @Config.RequiresMcRestart
    public static int extraCharms = 0;

    @Config.Comment({
            "额外万能槽位数量（任何饰品都能放）",
            "Extra TRINKET slots (accepts any bauble)"
    })
    @Config.Name("额外万能 | Extra Trinkets")
    @Config.RangeInt(min = 0, max = 42)
    @Config.RequiresMcRestart
    public static int extraTrinkets = 0;

    @Config.Comment({
            "启用额外槽位系统",
            "Enable extra slots system"
    })
    @Config.Name("启用系统 | Enable System")
    @Config.RequiresMcRestart
    public static boolean enableExtraSlots = true;

    /**
     * 获取总额外槽位数量
     */
    public static int getTotalExtraSlots() {
        if (!enableExtraSlots) return 0;
        return extraAmulets + extraRings + extraBelts +
                extraHeads + extraBodies + extraCharms + extraTrinkets;
    }

    /**
     * 验证配置是否合法
     */
    public static boolean validateConfig() {
        int total = getTotalExtraSlots();
        if (total > 42) {
            System.err.println("[ExtraBaubles] 配置错误：总槽位数 " + total + " 超过最大值 42！");
            System.err.println("[ExtraBaubles] 额外槽位系统将被禁用，请修改配置后重启。");
            return false;
        }
        return true;
    }

    /**
     * 获取总槽位数（包括原版7个）
     */
    public static int getTotalSlots() {
        return 7 + getTotalExtraSlots();
    }

    /**
     * 打印当前配置
     */
    public static void printConfig() {
        System.out.println("========== 额外饰品槽位配置 ==========");
        System.out.println("启用状态: " + (enableExtraSlots ? "开启" : "关闭"));
        System.out.println("额外项链: " + extraAmulets);
        System.out.println("额外戒指: " + extraRings);
        System.out.println("额外腰带: " + extraBelts);
        System.out.println("额外头部: " + extraHeads);
        System.out.println("额外身体: " + extraBodies);
        System.out.println("额外挂饰: " + extraCharms);
        System.out.println("额外万能: " + extraTrinkets);
        System.out.println("--------------------------------------");
        System.out.println("原版槽位: 7");
        System.out.println("额外槽位: " + getTotalExtraSlots());
        System.out.println("总计槽位: " + getTotalSlots());
        System.out.println("配置状态: " + (validateConfig() ? "✓ 合法" : "✗ 超出限制"));
        System.out.println("======================================");
    }

    @Mod.EventBusSubscriber(modid = "moremod")
    public static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("moremod")) {
                ConfigManager.sync("moremod", Config.Type.INSTANCE);
                validateConfig();
                printConfig();
            }
        }
    }
}