package com.moremod.tile;

import com.moremod.recipe.SwordUpgradeRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntitySwordUpgradeStationMaterial extends TileEntity {

    public static final int SLOT_BASE = 0; // 基底剑
    public static final int SLOT_MAT  = 1; // 材料
    public static final int SLOT_OUT  = 2; // 产物

    private final ItemStackHandler items = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot == SLOT_BASE || slot == SLOT_MAT) recalcOutput();
            markDirty();
            super.onContentsChanged(slot);
        }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_OUT) return false; // 输出槽禁止放
            if (slot == SLOT_BASE) return isSword(stack);
            // ✅ 修复：使用新的 isValidMaterial 方法检查材料
            if (slot == SLOT_MAT)  return SwordUpgradeRegistry.isValidMaterial(stack.getItem());
            return true;
        }
    };

    public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }

    /** 生成预览产物（不消耗）：复制 NBT、保持耐久比例 */
    public void recalcOutput() {
        ItemStack base = items.getStackInSlot(SLOT_BASE);
        ItemStack mat  = items.getStackInSlot(SLOT_MAT);

        if (base.isEmpty() || mat.isEmpty() || !isSword(base)) {
            items.setStackInSlot(SLOT_OUT, ItemStack.EMPTY);
            markDirty();
            return;
        }

        // 使用NBT匹配的配方查找：传递完整ItemStack
        SwordUpgradeRegistry.Recipe r = SwordUpgradeRegistry.getRecipe(base, mat.getItem());
        if (r == null || r.targetSword == null) {
            items.setStackInSlot(SLOT_OUT, ItemStack.EMPTY);
            markDirty();
            return;
        }

        ItemStack out = new ItemStack(r.targetSword);
        if (out.isEmpty()) {
            items.setStackInSlot(SLOT_OUT, ItemStack.EMPTY);
            markDirty();
            return;
        }
        out.setCount(1);

        // 复制全部 NBT
        if (base.hasTagCompound()) {
            out.setTagCompound(base.getTagCompound().copy());
        }

        // 复制耐久"比例"
        if (base.isItemStackDamageable() && out.isItemStackDamageable()) {
            double ratio = base.getItemDamage() / (double) base.getMaxDamage();
            int newDamage = (int) Math.floor(ratio * out.getMaxDamage());
            out.setItemDamage(newDamage);
        }

        items.setStackInSlot(SLOT_OUT, out);
        markDirty();
    }

    /** 真实消耗：材料 -1，基底 -1，然后重算预览 */
    public void finishUpgrade() {
        // 消耗材料
        ItemStack mat = items.getStackInSlot(SLOT_MAT);
        if (!mat.isEmpty()) {
            mat.shrink(1);
            if (mat.getCount() <= 0) items.setStackInSlot(SLOT_MAT, ItemStack.EMPTY);
        }
        // 消耗基底
        ItemStack base = items.getStackInSlot(SLOT_BASE);
        if (!base.isEmpty()) {
            base.shrink(1);
            if (base.getCount() <= 0) items.setStackInSlot(SLOT_BASE, ItemStack.EMPTY);
        }
        // 重新计算（通常会清空输出）
        recalcOutput();
        markDirty();
    }

    private boolean isSword(ItemStack s) {
        return !s.isEmpty() && (s.getItem() instanceof ItemSword ||
                s.getItem().getToolClasses(s).contains("sword"));
    }

    // ---- Capability / NBT ----
    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T) items;
        return super.getCapability(capability, facing);
    }
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("inv", items.serializeNBT());
        return tag;
    }
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("inv")) items.deserializeNBT(tag.getCompoundTag("inv"));
        recalcOutput(); // 重新计算输出槽，确保世界加载后显示正确
    }
}