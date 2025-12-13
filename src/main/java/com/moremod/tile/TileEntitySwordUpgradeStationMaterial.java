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

/**
 * 物品升级台 TileEntity
 * v2.0: 支持任意物品升级，不限于剑
 */
public class TileEntitySwordUpgradeStationMaterial extends TileEntity {

    public static final int SLOT_BASE = 0; // 输入物品A
    public static final int SLOT_MAT  = 1; // 材料物品B
    public static final int SLOT_OUT  = 2; // 输出物品C

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
            // v2.0: 基底槽接受任意物品（不再限制为剑）
            if (slot == SLOT_BASE) return !stack.isEmpty();
            // 材料槽检查是否有匹配的配方
            if (slot == SLOT_MAT) return SwordUpgradeRegistry.isValidMaterial(stack);
            return true;
        }
    };

    public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }

    /**
     * 生成预览产物（不消耗）
     * v2.0: 支持任意物品，根据配方决定是否复制输入NBT
     */
    public void recalcOutput() {
        ItemStack base = items.getStackInSlot(SLOT_BASE);
        ItemStack mat  = items.getStackInSlot(SLOT_MAT);

        // v2.0: 不再要求必须是剑
        if (base.isEmpty() || mat.isEmpty()) {
            items.setStackInSlot(SLOT_OUT, ItemStack.EMPTY);
            markDirty();
            return;
        }

        // 使用完整ItemStack查找配方（支持材料NBT匹配）
        SwordUpgradeRegistry.Recipe r = SwordUpgradeRegistry.getRecipe(base, mat);
        if (r == null) {
            items.setStackInSlot(SLOT_OUT, ItemStack.EMPTY);
            markDirty();
            return;
        }

        // 使用新版outputStack（支持完整NBT）
        ItemStack out;
        if (!r.outputStack.isEmpty()) {
            out = r.outputStack.copy();
        } else if (r.targetSword != null) {
            // 兼容旧版配方
            out = new ItemStack(r.targetSword);
        } else {
            items.setStackInSlot(SLOT_OUT, ItemStack.EMPTY);
            markDirty();
            return;
        }

        if (out.isEmpty()) {
            items.setStackInSlot(SLOT_OUT, ItemStack.EMPTY);
            markDirty();
            return;
        }
        out.setCount(1);

        // 根据配方设置决定是否复制输入NBT
        if (r.copyInputNBT && base.hasTagCompound()) {
            // 合并NBT：先复制输入NBT，再叠加输出NBT
            NBTTagCompound baseNBT = base.getTagCompound().copy();
            if (out.hasTagCompound()) {
                // 输出NBT覆盖输入NBT中的同名键
                NBTTagCompound outNBT = out.getTagCompound();
                for (String key : outNBT.getKeySet()) {
                    baseNBT.setTag(key, outNBT.getTag(key).copy());
                }
            }
            out.setTagCompound(baseNBT);
        }
        // 如果 copyInputNBT = false，则仅使用配方指定的输出NBT（已在outputStack中）

        // 复制耐久"比例"（仅当两者都可损坏时）
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