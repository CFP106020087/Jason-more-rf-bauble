package com.moremod.upgrades.platform;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 模块运行上下文
 *
 * 功能：
 * - 提供模块运行时需要的所有信息
 * - 封装玩家、核心物品、能量系统访问
 * - 提供便捷方法访问常用功能
 *
 * 设计：
 * - 不可变对象（创建后不能修改）
 * - 所有方法都是 Null-Safe 的
 */
public class ModuleContext {

    private final EntityPlayer player;
    private final ItemStack coreStack;
    private final ModuleState moduleState;
    private final World world;
    private final boolean isClientSide;

    public ModuleContext(@Nonnull EntityPlayer player,
                        @Nonnull ItemStack coreStack,
                        @Nonnull ModuleState moduleState) {
        this.player = player;
        this.coreStack = coreStack;
        this.moduleState = moduleState;
        this.world = player.world;
        this.isClientSide = world.isRemote;
    }

    // ===== 基础信息访问 =====

    @Nonnull
    public EntityPlayer getPlayer() {
        return player;
    }

    @Nonnull
    public ItemStack getCoreStack() {
        return coreStack;
    }

    @Nonnull
    public ModuleState getModuleState() {
        return moduleState;
    }

    @Nonnull
    public World getWorld() {
        return world;
    }

    public boolean isClientSide() {
        return isClientSide;
    }

    public boolean isServerSide() {
        return !isClientSide;
    }

    // ===== 模块状态快捷访问 =====

    public String getModuleId() {
        return moduleState.getModuleId();
    }

    public int getLevel() {
        return moduleState.getLevel();
    }

    public int getEffectiveLevel() {
        return moduleState.getEffectiveLevel();
    }

    public boolean isActive() {
        return moduleState.isActive();
    }

    public boolean isPaused() {
        return moduleState.isPaused();
    }

    public boolean isDisabled() {
        return moduleState.isDisabled();
    }

    // ===== 能量系统访问 =====

    /**
     * 获取核心的能量存储
     */
    @Nullable
    public IEnergyStorage getEnergyStorage() {
        if (coreStack.isEmpty()) {
            return null;
        }
        return coreStack.getCapability(CapabilityEnergy.ENERGY, null);
    }

    /**
     * 获取当前能量值
     */
    public int getEnergyStored() {
        IEnergyStorage storage = getEnergyStorage();
        return storage != null ? storage.getEnergyStored() : 0;
    }

    /**
     * 获取最大能量值
     */
    public int getMaxEnergyStored() {
        IEnergyStorage storage = getEnergyStorage();
        return storage != null ? storage.getMaxEnergyStored() : 0;
    }

    /**
     * 获取能量百分比 (0.0 - 1.0)
     */
    public float getEnergyPercentage() {
        int max = getMaxEnergyStored();
        if (max <= 0) {
            return 0.0f;
        }
        return (float) getEnergyStored() / max;
    }

    /**
     * 获取能量耗尽状态
     */
    @Nonnull
    public EnergyDepletionManager.EnergyStatus getEnergyStatus() {
        return EnergyDepletionManager.getCurrentEnergyStatus(coreStack);
    }

    /**
     * 消耗能量
     */
    public boolean consumeEnergy(int amount) {
        return ItemMechanicalCore.consumeEnergy(coreStack, amount);
    }

    /**
     * 消耗能量（带平衡计算）
     */
    public boolean consumeEnergyBalanced(String upgradeId, int baseAmount) {
        return ItemMechanicalCore.consumeEnergyForUpgradeBalanced(coreStack, upgradeId, baseAmount);
    }

    /**
     * 添加能量
     */
    public void addEnergy(int amount) {
        ItemMechanicalCore.addEnergy(coreStack, amount);
    }

    // ===== 时间相关 =====

    /**
     * 获取世界总时间（tick）
     */
    public long getWorldTime() {
        return world.getTotalWorldTime();
    }

    /**
     * 检查是否在冷却中
     */
    public boolean isOnCooldown() {
        return moduleState.isOnCooldown(getWorldTime());
    }

    /**
     * 设置冷却时间（tick）
     */
    public void setCooldown(long durationTicks) {
        moduleState.setCooldown(getWorldTime(), durationTicks);
    }

    /**
     * 清除冷却
     */
    public void clearCooldown() {
        moduleState.clearCooldown();
    }

    /**
     * 获取剩余冷却时间（tick）
     */
    public long getRemainingCooldown() {
        return moduleState.getRemainingCooldown(getWorldTime());
    }

    // ===== 玩家状态快捷访问 =====

    public boolean isPlayerSneaking() {
        return player.isSneaking();
    }

    public boolean isPlayerSprinting() {
        return player.isSprinting();
    }

    public boolean isPlayerFlying() {
        return player.capabilities.isFlying;
    }

    public boolean isPlayerOnGround() {
        return player.onGround;
    }

    public boolean isPlayerInWater() {
        return player.isInWater();
    }

    public boolean isPlayerInLava() {
        return player.isInLava();
    }

    public float getPlayerHealth() {
        return player.getHealth();
    }

    public float getPlayerMaxHealth() {
        return player.getMaxHealth();
    }

    public int getPlayerFoodLevel() {
        return player.getFoodStats().getFoodLevel();
    }

    // ===== 自定义数据存储 =====

    public void setCustomInt(String key, int value) {
        moduleState.setCustomInt(key, value);
    }

    public int getCustomInt(String key, int defaultValue) {
        return moduleState.getCustomInt(key, defaultValue);
    }

    public void setCustomLong(String key, long value) {
        moduleState.setCustomLong(key, value);
    }

    public long getCustomLong(String key, long defaultValue) {
        return moduleState.getCustomLong(key, defaultValue);
    }

    public void setCustomBoolean(String key, boolean value) {
        moduleState.setCustomBoolean(key, value);
    }

    public boolean getCustomBoolean(String key, boolean defaultValue) {
        return moduleState.getCustomBoolean(key, defaultValue);
    }

    public void setCustomString(String key, String value) {
        moduleState.setCustomString(key, value);
    }

    public String getCustomString(String key, String defaultValue) {
        return moduleState.getCustomString(key, defaultValue);
    }

    // ===== 工具方法 =====

    /**
     * 创建子上下文（用于其他模块）
     */
    public ModuleContext createSubContext(ModuleState otherState) {
        return new ModuleContext(player, coreStack, otherState);
    }

    @Override
    public String toString() {
        return String.format("ModuleContext{module=%s, level=%d, player=%s, energy=%d/%d}",
                getModuleId(), getLevel(), player.getName(), getEnergyStored(), getMaxEnergyStored());
    }
}
