package com.moremod.tile;

import com.moremod.recipe.DimensionLoomRecipes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

public class TileEntityDimensionLoom extends TileEntity implements IInventory, ITickable {

    private NonNullList<ItemStack> inventory = NonNullList.<ItemStack>withSize(10, ItemStack.EMPTY);
    // 0-8: 输入格子 (3x3)
    // 9: 输出格子

    private int processTime = 0;
    private int maxProcessTime = 200; // 10秒 (200 ticks)
    private String customName;

    @Override
    public void update() {
        if (!this.world.isRemote) {
            boolean shouldMarkDirty = false;

            // 如果能处理，自动开始处理（不需要燃料）
            if (this.canProcess()) {
                ++this.processTime;

                // 处理完成
                if (this.processTime >= this.maxProcessTime) {
                    this.processTime = 0;
                    this.processItem();
                    shouldMarkDirty = true;
                }
            } else {
                // 重置进度
                if (this.processTime > 0) {
                    this.processTime = 0;
                    shouldMarkDirty = true;
                }
            }

            if (shouldMarkDirty) {
                this.markDirty();
            }
        }
    }

    private boolean canProcess() {
        // 检查是否有有效的配方
        ItemStack[] inputs = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            inputs[i] = this.inventory.get(i);
        }

        ItemStack result = DimensionLoomRecipes.getResultForTileEntity(inputs);
        if (result.isEmpty()) {
            return false;
        }

        ItemStack output = this.inventory.get(9);
        if (output.isEmpty()) {
            return true;
        }
        if (!output.isItemEqual(result)) {
            return false;
        }

        int resultCount = output.getCount() + result.getCount();
        return resultCount <= this.getInventoryStackLimit() && resultCount <= output.getMaxStackSize();
    }

    private void processItem() {
        if (this.canProcess()) {
            ItemStack[] inputs = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                inputs[i] = this.inventory.get(i);
            }

            ItemStack result = DimensionLoomRecipes.getResultForTileEntity(inputs);
            ItemStack output = this.inventory.get(9);

            if (output.isEmpty()) {
                this.inventory.set(9, result.copy());
            } else if (output.getItem() == result.getItem() && output.getMetadata() == result.getMetadata()) {
                output.grow(result.getCount());
            }

            // 消耗输入物品
            for (int i = 0; i < 9; i++) {
                if (!this.inventory.get(i).isEmpty()) {
                    this.inventory.get(i).shrink(1);
                }
            }
        }
    }

    public int getProcessTime() {
        return this.processTime;
    }

    public int getMaxProcessTime() {
        return this.maxProcessTime;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("ProcessTime", this.processTime);
        ItemStackHelper.saveAllItems(compound, this.inventory);

        if (this.hasCustomName()) {
            compound.setString("CustomName", this.customName);
        }

        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.inventory = NonNullList.<ItemStack>withSize(this.getSizeInventory(), ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(compound, this.inventory);
        this.processTime = compound.getInteger("ProcessTime");

        if (compound.hasKey("CustomName", 8)) {
            this.customName = compound.getString("CustomName");
        }
    }

    @Override
    public int getSizeInventory() {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.inventory) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return this.inventory.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return ItemStackHelper.getAndSplit(this.inventory, index, count);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return ItemStackHelper.getAndRemove(this.inventory, index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.inventory.set(index, stack);
        if (stack.getCount() > this.getInventoryStackLimit()) {
            stack.setCount(this.getInventoryStackLimit());
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        if (this.world.getTileEntity(this.pos) != this) {
            return false;
        } else {
            return player.getDistanceSq((double)this.pos.getX() + 0.5D,
                    (double)this.pos.getY() + 0.5D,
                    (double)this.pos.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == 9) {
            return false; // 输出槽不能输入
        }
        return true; // 输入槽可以放入任何物品
    }

    @Override
    public int getField(int id) {
        switch(id) {
            case 0: return this.processTime;
            case 1: return this.maxProcessTime;
            default: return 0;
        }
    }

    @Override
    public void setField(int id, int value) {
        switch(id) {
            case 0: this.processTime = value; break;
            case 1: this.maxProcessTime = value; break;
        }
    }

    @Override
    public int getFieldCount() {
        return 2;
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.customName : "container.dimension_loom";
    }

    @Override
    public boolean hasCustomName() {
        return this.customName != null && !this.customName.isEmpty();
    }

    public void setCustomName(String name) {
        this.customName = name;
    }

    @Override
    public ITextComponent getDisplayName() {
        return this.hasCustomName() ? new TextComponentString(this.getName()) : new TextComponentTranslation(this.getName());
    }
}
