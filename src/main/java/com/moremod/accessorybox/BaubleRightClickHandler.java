package com.moremod.accessorybox;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.accessorybox.unlock.SlotUnlockManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 右键佩戴接管器：
 * - 不改 BaublesContainer / BaublesApi
 * - 只在事件层改写“右键佩戴”的行为
 * - 所有 IBauble 右键都走这里 → 强制经过解锁 & 类型检查
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class BaubleRightClickHandler {

    /**
     * 右键空中 / 物品事件（不点方块时）
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        handleRightClick(event);
    }

    /**
     * 右键点方块时（Forge 文档：RightClickItem 不会在点方块时触发）
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        handleRightClick(event);
    }

    /**
     * 通用处理逻辑
     */
    private static void handleRightClick(PlayerInteractEvent event) {
        // 已被别的模组取消就别动
        if (event.isCanceled()) return;

        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = event.getItemStack();

        // 只管 IBauble
        if (stack.isEmpty() || !(stack.getItem() instanceof IBauble)) {
            return;
        }

        IBauble bauble = (IBauble) stack.getItem();
        BaubleType itemType = bauble.getBaubleType(stack);

        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        SlotUnlockManager unlockMgr = SlotUnlockManager.getInstance();
        int totalSlots = handler.getSlots();

        // ================================
        // Pass 1：优先找“同类型槽位”
        // 从高 ID 往低扫 = 最近的槽位优先
        // ================================
        int targetSlot = findSlotForBauble(
                player, stack, bauble, itemType,
                handler, unlockMgr,
                totalSlots,
                /*preferSameType*/ true
        );

        // ================================
        // Pass 2：如果没找到，再允许 TRINKET 弹性（万能槽位）
        // ================================
        if (targetSlot == -1) {
            targetSlot = findSlotForBauble(
                    player, stack, bauble, itemType,
                    handler, unlockMgr,
                    totalSlots,
                    /*preferSameType*/ false
            );
        }

        if (targetSlot != -1) {
            // 真正执行装备
            ItemStack toEquip = stack.copy();
            handler.setStackInSlot(targetSlot, toEquip);

            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }

            bauble.onEquipped(toEquip, player);

            // 完全接管：取消事件 → 不让原模组再跑 onItemRightClick
            event.setCanceled(true);
            return;
        }

        // ========== 没有合适槽位 ==========
        // 判断是不是“有空槽但被锁住”这种情况，用来发提示
        boolean hasLockedSlotForThisType = false;
        for (int physicalSlot = 0; physicalSlot < totalSlots; physicalSlot++) {
            int logicalSlot = SlotLayoutHelper.toLogicalSlot(physicalSlot);

            // 已解锁的不算
            if (unlockMgr.isSlotUnlocked(player, logicalSlot)) continue;

            BaubleType slotType = SlotLayoutHelper.getExpectedTypeForSlot(logicalSlot);
            if (slotAcceptsType(slotType, itemType)) {
                hasLockedSlotForThisType = true;
                break;
            }
        }

        if (player.world.isRemote && hasLockedSlotForThisType) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "[饰品盒] " +
                            TextFormatting.GRAY + "该类型的额外饰品槽尚未解锁！"
            ));
        }

        // 重要：即使没成功装备，也要取消事件，
        // 防止原 BaubleBase.onItemRightClick 把东西塞进“锁上的格子”
        event.setCanceled(true);
    }

    /**
     * 按策略查找可用槽位：
     * - preferSameType = true → 只找“槽位类型 == 物品类型”的
     * - preferSameType = false → 放宽到 TRINKET 弹性
     */
    private static int findSlotForBauble(EntityPlayer player,
                                         ItemStack stack,
                                         IBauble bauble,
                                         BaubleType itemType,
                                         IBaublesItemHandler handler,
                                         SlotUnlockManager unlockMgr,
                                         int totalSlots,
                                         boolean preferSameType) {

        // 从高到低扫 → “最近解锁 / 最新槽位”优先
        for (int physicalSlot = totalSlots - 1; physicalSlot >= 0; physicalSlot--) {

            int logicalSlot = SlotLayoutHelper.toLogicalSlot(physicalSlot);

            // ① 解锁检查（统一交给 SlotUnlockManager）
            if (!unlockMgr.isSlotUnlocked(player, logicalSlot)) {
                continue;
            }

            // ② 槽位必须是空的
            if (!handler.getStackInSlot(physicalSlot).isEmpty()) {
                continue;
            }

            // ③ 类型匹配：先看槽位类型
            BaubleType slotType = SlotLayoutHelper.getExpectedTypeForSlot(logicalSlot);

            if (preferSameType) {
                // 第一轮：只接受“槽位类型 == 物品类型”的情况
                if (slotType != itemType) {
                    continue;
                }
            } else {
                // 第二轮：允许 TRINKET 弹性
                if (!slotAcceptsType(slotType, itemType)) {
                    continue;
                }
            }

            // ④ 交给 Baubles 自己的 isItemValidForSlot 再做一次兜底
            if (!handler.isItemValidForSlot(physicalSlot, stack, player)) {
                continue;
            }

            // ⑤ IBauble.canEquip
            if (!bauble.canEquip(stack, player)) {
                continue;
            }

            return physicalSlot;
        }

        return -1;
    }

    /**
     * 判断某个槽位类型是否接受当前饰品类型（考虑 TRINKET 万能逻辑）
     */
    private static boolean slotAcceptsType(BaubleType slotType, BaubleType itemType) {
        // TRINKET 槽位可以接受任何类型
        if (slotType == BaubleType.TRINKET) return true;

        // TRINKET 饰品可以佩戴在任何槽位
        if (itemType == BaubleType.TRINKET) return true;

        // 其他情况必须匹配
        return slotType == itemType;
    }
}
