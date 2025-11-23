package com.moremod.upgrades;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.potion.ModPotions;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 智能防水模块升级系统
 *
 * 等级1 - 日常防水：
 *   - 免疫浅水（水深<40%身高）
 *   - 免疫短暂水接触（<2秒）
 *   - 免疫高速通过水体
 *
 * 等级2 - 深度防水：
 *   - 包含等级1所有功能
 *   - 免疫中等深度水体（水深<70%身高）
 *   - 水下呼吸时间延长50%
 *   - 游泳速度+30%
 *
 * 等级3 - 潜水适应：
 *   - 完全防水
 *   - 水下呼吸
 *   - 水下夜视
 *   - 水下速度+100%
 *   - 水下挖掘速度+100%
 */
public class WaterproofUpgrade {

    // 防水模块的最大等级
    public static final int MAX_LEVEL = 3;

    // 支持的防水键名
    private static final String[] WATERPROOF_IDS = { "waterproof_module" };

    // 玩家状态追踪
    private static final Map<UUID, WaterState> lastWaterState = new HashMap<>();
    private static final Map<UUID, Long> lastWarningTime = new HashMap<>();
    private static final Map<UUID, Integer> malfunctionLevel = new HashMap<>();
    private static final Map<UUID, Long> lastEffectTime = new HashMap<>();
    private static final Map<UUID, Long> lastStatusTime = new HashMap<>();

    // 新增：水接触计时器和速度追踪
    private static final Map<UUID, Integer> waterContactTime = new HashMap<>();
    private static final Map<UUID, Double> lastPlayerSpeed = new HashMap<>();

    // 常量配置
    private static final long WARNING_COOLDOWN = 5000;      // 警告冷却时间(ms)
    private static final int  MALFUNCTION_DURATION = 200;   // 故障持续时间(tick)
    private static final int  WATER_DAMAGE_ENERGY = 50;     // 基础能量损耗

    // 智能检测阈值
    private static final int  BRIEF_CONTACT_THRESHOLD = 40;  // 2秒 = 短暂接触
    private static final double SPEED_IMMUNITY_THRESHOLD = 0.15; // 速度豁免阈值
    private static final float SHALLOW_WATER_DEPTH = 0.4f;   // 浅水深度（40%身高）
    private static final float MEDIUM_WATER_DEPTH = 0.7f;    // 中等深度（70%身高）

    private static final boolean DEBUG_MODE = false;

    // 水体接触状态
    private enum WaterState {
        NONE,       // 无水接触
        SHALLOW,    // 浅水（<40%身高）
        MEDIUM,     // 中等深度（40-70%身高）
        DEEP,       // 深水（70-100%身高）
        SUBMERGED   // 完全浸没（>100%身高）
    }

    /**
     * 在 onWornTick 调用
     */
    public static void applyWaterproofEffect(EntityPlayer player, ItemStack coreStack) {
        if (player.world.isRemote) return;

        UUID playerId = player.getUniqueID();

        // 检测当前水体状态
        WaterState currentState = detectWaterState(player);
        WaterState previousState = lastWaterState.getOrDefault(playerId, WaterState.NONE);

        // 获取有效防水等级
        int effectiveLevel = getEffectiveWaterproofLevel(coreStack);

        // 智能判断是否需要故障
        boolean shouldMalfunction = shouldCauseMalfunction(player, currentState, effectiveLevel);

        if (shouldMalfunction) {
            // 处理故障
            handleWaterMalfunction(player, coreStack, currentState, previousState);
        } else {
            // 无故障或有保护
            if (currentState != WaterState.NONE) {
                // 在水中但有保护
                handleWaterProtection(player, currentState, effectiveLevel);

                // 根据等级提供增益
                applyWaterBenefits(player, currentState, effectiveLevel);
            } else {
                // 离开水体
                if (previousState != WaterState.NONE) {
                    handleLeavingWater(player);
                }
            }

            // 清除故障效果
            clearMalfunctionEffects(player);
        }

        // 更新状态
        lastWaterState.put(playerId, currentState);
    }

