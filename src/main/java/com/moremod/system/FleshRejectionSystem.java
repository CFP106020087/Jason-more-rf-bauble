package com.moremod.system;

import com.moremod.config.FleshRejectionConfig;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCore.UpgradeType;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.ItemMechanicalCoreExtended.UpgradeInfo;
import net.minecraft.entity.player.EntityPlayer;
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
 * 血肉排异系统 (Flesh Rejection System)
 *
 * 設計重點：
 * - 主存：玩家 EntityData（記憶體內，HUD 直接讀這裡）
 * - 備份：核心物品 NBT（顯示、跨死亡、掉落時保存）
 * - 優化：改為「標記髒數據 + 延遲同步」，避免每次修改都寫 NBT
 */
public class FleshRejectionSystem {

    // NBT 子標籤名
    private static final String NBT_REJECTION       = "RejectionLevel";
    private static final String NBT_ADAPTATION      = "AdaptationLevel";
    private static final String NBT_TRANSCENDED     = "RejectionTranscended";
    private static final String NBT_BLEEDING_TICKS  = "BleedingTicks";
    private static final String NBT_LAST_STABILIZER = "LastStabilizerUse";
    private static final String NBT_GROUP           = "rejection";

    // 玩家 EntityData 的根鍵
    private static final String PLAYER_DATA_KEY = "MoreMod_RejectionData";
    private static final String NBT_DIRTY       = "Dirty"; // 是否需要同步到核心

    /**
     * ✅ 取得玩家的排異資料（主存）
     */
    private static NBTTagCompound getPlayerRejectionData(EntityPlayer player) {
        NBTTagCompound entityData = player.getEntityData();
        if (!entityData.hasKey(PLAYER_DATA_KEY, 10)) {
            entityData.setTag(PLAYER_DATA_KEY, new NBTTagCompound());
        }
        return entityData.getCompoundTag(PLAYER_DATA_KEY);
    }

    /**
     * ✅ 標記為「已修改，需要同步到核心」
     */
    private static void markDirty(NBTTagCompound playerData) {
        playerData.setBoolean(NBT_DIRTY, true);
    }

    /**
     * ✅ 從核心取得排異資料（備份）
     */
    private static NBTTagCompound getCoreRejectionData(ItemStack core) {
        if (core.isEmpty()) return new NBTTagCompound();
        return core.getOrCreateSubCompound(NBT_GROUP);
    }

    /**
     * ✅ 玩家 → 核心，同步（僅在 Dirty 為 true 時呼叫）
     */
    private static void syncToCore(EntityPlayer player, ItemStack core) {
        if (core.isEmpty()) return;

        NBTTagCompound playerData = getPlayerRejectionData(player);
        if (!playerData.getBoolean(NBT_DIRTY)) return; // 無變化就不寫 NBT

        NBTTagCompound coreData = core.getOrCreateSubCompound(NBT_GROUP);

        coreData.setFloat(NBT_REJECTION, playerData.getFloat(NBT_REJECTION));
        coreData.setFloat(NBT_ADAPTATION, playerData.getFloat(NBT_ADAPTATION));
        coreData.setBoolean(NBT_TRANSCENDED, playerData.getBoolean(NBT_TRANSCENDED));
        coreData.setInteger(NBT_BLEEDING_TICKS, playerData.getInteger(NBT_BLEEDING_TICKS));
        coreData.setLong(NBT_LAST_STABILIZER, playerData.getLong(NBT_LAST_STABILIZER));

        // 清除髒標記
        playerData.setBoolean(NBT_DIRTY, false);
    }

    /**
     * ✅ 核心 → 玩家，僅在玩家尚未有資料時載入一次
     */
    private static void syncFromCoreIfEmpty(EntityPlayer player, ItemStack core) {
        if (core.isEmpty()) return;

        NBTTagCompound coreData   = getCoreRejectionData(core);
        NBTTagCompound playerData = getPlayerRejectionData(player);

        // 玩家已有資料就不覆蓋（避免把新狀態蓋回舊備份）
        if (playerData.hasKey(NBT_REJECTION)) return;
        if (!coreData.hasKey(NBT_REJECTION))  return;

        playerData.setFloat(NBT_REJECTION, coreData.getFloat(NBT_REJECTION));
        playerData.setFloat(NBT_ADAPTATION, coreData.getFloat(NBT_ADAPTATION));
        playerData.setBoolean(NBT_TRANSCENDED, coreData.getBoolean(NBT_TRANSCENDED));
        playerData.setInteger(NBT_BLEEDING_TICKS, coreData.getInteger(NBT_BLEEDING_TICKS));
        playerData.setLong(NBT_LAST_STABILIZER, coreData.getLong(NBT_LAST_STABILIZER));

        // 剛載入後狀態算乾淨
        playerData.setBoolean(NBT_DIRTY, false);
    }

