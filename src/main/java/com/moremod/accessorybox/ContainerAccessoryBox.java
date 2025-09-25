package com.moremod.accessorybox;

import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;

public class ContainerAccessoryBox extends Container {
    private final EntityPlayer player;
    private final IBaublesItemHandler baubles;
    private static final int BAUBLE_SLOTS = 7;

    public ContainerAccessoryBox(InventoryPlayer playerInv, EntityPlayer player) {
        this.player = player;
        this.baubles = BaublesApi.getBaublesHandler(player);

        // 添加飾品槽位 - 使用參考代碼的橫向排列
        if (baubles != null && baubles.getSlots() >= 22) {
            baubles.setPlayer(player);
            baubles.setEventBlock(true);

            // 橫向排列7個槽位
            this.addSlotToContainer(new SlotMirrorBauble(baubles, player, 14, 8 + 0 * 18, 19));  // Amulet
            this.addSlotToContainer(new SlotMirrorBauble(baubles, player, 15, 8 + 1 * 18, 19));  // Ring 1
            this.addSlotToContainer(new SlotMirrorBauble(baubles, player, 16, 8 + 2 * 18, 19));  // Ring 2
            this.addSlotToContainer(new SlotMirrorBauble(baubles, player, 17, 8 + 3 * 18, 19));  // Belt
            this.addSlotToContainer(new SlotMirrorBauble(baubles, player, 18, 8 + 4 * 18, 19));  // Head
            this.addSlotToContainer(new SlotMirrorBauble(baubles, player, 19, 8 + 5 * 18, 19));  // Body
            this.addSlotToContainer(new SlotMirrorBauble(baubles, player, 20, 8 + 6 * 18, 19));  // Charm
            this.addSlotToContainer(new SlotMirrorBauble(baubles, player, 21, 8 + 7 * 18, 19));  // 新槽位

            baubles.setEventBlock(false);

            System.out.println("[AccessoryBox] Added 7 bauble slots (14-20)");
        }

        // 玩家背包 - 使用參考位置
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInv,
                        col + row * 9 + 9,
                        8 + col * 18,
                        86 + row * 18));
            }
        }

        // 快捷欄 - 使用參考位置
        for (int col = 0; col < 9; ++col) {
            this.addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 144));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return hasAccessoryBox(playerIn);
    }

    private boolean hasAccessoryBox(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemAccessoryBox) {
                return true;
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

            // 從飾品槽移到背包
            if (index < BAUBLE_SLOTS) {
                if (!this.mergeItemStack(slotStack, BAUBLE_SLOTS,
                        BAUBLE_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 從背包或快捷欄移動
            else {
                // 如果是飾品，嘗試放入對應槽位
                if (slotStack.getItem() instanceof IBauble) {
                    boolean placed = false;

                    for (int i = 0; i < BAUBLE_SLOTS; i++) {
                        Slot baubleSlot = this.inventorySlots.get(i);
                        if (baubleSlot instanceof SlotMirrorBauble) {
                            SlotMirrorBauble mirrorSlot = (SlotMirrorBauble) baubleSlot;
                            if (mirrorSlot.isItemValid(slotStack) && !mirrorSlot.getHasStack()) {
                                ItemStack singleItem = slotStack.splitStack(1);
                                mirrorSlot.putStack(singleItem);
                                placed = true;
                                break;
                            }
                        }
                    }

                    if (placed) {
                        if (slotStack.isEmpty()) {
                            slot.putStack(ItemStack.EMPTY);
                        } else {
                            slot.onSlotChanged();
                        }
                        return returnStack;
                    }
                }

                // 在背包和快捷欄之間移動
                if (index >= BAUBLE_SLOTS && index < BAUBLE_SLOTS + 27) {
                    if (!this.mergeItemStack(slotStack, BAUBLE_SLOTS + 27,
                            BAUBLE_SLOTS + 36, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= BAUBLE_SLOTS + 27) {
                    if (!this.mergeItemStack(slotStack, BAUBLE_SLOTS,
                            BAUBLE_SLOTS + 27, false)) {
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