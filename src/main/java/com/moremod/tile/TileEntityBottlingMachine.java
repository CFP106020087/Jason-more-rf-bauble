package com.moremod.tile;

import com.moremod.recipe.BottlingMachineRecipe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class TileEntityBottlingMachine extends TileEntity implements ITickable, ISidedInventory {

    // 液体储罐 - 容量10000mB (10桶)
    private FluidTank fluidTank = new FluidTank(10000) {
        @Override
        protected void onContentsChanged() {
            TileEntityBottlingMachine.this.markDirty();
            if (!world.isRemote) {
                world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            }
        }
    };

    // 物品栏位处理器 - 3个槽位：输入、输出、流体容器
    private ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            TileEntityBottlingMachine.this.markDirty();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) { // 输入槽
                return true;
            } else if (slot == 2) { // 流体容器槽
                return stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
            }
            return false; // 输出槽只能提取
        }
    };

    // 处理进度
    private int processTime = 0;
    private int maxProcessTime = 40; // 2秒处理时间
    private BottlingMachineRecipe currentRecipe = null;

    // 库存
    private NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);

    @Override
    public void update() {
        if (!world.isRemote) {
            // 处理流体容器（如桶）
            handleFluidContainer();

            // 查找配方
            if (currentRecipe == null && canStartProcess()) {
                ItemStack input = itemHandler.getStackInSlot(0);
                FluidStack fluid = fluidTank.getFluid();
                currentRecipe = BottlingMachineRecipe.findRecipe(input, fluid);
                if (currentRecipe != null) {
                    processTime = 0;
                }
            }

            // 处理配方
            if (currentRecipe != null) {
                if (canProcess(currentRecipe)) {
                    processTime++;
                    if (processTime >= maxProcessTime) {
                        processRecipe(currentRecipe);
                        currentRecipe = null;
                        processTime = 0;
                    }
                } else {
                    currentRecipe = null;
                    processTime = 0;
                }
            }
        }
    }

    private void handleFluidContainer() {
        ItemStack container = itemHandler.getStackInSlot(2);
        if (!container.isEmpty() && container.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
            // 重要：这里使用 IFluidHandlerItem 而不是 IFluidHandler
            IFluidHandlerItem containerHandler = container.getCapability(
                    CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);

            if (containerHandler == null) return; // 空值检查

            // 尝试从容器中抽取流体
            FluidStack drained = containerHandler.drain(1000, false);
            if (drained != null && drained.amount > 0) {
                int filled = fluidTank.fill(drained, false);
                if (filled > 0) {
                    drained = containerHandler.drain(filled, true);
                    fluidTank.fill(drained, true);

                    // 更新容器 - getContainer() 方法在 IFluidHandlerItem 中
                    itemHandler.setStackInSlot(2, containerHandler.getContainer());
                }
            }
        }
    }

    private boolean canStartProcess() {
        ItemStack input = itemHandler.getStackInSlot(0);
        return !input.isEmpty() && fluidTank.getFluidAmount() > 0;
    }

    private boolean canProcess(BottlingMachineRecipe recipe) {
        if (recipe == null) return false;

        ItemStack input = itemHandler.getStackInSlot(0);
        ItemStack output = itemHandler.getStackInSlot(1);

        // 检查输入
        if (input.isEmpty() || input.getCount() < recipe.input.amount) {
            return false;
        }

        // 检查流体
        if (!fluidTank.getFluid().containsFluid(recipe.fluidInput)) {
            return false;
        }

        // 检查输出槽
        if (output.isEmpty()) {
            return true;
        } else if (output.isItemEqual(recipe.output)) {
            int resultCount = output.getCount() + recipe.output.getCount();
            return resultCount <= output.getMaxStackSize();
        }

        return false;
    }

    private void processRecipe(BottlingMachineRecipe recipe) {
        ItemStack input = itemHandler.extractItem(0, recipe.input.amount, false);
        fluidTank.drain(recipe.fluidInput.amount, true);

        ItemStack output = itemHandler.getStackInSlot(1);
        if (output.isEmpty()) {
            itemHandler.setStackInSlot(1, recipe.output.copy());
        } else {
            output.grow(recipe.output.getCount());
        }
    }

    // 流体处理能力
    private IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            return fluidTank.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return fluidTank.fill(resource, doFill);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return fluidTank.drain(resource, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return fluidTank.drain(maxDrain, doDrain);
        }
    };

    // NBT保存和加载
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        itemHandler.deserializeNBT(compound.getCompoundTag("inventory"));
        fluidTank.readFromNBT(compound);
        processTime = compound.getInteger("processTime");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("inventory", itemHandler.serializeNBT());
        fluidTank.writeToNBT(compound);
        compound.setInteger("processTime", processTime);
        return compound;
    }

    // 网络同步
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    // Capability支持
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandler);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluidHandler);
        }
        return super.getCapability(capability, facing);
    }

    // ISidedInventory实现
    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        if (side == EnumFacing.UP) {
            return new int[]{0}; // 顶部：输入
        } else if (side == EnumFacing.DOWN) {
            return new int[]{1}; // 底部：输出
        } else {
            return new int[]{2}; // 侧面：流体容器
        }
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return itemHandler.isItemValid(index, itemStackIn);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return index == 1; // 只能从输出槽提取
    }

    @Override
    public int getSizeInventory() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < 3; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return itemHandler.getStackInSlot(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return itemHandler.extractItem(index, count, false);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = itemHandler.getStackInSlot(index);
        itemHandler.setStackInSlot(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        itemHandler.setStackInSlot(index, stack);
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return world.getTileEntity(pos) == this &&
                player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return itemHandler.isItemValid(index, stack);
    }

    @Override
    public int getField(int id) {
        switch (id) {
            case 0: return processTime;
            case 1: return maxProcessTime;
            case 2: return fluidTank.getFluidAmount();
            case 3: return fluidTank.getCapacity();
            default: return 0;
        }
    }

    @Override
    public void setField(int id, int value) {
        switch (id) {
            case 0: processTime = value; break;
            case 1: maxProcessTime = value; break;
        }
    }

    @Override
    public int getFieldCount() {
        return 4;
    }

    @Override
    public void clear() {
        for (int i = 0; i < 3; i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public String getName() {
        return "container.bottling_machine";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(getName());
    }

    // Getter方法供GUI使用
    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public int getProcessTime() {
        return processTime;
    }

    public int getMaxProcessTime() {
        return maxProcessTime;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }
}