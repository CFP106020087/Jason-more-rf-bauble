package com.moremod.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

/**
 * MoreMod 物品配置管理类（手动 Configuration 版）
 * 统一文件：config/moremod/itemconfig.cfg
 */
public class ItemConfig {

    private static Configuration config;
    private static volatile boolean loaded = false;

    // ===== 村民职业转换工具配置 =====
    public static class VillagerTransformer {
        public static int maxEnergy = 100000;
        public static int energyPerUse = 5000;
        public static double failureChance = 0.60;
        public static int maxTransformAttempts = 20;
    }

    // ===== 商人说服器配置 =====
    public static class MerchantPersuader {
        public static int maxEnergy = 1000000;
        public static int energyPerTrade = 1000;
        public static double baseDiscount = 0.15;
        public static double maxDiscount = 0.50;
        public static double range = 12.0;
    }

    // ===== 机械外骨骼配置 =====
    public static class MechanicalExoskeleton {
        public static int minActiveModules = 6;
        public static boolean countGenerators = false;
        public static float damageBonus = 0.08f;     // 每个激活等级的伤害加成
        public static float energySyncBonus = 0.02f; // 能效同步加成（每级）
    }

    // ===== 铜质许愿骨配置 =====
    public static class CopperWishbone {
        public static int requiredActiveModules = 4;
        public static int minLevelForLuck = 3;
        public static int maxLuckPoints = 30;
    }

    // ===== 外置内循环系统配置 =====
    public static class ExternalCirculation {
        public static int requiredActiveModules = 5;
        public static float healthPerModule = 2.0f;
        public static float maxBonusHealth = 40.0f;
    }

    // ===== 时间撕裂手套配置 =====
    public static class TemporalRiftGlove {
        public static int requiredActiveModules = 6;
        public static int modulesPerReduction = 5;
        public static int iframeReductionPerTier = 2;
        public static int maxIframeReduction = 8;
    }

    /**
     * 初始化配置（在 PreInit 调用）
     */
    public static void init(FMLPreInitializationEvent event) {
        File cfg = new File(event.getModConfigurationDirectory(), "moremod/itemconfig.cfg");
        config = new Configuration(cfg);
        loadConfig();
    }

    /**
     * 确保配置已加载（供其他模块在运行期调用）
     */
    public static void ensureLoaded() {
        if (loaded) return;
        File cfg = new File(Loader.instance().getConfigDir(), "moremod/itemconfig.cfg");
        config = new Configuration(cfg);
        loadConfig();
    }

    /**
     * 加载所有配置
     */
    private static void loadConfig() {
        try {
            config.load();

            // ===== 村民职业转换工具 =====
            String category = "villager_transformer";
            VillagerTransformer.maxEnergy = config.get(category, "maxEnergy", 100000,
                    "村民职业转换工具的最大能量存储量", 10000, 10000000).getInt();
            VillagerTransformer.energyPerUse = config.get(category, "energyPerUse", 5000,
                    "每次转换村民职业消耗的能量", 100, 50000).getInt();
            VillagerTransformer.failureChance = config.get(category, "failureChance", 0.60D,
                    "转换失败变成傻子的概率（0.0-1.0）", 0.0D, 1.0D).getDouble();
            VillagerTransformer.maxTransformAttempts = config.get(category, "maxTransformAttempts", 20,
                    "尝试生成特定职业的最大次数", 1, 100).getInt();

            // ===== 商人说服器 =====
            category = "merchant_persuader";
            MerchantPersuader.maxEnergy = config.get(category, "maxEnergy", 1000000,
                    "商人说服器的最大能量存储量", 10000, 10000000).getInt();
            MerchantPersuader.energyPerTrade = config.get(category, "energyPerTrade", 1000,
                    "每次交易消耗的能量", 10, 10000).getInt();
            MerchantPersuader.baseDiscount = config.get(category, "baseDiscount", 0.15D,
                    "基础折扣率（0.0-1.0）", 0.0D, 1.0D).getDouble();
            MerchantPersuader.maxDiscount = config.get(category, "maxDiscount", 0.50D,
                    "满能量时的最大折扣率（0.0-1.0）", 0.0D, 1.0D).getDouble();
            MerchantPersuader.range = config.get(category, "range", 12.0D,
                    "影响范围（格）", 1.0D, 32.0D).getDouble();

            // ===== 机械外骨骼 =====
            category = "mechanical_exoskeleton";
            MechanicalExoskeleton.minActiveModules = config.get(category, "minActiveModules", 6,
                    "佩戴外骨骼需要的最低激活模块等级总和", 0, 999).getInt();
            MechanicalExoskeleton.countGenerators = config.get(category, "countGenerators", false,
                    "是否把发电/充能类模块计入激活模块数").getBoolean();
            MechanicalExoskeleton.damageBonus = (float) config.get(category, "damageBonusPerLevel", 0.08D,
                    "每个激活等级提供的伤害加成倍率", 0.01D, 1.0D).getDouble();
            MechanicalExoskeleton.energySyncBonus = (float) config.get(category, "energySyncBonusPerLevel", 0.02D,
                    "能量效率每级提供的额外伤害加成", 0.0D, 0.1D).getDouble();

            // ===== 铜质许愿骨 =====
            category = "copper_wishbone";
            CopperWishbone.requiredActiveModules = config.get(category, "requiredActiveModules", 4,
                    "佩戴许愿骨需要的激活模块数量", 1, 20).getInt();
            CopperWishbone.minLevelForLuck = config.get(category, "minLevelForLuck", 3,
                    "提供幸运加成的最低模块等级", 1, 10).getInt();
            CopperWishbone.maxLuckPoints = config.get(category, "maxLuckPoints", 30,
                    "最大幸运点数上限", 1, 100).getInt();

            // ===== 外置内循环系统 =====
            category = "external_circulation";
            ExternalCirculation.requiredActiveModules = config.get(category, "requiredActiveModules", 5,
                    "佩戴内循环系统需要的激活模块数量", 1, 20).getInt();
            ExternalCirculation.healthPerModule = (float) config.get(category, "healthPerModule", 2.0D,
                    "每个激活模块提供的生命值", 0.5D, 10.0D).getDouble();
            ExternalCirculation.maxBonusHealth = (float) config.get(category, "maxBonusHealth", 40.0D,
                    "最大额外生命值上限", 10.0D, 200.0D).getDouble();

            // ===== 时间撕裂手套 =====
            category = "temporal_rift_glove";
            TemporalRiftGlove.requiredActiveModules = config.get(category, "requiredActiveModules", 6,
                    "佩戴时间撕裂手套需要的激活模块数量", 1, 20).getInt();
            TemporalRiftGlove.modulesPerReduction = config.get(category, "modulesPerReduction", 5,
                    "每多少个模块提供一次无敌帧削减", 1, 10).getInt();
            TemporalRiftGlove.iframeReductionPerTier = config.get(category, "iframeReductionPerTier", 2,
                    "每层级减少的无敌帧数", 1, 10).getInt();
            TemporalRiftGlove.maxIframeReduction = config.get(category, "maxIframeReduction", 8,
                    "最大无敌帧削减量", 1, 20).getInt();

            loaded = true;

        } catch (Exception e) {
            System.err.println("[MoreMod] Error loading configuration: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (config != null && config.hasChanged()) {
                config.save();
            }
        }
    }

    /** 保存配置 */
    public static void save() {
        if (config != null && config.hasChanged()) {
            config.save();
        }
    }

    /** 重新加载配置（运行期刷新） */
    public static void reload() {
        if (config != null) {
            loaded = false;
            loadConfig();
        }
    }
}
