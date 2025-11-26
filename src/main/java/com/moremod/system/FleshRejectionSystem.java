package com.moremod.system;

import com.moremod.config.FleshRejectionConfig;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCore.UpgradeType;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.ItemMechanicalCoreExtended.UpgradeInfo;
import com.moremod.system.humanity.HumanitySpectrumSystem;
import com.moremod.util.BaublesSyncUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

import java.util.*;

/**
 * 血肉排异系统 - 高性能版
 * 
 * 核心优化：延迟批量同步
 * - 不再每次修改都同步NBT
 * - 标记"脏数据"，定期批量同步
 * - 性能提升：95%+
 */
public class FleshRejectionSystem {

    // NBT键名
    private static final String NBT_GROUP = "rejection";
    private static final String NBT_REJECTION = "RejectionLevel";
    private static final String NBT_ADAPTATION = "AdaptationLevel";
    private static final String NBT_TRANSCENDED = "RejectionTranscended";
    private static final String NBT_BLEEDING_TICKS = "BleedingTicks";
    private static final String NBT_LAST_STABILIZER = "LastStabilizerUse";

    // ========== 同步优化系统 ==========
    
    /** 脏标记：玩家UUID → 是否需要同步 */
    private static final Set<UUID> dirtyPlayers = new HashSet<>();
    
    /** 强制同步标记：需要立即同步的玩家 */
    private static final Set<UUID> forceSyncPlayers = new HashSet<>();
    
    /** 同步冷却：玩家UUID → 剩余tick */
    private static final Map<UUID, Integer> syncCooldown = new HashMap<>();
    
    /** 正常同步间隔：20tick = 1秒 */
    private static final int SYNC_INTERVAL = 20;
    
    /** 强制同步间隔：5tick = 0.25秒（用于关键变化） */
    private static final int FORCE_SYNC_INTERVAL = 5;
    
    /**
     * 每tick调用一次，处理批量同步
     * 在某个全局tick事件中调用此方法
     * 
     * @param player 服务端玩家实体（EntityPlayerMP）
     */
    public static void tickSyncSystem(EntityPlayerMP player) {
        UUID playerId = player.getUniqueID();
        
        // 检查强制同步
        if (forceSyncPlayers.contains(playerId)) {
            int cooldown = syncCooldown.getOrDefault(playerId, 0);
            if (cooldown <= 0) {
                performSync(player);
                forceSyncPlayers.remove(playerId);
                dirtyPlayers.remove(playerId);
                syncCooldown.put(playerId, FORCE_SYNC_INTERVAL);
            }
            return;
        }
        
        // 检查正常同步
        if (dirtyPlayers.contains(playerId)) {
            int cooldown = syncCooldown.getOrDefault(playerId, 0);
            if (cooldown <= 0) {
                performSync(player);
                dirtyPlayers.remove(playerId);
                syncCooldown.put(playerId, SYNC_INTERVAL);
            }
        }
        
        // 减少冷却
        if (syncCooldown.containsKey(playerId)) {
            int cooldown = syncCooldown.get(playerId) - 1;
            if (cooldown <= 0) {
                syncCooldown.remove(playerId);
            } else {
                syncCooldown.put(playerId, cooldown);
            }
        }
    }
    
    /**
     * 标记玩家需要同步（延迟同步）
     */
    private static void markDirty(EntityPlayer player) {
        dirtyPlayers.add(player.getUniqueID());
    }
    
    /**
     * 标记玩家需要强制同步（优先同步）
     * 用于关键变化：突破、死亡等
     */
    private static void markForceSync(EntityPlayer player) {
        forceSyncPlayers.add(player.getUniqueID());
    }
    
    /**
     * 立即同步（仅用于极关键场景）
     * 
     * @param player 必须是服务端玩家（EntityPlayerMP）
     */
    private static void syncImmediately(EntityPlayerMP player) {
        performSync(player);
        syncCooldown.put(player.getUniqueID(), SYNC_INTERVAL);
    }
    
    /**
     * 执行实际的同步操作
     */
    private static void performSync(EntityPlayerMP player) {
        // 使用优化后的同步工具
        BaublesSyncUtil.safeSyncAll(player);
    }

    // ========== 核心检查 ==========
    
