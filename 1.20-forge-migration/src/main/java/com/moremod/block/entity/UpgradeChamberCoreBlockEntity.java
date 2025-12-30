package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

/**
 * 升級艙核心BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 存儲RF能量（可配置容量）
 * - 檢測多方塊結構
 * - 檢測玩家進入艙室
 * - 執行升級（消耗能量）
 * - 修復模式
 *
 * 1.12 -> 1.20 API变更:
 * - TileEntity -> BlockEntity
 * - ITickable.update() -> serverTick()
 * - EntityPlayer -> Player
 * - AxisAlignedBB -> AABB
 * - EnumParticleTypes -> ParticleTypes
 * - SoundEvents/SoundCategory -> SoundEvents/SoundSource
 */
public class UpgradeChamberCoreBlockEntity extends BaseEnergyBlockEntity {

    // 能量配置
    private static final int BASE_CAPACITY = 500000;
    private static final int BASE_REQUIRED = 50000;
    private static final int ENERGY_PER_LEVEL = 25000;
    private static final int UPGRADE_TICKS = 100;

    // 修復配置
    private static final float REPAIR_ENERGY_RATIO = 0.5f;
    private static final int REPAIR_TICKS = 60;

    // 升級進度
    private int upgradeProgress = 0;
    private boolean isUpgrading = false;
    private Player upgradingPlayer = null;
    private int tickCounter = 0;

    // 修復模式
    private boolean isRepairing = false;
    private int repairProgress = 0;

    // 冷卻時間
    private int cooldown = 0;

    // 多方块结构缓存
    private boolean structureValid = false;
    private int frameTier = 1;

