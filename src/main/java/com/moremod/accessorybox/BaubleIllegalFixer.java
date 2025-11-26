package com.moremod.accessorybox;

import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.BaubleType;
import baubles.api.cap.IBaublesItemHandler;

import com.moremod.accessorybox.unlock.SlotUnlockManager;
import com.moremod.accessorybox.SlotLayoutHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Set;

@Mod.EventBusSubscriber(modid = "moremod")
public class BaubleIllegalFixer {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 1s 检查一次（节省性能）
        if (player.ticksExisted % 20 != 0) return;

        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        SlotUnlockManager unlockMgr = SlotUnlockManager.getInstance();

        // 获取玩家所有可用槽位
        Set<Integer> availableSlots = unlockMgr.getAvailableSlots(player.getUniqueID());

        int totalSlots = handler.getSlots();

        for (int physicalSlot = 0; physicalSlot < totalSlots; physicalSlot++) {

            ItemStack stack = handler.getStackInSlot(physicalSlot);
            if (stack.isEmpty()) continue;

            if (!(stack.getItem() instanceof IBauble)) continue;

            int logicalSlot = SlotLayoutHelper.toLogicalSlot(physicalSlot);

            IBauble bauble = (IBauble) stack.getItem();
            BaubleType itemType = bauble.getBaubleType(stack);
            BaubleType slotType = SlotLayoutHelper.getExpectedTypeForSlot(logicalSlot);

            // ① 槽位必须已解锁
            if (!availableSlots.contains(logicalSlot)) {
                forceUnequip(player, handler, physicalSlot, stack);
                continue;
            }

            // ② 类型匹配检查
            if (!acceptsType(slotType, itemType)) {
                forceUnequip(player, handler, physicalSlot, stack);
            }
        }
    }

    private static boolean acceptsType(BaubleType slotType, BaubleType itemType) {
        if (slotType == BaubleType.TRINKET) return true;
        if (itemType == BaubleType.TRINKET) return true;
        return slotType == itemType;
    }

    private static void forceUnequip(EntityPlayer player,
                                     IBaublesItemHandler handler,
                                     int physicalSlot,
                                     ItemStack stack) {

        ItemStack copy = stack.copy();

        if (!player.inventory.addItemStackToInventory(copy)) {
            player.dropItem(copy, false);
        }

        handler.setStackInSlot(physicalSlot, ItemStack.EMPTY);

        if (stack.getItem() instanceof IBauble) {
            ((IBauble) stack.getItem()).onUnequipped(stack, player);
        }
    }
}
