package com.moremod.accessorybox.unlock;

import com.moremod.accessorybox.SlotLayoutHelper;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
/**
 * 可解锁槽位配置
 * 灵活配置每个额外槽位是否需要解锁
 */
@Config(modid = "moremod", name = "moremod/unlockable_slots")
public class UnlockableSlotsConfig {

    @Config.Comment({
            "=== 可解锁槽位配置 ===",
            "配置额外槽位中哪些需要解锁",
            "只影响额外槽位，原版7个槽位始终可用"
    })
    @Config.Name("--- 配置说明 ---")
    public static String INFO = "true = 锁定(需要解锁), false = 默认可用";

    @Config.Comment({
            "启用槽位解锁系统",
            "false = 禁用，所有额外槽位直接可用"
    })
    @Config.Name("启用解锁系统 | Enable Unlock System")
    public static boolean enableUnlockSystem = true;

    // ==================== 项链槽位 ====================
    
    @Config.Comment({
            "额外项链槽位锁定状态",
            "数组长度 = 额外项链数量",
            "例如: [false, true] = 第1个额外项链可用，第2个锁定"
    })
    @Config.Name("额外项链锁定 | Extra Amulet Locks")
    public static boolean[] extraAmuletLocks = {false, true};

    // ==================== 戒指槽位 ====================
    
    @Config.Comment({
            "额外戒指槽位锁定状态",
            "例如: [false, true, false] = 第1个可用，第2个锁定，第3个可用"
    })
    @Config.Name("额外戒指锁定 | Extra Ring Locks")
    public static boolean[] extraRingLocks = {false, true, false};

    // ==================== 腰带槽位 ====================
    
    @Config.Comment({
            "额外腰带槽位锁定状态"
    })
    @Config.Name("额外腰带锁定 | Extra Belt Locks")
    public static boolean[] extraBeltLocks = {true};

    // ==================== 头部槽位 ====================
    
    @Config.Comment({
            "额外头部槽位锁定状态"
    })
    @Config.Name("额外头部锁定 | Extra Head Locks")
    public static boolean[] extraHeadLocks = {true};

    // ==================== 身体槽位 ====================
    
    @Config.Comment({
            "额外身体槽位锁定状态"
    })
    @Config.Name("额外身体锁定 | Extra Body Locks")
    public static boolean[] extraBodyLocks = {true};

    // ==================== 挂饰槽位 ====================
    
    @Config.Comment({
            "额外挂饰槽位锁定状态"
    })
    @Config.Name("额外挂饰锁定 | Extra Charm Locks")
    public static boolean[] extraCharmLocks = {false, true};

    // ==================== 万能槽位 ====================
    
    @Config.Comment({
            "额外万能槽位锁定状态"
    })
    @Config.Name("额外万能锁定 | Extra Trinket Locks")
    public static boolean[] extraTrinketLocks = {false, true, true, false};

    // ==================== 工具方法 ====================

    /**
     * 检查指定槽位ID是否默认锁定
     * @param slotId Bauble槽位ID (7+)
     * @return true=锁定, false=默认可用
     */
    public static boolean isSlotDefaultLocked(int slotId) {
        if (!enableUnlockSystem) {
            System.out.println("[UnlockableSlots] 槽位 " + slotId + " - 系统禁用，返回false");
            return false; // 系统禁用，所有槽位可用
        }

        if (slotId < 7) {
            return false; // 原版槽位永远可用
        }

        // 根据槽位ID判断类型和索引
        SlotInfo info = getSlotInfo(slotId);
        if (info == null) {
            System.out.println("[UnlockableSlots] ⚠️ 槽位 " + slotId + " - getSlotInfo()返回null，默认可用");
            return false; // 未知槽位，默认可用
        }

        // 获取对应配置数组
        boolean[] locks = getLockArrayForType(info.type);
        if (locks == null) {
            System.out.println("[UnlockableSlots] ⚠️ 槽位 " + slotId + " (" + info.type + ":" + info.extraIndex + ") - getLockArrayForType()返回null");
            return false;
        }

        if (info.extraIndex >= locks.length) {
            System.out.println("[UnlockableSlots] ⚠️ 槽位 " + slotId + " (" + info.type + ":" + info.extraIndex + ") - 索引越界，数组长度=" + locks.length);
            return false; // 配置不足，默认可用
        }

        boolean result = locks[info.extraIndex];
        System.out.println("[UnlockableSlots] 槽位 " + slotId + " (" + info.type + ":" + info.extraIndex + ") - locks[" + info.extraIndex + "]=" + result);
        return result;
    }

