package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

/**
 * 温度控制模块
 *
 * 功能：
 *  - 自动调节玩家体温，抵抗极端温度
 *  - 集成 SimpleDifficulty 温度系统（通过反射）
 *  - 备用方案：基于生物群系温度检测
 *  - Lv.1: 基础温度调节
 *  - Lv.2: + 抗火（Fire Resistance）
 *  - Lv.3: + 极端温度抗性（Resistance）
 *  - Lv.4: + 水下呼吸（Water Breathing）
 *  - Lv.5: + 夜视 + 特殊环境增强
 *
 * 能量消耗：
 *  - 被动消耗：10 * level RF/tick
 *
 * 温度目标范围：
 *  - SimpleDifficulty: 11-14 级（正常温度）
 *  - 生物群系: 0.2-1.0 温度值
 */
public class TemperatureControlModule extends AbstractMechCoreModule {

    public static final TemperatureControlModule INSTANCE = new TemperatureControlModule();

    // SimpleDifficulty 集成
    private static final boolean SD_LOADED = Loader.isModLoaded("simpledifficulty");
    private static Object temperatureCapability;
    private static Method getCapabilityMethod;
    private static Method getTemperatureLevelMethod;
    private static Method addTemperatureLevelMethod;
    private static Method getTemperatureEnumMethod;

    // 温度调节参数
    private static final int REGULATE_INTERVAL = 20;  // 每秒调节一次
    private static final int TARGET_TEMP_MIN = 11;
    private static final int TARGET_TEMP_MAX = 14;
    private static final int COLD_THRESHOLD = 10;
    private static final int HOT_THRESHOLD = 15;
    private static final int TEMP_ADJUSTMENT_BASE = 1;

    static {
        if (SD_LOADED) {
            initializeSDReflection();
        }
    }

    /**
     * 初始化 SimpleDifficulty 反射
     */
    private static void initializeSDReflection() {
        try {
            Class<?> sdCapabilitiesClass = Class.forName("com.charles445.simpledifficulty.api.SDCapabilities");
            temperatureCapability = sdCapabilitiesClass.getField("TEMPERATURE").get(null);

            getCapabilityMethod = EntityPlayer.class.getMethod("getCapability",
                    Class.forName("net.minecraftforge.common.capabilities.Capability"),
                    Class.forName("net.minecraft.util.EnumFacing"));

            Class<?> tempCapabilityClass = Class.forName("com.charles445.simpledifficulty.api.temperature.ITemperatureCapability");
            getTemperatureLevelMethod = tempCapabilityClass.getMethod("getTemperatureLevel");
            addTemperatureLevelMethod = tempCapabilityClass.getMethod("addTemperatureLevel", int.class);
            getTemperatureEnumMethod = tempCapabilityClass.getMethod("getTemperatureEnum");

            System.out.println("[TemperatureControlModule] SimpleDifficulty 集成成功");
        } catch (Exception e) {
            System.err.println("[TemperatureControlModule] SimpleDifficulty 初始化失败: " + e.getMessage());
        }
    }