    /**
     * 智能检测玩家当前的水体接触状态
     */
    private static WaterState detectWaterState(EntityPlayer player) {
        // 坐船时豁免
        if (player.isRiding() && player.getRidingEntity() instanceof EntityBoat) {
            return WaterState.NONE;
        }

        // 计算实际水深
        double waterDepth = getActualWaterDepth(player);

        if (waterDepth <= 0) {
            return WaterState.NONE;
        } else if (waterDepth < SHALLOW_WATER_DEPTH) {
            return WaterState.SHALLOW;
        } else if (waterDepth < MEDIUM_WATER_DEPTH) {
            return WaterState.MEDIUM;
        } else if (waterDepth < 1.0) {
            return WaterState.DEEP;
        } else {
            return WaterState.SUBMERGED;
        }
    }

    /**
     * 获取玩家实际浸水深度（0-1+，相对于玩家身高）
     */
    private static double getActualWaterDepth(EntityPlayer player) {
        // 检查玩家脚部位置
        BlockPos feetPos = new BlockPos(player.posX, player.posY, player.posZ);

        // 如果不在水中，返回0
        if (!player.isInWater() && player.world.getBlockState(feetPos).getMaterial() != Material.WATER) {
            return 0;
        }

        // 向上搜索找到水面
        double playerY = player.posY;
        for (int y = 0; y <= 3; y++) {
            BlockPos checkPos = feetPos.up(y);
            IBlockState state = player.world.getBlockState(checkPos);

            if (state.getMaterial() != Material.WATER) {
                // 找到水面，计算相对高度
                double waterSurface = checkPos.getY() - 0.125;
                double submergedHeight = waterSurface - playerY;
                return Math.max(0, submergedHeight / player.height);
            }
        }

        // 完全在水下
        return 1.5;
    }

    /**
     * 判断玩家是否在游泳（1.12.2兼容版本）
     */
    private static boolean isPlayerSwimming(EntityPlayer player) {
        // 在1.12.2中，通过以下条件判断游泳：
        // 1. 玩家在水中
        // 2. 玩家有水平移动
        // 3. 玩家不在地面上
        if (!player.isInWater()) {
            return false;
        }

        // 检查是否有水平移动
        double horizontalSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);

        // 检查是否不在地面（在游泳而不是涉水）
        boolean notOnGround = !player.onGround;

        // 检查玩家是否被水淹没超过一半
        BlockPos waistPos = new BlockPos(player.posX, player.posY + 0.8, player.posZ);
        boolean deepEnough = player.world.getBlockState(waistPos).getMaterial() == Material.WATER;

