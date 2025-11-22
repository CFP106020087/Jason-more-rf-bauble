package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

/**
 * 饥饿与口渴管理模块
 *
 * 功能：
 *  - 自动恢复饱食度
 *  - 集成 SimpleDifficulty 口渴系统（如果已加载）
 *  - Lv.1: 基础代谢调节
 *  - Lv.2: 高效代谢
 *  - Lv.3: 完美代谢
 *
 * 能量消耗：
 *  - 饥饿恢复：10 * level RF/次
 *  - 口渴恢复：极低能耗（1-20 RF）
 */
public class HungerThirstModule extends AbstractMechCoreModule {

    public static final HungerThirstModule INSTANCE = new HungerThirstModule();

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

    private HungerThirstModule() {
        super(
            "HUNGER_THIRST",
            "代谢调节",
            "自动管理饥饿与口渴",
            3  // 最大等级
        );
    }

    private static void initializeReflection() {
        try {
            // 加载 SDCapabilities 类
            sdCapabilitiesClass = Class.forName("com.charles445.simpledifficulty.api.SDCapabilities");

            // 获取 THIRST 字段
            thirstCapability = sdCapabilitiesClass.getField("THIRST").get(null);

            // 获取 getCapability 方法
            getCapabilityMethod = EntityPlayer.class.getMethod("getCapability",
                    Class.forName("net.minecraftforge.common.capabilities.Capability"),
                    Class.forName("net.minecraft.util.EnumFacing"));

            // 加载 IThirstCapability 接口
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
            System.out.println("[HungerThirstModule] SimpleDifficulty 口渴系统反射初始化成功");
        } catch (Exception e) {
            REFLECTION_INITIALIZED = false;
            System.err.println("[HungerThirstModule] SimpleDifficulty 口渴系统反射初始化失败: " + e.getMessage());
        }
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 初始化计时器
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setLong("LAST_FOOD_RESTORE", player.world.getTotalWorldTime());
        meta.setLong("LAST_THIRST_RESTORE", player.world.getTotalWorldTime());
        meta.setBoolean("SYSTEM_ACTIVE", true);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 清除状态
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setBoolean("SYSTEM_ACTIVE", false);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        long currentTime = player.world.getTotalWorldTime();

        // 标记系统激活
        if (!meta.getBoolean("SYSTEM_ACTIVE")) {
            meta.setBoolean("SYSTEM_ACTIVE", true);
        }

        // 处理饥饿
        manageHunger(player, data, level, currentTime, meta);

        // 处理口渴（如果 SimpleDifficulty 已加载）
        if (SIMPLE_DIFFICULTY_LOADED && REFLECTION_INITIALIZED) {
            manageThirst(player, data, level, currentTime, meta);
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时重置计时器
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        long currentTime = player.world.getTotalWorldTime();
        meta.setLong("LAST_FOOD_RESTORE", currentTime);
        meta.setLong("LAST_THIRST_RESTORE", currentTime);
    }

    /**
     * 管理饥饿
     */
    private void manageHunger(EntityPlayer player, IMechCoreData data, int level, long currentTime, NBTTagCompound meta) {
        long lastRestore = meta.getLong("LAST_FOOD_RESTORE");

        // 每 120/80/40 秒恢复一次
        int restoreInterval = (160 - level * 40) * 20;

        if (currentTime - lastRestore >= restoreInterval) {
            if (player.getFoodStats().getFoodLevel() < 20) {
                // 恢复饱食度消耗能量
                int energyCost = 10 * level;
                if (data.consumeEnergy(energyCost)) {
                    // 等级越高恢复越多
                    int foodRestore = level;
                    float saturationRestore = 0.5F * level;

                    player.getFoodStats().addStats(foodRestore, saturationRestore);
                    meta.setLong("LAST_FOOD_RESTORE", currentTime);

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
     * 管理口渴（SimpleDifficulty 集成）
     */
    private void manageThirst(EntityPlayer player, IMechCoreData data, int level, long currentTime, NBTTagCompound meta) {
        try {
            // 通过反射获取口渴能力
            Object thirstCap = getCapabilityMethod.invoke(player, thirstCapability, null);
            if (thirstCap == null) return;

            long lastThirstRestore = meta.getLong("LAST_THIRST_RESTORE");
            int lastThirstStatus = meta.getInteger("LAST_THIRST_STATUS");

            // 根据等级设置不同的补水策略
            switch (level) {
                case 1:
                    manageThirstLevel1(player, data, thirstCap, currentTime, lastThirstRestore, meta);
                    break;
                case 2:
                    manageThirstLevel2(player, data, thirstCap, currentTime, lastThirstRestore, meta);
                    break;
                case 3:
                    manageThirstLevel3(player, data, thirstCap, currentTime, lastThirstRestore, meta);
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
    private void manageThirstLevel1(EntityPlayer player, IMechCoreData data, Object thirstCap,
                                   long currentTime, long lastRestore, NBTTagCompound meta) throws Exception {
        // 补水间隔：60 ticks
        if (currentTime - lastRestore < 60) return;

        int currentThirst = (int) getThirstLevelMethod.invoke(thirstCap);
        float currentSaturation = (float) getThirstSaturationMethod.invoke(thirstCap);

        // 维持在18点以上
        if (currentThirst < 18) {
            // 智能补水：缺得越多补得越多
            int restoreAmount = 2 + (18 - currentThirst) / 4;
            restoreAmount = Math.min(restoreAmount, 18 - currentThirst);

            // 消耗少量能量（维持性补水能耗很低）
            int energyCost = currentThirst < 10 ? 20 : 5; // 紧急时能耗稍高

            if (data.consumeEnergy(energyCost)) {
                addThirstLevelMethod.invoke(thirstCap, restoreAmount);
                addThirstSaturationMethod.invoke(thirstCap, 1.0F);

                meta.setLong("LAST_THIRST_RESTORE", currentTime);

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
    private void manageThirstLevel2(EntityPlayer player, IMechCoreData data, Object thirstCap,
                                   long currentTime, long lastRestore, NBTTagCompound meta) throws Exception {
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

            if (data.consumeEnergy(energyCost)) {
                addThirstLevelMethod.invoke(thirstCap, restoreAmount);
                addThirstSaturationMethod.invoke(thirstCap, 2.0F);

                // 清零口渴消耗
                setThirstExhaustionMethod.invoke(thirstCap, 0.0F);

                meta.setLong("LAST_THIRST_RESTORE", currentTime);

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
    private void manageThirstLevel3(EntityPlayer player, IMechCoreData data, Object thirstCap,
                                   long currentTime, long lastRestore, NBTTagCompound meta) throws Exception {
        // 补水间隔：20 ticks
        if (currentTime - lastRestore < 20) return;

        int currentThirst = (int) getThirstLevelMethod.invoke(thirstCap);
        float currentSaturation = (float) getThirstSaturationMethod.invoke(thirstCap);
        int lastStatus = meta.getInteger("LAST_THIRST_STATUS");

        // 始终维持满值
        if (currentThirst < 20) {
            // 直接补满
            int restoreAmount = 20 - currentThirst;

            // 几乎无能耗（1点象征性消耗）
            if (data.consumeEnergy(1)) {
                setThirstLevelMethod.invoke(thirstCap, 20);
                setThirstSaturationMethod.invoke(thirstCap, 5.0F); // 饱和度也满
                setThirstExhaustionMethod.invoke(thirstCap, 0.0F);

                meta.setLong("LAST_THIRST_RESTORE", currentTime);

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
        meta.setInteger("LAST_THIRST_STATUS", currentThirst);

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
    private void createWaterParticles(EntityPlayer player, int count) {
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

    @Override
    public int getPassiveEnergyCost(int level) {
        // 饥饿/口渴管理没有固定的被动消耗
        // 实际消耗取决于恢复频率
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 只要玩家饥饿或口渴，就可以执行
        return player.getFoodStats().getFoodLevel() < 20;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setLong("LAST_FOOD_RESTORE", 0);
        meta.setLong("LAST_THIRST_RESTORE", 0);
        meta.setBoolean("SYSTEM_ACTIVE", false);
        meta.setInteger("LAST_THIRST_STATUS", 20);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("LAST_FOOD_RESTORE")) {
            meta.setLong("LAST_FOOD_RESTORE", 0);
        }
        if (!meta.hasKey("LAST_THIRST_RESTORE")) {
            meta.setLong("LAST_THIRST_RESTORE", 0);
        }
        if (!meta.hasKey("SYSTEM_ACTIVE")) {
            meta.setBoolean("SYSTEM_ACTIVE", false);
        }
        if (!meta.hasKey("LAST_THIRST_STATUS")) {
            meta.setInteger("LAST_THIRST_STATUS", 20);
        }
        return true;
    }
}
