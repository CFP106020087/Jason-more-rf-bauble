package com.moremod.config;

/**
 * 能量平衡配置类 - 电池依赖型设计
 *
 * 核心设计理念：
 * - 25种模块，总等级60-125级时消耗巨大
 * - 后期即使有最强发电设备，也只能支撑约50%的模块
 * - 必须配合充电电池才能全开所有模块
 * - 电池不仅是缓冲，更是额外能源供应
 *
 * 游戏阶段设计：
 * 初期(1-15级): 基础核心，无电池，2-3个低级模块
 * 中期(15-40级): 简易电池，5-8个模块
 * 后期(40-75级): 高级电池必需，15个模块
 * 终极(75-125级): 量子电池组，25个模块全开
 */
public class EnergyBalanceConfig {

    // ====== 基础容量配置 ======
    /** 机械核心基础能量容量 - 较小的内置容量 */
    public static int BASE_ENERGY_CAPACITY = 20_000;        // 小容量
    /** 每级容量升级的提升值 */
    public static int ENERGY_PER_CAPACITY_LEVEL = 5_000;    // 每级+5k
    /** 基础充放电传输速率 */
    public static int BASE_ENERGY_TRANSFER = 2_000;         // 基础传输

    // ====== 核心消耗机制 ======

    /** 核心待机消耗 - 始终存在 */
    public static int CORE_IDLE_DRAIN = 5;  // RF/秒

    /** 有任何升级时的基础运行消耗 */
    public static int BASE_PASSIVE_DRAIN = 20;  // 基础消耗

    // ===== 被动消耗配置（每级）- 相对温和但累加可观 =====

    /** 基础升级被动消耗（每级）*/
    public static final class BasicUpgrades {
        public static int ARMOR_ENHANCEMENT   = 15;   // Lv5=75 RF/s
        public static int SPEED_BOOST         = 20;   // Lv5=100 RF/s
        public static int SHIELD_GENERATOR    = 35;   // Lv5=175 RF/s
        public static int TEMPERATURE_CONTROL = 10;   // Lv5=50 RF/s
        public static int FLIGHT_MODULE       = 50;   // Lv3=150 RF/s
        public static int REGENERATION        = 25;   // Lv5=125 RF/s
        public static int ENERGY_EFFICIENCY   = 5;    // Lv5=25 RF/s（效率模块也有成本）
        public static int ENERGY_CAPACITY     = 3;    // Lv10=30 RF/s（容量扩展维护）
    }

    /** 扩展升级被动消耗（每级）*/
    public static final class ExtendedUpgrades {
        // 生存类（7种）
        public static int YELLOW_SHIELD    = 40;   // Lv3=120 RF/s
        public static int HEALTH_REGEN     = 45;   // Lv3=135 RF/s
        public static int HUNGER_THIRST    = 15;   // Lv3=45 RF/s
        public static int THORNS           = 12;   // Lv3=36 RF/s
        public static int FIRE_EXTINGUISH  = 8;    // Lv3=24 RF/s
        public static int WATERPROOF_MODULE= 10;   // Lv3=30 RF/s
        public static int POISON_IMMUNITY   = 18;   // Lv3=54 RF/s

        // 辅助类（5种）
        public static int MOVEMENT_SPEED   = 30;   // Lv5=150 RF/s
        public static int STEALTH          = 0;    // 主动消耗
        public static int ORE_VISION       = 0;    // 主动消耗
        public static int NIGHT_VISION     = 10;   // Lv3=30 RF/s
        public static int WATER_BREATHING  = 12;   // Lv3=36 RF/s

        // 战斗类（5种）
        public static int DAMAGE_BOOST     = 25;   // Lv5=125 RF/s
        public static int ATTACK_SPEED     = 20;   // Lv5=100 RF/s
        public static int RANGE_EXTENSION  = 15;   // Lv5=75 RF/s
        public static int PURSUIT          = 22;   // Lv3=66 RF/s
        public static int CRITICAL_STRIKE  = 18;   // Lv5=90 RF/s

        // 实用类（2种）
        public static int EXP_AMPLIFIER    = 8;    // Lv5=40 RF/s
        public static int ITEM_MAGNET      = 10;   // Lv3=30 RF/s

