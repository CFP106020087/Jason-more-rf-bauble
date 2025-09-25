package com.moremod.upgrades;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.potion.ModPotions;

import net.minecraft.block.material.Material;
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
 * 防水模块升级效果（雨天免疫 + 搭船豁免 + 仅浸没触发）
 */
public class WaterproofUpgrade {

    // 防水模块的最大等级
    public static final int MAX_LEVEL = 3;

    // 支持的防水键名（兼容多写法）
    private static final String[] WATERPROOF_IDS = { "waterproof_module" };

    // 玩家状态
    private static final Map<UUID, Boolean> wasInWater = new HashMap<>();
    private static final Map<UUID, Long> lastWarningTime = new HashMap<>();
    private static final Map<UUID, Integer> malfunctionLevel = new HashMap<>();
    private static final Map<UUID, Long> lastEffectTime = new HashMap<>();
    private static final Map<UUID, Long> lastDebugTime = new HashMap<>();

    // 常量配置
    private static final long WARNING_COOLDOWN = 5000;    // ms
    private static final int  MALFUNCTION_DURATION = 200; // tick
    private static final int  WATER_DAMAGE_ENERGY = 100;  // FE per tick when unprotected in water
    private static final boolean DEBUG_MODE = false;

    // ========== 可选：从配置读取（没有配置也有合理默认） ==========
    private static boolean cfgIgnoreRain() {
        try {
            // 如果你有类似 com.moremod.config.MalfunctionConfig.environment.ignoreRain
            return com.moremod.config.MalfunctionConfig.environment.ignoreRain;
        } catch (Throwable t) {
            return true; // 默认雨天免疫
        }
    }
    private static boolean cfgAllowBoats() {
        try {
            // 如果你有类似 com.moremod.config.MalfunctionConfig.environment.allowBoats
            return com.moremod.config.MalfunctionConfig.environment.allowBoats;
        } catch (Throwable t) {
            return true; // 默认允许坐船豁免
        }
    }

    /**
     * 在 onWornTick 调用
     */
    public static void applyWaterproofEffect(EntityPlayer player, ItemStack coreStack) {
        if (player.world.isRemote) return;

        UUID playerId = player.getUniqueID();

        // 檢測是否在淋雨（新增）
        boolean inRain = isPlayerInRain(player);
        boolean submerged = isPlayerSubmerged(player);
        boolean wasInWaterBefore = wasInWater.getOrDefault(playerId, false);

        int effectiveLevel = getEffectiveWaterproofLevel(coreStack);

        if (effectiveLevel <= 0) {
            // 無防水保護
            if (submerged) {
                // 水中 = 完整故障
                handleWaterDamage(player, coreStack, wasInWaterBefore);
            } else if (inRain) {
                // 淋雨 = 輕微故障（新增）
                handleRainDamage(player, coreStack);
            } else {
                // 離開水體/雨天
                if (wasInWaterBefore) {
                    handleLeavingWater(player);
                }
                handleLeavingRain(player); // 新增
            }
        } else {
            // 有防水保護
            if (submerged) {
                // 原有的水下保護邏輯...
            } else if (inRain) {
                // 防水模塊保護淋雨（新增）
                handleRainProtection(player, effectiveLevel);
            }

            // 清除所有故障效果
            if (player.isPotionActive(ModPotions.MALFUNCTION)) {
                player.removePotionEffect(ModPotions.MALFUNCTION);
            }
            if (player.isPotionActive(ModPotions.MINOR_MALFUNCTION)) {
                player.removePotionEffect(ModPotions.MINOR_MALFUNCTION);
            }
        }

        wasInWater.put(playerId, submerged || inRain);
    }

    /**
     * 檢測玩家是否在淋雨
     */
    private static boolean isPlayerInRain(EntityPlayer player) {
        // 不在室內 + 世界在下雨 + 能看到天空
        if (!player.world.isRaining()) {
            return false;
        }

        // 檢查頭頂是否能看到天空
        BlockPos pos = player.getPosition();
        return player.world.canSeeSky(pos.up()) &&
                player.world.getPrecipitationHeight(pos).getY() <= pos.getY() + 1;
    }

