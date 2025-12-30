package com.moremod.block.entity;

import com.moremod.block.BottlingMachineBlock;
import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 装瓶机BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 将液体装入容器中
 * - 支持自动化流体处理
 *
 * 槽位定义:
 * 0 = 输入槽
 * 1 = 输出槽
 * 2 = 流体容器槽
 */
public class BottlingMachineBlockEntity extends BlockEntity implements MenuProvider {

    private static final int PROCESS_TIME = 40; // 2秒

    // 液体储罐
    private final FluidTank fluidTank = new FluidTank(10000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> fluidTank);

    // 物品栏位
    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot == 0) { // 输入槽
                return true;
            } else if (slot == 2) { // 流体容器槽
                return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            }
            return false; // 输出槽只能提取
        }
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    // 处理进度
    private int processTime = 0;
    private boolean isProcessing = false;

    public BottlingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOTTLING_MACHINE.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        boolean wasProcessing = isProcessing;

        // 处理流体容器
        handleFluidContainer();

        // 查找并处理配方
        if (canProcess()) {
            isProcessing = true;
            processTime++;

            if (processTime >= PROCESS_TIME) {
                processRecipe();
                processTime = 0;
            }
        } else {
            isProcessing = false;
            processTime = 0;
        }

        // 更新方块状态
        if (wasProcessing != isProcessing) {
            BottlingMachineBlock.setActiveState(level, worldPosition, isProcessing);
        }
    }

    private void handleFluidContainer() {
        ItemStack container = inventory.getStackInSlot(2);
        if (container.isEmpty()) return;

        container.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(containerHandler -> {
            // 尝试从容器中抽取流体
            FluidStack drained = containerHandler.drain(1000, IFluidHandler.FluidAction.SIMULATE);
            if (!drained.isEmpty()) {
                int filled = fluidTank.fill(drained, IFluidHandler.FluidAction.SIMULATE);
                if (filled > 0) {
                    FluidStack actualDrained = containerHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    fluidTank.fill(actualDrained, IFluidHandler.FluidAction.EXECUTE);
                    inventory.setStackInSlot(2, containerHandler.getContainer());
                }
            }
        });
    }

    private boolean canProcess() {
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty() || fluidTank.getFluidAmount() <= 0) {
            return false;
        }

        // TODO: 实现配方检查
        // 检查输出槽是否有空间
        return true;
    }

    private void processRecipe() {
        // TODO: 实现配方处理
        // 1. 消耗输入物品
        // 2. 消耗流体
        // 3. 生成输出物品
    }

    // ===== Getters =====

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public int getProcessTime() {
        return processTime;
    }

    public int getMaxProcessTime() {
        return PROCESS_TIME;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    // ===== MenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.moremod.bottling_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // TODO: 返回BottlingMachineMenu实例
        return null;
    }

    // ===== Capabilities =====

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
        fluidHandler.invalidate();
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.put("FluidTank", fluidTank.writeToNBT(new CompoundTag()));
        tag.putInt("ProcessTime", processTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        if (tag.contains("FluidTank")) {
            fluidTank.readFromNBT(tag.getCompound("FluidTank"));
        }
        processTime = tag.getInt("ProcessTime");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
}