    // ─────────────────  對外 API：查詢／設置基本數值  ─────────────────

    public static float getRejectionLevel(EntityPlayer player) {
        return getPlayerRejectionData(player).getFloat(NBT_REJECTION);
    }

    public static void setRejectionLevel(EntityPlayer player, float value) {
        NBTTagCompound playerData = getPlayerRejectionData(player);
        float clamped = MathHelper.clamp(value, 0f, (float) FleshRejectionConfig.maxRejection);
        playerData.setFloat(NBT_REJECTION, clamped);
        markDirty(playerData);
    }

    public static void reduceRejection(EntityPlayer player, float amount) {
        setRejectionLevel(player, getRejectionLevel(player) - amount);
    }

    public static float getAdaptationLevel(EntityPlayer player) {
        return getPlayerRejectionData(player).getFloat(NBT_ADAPTATION);
    }

    public static boolean hasTranscended(EntityPlayer player) {
        return getPlayerRejectionData(player).getBoolean(NBT_TRANSCENDED);
    }

    public static int getBleedingTicks(EntityPlayer player) {
        return getPlayerRejectionData(player).getInteger(NBT_BLEEDING_TICKS);
    }

    public static void setBleedingTicks(EntityPlayer player, int ticks) {
        NBTTagCompound playerData = getPlayerRejectionData(player);
        playerData.setInteger(NBT_BLEEDING_TICKS, Math.max(0, ticks));
        markDirty(playerData);
    }

    public static long getLastStabilizerUse(EntityPlayer player) {
        return getPlayerRejectionData(player).getLong(NBT_LAST_STABILIZER);
    }

    public static void setLastStabilizerUse(EntityPlayer player, long time) {
        NBTTagCompound playerData = getPlayerRejectionData(player);
        playerData.setLong(NBT_LAST_STABILIZER, time);
        markDirty(playerData);
    }

    // ─────────────────  模組統計部分  ─────────────────

    /**
     * 安裝的模組總等級（無論是否運行）
     */
    public static int getTotalInstalledModules(ItemStack core) {
        if (core.isEmpty()) return 0;

        int total = 0;

        // 傳統 UpgradeType
        for (UpgradeType type : UpgradeType.values()) {
            int level = ItemMechanicalCore.getUpgradeLevel(core, type);
            if (level > 0) total += level;
        }

        // 擴展模組
        Map<String, UpgradeInfo> allUpgrades = ItemMechanicalCoreExtended.getAllUpgrades();
        for (Map.Entry<String, UpgradeInfo> entry : allUpgrades.entrySet()) {
            String id    = entry.getKey();
            int level    = ItemMechanicalCoreExtended.getUpgradeLevel(core, id);
            if (level > 0) total += level;
        }

        return total;
    }

    /**
     * 實際運行中的模組總等級（被暫停的不算）
     */
    public static int getRunningModuleCount(ItemStack core) {
        if (core.isEmpty()) return 0;

        int count = 0;
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return 0;

        for (UpgradeType type : UpgradeType.values()) {
            String id  = type.getKey();
            int level  = ItemMechanicalCore.getUpgradeLevel(core, type);
            if (level > 0 && !isPaused(nbt, id)) {
                count += level;
            }
        }

        Map<String, UpgradeInfo> allUpgrades = ItemMechanicalCoreExtended.getAllUpgrades();
        for (Map.Entry<String, UpgradeInfo> entry : allUpgrades.entrySet()) {
            String id  = entry.getKey();
            int level  = ItemMechanicalCoreExtended.getUpgradeLevel(core, id);
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
        return nbt.getBoolean("IsPaused_" + id)
                || nbt.getBoolean("IsPaused_" + upper)
                || nbt.getBoolean("IsPaused_" + lower);
    }

    /**
     * 是否安裝神經同步器（任一系統的 key 符合即可）
     */
    public static boolean hasNeuralSynchronizer(ItemStack core) {
        if (core.isEmpty()) return false;

        // 先檢查傳統 UpgradeType
        for (UpgradeType type : UpgradeType.values()) {
            String key = type.getKey();
            if ("neural_synchronizer".equalsIgnoreCase(key)) {
                if (ItemMechanicalCore.getUpgradeLevel(core, type) > 0) {
                    return true;
                }
            }
        }

        // 再檢查擴展 ID
        if (ItemMechanicalCoreExtended.getUpgradeLevel(core, "neural_synchronizer") > 0) return true;
        if (ItemMechanicalCoreExtended.getUpgradeLevel(core, "NEURAL_SYNCHRONIZER") > 0) return true;

        //最後兜底檢查 NBT key（兼容舊資料）
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt != null) {
            if (nbt.getInteger("upgrade_neural_synchronizer") > 0) return true;
            if (nbt.getInteger("upgrade_NEURAL_SYNCHRONIZER") > 0) return true;
        }

        return false;
    }

