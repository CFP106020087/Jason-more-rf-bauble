package com.moremod.accessorybox;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.accessorybox.unlock.SlotBaubleTyped;
import com.moremod.accessorybox.unlock.SlotUnlockManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;

import java.util.Set;

/**
 * 配饰盒容器 - 适配新的动态解锁系统
 */
public class ContainerAccessoryBox extends Container {
    private final EntityPlayer player;
    private final IBaublesItemHandler baubles;
    private final int tier;
    private int baubleSlots = 0;

    public ContainerAccessoryBox(InventoryPlayer playerInv, EntityPlayer player, int tier) {
        this.player = player;
        this.baubles = BaublesApi.getBaublesHandler(player);
        this.tier = tier;

        // ⭐ 使用新系统添加饰品槽位
        if (baubles != null) {
            addBaubleSlots();
        }

        // 玩家背包
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInv,
                        col + row * 9 + 9,
                        9 + col * 18,
                        85 + row * 18));
            }
        }

        // 快捷栏
        for (int col = 0; col < 9; ++col) {
            this.addSlotToContainer(new Slot(playerInv, col,
                    9 + col * 18,
                    143));
        }
    }

    /**
     * 添加饰品槽位 - 使用新的解锁系统
     */
    private void addBaubleSlots() {
        SlotLayoutHelper.SlotAllocation alloc = SlotLayoutHelper.calculateSlotAllocation();
        Set<Integer> availableSlots = SlotUnlockManager.getInstance().getAvailableSlots(player.getUniqueID());

        int startX = 9;
        int startY = 19;

        // 按顺序添加已解锁的额外槽位
        int displayIndex = 0;

        // 添加 AMULET 额外槽位
        displayIndex = addSlotsForType(alloc.amuletSlots, 1, BaubleType.AMULET, availableSlots, startX, startY, displayIndex);

        // 添加 RING 额外槽位
        displayIndex = addSlotsForType(alloc.ringSlots, 2, BaubleType.RING, availableSlots, startX, startY, displayIndex);

        // 添加 BELT 额外槽位
        displayIndex = addSlotsForType(alloc.beltSlots, 1, BaubleType.BELT, availableSlots, startX, startY, displayIndex);

        // 添加 HEAD 额外槽位
        displayIndex = addSlotsForType(alloc.headSlots, 1, BaubleType.HEAD, availableSlots, startX, startY, displayIndex);

        // 添加 BODY 额外槽位
        displayIndex = addSlotsForType(alloc.bodySlots, 1, BaubleType.BODY, availableSlots, startX, startY, displayIndex);

        // 添加 CHARM 额外槽位
        displayIndex = addSlotsForType(alloc.charmSlots, 1, BaubleType.CHARM, availableSlots, startX, startY, displayIndex);

        // 添加 TRINKET 额外槽位
        displayIndex = addSlotsForType(alloc.trinketSlots, 7, BaubleType.TRINKET, availableSlots, startX, startY, displayIndex);

        System.out.println("[ContainerAccessoryBox] Added " + baubleSlots + " bauble slots for player " + player.getName());
    }

    /**
     * 为指定类型添加槽位
     */
    private int addSlotsForType(int[] allSlots, int vanillaCount, BaubleType type,
                                Set<Integer> availableSlots, int startX, int startY, int currentIndex) {
        if (allSlots == null || allSlots.length <= vanillaCount) {
            return currentIndex;
        }

        // 只添加额外槽位（跳过原版槽位）
        for (int i = vanillaCount; i < allSlots.length; i++) {
            int slotId = allSlots[i];

            // 只添加已解锁的槽位
            if (availableSlots.contains(slotId)) {
                int x = startX + currentIndex * 18;
                int y = startY;

                this.addSlotToContainer(new SlotBaubleTyped(
                        player,
                        baubles,
                        slotId,
                        x,
                        y,
                        type
                ));

                baubleSlots++;
                currentIndex++;

                System.out.println("[ContainerAccessoryBox] Added slot " + slotId +
                        " (type: " + type + ") at position " + currentIndex);
            }
        }

        return currentIndex;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return hasAccessoryBox(playerIn, tier);
    }

    private boolean hasAccessoryBox(EntityPlayer player, int requiredTier) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemAccessoryBox) {
                ItemAccessoryBox box = (ItemAccessoryBox) stack.getItem();
                if (box.getTier() >= requiredTier) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        if (!playerIn.world.isRemote) {
            playerIn.world.playSound(null, playerIn.posX, playerIn.posY, playerIn.posZ,
                    SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.PLAYERS,
                    0.5F, playerIn.world.rand.nextFloat() * 0.1F + 0.9F);
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            returnStack = slotStack.copy();

            final int BAUBLE_END = baubleSlots;
            final int INV_START = baubleSlots;
            final int INV_END = baubleSlots + 27;
            final int HOTBAR_START = INV_END;
            final int HOTBAR_END = HOTBAR_START + 9;

            // 从饰品槽移到背包
            if (index < BAUBLE_END) {
                if (!this.mergeItemStack(slotStack, INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 从背包或快捷栏移动
            else {
                // 如果是饰品，尝试放入对应槽位
                if (slotStack.getItem() instanceof IBauble) {
                    for (int i = 0; i < baubleSlots; i++) {
                        Slot baubleSlot = this.inventorySlots.get(i);
                        if (baubleSlot.isItemValid(slotStack) && !baubleSlot.getHasStack()) {
                            baubleSlot.putStack(slotStack.splitStack(1));
                            slot.onSlotChanged();

                            if (slotStack.isEmpty()) {
                                slot.putStack(ItemStack.EMPTY);
                            }
                            return returnStack;
                        }
                    }
                }

                // 在背包和快捷栏之间移动
                if (index >= INV_START && index < INV_END) {
                    if (!this.mergeItemStack(slotStack, HOTBAR_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= HOTBAR_START) {
                    if (!this.mergeItemStack(slotStack, INV_START, INV_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (slotStack.getCount() == returnStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, slotStack);
        }

        return returnStack;
    }
}