        // 发电类（4种）- 有维护成本
        public static int KINETIC_GENERATOR = 5;   // Lv3=15 RF/s
        public static int SOLAR_GENERATOR   = 8;   // Lv3=24 RF/s
        public static int VOID_ENERGY       = 12;  // Lv3=36 RF/s
        public static int COMBAT_CHARGER    = 10;  // Lv3=30 RF/s
    }

    // ===== 主动消耗配置（使用时额外消耗）=====

    /** 辅助类主动消耗 */
    public static final class AuxiliaryActive {
        public static int ORE_VISION_BASE       = 200;   // 基础扫描
        public static int ORE_VISION_PER_LEVEL  = 50;    // 每级额外
        public static int ORE_VISION_SCAN       = 500;   // 深度扫描
        public static int STEALTH_LEVEL_1       = 800;   // RF/秒
        public static int STEALTH_LEVEL_2       = 600;
        public static int STEALTH_LEVEL_3       = 400;
        public static int EXP_AMPLIFIER_BASE    = 20;
        public static int EXP_AMPLIFIER_MULTIPLIER = 3;
    }

    /** 战斗类主动消耗 */
    public static final class CombatActive {
        public static int DAMAGE_BOOST_PER_HIT = 100;
        public static int CRITICAL_STRIKE      = 50;
        public static int PURSUIT_MARK         = 30;
        public static int PURSUIT_DASH         = 200;
        public static int RANGE_INDICATOR      = 10;
    }

    /** 生存类主动消耗 */
    public static final class SurvivalActive {
        public static int SHIELD_MAINTAIN_PER_LEVEL = 40;
        public static int SHIELD_RESTORE_PER_POINT  = 20;
        public static int HEALTH_REGEN_PER_LEVEL    = 60;
        public static int HUNGER_RESTORE            = 50;
        public static int THIRST_RESTORE            = 50;
        public static int FIRE_EXTINGUISH           = 200;
    }

    // ===== 能量产生配置 - 设计为最多支撑50%全开模块 =====

    /** 动能发电 - 移动发电 */
    public static final class KineticGenerator {
        public static int  ENERGY_PER_BLOCK  = 5;     // 每格5 RF
        public static int  ENERGY_PER_LEVEL  = 8;     // 每级+8
        public static float SPRINT_MULTIPLIER = 1.5f;
        public static float ELYTRA_MULTIPLIER = 2.0f;
        public static float JUMP_MULTIPLIER   = 1.2f;
        public static int  BLOCK_BREAK_BASE   = 10;
        public static int  BUFFER_THRESHOLD   = 500; // 维持不变
        // Lv3满级：约 90-180 RF/s（跑动）
    }

    /** 太阳能发电 - 日间发电 */
    public static final class SolarGenerator {
        public static int   ENERGY_PER_LEVEL = 40;    // 每级40 RF/s
        public static float HEIGHT_BONUS_MAX = 1.3f;
        public static float RAIN_PENALTY     = 0.4f;
        public static float STORM_PENALTY    = 0.2f;
        public static int   MIN_SKY_LIGHT    = 12;
        // Lv3满级：约 120-156 RF/s（晴天高处）
    }

    /** 虚空能量 - 深层/末地发电 */
    public static final class VoidEnergy {
        public static int   CHARGE_PER_TICK = 2;      // 每tick充能
        public static int   DEEP_Y_LEVEL    = 20;
        public static int   VOID_Y_LEVEL    = 0;
        public static int   CHARGE_CONVERSION = 25;   // 25充能=1RF
        public static int   END_BONUS       = 80;     // 末地额外
        public static float END_MULTIPLIER = 1.5f;
        // Lv3满级：约 150-200 RF/s（末地）
    }

    /** 战斗充能 - 击杀充能 */
    public static final class CombatCharger {
        public static int   ENERGY_PER_HP      = 20;
        public static float MAX_STREAK_BONUS   = 2.0f;
        public static float BOSS_MULTIPLIER    = 3.0f;
        public static float MINIBOSS_MULTIPLIER= 2.0f;
        public static int   STREAK_TIMEOUT     = 6000;
        // 战斗时: 200-600 RF/击杀（爆发性）
    }

