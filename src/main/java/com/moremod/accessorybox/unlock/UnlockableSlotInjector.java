package com.moremod.accessorybox.unlock;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import baubles.common.container.SlotBauble;
import com.moremod.accessorybox.DynamicGuiLayout;
import com.moremod.accessorybox.Point;
import com.moremod.accessorybox.SlotLayoutHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import java.util.Set;

/**
 * 支持解锁系统的动态槽位注入器（强制类型版）
 * 只注入已解锁的额外槽位到容器中，并按类型强制校验（解决“戒指戴不上去”）
 */
public class UnlockableSlotInjector {

    /**
     * 注入已解锁的额外槽位（由 ASM 调用）
     */
    public static void injectUnlockedSlots(Container container, InventoryPlayer playerInv, IBaublesItemHandler baubles) {
        EntityPlayer player = playerInv.player;

        // 若未启用解锁系统：注入全部额外槽位
        if (!UnlockableSlotsConfig.enableUnlockSystem) {
            injectAllExtraSlots(container, player);
            return;
        }

        // 可用槽位（全局 slotId 集合）
        Set<Integer> availableSlots = SlotUnlockManager.getInstance().getAvailableSlots(player.getUniqueID());

        System.out.println("[UnlockableSlots] ========== 开始注入已解锁槽位 ==========");
        System.out.println("[UnlockableSlots] 可用槽位: " + availableSlots);

        // 各类型槽位布局（数组元素是 handler 的全局 slotId）
        SlotLayoutHelper.SlotAllocation alloc = SlotLayoutHelper.calculateSlotAllocation();

        int injectedCount = 0;
        injectedCount += injectUnlockedSlotsForType(container, player, alloc.amuletSlots,  1, availableSlots, BaubleType.AMULET);
        injectedCount += injectUnlockedSlotsForType(container, player, alloc.ringSlots,    2, availableSlots, BaubleType.RING);
        injectedCount += injectUnlockedSlotsForType(container, player, alloc.beltSlots,    1, availableSlots, BaubleType.BELT);
        injectedCount += injectUnlockedSlotsForType(container, player, alloc.headSlots,    1, availableSlots, BaubleType.HEAD);
        injectedCount += injectUnlockedSlotsForType(container, player, alloc.bodySlots,    1, availableSlots, BaubleType.BODY);
        injectedCount += injectUnlockedSlotsForType(container, player, alloc.charmSlots,   1, availableSlots, BaubleType.CHARM);
        injectedCount += injectUnlockedSlotsForType(container, player, alloc.trinketSlots, 7, availableSlots, BaubleType.TRINKET);

        System.out.println("[UnlockableSlots] ========== 注入完成 ==========");
        System.out.println("[UnlockableSlots] 新增槽位数: " + injectedCount);
        System.out.println("[UnlockableSlots] 最终槽位总数: " + container.inventorySlots.size());
    }

    /**
     * 为指定类型注入“已解锁”的额外槽位
     * @param allSlots     该类型的所有（基础+额外）全局 slotId 数组
     * @param vanillaCount 该类型基础槽位数量（用于跳过）
     * @param available    可用全局 slotId 集合
     * @param expectedType 强制的 BaubleType（解决类型不匹配导致的“戴不上去”）
     */
    private static int injectUnlockedSlotsForType(Container container,
                                                  EntityPlayer player,
                                                  int[] allSlots,
                                                  int vanillaCount,
                                                  Set<Integer> available,
                                                  BaubleType expectedType) {
        if (allSlots == null || allSlots.length <= vanillaCount) return 0;

        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        int injected = 0;

        for (int i = vanillaCount; i < allSlots.length; i++) {
            int baubleSlotId = allSlots[i];

            // 只注入“当前玩家已解锁”的槽位
            if (!available.contains(baubleSlotId)) continue;

            // 防御：handler 的实际大小不足以容纳该索引时跳过（避免越界）
            if (baubleSlotId < 0 || baubleSlotId >= handler.getSlots()) {
                System.err.println("[UnlockableSlots] 警告：handler.getSlots()=" + handler.getSlots()
                        + "，无法容纳 slotId=" + baubleSlotId + "（跳过注入）");
                continue;
            }

            // GUI 坐标
            Point coord = DynamicGuiLayout.getSlotPosition(baubleSlotId);

            // ★ 使用“强制类型版槽位”：不看 handler 的类型表，直接用期望类型校验
            SlotBauble slot = new SlotBaubleTyped(
                    player,
                    handler,
                    baubleSlotId,
                    coord.getX(),
                    coord.getY(),
                    expectedType
            );

            // 设置 slotNumber 并加入容器
            slot.slotNumber = container.inventorySlots.size();
            container.inventorySlots.add(slot);
            container.inventoryItemStacks.add(ItemStack.EMPTY);

            injected++;
        }
        return injected;
    }

    /**
     * 解锁系统禁用时：注入所有额外槽位（也用强制类型版，确保类型正确）
     */
    private static void injectAllExtraSlots(Container container, EntityPlayer player) {
        System.out.println("[UnlockableSlots] 解锁系统禁用，注入所有额外槽位");

        SlotLayoutHelper.SlotAllocation alloc = SlotLayoutHelper.calculateSlotAllocation();
        int injectedCount = 0;

        injectedCount += injectAllSlotsForType(container, player, alloc.amuletSlots,  1, BaubleType.AMULET);
        injectedCount += injectAllSlotsForType(container, player, alloc.ringSlots,    2, BaubleType.RING);
        injectedCount += injectAllSlotsForType(container, player, alloc.beltSlots,    1, BaubleType.BELT);
        injectedCount += injectAllSlotsForType(container, player, alloc.headSlots,    1, BaubleType.HEAD);
        injectedCount += injectAllSlotsForType(container, player, alloc.bodySlots,    1, BaubleType.BODY);
        injectedCount += injectAllSlotsForType(container, player, alloc.charmSlots,   1, BaubleType.CHARM);
        injectedCount += injectAllSlotsForType(container, player, alloc.trinketSlots, 7, BaubleType.TRINKET);

        System.out.println("[UnlockableSlots] 注入完成: " + injectedCount + " 个槽位");
    }

    /**
     * 注入指定类型的所有额外槽位（禁用解锁系统时）
     */
    private static int injectAllSlotsForType(Container container,
                                             EntityPlayer player,
                                             int[] allSlots,
                                             int vanillaCount,
                                             BaubleType expectedType) {
        if (allSlots == null || allSlots.length <= vanillaCount) return 0;

        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        int injected = 0;

        for (int i = vanillaCount; i < allSlots.length; i++) {
            int baubleSlotId = allSlots[i];

            if (baubleSlotId < 0 || baubleSlotId >= handler.getSlots()) {
                System.err.println("[UnlockableSlots] 警告：handler.getSlots()=" + handler.getSlots()
                        + "，无法容纳 slotId=" + baubleSlotId + "（跳过注入）");
                continue;
            }

            Point coord = DynamicGuiLayout.getSlotPosition(baubleSlotId);

            SlotBauble slot = new SlotBaubleTyped(
                    player,
                    handler,
                    baubleSlotId,
                    coord.getX(),
                    coord.getY(),
                    expectedType
            );

            slot.slotNumber = container.inventorySlots.size();
            container.inventorySlots.add(slot);
            container.inventoryItemStacks.add(ItemStack.EMPTY);
            injected++;
        }
        return injected;
    }
}
