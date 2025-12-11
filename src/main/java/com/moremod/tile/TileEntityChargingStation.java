package com.moremod.tile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 充能站TileEntity
 *
 * 功能：
 * - 極大容量 (100M RF)
 * - 無限快充電速度
 * - 可放入9個物品充電
 * - 站在上面的玩家也會被充電
 */
public class TileEntityChargingStation extends TileEntity implements ITickable {

    // 配置
    private static final int ENERGY_CAPACITY = 100000000;    // 100M RF
    private static final int MAX_RECEIVE = 50000000;         // 每tick最多接收 50M RF (大幅提升)
    private static final int PULL_RATE = 10000000;           // 每tick从周围抽取 10M RF
    private static final int CHARGE_RATE = Integer.MAX_VALUE; // 無限快充電
    private static final int SLOT_COUNT = 9;                 // 9個充電槽

    // 能量存儲
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                markDirty();
            }
            return received;
        }
    };

    // 物品槽
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // 只接受可充電的物品
            return stack.hasCapability(CapabilityEnergy.ENERGY, null);
        }
    };

    private int tickCounter = 0;

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        tickCounter++;

        // 主動從周圍方塊抽取能量
        if (energy.getEnergyStored() < energy.getMaxEnergyStored()) {
            pullEnergyFromNeighbors();
        }

        // 每tick處理充電
        if (energy.getEnergyStored() > 0) {
            // 充電物品槽中的物品
            chargeInventoryItems();

            // 充電站上方的玩家
            if (tickCounter % 5 == 0) {
                chargePlayersOnTop();
            }
        }
    }

    /**
     * 主動從周圍發電機/管線抽取能量
     */
    private void pullEnergyFromNeighbors() {
        int spaceAvailable = energy.getMaxEnergyStored() - energy.getEnergyStored();
        if (spaceAvailable <= 0) return;

        int toPull = Math.min(PULL_RATE, spaceAvailable);

        for (EnumFacing facing : EnumFacing.values()) {
            if (toPull <= 0) break;

            TileEntity neighbor = world.getTileEntity(pos.offset(facing));
            if (neighbor != null && neighbor.hasCapability(CapabilityEnergy.ENERGY, facing.getOpposite())) {
                IEnergyStorage neighborEnergy = neighbor.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
                if (neighborEnergy != null && neighborEnergy.canExtract()) {
                    int extracted = neighborEnergy.extractEnergy(toPull, false);
                    if (extracted > 0) {
                        energy.receiveEnergy(extracted, false);
                        toPull -= extracted;
                    }
                }
            }
        }
    }

    /**
     * 充電物品槽中的物品
     */
    private void chargeInventoryItems() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.hasCapability(CapabilityEnergy.ENERGY, null)) {
                IEnergyStorage itemEnergy = stack.getCapability(CapabilityEnergy.ENERGY, null);
                if (itemEnergy != null && itemEnergy.canReceive()) {
                    int toTransfer = Math.min(energy.getEnergyStored(), CHARGE_RATE);
                    int accepted = itemEnergy.receiveEnergy(toTransfer, false);
                    if (accepted > 0) {
                        extractEnergyInternal(accepted);
                    }
                }
            }
        }
    }

    /**
     * 充電站上方的玩家裝備
     */
    private void chargePlayersOnTop() {
        AxisAlignedBB area = new AxisAlignedBB(
            pos.getX(), pos.getY() + 0.875, pos.getZ(),
            pos.getX() + 1, pos.getY() + 2.5, pos.getZ() + 1
        );

        List<Entity> entities = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (Entity entity : entities) {
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                chargePlayerEquipment(player);
            }
        }
    }

    /**
     * 充電玩家的裝備
     */
    private void chargePlayerEquipment(EntityPlayer player) {
        // 充電主手和副手
        chargeItemStack(player.getHeldItemMainhand());
        chargeItemStack(player.getHeldItemOffhand());

        // 充電盔甲
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (slot.getSlotType() == EntityEquipmentSlot.Type.ARMOR) {
                chargeItemStack(player.getItemStackFromSlot(slot));
            }
        }

        // 充電整個背包
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            chargeItemStack(player.inventory.getStackInSlot(i));
        }

        // 充電飾品欄（如果有）
        if (player.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            // Baubles integration would go here
        }
    }

    /**
     * 充電單個物品
     */
    private void chargeItemStack(ItemStack stack) {
        if (stack.isEmpty() || energy.getEnergyStored() <= 0) return;

        if (stack.hasCapability(CapabilityEnergy.ENERGY, null)) {
            IEnergyStorage itemEnergy = stack.getCapability(CapabilityEnergy.ENERGY, null);
            if (itemEnergy != null && itemEnergy.canReceive()) {
                int toTransfer = Math.min(energy.getEnergyStored(), CHARGE_RATE);
                int accepted = itemEnergy.receiveEnergy(toTransfer, false);
                if (accepted > 0) {
                    extractEnergyInternal(accepted);
                }
            }
        }
    }

    /**
     * 內部提取能量
     */
    private void extractEnergyInternal(int amount) {
        // 直接操作能量值（因為 EnergyStorage 不允許提取）
        int stored = energy.getEnergyStored();
        int toExtract = Math.min(amount, stored);
        if (toExtract > 0) {
            // 使用反射或直接設置
            try {
                java.lang.reflect.Field field = EnergyStorage.class.getDeclaredField("energy");
                field.setAccessible(true);
                field.setInt(energy, stored - toExtract);
            } catch (Exception e) {
                // 如果反射失敗，使用替代方法
            }
            markDirty();
        }
    }

    /**
     * 掉落物品
     */
    public void dropItems() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                net.minecraft.inventory.InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    // ===== Getters =====

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    // ===== Capabilities =====

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY ||
               capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ||
               super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) energy;
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) inventory;
        }
        return super.getCapability(capability, facing);
    }

    // ===== NBT =====

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("Energy", energy.getEnergyStored());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Inventory")) {
            inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        }
        int fe = compound.getInteger("Energy");
        while (energy.getEnergyStored() < fe && energy.receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
    }

    // ===== 網絡同步 =====

    @Nullable
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

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }
}
