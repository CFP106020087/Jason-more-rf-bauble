package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 充电站BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 为范围内玩家的饰品充能
 * - RF能量存储
 * - 可配置充电范围
 */
public class ChargingStationBlockEntity extends BaseEnergyBlockEntity implements MenuProvider {

    private static final int CAPACITY = 500000;
    private static final int DEFAULT_RANGE = 8;
    private static final int CHARGE_RATE = 1000; // RF per tick per player

    private int chargeRange = DEFAULT_RANGE;
    private int tickCounter = 0;

    public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_STATION.get(), pos, state,
                CAPACITY, 20000, 0, 0);
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        tickCounter++;

        // 每5tick充电一次
        if (tickCounter % 5 != 0) return;

        // 如果没有能量，跳过
        if (getEnergyStored() <= 0) return;

        // 获取范围内的玩家
        AABB area = new AABB(getBlockPos()).inflate(chargeRange);
        List<Player> players = level.getEntitiesOfClass(Player.class, area);

        for (Player player : players) {
            chargePlayerItems(player);
        }
    }

    private void chargePlayerItems(Player player) {
        // TODO: 使用Curios API查找玩家饰品并充能
        // 需要集成Curios API后实现

        // 示例：查找主手物品并充能
        // ItemStack mainHand = player.getMainHandItem();
        // mainHand.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {
        //     int transferred = Math.min(CHARGE_RATE, getEnergyStored());
        //     transferred = energy.receiveEnergy(transferred, false);
        //     extractEnergy(transferred, false);
        // });
    }

    public int getChargeRange() {
        return chargeRange;
    }

    public void setChargeRange(int range) {
        this.chargeRange = Math.max(1, Math.min(16, range));
        setChanged();
    }

    public void openMenu(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, this, getBlockPos());
        }
    }

    // ===== MenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.moremod.charging_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // TODO: 返回ChargingStationMenu实例
        return null;
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ChargeRange", chargeRange);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        chargeRange = tag.getInt("ChargeRange");
        if (chargeRange <= 0) chargeRange = DEFAULT_RANGE;
    }
}
