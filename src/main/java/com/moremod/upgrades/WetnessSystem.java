package com.moremod.upgrades;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.potion.ModPotions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 潮濕值系統 - 處理雨天和濕度管理
 * 優化版：每秒更新，給玩家20-30秒反應時間
 */
public class WetnessSystem {

    // 潮濕值常量 (每秒更新系統)
    private static final int MAX_WETNESS = 100;
    private static final int UPDATE_INTERVAL = 20;       // 每20 ticks (1秒) 更新一次
    private static final int RAIN_WETNESS_PER_SEC = 4;   // 普通雨每秒+4 (25秒滿)
    private static final int THUNDER_WETNESS_PER_SEC = 5; // 雷雨每秒+5 (20秒滿)
    private static final int NATURAL_DRY_PER_SEC = 2;    // 自然乾燥每秒-2 (50秒完全乾燥)
    private static final int HEAT_DRY_PER_SEC = 4;       // 高溫乾燥每秒-4 (25秒完全乾燥)
    private static final int WETNESS_MALFUNCTION_THRESHOLD = 80; // 故障閾值 (普通雨20秒達到)
    private static final int DRYING_DELAY_SECONDS = 3;   // 離開雨3秒後開始乾燥

    // 玩家潮濕值追踪
    private static final Map<UUID, Integer> playerWetness = new HashMap<>();
    private static final Map<UUID, Integer> updateTickCounter = new HashMap<>(); // tick計數器
    private static final Map<UUID, Integer> dryingDelayCounter = new HashMap<>(); // 乾燥延遲計數器
    private static final Map<UUID, Long> malfunctionStartTime = new HashMap<>();
    private static final Map<UUID, Integer> currentMalfunctionLevel = new HashMap<>();

    // SimpleDifficulty反射緩存
    private static boolean sdLoaded = Loader.isModLoaded("simpledifficulty");
    private static Object temperatureCapability; // 緩存 Capability 對象
    private static Method getTemperatureLevelMethod;

    static {
        if (sdLoaded) {
            try {
                // 只反射獲取 SimpleDifficulty 的類和 Capability
                Class<?> sdCapabilities = Class.forName("com.charles445.simpledifficulty.api.SDCapabilities");
                temperatureCapability = sdCapabilities.getField("TEMPERATURE").get(null);

                // 反射獲取溫度接口的方法
                Class<?> tempCapabilityClass = Class.forName("com.charles445.simpledifficulty.api.temperature.ITemperatureCapability");
                getTemperatureLevelMethod = tempCapabilityClass.getMethod("getTemperatureLevel");

                System.out.println("[WetnessSystem] SimpleDifficulty 整合成功");
            } catch (Exception e) {
                System.err.println("[WetnessSystem] SimpleDifficulty 初始化失敗: " + e);
                sdLoaded = false;
            }
        }
    }

    /**
     * 每tick調用，但內部每秒才更新一次（性能優化）
     */
    public static void updateWetness(EntityPlayer player, ItemStack coreStack) {
        if (player.world.isRemote) return;

        // 確認是機械核心
        if (!ItemMechanicalCore.isMechanicalCore(coreStack)) return;

        UUID playerId = player.getUniqueID();

        // 累加tick計數器
        int tickCount = updateTickCounter.getOrDefault(playerId, 0) + 1;
        updateTickCounter.put(playerId, tickCount);

        // 每20 ticks (1秒) 才真正更新
        if (tickCount < UPDATE_INTERVAL) {
            // 非更新週期，只檢查故障效果
            checkMalfunctionEffects(player, coreStack);
            return;
        }

        // 重置計數器
        updateTickCounter.put(playerId, 0);

        // === 每秒更新邏輯 ===
        int currentWetness = playerWetness.getOrDefault(playerId, 0);
        int waterproofLevel = WaterproofUpgrade.getEffectiveWaterproofLevel(coreStack);

        // 檢查是否淋雨
        boolean isInRain = isPlayerInRain(player);
        boolean isThundering = player.world.isThundering();

        if (isInRain) {
            // 淋雨時增加潮濕值
            handleRainWetness(player, currentWetness, isThundering, waterproofLevel);
            // 重置乾燥延遲
            dryingDelayCounter.put(playerId, 0);
        } else {
            // 不在雨中時處理乾燥延遲和自然乾燥
            handleDrying(player, currentWetness);
            // 離開雨中，重置故障升級計時
            malfunctionStartTime.remove(playerId);
        }

        // 檢查是否需要施加故障效果
        currentWetness = playerWetness.getOrDefault(playerId, 0);
        if (currentWetness >= WETNESS_MALFUNCTION_THRESHOLD && waterproofLevel < 2) {
            applyWetnessMalfunction(player, coreStack, currentWetness, isInRain);
        } else {
            // 清除故障記錄
            currentMalfunctionLevel.remove(playerId);
            malfunctionStartTime.remove(playerId);
        }

        // 定期顯示狀態（每5秒）
        if (tickCount % 100 == 0 && currentWetness >= WETNESS_MALFUNCTION_THRESHOLD) {
            displayCriticalWarning(player, currentWetness);
        }
    }

