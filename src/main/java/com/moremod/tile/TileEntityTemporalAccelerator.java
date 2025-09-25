package com.moremod.tile;

import net.minecraft.block.BlockFurnace;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TileEntityTemporalAccelerator extends TileEntity implements ITickable, IEnergyStorage {

    // 配置参数
    private static final int RANGE = 8; // 16x16 区域 (中心±8)
    private static final int HEIGHT_RANGE = 4; // 垂直范围
    private static final int TICK_RATE = 3; // 加速倍率
    private static final double DOUBLE_OUTPUT_CHANCE = 0.25; // 25%双倍产出概率
    private static final double TRIPLE_OUTPUT_CHANCE = 0.05; // 5%三倍产出概率（矿物）

    // 能量配置
    private static final int MAX_ENERGY = 100000; // 最大存储100k FE
    private static final int MAX_RECEIVE = 1000; // 最大输入1000 FE/t
    private static final int ENERGY_PER_OPERATION = 5; // 每次加速操作消耗5 FE
    private static final int BASE_CONSUMPTION = 20; // 基础消耗20 FE/t (激活时)

    // 状态变量
    private int energy = 0;
    private boolean active = false;
    private Random random = new Random();
    private int tickCounter = 0;
    private int operationsThisTick = 0;

    // 用于跟踪熔炉的熔炼完成，防止重复触发双倍产出
    private Map<BlockPos, SmeltTracker> smeltTrackers = new HashMap<>();

    // 内部类用于跟踪熔炼
    private static class SmeltTracker {
        ItemStack lastInput;
        int completionCount;
        long lastCompletionTime;

        SmeltTracker() {
            this.lastInput = ItemStack.EMPTY;
            this.completionCount = 0;
            this.lastCompletionTime = 0;
        }

        boolean shouldProcessBonus(ItemStack currentInput, long worldTime) {
            // 检查是否是新的熔炼批次
            if (!ItemStack.areItemsEqual(lastInput, currentInput) ||
                    !ItemStack.areItemStackTagsEqual(lastInput, currentInput)) {
                // 新的输入物品，重置计数
                lastInput = currentInput.copy();
                completionCount = 0;
                return true;
            }

            // 检查是否是同一批次的重复触发（10tick内视为重复）
            if (worldTime - lastCompletionTime < 10) {
                return false;
            }

            return true;
        }

        void recordCompletion(long worldTime) {
            completionCount++;
            lastCompletionTime = worldTime;
        }
    }

    @Override
    public void update() {
        if (!world.isRemote) {
            tickCounter++;
            operationsThisTick = 0;

            if (active && energy >= BASE_CONSUMPTION) {
                // 消耗基础能量
                energy -= BASE_CONSUMPTION;

                // 每2tick执行一次加速以降低性能消耗
                if (tickCounter % 2 == 0) {
                    accelerateArea();
                }

                markDirty();
            } else if (active && energy < BASE_CONSUMPTION) {
                // 能量不足，自动关闭
                active = false;
                markDirty();
            }

            // 每3000tick清理一次跟踪数据，避免内存泄漏
            if (tickCounter % 3000 == 0) {
                cleanupTrackers();
            }
        }
    }

    private void accelerateArea() {
        BlockPos centerPos = this.getPos();

        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -HEIGHT_RANGE; y <= HEIGHT_RANGE; y++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    if (energy < ENERGY_PER_OPERATION) return; // 能量不足停止

                    BlockPos targetPos = centerPos.add(x, y, z);
                    if (targetPos.equals(centerPos)) continue;

                    if (accelerateBlock(targetPos)) {
                        energy -= ENERGY_PER_OPERATION;
                        operationsThisTick++;
                    }
                }
            }
        }
    }

    private boolean accelerateBlock(BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        TileEntity tileEntity = world.getTileEntity(pos);
        boolean operated = false;

        // 加速熔炉
        if (tileEntity instanceof TileEntityFurnace) {
            TileEntityFurnace furnace = (TileEntityFurnace) tileEntity;

            if (furnace.isBurning()) {
                // 获取当前熔炼状态
                ItemStack currentInput = furnace.getStackInSlot(0);
                ItemStack currentOutput = furnace.getStackInSlot(2);

                // 只有在有输入且可以熔炼时才加速
                if (!currentInput.isEmpty()) {
                    // 保存熔炼前的状态
                    int previousStackSize = currentInput.getCount();
                    int previousOutputSize = currentOutput.isEmpty() ? 0 : currentOutput.getCount();

                    // 调用多次update来加速熔炉
                    for (int i = 0; i < TICK_RATE; i++) {
                        // 直接调用熔炉的update方法，让它自己处理所有逻辑
                        furnace.update();

                        // 检查是否刚刚完成了一次熔炼
                        ItemStack newInput = furnace.getStackInSlot(0);
                        ItemStack newOutput = furnace.getStackInSlot(2);

                        // 通过检查输入减少和输出增加来判断熔炼是否完成
                        if (!newInput.isEmpty() && !ItemStack.areItemsEqual(currentInput, newInput)) {
                            // 输入物品类型改变了，停止加速
                            break;
                        }

                        if (newInput.isEmpty() || newInput.getCount() < previousStackSize) {
                            // 输入物品减少了，说明完成了一次熔炼
                            if (!newOutput.isEmpty() && newOutput.getCount() > previousOutputSize) {
                                // 输出增加了，确认完成了熔炼
                                tryApplyBonus(furnace, currentInput, pos);

                                // 更新追踪的数量
                                previousStackSize = newInput.isEmpty() ? 0 : newInput.getCount();
                                previousOutputSize = newOutput.getCount();
                                currentInput = newInput.isEmpty() ? ItemStack.EMPTY : newInput.copy();
                            }
                        }
                    }
                    operated = true;
                }
            } else {
                // 熔炉不在燃烧，移除跟踪器
                smeltTrackers.remove(pos);
            }
        }

        // 加速酿造台
        else if (tileEntity instanceof TileEntityBrewingStand) {
            TileEntityBrewingStand brewingStand = (TileEntityBrewingStand) tileEntity;
            if (brewingStand.getField(0) > 0) { // 如果正在酿造
                for (int i = 0; i < TICK_RATE; i++) {
                    brewingStand.update();
                }
                operated = true;
            }
        }

        // 加速作物生长
        else if (state.getBlock() instanceof IGrowable) {
            IGrowable growable = (IGrowable) state.getBlock();
            if (growable.canGrow(world, pos, state, world.isRemote)) {
                if (random.nextInt(100) < 30) { // 30%概率每次加速
                    growable.grow(world, random, pos, state);
                    operated = true;
                }
            }
        }

        // 加速树苗
        else if (state.getBlock() == Blocks.SAPLING) {
            if (random.nextInt(100) < 10) { // 10%概率
                state.getBlock().updateTick(world, pos, state, random);
                operated = true;
            }
        }

        return operated;
    }

    private void tryApplyBonus(TileEntityFurnace furnace, ItemStack originalInput, BlockPos pos) {
        // 获取或创建跟踪器
        SmeltTracker tracker = smeltTrackers.computeIfAbsent(pos, k -> new SmeltTracker());

        // 检查是否应该处理奖励
        if (!tracker.shouldProcessBonus(originalInput, world.getTotalWorldTime())) {
            return;
        }

        // 记录这次完成
        tracker.recordCompletion(world.getTotalWorldTime());

        // 概率检查
        if (random.nextDouble() >= DOUBLE_OUTPUT_CHANCE) {
            return;
        }

        // 获取熔炼结果
        ItemStack result = FurnaceRecipes.instance().getSmeltingResult(originalInput);
        if (result.isEmpty()) {
            return;
        }

        // 判断是否是矿物
        boolean isOre = originalInput.getTranslationKey().contains("ore") ||
                result.getTranslationKey().contains("ingot");

        int bonusAmount = 1; // 默认双倍（额外1个）
        if (isOre && random.nextDouble() < TRIPLE_OUTPUT_CHANCE) {
            bonusAmount = 2; // 三倍（额外2个）
        }

        // 尝试添加到输出槽
        ItemStack currentOutput = furnace.getStackInSlot(2);

        if (!currentOutput.isEmpty() &&
                ItemStack.areItemsEqual(currentOutput, result) &&
                ItemStack.areItemStackTagsEqual(currentOutput, result)) {

            // 计算可以添加的数量
            int maxStack = currentOutput.getMaxStackSize();
            int currentCount = currentOutput.getCount();
            int spaceAvailable = maxStack - currentCount;

            if (spaceAvailable > 0) {
                int toAdd = Math.min(bonusAmount, spaceAvailable);
                currentOutput.setCount(currentCount + toAdd);
                bonusAmount -= toAdd;

                // 标记熔炉需要更新
                furnace.markDirty();
            }
        }

        // 如果还有剩余的奖励物品，掉落在地上
        if (bonusAmount > 0) {
            ItemStack drop = result.copy();
            drop.setCount(bonusAmount);
            Block.spawnAsEntity(world, pos, drop);
        }
    }

    private void cleanupTrackers() {
        long currentTime = world.getTotalWorldTime();
        // 移除超过60秒（1200 ticks）没有活动的跟踪器
        smeltTrackers.entrySet().removeIf(entry ->
                currentTime - entry.getValue().lastCompletionTime > 1200
        );
    }

    public void toggleActive() {
        this.active = !this.active;
        markDirty();
    }

    public boolean isActive() {
        return active && energy >= BASE_CONSUMPTION;
    }

    public int getEnergyConsumption() {
        return active ? BASE_CONSUMPTION + (operationsThisTick * ENERGY_PER_OPERATION) : 0;
    }

    // IEnergyStorage 实现
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int energyReceived = Math.min(MAX_ENERGY - energy, Math.min(MAX_RECEIVE, maxReceive));
        if (!simulate) {
            energy += energyReceived;
            markDirty();
        }
        return energyReceived;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0; // 不允许提取能量
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return MAX_ENERGY;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    // Capability 处理
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(this);
        }
        return super.getCapability(capability, facing);
    }

    // NBT 存储
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("active", active);
        compound.setInteger("energy", energy);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.active = compound.getBoolean("active");
        this.energy = compound.getInteger("energy");
    }

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
}