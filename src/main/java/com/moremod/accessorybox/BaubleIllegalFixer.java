package com.moremod.accessorybox;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import baubles.api.IBauble;
import baubles.api.BaubleType;

import com.moremod.accessorybox.SlotLayoutHelper;
import com.moremod.accessorybox.unlock.SlotUnlockManager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 定期检查并移除非法装备的饰品（服务器端安全兜底）
 *
 * 规则：
 * 1. 槽位未解锁 → 无条件卸下
 * 2. 槽位类型不匹配（考虑 TRINKET 规则）→ 卸下
 *
 * 注意：不触发 IBauble.onUnequipped 以避免双触发
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class BaubleIllegalFixer {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return; // 仅服务器

        EntityPlayer player = event.player;

        // 每 20 tick 执行一次（使用 per-player ticksExisted）
        if (player.ticksExisted % 20 != 0) return;

        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        SlotUnlockManager unlockMgr = SlotUnlockManager.getInstance();

        int totalSlots = handler.getSlots();

        for (int physicalSlot = 0; physicalSlot < totalSlots; physicalSlot++) {

            ItemStack stack = handler.getStackInSlot(physicalSlot);
            if (stack.isEmpty() || !(stack.getItem() instanceof IBauble)) continue;

            IBauble bauble = (IBauble) stack.getItem();
            BaubleType itemType = bauble.getBaubleType(stack);

            // 转换为逻辑槽位ID
            int logicalSlot = SlotLayoutHelper.toLogicalSlot(physicalSlot);
            BaubleType slotType = SlotLayoutHelper.getExpectedTypeForSlot(logicalSlot);

            // 规则 1：未解锁 → 无条件卸下
            if (!unlockMgr.isSlotUnlocked(player, logicalSlot)) {
                forceRemove(player, handler, physicalSlot, logicalSlot, stack, "槽位未解锁");
                continue;
            }

            // 规则 2：类型不匹配 → 卸下
            if (!slotAcceptsType(slotType, itemType)) {
                forceRemove(player, handler, physicalSlot, logicalSlot, stack,
                        "类型不匹配 (槽位=" + slotType + ", 物品=" + itemType + ")");
            }
        }
    }

    /** 槽位是否允许该类型饰品 */
    private static boolean slotAcceptsType(BaubleType slotType, BaubleType itemType) {
        if (slotType == BaubleType.TRINKET) return true;
        if (itemType == BaubleType.TRINKET) return true;
        return slotType == itemType;
    }

    /**
     * 强制卸下饰品
     * 注意：不调用 IBauble.onUnequipped，避免双触发
     */
    private static void forceRemove(EntityPlayer player,
                                    IBaublesItemHandler handler,
                                    int physicalSlot,
                                    int logicalSlot,
                                    ItemStack stack,
                                    String reason) {

        if (com.moremod.accessorybox.unlock.UnlockableSlotInjector.isDebugEnabled()) {
            System.out.println("[BaubleIllegalFixer] 移除非法饰品： slot=" + physicalSlot +
                    " logical=" + logicalSlot + " 原因=" + reason);
        }

        // 拷贝物品 → 放回背包 / 掉落
        ItemStack copy = stack.copy();

        // 清空槽位（不触发 onUnequipped）
        handler.setStackInSlot(physicalSlot, ItemStack.EMPTY);

        // 尝试放到背包
        if (!player.inventory.addItemStackToInventory(copy)) {
            // 背包满 → 掉落
            player.dropItem(copy, false);
        }
    }
}
