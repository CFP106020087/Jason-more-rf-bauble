package com.moremod.upgrades.survival;

import com.moremod.config.BrokenGodConfig;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.system.humanity.AscensionRoute;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
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
 * 生存类升级效果管理器 - 强化补水版本
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

            // 确保不超过最大值（破碎之神玩家不受限制）
            IHumanityData data = HumanityCapabilityHandler.getData(player);
            if (data != null && data.getAscensionRoute() == AscensionRoute.BROKEN_GOD) {
                // 破碎之神：不限制吸收之心上限，通过击杀可无限累积
                // 只有非破碎之神玩家才受护盾上限限制
            } else {
                // 普通玩家：限制在护盾等级决定的上限
                if (currentShield > maxShield) {
                    player.setAbsorptionAmount(maxShield);
                }
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
     * 饥饿与口渴管理系统（强化版本）
     */
    public static class HungerThirstSystem {
        private static final String NBT_LAST_FOOD_RESTORE = "MechanicalCoreLastFood";
        private static final String NBT_LAST_THIRST_RESTORE = "MechanicalCoreLastThirst";
        private static final String NBT_SYSTEM_ACTIVE = "MechanicalCoreHungerThirstActive";
        private static final String NBT_LAST_THIRST_STATUS = "MechanicalCoreLastThirstStatus";

        // SimpleDifficulty 反射缓存
        private static final boolean SIMPLE_DIFFICULTY_LOADED = Loader.isModLoaded("simpledifficulty");
        private static boolean REFLECTION_INITIALIZED = false;

        private static Class<?> sdCapabilitiesClass;
        private static Object thirstCapability;
        private static Method getCapabilityMethod;
        private static Method getThirstLevelMethod;
        private static Method addThirstLevelMethod;
        private static Method setThirstLevelMethod;
        private static Method getThirstSaturationMethod;
        private static Method addThirstSaturationMethod;
        private static Method setThirstSaturationMethod;
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
                setThirstLevelMethod = thirstCapabilityClass.getMethod("setThirstLevel", int.class);
                getThirstSaturationMethod = thirstCapabilityClass.getMethod("getThirstSaturation");
                addThirstSaturationMethod = thirstCapabilityClass.getMethod("addThirstSaturation", float.class);
                setThirstSaturationMethod = thirstCapabilityClass.getMethod("setThirstSaturation", float.class);
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

            // 处理口渴（如果SimpleDifficulty已加载） - 强化版本
            if (SIMPLE_DIFFICULTY_LOADED && REFLECTION_INITIALIZED) {
                manageThirstEnhanced(player, level, currentTime, coreStack);
            }
        }

        private static void manageHunger(EntityPlayer player, int level, long currentTime, ItemStack coreStack) {
            long lastRestore = player.getEntityData().getLong(NBT_LAST_FOOD_RESTORE);

            // 每120/80/40秒恢复一次
            int restoreInterval = (160 - level * 40) * 20;

            if (currentTime - lastRestore >= restoreInterval) {
                if (player.getFoodStats().getFoodLevel() < 20) {
                    // 恢复饱食度消耗能量
                    if (ItemMechanicalCore.consumeEnergyForUpgrade(coreStack, "HUNGER_THIRST", 10 * level)) {
                        // 等级越高恢复越多
                        int foodRestore = level;
                        float saturationRestore = 0.5F * level;

                        player.getFoodStats().addStats(foodRestore, saturationRestore);
                        player.getEntityData().setLong(NBT_LAST_FOOD_RESTORE, currentTime);

                        if (player.world.rand.nextInt(3) == 0) {
                            player.sendStatusMessage(new TextComponentString(
                                    TextFormatting.GOLD + "🍖 饥饿管理: +" + foodRestore + " 饱食度"
                            ), true);
                        }

                        // 高级别减缓饥饿消耗
                        if (level >= 2) {
                            player.getFoodStats().addExhaustion(-0.2F * (level - 1));
                        }
                    }
                }
            }
        }

        /**
         * 强化版口渴管理
         */
        private static void manageThirstEnhanced(EntityPlayer player, int level, long currentTime, ItemStack coreStack) {
            try {
                // 通过反射获取口渴能力
                Object thirstCap = getCapabilityMethod.invoke(player, thirstCapability, null);
                if (thirstCap == null) return;

                long lastThirstRestore = player.getEntityData().getLong(NBT_LAST_THIRST_RESTORE);
                int lastThirstStatus = player.getEntityData().getInteger(NBT_LAST_THIRST_STATUS);

                // 根据等级设置不同的补水策略
                switch (level) {
                    case 1:
                        manageThirstLevel1(player, thirstCap, currentTime, lastThirstRestore, coreStack);
                        break;
                    case 2:
                        manageThirstLevel2(player, thirstCap, currentTime, lastThirstRestore, coreStack);
                        break;
                    case 3:
                        manageThirstLevel3(player, thirstCap, currentTime, lastThirstRestore, coreStack);
                        break;
                }

            } catch (Exception e) {
                // 静默处理
            }
        }

        /**
         * 等级1：基础水分管理
         * - 补水间隔：60 ticks (3秒)
         * - 维持在18点以上
         * - 智能补水量
         */
        private static void manageThirstLevel1(EntityPlayer player, Object thirstCap, long currentTime,
                                               long lastRestore, ItemStack coreStack) throws Exception {
            // 补水间隔：60 ticks
            if (currentTime - lastRestore < 60) return;

            int currentThirst = (int) getThirstLevelMethod.invoke(thirstCap);
            float currentSaturation = (float) getThirstSaturationMethod.invoke(thirstCap);
            boolean isThirsty = (boolean) isThirstyMethod.invoke(thirstCap);

            // 维持在18点以上
            if (currentThirst < 18) {
                // 智能补水：缺得越多补得越多
                int restoreAmount = 2 + (18 - currentThirst) / 4;
                restoreAmount = Math.min(restoreAmount, 18 - currentThirst);

                // 消耗少量能量（维持性补水能耗很低）
                int energyCost = currentThirst < 10 ? 20 : 5; // 紧急时能耗稍高

                if (ItemMechanicalCore.consumeEnergy(coreStack, energyCost)) {
                    addThirstLevelMethod.invoke(thirstCap, restoreAmount);
                    addThirstSaturationMethod.invoke(thirstCap, 1.0F);

                    player.getEntityData().setLong(NBT_LAST_THIRST_RESTORE, currentTime);

                    // 提示信息
                    if (player.world.rand.nextInt(5) == 0 || currentThirst < 10) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.AQUA + String.format("💧 水分管理: %d→%d",
                                        currentThirst, Math.min(20, currentThirst + restoreAmount))
                        ), true);
                    }

                    // 粒子效果
                    createWaterParticles(player, 3);
                }
            }
        }

        /**
         * 等级2：高效水循环
         * - 补水间隔：40 ticks (2秒)
         * - 维持在19点以上
         * - 清零口渴消耗
         */
        private static void manageThirstLevel2(EntityPlayer player, Object thirstCap, long currentTime,
                                               long lastRestore, ItemStack coreStack) throws Exception {
            // 补水间隔：40 ticks
            if (currentTime - lastRestore < 40) return;

            int currentThirst = (int) getThirstLevelMethod.invoke(thirstCap);
            float currentSaturation = (float) getThirstSaturationMethod.invoke(thirstCap);

            // 维持在19点以上
            if (currentThirst < 19) {
                // 智能补水
                int restoreAmount = 3 + (19 - currentThirst) / 3;
                restoreAmount = Math.min(restoreAmount, 19 - currentThirst);

                // 极低能耗
                int energyCost = currentThirst < 10 ? 15 : 3;

                if (ItemMechanicalCore.consumeEnergy(coreStack, energyCost)) {
                    addThirstLevelMethod.invoke(thirstCap, restoreAmount);
                    addThirstSaturationMethod.invoke(thirstCap, 2.0F);

                    // 清零口渴消耗
                    setThirstExhaustionMethod.invoke(thirstCap, 0.0F);

                    player.getEntityData().setLong(NBT_LAST_THIRST_RESTORE, currentTime);

                    // 提示信息
                    if (currentThirst < 15 || player.world.rand.nextInt(8) == 0) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.BLUE + String.format("💧 高效水循环: %d→%d",
                                        currentThirst, Math.min(20, currentThirst + restoreAmount))
                        ), true);
                    }

                    createWaterParticles(player, 5);
                }
            } else {
                // 即使在高水分时也清零消耗
                if (currentTime % 20 == 0) {
                    setThirstExhaustionMethod.invoke(thirstCap, 0.0F);
                }
            }
        }

        /**
         * 等级3：完美水合状态
         * - 补水间隔：20 ticks (1秒)
         * - 始终维持满值20
         * - 完全免疫口渴
         */
        private static void manageThirstLevel3(EntityPlayer player, Object thirstCap, long currentTime,
                                               long lastRestore, ItemStack coreStack) throws Exception {
            // 补水间隔：20 ticks
            if (currentTime - lastRestore < 20) return;

            int currentThirst = (int) getThirstLevelMethod.invoke(thirstCap);
            float currentSaturation = (float) getThirstSaturationMethod.invoke(thirstCap);
            int lastStatus = player.getEntityData().getInteger(NBT_LAST_THIRST_STATUS);

            // 始终维持满值
            if (currentThirst < 20) {
                // 直接补满
                int restoreAmount = 20 - currentThirst;

                // 几乎无能耗（1点象征性消耗）
                if (ItemMechanicalCore.consumeEnergy(coreStack, 1)) {
                    setThirstLevelMethod.invoke(thirstCap, 20);
                    setThirstSaturationMethod.invoke(thirstCap, 5.0F); // 饱和度也满
                    setThirstExhaustionMethod.invoke(thirstCap, 0.0F);

                    player.getEntityData().setLong(NBT_LAST_THIRST_RESTORE, currentTime);

                    // 只在水分刚满时提示
                    if (lastStatus < 20) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.DARK_AQUA + "💧 完美水合: 水分始终充足"
                        ), true);
                    }

                    createWaterParticles(player, 8);
                }
            } else {
                // 保持满值状态
                if (currentTime % 10 == 0) {
                    setThirstExhaustionMethod.invoke(thirstCap, 0.0F);
                    if (currentSaturation < 5.0F) {
                        setThirstSaturationMethod.invoke(thirstCap, 5.0F);
                    }
                }
            }

            // 记录状态
            player.getEntityData().setInteger(NBT_LAST_THIRST_STATUS, currentThirst);

            // 环境适应：炎热环境额外保护
            if (player.world.getBiome(player.getPosition()).getTemperature(player.getPosition()) > 1.0F) {
                if (currentTime % 40 == 0) {
                    setThirstLevelMethod.invoke(thirstCap, 20);
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.DARK_AQUA + "💧 炎热环境保护激活"
                    ), true);
                }
            }
        }

        /**
         * 创建水滴粒子效果
         */
        private static void createWaterParticles(EntityPlayer player, int count) {
            if (player.world.rand.nextInt(4) == 0) {
                for (int i = 0; i < count; i++) {
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