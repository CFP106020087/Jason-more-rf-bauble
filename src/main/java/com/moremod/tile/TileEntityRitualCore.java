package com.moremod.tile;

import com.moremod.ritual.RitualInfusionAPI;
import com.moremod.ritual.RitualInfusionRecipe;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileEntityRitualCore extends TileEntity implements ITickable {

    private final ItemStackHandler inv = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            if (world != null && !world.isRemote) {
                IBlockState state = world.getBlockState(pos);
                world.notifyBlockUpdate(pos, state, state, 3);
            }
        }
    };

    private RitualInfusionRecipe active;
    private int process;
    private boolean isActive = false;
    private boolean hasEnoughEnergy = false;

    // 8个基座位置：东南西北 + 东南、东北、西南、西北
    private static final BlockPos[] OFFS8 = new BlockPos[]{
            new BlockPos( 3, 0,  0),  // 东
            new BlockPos(-3, 0,  0),  // 西
            new BlockPos( 0, 0,  3),  // 南
            new BlockPos( 0, 0, -3),  // 北
            new BlockPos( 2, 0,  2),  // 东南
            new BlockPos( 2, 0, -2),  // 东北
            new BlockPos(-2, 0,  2),  // 西南
            new BlockPos(-2, 0, -2)   // 西北
    };

    @Override
    public void update() {
        if (world == null) return;

        if (world.isRemote) {
            if (isActive && hasEnoughEnergy) {
                spawnParticles();
            }
            return;
        }

        // 获取所有有物品的基座
        List<TileEntityPedestal> peds = findPedestals();
        if (peds.isEmpty()) {
            reset();
            return;
        }

        // 检查配方
        if (active == null || !coreMatchesAnyRecipe()) {
            active = findMatchingRecipe(peds);
            process = 0;
        }

        if (active == null) {
            setActiveState(false, false);
            return;
        }

        // 确保有足够的基座
        if (peds.size() < active.getPedestalCount()) {
            setActiveState(false, false);
            return;
        }

        int perTick = Math.max(1, active.getEnergyPerPedestal() / Math.max(1, active.getTime()));

        // 能量检查 - 只检查配方需要的基座数量
        boolean energyOk = true;
        for (int i = 0; i < active.getPedestalCount() && i < peds.size(); i++) {
            if (peds.get(i).getEnergy().getEnergyStored() < perTick) {
                energyOk = false;
                break;
            }
        }

        if (!energyOk) {
            process = 0;
            setActiveState(true, false);  // 激活但能量不足
            return;
        }

        // 扣能量 & 进度 - 只扣配方需要的基座
        for (int i = 0; i < active.getPedestalCount() && i < peds.size(); i++) {
            peds.get(i).getEnergy().extractEnergy(perTick, false);
        }
        process++;
        setActiveState(true, true);  // 激活且能量充足

        if (process % 5 == 0) {
            world.playEvent(2005, pos, 0);
        }

        if (process >= active.getTime()) {
            if (world.rand.nextFloat() < active.getFailChance()) {
                onFail(peds);
            } else {
                completeRitual(peds);
            }
            reset();
        }
    }

    private List<TileEntityPedestal> findPedestals() {
        List<TileEntityPedestal> list = new ArrayList<>();
        for (BlockPos off : OFFS8) {
            TileEntity te = world.getTileEntity(pos.add(off));
            if (te instanceof TileEntityPedestal) {
                TileEntityPedestal ped = (TileEntityPedestal) te;
                // 只添加有物品的基座
                if (!ped.isEmpty()) {
                    list.add(ped);
                }
            }
        }
        return list;
    }

    private void completeRitual(List<TileEntityPedestal> peds) {
        consumeInputs(peds);
        inv.setStackInSlot(1, active.getOutput().copy());
        markDirty();
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
        world.markBlockRangeForRenderUpdate(pos.add(-1, -1, -1), pos.add(1, 1, 1));
    }

    private void setActiveState(boolean active, boolean hasEnergy) {
        if (this.isActive != active || this.hasEnoughEnergy != hasEnergy) {
            this.isActive = active;
            this.hasEnoughEnergy = hasEnergy;
            markDirty();
            if (!world.isRemote) {
                IBlockState state = world.getBlockState(pos);
                world.notifyBlockUpdate(pos, state, state, 3);
            }
        }
    }

    private void spawnParticles() {
        if (world.rand.nextInt(3) == 0) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5;

            for (int i = 0; i < 2; i++) {
                world.spawnParticle(
                        EnumParticleTypes.PORTAL,
                        x + (world.rand.nextDouble() - 0.5) * 0.5,
                        y + world.rand.nextDouble() * 0.5,
                        z + (world.rand.nextDouble() - 0.5) * 0.5,
                        0, 0.1, 0
                );
            }
        }
    }

    private boolean coreMatchesAnyRecipe() {
        for (RitualInfusionRecipe r : RitualInfusionAPI.RITUAL_RECIPES) {
            if (r.getCore().apply(inv.getStackInSlot(0))) return true;
        }
        return false;
    }

    private RitualInfusionRecipe findMatchingRecipe(List<TileEntityPedestal> peds) {
        List<net.minecraft.item.ItemStack> stacks = new ArrayList<>();
        for (TileEntityPedestal p : peds) {
            stacks.add(p.getInv().getStackInSlot(0));
        }

        for (RitualInfusionRecipe r : RitualInfusionAPI.RITUAL_RECIPES) {
            if (r.getCore().apply(inv.getStackInSlot(0)) && r.matchPedestalStacks(stacks)) {
                return r;
            }
        }
        return null;
    }

    private void consumeInputs(List<TileEntityPedestal> peds) {
        inv.extractItem(0, 1, false);

        // 创建配方需要的物品列表副本
        List<net.minecraft.item.crafting.Ingredient> needed = new ArrayList<>(active.getPedestalCount());
        for (int i = 0; i < active.getPedestalCount(); i++) {
            needed.add(active.getPedestalItems().get(i));
        }

        // 消耗匹配的物品
        for (TileEntityPedestal ped : peds) {
            net.minecraft.item.ItemStack stack = ped.getInv().getStackInSlot(0);
            for (int i = 0; i < needed.size(); i++) {
                if (needed.get(i).apply(stack)) {
                    ped.consumeOne();
                    needed.remove(i);
                    break;
                }
            }
        }
    }

    private void onFail(List<TileEntityPedestal> peds) {
        world.playEvent(2001, pos, 0);
        world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.0F, false);
        consumeInputs(peds);
    }

    private void reset() {
        process = 0;
        active = null;
        setActiveState(false, false);
    }

    public boolean isActive() { return isActive && process > 0; }
    public boolean hasEnoughEnergy() { return hasEnoughEnergy; }
    public int getProgress() { return process; }
    public int getMaxTime() { return active != null ? active.getTime() : 100; }
    public ItemStackHandler getInv() { return inv; }
    public BlockPos[] getPedestalOffsets() { return OFFS8; }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 256.0D;
    }

    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inv);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inv", inv.serializeNBT());
        compound.setInteger("Proc", process);
        compound.setBoolean("Active", isActive);
        compound.setBoolean("HasEnergy", hasEnoughEnergy);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inv.deserializeNBT(compound.getCompoundTag("Inv"));
        process = compound.getInteger("Proc");
        isActive = compound.getBoolean("Active");
        hasEnoughEnergy = compound.getBoolean("HasEnergy");
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
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }
}