    /**
     * 获取槽位类型和额外索引
     */
    private static SlotInfo getSlotInfo(int slotId) {
        // 需要从 SlotLayoutHelper 获取实际分配
        // 这里先提供接口，实现在后面
        return SlotLayoutHelper.getSlotInfo(slotId);
    }

    /**
     * 根据类型获取锁定配置数组
     */
    private static boolean[] getLockArrayForType(String type) {
        switch (type) {
            case "AMULET": return extraAmuletLocks;
            case "RING": return extraRingLocks;
            case "BELT": return extraBeltLocks;
            case "HEAD": return extraHeadLocks;
            case "BODY": return extraBodyLocks;
            case "CHARM": return extraCharmLocks;
            case "TRINKET": return extraTrinketLocks;
            default: return null;
        }
    }

    /**
     * 打印配置信息
     */
    public static void printConfig() {
        System.out.println("========== 可解锁槽位配置 ==========");
        System.out.println("系统状态: " + (enableUnlockSystem ? "启用" : "禁用"));

        if (enableUnlockSystem) {
            System.out.println("\n锁定配置:");
            printLocks("项链", extraAmuletLocks);
            printLocks("戒指", extraRingLocks);
            printLocks("腰带", extraBeltLocks);
            printLocks("头部", extraHeadLocks);
            printLocks("身体", extraBodyLocks);
            printLocks("挂饰", extraCharmLocks);
            printLocks("万能", extraTrinketLocks);

            // 额外调试：测试具体槽位
            System.out.println("\n槽位映射测试:");
            testSlotMapping(7, "AMULET:0");
            testSlotMapping(8, "AMULET:1");
            testSlotMapping(9, "RING:0");
            testSlotMapping(10, "RING:1");
        }

        System.out.println("======================================");
    }

    private static void testSlotMapping(int slotId, String expectedName) {
        SlotInfo info = getSlotInfo(slotId);
        if (info == null) {
            System.out.println("  槽位 " + slotId + " (" + expectedName + ") → getSlotInfo()返回null ⚠️");
        } else {
            boolean locked = isSlotDefaultLocked(slotId);
            System.out.println("  槽位 " + slotId + " (" + expectedName + ") → " +
                info.type + ":" + info.extraIndex + " → " + (locked ? "🔒锁定" : "🔓解锁"));
        }
    }

    /**
     * 打印单个类型的锁定状态
     */
    private static void printLocks(String name, boolean[] locks) {
        if (locks == null || locks.length == 0) {
            System.out.println("  " + name + ": 无额外槽位");
            return;
        }

        StringBuilder sb = new StringBuilder("  " + name + ": [");
        for (int i = 0; i < locks.length; i++) {
            sb.append(locks[i] ? "🔒" : "🔓");
            if (i < locks.length - 1) sb.append(", ");
        }
        sb.append("]");
        
        // 统计
        int locked = 0;
        for (boolean lock : locks) {
            if (lock) locked++;
        }
        sb.append(" (").append(locked).append("/").append(locks.length).append(" 锁定)");
        
        System.out.println(sb.toString());
    }

    /**
     * 槽位信息类
     */
    public static class SlotInfo {
        public final String type;      // AMULET, RING, etc.
        public final int extraIndex;   // 在额外槽位中的索引
        public final int slotId;       // 实际槽位ID

        public SlotInfo(String type, int extraIndex, int slotId) {
            this.type = type;
            this.extraIndex = extraIndex;
            this.slotId = slotId;
        }
    }

    @Mod.EventBusSubscriber(modid = "moremod")
    public static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("moremod")) {
                ConfigManager.sync("moremod", Config.Type.INSTANCE);
                printConfig();
            }
        }
    }
}
