package com.moremod.upgrades;

import com.moremod.item.ItemMechanicalCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

/**
 * 温度调节效果系统
 * 集成温度调节器功能到机械核心
 */
public class TemperatureControlEffect {

    // 检查SimpleDifficulty是否已加载
    private static final boolean SIMPLE_DIFFICULTY_LOADED = Loader.isModLoaded("simpledifficulty");

    // 反射缓存
    private static Class<?> sdCapabilitiesClass;
    private static Object temperatureCapability;
    private static Method getCapabilityMethod;
    private static Method getTemperatureLevelMethod;
    private static Method addTemperatureLevelMethod;
    private static Method getTemperatureEnumMethod;

    static {
        if (SIMPLE_DIFFICULTY_LOADED) {
            initializeReflection();
        }
    }

    private static void initializeReflection() {
        try {
            // 加载SDCapabilities类
            sdCapabilitiesClass = Class.forName("com.charles445.simpledifficulty.api.SDCapabilities");

            // 获取TEMPERATURE字段
            temperatureCapability = sdCapabilitiesClass.getField("TEMPERATURE").get(null);

            // 获取getCapability方法
            getCapabilityMethod = EntityPlayer.class.getMethod("getCapability",
                    Class.forName("net.minecraftforge.common.capabilities.Capability"),
                    Class.forName("net.minecraft.util.EnumFacing"));

            // 加载ITemperatureCapability接口
            Class<?> tempCapabilityClass = Class.forName("com.charles445.simpledifficulty.api.temperature.ITemperatureCapability");

            // 获取温度相关方法
            getTemperatureLevelMethod = tempCapabilityClass.getMethod("getTemperatureLevel");
            addTemperatureLevelMethod = tempCapabilityClass.getMethod("addTemperatureLevel", int.class);
            getTemperatureEnumMethod = tempCapabilityClass.getMethod("getTemperatureEnum");

            System.out.println("[TemperatureControl] SimpleDifficulty反射初始化成功");
        } catch (Exception e) {
            System.err.println("[TemperatureControl] SimpleDifficulty反射初始化失败: " + e.getMessage());
        }
    }

    // 温度调节参数
    private static final int REGULATE_INTERVAL = 20;
    private static final int TARGET_TEMP_MIN = 11;
    private static final int TARGET_TEMP_MAX = 14;
    private static final int COLD_THRESHOLD = 10;
    private static final int HOT_THRESHOLD = 15;
    private static final int TEMP_ADJUSTMENT_BASE = 1;

    /**
     * 应用温度控制效果
     */
    public static void applyTemperatureControl(EntityPlayer player, ItemStack coreStack) {
        int level = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.TEMPERATURE_CONTROL);

        if (level <= 0) return;

        if (player.world.getTotalWorldTime() % REGULATE_INTERVAL == 0) {
            regulateTemperature(player, level);
        }

        applyTemperatureBuffs(player, level);
    }

    /**
     * 温度调节主逻辑
     */
    private static void regulateTemperature(EntityPlayer player, int level) {
        boolean temperatureRegulated = false;

        if (SIMPLE_DIFFICULTY_LOADED && temperatureCapability != null) {
            temperatureRegulated = regulateWithSimpleDifficulty(player, level);
        } else {
            temperatureRegulated = regulateWithBiomeDetection(player, level);
        }

        if (temperatureRegulated) {
            spawnRegulationParticles(player, level);
        }
    }

    /**
     * 使用SimpleDifficulty API调节温度（通过反射）
     */
    private static boolean regulateWithSimpleDifficulty(EntityPlayer player, int level) {
        try {
            // 通过反射获取温度能力
            Object tempCapability = getCapabilityMethod.invoke(player, temperatureCapability, null);
            if (tempCapability == null) {
                return regulateWithBiomeDetection(player, level);
            }

            // 获取当前温度等级
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

                if (level <= 5 && player.world.rand.nextInt(5) == 0) {
                    String tempStatus = tempEnum != null ? tempEnum.toString() + " (" + currentTempLevel + ")" :
                            getTemperatureStatusForLevel(currentTempLevel);
                    String newTempStatus = getTemperatureStatusForLevel(targetTemp);
                    sendMessage(player, "🌡️ 温度调节: " + action +
                            " (" + tempStatus + " → " + newTempStatus + ")", TextFormatting.AQUA);
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
    private static boolean regulateWithBiomeDetection(EntityPlayer player, int level) {
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
                String mode = SIMPLE_DIFFICULTY_LOADED ? "[API备用]" : "[生物群系]";
                sendMessage(player,   mode + " 温度调节: " + action, TextFormatting.AQUA);
            }

            return needsRegulation;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 根据温度等级获取对应的状态文本
     */
    private static String getTemperatureStatusForLevel(int level) {
        if (level >= 0 && level <= 5) return "极寒 (" + level + ")";
        else if (level >= 6 && level <= 10) return "寒冷 (" + level + ")";
        else if (level >= 11 && level <= 14) return "正常 (" + level + ")";
        else if (level >= 15 && level <= 19) return "炎热 (" + level + ")";
        else if (level >= 20 && level <= 25) return "灼热 (" + level + ")";
        else return "异常 (" + level + ")";
    }

    /**
     * 根据等级应用温度相关的Buff效果
     */
    private static void applyTemperatureBuffs(EntityPlayer player, int level) {
        if (level >= 2) {
            player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 100, 0, true, false));
        }

        if (level >= 3) {
            if (SIMPLE_DIFFICULTY_LOADED && temperatureCapability != null) {
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

        if (level >= 4) {
            if (player.isInWater()) {
                player.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 100, 0, true, false));
            }
        }

        if (level >= 5) {
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 250, 0, true, false));

            if (player.world.provider.isNether() || player.posY > 200 || player.posY < 20) {
                player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 100, 1, true, false));
            }
        }
    }

    /**
     * 基于生物群系的抗性应用（备用方案）
     */
    private static void applyBiomeBasedResistance(EntityPlayer player) {
        float biomeTemp = player.world.getBiome(player.getPosition()).getTemperature(player.getPosition());
        if (biomeTemp < 0.1f || biomeTemp > 1.2f) {
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 100, 0, true, false));
        }
    }

    /**
     * 生成温度调节的粒子效果
     */
    private static void spawnRegulationParticles(EntityPlayer player, int level) {
        if (player.world.rand.nextInt(4 - Math.min(level, 3)) == 0) {
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
    }

    /**
     * 根据生物群系获取粒子类型
     */
    private static EnumParticleTypes getParticleByBiome(EntityPlayer player) {
        float biomeTemp = player.world.getBiome(player.getPosition()).getTemperature(player.getPosition());

        if (biomeTemp < 0.3f) {
            return EnumParticleTypes.FLAME;
        } else if (biomeTemp > 1.0f) {
            return EnumParticleTypes.SNOWBALL;
        } else {
            return player.world.rand.nextBoolean() ?
                    EnumParticleTypes.VILLAGER_HAPPY : EnumParticleTypes.ENCHANTMENT_TABLE;
        }
    }

    /**
     * 发送消息给玩家
     */
    private static void sendMessage(EntityPlayer player, String text, TextFormatting color) {
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(color);
        player.sendStatusMessage(message, true);
    }
}