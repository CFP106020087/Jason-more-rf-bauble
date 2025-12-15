package com.moremod.container;

import com.moremod.tile.TileEntityOilGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 石油发电机Container
 */
public class ContainerOilGenerator extends Container {

    private final TileEntityOilGenerator te;

    // 同步字段
    private int lastEnergy = -1;
    private int lastFluidAmount = -1;
    private int lastBurnTime = -1;
    private int lastMaxBurnTime = -1;
    private int lastRFPerTick = -1;

    public ContainerOilGenerator(InventoryPlayer playerInv, TileEntityOilGenerator te) {
        this.te = te;

        IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (handler != null) {
            // 燃料槽 (0)
            addSlotToContainer(new SlotItemHandler(handler, 0, 80, 53));

            // 增速插件槽 (1-4)
            addSlotToContainer(new SlotItemHandler(handler, 1, 44, 17));
            addSlotToContainer(new SlotItemHandler(handler, 2, 62, 17));
            addSlotToContainer(new SlotItemHandler(handler, 3, 98, 17));
            addSlotToContainer(new SlotItemHandler(handler, 4, 116, 17));
        }

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 玩家快捷栏
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        for (IContainerListener listener : listeners) {
            if (lastEnergy != te.getEnergyStored()) {
                // 分两个字段发送（因为int最大值问题）
                listener.sendWindowProperty(this, 0, te.getEnergyStored() & 0xFFFF);
                listener.sendWindowProperty(this, 1, (te.getEnergyStored() >> 16) & 0xFFFF);
            }
            if (lastFluidAmount != te.getFluidAmount()) {
                listener.sendWindowProperty(this, 2, te.getFluidAmount());
            }
            if (lastBurnTime != te.getBurnTime()) {
                listener.sendWindowProperty(this, 3, te.getBurnTime());
            }
            if (lastMaxBurnTime != te.getMaxBurnTime()) {
                listener.sendWindowProperty(this, 4, te.getMaxBurnTime());
            }
            if (lastRFPerTick != te.getRFPerTick()) {
                listener.sendWindowProperty(this, 5, te.getRFPerTick());
            }
        }

        lastEnergy = te.getEnergyStored();
        lastFluidAmount = te.getFluidAmount();
        lastBurnTime = te.getBurnTime();
        lastMaxBurnTime = te.getMaxBurnTime();
        lastRFPerTick = te.getRFPerTick();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        switch (id) {
            case 0:
                // 低16位
                int current = te.getEnergyStored() & 0xFFFF0000;
                te.setClientEnergy(current | (data & 0xFFFF));
                break;
            case 1:
                // 高16位
                int low = te.getEnergyStored() & 0xFFFF;
                te.setClientEnergy(low | ((data & 0xFFFF) << 16));
                break;
            case 2:
                te.setClientFluidAmount(data);
                break;
            case 3:
                te.setClientBurnTime(data);
                break;
            case 4:
                te.setClientMaxBurnTime(data);
                break;
            case 5:
                te.setClientRFPerTick(data);
                break;
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player.getDistanceSq(te.getPos()) <= 64;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();

            // 从机器槽位移到玩家背包
            if (index < 5) {
                if (!mergeItemStack(stack, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 从玩家背包移到机器
            else {
                // 燃料
                if (TileEntityOilGenerator.isValidFuel(stack)) {
                    if (!mergeItemStack(stack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 增速插件
                else if (TileEntityOilGenerator.isValidUpgrade(stack)) {
                    if (!mergeItemStack(stack, 1, 5, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                else {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return result;
    }

    public TileEntityOilGenerator getTileEntity() {
        return te;
    }
}
