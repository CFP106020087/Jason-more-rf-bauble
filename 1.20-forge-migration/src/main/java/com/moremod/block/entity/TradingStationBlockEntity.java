package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
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
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 村民交易机BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 存储村民数据和交易列表
 * - 自动化执行交易
 * - 消耗RF能量
 *
 * 槽位定义:
 * 0 = 村民胶囊槽
 * 1 = 输入物品槽1
 * 2 = 输入物品槽2 (可选)
 * 3 = 输出物品槽
 */
public class TradingStationBlockEntity extends BlockEntity implements MenuProvider {

    private static final int MAX_ENERGY = 100000;
    private static final int ENERGY_PER_TRADE = 10000;
    private static final int ENERGY_RECEIVE_RATE = 100;
    private static final int AUTO_TRADE_INTERVAL = 100; // 5秒

    // 物品处理器
    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == 0) {
                loadVillagerFromCapsule();
            }
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot == 3) {
                return false; // 输出槽不允许插入
            }
            // TODO: 槽位0只允许村民胶囊
            return true;
        }
    };

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    // 能量存储
    private final EnergyStorage energyStorage = new EnergyStorage(MAX_ENERGY, ENERGY_RECEIVE_RATE, 0);
    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> energyStorage);

    // 村民数据
    private CompoundTag villagerData = null;
    private int currentTradeIndex = 0;
    private int workTimer = 0;

    public TradingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRADING_STATION.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        workTimer++;
        if (workTimer >= AUTO_TRADE_INTERVAL) {
            workTimer = 0;

            if (canTrade()) {
                executeTrade();
            }
        }
    }

    private void loadVillagerFromCapsule() {
        ItemStack capsule = inventory.getStackInSlot(0);

        if (capsule.isEmpty()) {
            if (villagerData != null) {
                villagerData = null;
                currentTradeIndex = 0;
                syncToClient();
            }
            return;
        }

        // TODO: 检查是否是村民胶囊并读取数据
        // 需要实现ItemVillagerCapsule
    }

    private boolean canTrade() {
        if (villagerData == null) return false;
        if (energyStorage.getEnergyStored() < ENERGY_PER_TRADE) return false;

        // TODO: 实现交易检查逻辑
        // 需要读取村民交易列表并检查输入物品
        return false;
    }

    private void executeTrade() {
        // TODO: 实现交易执行逻辑
        // 1. 消耗输入物品
        // 2. 生成输出物品
        // 3. 消耗能量
        // energyStorage.extractEnergy(ENERGY_PER_TRADE, false);
    }

    public void nextTrade() {
        // TODO: 切换到下一个交易
        setChanged();
        syncToClient();
    }

    public void previousTrade() {
        // TODO: 切换到上一个交易
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void dropInventory() {
        if (level == null || level.isClientSide()) return;

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5,
                        stack.copy());
            }
        }
    }

    // ===== Getters =====

    public boolean hasVillager() {
        return villagerData != null;
    }

    public int getCurrentTradeIndex() {
        return currentTradeIndex;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    // ===== MenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.moremod.trading_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // TODO: 返回TradingStationMenu实例
        return null;
    }

    // ===== Capabilities =====

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
        energyHandler.invalidate();
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putInt("TradeIndex", currentTradeIndex);
        tag.putInt("WorkTimer", workTimer);

        if (villagerData != null) {
            tag.put("Villager", villagerData);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        int storedEnergy = tag.getInt("Energy");
        energyStorage.receiveEnergy(storedEnergy, false);
        currentTradeIndex = tag.getInt("TradeIndex");
        workTimer = tag.getInt("WorkTimer");

        if (tag.contains("Villager")) {
            villagerData = tag.getCompound("Villager");
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("TradeIndex", currentTradeIndex);
        tag.putInt("Energy", energyStorage.getEnergyStored());

        if (villagerData != null) {
            tag.put("Villager", villagerData);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        currentTradeIndex = tag.getInt("TradeIndex");
        int energy = tag.getInt("Energy");
        energyStorage.receiveEnergy(energy, false);

        if (tag.contains("Villager")) {
            villagerData = tag.getCompound("Villager");
        } else {
            villagerData = null;
        }
    }
}