    /**
     * 身上发电模块的实际产能限制
     * 4种发电模块全开也只能提供有限能源
     */
    public static final class MaxGenerationLimits {
        // 4种发电模块Lv3全开的理论最大值
        public static int MAX_KINETIC  = 180;   // 持续跑动
        public static int MAX_SOLAR    = 156;   // 晴天高处
        public static int MAX_VOID     = 200;   // 末地环境
        public static int MAX_COMBAT   = 100;   // 平均战斗

        // 实际同时最大（不可能同时满足所有条件）
        public static int REALISTIC_MAX = 400;   // 实际约400 RF/s

        // 即使最理想情况也远不够25种模块全开
    }

    // ===== 全开消耗计算示例 =====
    /**
     * 25种模块全开的预期消耗：
     *
     * 基础8种 x 平均Lv4 = 32级
     * - 消耗: 约 700 RF/s
     *
     * 扩展17种 x 平均Lv3 = 51级
     * - 消耗: 约 1200 RF/s
     *
     * 基础消耗: 20 RF/s
     * 泄漏损耗: 25种 x 3 = 75 RF/s
     *
     * 小计: 约 2000 RF/s
     *
     * 过载惩罚（83级）: x3.5
     * 总计: 约 7000 RF/s
     *
     * 身上最强发电（4种发电模块全开）: 约 400 RF/s
     *
     * 缺口: 6600 RF/s - 必须完全由电池提供！
     *
     * 结论：不带电池根本无法全开模块
     */

    // ===== 能量状态阈值 =====
    public static final class EnergyThresholds {
        public static float NORMAL       = 0.40f;  // 40%以上正常
        public static float POWER_SAVING = 0.25f;  // 25%省电
        public static float EMERGENCY    = 0.10f;  // 10%紧急
        public static float CRITICAL     = 0.03f;  // 3%危急
        public static float SHUTDOWN     = 0.00f;  // 0%关机
    }

    // ===== 低电量惩罚 =====
    public static final class LowEnergyPenalty {
        public static float BELOW_40_PERCENT = 0.95f;
        public static float BELOW_25_PERCENT = 0.80f;
        public static float BELOW_10_PERCENT = 0.50f;
        public static float BELOW_3_PERCENT  = 0.20f;
        public static float ZERO_ENERGY      = 0.00f;
    }

    // ===== 低电量DEBUFF =====
    public static final class LowEnergyDebuffs {
        public static float MINING_FATIGUE_THRESHOLD = 0.25f;
        public static int MINING_FATIGUE_LEVEL = 0;

        public static float SLOWNESS_THRESHOLD = 0.10f;
        public static int SLOWNESS_LEVEL = 0;
        public static int WEAKNESS_LEVEL = 0;

        public static float CRITICAL_THRESHOLD = 0.03f;
        public static int CRITICAL_SLOWNESS_LEVEL = 1;
        public static int CRITICAL_WEAKNESS_LEVEL = 1;
        public static boolean BLINDNESS_ENABLED = true;

        public static float WITHER_THRESHOLD = 0.00f;
        public static int WITHER_LEVEL = 0;
    }

    // ===== 过载惩罚（基于总等级的指数增长）=====
    public static final class OverloadPenalty {
        /**
         * 基于总等级数的过载惩罚
         * 使用分段指数函数，让前期平缓，后期陡峭
         */
        public static float getOverloadMultiplier(int totalLevels) {
            if (totalLevels <= 10) {
                return 1.0f;  // 10级内无惩罚
            } else if (totalLevels <= 20) {
                // 10-20级: 线性增长 1.0 -> 1.2
                return 1.0f + (totalLevels - 10) * 0.02f;
            } else if (totalLevels <= 40) {
                // 20-40级: 缓慢指数 1.2 -> 1.8
                float progress = (totalLevels - 20) / 20.0f;
                return 1.2f + 0.6f * progress * progress;
            } else if (totalLevels <= 60) {
                // 40-60级: 中速指数 1.8 -> 3.0
                float progress = (totalLevels - 40) / 20.0f;
                return 1.8f + 1.2f * (float)Math.pow(progress, 1.5);
            } else if (totalLevels <= 80) {
                // 60-80级: 快速指数 3.0 -> 5.0
                float progress = (totalLevels - 60) / 20.0f;
                return 3.0f + 2.0f * (float)Math.pow(progress, 2);
            } else {
                // 80级以上: 极速增长
                return 5.0f * (float)Math.pow(1.02, totalLevels - 80);
            }
        }
    }