    /**
     * 非更新週期時檢查故障效果（確保效果不中斷）
     */
    private static void checkMalfunctionEffects(EntityPlayer player, ItemStack coreStack) {
        UUID playerId = player.getUniqueID();
        int currentWetness = playerWetness.getOrDefault(playerId, 0);
        int waterproofLevel = WaterproofUpgrade.getEffectiveWaterproofLevel(coreStack);

        if (currentWetness >= WETNESS_MALFUNCTION_THRESHOLD && waterproofLevel < 2) {
            // 確保故障效果持續
            if (!player.isPotionActive(ModPotions.MALFUNCTION)) {
                int level = currentMalfunctionLevel.getOrDefault(playerId, 0);
                player.addPotionEffect(new PotionEffect(
                        ModPotions.MALFUNCTION,
                        40, // 2秒持續時間
                        level,
                        false,
                        true
                ));
            }
        }
    }

    /**
     * 檢查玩家是否在雨中
     */
    private static boolean isPlayerInRain(EntityPlayer player) {
        World world = player.world;
        if (!world.isRaining()) return false;

        // 檢查玩家上方是否有遮擋
        return world.canSeeSky(player.getPosition()) &&
                world.getPrecipitationHeight(player.getPosition()).getY() <= player.posY;
    }

    /**
     * 處理雨中潮濕值增加（每秒更新，LV1減50%，LV2+免疫）
     */
    private static void handleRainWetness(EntityPlayer player, int currentWetness, boolean isThundering, int waterproofLevel) {
        UUID playerId = player.getUniqueID();

        // 計算潮濕值增加量（每秒）
        int wetnessGain = isThundering ? THUNDER_WETNESS_PER_SEC : RAIN_WETNESS_PER_SEC;

        // 防水模塊效果
        if (waterproofLevel >= 2) {
            // LV2及以上：完全免疫
            wetnessGain = 0;

            // 每5秒顯示一次防護提示
            if (player.ticksExisted % 100 == 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.AQUA + "✓ 防水模組運作中 - 完全防護"
                ), true);
            }
        } else if (waterproofLevel == 1) {
            // LV1：減少50%
            wetnessGain = wetnessGain / 2;

            if (player.ticksExisted % 100 == 0 && currentWetness > 20) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.YELLOW + "⚡ 防水LV1 - 部分防護(50%)"
                ), true);
            }
        }

        // 更新潮濕值
        int newWetness = Math.min(MAX_WETNESS, currentWetness + wetnessGain);
        playerWetness.put(playerId, newWetness);

        // 首次淋雨警告
        if (currentWetness < 20 && newWetness >= 20) {
            String rainType = isThundering ? "雷雨" : "雨";
            TextFormatting color = waterproofLevel > 0 ? TextFormatting.AQUA : TextFormatting.YELLOW;
            String protection = waterproofLevel >= 2 ? " [完全防護]" :
                    waterproofLevel == 1 ? " [部分防護]" : "";

            // 計算預估到達故障時間
            int timeToMalfunction = (WETNESS_MALFUNCTION_THRESHOLD - newWetness) / (wetnessGain > 0 ? wetnessGain : 1);

            player.sendMessage(new TextComponentString(
                    color + "☔ 檢測到" + rainType + "，機械核心開始受潮" + protection +
                            TextFormatting.GRAY + " (約" + timeToMalfunction + "秒後故障)"
            ));
        }

        // 接近故障閾值警告
        if (currentWetness < WETNESS_MALFUNCTION_THRESHOLD && newWetness >= WETNESS_MALFUNCTION_THRESHOLD) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "⚠ 警告：潮濕值達到臨界，機械核心開始故障！"
            ));
        }

        // 接近最大值警告
        if (currentWetness < 90 && newWetness >= 90) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 危險：潮濕值極高！快找避雨處！"
            ));
        }
    }

    /**
     * 處理乾燥過程（延遲3秒後開始，每秒更新）
     */
    private static void handleDrying(EntityPlayer player, int currentWetness) {
        if (currentWetness <= 0) return;

        UUID playerId = player.getUniqueID();

        // 增加乾燥延遲計數器（每秒+1）
        int delayCount = dryingDelayCounter.getOrDefault(playerId, 0) + 1;
        dryingDelayCounter.put(playerId, delayCount);

        // 延遲3秒後開始乾燥
        if (delayCount < DRYING_DELAY_SECONDS) {
            return;
        }

        // 計算乾燥速率（每秒）
        int dryRate = NATURAL_DRY_PER_SEC;

        // 高溫加速乾燥
        if (sdLoaded) {
            int temp = getPlayerTemperature(player);
            if (temp > 15) {
                dryRate = HEAT_DRY_PER_SEC;
            } else if (temp > 20) {
                dryRate = HEAT_DRY_PER_SEC * 2;
            }
        }

        // 更新潮濕值
        int newWetness = Math.max(0, currentWetness - dryRate);
        playerWetness.put(playerId, newWetness);

        // 降到安全值時清除故障效果
        if (currentWetness >= WETNESS_MALFUNCTION_THRESHOLD && newWetness < WETNESS_MALFUNCTION_THRESHOLD) {
            if (player.isPotionActive(ModPotions.MALFUNCTION)) {
                player.removePotionEffect(ModPotions.MALFUNCTION);
            }
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 潮濕值已降至安全範圍，故障解除"
            ));
        }

        // 完全乾燥通知
        if (currentWetness > 0 && newWetness == 0) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 機械核心已完全乾燥"
            ), true);
            // 清理所有狀態
            dryingDelayCounter.remove(playerId);
        }
    }

    /**
     * 獲取玩家溫度（SimpleDifficulty）
     */
    private static int getPlayerTemperature(EntityPlayer player) {
        if (!sdLoaded || temperatureCapability == null || getTemperatureLevelMethod == null) {
            return 12; // 預設正常溫度
        }

        try {
            // 直接調用 getCapability（不用反射）
            Object tempCap = player.getCapability((net.minecraftforge.common.capabilities.Capability)temperatureCapability, null);

            if (tempCap != null) {
                // 只對溫度值的獲取使用反射
                return (int) getTemperatureLevelMethod.invoke(tempCap);
            }
        } catch (Exception e) {
            // 靜默失敗
        }

        return 12;
    }

    /**
     * 施加潮濕故障效果（隨時間升級）
     */
    private static void applyWetnessMalfunction(EntityPlayer player, ItemStack coreStack, int wetness, boolean stillInRain) {
        UUID playerId = player.getUniqueID();

        // 初始化故障開始時間
        if (!malfunctionStartTime.containsKey(playerId)) {
            malfunctionStartTime.put(playerId, System.currentTimeMillis());
            currentMalfunctionLevel.put(playerId, 0);
        }

        // 計算故障持續時間
        long duration = System.currentTimeMillis() - malfunctionStartTime.get(playerId);
        int secondsInMalfunction = (int)(duration / 1000);

        // 基礎故障等級
        int baseLevel = 0;
        if (wetness >= MAX_WETNESS) {
            baseLevel = 1;
        } else if (wetness >= 90) {
            baseLevel = 0;
        }

        // 如果還在雨中且潮濕值已滿，故障隨時間升級
        int timeBonus = 0;
        if (stillInRain && wetness >= MAX_WETNESS) {
            if (secondsInMalfunction > 30) { // 30秒後升至LV2
                timeBonus = 1;

                // 升級提醒
                int currentLevel = currentMalfunctionLevel.getOrDefault(playerId, 0);
                if (currentLevel < baseLevel + timeBonus) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "⚠ 故障加劇！持續淋雨導致系統惡化！"
                    ));
                }
            }
            if (secondsInMalfunction > 60) { // 60秒後升至LV3
                timeBonus = 2;

                int currentLevel = currentMalfunctionLevel.getOrDefault(playerId, 0);
                if (currentLevel < baseLevel + timeBonus) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_RED + "☠ 嚴重故障！立即尋找避雨處！"
                    ));
                }
            }
        }

        int malfunctionLevel = Math.min(2, baseLevel + timeBonus);
        currentMalfunctionLevel.put(playerId, malfunctionLevel);

        // 施加故障效果（較長持續時間，因為是每秒更新）
        player.addPotionEffect(new PotionEffect(
                ModPotions.MALFUNCTION,
                40, // 2秒持續時間（確保覆蓋到下次更新）
                malfunctionLevel,
                false,
                true
        ));

        // 消耗能量（根據故障等級增加，改為每秒消耗）
        IEnergyStorage energy = coreStack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            int drain = 50 * (malfunctionLevel + 1); // 每秒消耗
            energy.extractEnergy(drain, false);
        }

        // 隨機短路（每秒檢查一次，故障等級越高機率越大）
        int shortCircuitChance = 15 - (malfunctionLevel * 4); // 15%/11%/7%
        if (wetness >= MAX_WETNESS && player.world.rand.nextInt(100) < shortCircuitChance) {
            if (energy != null) {
                int shortCircuit = energy.getEnergyStored() / (4 - malfunctionLevel);
                energy.extractEnergy(shortCircuit, false);
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "⚡ 潮濕短路！損失 " + shortCircuit + " FE [故障LV" + (malfunctionLevel+1) + "]"
                ));
            }
        }
    }

    /**
     * 顯示關鍵警告
     */
    private static void displayCriticalWarning(EntityPlayer player, int wetness) {
        UUID playerId = player.getUniqueID();
        int malfLvl = currentMalfunctionLevel.getOrDefault(playerId, 0);

        String status;
        TextFormatting color;

        if (malfLvl >= 2) {
            status = "☠ 嚴重故障 LV" + (malfLvl + 1);
            color = TextFormatting.DARK_RED;
        } else if (malfLvl >= 1) {
            status = "⚠ 中度故障 LV" + (malfLvl + 1);
            color = TextFormatting.RED;
        } else {
            status = "⚡ 輕微故障";
            color = TextFormatting.YELLOW;
        }

        player.sendStatusMessage(new TextComponentString(
                color + status + " - 潮濕度: " + wetness + "%"
        ), true);
    }

    /**
     * 使用毛巾降低潮濕值
     */
    public static boolean useTowel(EntityPlayer player, int dryAmount) {
        UUID playerId = player.getUniqueID();
        int currentWetness = playerWetness.getOrDefault(playerId, 0);

        if (currentWetness <= 0) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "你已經很乾燥了"
            ), true);
            return false;
        }

        if (isPlayerInRain(player)) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "不能在雨中使用毛巾！"
            ), true);
            return false;
        }

        // 減少潮濕值
        int newWetness = Math.max(0, currentWetness - dryAmount);
        playerWetness.put(playerId, newWetness);

        // 如果降到閾值以下，清除故障記錄
        if (newWetness < WETNESS_MALFUNCTION_THRESHOLD) {
            malfunctionStartTime.remove(playerId);
            currentMalfunctionLevel.remove(playerId);

            if (player.isPotionActive(ModPotions.MALFUNCTION)) {
                player.removePotionEffect(ModPotions.MALFUNCTION);
            }
        }

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.AQUA + "使用毛巾擦拭 (潮濕度: " + currentWetness + "% → " + newWetness + "%)"
        ), true);

        return true;
    }

    /**
     * 獲取玩家當前潮濕值
     */
    public static int getWetness(EntityPlayer player) {
        return playerWetness.getOrDefault(player.getUniqueID(), 0);
    }

    /**
     * 獲取玩家當前故障等級
     */
    public static int getMalfunctionLevel(EntityPlayer player) {
        return currentMalfunctionLevel.getOrDefault(player.getUniqueID(), 0);
    }

    /**
     * 清理玩家數據
     */
    public static void cleanupPlayer(EntityPlayer player) {
        UUID id = player.getUniqueID();
        playerWetness.remove(id);
        updateTickCounter.remove(id);
        dryingDelayCounter.remove(id);
        malfunctionStartTime.remove(id);
        currentMalfunctionLevel.remove(id);
    }
}