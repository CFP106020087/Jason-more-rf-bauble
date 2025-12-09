package com.moremod.tile;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.upgrades.ItemUpgradeComponent;
import com.moremod.multiblock.MultiblockUpgradeChamber;
import com.moremod.util.UpgradeKeys;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * 升級艙核心TileEntity
 *
 * 功能：
 * - 存儲RF能量（可配置容量）
 * - 檢測多方塊結構
 * - 檢測玩家進入艙室
 * - 執行升級（消耗能量）
 * - 升級失敗機制（能量不足）
 */
public class TileEntityUpgradeChamberCore extends TileEntity implements ITickable {

    // 能量配置
    private static final int BASE_CAPACITY = 500000;      // 基礎容量 500k RF
    private static final int BASE_REQUIRED = 50000;       // 基礎所需能量 50k RF
    private static final int ENERGY_PER_LEVEL = 25000;    // 每級額外能量 25k RF
    private static final int UPGRADE_TICKS = 100;         // 升級時間 5秒 (100 ticks)

    // 修復配置
    private static final float REPAIR_ENERGY_RATIO = 0.5f;  // 修復消耗50%當前能量
    private static final int REPAIR_TICKS = 60;             // 修復時間 3秒 (60 ticks)

    // 能量存儲（可接收但不可輸出）
    private final EnergyStorage energy = new EnergyStorage(BASE_CAPACITY, 10000, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                markDirty();
            }
            return received;
        }
    };

    // 物品槽位（只接受升級模組）
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            syncToClient();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof ItemUpgradeComponent;
        }
    };

    // 升級進度
    private int upgradeProgress = 0;
    private boolean isUpgrading = false;
    private EntityPlayer upgradingPlayer = null;
    private int tickCounter = 0;

    // 修復模式
    private boolean isRepairing = false;
    private int repairProgress = 0;

    // 冷卻時間（防止連續升級）
    private int cooldown = 0;

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        tickCounter++;

        // 冷卻計時
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // 每10tick檢測一次（提高性能）
        if (tickCounter % 10 != 0) {
            if (isUpgrading) {
                // 升級過程中繼續處理
                processUpgrade();
            }
            if (isRepairing) {
                // 修復過程中繼續處理
                processRepair();
            }
            return;
        }

        // 檢查結構
        if (!MultiblockUpgradeChamber.checkStructure(world, pos)) {
            if (isUpgrading) {
                cancelUpgrade("結構被破壞！");
            }
            if (isRepairing) {
                cancelRepair("結構被破壞！");
            }
            return;
        }

        // 檢測玩家
        EntityPlayer playerInChamber = findPlayerInChamber();
        if (playerInChamber == null) {
            if (isUpgrading) {
                cancelUpgrade("玩家離開升級艙！");
            }
            if (isRepairing) {
                cancelRepair("玩家離開升級艙！");
            }
            return;
        }

        // 檢查玩家是否有機械核心
        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(playerInChamber);
        if (!ItemMechanicalCore.isMechanicalCore(coreStack)) {
            if (isUpgrading) {
                cancelUpgrade("未檢測到機械核心！");
            }
            if (isRepairing) {
                cancelRepair("未檢測到機械核心！");
            }
            if (tickCounter % 40 == 0) {
                playerInChamber.sendMessage(new TextComponentString(
                        TextFormatting.RED + "請裝備機械核心到頭部飾品欄！"
                ));
            }
            return;
        }

        // 檢查是否有模組
        ItemStack module = inventory.getStackInSlot(0);

        if (module.isEmpty()) {
            // 無模組時 -> 修復模式
            if (isUpgrading) {
                cancelUpgrade("模組被移除！");
            }

            // 檢查是否有可修復的模組
            if (!isRepairing && hasDamagedModules(coreStack)) {
                startRepair(playerInChamber, coreStack);
            } else if (isRepairing && upgradingPlayer != playerInChamber) {
                cancelRepair("修復中的玩家已改變！");
            }
        } else {
            // 有模組時 -> 升級模式
            if (isRepairing) {
                cancelRepair("已放入升級模組，切換到升級模式！");
            }

            if (!isUpgrading) {
                startUpgrade(playerInChamber, module, coreStack);
            } else if (upgradingPlayer != playerInChamber) {
                cancelUpgrade("升級中的玩家已改變！");
            }
        }
    }

    private void startUpgrade(EntityPlayer player, ItemStack module, ItemStack coreStack) {
        if (!(module.getItem() instanceof ItemUpgradeComponent)) return;

        ItemUpgradeComponent upgradeItem = (ItemUpgradeComponent) module.getItem();
        int requiredEnergy = getRequiredEnergy(upgradeItem);

        // 檢查能量
        if (energy.getEnergyStored() < requiredEnergy) {
            if (tickCounter % 40 == 0) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "能量不足！需要 " + requiredEnergy + " RF，當前 " + energy.getEnergyStored() + " RF"
                ));
            }
            return;
        }

        // 驗證升級是否可行
        String validationResult = validateUpgrade(coreStack, upgradeItem);
        if (validationResult != null) {
            if (tickCounter % 40 == 0) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + validationResult));
            }
            return;
        }

        // 開始升級
        isUpgrading = true;
        upgradingPlayer = player;
        upgradeProgress = 0;

        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "⚡ 升級開始！請保持在升級艙內..."
        ));

        // 播放開始音效
        world.playSound(null, pos, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    private void processUpgrade() {
        if (!isUpgrading || upgradingPlayer == null) return;

        upgradeProgress++;

        // 渲染粒子效果
        if (upgradeProgress % 5 == 0) {
            spawnUpgradeParticles();
        }

        // 播放進度音效
        if (upgradeProgress % 20 == 0) {
            world.playSound(null, pos, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.BLOCKS, 0.5F, 1.0F + (upgradeProgress / 100.0F));
        }

        // 檢查是否完成
        if (upgradeProgress >= UPGRADE_TICKS) {
            completeUpgrade();
        }
    }

    private void completeUpgrade() {
        if (upgradingPlayer == null) return;

        ItemStack module = inventory.getStackInSlot(0);
        if (module.isEmpty() || !(module.getItem() instanceof ItemUpgradeComponent)) {
            cancelUpgrade("模組無效！");
            return;
        }

        ItemUpgradeComponent upgradeItem = (ItemUpgradeComponent) module.getItem();
        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(upgradingPlayer);

        if (!ItemMechanicalCore.isMechanicalCore(coreStack)) {
            cancelUpgrade("機械核心無效！");
            return;
        }

        int requiredEnergy = getRequiredEnergy(upgradeItem);

        // 最終能量檢查
        if (energy.getEnergyStored() < requiredEnergy) {
            failUpgrade("能量不足！升級失敗！");
            return;
        }

        // 消耗能量
        energy.extractEnergy(requiredEnergy, false);

        // 執行升級
        boolean success = performUpgrade(upgradingPlayer, coreStack, upgradeItem);

        if (success) {
            // 消耗模組
            module.shrink(1);
            inventory.setStackInSlot(0, module);

            // 成功效果
            world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
            spawnSuccessParticles();

            upgradingPlayer.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 升級成功！"
            ));

            // 同步核心
            forceSyncCore(upgradingPlayer);
        } else {
            failUpgrade("升級過程中發生錯誤！");
        }

        // 重置狀態
        isUpgrading = false;
        upgradingPlayer = null;
        upgradeProgress = 0;
        cooldown = 40; // 2秒冷卻
    }

    private void failUpgrade(String reason) {
        if (upgradingPlayer != null) {
            upgradingPlayer.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ " + reason
            ));

            // 失敗效果
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 1.0F, 0.5F);
            spawnFailParticles();
        }

        isUpgrading = false;
        upgradingPlayer = null;
        upgradeProgress = 0;
        cooldown = 60; // 3秒冷卻
    }

    private void cancelUpgrade(String reason) {
        if (upgradingPlayer != null) {
            upgradingPlayer.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚠ 升級中斷: " + reason
            ));
        }

        isUpgrading = false;
        upgradingPlayer = null;
        upgradeProgress = 0;
    }

    // ===== 修復系統 =====

    /**
     * 檢查機械核心是否有損壞的模組
     */
    private boolean hasDamagedModules(ItemStack coreStack) {
        NBTTagCompound nbt = coreStack.getTagCompound();
        if (nbt == null) return false;

        for (String key : nbt.getKeySet()) {
            if (key.startsWith("OriginalMax_")) {
                String moduleId = key.substring("OriginalMax_".length());
                int originalMax = nbt.getInteger(key);
                int ownedMax = getOwnedMaxValue(nbt, moduleId);

                if (originalMax > 0 && ownedMax < originalMax) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getOwnedMaxValue(NBTTagCompound nbt, String moduleId) {
        int max = 0;
        max = Math.max(max, nbt.getInteger("OwnedMax_" + moduleId));
        max = Math.max(max, nbt.getInteger("OwnedMax_" + moduleId.toUpperCase()));
        max = Math.max(max, nbt.getInteger("OwnedMax_" + moduleId.toLowerCase()));
        return max;
    }

    /**
     * 開始修復
     */
    private void startRepair(EntityPlayer player, ItemStack coreStack) {
        // 計算修復所需能量（50%當前能量）
        int repairEnergy = (int)(energy.getEnergyStored() * REPAIR_ENERGY_RATIO);

        if (repairEnergy < 10000) {
            if (tickCounter % 40 == 0) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "能量不足！需要至少 20,000 RF 進行修復"
                ));
            }
            return;
        }

        // 開始修復
        isRepairing = true;
        upgradingPlayer = player;
        repairProgress = 0;

        player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "⚡ 開始修復損壞的模組... (消耗 " + repairEnergy + " RF)"
        ));

        // 播放開始音效
        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    /**
     * 處理修復進度
     */
    private void processRepair() {
        if (!isRepairing || upgradingPlayer == null) return;

        repairProgress++;

        // 渲染粒子效果
        if (repairProgress % 5 == 0) {
            spawnRepairParticles();
        }

        // 播放進度音效
        if (repairProgress % 15 == 0) {
            world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.3F, 1.2F);
        }

        // 檢查是否完成
        if (repairProgress >= REPAIR_TICKS) {
            completeRepair();
        }
    }

    /**
     * 完成修復
     */
    private void completeRepair() {
        if (upgradingPlayer == null) return;

        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(upgradingPlayer);
        if (!ItemMechanicalCore.isMechanicalCore(coreStack)) {
            cancelRepair("機械核心無效！");
            return;
        }

        // 計算並消耗能量（50%）
        int repairEnergy = (int)(energy.getEnergyStored() * REPAIR_ENERGY_RATIO);
        if (repairEnergy < 10000) {
            cancelRepair("能量不足！");
            return;
        }

        energy.extractEnergy(repairEnergy, false);

        // 執行修復
        int repairedCount = performRepair(coreStack);

        if (repairedCount > 0) {
            // 成功效果
            world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.0F, 1.5F);
            spawnSuccessParticles();

            upgradingPlayer.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 修復完成！已修復 " + repairedCount + " 個損壞的模組"
            ));

            // 同步核心
            forceSyncCore(upgradingPlayer);
        } else {
            upgradingPlayer.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "沒有需要修復的模組"
            ));
        }

        // 重置狀態
        isRepairing = false;
        upgradingPlayer = null;
        repairProgress = 0;
        cooldown = 40; // 2秒冷卻
    }

    /**
     * 執行修復邏輯 - 將所有損壞模組恢復到原始等級
     */
    private int performRepair(ItemStack coreStack) {
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(coreStack);
        int repairedCount = 0;

        // 找出所有損壞的模組
        java.util.Set<String> processed = new java.util.HashSet<>();

        for (String key : new java.util.ArrayList<>(nbt.getKeySet())) {
            if (!key.startsWith("OriginalMax_")) continue;

            String moduleId = key.substring("OriginalMax_".length());

            // 避免重複處理（大小寫變體）
            String normalizedId = moduleId.toUpperCase();
            if (processed.contains(normalizedId)) continue;
            processed.add(normalizedId);

            int originalMax = nbt.getInteger(key);
            int ownedMax = getOwnedMaxValue(nbt, moduleId);

            // 如果有損壞（ownedMax < originalMax）
            if (originalMax > 0 && ownedMax < originalMax) {
                // 恢復到原始等級
                setOwnedMaxAll(nbt, moduleId, originalMax);

                // 同時恢復當前等級
                setLevelAll(coreStack, moduleId, originalMax);

                // 清除損壞標記
                nbt.removeTag("WasPunished_" + moduleId);
                nbt.removeTag("WasPunished_" + moduleId.toUpperCase());
                nbt.removeTag("WasPunished_" + moduleId.toLowerCase());

                // 重置損壞計數
                nbt.setInteger("DamageCount_" + moduleId, 0);
                nbt.setInteger("DamageCount_" + moduleId.toUpperCase(), 0);
                nbt.setInteger("DamageCount_" + moduleId.toLowerCase(), 0);

                repairedCount++;

                // 通知玩家
                String displayName = getDisplayNameForModule(moduleId);
                upgradingPlayer.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "  ✓ " + displayName + " 已修復至 Lv." + originalMax
                ));
            }
        }

        return repairedCount;
    }

    private void setOwnedMaxAll(NBTTagCompound nbt, String moduleId, int value) {
        nbt.setInteger("OwnedMax_" + moduleId, value);
        nbt.setInteger("OwnedMax_" + moduleId.toUpperCase(), value);
        nbt.setInteger("OwnedMax_" + moduleId.toLowerCase(), value);
    }

    private void setLevelAll(ItemStack coreStack, String moduleId, int value) {
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(coreStack);
        nbt.setInteger("upgrade_" + moduleId, value);
        nbt.setInteger("upgrade_" + moduleId.toUpperCase(), value);
        nbt.setInteger("upgrade_" + moduleId.toLowerCase(), value);

        try {
            ItemMechanicalCoreExtended.setUpgradeLevel(coreStack, moduleId, value);
        } catch (Throwable ignored) {}

        try {
            for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
                if (t.getKey().equalsIgnoreCase(moduleId) || t.name().equalsIgnoreCase(moduleId)) {
                    ItemMechanicalCore.setUpgradeLevel(coreStack, t, value);
                    break;
                }
            }
        } catch (Throwable ignored) {}

        try {
            UpgradeKeys.setLevel(coreStack, moduleId, value);
            UpgradeKeys.markOwnedActive(coreStack, moduleId, value);
        } catch (Throwable ignored) {}
    }

    private String getDisplayNameForModule(String moduleId) {
        try {
            ItemMechanicalCoreExtended.UpgradeInfo info = ItemMechanicalCoreExtended.getUpgradeInfo(moduleId);
            if (info != null && info.displayName != null) return info.displayName;
        } catch (Throwable ignored) {}

        try {
            for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
                if (t.getKey().equalsIgnoreCase(moduleId) || t.name().equalsIgnoreCase(moduleId)) {
                    return t.getDisplayName();
                }
            }
        } catch (Throwable ignored) {}

        return moduleId.replace("_", " ");
    }

    private void cancelRepair(String reason) {
        if (upgradingPlayer != null) {
            upgradingPlayer.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚠ 修復中斷: " + reason
            ));
        }

        isRepairing = false;
        upgradingPlayer = null;
        repairProgress = 0;
    }

    private void spawnRepairParticles() {
        BlockPos center = pos.up();
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2) * i / 8;
            double radius = 0.8;
            double x = center.getX() + 0.5 + Math.cos(angle) * radius;
            double z = center.getZ() + 0.5 + Math.sin(angle) * radius;
            double y = center.getY() + 0.5 + (repairProgress / (float)REPAIR_TICKS) * 1.0;

            world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                    x, y, z, 0, 0.05, 0);
            world.spawnParticle(EnumParticleTypes.REDSTONE,
                    x, y, z, 0.2, 0.8, 1.0); // 藍色
        }
    }

    @Nullable
    private EntityPlayer findPlayerInChamber() {
        // 玩家空間位置（第1-2層，共2格高）
        // 結構是3x3x4，中間有十字形空氣區域，需要覆蓋整個2層高空間
        BlockPos chamberCenter = pos.up();
        AxisAlignedBB chamberBox = new AxisAlignedBB(
                chamberCenter.getX() - 1, chamberCenter.getY(), chamberCenter.getZ() - 1,
                chamberCenter.getX() + 2, chamberCenter.getY() + 3, chamberCenter.getZ() + 2
        );

        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, chamberBox);
        return players.isEmpty() ? null : players.get(0);
    }

    private String validateUpgrade(ItemStack coreStack, ItemUpgradeComponent upgradeItem) {
        String rawId = upgradeItem.getUpgradeType();
        String cid = UpgradeKeys.foldAlias(rawId);
        String registryName = upgradeItem.getRegistryName() != null ?
                upgradeItem.getRegistryName().toString() : "";

        int moduleLevel = getModuleLevel(upgradeItem, registryName);

        // 飛行模組特殊處理
        if (registryName.contains("flight_module")) {
            int current = getFlightLevel(coreStack);
            int target = moduleLevel;
            if (target <= current) {
                return "已安裝相同或更高級的飛行模組！";
            }
            if (target > current + 1 && current > 0) {
                return "需要先安裝更低級的飛行模組！";
            }
            return null;
        }

        // 防水模組特殊處理
        if (registryName.contains("waterproof_module") || UpgradeKeys.isWaterproof(cid)) {
            int current = getWaterproofLevel(coreStack);
            int target = moduleLevel;
            if (target <= current) {
                return "已安裝相同或更高級的防水模組！";
            }
            if (target > current + 1 && current > 0) {
                return "需要先安裝更低級的防水模組！";
            }
            return null;
        }

        // 套裝包特殊處理
        if (rawId.contains("PACKAGE") || registryName.contains("_package")) {
            return validatePackage(coreStack, rawId, registryName);
        }

        // 普通升級
        int currentLevel = lvOf(coreStack, cid);
        int maxLevel = maxOf(coreStack, cid);

        if (currentLevel >= maxLevel) {
            return getDisplayName(cid) + " 已達到最大等級！";
        }

        int requiredLevel = currentLevel + 1;
        if (moduleLevel != requiredLevel) {
            return String.format("升級到 Lv.%d 需要 %d 級模組，當前模組為 %d 級！",
                    requiredLevel, requiredLevel, moduleLevel);
        }

        return null;
    }

    private String validatePackage(ItemStack core, String rawType, String registryName) {
        boolean isSurvival = rawType.equalsIgnoreCase("SURVIVAL_PACKAGE") ||
                registryName.contains("survival_enhancement_package");
        boolean isCombat = rawType.equalsIgnoreCase("COMBAT_PACKAGE") ||
                registryName.contains("combat_enhancement_package");
        boolean isOmni = rawType.equalsIgnoreCase("OMNIPOTENT_PACKAGE") ||
                registryName.contains("omnipotent_package");

        if (!isSurvival && !isCombat && !isOmni) {
            return "未知的套裝類型！";
        }

        String[] targetList = isSurvival ? new String[]{"YELLOW_SHIELD", "HEALTH_REGEN", "HUNGER_THIRST"} :
                (isCombat ? new String[]{"DAMAGE_BOOST", "ATTACK_SPEED", "RANGE_EXTENSION"} :
                        new String[]{"ENERGY_CAPACITY", "ENERGY_EFFICIENCY", "ARMOR_ENHANCEMENT"});

        for (String u : targetList) {
            int cur = lvOf(core, u);
            int max = maxOf(core, u);
            if (cur >= max) {
                return getDisplayName(u) + " 已達最大等級，無法應用套裝！";
            }
        }

        return null;
    }

    private boolean performUpgrade(EntityPlayer player, ItemStack coreStack, ItemUpgradeComponent upgradeItem) {
        String rawId = upgradeItem.getUpgradeType();
        String cid = UpgradeKeys.foldAlias(rawId);
        String registryName = upgradeItem.getRegistryName() != null ? upgradeItem.getRegistryName().toString() : "";

        int moduleLevel = getModuleLevel(upgradeItem, registryName);

        // 飛行模組
        if (registryName.contains("flight_module")) {
            return handleFlightModule(player, coreStack, moduleLevel);
        }

        // 防水模組
        if (registryName.contains("waterproof_module") || UpgradeKeys.isWaterproof(cid)) {
            return handleWaterproofModule(player, coreStack, moduleLevel);
        }

        // 套裝包
        if (rawId.contains("PACKAGE") || registryName.contains("_package")) {
            return handlePackageUpgrade(player, coreStack, rawId, registryName);
        }

        // 普通升級
        return handleNormalUpgrade(player, coreStack, cid, moduleLevel);
    }

    private boolean handleNormalUpgrade(EntityPlayer player, ItemStack coreStack, String cid, int moduleLevel) {
        unlockIfLocked(coreStack, cid);

        // 記錄歷史最高值
        recordOriginalMax(coreStack, cid, moduleLevel);

        // 設置新等級
        ItemMechanicalCoreExtended.setUpgradeLevel(coreStack, cid, moduleLevel);
        UpgradeKeys.setLevel(coreStack, cid, moduleLevel);
        UpgradeKeys.markOwnedActive(coreStack, cid, moduleLevel);

        // 嘗試設置基礎升級
        try {
            ItemMechanicalCore.UpgradeType enumType = null;
            for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
                if (t.getKey().equalsIgnoreCase(cid) || t.name().equalsIgnoreCase(cid)) {
                    enumType = t;
                    break;
                }
            }
            if (enumType != null) {
                ItemMechanicalCore.setUpgradeLevel(coreStack, enumType, moduleLevel);
            }
        } catch (Throwable ignored) {}

        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ " + getDisplayName(cid) +
                        TextFormatting.WHITE + " 升級至 Lv." + moduleLevel
        ));

        return true;
    }

    private boolean handleFlightModule(EntityPlayer player, ItemStack coreStack, int moduleLevel) {
        String cid = "FLIGHT_MODULE";
        unlockIfLocked(coreStack, cid);

        recordOriginalMax(coreStack, cid, moduleLevel);

        ItemMechanicalCore.setUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE, moduleLevel);
        ItemMechanicalCoreExtended.setUpgradeLevel(coreStack, "FLIGHT_MODULE", moduleLevel);
        UpgradeKeys.setLevel(coreStack, "FLIGHT_MODULE", moduleLevel);
        UpgradeKeys.markOwnedActive(coreStack, "FLIGHT_MODULE", moduleLevel);

        NBTTagCompound nbt = UpgradeKeys.getOrCreate(coreStack);
        nbt.setBoolean("FlightModuleEnabled", true);
        if (moduleLevel >= 2 && !nbt.hasKey("FlightHoverMode")) nbt.setBoolean("FlightHoverMode", false);
        if (moduleLevel >= 3 && !nbt.hasKey("CoreSpeedMode")) nbt.setInteger("CoreSpeedMode", 0);

        switch (moduleLevel) {
            case 1:
                player.sendMessage(new TextComponentString(TextFormatting.LIGHT_PURPLE + "✦ 飛行系統已激活！"));
                break;
            case 2:
                player.sendMessage(new TextComponentString(TextFormatting.GOLD + "✦ 飛行系統升級！懸停模式已解鎖！"));
                break;
            case 3:
                player.sendMessage(new TextComponentString(TextFormatting.DARK_PURPLE + "✦✦ 終極飛行系統已啟動！速度模式已解鎖！"));
                break;
        }

        return true;
    }

    private boolean handleWaterproofModule(EntityPlayer player, ItemStack coreStack, int moduleLevel) {
        String cid = "WATERPROOF_MODULE";
        unlockIfLocked(coreStack, cid);

        recordOriginalMax(coreStack, cid, moduleLevel);
        recordOriginalMax(coreStack, "WATERPROOF", moduleLevel);

        ItemMechanicalCoreExtended.setUpgradeLevel(coreStack, "WATERPROOF_MODULE", moduleLevel);
        UpgradeKeys.setLevel(coreStack, "WATERPROOF_MODULE", moduleLevel);
        UpgradeKeys.markOwnedActive(coreStack, "WATERPROOF_MODULE", moduleLevel);
        ItemMechanicalCoreExtended.setUpgradeLevel(coreStack, "WATERPROOF", moduleLevel);

        NBTTagCompound nbt = UpgradeKeys.getOrCreate(coreStack);
        nbt.setBoolean("hasWaterproofModule", moduleLevel > 0);
        nbt.setInteger("waterproofLevel", moduleLevel);

        switch (moduleLevel) {
            case 1:
                player.sendMessage(new TextComponentString(TextFormatting.AQUA + "💧 基礎防水塗層已應用！"));
                break;
            case 2:
                player.sendMessage(new TextComponentString(TextFormatting.BLUE + "💧 高級防水系統已安裝！"));
                break;
            case 3:
                player.sendMessage(new TextComponentString(TextFormatting.DARK_AQUA + "🌊 深海適應模組已激活！"));
                break;
        }

        return true;
    }

    private boolean handlePackageUpgrade(EntityPlayer player, ItemStack core, String rawType, String registryName) {
        boolean isSurvival = rawType.equalsIgnoreCase("SURVIVAL_PACKAGE") || registryName.contains("survival_enhancement_package");
        boolean isCombat = rawType.equalsIgnoreCase("COMBAT_PACKAGE") || registryName.contains("combat_enhancement_package");

        String[] survivalUps = {"YELLOW_SHIELD", "HEALTH_REGEN", "HUNGER_THIRST"};
        String[] combatUps = {"DAMAGE_BOOST", "ATTACK_SPEED", "RANGE_EXTENSION"};
        String[] omniUps = {"ENERGY_CAPACITY", "ENERGY_EFFICIENCY", "ARMOR_ENHANCEMENT"};

        String[] targetList = isSurvival ? survivalUps : (isCombat ? combatUps : omniUps);

        for (String u : targetList) {
            unlockIfLocked(core, u);
            int cur = lvOf(core, u);
            int newLevel = cur + 1;

            recordOriginalMax(core, u, newLevel);
            applyUpgrade(core, u, newLevel);
        }

        if (isSurvival) {
            player.sendMessage(new TextComponentString(TextFormatting.GREEN + "✦ 生存強化套裝已應用！"));
        } else if (isCombat) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "✦ 戰鬥強化套裝已應用！"));
        } else {
            player.sendMessage(new TextComponentString(TextFormatting.LIGHT_PURPLE + "✦ 全能強化芯片已應用！"));
        }

        return true;
    }

    // ===== 工具方法 =====

    private void recordOriginalMax(ItemStack coreStack, String upgradeId, int newLevel) {
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(coreStack);
        String key = "OriginalMax_" + upgradeId.toUpperCase();
        int currentMax = nbt.getInteger(key);
        if (newLevel > currentMax) {
            nbt.setInteger(key, newLevel);
            nbt.setInteger("OriginalMax_" + upgradeId.toLowerCase(), newLevel);
        }
    }

    private void unlockIfLocked(ItemStack core, String id) {
        UpgradeKeys.unlock(core, id);
    }

    private int lvOf(ItemStack core, String id) {
        int lv = 0;
        lv = Math.max(lv, ItemMechanicalCoreExtended.getUpgradeLevel(core, id));
        lv = Math.max(lv, ItemMechanicalCoreExtended.getUpgradeLevel(core, id.toLowerCase(Locale.ROOT)));
        try {
            ItemMechanicalCore.UpgradeType t = ItemMechanicalCore.UpgradeType.valueOf(UpgradeKeys.canon(id));
            lv = Math.max(lv, ItemMechanicalCore.getUpgradeLevel(core, t));
        } catch (Throwable ignored) {}
        lv = Math.max(lv, UpgradeKeys.getLevel(core, id));
        return lv;
    }

    private int maxOf(ItemStack core, String id) {
        ItemMechanicalCoreExtended.UpgradeInfo info = ItemMechanicalCoreExtended.getUpgradeInfo(id);
        if (info == null) info = ItemMechanicalCoreExtended.getUpgradeInfo(id.toUpperCase(Locale.ROOT));
        if (info == null) info = ItemMechanicalCoreExtended.getUpgradeInfo(id.toLowerCase(Locale.ROOT));
        if (info != null) return info.maxLevel;

        try {
            ItemMechanicalCore.UpgradeType t = ItemMechanicalCore.UpgradeType.valueOf(UpgradeKeys.canon(id));
            return getMaxLevel(t);
        } catch (Throwable ignored) {}
        return 3;
    }

    private int getMaxLevel(ItemMechanicalCore.UpgradeType type) {
        switch (type) {
            case ENERGY_CAPACITY: return 10;
            case ENERGY_EFFICIENCY: return 5;
            case ARMOR_ENHANCEMENT: return 5;
            case SPEED_BOOST: return 3;
            case REGENERATION: return 3;
            case FLIGHT_MODULE: return 3;
            case SHIELD_GENERATOR: return 3;
            case TEMPERATURE_CONTROL: return 5;
            default: return 5;
        }
    }

    private void applyUpgrade(ItemStack core, String id, int level) {
        String cid = UpgradeKeys.foldAlias(id);
        ItemMechanicalCoreExtended.setUpgradeLevel(core, cid, level);
        try {
            ItemMechanicalCore.UpgradeType t = ItemMechanicalCore.UpgradeType.valueOf(cid);
            ItemMechanicalCore.setUpgradeLevel(core, t, level);
        } catch (Throwable ignored) {}
        UpgradeKeys.setLevel(core, cid, level);
        UpgradeKeys.markOwnedActive(core, cid, level);
    }

    private String getDisplayName(String id) {
        ItemMechanicalCoreExtended.UpgradeInfo info = ItemMechanicalCoreExtended.getUpgradeInfo(id);
        if (info != null) return info.displayName;

        for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
            if (t.getKey().equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)) {
                return t.getDisplayName();
            }
        }

        return UpgradeKeys.canon(id).replace("_", " ");
    }

    private int getModuleLevel(ItemUpgradeComponent item, String registryName) {
        ItemStack stack = new ItemStack(item);
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt.hasKey("ModuleLevel")) return nbt.getInteger("ModuleLevel");
            if (nbt.hasKey("Level")) return nbt.getInteger("Level");
        }

        if (registryName.contains("_lv") || registryName.contains("_level")) {
            String[] parts = registryName.split("_");
            for (String part : parts) {
                if (part.startsWith("lv") || part.startsWith("level")) {
                    String numStr = part.replaceAll("[^0-9]", "");
                    try { return Integer.parseInt(numStr); } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (registryName.contains("basic") || registryName.contains("tier1")) return 1;
        if (registryName.contains("advanced") || registryName.contains("tier2")) return 2;
        if (registryName.contains("ultimate") || registryName.contains("tier3")) return 3;
        if (registryName.contains("legendary") || registryName.contains("tier4")) return 4;
        if (registryName.contains("mythic") || registryName.contains("tier5")) return 5;

        int upVal = item.getUpgradeValue();
        if (upVal > 0 && upVal <= 10) return upVal;

        return 1;
    }

    private int getFlightLevel(ItemStack core) {
        int lv = 0;
        lv = Math.max(lv, ItemMechanicalCore.getUpgradeLevel(core, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE));
        lv = Math.max(lv, ItemMechanicalCoreExtended.getUpgradeLevel(core, "FLIGHT_MODULE"));
        return lv;
    }

    private int getWaterproofLevel(ItemStack core) {
        return Math.max(lvOf(core, "WATERPROOF_MODULE"), lvOf(core, "WATERPROOF"));
    }

    private void forceSyncCore(EntityPlayer player) {
        try {
            IBaublesItemHandler h = BaublesApi.getBaublesHandler(player);
            if (h != null) {
                for (int i = 0; i < h.getSlots(); i++) {
                    ItemStack s = h.getStackInSlot(i);
                    if (ItemMechanicalCore.isMechanicalCore(s)) {
                        h.setStackInSlot(i, s.copy());
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}
        player.inventory.markDirty();
        player.openContainer.detectAndSendChanges();
    }

    // ===== 粒子效果 =====

    private void spawnUpgradeParticles() {
        BlockPos center = pos.up();
        for (int i = 0; i < 10; i++) {
            double angle = (Math.PI * 2) * i / 10;
            double radius = 1.0;
            double x = center.getX() + 0.5 + Math.cos(angle) * radius;
            double z = center.getZ() + 0.5 + Math.sin(angle) * radius;
            double y = center.getY() + 0.5 + (upgradeProgress / (float)UPGRADE_TICKS) * 1.5;

            world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                    x, y, z,
                    (center.getX() + 0.5 - x) * 0.1,
                    0.2,
                    (center.getZ() + 0.5 - z) * 0.1);
        }
    }

    private void spawnSuccessParticles() {
        BlockPos center = pos.up();
        for (int i = 0; i < 50; i++) {
            double x = center.getX() + 0.5 + (world.rand.nextDouble() - 0.5) * 2;
            double y = center.getY() + 1.0 + world.rand.nextDouble();
            double z = center.getZ() + 0.5 + (world.rand.nextDouble() - 0.5) * 2;

            world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, x, y, z, 0, 0.1, 0);
            world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, x, y, z,
                    world.rand.nextGaussian() * 0.05, 0.1, world.rand.nextGaussian() * 0.05);
        }
    }

    private void spawnFailParticles() {
        BlockPos center = pos.up();
        for (int i = 0; i < 30; i++) {
            double x = center.getX() + 0.5 + (world.rand.nextDouble() - 0.5) * 2;
            double y = center.getY() + 1.0 + world.rand.nextDouble();
            double z = center.getZ() + 0.5 + (world.rand.nextDouble() - 0.5) * 2;

            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z, 0, 0.05, 0);
            world.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, 0, 0, 0);
        }
    }

    // ===== Getters =====

    public int getRequiredEnergy() {
        ItemStack module = inventory.getStackInSlot(0);
        if (module.isEmpty() || !(module.getItem() instanceof ItemUpgradeComponent)) {
            return BASE_REQUIRED;
        }
        return getRequiredEnergy((ItemUpgradeComponent) module.getItem());
    }

    public int getRequiredEnergy(ItemUpgradeComponent upgradeItem) {
        String registryName = upgradeItem.getRegistryName() != null ?
                upgradeItem.getRegistryName().toString() : "";
        int moduleLevel = getModuleLevel(upgradeItem, registryName);
        return BASE_REQUIRED + (moduleLevel * ENERGY_PER_LEVEL);
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
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

    public int getRepairProgress() {
        return repairProgress;
    }

    public int getMaxRepairProgress() {
        return REPAIR_TICKS;
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
        compound.setInteger("Progress", upgradeProgress);
        compound.setBoolean("IsUpgrading", isUpgrading);
        compound.setInteger("Cooldown", cooldown);
        // 修復狀態
        compound.setBoolean("IsRepairing", isRepairing);
        compound.setInteger("RepairProgress", repairProgress);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Inventory")) {
            inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        }
        if (compound.hasKey("Energy")) {
            int fe = compound.getInteger("Energy");
            while (energy.getEnergyStored() < fe && energy.receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        }
        upgradeProgress = compound.getInteger("Progress");
        isUpgrading = compound.getBoolean("IsUpgrading");
        cooldown = compound.getInteger("Cooldown");
        // 修復狀態
        isRepairing = compound.getBoolean("IsRepairing");
        repairProgress = compound.getInteger("RepairProgress");
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

    private void syncToClient() {
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }
}
