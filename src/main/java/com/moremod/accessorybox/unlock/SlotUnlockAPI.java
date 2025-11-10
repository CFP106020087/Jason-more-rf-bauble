package com.moremod.accessorybox.unlock;

import net.minecraft.entity.player.EntityPlayer;

/**
 * 槽位解锁 API
 * 提供简单的调用接口供其他代码使用
 */
public class SlotUnlockAPI {

    /**
     * 解锁指定玩家的单个槽位
     * 
     * @param player 玩家
     * @param slotId 槽位ID (7+)
     * @return true=成功解锁, false=已经解锁或无效
     * 
     * 示例:
     * SlotUnlockAPI.unlockSlot(player, 7);  // 解锁第1个额外项链
     * SlotUnlockAPI.unlockSlot(player, 10); // 解锁第2个额外戒指
     */
    public static boolean unlockSlot(EntityPlayer player, int slotId) {
        return SlotUnlockManager.getInstance().unlockSlot(player, slotId);
    }

    /**
     * 批量解锁槽位
     * 
     * @param player 玩家
     * @param slotIds 槽位ID数组
     * @return 成功解锁的数量
     * 
     * 示例:
     * SlotUnlockAPI.unlockSlots(player, 7, 8, 9); // 解锁3个槽位
     */
    public static int unlockSlots(EntityPlayer player, int... slotIds) {
        return SlotUnlockManager.getInstance().unlockSlots(player, slotIds);
    }

    /**
     * 解锁指定类型的所有槽位
     * 
     * @param player 玩家
     * @param type 类型名称: AMULET, RING, BELT, HEAD, BODY, CHARM, TRINKET
     * @return 成功解锁的数量
     * 
     * 示例:
     * SlotUnlockAPI.unlockAllSlotsOfType(player, "RING");    // 解锁所有戒指槽位
     * SlotUnlockAPI.unlockAllSlotsOfType(player, "TRINKET"); // 解锁所有万能槽位
     */
    public static int unlockAllSlotsOfType(EntityPlayer player, String type) {
        return SlotUnlockManager.getInstance().unlockAllSlotsOfType(player, type);
    }

    /**
     * 检查槽位是否已解锁
     * 
     * @param player 玩家
     * @param slotId 槽位ID
     * @return true=已解锁可用, false=锁定
     * 
     * 示例:
     * if (SlotUnlockAPI.isSlotUnlocked(player, 7)) {
     *     // 槽位7已解锁
     * }
     */
    public static boolean isSlotUnlocked(EntityPlayer player, int slotId) {
        return SlotUnlockManager.getInstance().isSlotUnlocked(player, slotId);
    }

    /**
     * 重新锁定槽位（慎用！）
     * 
     * @param player 玩家
     * @param slotId 槽位ID
     * @return true=成功锁定, false=失败
     * 
     * 注意: 锁定后该槽位的物品会无法访问！
     * 建议先检查槽位是否为空再锁定
     */
    public static boolean lockSlot(EntityPlayer player, int slotId) {
        return SlotUnlockManager.getInstance().lockSlot(player, slotId);
    }

    /**
     * 重置玩家所有解锁状态
     * 
     * @param player 玩家
     * 
     * 注意: 这会将所有解锁状态恢复为配置中的默认值！
     */
    public static void resetPlayer(EntityPlayer player) {
        SlotUnlockManager.getInstance().resetPlayer(player);
    }

    // ==================== 高级 API ====================

    /**
     * 解锁额外项链槽位
     * 
     * @param player 玩家
     * @param index 额外项链索引 (0, 1, 2...)
     * @return true=成功解锁
     */
    public static boolean unlockExtraAmulet(EntityPlayer player, int index) {
        int slotId = getExtraSlotId("AMULET", index);
        return slotId >= 0 && unlockSlot(player, slotId);
    }

    /**
     * 解锁额外戒指槽位
     * 
     * @param player 玩家
     * @param index 额外戒指索引 (0, 1, 2...)
     * @return true=成功解锁
     */
    public static boolean unlockExtraRing(EntityPlayer player, int index) {
        int slotId = getExtraSlotId("RING", index);
        return slotId >= 0 && unlockSlot(player, slotId);
    }

    /**
     * 解锁额外腰带槽位
     */
    public static boolean unlockExtraBelt(EntityPlayer player, int index) {
        int slotId = getExtraSlotId("BELT", index);
        return slotId >= 0 && unlockSlot(player, slotId);
    }

    /**
     * 解锁额外头部槽位
     */
    public static boolean unlockExtraHead(EntityPlayer player, int index) {
        int slotId = getExtraSlotId("HEAD", index);
        return slotId >= 0 && unlockSlot(player, slotId);
    }

    /**
     * 解锁额外身体槽位
     */
    public static boolean unlockExtraBody(EntityPlayer player, int index) {
        int slotId = getExtraSlotId("BODY", index);
        return slotId >= 0 && unlockSlot(player, slotId);
    }

    /**
     * 解锁额外挂饰槽位
     */
    public static boolean unlockExtraCharm(EntityPlayer player, int index) {
        int slotId = getExtraSlotId("CHARM", index);
        return slotId >= 0 && unlockSlot(player, slotId);
    }

    /**
     * 解锁额外万能槽位
     */
    public static boolean unlockExtraTrinket(EntityPlayer player, int index) {
        int slotId = getExtraSlotId("TRINKET", index);
        return slotId >= 0 && unlockSlot(player, slotId);
    }

    // ==================== 辅助方法 ====================

    /**
     * 根据类型和索引获取槽位ID
     */
    private static int getExtraSlotId(String type, int extraIndex) {
        int[] slotIds = com.moremod.accessorybox.SlotLayoutHelper.getSlotIdsForType(type);
        
        // 获取原版槽位数量
        int vanillaCount = getVanillaCount(type);
        
        // 计算额外槽位的实际索引
        int actualIndex = vanillaCount + extraIndex;
        
        if (actualIndex >= 0 && actualIndex < slotIds.length) {
            return slotIds[actualIndex];
        }
        
        return -1; // 无效索引
    }

    /**
     * 获取原版槽位数量
     */
    private static int getVanillaCount(String type) {
        switch (type.toUpperCase()) {
            case "AMULET": return 1;
            case "RING": return 2;
            case "BELT": return 1;
            case "HEAD": return 1;
            case "BODY": return 1;
            case "CHARM": return 1;
            case "TRINKET": return 7;
            default: return 0;
        }
    }

    /**
     * 打印玩家解锁状态（调试用）
     */
    public static void debugPrint(EntityPlayer player) {
        SlotUnlockManager.getInstance().debugPrint(player.getUniqueID());
    }
}
