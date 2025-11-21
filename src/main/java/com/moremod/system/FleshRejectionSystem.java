package com.moremod.system;

import com.moremod.config.FleshRejectionConfig;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCore.UpgradeType;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.ItemMechanicalCoreExtended.UpgradeInfo;
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

import java.util.Locale;
import java.util.Map;

/**
 * 血肉排异系统
 * 完全基于核心NBT存储，死亡处理由CoreDropProtection负责
 */
public class FleshRejectionSystem {

    // NBT键名
    private static final String NBT_GROUP = "rejection";
    private static final String NBT_REJECTION = "RejectionLevel";
    private static final String NBT_ADAPTATION = "AdaptationLevel";
    private static final String NBT_TRANSCENDED = "RejectionTranscended";
    private static final String NBT_BLEEDING_TICKS = "BleedingTicks";
    private static final String NBT_LAST_STABILIZER = "LastStabilizerUse";

    // ========== 核心检查 ==========
    
    /**
     * 检查玩家是否佩戴机械核心
     */
    public static boolean hasMechanicalCore(EntityPlayer player) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        return !core.isEmpty();
    }

    // ========== 基础数值访问 ==========

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
     * 设置排异值
     */
    public static void setRejectionLevel(EntityPlayer player, float value) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;
        
        NBTTagCompound rejData = core.getOrCreateSubCompound(NBT_GROUP);
        float clamped = MathHelper.clamp(value, 0f, (float) FleshRejectionConfig.maxRejection);
        rejData.setFloat(NBT_REJECTION, clamped);
        
        syncToClient(player);
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
     * 设置适应度
     */
    public static void setAdaptationLevel(EntityPlayer player, float value) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;
        
        NBTTagCompound rejData = core.getOrCreateSubCompound(NBT_GROUP);
        float clamped = MathHelper.clamp(value, 0f, (float) FleshRejectionConfig.adaptationThreshold);
        rejData.setFloat(NBT_ADAPTATION, clamped);
        
        syncToClient(player);
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
     * 设置突破状态
     */
    public static void setTranscended(EntityPlayer player, boolean transcended) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;
        
        NBTTagCompound rejData = core.getOrCreateSubCompound(NBT_GROUP);
        rejData.setBoolean(NBT_TRANSCENDED, transcended);
        
        syncToClient(player);
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
     * 设置出血时间
     */
    public static void setBleedingTicks(EntityPlayer player, int ticks) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;
        
        NBTTagCompound rejData = core.getOrCreateSubCompound(NBT_GROUP);
        rejData.setInteger(NBT_BLEEDING_TICKS, Math.max(0, ticks));
        
        syncToClient(player);
    }

    /**
     * 触发出血
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
     * 设置最后使用稳定剂的时间
     */
    public static void setLastStabilizerUse(EntityPlayer player, long time) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;
        
        NBTTagCompound rejData = core.getOrCreateSubCompound(NBT_GROUP);
        rejData.setLong(NBT_LAST_STABILIZER, time);
        
        syncToClient(player);
    }

    // ========== 模组统计 ==========

    /**
     * 获取安装的模组总等级
     */
    public static int getTotalInstalledModules(ItemStack core) {
        if (core.isEmpty()) return 0;

        int total = 0;

        // 传统UpgradeType
        for (UpgradeType type : UpgradeType.values()) {
            int level = ItemMechanicalCore.getUpgradeLevel(core, type);
            if (level > 0) total += level;
        }

        // 扩展模组
        Map<String, UpgradeInfo> allUpgrades = ItemMechanicalCoreExtended.getAllUpgrades();
        for (Map.Entry<String, UpgradeInfo> entry : allUpgrades.entrySet()) {
            String id = entry.getKey();
            int level = ItemMechanicalCoreExtended.getUpgradeLevel(core, id);
            if (level > 0) total += level;
        }

        return total;
    }

    /**
     * 获取运行中的模组数量
     */
    public static int getRunningModuleCount(ItemStack core) {
        if (core.isEmpty()) return 0;

        int count = 0;
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return 0;

        // 传统模组
        for (UpgradeType type : UpgradeType.values()) {
            String id = type.getKey();
            int level = ItemMechanicalCore.getUpgradeLevel(core, type);
            if (level > 0 && !isPaused(nbt, id)) {
                count += level;
            }
        }

        // 扩展模组
        Map<String, UpgradeInfo> allUpgrades = ItemMechanicalCoreExtended.getAllUpgrades();
        for (Map.Entry<String, UpgradeInfo> entry : allUpgrades.entrySet()) {
            String id = entry.getKey();
            int level = ItemMechanicalCoreExtended.getUpgradeLevel(core, id);
            if (level > 0 && !isPaused(nbt, id)) {
                count += level;
            }
        }

        return count;
    }

    private static boolean isPaused(NBTTagCompound nbt, String id) {
        if (nbt == null) return false;
        String upper = id.toUpperCase(Locale.ROOT);
        String lower = id.toLowerCase(Locale.ROOT);
        return nbt.getBoolean("IsPaused_" + id) ||
               nbt.getBoolean("IsPaused_" + upper) ||
               nbt.getBoolean("IsPaused_" + lower);
    }

    /**
     * 检查是否安装神经同步器
     */
    public static boolean hasNeuralSynchronizer(ItemStack core) {
        if (core.isEmpty()) return false;

        // 检查传统UpgradeType
        for (UpgradeType type : UpgradeType.values()) {
            String key = type.getKey();
            if ("neural_synchronizer".equalsIgnoreCase(key)) {
                if (ItemMechanicalCore.getUpgradeLevel(core, type) > 0) {
                    return true;
                }
            }
        }

        // 检查扩展ID
        if (ItemMechanicalCoreExtended.getUpgradeLevel(core, "neural_synchronizer") > 0) return true;
        if (ItemMechanicalCoreExtended.getUpgradeLevel(core, "NEURAL_SYNCHRONIZER") > 0) return true;

        // 兜底检查NBT
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt != null) {
            if (nbt.getInteger("upgrade_neural_synchronizer") > 0) return true;
            if (nbt.getInteger("upgrade_NEURAL_SYNCHRONIZER") > 0) return true;
        }

        return false;
    }

    // ========== 主更新逻辑 ==========

    /**
     * 每秒更新（服务端）
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
        
        // 计算适应度
        float adaptation = calcAdaptation(installedCount, hasSync);
        
        // 处理突破状态
        if (wasTranscend) {
            if (adaptation < FleshRejectionConfig.adaptationThreshold) {
                // 失去突破
                rejData.setBoolean(NBT_TRANSCENDED, false);
                player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "⚠ 警告：适应度下降，排异重新激活！"
                ));
                player.world.playSound(null, player.getPosition(),
                    net.minecraft.init.SoundEvents.ENTITY_WITHER_SPAWN,
                    net.minecraft.util.SoundCategory.PLAYERS, 0.5f, 0.5f);
            } else {
                // 维持突破
                rejData.setFloat(NBT_REJECTION, 0f);
                rejData.setFloat(NBT_ADAPTATION, adaptation);
                syncToClient(player);
                return;
            }
        }
        
        // 正常排异增长
        float currentRejection = rejData.getFloat(NBT_REJECTION);
        float growthPerSec = (float) (runningCount * FleshRejectionConfig.rejectionGrowthRate);
        float newRejection = currentRejection + growthPerSec;
        
        newRejection = MathHelper.clamp(newRejection, 0f, (float) FleshRejectionConfig.maxRejection);
        
        rejData.setFloat(NBT_REJECTION, newRejection);
        rejData.setFloat(NBT_ADAPTATION, adaptation);
        
        // 调试信息
        if (FleshRejectionConfig.debugMode) {
            player.sendStatusMessage(new TextComponentString(
                String.format("§7[排异] 运行:%d 增长:%.2f/s 排异:%.1f 适应:%.0f",
                    runningCount, growthPerSec, newRejection, adaptation)
            ), true);
        }
        
        // 检查突破
        if (adaptation >= FleshRejectionConfig.adaptationThreshold && !wasTranscend) {
            transcendRejection(player, rejData);
            return;
        }
        
        // 处理出血
        handleBleeding(player, newRejection, rejData);
        
        // 同步到客户端
        syncToClient(player);
    }

    private static float calcAdaptation(int installedCount, boolean hasSynchronizer) {
        float base = (float) (installedCount * FleshRejectionConfig.adaptationPerModule);
        if (hasSynchronizer) {
            base += FleshRejectionConfig.neuralSynchronizerBonus;
        }
        return base;
    }

    /**
     * 触发突破
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
        
        // 粒子效果
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
        
        syncToClient(player);
    }

    /**
     * 处理出血效果
     */
    private static void handleBleeding(EntityPlayer player, float rejection, NBTTagCompound rejData) {
        if (!FleshRejectionConfig.enableBleeding) return;
        
        int bleedTicks = rejData.getInteger(NBT_BLEEDING_TICKS);
        if (bleedTicks <= 0) return;
        
        // 每秒扣血
        if (player.ticksExisted % 20 == 0) {
            float damage = (float) (rejection >= FleshRejectionConfig.selfDamageStart
                    ? FleshRejectionConfig.bleedingDamageHigh
                    : FleshRejectionConfig.bleedingDamageLow);
            
            player.attackEntityFrom(
                new DamageSource("bleeding").setDamageBypassesArmor(),
                damage
            );
            
            // 血液粒子
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

    /**
     * 同步到客户端
     */
    private static void syncToClient(EntityPlayer player) {
        if (!player.world.isRemote && player instanceof EntityPlayerMP) {
            BaublesSyncUtil.safeSyncAll((EntityPlayerMP) player);
        }
    }

    /**
     * 死亡处理（实际处理在CoreDropProtection中）
     */
    public static void handlePlayerDeath(EntityPlayer oldPlayer, EntityPlayer newPlayer) {
        // 留空，实际处理在 CoreDropProtection.processDeathRejection()
    }

    // ========== 状态摘要 ==========

    /**
     * 获取状态摘要
     */
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
}