    // ===== 电池系统配置（核心设计）=====
    public static final class BatterySystem {
        // 电池类型和容量
        public static int BASIC_BATTERY_CAPACITY    = 100_000;   // 基础电池 100k
        public static int ADVANCED_BATTERY_CAPACITY = 500_000;   // 高级电池 500k
        public static int QUANTUM_BATTERY_CAPACITY  = 2_000_000; // 量子电池 2M

        // 电池充放电速率
        public static int BASIC_BATTERY_TRANSFER    = 1000;      // 1000 RF/t
        public static int ADVANCED_BATTERY_TRANSFER = 5000;      // 5000 RF/t
        public static int QUANTUM_BATTERY_TRANSFER  = 20000;     // 20000 RF/t

        // 电池提供的额外发电（这是关键！电池不只是储能，还主动供电）
        public static int BASIC_BATTERY_OUTPUT      = 500;       // 500 RF/s额外
        public static int ADVANCED_BATTERY_OUTPUT   = 2000;      // 2000 RF/s额外
        public static int QUANTUM_BATTERY_OUTPUT    = 4000;      // 4000 RF/s额外

        // 多电池组合加成（鼓励玩家制作多个电池）
        public static float DUAL_BATTERY_BONUS      = 1.3f;      // 双电池130%效率
        public static float TRIPLE_BATTERY_BONUS    = 1.7f;      // 三电池170%效率
        public static float QUAD_BATTERY_BONUS      = 2.2f;      // 四电池220%效率

        // 电池效率随电量降低
        public static float getBatteryEfficiency(float chargePercent) {
            if (chargePercent > 0.8f) return 1.0f;     // 80%以上满效率
            if (chargePercent > 0.5f) return 0.9f;     // 50-80%降到90%
            if (chargePercent > 0.2f) return 0.7f;     // 20-50%降到70%
            return 0.4f;                               // 20%以下仅40%效率
        }
    }

    // ===== 模块泄漏机制 =====
    public static final class ModuleLeakage {
        // 每个安装的模块都会造成能量泄漏
        public static int LEAK_PER_MODULE_TYPE = 3;        // 每种模块+3 RF/s
        public static int LEAK_PER_TOTAL_LEVEL = 1;        // 每总等级+1 RF/s

        // 泄漏会随着模块数量指数增长
        public static float getLeakageMultiplier(int moduleTypes) {
            if (moduleTypes <= 5) return 1.0f;
            if (moduleTypes <= 10) return 1.2f;
            if (moduleTypes <= 15) return 1.5f;
            if (moduleTypes <= 20) return 2.0f;
            return 3.0f;  // 20种以上
        }
    }

    // ===== 工具方法 =====

