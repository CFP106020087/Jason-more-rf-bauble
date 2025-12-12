package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * moremod 配置文件
 * 使用 Forge 的 @Config 注解自动生成配置文件
 */
@Config(modid = "moremod", name = "moremod/energy_balance")
@Config.LangKey("config.moremod.title")
public class ModConfig {

    @Config.Comment("能量系统基础设置")
    @Config.LangKey("config.moremod.general")
    public static final General general = new General();

    @Config.Comment("被动能量消耗设置")
    @Config.LangKey("config.moremod.passive")
    public static final PassiveConsumption passive = new PassiveConsumption();

    @Config.Comment("主动能量消耗设置")
    @Config.LangKey("config.moremod.active")
    public static final ActiveConsumption active = new ActiveConsumption();

    @Config.Comment("能量产生设置")
    @Config.LangKey("config.moremod.generation")
    public static final EnergyGeneration generation = new EnergyGeneration();

    @Config.Comment("能量状态阈值设置")
    @Config.LangKey("config.moremod.thresholds")
    public static final Thresholds thresholds = new Thresholds();

    @Config.Comment("过载惩罚设置")
    @Config.LangKey("config.moremod.overload")
    public static final Overload overload = new Overload();

    @Config.Comment("七咒(Enigmatic Legacy)兼容设置")
    @Config.LangKey("config.moremod.enigmatic")
    public static final EnigmaticCompat enigmatic = new EnigmaticCompat();

    public static class General {
        @Config.Comment("基础运行消耗 (RF/秒)")
        @Config.RangeInt(min = 0, max = 1000)
        public int basePassiveDrain = 20;

        @Config.Comment("机械核心基础能量容量")
        @Config.RangeInt(min = 10000, max = 10000000)
        public int baseEnergyCapacity = 10000;

        @Config.Comment("每级能量容量升级增加的容量")
        @Config.RangeInt(min = 1000, max = 1000000)
        public int energyPerCapacityLevel = 50000;

        @Config.Comment("基础能量传输速率 (RF/tick)")
        @Config.RangeInt(min = 100, max = 100000)
        public int baseEnergyTransfer = 10000;
    }

    public static class PassiveConsumption {
        @Config.Comment("基础升级被动消耗 (RF/秒/级)")
        public final BasicUpgrades basic = new BasicUpgrades();

        @Config.Comment("扩展升级被动消耗 (RF/秒/级)")
        public final ExtendedUpgrades extended = new ExtendedUpgrades();

        public static class BasicUpgrades {
            @Config.RangeInt(min = 0, max = 100)
            public int armorEnhancement = 10;

            @Config.RangeInt(min = 0, max = 100)
            public int speedBoost = 15;

            @Config.RangeInt(min = 0, max = 100)
            public int shieldGenerator = 20;

            @Config.RangeInt(min = 0, max = 100)
            public int temperatureControl = 8;

            @Config.RangeInt(min = 0, max = 100)
            public int flightModule = 25;

            @Config.RangeInt(min = 0, max = 100)
            public int regeneration = 12;
        }

        public static class ExtendedUpgrades {
            @Config.RangeInt(min = 0, max = 200)
            public int yellowShield = 25;

            @Config.RangeInt(min = 0, max = 200)
            public int healthRegen = 30;

            @Config.RangeInt(min = 0, max = 200)
            public int movementSpeed = 40;

            @Config.RangeInt(min = 0, max = 200)
            public int damageBoost = 15;

            @Config.RangeInt(min = 0, max = 200)
            public int attackSpeed = 10;

            @Config.RangeInt(min = 0, max = 200)
            public int rangeExtension = 8;

            @Config.RangeInt(min = 0, max = 200)
            public int pursuit = 12;

            @Config.RangeInt(min = 0, max = 200)
            public int expAmplifier = 5;

            @Config.RangeInt(min = 0, max = 200)
            public int thorns = 10;

            @Config.RangeInt(min = 0, max = 200)
            public int fireExtinguish = 5;

            @Config.Comment("能源类升级不消耗被动能量")
            @Config.RangeInt(min = 0, max = 0)
            public int kineticGenerator = 0;

            @Config.RangeInt(min = 0, max = 0)
            public int solarGenerator = 0;

            @Config.RangeInt(min = 0, max = 0)
            public int voidEnergy = 0;

            @Config.RangeInt(min = 0, max = 0)
            public int combatCharger = 0;
        }
    }

    public static class ActiveConsumption {
        @Config.Comment("辅助类主动消耗")
        public final Auxiliary auxiliary = new Auxiliary();

        @Config.Comment("战斗类主动消耗")
        public final Combat combat = new Combat();

