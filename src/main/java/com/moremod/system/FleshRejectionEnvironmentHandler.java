// FleshRejectionEnvironmentHandler.java - 干净版本
package com.moremod.system;

import com.moremod.config.FleshRejectionConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 血肉排异环境处理器
 * 负责处理饥饿、口渴、失眠、药水等环境因素对排异的影响
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class FleshRejectionEnvironmentHandler {

    // SimpleDifficulty 集成
    private static final boolean SD_LOADED = Loader.isModLoaded("simpledifficulty");
    private static Object thirstCapability;
    private static Method getCapabilityMethod;
    private static Method getThirstLevelMethod;

    // 防刷屏控制
    private static final Map<EntityPlayer, Long> lastPotionBlockMessage = new HashMap<>();
    private static final long MESSAGE_COOLDOWN = 3000; // 3秒冷却

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
     * 主更新循环 - 处理饥饿、口渴、失眠、睡眠恢复
     */
    @SubscribeEvent
    public void onPlayerTick(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 基础检查
        if (player.world.isRemote) return;
        if (!FleshRejectionConfig.enableRejectionSystem) return;
        if (!FleshRejectionSystem.hasMechanicalCore(player)) return;
        if (player.ticksExisted % 20 != 0) return; // 每秒执行

        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        float adaptation = FleshRejectionSystem.getAdaptationLevel(player);

        // 突破后免疫
        if (FleshRejectionSystem.hasTranscended(player) &&
                adaptation >= FleshRejectionConfig.adaptationThreshold) return;

        // 处理各种环境因素
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
     * 统一的药水处理 - 阻挡与排异增加
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 基础检查
        if (player.world.isRemote) return;
        if (!FleshRejectionConfig.enableRejectionSystem) return;
        if (!FleshRejectionSystem.hasMechanicalCore(player)) return;

        PotionEffect effect = event.getPotionEffect();
        if (effect == null || effect.getPotion().isBadEffect()) return;

        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        float adaptation = FleshRejectionSystem.getAdaptationLevel(player);

        // 突破后免疫
        if (FleshRejectionSystem.hasTranscended(player)) return;

        // 适应度满了排异归零
        if (adaptation >= FleshRejectionConfig.adaptationThreshold) {
            if (rejection > 0) {
                FleshRejectionSystem.setRejectionLevel(player, 0);
            }
            return;
        }

        // 阻挡逻辑
        boolean blocked = false;
        String blockReason = "";

        if (rejection >= 80) {
            // 80%+ 完全阻挡
            blocked = true;
            blockReason = "血肉完全排斥药剂";
            event.setResult(Event.Result.DENY);
        } else if (rejection >= 60) {
            // 60-80% 概率阻挡
            float blockChance = (rejection - 60) / 20f * 0.5f;
            if (player.world.rand.nextFloat() < blockChance) {
                blocked = true;
                blockReason = "身体抗拒药水效果";
                event.setResult(Event.Result.DENY);
            }
        } else if (rejection >= 40) {
            // 40-60% 低概率阻挡
            float blockChance = (rejection - 40) / 20f * 0.2f;
            if (player.world.rand.nextFloat() < blockChance) {
                blocked = true;
                blockReason = "药水效果被削弱";
                event.setResult(Event.Result.DENY);
            }
        }

        // 容量限制
        if (!blocked && rejection >= FleshRejectionConfig.potionLimitStart) {
            int currentEffects = player.getActivePotionEffects().size();
            int maxAllowed = Math.max(1, (int)(FleshRejectionConfig.potionMaxAtZero *
                    (1.0 - rejection / 100f)));

            if (currentEffects >= maxAllowed) {
                blocked = true;
                blockReason = String.format("药水容量已满 (%d/%d)", currentEffects, maxAllowed);
                event.setResult(Event.Result.DENY);
            }
        }

        // 发送阻挡提示
        if (blocked) {
            sendPotionBlockMessage(player, blockReason, rejection);
            player.world.playSound(null, player.getPosition(),
                    net.minecraft.init.SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    net.minecraft.util.SoundCategory.PLAYERS, 0.5f, 2.0f);
        } else {
            // 未阻挡则增加排异
            calculatePotionRejection(player, effect, rejection, adaptation);
        }
    }

    // ========== 内部处理方法 ==========

    private void handleHunger(EntityPlayer player, float rejection) {
        int food = player.getFoodStats().getFoodLevel();
        if (food < 20) {
            double hungerPain = Math.pow(20 - food, FleshRejectionConfig.hungerCurve) *
                    FleshRejectionConfig.hungerRejectionFactor;

            if (hungerPain > 0.1) {
                FleshRejectionSystem.setRejectionLevel(player, rejection + (float)hungerPain);
                // HUD提示由客户端处理
            }
        }
    }

    private void handleThirst(EntityPlayer player, float rejection) {
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

    private void handleInsomnia(EntityPlayer player, float rejection) {
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

    private void handleSleepRecovery(EntityPlayer player) {
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

    private void calculatePotionRejection(EntityPlayer player, PotionEffect effect,
                                          float currentRejection, float adaptation) {
        double amplifierFactor = 1.0 + Math.min(1.5, effect.getAmplifier() * 0.5);
        double durationInSeconds = effect.getDuration() / 20.0;
        double durationFactor = 1.0;

        if (durationInSeconds > 30) {
            durationFactor = 1.0 + Math.log10(durationInSeconds / 30.0);
            durationFactor = Math.min(3.0, durationFactor);
        }

        double baseIncrease = amplifierFactor * durationFactor;
        double adaptationFactor = Math.max(0.1,
                1.0 - (adaptation / FleshRejectionConfig.adaptationThreshold) * 0.9);

        double increase = baseIncrease * FleshRejectionConfig.potionRejectionFactor * adaptationFactor;
        increase = Math.min(8.0, Math.max(0.5, increase));

        FleshRejectionSystem.setRejectionLevel(player, currentRejection + (float) increase);
    }

    private void sendPotionBlockMessage(EntityPlayer player, String reason, float rejection) {
        Long lastTime = lastPotionBlockMessage.get(player);
        long now = System.currentTimeMillis();

        if (lastTime == null || now - lastTime > MESSAGE_COOLDOWN) {
            TextFormatting color = rejection >= 80 ? TextFormatting.DARK_RED :
                    rejection >= 60 ? TextFormatting.RED :
                            TextFormatting.GOLD;

            player.sendStatusMessage(
                    new TextComponentString(color + "⚠ " + reason),
                    true
            );
            lastPotionBlockMessage.put(player, now);
        }
    }

    private void checkRejectionWarning(EntityPlayer player, float rejection, float adaptation) {
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