    public UpgradeChamberCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UPGRADE_CHAMBER_CORE.get(), pos, state,
                BASE_CAPACITY, 10000, 0, 1);
    }

    @Override
    protected ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                syncToClient();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                // TODO: 检查是否为升级模组
                // return stack.getItem() instanceof UpgradeModuleItem;
                return true;
            }
        };
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        tickCounter++;

        // 冷卻計時
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // 每tick處理升級/修復進度
        if (isUpgrading) {
            processUpgrade();
        }
        if (isRepairing) {
            processRepair();
        }

        // 每10tick檢測一次結構和玩家
        if (tickCounter % 10 != 0) return;

        // 檢查結構
        checkMultiblockStructure();
        if (!structureValid) {
            if (isUpgrading) cancelUpgrade("結構被破壞！");
            if (isRepairing) cancelRepair("結構被破壞！");
            return;
        }

        // 檢測玩家
        Player playerInChamber = findPlayerInChamber();
        if (playerInChamber == null) {
            if (isUpgrading) cancelUpgrade("玩家離開升級艙！");
            if (isRepairing) cancelRepair("玩家離開升級艙！");
            return;
        }

        // TODO: 检查玩家机械核心并执行升级/修复逻辑
        // 需要移植Curios API集成来查找机械核心
    }

    /**
     * 检查多方块结构
     */
    private void checkMultiblockStructure() {
        // TODO: 实现MultiblockUpgradeChamber.checkStructure逻辑
        // 暂时返回true用于测试
        structureValid = true;
        frameTier = 1;
    }

    private Player findPlayerInChamber() {
        if (level == null) return null;

        BlockPos chamberCenter = getBlockPos().above();
        AABB chamberBox = new AABB(
                chamberCenter.getX() - 1, chamberCenter.getY(), chamberCenter.getZ() - 1,
                chamberCenter.getX() + 2, chamberCenter.getY() + 3, chamberCenter.getZ() + 2
        );

        List<Player> players = level.getEntitiesOfClass(Player.class, chamberBox);
        return players.isEmpty() ? null : players.get(0);
    }

    private void processUpgrade() {
        if (!isUpgrading || upgradingPlayer == null) return;

        upgradeProgress++;

        // 渲染粒子效果
        if (upgradeProgress % 5 == 0 && level instanceof ServerLevel serverLevel) {
            spawnUpgradeParticles(serverLevel);
        }

        // 播放進度音效
        if (upgradeProgress % 20 == 0) {
            level.playSound(null, getBlockPos(), SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS,
                    0.5F, 1.0F + (upgradeProgress / 100.0F));
        }

        // 檢查是否完成
        if (upgradeProgress >= UPGRADE_TICKS) {
            completeUpgrade();
        }
    }

    private void processRepair() {
        if (!isRepairing || upgradingPlayer == null) return;

        repairProgress++;

        // 渲染粒子效果
        if (repairProgress % 5 == 0 && level instanceof ServerLevel serverLevel) {
            spawnRepairParticles(serverLevel);
        }

        // 播放進度音效
        if (repairProgress % 15 == 0) {
            level.playSound(null, getBlockPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.3F, 1.2F);
        }

        // 檢查是否完成
        if (repairProgress >= REPAIR_TICKS) {
            completeRepair();
        }
    }

    private void completeUpgrade() {
        // TODO: 实现升级完成逻辑
        isUpgrading = false;
        upgradingPlayer = null;
        upgradeProgress = 0;
        cooldown = 40;
    }

    private void completeRepair() {
        // TODO: 实现修复完成逻辑
        isRepairing = false;
        upgradingPlayer = null;
        repairProgress = 0;
        cooldown = 40;
    }

    private void cancelUpgrade(String reason) {
        isUpgrading = false;
        upgradingPlayer = null;
        upgradeProgress = 0;
    }

    private void cancelRepair(String reason) {
        isRepairing = false;
        upgradingPlayer = null;
        repairProgress = 0;
    }

    private void spawnUpgradeParticles(ServerLevel serverLevel) {
        BlockPos center = getBlockPos().above();
        for (int i = 0; i < 10; i++) {
            double angle = (Math.PI * 2) * i / 10;
            double radius = 1.0;
            double x = center.getX() + 0.5 + Math.cos(angle) * radius;
            double z = center.getZ() + 0.5 + Math.sin(angle) * radius;
            double y = center.getY() + 0.5 + (upgradeProgress / (float)UPGRADE_TICKS) * 1.5;

            serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y, z, 1,
                    (center.getX() + 0.5 - x) * 0.1, 0.2, (center.getZ() + 0.5 - z) * 0.1, 0.1);
        }
    }

    private void spawnRepairParticles(ServerLevel serverLevel) {
        BlockPos center = getBlockPos().above();
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2) * i / 8;
            double radius = 0.8;
            double x = center.getX() + 0.5 + Math.cos(angle) * radius;
            double z = center.getZ() + 0.5 + Math.sin(angle) * radius;
            double y = center.getY() + 0.5 + (repairProgress / (float)REPAIR_TICKS) * 1.0;

            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0, 0.05, 0, 0);
        }
    }

    // ===== Getters =====

    public boolean isStructureValid() {
        return structureValid;
    }

    public int getFrameTier() {
        return frameTier;
    }

    public ItemStack getModuleStack() {
        return inventory.getStackInSlot(0);
    }

    public boolean isUpgrading() {
        return isUpgrading;
    }

    public boolean isRepairing() {
        return isRepairing;
    }

    public int getProgress() {
        return upgradeProgress;
    }

    public int getMaxProgress() {
        return UPGRADE_TICKS;
    }

    public int getRequiredEnergy() {
        // TODO: 根据模组等级计算
        return BASE_REQUIRED;
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Progress", upgradeProgress);
        tag.putBoolean("IsUpgrading", isUpgrading);
        tag.putInt("Cooldown", cooldown);
        tag.putBoolean("IsRepairing", isRepairing);
        tag.putInt("RepairProgress", repairProgress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        upgradeProgress = tag.getInt("Progress");
        isUpgrading = tag.getBoolean("IsUpgrading");
        cooldown = tag.getInt("Cooldown");
        isRepairing = tag.getBoolean("IsRepairing");
        repairProgress = tag.getInt("RepairProgress");
    }
}
