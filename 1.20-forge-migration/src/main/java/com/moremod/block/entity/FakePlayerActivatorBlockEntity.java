package com.moremod.block.entity;

import com.moremod.block.FakePlayerActivatorBlock;
import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

/**
 * 假玩家激活器BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 使用假玩家核心模拟玩家操作
 * - 自动右键点击前方方块
 * - 自动使用物品
 * - 消耗RF能量
 */
public class FakePlayerActivatorBlockEntity extends BlockEntity {

    private static final int ENERGY_CAPACITY = 50000;
    private static final int ENERGY_PER_USE = 100;
    private static final int TICK_INTERVAL = 20; // 每秒触发一次
    private static final int SLOT_COUNT = 9;

    private WeakReference<FakePlayer> fakePlayerRef = new WeakReference<>(null);

    // 能量存储
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, 2000, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }
    };
    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> energy);

    // 物品槽
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    // 状态
    private int tickCounter = 0;
    private int currentSlot = 0;
    private boolean isActive = false;

    public FakePlayerActivatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FAKE_PLAYER_ACTIVATOR.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        tickCounter++;
        if (tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;

        // 检查是否有能量和物品
        boolean hasEnergy = energy.getEnergyStored() >= ENERGY_PER_USE;
        boolean hasItems = !getCurrentItem().isEmpty();

        if (hasEnergy && hasItems) {
            if (!isActive) {
                isActive = true;
                FakePlayerActivatorBlock.setActiveState(level, worldPosition, true);
            }

            performAction();
            energy.extractEnergy(ENERGY_PER_USE, false);
            setChanged();
        } else {
            if (isActive) {
                isActive = false;
                FakePlayerActivatorBlock.setActiveState(level, worldPosition, false);
            }
        }
    }

    private ItemStack getCurrentItem() {
        // 找到第一个非空槽
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                currentSlot = i;
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private void performAction() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Direction facing = getBlockState().getValue(FakePlayerActivatorBlock.FACING);
        BlockPos targetPos = worldPosition.relative(facing);

        FakePlayer fakePlayer = getOrCreateFakePlayer(serverLevel);
        if (fakePlayer == null) return;

        ItemStack item = inventory.getStackInSlot(currentSlot).copy();
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, item);

        // 模拟右键点击
        BlockHitResult hitResult = new BlockHitResult(
                Vec3.atCenterOf(targetPos),
                facing.getOpposite(),
                targetPos,
                false
        );

        BlockState targetState = level.getBlockState(targetPos);

        // 尝试对方块使用物品
        fakePlayer.gameMode.useItemOn(fakePlayer, serverLevel, item, InteractionHand.MAIN_HAND, hitResult);

        // 更新物品状态
        inventory.setStackInSlot(currentSlot, fakePlayer.getItemInHand(InteractionHand.MAIN_HAND));
    }

    private FakePlayer getOrCreateFakePlayer(ServerLevel level) {
        FakePlayer player = fakePlayerRef.get();
        if (player == null || player.level() != level) {
            player = FakePlayerFactory.getMinecraft(level);
            player.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
            fakePlayerRef = new WeakReference<>(player);
        }
        return player;
    }

    public IItemHandler getItemHandler() {
        return inventory;
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public boolean isActive() {
        return isActive;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyHandler.invalidate();
        itemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("CurrentSlot", currentSlot);
        tag.putBoolean("IsActive", isActive);
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int fe = tag.getInt("Energy");
        energy.receiveEnergy(fe, false);
        currentSlot = tag.getInt("CurrentSlot");
        isActive = tag.getBoolean("IsActive");
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
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