    /**
     * 获取升级的被动消耗
     */
    public static int getPassiveDrain(String upgradeId, int level) {
        if (level <= 0) return 0;

        // 基础升级
        switch (upgradeId.toUpperCase()) {
            case "ARMOR_ENHANCEMENT": return BasicUpgrades.ARMOR_ENHANCEMENT * level;
            case "SPEED_BOOST": return BasicUpgrades.SPEED_BOOST * level;
            case "SHIELD_GENERATOR": return BasicUpgrades.SHIELD_GENERATOR * level;
            case "TEMPERATURE_CONTROL": return BasicUpgrades.TEMPERATURE_CONTROL * level;
            case "FLIGHT_MODULE": return BasicUpgrades.FLIGHT_MODULE * level;
            case "REGENERATION": return BasicUpgrades.REGENERATION * level;
            case "ENERGY_EFFICIENCY": return BasicUpgrades.ENERGY_EFFICIENCY * level;
            case "ENERGY_CAPACITY": return BasicUpgrades.ENERGY_CAPACITY * level;

            // 扩展升级
            case "YELLOW_SHIELD": return ExtendedUpgrades.YELLOW_SHIELD * level;
            case "HEALTH_REGEN": return ExtendedUpgrades.HEALTH_REGEN * level;
            case "HUNGER_THIRST": return ExtendedUpgrades.HUNGER_THIRST * level;
            case "THORNS": return ExtendedUpgrades.THORNS * level;
            case "FIRE_EXTINGUISH": return ExtendedUpgrades.FIRE_EXTINGUISH * level;
            case "WATERPROOF_MODULE": return ExtendedUpgrades.WATERPROOF_MODULE * level;
            case "POISON_IMMUNITY": return ExtendedUpgrades.POISON_IMMUNITY * level;

            case "MOVEMENT_SPEED": return ExtendedUpgrades.MOVEMENT_SPEED * level;
            case "STEALTH": return ExtendedUpgrades.STEALTH * level;
            case "ORE_VISION": return ExtendedUpgrades.ORE_VISION * level;
            case "NIGHT_VISION": return ExtendedUpgrades.NIGHT_VISION * level;
            case "WATER_BREATHING": return ExtendedUpgrades.WATER_BREATHING * level;

            case "DAMAGE_BOOST": return ExtendedUpgrades.DAMAGE_BOOST * level;
            case "ATTACK_SPEED": return ExtendedUpgrades.ATTACK_SPEED * level;
            case "RANGE_EXTENSION": return ExtendedUpgrades.RANGE_EXTENSION * level;
            case "PURSUIT": return ExtendedUpgrades.PURSUIT * level;
            case "CRITICAL_STRIKE": return ExtendedUpgrades.CRITICAL_STRIKE * level;

            case "EXP_AMPLIFIER": return ExtendedUpgrades.EXP_AMPLIFIER * level;
            case "ITEM_MAGNET": return ExtendedUpgrades.ITEM_MAGNET * level;

            // 发电模块
            case "KINETIC_GENERATOR": return ExtendedUpgrades.KINETIC_GENERATOR * level;
            case "SOLAR_GENERATOR": return ExtendedUpgrades.SOLAR_GENERATOR * level;
            case "VOID_ENERGY": return ExtendedUpgrades.VOID_ENERGY * level;
            case "COMBAT_CHARGER": return ExtendedUpgrades.COMBAT_CHARGER * level;

            default: return 10 * level;  // 默认消耗
        }
    }

    /**
     * 计算总消耗（包含所有机制）
     * @param baseConsumption 基础消耗总和
     * @param totalLevels 所有模块的等级总和
     * @param moduleTypes 不同模块种类数
     * @param energyPercent 当前能量百分比
     * @param hasBattery 是否装备电池
     */
    public static int calculateTotalDrain(int baseConsumption, int totalLevels,
                                          int moduleTypes, float energyPercent,
                                          boolean hasBattery) {
        // 基础消耗
        float total = baseConsumption;

        // 加上核心空载消耗
        total += CORE_IDLE_DRAIN;

        // 加上泄漏损耗
        float leakage = moduleTypes * ModuleLeakage.LEAK_PER_MODULE_TYPE +
                totalLevels * ModuleLeakage.LEAK_PER_TOTAL_LEVEL;
        leakage *= ModuleLeakage.getLeakageMultiplier(moduleTypes);
        total += leakage;

        // 应用过载惩罚（基于总等级）
        total *= OverloadPenalty.getOverloadMultiplier(totalLevels);

        // 如果有电池，减少20%消耗（电池优化）
        if (hasBattery) {
            total *= 0.8f;
        }

        // 低电量时的额外惩罚
        if (energyPercent < 0.1f && !hasBattery) {
            total *= 1.5f;  // 无电池低电量额外50%消耗
        }

        return Math.round(total);
    }

    /**
     * 获取低电量效率倍率
     */
    public static float getLowEnergyMultiplier(float energyPercent) {
        if (energyPercent <= 0.00f) return LowEnergyPenalty.ZERO_ENERGY;
        if (energyPercent < 0.03f) return LowEnergyPenalty.BELOW_3_PERCENT;
        if (energyPercent < 0.10f) return LowEnergyPenalty.BELOW_10_PERCENT;
        if (energyPercent < 0.25f) return LowEnergyPenalty.BELOW_25_PERCENT;
        if (energyPercent < 0.40f) return LowEnergyPenalty.BELOW_40_PERCENT;
        return 1.0f;
    }

    /** 从 Forge 配置同步到本类静态字段 */
    public static void init() {
        ModConfig.updateEnergyBalanceConfig();
    }
}