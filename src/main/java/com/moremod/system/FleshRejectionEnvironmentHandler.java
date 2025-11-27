package com.moremod.system;

import com.moremod.config.FleshRejectionConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Method;

/**
 * 血肉排异环境处理器 - 简化版
 * 
 * 职责：
 * - 饥饿排异
 * - 口渴排异（SimpleDifficulty）
 * - 失眠惩罚
 * - 睡眠恢复
 * 
 * 注意：药水阻挡逻辑已移至 FleshRejectionPotionHandler
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class FleshRejectionEnvironmentHandler {

    // SimpleDifficulty 集成
    private static final boolean SD_LOADED = Loader.isModLoaded("simpledifficulty");
    private static Object thirstCapability;
    private static Method getCapabilityMethod;
    private static Method getThirstLevelMethod;

    static {
        if (SD_LOADED) {
            try {
                Class<?> sdCapabilities = Class.forName("com.charles445.simpledifficulty.api.SDCapabilities");
                thirstCapability = sdCapabilities.getField("THIRST").get(null);
                getCapabilityMethod = EntityPlayer.class.getMethod("getCapability",
                        Class.forName("net.minecraftforge.common.capabilities.Capability"),
                        Class.forName("net.minecraft.util.EnumFacing"));
                Class<?> thirstCapabilityClass = Class.forName("com.charles445.simpledifficulty.api.thirst.IThirstCapability");
                getThirstLevelMethod = thirstCapabilityClass.getMethod("getThirstLevel");
            } catch (Exception e) {
                System.err.println("[MoreMod] SimpleDifficulty integration failed: " + e.getMessage());
            }
        }
    }

    /**
     * 主更新循环 - 每秒执行
     */
    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (player.world.isRemote) return;
        if (!FleshRejectionConfig.enableRejectionSystem) return;
        if (!FleshRejectionSystem.hasMechanicalCore(player)) return;
        if (player.ticksExisted % 20 != 0) return;

        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        float adaptation = FleshRejectionSystem.getAdaptationLevel(player);

        // 突破后免疫
        if (FleshRejectionSystem.hasTranscended(player) &&
                adaptation >= FleshRejectionConfig.adaptationThreshold) return;

        // 处理环境因素
        handleHunger(player, rejection);
        handleThirst(player, rejection);
        handleInsomnia(player, rejection);
        handleSleepRecovery(player);

        // 定期警告
        if (player.ticksExisted % 100 == 0) {
            checkRejectionWarning(player, rejection, adaptation);
        }
    }

    /**
     * 饥饿排异
     */
    private static void handleHunger(EntityPlayer player, float rejection) {
        int food = player.getFoodStats().getFoodLevel();
        if (food < 20) {
            double hungerPain = Math.pow(20 - food, FleshRejectionConfig.hungerCurve) *
                    FleshRejectionConfig.hungerRejectionFactor;

            if (hungerPain > 0.1) {
                FleshRejectionSystem.setRejectionLevel(player, rejection + (float)hungerPain);
            }
        }
    }

    /**
     * 口渴排异（SimpleDifficulty）
     */
    private static void handleThirst(EntityPlayer player, float rejection) {
        if (!SD_LOADED || thirstCapability == null) return;

        try {
            Object thirstCap = getCapabilityMethod.invoke(player, thirstCapability, null);
            if (thirstCap != null) {
                int thirst = (int) getThirstLevelMethod.invoke(thirstCap);
                if (thirst < 20) {
                    double thirstPain = Math.pow(20 - thirst, FleshRejectionConfig.hungerCurve) *
                            FleshRejectionConfig.thirstRejectionFactor;

                    if (thirstPain > 0.1) {
                        FleshRejectionSystem.setRejectionLevel(player,
                                rejection + (float) thirstPain);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * 失眠惩罚
     */
    private static void handleInsomnia(EntityPlayer player, float rejection) {
        long time = player.world.getWorldTime() % 24000;
        boolean isNight = time >= 13000;

        if (isNight) {
            int noSleepTicks = player.getEntityData().getInteger("MoreMod_NoSleepTicks");
            player.getEntityData().setInteger("MoreMod_NoSleepTicks", noSleepTicks + 20);
        }

        int noSleepTicks = player.getEntityData().getInteger("MoreMod_NoSleepTicks");
        int noSleepDays = noSleepTicks / 24000;

        float insomniaBoost = 0f;
        if (noSleepDays >= FleshRejectionConfig.insomniaStage3Days) {
            insomniaBoost = FleshRejectionConfig.insomniaStage3Boost;
        } else if (noSleepDays >= FleshRejectionConfig.insomniaStage2Days) {
            insomniaBoost = FleshRejectionConfig.insomniaStage2Boost;
        } else if (noSleepDays >= FleshRejectionConfig.insomniaStage1Days) {
            insomniaBoost = FleshRejectionConfig.insomniaStage1Boost;
        }

        if (insomniaBoost > 0) {
            FleshRejectionSystem.setRejectionLevel(player, rejection * (1f + insomniaBoost));
        }
    }

    /**
     * 睡眠恢复
     */
    private static void handleSleepRecovery(EntityPlayer player) {
        boolean sleeping = player.isPlayerSleeping();
        if (sleeping) {
            player.getEntityData().setInteger("MoreMod_SleepRecoveryTicks",
                    FleshRejectionConfig.sleepRecoveryDuration);
            player.getEntityData().setInteger("MoreMod_NoSleepTicks", 0);
        }

        int recovery = player.getEntityData().getInteger("MoreMod_SleepRecoveryTicks");
        if (recovery > 0) {
            player.getEntityData().setInteger("MoreMod_SleepRecoveryTicks", recovery - 20);
            FleshRejectionSystem.reduceRejection(player, FleshRejectionConfig.sleepRecoveryPerSecond);
        }
    }

    /**
     * 排异警告
     */
    private static void checkRejectionWarning(EntityPlayer player, float rejection, float adaptation) {
        if (adaptation >= FleshRejectionConfig.adaptationThreshold) return;

        if (rejection >= 90) {
            player.sendStatusMessage(
                    new TextComponentString(TextFormatting.DARK_RED + "⚠ 排异临界！立即处理！"),
                    true
            );
        } else if (rejection >= 70 && player.ticksExisted % 200 == 0) {
            player.sendStatusMessage(
                    new TextComponentString(TextFormatting.RED + "⚠ 排异严重"),
                    true
            );
        }
    }
}