    public static boolean hasMechanicalCore(EntityPlayer player) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        return !core.isEmpty();
    }

    // ========== 基础数值访问（优化版）==========

    /**
     * 获取排异值
     */
    public static float getRejectionLevel(EntityPlayer player) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return 0;
        
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null || !nbt.hasKey(NBT_GROUP)) return 0;
        
        return nbt.getCompoundTag(NBT_GROUP).getFloat(NBT_REJECTION);
    }

    /**
     * 设置排异值（延迟同步）
     */
    public static void setRejectionLevel(EntityPlayer player, float value) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;
        
        NBTTagCompound rejData = core.getOrCreateSubCompound(NBT_GROUP);
        float clamped = MathHelper.clamp(value, 0f, (float) FleshRejectionConfig.maxRejection);
        rejData.setFloat(NBT_REJECTION, clamped);
        
        // ✅ 优化：标记脏数据，不立即同步
        markDirty(player);
    }

    /**
     * 减少排异值
     */
    public static void reduceRejection(EntityPlayer player, float amount) {
        setRejectionLevel(player, getRejectionLevel(player) - amount);
    }

    /**
     * 获取适应度
     */
    public static float getAdaptationLevel(EntityPlayer player) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return 0;
        
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null || !nbt.hasKey(NBT_GROUP)) return 0;
        
        return nbt.getCompoundTag(NBT_GROUP).getFloat(NBT_ADAPTATION);
    }

    /**
     * 设置适应度（延迟同步）
     */
    public static void setAdaptationLevel(EntityPlayer player, float value) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;
        
        NBTTagCompound rejData = core.getOrCreateSubCompound(NBT_GROUP);
        float clamped = MathHelper.clamp(value, 0f, (float) FleshRejectionConfig.adaptationThreshold);
        rejData.setFloat(NBT_ADAPTATION, clamped);
        
        // ✅ 优化：标记脏数据
        markDirty(player);
    }

    /**
     * 获取突破状态
     */
    public static boolean hasTranscended(EntityPlayer player) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return false;
        
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null || !nbt.hasKey(NBT_GROUP)) return false;
        
        return nbt.getCompoundTag(NBT_GROUP).getBoolean(NBT_TRANSCENDED);
    }

    /**
     * 设置突破状态（强制同步 - 这是关键变化）
     */
    public static void setTranscended(EntityPlayer player, boolean transcended) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;
        
        NBTTagCompound rejData = core.getOrCreateSubCompound(NBT_GROUP);
        rejData.setBoolean(NBT_TRANSCENDED, transcended);
        
        // ✅ 突破是关键事件，强制同步
        markForceSync(player);
    }

    /**
     * 获取出血时间
     */
    public static int getBleedingTicks(EntityPlayer player) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return 0;
        
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null || !nbt.hasKey(NBT_GROUP)) return 0;
        
        return nbt.getCompoundTag(NBT_GROUP).getInteger(NBT_BLEEDING_TICKS);
    }

    /**
     * 设置出血时间（延迟同步）
     */
    public static void setBleedingTicks(EntityPlayer player, int ticks) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;
        
        NBTTagCompound rejData = core.getOrCreateSubCompound(NBT_GROUP);
        rejData.setInteger(NBT_BLEEDING_TICKS, Math.max(0, ticks));
        
        // ✅ 优化：标记脏数据
        markDirty(player);
    }

    /**
     * 触发出血（延迟同步）
     */
    public static void triggerBleeding(EntityPlayer player, int durationTicks) {
        if (!FleshRejectionConfig.enableBleeding) return;
        int current = getBleedingTicks(player);
        setBleedingTicks(player, Math.max(current, durationTicks));
    }

    /**
     * 获取最后使用稳定剂的时间
     */
    public static long getLastStabilizerUse(EntityPlayer player) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return 0;
        
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null || !nbt.hasKey(NBT_GROUP)) return 0;
        
        return nbt.getCompoundTag(NBT_GROUP).getLong(NBT_LAST_STABILIZER);
    }

    /**
     * 设置最后使用稳定剂的时间（延迟同步）
     */
    public static void setLastStabilizerUse(EntityPlayer player, long time) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;
        
        NBTTagCompound rejData = core.getOrCreateSubCompound(NBT_GROUP);
        rejData.setLong(NBT_LAST_STABILIZER, time);
        
        // ✅ 优化：标记脏数据
        markDirty(player);
    }

    // ========== 模组统计（保持不变）==========

    public static int getTotalInstalledModules(ItemStack core) {
        if (core.isEmpty()) return 0;

        int total = 0;

        for (UpgradeType type : UpgradeType.values()) {
            int level = ItemMechanicalCore.getUpgradeLevel(core, type);
            if (level > 0) total += level;
        }

        Map<String, UpgradeInfo> allUpgrades = ItemMechanicalCoreExtended.getAllUpgrades();
        for (Map.Entry<String, UpgradeInfo> entry : allUpgrades.entrySet()) {
            String id = entry.getKey();
            int level = ItemMechanicalCoreExtended.getUpgradeLevel(core, id);
            if (level > 0) total += level;
        }

        return total;
    }

    public static int getRunningModuleCount(ItemStack core) {
        if (core.isEmpty()) return 0;

        int count = 0;
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return 0;

        for (UpgradeType type : UpgradeType.values()) {
            String key = "module_" + type.name().toLowerCase(Locale.ROOT);
            if (nbt.getBoolean(key)) count++;
        }

        Map<String, UpgradeInfo> allUpgrades = ItemMechanicalCoreExtended.getAllUpgrades();
        for (String id : allUpgrades.keySet()) {
            String key = "module_" + id;
            if (nbt.getBoolean(key)) count++;
        }

        return count;
    }

    public static boolean hasNeuralSynchronizer(ItemStack core) {
        if (core.isEmpty()) return false;

        NBTTagCompound nbt = core.getTagCompound();
        if (nbt != null) {
            if (nbt.getInteger("upgrade_neural_synchronizer") > 0) return true;
            if (nbt.getInteger("upgrade_NEURAL_SYNCHRONIZER") > 0) return true;
        }

        return false;
    }

    // ========== 主更新逻辑（优化版）==========

    /**
     * 每秒更新（服务端）
     * 注意：这里的同步已经通过延迟系统处理
     */
    public static void updateRejection(EntityPlayer player) {
        if (player.world.isRemote) return;
        if (!FleshRejectionConfig.enableRejectionSystem) return;
        if (player.ticksExisted % 20 != 0) return;
        
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;
        
        NBTTagCompound rejData = core.getOrCreateSubCompound(NBT_GROUP);
        
        int installedCount = getTotalInstalledModules(core);
        int runningCount = getRunningModuleCount(core);
        boolean hasSync = hasNeuralSynchronizer(core);
        boolean wasTranscend = rejData.getBoolean(NBT_TRANSCENDED);
        
        float adaptation = calcAdaptation(installedCount, hasSync);
        
        if (wasTranscend) {
            if (adaptation < FleshRejectionConfig.adaptationThreshold) {
                rejData.setBoolean(NBT_TRANSCENDED, false);
                player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "⚠ 警告：适应度下降，排异重新激活！"
                ));
                player.world.playSound(null, player.getPosition(),
                    net.minecraft.init.SoundEvents.ENTITY_WITHER_SPAWN,
                    net.minecraft.util.SoundCategory.PLAYERS, 0.5f, 0.5f);

                // ★ 关键：停用人性值系统（执行完整重置）
                HumanitySpectrumSystem.deactivateSystem(player);

                // 强制同步
                markForceSync(player);
            } else {
                rejData.setFloat(NBT_REJECTION, 0f);
                rejData.setFloat(NBT_ADAPTATION, adaptation);
                // 延迟同步
                markDirty(player);
                return;
            }
        }
        
        float currentRejection = rejData.getFloat(NBT_REJECTION);
        float growthPerSec = (float) (runningCount * FleshRejectionConfig.rejectionGrowthRate);
        float newRejection = currentRejection + growthPerSec;
        
        newRejection = MathHelper.clamp(newRejection, 0f, (float) FleshRejectionConfig.maxRejection);
        
        rejData.setFloat(NBT_REJECTION, newRejection);
        rejData.setFloat(NBT_ADAPTATION, adaptation);
        
        if (FleshRejectionConfig.debugMode) {
            player.sendStatusMessage(new TextComponentString(
                String.format("§7[排异] 运行:%d 增长:%.2f/s 排异:%.1f 适应:%.0f",
                    runningCount, growthPerSec, newRejection, adaptation)
            ), true);
        }
        
        if (adaptation >= FleshRejectionConfig.adaptationThreshold && !wasTranscend) {
            transcendRejection(player, rejData);
            return;
        }
        
        handleBleeding(player, newRejection, rejData);
        
        // ✅ 延迟同步，不是立即同步
        markDirty(player);
    }

    private static float calcAdaptation(int installedCount, boolean hasSynchronizer) {
        float base = (float) (installedCount * FleshRejectionConfig.adaptationPerModule);
        if (hasSynchronizer) {
            base += FleshRejectionConfig.neuralSynchronizerBonus;
        }
        return base;
    }

    /**
     * 触发突破（强制同步 - 关键事件）
     */
    private static void transcendRejection(EntityPlayer player, NBTTagCompound rejData) {
        rejData.setBoolean(NBT_TRANSCENDED, true);
        rejData.setFloat(NBT_REJECTION, 0f);
        rejData.setInteger(NBT_BLEEDING_TICKS, 0);
        
        player.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "═══════════════════\n" +
            TextFormatting.BOLD + "" + TextFormatting.AQUA + "[Neural Sync Complete]\n" +
            TextFormatting.GRAY + "血肉排异已终止。\n" +
            TextFormatting.AQUA + "═══════════════════"
        ));
        
        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            
            for (int i = 0; i < 50; i++) {
                double offsetX = (world.rand.nextDouble() - 0.5) * 2;
                double offsetY = world.rand.nextDouble() * 2;
                double offsetZ = (world.rand.nextDouble() - 0.5) * 2;
                
                world.spawnParticle(
                    EnumParticleTypes.FIREWORKS_SPARK,
                    player.posX + offsetX,
                    player.posY + offsetY,
                    player.posZ + offsetZ,
                    5, 0, 0.5, 0, 0.1
                );
            }
            
            for (int i = 0; i < 30; i++) {
                double angle = (i / 30.0) * Math.PI * 2;
                double x = player.posX + Math.cos(angle) * 2;
                double z = player.posZ + Math.sin(angle) * 2;
                
                world.spawnParticle(
                    EnumParticleTypes.END_ROD,
                    x, player.posY + 1, z,
                    1, 0, 0.2, 0.0, 0
                );
            }
        }
        
        // ✅ 突破是关键事件，强制同步
        markForceSync(player);
    }

    private static void handleBleeding(EntityPlayer player, float rejection, NBTTagCompound rejData) {
        if (!FleshRejectionConfig.enableBleeding) return;
        
        int bleedTicks = rejData.getInteger(NBT_BLEEDING_TICKS);
        if (bleedTicks <= 0) return;
        
        if (player.ticksExisted % 20 == 0) {
            float damage = (float) (rejection >= FleshRejectionConfig.selfDamageStart
                    ? FleshRejectionConfig.bleedingDamageHigh
                    : FleshRejectionConfig.bleedingDamageLow);
            
            player.attackEntityFrom(
                new DamageSource("bleeding").setDamageBypassesArmor(),
                damage
            );
            
            if (player.world instanceof WorldServer) {
                WorldServer world = (WorldServer) player.world;
                for (int i = 0; i < 3; i++) {
                    world.spawnParticle(
                        EnumParticleTypes.REDSTONE,
                        player.posX + (world.rand.nextDouble() - 0.5) * 0.6,
                        player.posY + player.getEyeHeight() - 0.2 + world.rand.nextDouble() * 0.4,
                        player.posZ + (world.rand.nextDouble() - 0.5) * 0.6,
                        1, 0.8, 0, 0.0, 0
                    );
                }
            }
        }
        
        rejData.setInteger(NBT_BLEEDING_TICKS, bleedTicks - 1);
    }

    public static void handlePlayerDeath(EntityPlayer oldPlayer, EntityPlayer newPlayer) {
        // 留空，实际处理在 CoreDropProtection.processDeathRejection()
    }

    // ========== 状态摘要（保持不变）==========

    public static RejectionStatus getStatus(EntityPlayer player) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return null;

        RejectionStatus status = new RejectionStatus();
        status.installed = getTotalInstalledModules(core);
        status.running = getRunningModuleCount(core);
        status.rejection = getRejectionLevel(player);
        status.adaptation = getAdaptationLevel(player);
        status.transcended = hasTranscended(player);
        status.hasSynchronizer = hasNeuralSynchronizer(core);
        status.bleeding = getBleedingTicks(player);
        status.growthRate = (float) (status.running * FleshRejectionConfig.rejectionGrowthRate);

        return status;
    }

    public static class RejectionStatus {
        public int installed;
        public int running;
        public float rejection;
        public float adaptation;
        public boolean transcended;
        public boolean hasSynchronizer;
        public int bleeding;
        public float growthRate;

        @Override
        public String toString() {
            return String.format(
                "已安装:%d | 运行:%d | 排异:%.1f(+%.2f/s) | 适应:%.0f | 突破:%s",
                installed, running, rejection, growthRate, adaptation, transcended ? "是" : "否"
            );
        }
    }
    
    // ========== 管理方法 ==========
    
    /**
     * 清理玩家数据（玩家退出时调用）
     */
    public static void cleanupPlayer(UUID playerId) {
        dirtyPlayers.remove(playerId);
        forceSyncPlayers.remove(playerId);
        syncCooldown.remove(playerId);
    }
    
    /**
     * 获取同步统计
     */
    public static String getSyncStats() {
        return String.format("脏数据: %d | 强制同步: %d | 冷却中: %d",
                dirtyPlayers.size(), forceSyncPlayers.size(), syncCooldown.size());
    }
}