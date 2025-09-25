package com.moremod.upgrades.survival;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

/**
 * 生存类升级效果管理器 - 完整版带能量检查
 */
public class SurvivalUpgradeManager {

    /**
     * 黄条护盾系统
     * 使用原版的Absorption Hearts机制
     */
    public static class YellowShieldSystem {
        private static final String NBT_SHIELD_COOLDOWN = "MechanicalCoreShieldCooldown";
        private static final String NBT_LAST_UPDATE = "MechanicalCoreShieldLastUpdate";
        private static final String NBT_SHIELD_ACTIVE = "MechanicalCoreShieldActive";
        private static final String NBT_LAST_ENERGY_CHECK = "MechanicalCoreShieldEnergyCheck";

        public static void updateShield(EntityPlayer player, ItemStack coreStack) {
            int level = ItemMechanicalCore.getUpgradeLevel(coreStack, "YELLOW_SHIELD");
            if (level <= 0) {
                // 移除所有吸收心
                if (player.getAbsorptionAmount() > 0) {
                    player.setAbsorptionAmount(0);
                }
                return;
            }

            // 检查升级是否激活
            if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "YELLOW_SHIELD")) {
                // 升级未激活，移除护盾
                if (player.getAbsorptionAmount() > 0) {
                    player.setAbsorptionAmount(0);

                    // 提示信息
                    if (player.getEntityData().getBoolean(NBT_SHIELD_ACTIVE)) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.YELLOW + "⚡ 护盾系统离线（能量不足）"
                        ), true);
                        player.getEntityData().setBoolean(NBT_SHIELD_ACTIVE, false);
                    }
                }
                return;
            }

            long cooldown = player.getEntityData().getLong(NBT_SHIELD_COOLDOWN);
            long currentTime = player.world.getTotalWorldTime();

            // 检查冷却
            if (currentTime < cooldown) {
                return;
            }

            // 护盾维持消耗能量（每秒）
            long lastEnergyCheck = player.getEntityData().getLong(NBT_LAST_ENERGY_CHECK);
            if (currentTime - lastEnergyCheck >= 20) {
                if (!ItemMechanicalCore.consumeEnergyForUpgradeBalanced(coreStack, "YELLOW_SHIELD", 10 * level)) {
                    // 能量不足，护盾开始衰减
                    float currentShield = player.getAbsorptionAmount();
                    if (currentShield > 0) {
                        player.setAbsorptionAmount(Math.max(0, currentShield - 1.0F));

                        if (player.world.getTotalWorldTime() % 60 == 0) { // 每3秒提示一次
                            player.sendStatusMessage(new TextComponentString(
                                    TextFormatting.YELLOW + "⚡ 护盾能量不足，正在衰减"
                            ), true);
                        }
                    }
                    player.getEntityData().setLong(NBT_LAST_ENERGY_CHECK, currentTime);
                    return;
                }
                player.getEntityData().setLong(NBT_LAST_ENERGY_CHECK, currentTime);
            }

            // 护盾上限：每级4点（2心）
            // Level 1: 4 HP = 2心
            // Level 2: 8 HP = 4心
            // Level 3: 12 HP = 6心
            float maxShield = level * 7.0F;
            float currentShield = player.getAbsorptionAmount();

            // 护盾恢复：每秒0.5点
            long lastUpdate = player.getEntityData().getLong(NBT_LAST_UPDATE);
            if (currentTime - lastUpdate >= 20) { // 每秒更新一次
                if (currentShield < maxShield) {
                    // 恢复护盾需要额外能量
                    if (ItemMechanicalCore.consumeEnergy(coreStack, 5)) {
                        float newShield = Math.min(currentShield + 0.5F, maxShield);
                        player.setAbsorptionAmount(newShield);
                        player.getEntityData().setLong(NBT_LAST_UPDATE, currentTime);

                        // 标记护盾激活
                        if (!player.getEntityData().getBoolean(NBT_SHIELD_ACTIVE)) {
                            player.getEntityData().setBoolean(NBT_SHIELD_ACTIVE, true);
                        }

                        // 显示护盾充能提示
                        if (newShield == maxShield && currentShield < maxShield) {
                            player.sendStatusMessage(new TextComponentString(
                                    TextFormatting.YELLOW + "💛 护盾已充满: " + (int)newShield + "/" + (int)maxShield
                            ), true);
                        }
                    }
                }
            }

            // 确保不超过最大值
            if (currentShield > maxShield) {
                player.setAbsorptionAmount(maxShield);
            }
        }

        public static void onShieldDepleted(EntityPlayer player) {
            // 当吸收心完全耗尽时触发冷却
            if (player.getAbsorptionAmount() <= 0) {
                long cooldownTime = player.world.getTotalWorldTime() + 600; // 30秒冷却
                player.getEntityData().setLong(NBT_SHIELD_COOLDOWN, cooldownTime);
                player.getEntityData().setBoolean(NBT_SHIELD_ACTIVE, false);

                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 护盾破碎！30秒后重新充能"
                ), true);

                // 粒子效果
                for (int i = 0; i < 20; i++) {
                    player.world.spawnParticle(
                            net.minecraft.util.EnumParticleTypes.CRIT,
                            player.posX + (player.getRNG().nextDouble() - 0.5) * player.width * 2,
                            player.posY + player.getRNG().nextDouble() * player.height,
                            player.posZ + (player.getRNG().nextDouble() - 0.5) * player.width * 2,
                            (player.getRNG().nextDouble() - 0.5) * 0.5,
                            player.getRNG().nextDouble() * 0.5,
                            (player.getRNG().nextDouble() - 0.5) * 0.5
                    );
                }
            }
        }
    }

    /**
     * 直接生命恢复系统（不使用药水）
     */
    public static class HealthRegenSystem {
        private static final String NBT_LAST_HEAL = "MechanicalCoreLastHeal";
        private static final String NBT_REGEN_ACTIVE = "MechanicalCoreRegenActive";

        public static void applyRegeneration(EntityPlayer player, ItemStack coreStack) {
            int level = ItemMechanicalCore.getUpgradeLevel(coreStack, "HEALTH_REGEN");
            if (level <= 0) return;

            // 生命恢复是生存必需，检查是否激活（在CRITICAL模式下仍能工作）
            if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "HEALTH_REGEN")) {
                // 提示信息
                if (player.getEntityData().getBoolean(NBT_REGEN_ACTIVE)) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.RED + "⚡ 生命恢复系统离线"
                    ), true);
                    player.getEntityData().setBoolean(NBT_REGEN_ACTIVE, false);
                }
                return;
            }

            // 恢复间隔：60/40/20 tick
            int interval = 80 - level * 20;

            long lastHeal = player.getEntityData().getLong(NBT_LAST_HEAL);
            long currentTime = player.world.getTotalWorldTime();

            if (currentTime - lastHeal >= interval) {
                if (player.getHealth() < player.getMaxHealth()) {
                    // 恢复消耗能量
                    if (ItemMechanicalCore.consumeEnergyForUpgrade(coreStack, "HEALTH_REGEN", 15 * level)) {
                        // 恢复量：0.5/1.0/1.5 心
                        player.heal(0.5F * level);
                        player.getEntityData().setLong(NBT_LAST_HEAL, currentTime);

                        // 标记系统激活
                        if (!player.getEntityData().getBoolean(NBT_REGEN_ACTIVE)) {
                            player.getEntityData().setBoolean(NBT_REGEN_ACTIVE, true);
                        }

                        // 粒子效果
                        if (player.world.rand.nextInt(3) == 0) {
                            for (int i = 0; i < 3; i++) {
                                player.world.spawnParticle(
                                        net.minecraft.util.EnumParticleTypes.HEART,
                                        player.posX + (player.getRNG().nextDouble() - 0.5),
                                        player.posY + player.getRNG().nextDouble() * 2,
                                        player.posZ + (player.getRNG().nextDouble() - 0.5),
                                        0, 0.05, 0
                                );
                            }
                        }
                    } else {
                        // 能量不足提示
                        if (player.world.getTotalWorldTime() % 100 == 0) {
                            player.sendStatusMessage(new TextComponentString(
                                    TextFormatting.YELLOW + "⚡ 生命恢复能量不足"
                            ), true);
                        }
                    }
                }
            }
        }
    }

    /**
     * 饥饿与口渴管理系统（整合SimpleDifficulty）
     */
    public static class HungerThirstSystem {
        private static final String NBT_LAST_FOOD_RESTORE = "MechanicalCoreLastFood";
        private static final String NBT_LAST_THIRST_RESTORE = "MechanicalCoreLastThirst";
        private static final String NBT_SYSTEM_ACTIVE = "MechanicalCoreHungerThirstActive";

        // SimpleDifficulty 反射缓存
        private static final boolean SIMPLE_DIFFICULTY_LOADED = Loader.isModLoaded("simpledifficulty");
        private static boolean REFLECTION_INITIALIZED = false;

        private static Class<?> sdCapabilitiesClass;
        private static Object thirstCapability;
        private static Method getCapabilityMethod;
        private static Method getThirstLevelMethod;
        private static Method addThirstLevelMethod;
        private static Method getThirstSaturationMethod;
        private static Method addThirstSaturationMethod;
        private static Method setThirstExhaustionMethod;
        private static Method isThirstyMethod;

        static {
            if (SIMPLE_DIFFICULTY_LOADED) {
                initializeReflection();
            }
        }

        private static void initializeReflection() {
            try {
                // 加载SDCapabilities类
                sdCapabilitiesClass = Class.forName("com.charles445.simpledifficulty.api.SDCapabilities");

                // 获取THIRST字段
                thirstCapability = sdCapabilitiesClass.getField("THIRST").get(null);

                // 获取getCapability方法
                getCapabilityMethod = EntityPlayer.class.getMethod("getCapability",
                        Class.forName("net.minecraftforge.common.capabilities.Capability"),
                        Class.forName("net.minecraft.util.EnumFacing"));

                // 加载IThirstCapability接口
                Class<?> thirstCapabilityClass = Class.forName("com.charles445.simpledifficulty.api.thirst.IThirstCapability");

                // 获取口渴相关方法
                getThirstLevelMethod = thirstCapabilityClass.getMethod("getThirstLevel");
                addThirstLevelMethod = thirstCapabilityClass.getMethod("addThirstLevel", int.class);
                getThirstSaturationMethod = thirstCapabilityClass.getMethod("getThirstSaturation");
                addThirstSaturationMethod = thirstCapabilityClass.getMethod("addThirstSaturation", float.class);
                setThirstExhaustionMethod = thirstCapabilityClass.getMethod("setThirstExhaustion", float.class);
                isThirstyMethod = thirstCapabilityClass.getMethod("isThirsty");

                REFLECTION_INITIALIZED = true;
                System.out.println("[SurvivalUpgrade] SimpleDifficulty口渴系统反射初始化成功");
            } catch (Exception e) {
                REFLECTION_INITIALIZED = false;
                System.err.println("[SurvivalUpgrade] SimpleDifficulty口渴系统反射初始化失败: " + e.getMessage());
            }
        }

        public static void manageFoodStats(EntityPlayer player, ItemStack coreStack) {
            int level = ItemMechanicalCore.getUpgradeLevel(coreStack, "HUNGER_THIRST");
            if (level <= 0) return;

            // 检查升级是否激活
            if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "HUNGER_THIRST")) {
                // 提示信息
                if (player.getEntityData().getBoolean(NBT_SYSTEM_ACTIVE)) {
                    if (player.world.getTotalWorldTime() % 1200 == 0) { // 每60秒提示一次
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.YELLOW + "⚡ 代谢调节系统离线"
                        ), true);
                    }
                    player.getEntityData().setBoolean(NBT_SYSTEM_ACTIVE, false);
                }
                return;
            }

            // 标记系统激活
            if (!player.getEntityData().getBoolean(NBT_SYSTEM_ACTIVE)) {
                player.getEntityData().setBoolean(NBT_SYSTEM_ACTIVE, true);
            }

            long currentTime = player.world.getTotalWorldTime();

            // 处理饥饿
            manageHunger(player, level, currentTime, coreStack);

            // 处理口渴（如果SimpleDifficulty已加载）
            if (SIMPLE_DIFFICULTY_LOADED && REFLECTION_INITIALIZED) {
                manageThirst(player, level, currentTime, coreStack);
            }
        }

        private static void manageHunger(EntityPlayer player, int level, long currentTime, ItemStack coreStack) {
            long lastRestore = player.getEntityData().getLong(NBT_LAST_FOOD_RESTORE);

            // 每120/80/40秒恢复一次
            int restoreInterval = (130 - level * 40) * 10;

            if (currentTime - lastRestore >= restoreInterval) {
                if (player.getFoodStats().getFoodLevel() < 20) {
                    // 恢复饱食度消耗能量
                    if (ItemMechanicalCore.consumeEnergyForUpgrade(coreStack, "HUNGER_THIRST", 10 * level)) {
                        player.getFoodStats().addStats(1, 0.5F);
                        player.getEntityData().setLong(NBT_LAST_FOOD_RESTORE, currentTime);

                        if (player.world.rand.nextInt(3) == 0) {
                            player.sendStatusMessage(new TextComponentString(
                                    TextFormatting.GOLD + "🍖 饥饿管理: +1 饱食度"
                            ), true);
                        }

                        // 高级别减缓饥饿消耗
                        if (level >= 2) {
                            player.getFoodStats().addExhaustion(-0.1F * (level - 1));
                        }
                    }
                }
            }
        }

        private static void manageThirst(EntityPlayer player, int level, long currentTime, ItemStack coreStack) {
            try {
                // 通过反射获取口渴能力
                Object thirstCap = getCapabilityMethod.invoke(player, thirstCapability, null);
                if (thirstCap == null) return;

                long lastThirstRestore = player.getEntityData().getLong(NBT_LAST_THIRST_RESTORE);

                // 每120/80/40秒恢复一次（与饥饿同步）
                int restoreInterval = (130 - level * 40) * 10;

                if (currentTime - lastThirstRestore >= restoreInterval) {
                    int currentThirst = (int) getThirstLevelMethod.invoke(thirstCap);
                    float currentSaturation = (float) getThirstSaturationMethod.invoke(thirstCap);
                    boolean isThirsty = (boolean) isThirstyMethod.invoke(thirstCap);

                    boolean restored = false;

                    // 紧急补水（口渴值低于10）
                    if (currentThirst < 10 || isThirsty) {
                        // 补水消耗能量
                        if (ItemMechanicalCore.consumeEnergyForUpgrade(coreStack, "HUNGER_THIRST", 15 * level)) {
                            int restoreAmount = 3 + level;  // 2/3/4点口渴值
                            addThirstLevelMethod.invoke(thirstCap, restoreAmount);
                            addThirstSaturationMethod.invoke(thirstCap, 1.0F * level);
                            restored = true;

                            if (player.world.rand.nextInt(3) == 0) {
                                player.sendStatusMessage(new TextComponentString(
                                        TextFormatting.AQUA + "💧 口渴管理: +" + restoreAmount + " 水分"
                                ), true);
                            }
                        }
                    }
                    // 常规补水（口渴值低于16）
                    else if (currentThirst < 16) {
                        if (ItemMechanicalCore.consumeEnergy(coreStack, 5)) {
                            addThirstLevelMethod.invoke(thirstCap, 1);
                            addThirstSaturationMethod.invoke(thirstCap, 0.5F);
                            restored = true;
                        }
                    }

                    // 高级别减缓口渴消耗
                    if (level >= 2) {
                        setThirstExhaustionMethod.invoke(thirstCap, 0.0f);
                    }

                    if (restored) {
                        player.getEntityData().setLong(NBT_LAST_THIRST_RESTORE, currentTime);

                        // 粒子效果
                        if (player.world.rand.nextInt(4) == 0) {
                            for (int i = 0; i < 3; i++) {
                                player.world.spawnParticle(
                                        net.minecraft.util.EnumParticleTypes.WATER_DROP,
                                        player.posX + (player.getRNG().nextDouble() - 0.5) * 0.5,
                                        player.posY + player.getRNG().nextDouble() * 0.5 + 1.0,
                                        player.posZ + (player.getRNG().nextDouble() - 0.5) * 0.5,
                                        0, -0.1, 0
                                );
                            }
                        }
                    }
                }

            } catch (Exception e) {
                // 静默处理，避免垃圾日志
            }
        }
    }

    /**
     * 反伤系统
     */
    public static class ThornsSystem {

        public static void applyThorns(EntityPlayer player, EntityLivingBase attacker, float originalDamage, int level) {
            if (level <= 0 || attacker == null) return;

            ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);

            // 反伤是被动系统，检查是否激活（在CRITICAL模式下仍能工作）
            if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "THORNS")) {
                return;
            }

            // 反伤不消耗额外能量（被动系统，已在主循环中扣除）

            // 反伤比例：15%/30%/45%
            float reflectRatio = 0.15F * level;
            float damage = originalDamage * reflectRatio;

            if (damage > 0) {
                attacker.attackEntityFrom(DamageSource.causeThornsDamage(player), damage);

                // 视觉效果
                player.world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.CRIT_MAGIC,
                        attacker.posX, attacker.posY + attacker.height / 2, attacker.posZ,
                        0, 0, 0
                );

                // 提示信息
                if (player.world.rand.nextInt(5) == 0) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.DARK_PURPLE + String.format("⚔ 反伤 %.1f 点", damage)
                    ), true);
                }
            }
        }
    }

    /**
     * 自动灭火系统
     */
    public static class FireExtinguishSystem {
        private static final String NBT_LAST_EXTINGUISH = "MechanicalCoreLastExtinguish";
        private static final String NBT_SYSTEM_ACTIVE = "MechanicalCoreExtinguishActive";

        public static void checkAndExtinguish(EntityPlayer player, ItemStack coreStack) {
            int level = ItemMechanicalCore.getUpgradeLevel(coreStack, "FIRE_EXTINGUISH");
            if (level <= 0) return;

            if (player.isBurning()) {
                // 灭火是紧急功能，检查是否激活（在CRITICAL模式下仍能工作）
                if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "FIRE_EXTINGUISH")) {
                    // 每20tick警告一次
                    if (player.world.getTotalWorldTime() % 20 == 0) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.RED + "⚡ 灭火系统离线！能量不足！"
                        ), true);
                    }
                    player.getEntityData().setBoolean(NBT_SYSTEM_ACTIVE, false);
                    return;
                }

                long lastExtinguish = player.getEntityData().getLong(NBT_LAST_EXTINGUISH);
                long currentTime = player.world.getTotalWorldTime();

                // 冷却时间：60/40/20 tick
                int cooldown = 80 - level * 20;

                if (currentTime - lastExtinguish >= cooldown) {
                    // 灭火消耗少量能量
                    if (ItemMechanicalCore.consumeEnergyForUpgrade(coreStack, "FIRE_EXTINGUISH", 50)) {
                        player.extinguish();
                        player.getEntityData().setLong(NBT_LAST_EXTINGUISH, currentTime);

                        // 标记系统激活
                        if (!player.getEntityData().getBoolean(NBT_SYSTEM_ACTIVE)) {
                            player.getEntityData().setBoolean(NBT_SYSTEM_ACTIVE, true);
                        }

                        // 粒子效果
                        for (int i = 0; i < 10; i++) {
                            player.world.spawnParticle(
                                    net.minecraft.util.EnumParticleTypes.WATER_SPLASH,
                                    player.posX + (player.getRNG().nextDouble() - 0.5) * player.width,
                                    player.posY + player.getRNG().nextDouble() * player.height,
                                    player.posZ + (player.getRNG().nextDouble() - 0.5) * player.width,
                                    0, 0.1, 0
                            );
                        }

                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.BLUE + "💧 自动灭火系统启动！"
                        ), true);
                    } else {
                        // 能量不足，无法灭火
                        if (player.world.getTotalWorldTime() % 40 == 0) {
                            player.sendStatusMessage(new TextComponentString(
                                    TextFormatting.DARK_RED + "⚡ 灭火系统能量不足！"
                            ), true);
                        }
                    }
                }
            } else {
                // 不在燃烧时重置状态
                if (player.getEntityData().getBoolean(NBT_SYSTEM_ACTIVE)) {
                    player.getEntityData().setBoolean(NBT_SYSTEM_ACTIVE, false);
                }
            }
        }
    }

    /**
     * 主更新方法
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return;

        // 获取能量状态
        EnergyDepletionManager.EnergyStatus status = ItemMechanicalCore.getEnergyStatus(coreStack);

        // 根据能量状态决定更新哪些系统

        // 生命支持模式：只保留最基础的生存系统
        if (status == EnergyDepletionManager.EnergyStatus.CRITICAL) {
            // 只更新生命恢复和自动灭火
            HealthRegenSystem.applyRegeneration(player, coreStack);
            FireExtinguishSystem.checkAndExtinguish(player, coreStack);

            // 关闭护盾
            if (player.getAbsorptionAmount() > 0) {
                player.setAbsorptionAmount(0);
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "💀 生命支持模式 - 护盾系统关闭"
                ), true);
            }
            return;
        }

        // 紧急模式：保留重要生存系统
        if (status == EnergyDepletionManager.EnergyStatus.EMERGENCY) {
            YellowShieldSystem.updateShield(player, coreStack);
            HealthRegenSystem.applyRegeneration(player, coreStack);
            FireExtinguishSystem.checkAndExtinguish(player, coreStack);
            // 代谢调节系统关闭
            return;
        }

        // 省电模式或正常模式：所有系统正常运行
        YellowShieldSystem.updateShield(player, coreStack);
        HealthRegenSystem.applyRegeneration(player, coreStack);
        HungerThirstSystem.manageFoodStats(player, coreStack);
        FireExtinguishSystem.checkAndExtinguish(player, coreStack);
    }

    /**
     * 伤害事件处理
     */
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return;

        // 检查护盾是否耗尽（在伤害后检查）
        if (event.getAmount() > 0) {
            // 使用延迟检查，因为吸收心会在伤害计算后更新
            player.world.getMinecraftServer().addScheduledTask(() -> {
                if (player.getAbsorptionAmount() <= 0) {
                    int shieldLevel = ItemMechanicalCore.getUpgradeLevel(coreStack, "YELLOW_SHIELD");
                    if (shieldLevel > 0) {
                        YellowShieldSystem.onShieldDepleted(player);
                    }
                }
            });
        }

        // 反伤处理
        if (event.getSource().getTrueSource() instanceof EntityLivingBase) {
            int thornsLevel = ItemMechanicalCore.getUpgradeLevel(coreStack, "THORNS");
            ThornsSystem.applyThorns(player, (EntityLivingBase) event.getSource().getTrueSource(),
                    event.getAmount(), thornsLevel);
        }
    }
}