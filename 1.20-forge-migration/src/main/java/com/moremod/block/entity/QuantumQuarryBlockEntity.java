package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * 量子采石场BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 虚拟维度采矿
 * - 需要六面量子驱动器
 * - 多种模式（采矿/怪物掉落/战利品）
 */
public class QuantumQuarryBlockEntity extends BaseEnergyBlockEntity implements MenuProvider {

    private static final int CAPACITY = 1000000;
    private static final int BUFFER_SIZE = 27; // 输出缓冲区大小

    public enum QuarryMode {
        MINING,      // 采矿模式
        MOB_DROPS,   // 怪物掉落模式
        LOOT         // 战利品模式
    }

    private QuarryMode currentMode = QuarryMode.MINING;
    private boolean structureValid = false;
    private boolean redstoneEnabled = true;
    private int tickCounter = 0;

    public QuantumQuarryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUANTUM_QUARRY.get(), pos, state,
                CAPACITY, 50000, 0, BUFFER_SIZE);
    }

    @Override
    protected ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        tickCounter++;

        // 每20tick检查一次结构
        if (tickCounter % 20 == 0) {
            checkStructure();
        }

        // 如果红石信号关闭或结构无效，不工作
        if (!redstoneEnabled || !structureValid) {
            return;
        }

        // 每10tick尝试生成物品
        if (tickCounter % 10 == 0) {
            processQuarry();
        }
    }

    private void checkStructure() {
        // TODO: 检查六面是否有量子驱动器
        // 暂时返回true
        structureValid = true;
    }

    private void processQuarry() {
        // 检查能量
        int energyCost = getEnergyCostPerOperation();
        if (getEnergyStored() < energyCost) {
            return;
        }

        // 检查输出槽是否有空间
        if (!hasSpaceInBuffer()) {
            return;
        }

        // TODO: 根据模式生成物品
        // ItemStack result = generateItem();
        // if (!result.isEmpty()) {
        //     insertIntoBuffer(result);
        //     extractEnergy(energyCost, false);
        // }
    }

    private boolean hasSpaceInBuffer() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private int getEnergyCostPerOperation() {
        return switch (currentMode) {
            case MINING -> 1000;
            case MOB_DROPS -> 2000;
            case LOOT -> 5000;
        };
    }

    public void updateRedstoneState() {
        if (level != null) {
            redstoneEnabled = !level.hasNeighborSignal(getBlockPos());
        }
    }

    public boolean isStructureValid() {
        return structureValid;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    public void openMenu(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, this, getBlockPos());
        }
    }

    // ===== MenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.moremod.quantum_quarry");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // TODO: 返回QuantumQuarryMenu实例
        return null;
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Mode", currentMode.ordinal());
        tag.putBoolean("RedstoneEnabled", redstoneEnabled);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        currentMode = QuarryMode.values()[tag.getInt("Mode") % QuarryMode.values().length];
        redstoneEnabled = tag.getBoolean("RedstoneEnabled");
    }
}