        @Config.Comment("生存类主动消耗")
        public final Survival survival = new Survival();

        public static class Auxiliary {
            @Config.Comment("矿物透视基础消耗")
            @Config.RangeInt(min = 0, max = 1000)
            public int oreVisionBase = 100;

            @Config.Comment("矿物透视每级额外消耗")
            @Config.RangeInt(min = 0, max = 100)
            public int oreVisionPerLevel = 20;

            @Config.Comment("矿物透视初始扫描消耗")
            @Config.RangeInt(min = 0, max = 1000)
            public int oreVisionScan = 200;

            @Config.Comment("隐身等级1消耗 (RF/秒)")
            @Config.RangeInt(min = 0, max = 2000)
            public int stealthLevel1 = 500;

            @Config.Comment("隐身等级2消耗 (RF/秒)")
            @Config.RangeInt(min = 0, max = 2000)
            public int stealthLevel2 = 400;

            @Config.Comment("隐身等级3消耗 (RF/秒)")
            @Config.RangeInt(min = 0, max = 2000)
            public int stealthLevel3 = 300;

            @Config.Comment("经验增幅基础消耗")
            @Config.RangeInt(min = 0, max = 100)
            public int expAmplifierBase = 10;

            @Config.Comment("经验增幅消耗倍率")
            @Config.RangeInt(min = 1, max = 10)
            public int expAmplifierMultiplier = 3;
        }

        public static class Combat {
            @Config.Comment("伤害提升每次攻击消耗")
            @Config.RangeInt(min = 0, max = 500)
            public int damageBoostPerHit = 50;

            @Config.Comment("暴击额外消耗")
            @Config.RangeInt(min = 0, max = 100)
            public int criticalStrike = 20;

            @Config.Comment("追击标记消耗")
            @Config.RangeInt(min = 0, max = 100)
            public int pursuitMark = 10;

            @Config.Comment("追击冲刺消耗")
            @Config.RangeInt(min = 0, max = 500)
            public int pursuitDash = 100;

            @Config.Comment("范围显示消耗")
            @Config.RangeInt(min = 0, max = 50)
            public int rangeIndicator = 5;
        }

        public static class Survival {
            @Config.Comment("护盾维持消耗每级")
            @Config.RangeInt(min = 0, max = 100)
            public int shieldMaintainPerLevel = 15;

            @Config.Comment("护盾恢复每点消耗")
            @Config.RangeInt(min = 0, max = 100)
            public int shieldRestorePerPoint = 10;

            @Config.Comment("生命恢复每级消耗")
            @Config.RangeInt(min = 0, max = 200)
            public int healthRegenPerLevel = 30;

            @Config.Comment("饥饿恢复消耗")
            @Config.RangeInt(min = 0, max = 100)
            public int hungerRestore = 20;

            @Config.Comment("口渴恢复消耗")
            @Config.RangeInt(min = 0, max = 100)
            public int thirstRestore = 25;

            @Config.Comment("自动灭火消耗")
            @Config.RangeInt(min = 0, max = 500)
            public int fireExtinguish = 100;
        }
    }

    public static class EnergyGeneration {
        @Config.Comment("动能发电设置")
        public final Kinetic kinetic = new Kinetic();

        @Config.Comment("太阳能发电设置")
        public final Solar solar = new Solar();

        @Config.Comment("虚空能量设置")
        public final Void voidEnergy = new Void();

        @Config.Comment("战斗充能设置")
        public final Combat combat = new Combat();

        public static class Kinetic {
            @Config.Comment("每格移动产生的能量")
            @Config.RangeInt(min = 0, max = 100)
            public int energyPerBlock = 10;

            @Config.Comment("每级升级增加的能量")
            @Config.RangeInt(min = 0, max = 100)
            public int energyPerLevel = 10;

            @Config.Comment("冲刺倍率")
            @Config.RangeDouble(min = 1.0, max = 5.0)
            public double sprintMultiplier = 1.5;

            @Config.Comment("飞行倍率")
            @Config.RangeDouble(min = 1.0, max = 5.0)
            public double elytraMultiplier = 1.5;

            @Config.Comment("跳跃倍率")
            @Config.RangeDouble(min = 1.0, max = 5.0)
            public double jumpMultiplier = 1.1;

            @Config.Comment("方块破坏基础能量")
            @Config.RangeInt(min = 0, max = 500)
            public int blockBreakBase = 20;
        }

        public static class Solar {
            @Config.Comment("每级产生的能量")
            @Config.RangeInt(min = 0, max = 500)
            public int energyPerLevel = 80;

