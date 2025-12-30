package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Set;

/**
 * 石油发电机BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 燃烧石油或植物油发电
 * - 支持增速插件（最多4个）
 * - 输出RF能量到相邻机器
 */
public class OilGeneratorBlockEntity extends BaseEnergyBlockEntity {

    private static final int MAX_ENERGY = 500000;
    private static final int BASE_RF_PER_TICK = 80;
    private static final int MAX_EXTRACT = 2000;
    private static final int FUEL_BURN_TIME = 12000; // 10分钟

    // 有效的增速插件
    private static final Set<net.minecraft.world.item.Item> VALID_UPGRADES = Set.of(
            Items.REDSTONE,
            Items.GLOWSTONE_DUST,
            Items.BLAZE_POWDER,
            Items.EMERALD
    );

    // 燃烧状态
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private boolean burning = false;

    public OilGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OIL_GENERATOR.get(), pos, state,
                MAX_ENERGY, 0, MAX_EXTRACT, 5); // 1个燃料槽 + 4个增速槽
    }

    @Override
    protected ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == 0) {
                    return isValidFuel(stack);
                } else {
                    return isValidUpgrade(stack);
                }
            }
        };
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        boolean wasGenerating = burning;

        // 如果正在燃烧
        if (burnTime > 0) {
            burnTime--;
            burning = true;

            // 产生能量
            int rfPerTick = getRFPerTick();
            if (getEnergyStored() < getMaxEnergyStored()) {
                int toAdd = Math.min(rfPerTick, getMaxEnergyStored() - getEnergyStored());
                energy.setEnergy(getEnergyStored() + toAdd);
            }
        } else {
            burning = false;

            // 尝试消耗新燃料
            if (getEnergyStored() < getMaxEnergyStored()) {
                tryConsumeFuel();
            }
        }

        // 输出能量到相邻方块
        if (getEnergyStored() > 0) {
            outputEnergy();
        }

        // 状态变化时更新
        if (wasGenerating != burning) {
            setChanged();
            syncToClient();
        }
    }

    private void tryConsumeFuel() {
        ItemStack fuelStack = inventory.getStackInSlot(0);
        if (!fuelStack.isEmpty() && isValidFuel(fuelStack)) {
            maxBurnTime = FUEL_BURN_TIME;
            burnTime = maxBurnTime;
            burning = true;
            fuelStack.shrink(1);
            setChanged();
        }
    }

    private void outputEnergy() {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = getBlockPos().relative(direction);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(neighborStorage -> {
                    if (neighborStorage.canReceive()) {
                        int toTransfer = Math.min(MAX_EXTRACT, getEnergyStored());
                        int transferred = neighborStorage.receiveEnergy(toTransfer, false);
                        if (transferred > 0) {
                            extractEnergy(transferred, false);
                        }
                    }
                });
            }
        }
    }

    public static boolean isValidFuel(ItemStack stack) {
        // TODO: 检查是否为原油桶或植物油桶
        // 这里暂时接受岩浆桶作为测试
        return stack.is(Items.LAVA_BUCKET);
    }

    public static boolean isValidUpgrade(ItemStack stack) {
        return VALID_UPGRADES.contains(stack.getItem());
    }

    public int getRFPerTick() {
        int upgradeCount = 0;
        for (int i = 1; i <= 4; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                upgradeCount++;
            }
        }
        // 每个增速插件增加50%
        return BASE_RF_PER_TICK + (BASE_RF_PER_TICK * upgradeCount / 2);
    }

    public boolean isBurning() {
        return burning;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
        tag.putBoolean("Burning", burning);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        burnTime = tag.getInt("BurnTime");
        maxBurnTime = tag.getInt("MaxBurnTime");
        burning = tag.getBoolean("Burning");
    }
}