    // ─────────────────  主更新邏輯（每秒呼叫一次）  ─────────────────

    /**
     * 每秒調用一次（伺服端）
     */
    public static void updateRejection(EntityPlayer player, ItemStack core) {
        if (player.world.isRemote) return;
        if (!FleshRejectionConfig.enableRejectionSystem) return;
        if (player.ticksExisted % 20 != 0) return;

        // 初次裝備時若玩家沒有資料，從核心載入備份
        syncFromCoreIfEmpty(player, core);

        NBTTagCompound playerData = getPlayerRejectionData(player);

        int installedCount   = getTotalInstalledModules(core);
        int runningCount     = getRunningModuleCount(core);
        boolean hasSync      = hasNeuralSynchronizer(core);
        boolean wasTranscend = playerData.getBoolean(NBT_TRANSCENDED);

        // 已經突破過的情況：只要適應度還夠，就維持「排異=0」
        float adaptation = calcAdaptation(installedCount, hasSync);
        if (wasTranscend) {
            if (adaptation < FleshRejectionConfig.adaptationThreshold) {
                // 適應度掉到門檻以下，重新啟動排異
                playerData.setBoolean(NBT_TRANSCENDED, false);
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "⚠⚠⚠ 警告：適應度下降 ⚠⚠⚠\n" +
                                TextFormatting.RED + "血肉排異反應重新激活！"
                ));
                player.world.playSound(
                        null,
                        player.getPosition(),
                        net.minecraft.init.SoundEvents.ENTITY_WITHER_SPAWN,
                        net.minecraft.util.SoundCategory.PLAYERS,
                        0.5f, 0.5f
                );
                markDirty(playerData);
            } else {
                // 適應度仍足夠：維持 zero rejection 並更新適應度
                playerData.setFloat(NBT_REJECTION, 0f);
                playerData.setFloat(NBT_ADAPTATION, adaptation);
                markDirty(playerData);
                // 結束時再做一次同步即可
                syncToCore(player, core);
                return;
            }
        }

        // === 正常排異成長 ===
        float currentRejection = playerData.getFloat(NBT_REJECTION);
        float growthPerSec     = (float) (runningCount * FleshRejectionConfig.rejectionGrowthRate);
        float newRejection     = currentRejection + growthPerSec;

        newRejection = MathHelper.clamp(newRejection, 0f, (float) FleshRejectionConfig.maxRejection);

        playerData.setFloat(NBT_REJECTION, newRejection);
        playerData.setFloat(NBT_ADAPTATION, adaptation);
        markDirty(playerData);

        if (FleshRejectionConfig.debugMode) {
            player.sendStatusMessage(
                    new TextComponentString(
                            String.format("§7[Rejection] 运行:%d 增长:%.2f/s 排异:%.1f 适应:%.0f",
                                    runningCount, growthPerSec, newRejection, adaptation)
                    ),
                    true
            );
        }

        // 適應度達門檻，觸發突破
        if (adaptation >= FleshRejectionConfig.adaptationThreshold && !wasTranscend) {
            transcendRejection(player, core);
            return; // transcendRejection 內會處理同步
        }

        // 處理出血效果（依照最新排異值）
        handleBleeding(player, newRejection);

        // 最後再統一同步一次（若 Dirty）
        syncToCore(player, core);
    }

    private static float calcAdaptation(int installedCount, boolean hasSynchronizer) {
        float base = (float) (installedCount * FleshRejectionConfig.adaptationPerModule);
        if (hasSynchronizer) {
            base += FleshRejectionConfig.neuralSynchronizerBonus;
        }
        return base;
    }

    // ─────────────────  突破 / 粒子演出  ─────────────────

    private static void transcendRejection(EntityPlayer player, ItemStack core) {
        NBTTagCompound playerData = getPlayerRejectionData(player);
        playerData.setBoolean(NBT_TRANSCENDED, true);
        playerData.setFloat(NBT_REJECTION, 0f);
        playerData.setInteger(NBT_BLEEDING_TICKS, 0);
        markDirty(playerData);

        syncToCore(player, core);

        player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "═══════════════════════════\n" +
                        TextFormatting.BOLD  + "" + TextFormatting.AQUA + "[Neural Sync Complete]\n" +
                        TextFormatting.GRAY  + "血肉排異已終止。\n" +
                        TextFormatting.AQUA + "═══════════════════════════"
        ));

        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;

            // 外側火花
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

            // 環形 END_ROD
            for (int i = 0; i < 30; i++) {
                double angle = (i / 30.0) * Math.PI * 2;
                double x     = player.posX + Math.cos(angle) * 2;
                double z     = player.posZ + Math.sin(angle) * 2;

                world.spawnParticle(
                        EnumParticleTypes.END_ROD,
                        x, player.posY + 1, z,
                        1, 0, 0.2, 0.0, 0
                );
            }
        }
    }

    // ─────────────────  出血效果  ─────────────────

    private static void handleBleeding(EntityPlayer player, float rejection) {
        if (!FleshRejectionConfig.enableBleeding) return;

        NBTTagCompound playerData = getPlayerRejectionData(player);
        int bleedTicks = playerData.getInteger(NBT_BLEEDING_TICKS);

        if (bleedTicks <= 0) return;

        // 每秒扣血一次
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
            markDirty(playerData);
        }

        playerData.setInteger(NBT_BLEEDING_TICKS, bleedTicks - 1);
    }

    public static void triggerBleeding(EntityPlayer player, int durationTicks) {
        if (!FleshRejectionConfig.enableBleeding) return;
        int current = getBleedingTicks(player);
        setBleedingTicks(player, Math.max(current, durationTicks));
    }

    // ─────────────────  玩家死亡傳承處理  ─────────────────

    public static void handlePlayerDeath(EntityPlayer oldPlayer, EntityPlayer newPlayer) {
        if (oldPlayer.world.isRemote) return;
        if (!FleshRejectionConfig.enableRejectionSystem) return;

        NBTTagCompound oldData = getPlayerRejectionData(oldPlayer);
        NBTTagCompound newData = getPlayerRejectionData(newPlayer);

        boolean wasTranscended = oldData.getBoolean(NBT_TRANSCENDED);

        // 突破狀態是否保留
        if (FleshRejectionConfig.keepTranscendenceOnDeath) {
            newData.setBoolean(NBT_TRANSCENDED, wasTranscended);
        } else {
            newData.setBoolean(NBT_TRANSCENDED, false);
        }

        if (wasTranscended && FleshRejectionConfig.keepTranscendenceOnDeath) {
            newData.setFloat(NBT_REJECTION, 0f);
        } else {
            float oldRejection = oldData.getFloat(NBT_REJECTION);
            float newRejection = (float) (oldRejection * FleshRejectionConfig.deathRejectionRetention);
            newRejection = Math.max(newRejection, (float) FleshRejectionConfig.minRejectionAfterDeath);
            newData.setFloat(NBT_REJECTION, newRejection);

            if (!newPlayer.world.isRemote && oldRejection > FleshRejectionConfig.stabilizerMinRejection) {
                int lostPercent = (int) ((1.0 - FleshRejectionConfig.deathRejectionRetention) * 100);
                newPlayer.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "死亡使排異值降低了 " + lostPercent + "%"
                ));
            }
        }

        // 保留適應度
        newData.setFloat(NBT_ADAPTATION, oldData.getFloat(NBT_ADAPTATION));

        // 清空臨時狀態
        newData.setInteger(NBT_BLEEDING_TICKS, 0);
        newData.setLong(NBT_LAST_STABILIZER, 0);
        newData.setFloat("EventRejectionBonus", 0f);
        // 新資料已修改，標記髒
        newData.setBoolean(NBT_DIRTY, true);

        if (FleshRejectionConfig.debugMode) {
            newPlayer.sendMessage(new TextComponentString(
                    String.format("§7[Rejection] 死亡傳承: 旧排异 %.1f → 新排异 %.1f",
                            oldData.getFloat(NBT_REJECTION),
                            newData.getFloat(NBT_REJECTION))
            ));
        }
    }

    // ─────────────────  狀態摘要（HUD / Debug 用）  ─────────────────

    public static RejectionStatus getStatus(EntityPlayer player) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return null;

        RejectionStatus status = new RejectionStatus();
        status.installed      = getTotalInstalledModules(core);
        status.running        = getRunningModuleCount(core);
        status.rejection      = getRejectionLevel(player);
        status.adaptation     = getAdaptationLevel(player);
        status.transcended    = hasTranscended(player);
        status.hasSynchronizer= hasNeuralSynchronizer(core);
        status.bleeding       = getBleedingTicks(player);
        status.growthRate     = (float) (status.running * FleshRejectionConfig.rejectionGrowthRate);

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
                    "已安裝等級:%d | 運行等級:%d | 排異:%.1f (+%.2f/s) | 適應:%.0f | 突破:%s",
                    installed, running, rejection, growthRate, adaptation, transcended ? "是" : "否"
            );
        }
    }
}
