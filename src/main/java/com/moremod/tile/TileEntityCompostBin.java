package com.moremod.tile;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * 堆肥桶TileEntity - 将有机物转化为骨粉
 */
public class TileEntityCompostBin extends TileEntity implements ITickable {

    // 存储有机物的虚拟计数（不存储实际物品）
    private int storedCompost = 0;
    private static final int MAX_COMPOST = 64;

    // 堆肥进度
    private int compostProgress = 0;
    private static final int COMPOST_TIME = 600; // 30秒完成一次

    // 输出存储
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    // 有效的有机物列表
    private static final Set<Item> VALID_INPUTS = new HashSet<>();

    static {
        // 种子类
        VALID_INPUTS.add(Items.WHEAT_SEEDS);
        VALID_INPUTS.add(Items.MELON_SEEDS);
        VALID_INPUTS.add(Items.PUMPKIN_SEEDS);
        VALID_INPUTS.add(Items.BEETROOT_SEEDS);
        // 食物残渣
        VALID_INPUTS.add(Items.ROTTEN_FLESH);
        VALID_INPUTS.add(Items.SPIDER_EYE);
        VALID_INPUTS.add(Items.POISONOUS_POTATO);
        VALID_INPUTS.add(Items.APPLE);
        VALID_INPUTS.add(Items.MELON);
        VALID_INPUTS.add(Items.CARROT);
        VALID_INPUTS.add(Items.POTATO);
        VALID_INPUTS.add(Items.BEETROOT);
        VALID_INPUTS.add(Items.WHEAT);
        // 植物
        VALID_INPUTS.add(Items.REEDS);
        VALID_INPUTS.add(Item.getItemFromBlock(Blocks.TALLGRASS));
        VALID_INPUTS.add(Item.getItemFromBlock(Blocks.DEADBUSH));
        VALID_INPUTS.add(Item.getItemFromBlock(Blocks.VINE));
        VALID_INPUTS.add(Item.getItemFromBlock(Blocks.WATERLILY));
        VALID_INPUTS.add(Item.getItemFromBlock(Blocks.RED_FLOWER));
        VALID_INPUTS.add(Item.getItemFromBlock(Blocks.YELLOW_FLOWER));
        VALID_INPUTS.add(Item.getItemFromBlock(Blocks.SAPLING));
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        // 需要有机物才能进行堆肥
        if (storedCompost <= 0) {
            compostProgress = 0;
            return;
        }

        compostProgress++;

        // 每秒生成粒子效果
        if (compostProgress % 20 == 0 && world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                2, 0.3, 0.1, 0.3, 0.0);
        }

        // 完成堆肥
        if (compostProgress >= COMPOST_TIME) {
            // 消耗有机物
            int consumed = Math.min(storedCompost, 8);
            storedCompost -= consumed;

            // 生成骨粉
            int boneMealCount = Math.max(1, consumed / 4);
            ItemStack boneMeal = new ItemStack(Items.DYE, boneMealCount, 15); // 骨粉

            ItemStack existing = output.getStackInSlot(0);
            if (existing.isEmpty()) {
                output.setStackInSlot(0, boneMeal);
            } else if (existing.getMetadata() == 15 && existing.getCount() + boneMealCount <= 64) {
                existing.grow(boneMealCount);
            }
            // 如果输出满了，骨粉会丢失

            compostProgress = 0;
            markDirty();

            // 完成效果
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    15, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }

    /**
     * 检查是否是有效的有机物
     */
    public boolean isValidInput(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // 检查预定义列表
        if (VALID_INPUTS.contains(stack.getItem())) return true;

        // 检查树叶（所有树叶方块）
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block != Blocks.AIR && block.isLeaves(block.getDefaultState(), world, pos)) {
            return true;
        }

        return false;
    }

    /**
     * 添加有机物
     */
    public ItemStack addCompostMaterial(ItemStack stack) {
        if (!isValidInput(stack)) return stack;

        int canAdd = MAX_COMPOST - storedCompost;
        if (canAdd <= 0) return stack;

        int toAdd = Math.min(canAdd, stack.getCount());
        storedCompost += toAdd;
        markDirty();

        ItemStack remaining = stack.copy();
        remaining.shrink(toAdd);
        return remaining;
    }

    /**
     * 取出骨粉
     */
    public ItemStack extractOutput() {
        ItemStack result = output.getStackInSlot(0).copy();
        output.setStackInSlot(0, ItemStack.EMPTY);
        markDirty();
        return result;
    }

    /**
     * 掉落库存
     */
    public void dropInventory() {
        if (world == null || world.isRemote) return;

        ItemStack outputStack = output.getStackInSlot(0);
        if (!outputStack.isEmpty()) {
            EntityItem entityItem = new EntityItem(world,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                outputStack.copy());
            world.spawnEntity(entityItem);
        }
    }

    // ========== Getters ==========

    public int getCompostProgress() {
        if (storedCompost <= 0) return 0;
        return (compostProgress * 100) / COMPOST_TIME;
    }

    public int getStoredAmount() {
        return storedCompost;
    }

    public int getOutputCount() {
        ItemStack stack = output.getStackInSlot(0);
        return stack.isEmpty() ? 0 : stack.getCount();
    }

    // ========== NBT ==========

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("StoredCompost", storedCompost);
        compound.setInteger("Progress", compostProgress);
        compound.setTag("Output", output.serializeNBT());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        storedCompost = compound.getInteger("StoredCompost");
        compostProgress = compound.getInteger("Progress");
        output.deserializeNBT(compound.getCompoundTag("Output"));
    }

    // ========== Capability ==========

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return facing == EnumFacing.DOWN; // 只能从底部抽取
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == EnumFacing.DOWN) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(output);
        }
        return super.getCapability(capability, facing);
    }
}