    /**
     * 處理淋雨傷害（新方法）
     */
    private static void handleRainDamage(EntityPlayer player, ItemStack coreStack) {
        UUID playerId = player.getUniqueID();
        long now = System.currentTimeMillis();

        // 首次淋雨警告
        Long lastRainWarn = lastRainWarning.get(playerId);
        if (lastRainWarn == null || now - lastRainWarn > 30000) { // 30秒冷卻
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚠ 警告：雨水滲入機械核心外殼！"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "提示：防水模塊可防止雨水損害"
            ));
            lastRainWarning.put(playerId, now);
        }

        // 施加輕微故障效果
        int level = 0;

        // 雷雨時效果加重
        if (player.world.isThundering()) {
            level = 1;
            if (now - lastRainWarn > 60000) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GOLD + "⚡ 雷暴天氣導致干擾增強！"
                ), true);
            }
        }

        // 應用輕微故障
        player.addPotionEffect(new PotionEffect(
                ModPotions.MINOR_MALFUNCTION,
                100,  // 5秒持續
                level,
                false,
                true
        ));

        // 少量能量流失
        IEnergyStorage energy = coreStack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            energy.extractEnergy(5, false); // 每tick 10 FE（比水中少很多）
        }
    }

    /**
     * 處理雨天防護（新方法）
     */
    private static void handleRainProtection(EntityPlayer player, int level) {
        // 首次進入雨中的提示
        Long lastProtect = lastRainProtection.get(player.getUniqueID());
        long now = System.currentTimeMillis();

        if (lastProtect == null || now - lastProtect > 60000) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 防水塗層阻擋雨水侵蝕"
            ), true);
            lastRainProtection.put(player.getUniqueID(), now);

            // 消耗少量能量維持防護
            ItemMechanicalCore.consumeEnergy(
                    ItemMechanicalCore.getCoreFromPlayer(player),
                    1, // 極少能量消耗
                    true
            );
        }
    }

    /**
     * 離開雨天時的處理（新方法）
     */
    private static void handleLeavingRain(EntityPlayer player) {
        // 移除輕微故障效果
        if (player.isPotionActive(ModPotions.MINOR_MALFUNCTION)) {
            int remaining = player.getActivePotionEffect(ModPotions.MINOR_MALFUNCTION).getDuration();
            if (remaining > 60) {
                // 縮短到3秒
                player.removePotionEffect(ModPotions.MINOR_MALFUNCTION);
                player.addPotionEffect(new PotionEffect(
                        ModPotions.MINOR_MALFUNCTION,
                        60,
                        0,
                        false,
                        true
                ));
            }

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "系統乾燥中..."
            ), true);
        }
    }

    // 添加新的狀態追蹤
    private static final Map<UUID, Long> lastRainWarning = new HashMap<>();
    private static final Map<UUID, Long> lastRainProtection = new HashMap<>();

    // ===================== 有效等级判定 =====================

    public static int getEffectiveWaterproofLevel(ItemStack coreStack) {
        if (isWaterproofDisabled(coreStack)) return 0;

        int level = getWaterproofLevel(coreStack);
        if (level <= 0) return 0;

        // 能量/暂停状态由统一接口判定
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
        level = Math.max(level, nbt.getInteger("waterproofLevel")); // 兼容旧键

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
            nbt.setBoolean("HasUpgrade_" + id, level > 0);
        }
        nbt.setInteger("waterproofLevel", level); // 兼容
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

    // ===================== “进水”判定（新版） =====================

    /**
     * 仅在“真正浸没”时返回 true：
     * - 眼睛所在格是水，或
     * - 实体与水材质相交（isInsideOfMaterial）
     * 特别规则：
     * - 坐船时（EntityBoat）直接豁免
     * - 雨天/雷雨不算进水
     */
    private static boolean isPlayerSubmerged(EntityPlayer player) {
        // 坐船豁免
        if (cfgAllowBoats() && player.isRiding() && player.getRidingEntity() instanceof EntityBoat) {
            return false;
        }

        // 雨天免疫
        if (cfgIgnoreRain() && player.world.isRaining()) {
            // 以前这里会在雷暴时也算进水，现全部忽略
            // 只要不是“眼睛进水”，就不判定
        }

        // 眼睛所处方块
        BlockPos eye = new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        if (player.world.getBlockState(eye).getMaterial() == Material.WATER) {
            return true;
        }

        // 实体判定（包围盒与水材质重叠）
        if (player.isInsideOfMaterial(Material.WATER)) {
            return true;
        }

        // 脚下浅水不算（降低“过度敏感”）
        // 原实现会检查 player.isInWater() 或脚下是水，这里不采用

        return false;
    }

    // ===================== 无防水时的水体影响 =====================

    private static void handleWaterDamage(EntityPlayer player, ItemStack coreStack, boolean wasInWaterBefore) {
        UUID playerId = player.getUniqueID();
        long now = System.currentTimeMillis();

        if (!wasInWaterBefore) {
            int originalLevel = getWaterproofLevel(coreStack);
            if (originalLevel > 0) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "⚠ 警告：防水模块已暂停或禁用！"
                ));
            }
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 检测到水体侵入！机械核心开始故障！"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "提示：启用防水模块（等级>0且未禁用）可避免水体损害"
            ));

            malfunctionLevel.put(playerId, 0);
            player.world.playSound(null, player.getPosition(),
                    SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 1.0F, 0.5F);
        }

        IEnergyStorage energy = coreStack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            energy.extractEnergy(WATER_DAMAGE_ENERGY, false);
        }

        int curLv = malfunctionLevel.getOrDefault(playerId, 0);
        Long lastEff = lastEffectTime.get(playerId);
        if (lastEff == null || now - lastEff > 5000) { // 每5秒升级一次，封顶2
            if (curLv < 2) {
                curLv++;
                malfunctionLevel.put(playerId, curLv);
            }
            lastEffectTime.put(playerId, now);
        }

        applyMalfunctionEffect(player, curLv);

        Long lastWarn = lastWarningTime.get(playerId);
        if (lastWarn == null || now - lastWarn > WARNING_COOLDOWN) {
            sendWaterDamageWarning(player, curLv);
            lastWarningTime.put(playerId, now);
        }

        if (curLv >= 2 && energy != null && player.world.rand.nextInt(200) == 0) {
            energy.extractEnergy(energy.getEnergyStored(), false);
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "☠ 致命错误：能量系统短路！"
            ));
        }
    }

    private static void handleLeavingWater(EntityPlayer player) {
        UUID playerId = player.getUniqueID();

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.YELLOW + "正在进行系统自检..."
        ), true);

        PotionEffect current = player.getActivePotionEffect(ModPotions.MALFUNCTION);
        if (current != null) {
            int remaining = Math.max(100, current.getDuration());
            PotionEffect extended = new PotionEffect(
                    ModPotions.MALFUNCTION,
                    remaining,
                    Math.max(0, current.getAmplifier() - 1),
                    false,
                    true
            );
            player.addPotionEffect(extended);
        }

        malfunctionLevel.remove(playerId);
        lastEffectTime.remove(playerId);
    }

    private static void applyMalfunctionEffect(EntityPlayer player, int level) {
        player.addPotionEffect(new PotionEffect(
                ModPotions.MALFUNCTION, MALFUNCTION_DURATION, level, false, true
        ));
    }

    // ===================== 水下增益 =====================

    private static void applyUnderwaterBenefits(EntityPlayer player, int level) {
        switch (level) {
            case 1:
                // 基础：仅防故障，无额外增益
                break;
            case 2:
                // 水下呼吸
                player.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 100, 0, true, false));
                break;
            case 3:
                player.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 100, 0, true, false));
                player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 220, 0, true, false));
                // 只有真正浸没时给挖掘/速度
                if (isPlayerSubmerged(player)) {
                    player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 100, 1, true, false));
                    player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 100, 1, true, false));
                }
                break;
        }
    }

    private static void showWaterproofStatus(EntityPlayer player, int level) {
        String status; TextFormatting color;
        switch (level) {
            case 1: status = "防水涂层正常工作"; color = TextFormatting.AQUA; break;
            case 2: status = "高级防水系统已激活"; color = TextFormatting.BLUE; break;
            case 3: status = "深海适应模式已启动"; color = TextFormatting.DARK_AQUA; break;
            default: return;
        }
        player.sendStatusMessage(new TextComponentString(color + "💧 " + status), true);
        player.world.playSound(null, player.getPosition(),
                SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 0.5F, 1.0F);
    }

    private static void sendWaterDamageWarning(EntityPlayer player, int level) {
        String msg; TextFormatting color;
        switch (level) {
            case 0: msg = "⚡ 检测到水体，系统开始出现故障"; color = TextFormatting.YELLOW; break;
            case 1: msg = "⚠ 水体侵入严重，多个子系统故障！"; color = TextFormatting.GOLD; break;
            default: msg = "☠ 核心严重进水！立即离开水体！"; color = TextFormatting.DARK_RED; break;
        }
        player.sendStatusMessage(new TextComponentString(color + msg), true);
        player.world.playSound(null, player.getPosition(),
                SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT, SoundCategory.PLAYERS, 1.0F, 0.5F);
    }

    // ===================== GUI/信息 =====================

    public static ItemStack getUpgradeMaterial(int targetLevel) {
        switch (targetLevel) {
            case 1: return new ItemStack(Items.SLIME_BALL, 4);
            case 2: return new ItemStack(Items.PRISMARINE_SHARD, 8);
            case 3: return new ItemStack(Blocks.PRISMARINE, 4);
            default: return ItemStack.EMPTY;
        }
    }

    public static String getUpgradeDescription(ItemStack coreStack) {
        int level = getWaterproofLevel(coreStack);
        boolean disabled = isWaterproofDisabled(coreStack);
        int effectiveLevel = getEffectiveWaterproofLevel(coreStack);

        StringBuilder sb = new StringBuilder();
        if (level == 0) {
            sb.append(TextFormatting.GRAY).append("未安装 - 接触水体会导致故障");
        } else {
            sb.append(TextFormatting.WHITE).append("等级 ").append(level).append("/").append(MAX_LEVEL);
            if (disabled) sb.append(TextFormatting.RED).append(" [已禁用]");
            else if (effectiveLevel == 0) sb.append(TextFormatting.YELLOW).append(" [暂停]");
            else sb.append(TextFormatting.GREEN).append(" [激活]");
            sb.append("\n");
            switch (level) {
                case 1: sb.append(TextFormatting.AQUA).append("基础防水 - 防止水体损害"); break;
                case 2: sb.append(TextFormatting.BLUE).append("高级防水 - 水下呼吸"); break;
                case 3: sb.append(TextFormatting.DARK_AQUA).append("深海适应 - 完整水下能力"); break;
            }
        }
        return sb.toString();
    }

    public static String getUpgradeDescription(int level) {
        switch (level) {
            case 0: return TextFormatting.GRAY + "未安装 - 接触水体会导致故障";
            case 1: return TextFormatting.AQUA + "基础防水 - 防止水体损害";
            case 2: return TextFormatting.BLUE + "高级防水 - 水下呼吸";
            case 3: return TextFormatting.DARK_AQUA + "深海适应 - 完整水下能力";
            default: return "";
        }
    }

    public static void cleanupPlayer(EntityPlayer player) {
        UUID id = player.getUniqueID();
        wasInWater.remove(id);
        lastWarningTime.remove(id);
        malfunctionLevel.remove(id);
        lastEffectTime.remove(id);
        lastDebugTime.remove(id);
    }
}
