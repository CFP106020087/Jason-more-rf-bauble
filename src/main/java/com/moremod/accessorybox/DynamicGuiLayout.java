package com.moremod.accessorybox;

import com.moremod.accessorybox.client.ExtraSlotsToggle;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态 GUI 布局管理器
 * - 额外槽位排列在 inventory 正上方，左对齐
 * - 支持运行时切换显示/隐藏额外槽位
 */
public class DynamicGuiLayout {

    // ========== 配置常量 ==========

    /** 全局 X 轴偏移（默认 0） */
    public static int SHIFT_X = 0;

    /** 全局 Y 轴偏移（默认 0） */
    public static int SHIFT_Y = 0;

    // 原版槽位固定位置
    private static final int LEFT_COLUMN_X   = 8;   // 左侧列
    private static final int CENTER_COLUMN_X = 77;  // 中间列

    // 额外槽位区域配置
    private static final int EXTRA_START_X = 8;     // 对齐 inventory 左边缘
    private static final int EXTRA_START_Y = -20;   // inventory 上方起始位置
    private static final int SLOT_SIZE = 18;        // 标准槽位大小
    private static final int HORIZONTAL_SPACING = 0; // 槽位间距
    private static final int MAX_PER_ROW = 9;       // 每行最多 9 个槽位

    // 隐藏槽位时的坐标（屏幕外）
    private static final Point HIDDEN_POSITION = new Point(-9999, -9999);

    // 默认坐标（找不到槽位时的后备位置）
    private static final Point DEFAULT_POSITION = new Point(100, 8);

    // 坐标缓存
    private static Map<Integer, Point> basePositions = null;

    // ========== 公共方法 ==========

    /**
     * 获取槽位的屏幕坐标
     * @param slotId 逻辑槽位 ID
     * @return 槽位坐标（考虑 EX 开关状态）
     */
    public static Point getSlotPosition(int slotId) {
        // 懒加载:首次调用时计算所有坐标
        if (basePositions == null) {
            calculateAllPositions();
        }

        // EX 关闭时,额外槽位移到屏幕外(仅客户端检查)
        if (SlotLayoutHelper.isExtraSlot(slotId) && isClientSideHidden()) {
            return HIDDEN_POSITION;
        }

        // 获取基础坐标并应用全局偏移
        Point base = basePositions.getOrDefault(slotId, DEFAULT_POSITION);
        return new Point(base.getX() + SHIFT_X, base.getY() + SHIFT_Y);
    }

    /**
     * 检查客户端是否隐藏了额外槽位
     * 服务器端总是返回 false(不隐藏)
     */
    @SideOnly(Side.CLIENT)
    private static boolean isClientSideHiddenImpl() {
        return !ExtraSlotsToggle.isVisible();
    }

    /**
     * 安全的客户端隐藏状态检查
     */
    private static boolean isClientSideHidden() {
        try {
            // 仅在客户端调用
            if (net.minecraftforge.fml.common.FMLCommonHandler.instance().getSide().isClient()) {
                return isClientSideHiddenImpl();
            }
        } catch (Exception e) {
            // 服务器端或出错时不隐藏
        }
        return false;
    }

    /**
     * 清空坐标缓存
     * 当配置变更时需要调用此方法重新计算布局
     */
    public static void clearCache() {
        basePositions = null;
    }

    // ========== 内部布局计算 ==========

    /**
     * 计算所有槽位的基础坐标
     */
    private static void calculateAllPositions() {
        basePositions = new HashMap<>();

        // 1. 添加原版槽位坐标
        addVanillaSlotPositions();

        // 2. 添加额外槽位坐标
        addExtraSlotPositions();
    }

    /**
     * 添加原版 7 个槽位的固定坐标
     */
    private static void addVanillaSlotPositions() {
        basePositions.put(0, new Point(CENTER_COLUMN_X, 8));   // AMULET
        basePositions.put(1, new Point(CENTER_COLUMN_X, 26));  // RING 1
        basePositions.put(2, new Point(CENTER_COLUMN_X, 44));  // RING 2
        basePositions.put(3, new Point(CENTER_COLUMN_X, 62));  // BELT
        basePositions.put(4, new Point(LEFT_COLUMN_X, 8));     // HEAD
        basePositions.put(5, new Point(LEFT_COLUMN_X, 26));    // BODY
        basePositions.put(6, new Point(LEFT_COLUMN_X, 80));    // CHARM
    }

    /**
     * 添加额外槽位的动态坐标（横向排列）
     */
    private static void addExtraSlotPositions() {
        SlotLayoutHelper.SlotAllocation alloc = SlotLayoutHelper.calculateSlotAllocation();

        int currentIdx = 0;
        currentIdx = layoutExtraSlots(alloc.amuletSlots,  1, currentIdx);
        currentIdx = layoutExtraSlots(alloc.ringSlots,    2, currentIdx);
        currentIdx = layoutExtraSlots(alloc.beltSlots,    1, currentIdx);
        currentIdx = layoutExtraSlots(alloc.headSlots,    1, currentIdx);
        currentIdx = layoutExtraSlots(alloc.bodySlots,    1, currentIdx);
        currentIdx = layoutExtraSlots(alloc.charmSlots,   1, currentIdx);
        layoutExtraSlots(alloc.trinketSlots, 7, currentIdx);
    }

    /**
     * 布局一组额外槽位
     * @param allSlots 所有槽位数组（包含原版）
     * @param vanillaCount 原版槽位数量
     * @param startIndex 起始索引（用于连续排列）
     * @return 下一个可用索引
     */
    private static int layoutExtraSlots(int[] allSlots, int vanillaCount, int startIndex) {
        if (allSlots == null || allSlots.length <= vanillaCount) {
            return startIndex;
        }

        int extraCount = allSlots.length - vanillaCount;

        for (int i = 0; i < extraCount; i++) {
            int slotId = allSlots[vanillaCount + i];
            int idx = startIndex + i;

            // 计算网格位置（横向排列，向上堆叠）
            int row = idx / MAX_PER_ROW;
            int col = idx % MAX_PER_ROW;

            int x = EXTRA_START_X + col * (SLOT_SIZE + HORIZONTAL_SPACING);
            int y = EXTRA_START_Y - row * SLOT_SIZE; // 负数：向上

            basePositions.put(slotId, new Point(x, y));
        }

        return startIndex + extraCount;
    }
}