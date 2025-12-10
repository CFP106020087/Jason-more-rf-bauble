package com.moremod.tile;

import com.moremod.block.BlockBloodGenerator;
import com.moremod.energy.BloodEnergyHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import net.minecraft.block.state.IBlockState;

import javax.annotation.Nullable;

/**
 * 血液发电机 TileEntity
 * 从沾血武器中提取能量，转换为RF
 */
public class TileEntityBloodGenerator extends TileEntity implements ITickable {

    /**
     * 防止方块状态变化时TileEntity被重新创建
     * 这是导致物品消失的关键修复
     */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        // 只有方块类型改变时才重新创建TileEntity
        return oldState.getBlock() != newState.getBlock();
    }

    // 配置
    private static final int MAX_ENERGY = 5000000;          // 500万RF容量
    private static final int MAX_OUTPUT = 50000;            // 每tick最大输出 50000 RF
    private static final int EXTRACTION_RATE = 10000;       // 每tick提取 10000 血液能量
    private static final int CONVERSION_EFFICIENCY = 100;   // 转换效率 100% (血液能量 -> RF)

    // 能量存储
    private final EnergyStorageInternal energyStorage = new EnergyStorageInternal(MAX_ENERGY, 0, MAX_OUTPUT);

    // 物品存储: 0=输入槽(沾血武器), 1=输出槽(干净武器)
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            TileEntityBloodGenerator.this.markDirty();
            if (world != null && !world.isRemote) {
                syncToClient();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                // 输入槽：接受任何有效武器（有血液数据才会发电，但允许存放）
                return BloodEnergyHandler.isValidWeapon(stack);
            }
            // 输出槽：允许取出，但不限制（内部移动需要）
            return true;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1; // 每槽只能放一个物品
        }
    };

    // 运行状态
    private boolean isActive = false;
    private int currentBloodEnergy = 0;     // 当前正在处理的血液能量
    private int currentFleshChunks = 0;     // 当前正在处理的肉块
    private int totalEnergyToExtract = 0;   // 总共要提取的能量
    private int extractedEnergy = 0;        // 已提取的能量

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        boolean wasActive = isActive;

        // 尝试输出能量到相邻方块
        if (energyStorage.getEnergyStored() > 0) {
            outputEnergy();
        }

        // 检查是否有正在处理的物品
        if (totalEnergyToExtract > 0 && extractedEnergy < totalEnergyToExtract) {
            // 继续提取
            processExtraction();
            isActive = true;
        } else if (!inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(1).isEmpty()) {
            // 开始新的提取
            startExtraction();
        } else {
            isActive = false;
        }

        // 更新方块状态
        if (wasActive != isActive) {
            BlockBloodGenerator.setActiveState(world, pos, isActive);
            markDirty();
        }
    }

    /**
     * 开始提取过程
     */
    private void startExtraction() {
        ItemStack inputStack = inventory.getStackInSlot(0);
        if (inputStack.isEmpty() || !BloodEnergyHandler.hasBloodData(inputStack)) {
            return;
        }

        // 计算总能量
        currentBloodEnergy = BloodEnergyHandler.getBloodEnergy(inputStack);
        currentFleshChunks = BloodEnergyHandler.getFleshChunks(inputStack);
        int bossKills = BloodEnergyHandler.getBossKills(inputStack);

        totalEnergyToExtract = currentBloodEnergy +
                              (currentFleshChunks * BloodEnergyHandler.FLESH_ENERGY) +
                              (bossKills * 50000);
        extractedEnergy = 0;

        isActive = true;
        markDirty();
        syncToClient();
    }

    /**
     * 处理能量提取
     */
    private void processExtraction() {
        // 检查能量存储是否有空间
        int spaceAvailable = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        if (spaceAvailable <= 0) return;

        // 计算本tick提取量
        int toExtract = Math.min(EXTRACTION_RATE, totalEnergyToExtract - extractedEnergy);
        toExtract = Math.min(toExtract, spaceAvailable);

        if (toExtract > 0) {
            // 应用转换效率
            int rfGenerated = (toExtract * CONVERSION_EFFICIENCY) / 100;
            energyStorage.receiveEnergyInternal(rfGenerated);
            extractedEnergy += toExtract;
        }

        // 检查是否完成
        if (extractedEnergy >= totalEnergyToExtract) {
            finishExtraction();
        }

        markDirty();
    }

    /**
     * 完成提取，返还干净武器
     */
    private void finishExtraction() {
        ItemStack inputStack = inventory.getStackInSlot(0);
        if (!inputStack.isEmpty()) {
            // 清除血液数据
            BloodEnergyHandler.clearBloodData(inputStack);

            // 移动到输出槽
            inventory.setStackInSlot(1, inputStack.copy());
            inventory.setStackInSlot(0, ItemStack.EMPTY);
        }

        // 重置状态
        currentBloodEnergy = 0;
        currentFleshChunks = 0;
        totalEnergyToExtract = 0;
        extractedEnergy = 0;
        isActive = false;

        markDirty();
        syncToClient();
    }

    /**
     * 输出能量到相邻方块
     */
    private void outputEnergy() {
        int toOutput = Math.min(MAX_OUTPUT, energyStorage.getEnergyStored());
        if (toOutput <= 0) return;

        for (EnumFacing facing : EnumFacing.VALUES) {
            TileEntity te = world.getTileEntity(pos.offset(facing));
            if (te != null && te.hasCapability(CapabilityEnergy.ENERGY, facing.getOpposite())) {
                IEnergyStorage storage = te.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
                if (storage != null && storage.canReceive()) {
                    int accepted = storage.receiveEnergy(toOutput, false);
                    if (accepted > 0) {
                        energyStorage.extractEnergyInternal(accepted);
                        toOutput -= accepted;
                        if (toOutput <= 0) break;
                    }
                }
            }
        }
    }

    // ========== Getters ==========

    public boolean isActive() { return isActive; }
    public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    public int getMaxEnergy() { return MAX_ENERGY; }
    public IEnergyStorage getEnergyStorage() { return energyStorage; }
    public ItemStackHandler getInventory() { return inventory; }

    public int getCurrentBloodEnergy() { return currentBloodEnergy; }
    public int getCurrentFleshChunks() { return currentFleshChunks; }
    public int getTotalEnergyToExtract() { return totalEnergyToExtract; }
    public int getExtractedEnergy() { return extractedEnergy; }

    public float getProgress() {
        if (totalEnergyToExtract <= 0) return 0;
        return (float) extractedEnergy / totalEnergyToExtract;
    }

    // ========== 客户端同步Setters ==========

    public void setClientEnergy(int energy) {
        energyStorage.setEnergy(energy);
    }

    public void setClientTotalEnergy(int total) {
        this.totalEnergyToExtract = total;
    }

    public void setClientExtracted(int extracted) {
        this.extractedEnergy = extracted;
    }

    // ========== 同步 ==========

    private void syncToClient() {
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    // ========== NBT ==========

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setBoolean("Active", isActive);
        compound.setInteger("CurrentBlood", currentBloodEnergy);
        compound.setInteger("CurrentFlesh", currentFleshChunks);
        compound.setInteger("TotalEnergy", totalEnergyToExtract);
        compound.setInteger("Extracted", extractedEnergy);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        energyStorage.setEnergy(compound.getInteger("Energy"));
        isActive = compound.getBoolean("Active");
        currentBloodEnergy = compound.getInteger("CurrentBlood");
        currentFleshChunks = compound.getInteger("CurrentFlesh");
        totalEnergyToExtract = compound.getInteger("TotalEnergy");
        extractedEnergy = compound.getInteger("Extracted");
    }

    // ========== Capabilities ==========

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        if (capability == CapabilityEnergy.ENERGY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }
        return super.getCapability(capability, facing);
    }

    // ========== 内部能量存储 ==========

    private static class EnergyStorageInternal extends EnergyStorage {
        public EnergyStorageInternal(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        public void setEnergy(int energy) {
            this.energy = Math.min(energy, this.capacity);
        }

        public void receiveEnergyInternal(int amount) {
            this.energy = Math.min(this.energy + amount, this.capacity);
        }

        public void extractEnergyInternal(int amount) {
            this.energy = Math.max(0, this.energy - amount);
        }
    }
}