            @Config.Comment("最大高度加成")
            @Config.RangeDouble(min = 1.0, max = 3.0)
            public double heightBonusMax = 1.5;

            @Config.Comment("雨天产能倍率")
            @Config.RangeDouble(min = 0.0, max = 1.0)
            public double rainPenalty = 0.3;

            @Config.Comment("雷暴产能倍率")
            @Config.RangeDouble(min = 0.0, max = 1.0)
            public double stormPenalty = 0.1;

            @Config.Comment("最低天空亮度要求")
            @Config.RangeInt(min = 0, max = 15)
            public int minSkyLight = 10;
        }

        public static class Void {
            @Config.Comment("每tick充能量")
            @Config.RangeInt(min = 0, max = 10)
            public int chargePerTick = 1;

            @Config.Comment("深层Y坐标")
            @Config.RangeInt(min = 0, max = 64)
            public int deepYLevel = 20;

            @Config.Comment("虚空Y坐标")
            @Config.RangeInt(min = 0, max = 64)
            public int voidYLevel = 10;

            @Config.Comment("末地倍率")
            @Config.RangeInt(min = 1, max = 10)
            public int endMultiplier = 2;

            @Config.Comment("末地额外能量")
            @Config.RangeInt(min = 0, max = 1000)
            public int endBonus = 100;
        }

        public static class Combat {
            @Config.Comment("每点生命值产生的能量")
            @Config.RangeInt(min = 0, max = 100)
            public int energyPerHp = 50;

            @Config.Comment("最大连杀加成")
            @Config.RangeDouble(min = 1.0, max = 5.0)
            public double maxStreakBonus = 2.0;

            @Config.Comment("Boss倍率")
            @Config.RangeDouble(min = 1.0, max = 10.0)
            public double bossMultiplier = 3.0;

            @Config.Comment("小Boss倍率")
            @Config.RangeDouble(min = 1.0, max = 5.0)
            public double minibossMultiplier = 2.0;

            @Config.Comment("连杀超时(tick)")
            @Config.RangeInt(min = 0, max = 12000)
            public int streakTimeout = 6000;
        }
    }

    public static class Thresholds {
        @Config.Comment("正常状态阈值 (30%以上)")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double normal = 0.30;

        @Config.Comment("省电模式阈值 (15-30%)")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double powerSaving = 0.15;

        @Config.Comment("紧急模式阈值 (5-15%)")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double emergency = 0.05;

        @Config.Comment("生命支持阈值 (2-5%)")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double critical = 0.02;
    }

    public static class Overload {
        @Config.Comment("无惩罚的最大升级数")
        @Config.RangeInt(min = 1, max = 20)
        public int tier1Upgrades = 5;

        @Config.Comment("第一级惩罚倍率")
        @Config.RangeDouble(min = 1.0, max = 3.0)
        public double tier1Penalty = 1.2;

        @Config.Comment("第二级惩罚升级数")
        @Config.RangeInt(min = 1, max = 20)
        public int tier2Upgrades = 8;

        @Config.Comment("第二级惩罚倍率")
        @Config.RangeDouble(min = 1.0, max = 3.0)
        public double tier2Penalty = 1.5;

        @Config.Comment("第三级惩罚升级数")
        @Config.RangeInt(min = 1, max = 20)
        public int tier3Upgrades = 11;

        @Config.Comment("第三级惩罚倍率")
        @Config.RangeDouble(min = 1.0, max = 5.0)
        public double tier3Penalty = 2.0;
    }

    public static class EnigmaticCompat {
        @Config.Comment("是否阻止所有Enigmatic物品与机械核心同时佩戴")
        public boolean blockAllEnigmatic = true;

        @Config.Comment("是否输出详细的Enigmatic物品检测日志(调试用)")
        public boolean verboseEnigmaticDetection = false;
    }

