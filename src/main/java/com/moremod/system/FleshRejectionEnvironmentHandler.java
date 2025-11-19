package com.moremod.system;

import com.moremod.config.FleshRejectionConfig;
import com.moremod.system.FleshRejectionSystem;
import com.moremod.system.FleshRejectionHUDManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Method;
import java.util.Collection;

public class FleshRejectionEnvironmentHandler {

    // SimpleDifficulty 反射
    private static final boolean SD_LOADED = Loader.isModLoaded("simpledifficulty");
    private static Class<?> thirstCapabilityClass;
    private static Object thirstCapability;
    private static Method getCapabilityMethod;
    private static Method getThirstLevelMethod;

    static {
        if (SD_LOADED) {
            try {
                Class<?> sdCapabilities = Class.forName("com.charles445.simpledifficulty.api.SDCapabilities");
                thirstCapability = sdCapabilities.getField("THIRST").get(null);

                getCapabilityMethod = EntityPlayer.class.getMethod(
                        "getCapability",
                        Class.forName("net.minecraftforge.common.capabilities.Capability"),
                        Class.forName("net.minecraft.util.EnumFacing")
                );

                thirstCapabilityClass = Class.forName("com.charles445.simpledifficulty.api.thirst.IThirstCapability");
                getThirstLevelMethod = thirstCapabilityClass.getMethod("getThirstLevel");

            } catch (Exception e) {
                System.err.println("[MoreMod] SimpleDifficulty integration failed: " + e.getMessage());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(LivingEvent.LivingUpdateEvent event) {

        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;
        if (!FleshRejectionConfig.enableRejectionSystem) return;

        if (!FleshRejectionSystem.hasMechanicalCore(player)) return;

        // 每 20 tick = 1 秒
        if (player.ticksExisted % 20 != 0) return;

        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        float adaptation = FleshRejectionSystem.getAdaptationLevel(player);
        boolean transcended = FleshRejectionSystem.hasTranscended(player);

        // 突破后完全免疫
        if (transcended && adaptation >= FleshRejectionConfig.adaptationThreshold) return;

        // ========== 饥饿带来排异 ==========
        int food = player.getFoodStats().getFoodLevel();
        if (food < 20) {
            double hungerPain =
                    Math.pow(20 - food, FleshRejectionConfig.hungerCurve) *
                            FleshRejectionConfig.hungerRejectionFactor;

            if (hungerPain > 0.1) {
                // 发送HUD提示到客户端
                if (player.world.isRemote) {
                    FleshRejectionHUDManager.onHungerRejection(player, (float)hungerPain);
                }
            }

            FleshRejectionSystem.setRejectionLevel(player,
                    rejection + (float) hungerPain
            );
        }

        // ========== 口渴带来排异 ==========
        if (SD_LOADED && thirstCapability != null && getCapabilityMethod != null) {
            try {
                Object thirstCap = getCapabilityMethod.invoke(player, thirstCapability, null);
                if (thirstCap != null) {
                    int thirst = (int) getThirstLevelMethod.invoke(thirstCap);
                    if (thirst < 20) {
                        double thirstPain =
                                Math.pow(20 - thirst, FleshRejectionConfig.hungerCurve) *
                                        FleshRejectionConfig.thirstRejectionFactor;

                        if (thirstPain > 0.1) {
                            // 发送HUD提示到客户端
                            if (player.world.isRemote) {
                                FleshRejectionHUDManager.onThirstRejection(player, (float)thirstPain);
                            }
                        }

                        FleshRejectionSystem.setRejectionLevel(player,
                                FleshRejectionSystem.getRejectionLevel(player) + (float) thirstPain
                        );
                    }
                }

            } catch (Exception ignored) {}
        }

        // ========== 熬夜提升排异速度 ==========
        long time = player.world.getWorldTime() % 24000;
        boolean isNight = time >= 13000;

        if (isNight) {
            player.getEntityData().setInteger("MoreMod_NoSleepTicks",
                    player.getEntityData().getInteger("MoreMod_NoSleepTicks") + 20
            );
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

        // 发送失眠提示
        if (insomniaBoost > 0 && player.world.isRemote) {
            FleshRejectionHUDManager.onInsomniaBoost(player, noSleepDays, insomniaBoost);
        }

        // 提升目前排异
        FleshRejectionSystem.setRejectionLevel(player,
                FleshRejectionSystem.getRejectionLevel(player) * (1f + insomniaBoost)
        );

        // ========== 睡眠恢复 ==========
        boolean sleeping = player.isPlayerSleeping();
        if (sleeping) {
            player.getEntityData().setInteger("MoreMod_SleepRecoveryTicks",
                    FleshRejectionConfig.sleepRecoveryDuration
            );

            player.getEntityData().setInteger("MoreMod_NoSleepTicks", 0);
        }

        int recovery = player.getEntityData().getInteger("MoreMod_SleepRecoveryTicks");
        if (recovery > 0) {
            player.getEntityData().setInteger("MoreMod_SleepRecoveryTicks", recovery - 20);

            float reduction = FleshRejectionConfig.sleepRecoveryPerSecond;
            FleshRejectionSystem.reduceRejection(player, reduction);
            
            // 发送睡眠恢复提示
            if (player.world.isRemote && recovery % 60 == 0) {  // 每3秒提示一次
                FleshRejectionHUDManager.onSleepRecovery(player, reduction * 3);
            }
        }
        
        // ========== 排异警告 ==========
        if (player.ticksExisted % 100 == 0) {  // 每5秒检查一次
            if (player.world.isRemote) {
                FleshRejectionHUDManager.showRejectionWarning(player, rejection);
            }
        }
    }

    @SubscribeEvent
    public void onPotionAdded(PotionEvent.PotionApplicableEvent event) {

        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!FleshRejectionConfig.enableRejectionSystem) return;

        if (!FleshRejectionSystem.hasMechanicalCore(player)) return;

        PotionEffect effect = event.getPotionEffect();
        if (effect == null || effect.getPotion().isBadEffect()) return;

        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        float adaptation = FleshRejectionSystem.getAdaptationLevel(player);

        // 突破后完全免疫
        if (FleshRejectionSystem.hasTranscended(player)) {
            return;
        }
        
        // 适应度满了后排异值归零
        if (adaptation >= FleshRejectionConfig.adaptationThreshold) {
            if (rejection > 0) {
                FleshRejectionSystem.setRejectionLevel(player, 0);
            }
            return;
        }

        // 修正：使用对数计算，避免数值爆炸
        // 药水等级贡献：1.0-2.5
        double amplifierFactor = 1.0 + Math.min(1.5, effect.getAmplifier() * 0.5);
        
        // 持续时间贡献：使用对数缩放，最大约3.0
        double durationInSeconds = effect.getDuration() / 20.0;
        double durationFactor = 1.0;
        if (durationInSeconds > 30) {
            // 30秒以上才开始额外增加，使用对数避免爆炸
            durationFactor = 1.0 + Math.log10(durationInSeconds / 30.0);
            durationFactor = Math.min(3.0, durationFactor); // 上限3倍
        }
        
        // 基础增量：1.0-7.5之间
        double baseIncrease = amplifierFactor * durationFactor;

        // 适应度减免（0.1-1.0）
        double adaptationFactor = Math.max(0.1,
                1.0 - (adaptation / FleshRejectionConfig.adaptationThreshold) * 0.9
        );

        // 最终增量，严格限制在0.5-8.0之间
        double increase = baseIncrease * FleshRejectionConfig.potionRejectionFactor * adaptationFactor;
        increase = Math.min(8.0, Math.max(0.5, increase));

        // 发送药水排异提示
        if (increase > 0.1 && player.world.isRemote) {
            FleshRejectionHUDManager.onPotionRejection(player, (float)increase);
        }

        FleshRejectionSystem.setRejectionLevel(player,
                rejection + (float) increase
        );
    }

    @SubscribeEvent
    public void limitPotionCapacity(LivingEvent.LivingUpdateEvent event) {

        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        if (rejection <= FleshRejectionConfig.potionLimitStart) return;

        Collection<PotionEffect> active = player.getActivePotionEffects();
        int count = active.size();

        int maxAllowed = (int) Math.max(
                0,
                FleshRejectionConfig.potionMaxAtZero * (1.0 - rejection / FleshRejectionConfig.maxRejection)
        );

        if (count <= maxAllowed) return;

        // 超出，移除最弱效果
        PotionEffect weakest = null;
        for (PotionEffect e : active) {
            if (weakest == null) weakest = e;
            else {
                if (e.getAmplifier() < weakest.getAmplifier()) weakest = e;
                else if (e.getDuration() < weakest.getDuration()) weakest = e;
            }
        }

        if (weakest != null) {
            player.removePotionEffect(weakest.getPotion());
        }
    }
}