    private TemperatureControlModule() {
        super(
            "TEMPERATURE_CONTROL",
            "温度控制",
            "自动调节体温，抵抗极端温度",
            5  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.AQUA + "❄ 温度控制系统已激活 (Lv." + newLevel + ")"
        ), true);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.GRAY + "温度控制系统已关闭"
        ), true);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        // 每秒调节温度
        if (player.world.getTotalWorldTime() % REGULATE_INTERVAL == 0) {
            boolean regulated = regulateTemperature(player, level);

            // 生成粒子效果
            if (regulated && player.world.rand.nextInt(4 - Math.min(level, 3)) == 0) {
                spawnRegulationParticles(player, level);
            }
        }

        // 应用温度相关 Buff
        applyTemperatureBuffs(player, level);
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        if (newLevel > 0) {
            String features = getFeaturesList(newLevel);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.AQUA + "温度控制升级: " +
                            TextFormatting.WHITE + features
            ), true);
        }
    }

    /**
     * 调节温度
     */
    private boolean regulateTemperature(EntityPlayer player, int level) {
        if (SD_LOADED && temperatureCapability != null) {
            return regulateWithSimpleDifficulty(player, level);
        } else {
            return regulateWithBiomeDetection(player, level);
        }
    }

    /**
     * 使用 SimpleDifficulty API 调节温度
     */
    private boolean regulateWithSimpleDifficulty(EntityPlayer player, int level) {
        try {
            Object tempCapability = getCapabilityMethod.invoke(player, temperatureCapability, null);
            if (tempCapability == null) {
                return regulateWithBiomeDetection(player, level);
            }

            int currentTempLevel = (int) getTemperatureLevelMethod.invoke(tempCapability);
            Object tempEnum = getTemperatureEnumMethod.invoke(tempCapability);

            boolean needsRegulation = false;
            String action = "";
            int targetTemp = currentTempLevel;

            int adjustmentAmount = TEMP_ADJUSTMENT_BASE + (level - 1) / 2;

            if (currentTempLevel <= COLD_THRESHOLD) {
                targetTemp = Math.min(currentTempLevel + adjustmentAmount, TARGET_TEMP_MIN);
                action = "加热";
                needsRegulation = true;
            } else if (currentTempLevel >= HOT_THRESHOLD) {
                targetTemp = Math.max(currentTempLevel - adjustmentAmount, TARGET_TEMP_MAX);
                action = "降温";
                needsRegulation = true;
            }

            if (needsRegulation) {
                int adjustment = targetTemp - currentTempLevel;
                addTemperatureLevelMethod.invoke(tempCapability, adjustment);

                // 显示调节信息（降低频率）
                if (level <= 5 && player.world.rand.nextInt(5) == 0) {
                    String tempStatus = tempEnum != null ? tempEnum.toString() + " (" + currentTempLevel + ")" :
                            getTemperatureStatusForLevel(currentTempLevel);
                    String newTempStatus = getTemperatureStatusForLevel(targetTemp);
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.AQUA + "温度调节: " + action +
                                    " (" + tempStatus + " → " + newTempStatus + ")"
                    ), true);
                }
            }

            return needsRegulation;

        } catch (Exception e) {
            return regulateWithBiomeDetection(player, level);
        }
    }

    /**
     * 基于生物群系的温度检测（备用方案）
     */
    private boolean regulateWithBiomeDetection(EntityPlayer player, int level) {
        try {
            float biomeTemp = player.world.getBiome(player.getPosition()).getTemperature(player.getPosition());

            boolean needsRegulation = false;
            String action = "";

            if (biomeTemp < 0.2f) {
                action = "加热";
                needsRegulation = true;
            } else if (biomeTemp > 1.0f) {
                action = "降温";
                needsRegulation = true;
            }

            if (needsRegulation && level <= 2 && player.world.rand.nextInt(5) == 0) {
                String mode = SD_LOADED ? "[API备用]" : "[生物群系]";
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.AQUA + mode + " 温度调节: " + action
                ), true);
            }

            return needsRegulation;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 应用温度相关的 Buff 效果
     */
    private void applyTemperatureBuffs(EntityPlayer player, int level) {
        // Lv.2+: 抗火
        if (level >= 2) {
            player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 100, 0, true, false));
        }

        // Lv.3+: 极端温度抗性
        if (level >= 3) {
            if (SD_LOADED && temperatureCapability != null) {
                try {
                    Object tempCapability = getCapabilityMethod.invoke(player, temperatureCapability, null);
                    if (tempCapability != null) {
                        int currentTemp = (int) getTemperatureLevelMethod.invoke(tempCapability);
                        if (currentTemp <= 5 || currentTemp >= 20) {
                            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 100, 0, true, false));
                        }
                    }
                } catch (Exception e) {
                    applyBiomeBasedResistance(player);
                }
            } else {
                applyBiomeBasedResistance(player);
            }
        }

        // Lv.4+: 水下呼吸
        if (level >= 4) {
            if (player.isInWater()) {
                player.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 100, 0, true, false));
            }
        }

        // Lv.5: 夜视 + 特殊环境增强
        if (level >= 5) {
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 250, 0, true, false));

            // 特殊环境（地狱/高空/地底）增强抗性
            if (player.world.provider.isNether() || player.posY > 200 || player.posY < 20) {
                player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 100, 1, true, false));
            }
        }
    }

    /**
     * 基于生物群系的抗性应用
     */
    private void applyBiomeBasedResistance(EntityPlayer player) {
        float biomeTemp = player.world.getBiome(player.getPosition()).getTemperature(player.getPosition());
        if (biomeTemp < 0.1f || biomeTemp > 1.2f) {
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 100, 0, true, false));
        }
    }

    /**
     * 生成温度调节的粒子效果
     */
    private void spawnRegulationParticles(EntityPlayer player, int level) {
        int particleCount = Math.min(level, 3);

        for (int i = 0; i < particleCount; i++) {
            double offsetX = (player.world.rand.nextDouble() - 0.5) * 1.2;
            double offsetY = player.world.rand.nextDouble() * 1.5;
            double offsetZ = (player.world.rand.nextDouble() - 0.5) * 1.2;

            double x = player.posX + offsetX;
            double y = player.posY + offsetY;
            double z = player.posZ + offsetZ;

            EnumParticleTypes particleType = getParticleByBiome(player);

            player.world.spawnParticle(particleType, x, y, z,
                    (player.world.rand.nextDouble() - 0.5) * 0.1,
                    0.1,
                    (player.world.rand.nextDouble() - 0.5) * 0.1);
        }
    }

    /**
     * 根据生物群系获取粒子类型
     */
    private EnumParticleTypes getParticleByBiome(EntityPlayer player) {
        float biomeTemp = player.world.getBiome(player.getPosition()).getTemperature(player.getPosition());

        if (biomeTemp < 0.3f) {
            return EnumParticleTypes.FLAME;  // 寒冷环境显示火焰（加热）
        } else if (biomeTemp > 1.0f) {
            return EnumParticleTypes.SNOWBALL;  // 炎热环境显示雪（降温）
        } else {
            return player.world.rand.nextBoolean() ?
                    EnumParticleTypes.VILLAGER_HAPPY : EnumParticleTypes.ENCHANTMENT_TABLE;
        }
    }

    /**
     * 根据温度等级获取状态文本
     */
    private String getTemperatureStatusForLevel(int level) {
        if (level >= 0 && level <= 5) return "极寒 (" + level + ")";
        else if (level >= 6 && level <= 10) return "寒冷 (" + level + ")";
        else if (level >= 11 && level <= 14) return "正常 (" + level + ")";
        else if (level >= 15 && level <= 19) return "炎热 (" + level + ")";
        else if (level >= 20 && level <= 25) return "灼热 (" + level + ")";
        else return "异常 (" + level + ")";
    }

    /**
     * 获取功能列表
     */
    private String getFeaturesList(int level) {
        StringBuilder features = new StringBuilder("温度调节");
        if (level >= 2) features.append(", 抗火");
        if (level >= 3) features.append(", 极端温度抗性");
        if (level >= 4) features.append(", 水下呼吸");
        if (level >= 5) features.append(", 夜视+特殊环境增强");
        return features.toString();
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 每级 10 RF/tick
        return level * 10;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        return data.getEnergy() >= getPassiveEnergyCost(data.getModuleLevel(getModuleId()));
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        // 温度控制不需要额外的元数据
        return new NBTTagCompound();
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        // 无需验证
        return true;
    }
}