    /**
     * 配置变更事件处理
     */
    @Mod.EventBusSubscriber(modid = "moremod")
    public static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if ("moremod".equals(event.getModID())) {
                ConfigManager.sync("moremod", Config.Type.INSTANCE);
                // 更新 EnergyBalanceConfig 的值
                updateEnergyBalanceConfig();
            }
        }
    }

    /**
     * 将配置值同步到 EnergyBalanceConfig
     */
    public static void updateEnergyBalanceConfig() {
        // ===== 基础设置 =====
        EnergyBalanceConfig.BASE_PASSIVE_DRAIN        = general.basePassiveDrain;
        EnergyBalanceConfig.BASE_ENERGY_CAPACITY      = general.baseEnergyCapacity;
        EnergyBalanceConfig.ENERGY_PER_CAPACITY_LEVEL = general.energyPerCapacityLevel;
        EnergyBalanceConfig.BASE_ENERGY_TRANSFER      = general.baseEnergyTransfer;

        // ===== 被动消耗（基础）=====
        EnergyBalanceConfig.BasicUpgrades.ARMOR_ENHANCEMENT   = passive.basic.armorEnhancement;
        EnergyBalanceConfig.BasicUpgrades.SPEED_BOOST         = passive.basic.speedBoost;
        EnergyBalanceConfig.BasicUpgrades.SHIELD_GENERATOR    = passive.basic.shieldGenerator;
        EnergyBalanceConfig.BasicUpgrades.TEMPERATURE_CONTROL = passive.basic.temperatureControl;
        EnergyBalanceConfig.BasicUpgrades.FLIGHT_MODULE       = passive.basic.flightModule;
        EnergyBalanceConfig.BasicUpgrades.REGENERATION        = passive.basic.regeneration;
        //（注意：EnergyBalanceConfig.BasicUpgrades 里如果还有 ENERGY_EFFICIENCY/ENERGY_CAPACITY 等
        // 且你希望它们可配置，需要在此 ModConfig 增加字段后再同步）

        // ===== 被动消耗（扩展）=====
        EnergyBalanceConfig.ExtendedUpgrades.YELLOW_SHIELD    = passive.extended.yellowShield;
        EnergyBalanceConfig.ExtendedUpgrades.HEALTH_REGEN     = passive.extended.healthRegen;
        EnergyBalanceConfig.ExtendedUpgrades.MOVEMENT_SPEED   = passive.extended.movementSpeed;
        EnergyBalanceConfig.ExtendedUpgrades.DAMAGE_BOOST     = passive.extended.damageBoost;
        EnergyBalanceConfig.ExtendedUpgrades.ATTACK_SPEED     = passive.extended.attackSpeed;
        EnergyBalanceConfig.ExtendedUpgrades.RANGE_EXTENSION  = passive.extended.rangeExtension;
        EnergyBalanceConfig.ExtendedUpgrades.PURSUIT          = passive.extended.pursuit;
        EnergyBalanceConfig.ExtendedUpgrades.EXP_AMPLIFIER    = passive.extended.expAmplifier;
        EnergyBalanceConfig.ExtendedUpgrades.THORNS           = passive.extended.thorns;
        EnergyBalanceConfig.ExtendedUpgrades.FIRE_EXTINGUISH  = passive.extended.fireExtinguish;
        // 发电类被动成本（当前你配置为 0）
        EnergyBalanceConfig.ExtendedUpgrades.KINETIC_GENERATOR = passive.extended.kineticGenerator;
        EnergyBalanceConfig.ExtendedUpgrades.SOLAR_GENERATOR   = passive.extended.solarGenerator;
        EnergyBalanceConfig.ExtendedUpgrades.VOID_ENERGY       = passive.extended.voidEnergy;
        EnergyBalanceConfig.ExtendedUpgrades.COMBAT_CHARGER    = passive.extended.combatCharger;

        // ===== 主动消耗（辅助）=====
        EnergyBalanceConfig.AuxiliaryActive.ORE_VISION_BASE      = active.auxiliary.oreVisionBase;
        EnergyBalanceConfig.AuxiliaryActive.ORE_VISION_PER_LEVEL = active.auxiliary.oreVisionPerLevel;
        EnergyBalanceConfig.AuxiliaryActive.ORE_VISION_SCAN      = active.auxiliary.oreVisionScan;
        EnergyBalanceConfig.AuxiliaryActive.STEALTH_LEVEL_1      = active.auxiliary.stealthLevel1;
        EnergyBalanceConfig.AuxiliaryActive.STEALTH_LEVEL_2      = active.auxiliary.stealthLevel2;
        EnergyBalanceConfig.AuxiliaryActive.STEALTH_LEVEL_3      = active.auxiliary.stealthLevel3;
        EnergyBalanceConfig.AuxiliaryActive.EXP_AMPLIFIER_BASE   = active.auxiliary.expAmplifierBase;
        EnergyBalanceConfig.AuxiliaryActive.EXP_AMPLIFIER_MULTIPLIER = active.auxiliary.expAmplifierMultiplier;

        // ===== 主动消耗（战斗）=====
        EnergyBalanceConfig.CombatActive.DAMAGE_BOOST_PER_HIT = active.combat.damageBoostPerHit;
        EnergyBalanceConfig.CombatActive.CRITICAL_STRIKE      = active.combat.criticalStrike;
        EnergyBalanceConfig.CombatActive.PURSUIT_MARK         = active.combat.pursuitMark;
        EnergyBalanceConfig.CombatActive.PURSUIT_DASH         = active.combat.pursuitDash;
        EnergyBalanceConfig.CombatActive.RANGE_INDICATOR      = active.combat.rangeIndicator;

        // ===== 主动消耗（生存）=====
        EnergyBalanceConfig.SurvivalActive.SHIELD_MAINTAIN_PER_LEVEL = active.survival.shieldMaintainPerLevel;
        EnergyBalanceConfig.SurvivalActive.SHIELD_RESTORE_PER_POINT  = active.survival.shieldRestorePerPoint;
        EnergyBalanceConfig.SurvivalActive.HEALTH_REGEN_PER_LEVEL    = active.survival.healthRegenPerLevel;
        EnergyBalanceConfig.SurvivalActive.HUNGER_RESTORE            = active.survival.hungerRestore;
        EnergyBalanceConfig.SurvivalActive.THIRST_RESTORE            = active.survival.thirstRestore;
        EnergyBalanceConfig.SurvivalActive.FIRE_EXTINGUISH           = active.survival.fireExtinguish;

        // ===== 能量产生（动能）=====
        EnergyBalanceConfig.KineticGenerator.ENERGY_PER_BLOCK   = generation.kinetic.energyPerBlock;
        EnergyBalanceConfig.KineticGenerator.ENERGY_PER_LEVEL   = generation.kinetic.energyPerLevel;
        EnergyBalanceConfig.KineticGenerator.SPRINT_MULTIPLIER  = (float) generation.kinetic.sprintMultiplier;
        EnergyBalanceConfig.KineticGenerator.ELYTRA_MULTIPLIER  = (float) generation.kinetic.elytraMultiplier;
        EnergyBalanceConfig.KineticGenerator.JUMP_MULTIPLIER    = (float) generation.kinetic.jumpMultiplier;
        EnergyBalanceConfig.KineticGenerator.BLOCK_BREAK_BASE   = generation.kinetic.blockBreakBase;

        // ===== 能量产生（太阳）=====
        EnergyBalanceConfig.SolarGenerator.ENERGY_PER_LEVEL = generation.solar.energyPerLevel;
        EnergyBalanceConfig.SolarGenerator.HEIGHT_BONUS_MAX = (float) generation.solar.heightBonusMax;
        EnergyBalanceConfig.SolarGenerator.RAIN_PENALTY     = (float) generation.solar.rainPenalty;
        EnergyBalanceConfig.SolarGenerator.STORM_PENALTY    = (float) generation.solar.stormPenalty;
        EnergyBalanceConfig.SolarGenerator.MIN_SKY_LIGHT     = generation.solar.minSkyLight;

        // ===== 能量产生（虚空）=====
        EnergyBalanceConfig.VoidEnergy.CHARGE_PER_TICK  = generation.voidEnergy.chargePerTick;
        EnergyBalanceConfig.VoidEnergy.DEEP_Y_LEVEL     = generation.voidEnergy.deepYLevel;
        EnergyBalanceConfig.VoidEnergy.VOID_Y_LEVEL     = generation.voidEnergy.voidYLevel;
        EnergyBalanceConfig.VoidEnergy.END_MULTIPLIER   = generation.voidEnergy.endMultiplier;
        EnergyBalanceConfig.VoidEnergy.END_BONUS        = generation.voidEnergy.endBonus;

        // ===== 能量产生（战斗充能）=====
        EnergyBalanceConfig.CombatCharger.ENERGY_PER_HP        = generation.combat.energyPerHp;
        EnergyBalanceConfig.CombatCharger.MAX_STREAK_BONUS     = (float) generation.combat.maxStreakBonus;
        EnergyBalanceConfig.CombatCharger.BOSS_MULTIPLIER      = (float) generation.combat.bossMultiplier;
        EnergyBalanceConfig.CombatCharger.MINIBOSS_MULTIPLIER  = (float) generation.combat.minibossMultiplier;
        EnergyBalanceConfig.CombatCharger.STREAK_TIMEOUT       = generation.combat.streakTimeout;

        // ===== 阈值 =====
        EnergyBalanceConfig.EnergyThresholds.NORMAL       = (float) thresholds.normal;
        EnergyBalanceConfig.EnergyThresholds.POWER_SAVING = (float) thresholds.powerSaving;
        EnergyBalanceConfig.EnergyThresholds.EMERGENCY    = (float) thresholds.emergency;
        EnergyBalanceConfig.EnergyThresholds.CRITICAL     = (float) thresholds.critical;

        // ===== 过载惩罚 =====
        }
}