        return horizontalSpeed > 0.01 && (notOnGround || deepEnough);
    }

    /**
     * 智能判断是否应该造成故障
     */
    private static boolean shouldCauseMalfunction(EntityPlayer player, WaterState state, int level) {
        if (state == WaterState.NONE) return false;

        UUID playerId = player.getUniqueID();

        // 更新水接触时间
        updateWaterContactTime(player, state != WaterState.NONE);

        // 检查速度豁免
        if (hasSpeedImmunity(player)) {
            return false; // 高速移动时豁免
        }

        // 检查短暂接触豁免
        int contactTime = waterContactTime.getOrDefault(playerId, 0);
        if (contactTime < BRIEF_CONTACT_THRESHOLD) {
            return false; // 短暂接触豁免
        }

        // 游泳或潜行时的特殊处理
        if (player.isSneaking() || isPlayerSwimming(player)) {
            // 给予额外1级防护
            level++;
        }

        // 根据防水等级判断
        switch (level) {
            case 0:
                // 无防水：任何持续水接触都故障
                return true;

            case 1:
                // LV1：免疫浅水
                return state.ordinal() > WaterState.SHALLOW.ordinal();

            case 2:
                // LV2：免疫中等深度
                return state.ordinal() > WaterState.MEDIUM.ordinal();

            case 3:
            case 4: // 潜行/游泳加成
                // LV3：完全防水
                return false;

            default:
                return true;
        }
    }

    /**
     * 更新水接触时间
     */
    private static void updateWaterContactTime(EntityPlayer player, boolean inWater) {
        UUID playerId = player.getUniqueID();

        if (inWater) {
            int time = waterContactTime.getOrDefault(playerId, 0) + 1;
            waterContactTime.put(playerId, time);
        } else {
            // 离开水后快速重置
            int time = waterContactTime.getOrDefault(playerId, 0);
            if (time > 0) {
                waterContactTime.put(playerId, Math.max(0, time - 5));
            }
        }
    }

    /**
     * 检查是否有速度豁免
     */
    private static boolean hasSpeedImmunity(EntityPlayer player) {
        UUID playerId = player.getUniqueID();

        // 计算当前速度
        double currentSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        double lastSpeed = lastPlayerSpeed.getOrDefault(playerId, 0.0);

        // 更新速度记录
        lastPlayerSpeed.put(playerId, currentSpeed);

        // 平均速度超过阈值时豁免
        double avgSpeed = (currentSpeed + lastSpeed) / 2;
        return avgSpeed > SPEED_IMMUNITY_THRESHOLD;
    }

    /**
     * 处理水体造成的故障
     */
    private static void handleWaterMalfunction(EntityPlayer player, ItemStack coreStack,
                                               WaterState currentState, WaterState previousState) {
        UUID playerId = player.getUniqueID();
        long now = System.currentTimeMillis();

        // 首次进入故障状态
        if (previousState == WaterState.NONE || !shouldCauseMalfunction(player, previousState, getEffectiveWaterproofLevel(coreStack))) {
            sendMalfunctionWarning(player, currentState);
            malfunctionLevel.put(playerId, 0);
            player.world.playSound(null, player.getPosition(),
                    SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 1.0F, 0.5F);
        }

        // 消耗能量（根据水深调整）
        IEnergyStorage energy = coreStack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            int damage = calculateEnergyDamage(currentState);
            energy.extractEnergy(damage, false);
        }

        // 应用故障效果
        int malfLvl = calculateMalfunctionLevel(player, currentState);
        malfunctionLevel.put(playerId, malfLvl);

        // 施加故障效果
        player.addPotionEffect(new PotionEffect(
                ModPotions.MALFUNCTION,
                MALFUNCTION_DURATION,
                malfLvl,
                false,
                true
        ));

        // 定期警告
        Long lastWarn = lastWarningTime.get(playerId);
        if (lastWarn == null || now - lastWarn > WARNING_COOLDOWN) {
            sendPeriodicWarning(player, currentState, malfLvl);
            lastWarningTime.put(playerId, now);
        }

        // 严重故障时可能短路
        if (malfLvl >= 2 && energy != null && player.world.rand.nextInt(300) == 0) {
            int drain = energy.getEnergyStored() / 2;
            energy.extractEnergy(drain, false);
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "⚠ 严重故障：能量系统部分短路！损失 " + drain + " FE"
            ));
        }
    }

    /**
     * 计算能量损耗
     */
    private static int calculateEnergyDamage(WaterState state) {
        switch (state) {
            case SHALLOW:
                return WATER_DAMAGE_ENERGY / 2;      // 25 FE/tick
            case MEDIUM:
                return WATER_DAMAGE_ENERGY;          // 50 FE/tick
            case DEEP:
                return WATER_DAMAGE_ENERGY * 2;      // 100 FE/tick
            case SUBMERGED:
                return WATER_DAMAGE_ENERGY * 3;      // 150 FE/tick
            default:
                return 0;
        }
    }

    /**
     * 计算故障等级
     */
    private static int calculateMalfunctionLevel(EntityPlayer player, WaterState state) {
        UUID playerId = player.getUniqueID();
        int contactTime = waterContactTime.getOrDefault(playerId, 0);

        // 基础故障等级
        int baseLevel = 0;
        switch (state) {
            case SHALLOW:
                baseLevel = 0;
                break;
            case MEDIUM:
                baseLevel = 1;
                break;
            case DEEP:
            case SUBMERGED:
                baseLevel = 2;
                break;
        }

        // 长时间接触增加故障等级
        if (contactTime > 200) { // 10秒
            baseLevel = Math.min(baseLevel + 1, 2);
        }

        return baseLevel;
    }

    /**
     * 处理有防水保护时的状态
     */
    private static void handleWaterProtection(EntityPlayer player, WaterState state, int level) {
        UUID playerId = player.getUniqueID();
        long now = System.currentTimeMillis();
        Long lastStatus = lastStatusTime.get(playerId);

        if (lastStatus == null || now - lastStatus > 10000) {
            String message = getProtectionMessage(state, level);
            if (!message.isEmpty()) {
                player.sendStatusMessage(new TextComponentString(message), true);
                lastStatusTime.put(playerId, now);
            }
        }
    }

    /**
     * 应用水中增益效果（根据等级）
     */
    private static void applyWaterBenefits(EntityPlayer player, WaterState state, int level) {
        if (state == WaterState.NONE) return;

        switch (level) {
            case 1:
                // LV1：基础防水，无特殊增益
                break;

            case 2:
                // LV2：中级防水
                if (state.ordinal() >= WaterState.MEDIUM.ordinal()) {
                    // 延长氧气条
                    if (player.getAir() < 150) {
                        player.setAir(player.getAir() + 1);
                    }
                    // 游泳速度提升
                    player.addPotionEffect(new PotionEffect(
                            MobEffects.SPEED, 100, 0, true, false
                    ));
                }
                break;

            case 3:
                // LV3：完全防水 + 深海适应
                // 水下呼吸
                player.addPotionEffect(new PotionEffect(
                        MobEffects.WATER_BREATHING, 100, 0, true, false
                ));

                if (state.ordinal() >= WaterState.DEEP.ordinal()) {
                    // 深水增益
                    // 夜视
                    player.addPotionEffect(new PotionEffect(
                            MobEffects.NIGHT_VISION, 220, 0, true, false
                    ));
                    // 急迫（挖掘速度）
                    player.addPotionEffect(new PotionEffect(
                            MobEffects.HASTE, 100, 1, true, false
                    ));
                    // 速度II
                    player.addPotionEffect(new PotionEffect(
                            MobEffects.SPEED, 100, 1, true, false
                    ));
                    // 力量（游泳推进）
                    player.addPotionEffect(new PotionEffect(
                            MobEffects.STRENGTH, 100, 0, true, false
                    ));
                }
                break;
        }
    }

    /**
     * 离开水体时的处理
     */
    private static void handleLeavingWater(EntityPlayer player) {
        UUID playerId = player.getUniqueID();

        // 故障效果逐渐消退
        PotionEffect malfunction = player.getActivePotionEffect(ModPotions.MALFUNCTION);
        if (malfunction != null && malfunction.getDuration() > 60) {
            player.removePotionEffect(ModPotions.MALFUNCTION);
            player.addPotionEffect(new PotionEffect(
                    ModPotions.MALFUNCTION,
                    60,
                    Math.max(0, malfunction.getAmplifier() - 1),
                    false,
                    true
            ));
        }

        // 显示恢复消息
        int contactTime = waterContactTime.getOrDefault(playerId, 0);
        if (contactTime > BRIEF_CONTACT_THRESHOLD) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚡ 系统恢复中..."
            ), true);
        }

        // 清理状态
        malfunctionLevel.remove(playerId);
        lastEffectTime.remove(playerId);
    }

    /**
     * 清除故障效果
     *
     * 注意：不检查 WetnessSystem 潮湿值，避免循环依赖
     * 两个系统独立管理各自的 MALFUNCTION 效果
     */
    private static void clearMalfunctionEffects(EntityPlayer player) {
        // 移除水接触故障效果
        // WetnessSystem 会独立管理雨天潮湿故障
        UUID playerId = player.getUniqueID();
        if (malfunctionLevel.containsKey(playerId)) {
            player.removePotionEffect(ModPotions.MALFUNCTION);
            malfunctionLevel.remove(playerId);
        }
    }

    /**
     * 获取保护状态消息
     */
    private static String getProtectionMessage(WaterState state, int level) {
        switch (level) {
            case 1:
                if (state == WaterState.SHALLOW) {
                    return TextFormatting.AQUA + "✓ 基础防水涂层运作中";
                }
                break;
            case 2:
                if (state == WaterState.SHALLOW || state == WaterState.MEDIUM) {
                    return TextFormatting.BLUE + "✓ 深度防水系统激活";
                }
                break;
            case 3:
                return TextFormatting.DARK_AQUA + "✓ 潜水适应模式 - 完全防护";
        }
        return "";
    }

    /**
     * 发送故障警告
     */
    private static void sendMalfunctionWarning(EntityPlayer player, WaterState state) {
        String message;
        TextFormatting color;

        switch (state) {
            case SHALLOW:
                message = "⚠ 警告：检测到水体渗入！";
                color = TextFormatting.YELLOW;
                break;
            case MEDIUM:
                message = "⚠ 警告：水位上升，系统压力增大！";
                color = TextFormatting.GOLD;
                break;
            case DEEP:
                message = "⚠ 危险：深水压力，核心进水！";
                color = TextFormatting.RED;
                break;
            case SUBMERGED:
                message = "☠ 严重：完全浸没，系统故障！";
                color = TextFormatting.DARK_RED;
                break;
            default:
                return;
        }

        player.sendMessage(new TextComponentString(color + message));

        // 提示升级
        int currentLevel = getEffectiveWaterproofLevel(ItemMechanicalCore.getCoreFromPlayer(player));
        if (currentLevel < MAX_LEVEL) {
            String hint = getUpgradeHint(state, currentLevel);
            if (!hint.isEmpty()) {
                player.sendMessage(new TextComponentString(TextFormatting.GRAY + hint));
            }
        }
    }

    /**
     * 获取升级提示
     */
    private static String getUpgradeHint(WaterState state, int currentLevel) {
        switch (currentLevel) {
            case 0:
                return "提示：安装防水模块LV1可防止浅水损害";
            case 1:
                if (state.ordinal() > WaterState.SHALLOW.ordinal()) {
                    return "提示：升级至LV2可在更深的水中活动";
                }
                break;
            case 2:
                if (state.ordinal() > WaterState.MEDIUM.ordinal()) {
                    return "提示：升级至LV3获得完全防水和水下能力";
                }
                break;
        }
        return "";
    }

    /**
     * 发送周期性警告
     */
    private static void sendPeriodicWarning(EntityPlayer player, WaterState state, int malfunctionLevel) {
        String msg;
        TextFormatting color;

        if (malfunctionLevel >= 2) {
            msg = "☠ 核心严重进水！立即脱离水体！";
            color = TextFormatting.DARK_RED;
        } else if (malfunctionLevel >= 1) {
            msg = "⚠ 系统故障加剧，多个子系统失效！";
            color = TextFormatting.GOLD;
        } else {
            msg = "⚡ 检测到异常，开始出现轻微故障";
            color = TextFormatting.YELLOW;
        }

        player.sendStatusMessage(new TextComponentString(color + msg), true);
        player.world.playSound(null, player.getPosition(),
                SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT, SoundCategory.PLAYERS, 1.0F, 0.5F);
    }

    // ===================== 等级管理 =====================

    public static int getEffectiveWaterproofLevel(ItemStack coreStack) {
        if (isWaterproofDisabled(coreStack)) return 0;

        int level = getWaterproofLevel(coreStack);
        if (level <= 0) return 0;

        for (String id : WATERPROOF_IDS) {
            if (ItemMechanicalCore.isUpgradeActive(coreStack, id)) {
                return level;
            }
        }
        return 0;
    }

    private static boolean isWaterproofDisabled(ItemStack coreStack) {
        if (coreStack.isEmpty()) return false;
        NBTTagCompound nbt = coreStack.getTagCompound();
        if (nbt == null) return false;

        for (String id : WATERPROOF_IDS) {
            if (nbt.getBoolean("Disabled_" + id)) return true;
        }
        return false;
    }

    public static int getWaterproofLevel(ItemStack coreStack) {
        if (coreStack.isEmpty()) return 0;
        NBTTagCompound nbt = coreStack.getTagCompound();
        if (nbt == null) return 0;

        int level = 0;
        for (String id : WATERPROOF_IDS) {
            level = Math.max(level, nbt.getInteger("upgrade_" + id));
            level = Math.max(level, nbt.getInteger("upgrade_" + id.toLowerCase()));
            level = Math.max(level, nbt.getInteger("upgrade_" + id.toUpperCase()));
        }
        level = Math.max(level, nbt.getInteger("waterproofLevel"));

        return Math.min(level, MAX_LEVEL);
    }

    public static void setWaterproofLevel(ItemStack coreStack, int level) {
        NBTTagCompound nbt = coreStack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            coreStack.setTagCompound(nbt);
        }
        for (String id : WATERPROOF_IDS) {
            nbt.setInteger("upgrade_" + id, level);
            nbt.setInteger("upgrade_" + id.toLowerCase(), level);
            nbt.setInteger("upgrade_" + id.toUpperCase(), level);
        }
        nbt.setInteger("waterproofLevel", level);
        nbt.setBoolean("hasWaterproofModule", level > 0);
    }

    public static void setWaterproofDisabled(ItemStack coreStack, boolean disabled) {
        NBTTagCompound nbt = coreStack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            coreStack.setTagCompound(nbt);
        }
        for (String id : WATERPROOF_IDS) {
            nbt.setBoolean("Disabled_" + id, disabled);
        }
        if (DEBUG_MODE) System.out.println("[WaterproofUpgrade] disabled=" + disabled);
    }

    // ===================== GUI/信息 =====================

    public static ItemStack getUpgradeMaterial(int targetLevel) {
        switch (targetLevel) {
            case 1: return new ItemStack(Items.SLIME_BALL, 4);           // 史莱姆球
            case 2: return new ItemStack(Items.PRISMARINE_SHARD, 8);     // 海晶碎片
            case 3: return new ItemStack(Blocks.PRISMARINE, 4);          // 海晶石块
            default: return ItemStack.EMPTY;
        }
    }

    public static String getUpgradeDescription(ItemStack coreStack) {
        int level = getWaterproofLevel(coreStack);
        boolean disabled = isWaterproofDisabled(coreStack);
        int effectiveLevel = getEffectiveWaterproofLevel(coreStack);

        StringBuilder sb = new StringBuilder();
        if (level == 0) {
            sb.append(TextFormatting.GRAY).append("未安装 - 任何持续水接触都会故障");
        } else {
            sb.append(TextFormatting.WHITE).append("等级 ").append(level).append("/").append(MAX_LEVEL);
            if (disabled) sb.append(TextFormatting.RED).append(" [已禁用]");
            else if (effectiveLevel == 0) sb.append(TextFormatting.YELLOW).append(" [暂停]");
            else sb.append(TextFormatting.GREEN).append(" [激活]");
            sb.append("\n");
            switch (level) {
                case 1:
                    sb.append(TextFormatting.AQUA).append("日常防水");
                    sb.append("\n").append(TextFormatting.GREEN).append("  ✓ 免疫浅水(<40%深度)");
                    sb.append("\n").append(TextFormatting.GREEN).append("  ✓ 免疫短暂接触(<2秒)");
                    sb.append("\n").append(TextFormatting.GREEN).append("  ✓ 快速通过豁免");
                    sb.append("\n").append(TextFormatting.GRAY).append("  ✗ 不防中深水");
                    break;
                case 2:
                    sb.append(TextFormatting.BLUE).append("深度防水");
                    sb.append("\n").append(TextFormatting.GREEN).append("  ✓ 免疫中等深度(<70%)");
                    sb.append("\n").append(TextFormatting.GREEN).append("  ✓ 氧气条延长50%");
                    sb.append("\n").append(TextFormatting.GREEN).append("  ✓ 游泳速度+30%");
                    sb.append("\n").append(TextFormatting.GRAY).append("  ✗ 不防完全浸没");
                    break;
                case 3:
                    sb.append(TextFormatting.DARK_AQUA).append("潜水适应");
                    sb.append("\n").append(TextFormatting.GREEN).append("  ✓ 完全防水");
                    sb.append("\n").append(TextFormatting.GREEN).append("  ✓ 水下呼吸");
                    sb.append("\n").append(TextFormatting.GREEN).append("  ✓ 水下夜视");
                    sb.append("\n").append(TextFormatting.GREEN).append("  ✓ 水下速度+100%");
                    sb.append("\n").append(TextFormatting.GREEN).append("  ✓ 水下挖掘+100%");
                    break;
            }
        }
        return sb.toString();
    }

    public static String getUpgradeDescription(int level) {
        switch (level) {
            case 0: return TextFormatting.GRAY + "未安装 - 任何持续水接触都会故障";
            case 1: return TextFormatting.AQUA + "日常防水 - 浅水保护+智能豁免";
            case 2: return TextFormatting.BLUE + "深度防水 - 中等深度+游泳增强";
            case 3: return TextFormatting.DARK_AQUA + "潜水适应 - 完全防水+水下超能力";
            default: return "";
        }
    }

    public static void cleanupPlayer(EntityPlayer player) {
        UUID id = player.getUniqueID();
        lastWaterState.remove(id);
        lastWarningTime.remove(id);
        malfunctionLevel.remove(id);
        lastEffectTime.remove(id);
        lastStatusTime.remove(id);
        waterContactTime.remove(id);
        lastPlayerSpeed.remove(id);
    }
}