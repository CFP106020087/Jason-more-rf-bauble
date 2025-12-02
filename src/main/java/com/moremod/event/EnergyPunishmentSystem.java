package com.moremod.event;

import com.moremod.combat.TrueDamageHelper;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.upgrades.ItemUpgradeComponent;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import com.moremod.util.UpgradeKeys;
import com.moremod.system.ascension.ShambhalaHandler;
import com.moremod.system.ascension.BrokenGodHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = "moremod")
public class EnergyPunishmentSystem {

    public static final EnergyPunishmentSystem INSTANCE = new EnergyPunishmentSystem();

    // ===== 配置参数 =====
    private static final int DOT_DAMAGE_PER_TICK  = 1;
    private static final int DEGRADE_MODULE_COUNT = 2;

    private static final long TICK_1S  = 20;
    private static final long TICK_5S  = 20 * 5;
    private static final long TICK_10S = 20 * 10;
    private static final long TICK_15S = 20 * 15;
    private static final long TICK_20S = 20 * 20;

    // 发电模块列表
    private static final Set<String> GENERATOR_MODULES = new HashSet<>(Arrays.asList(
            "SOLAR_GENERATOR","KINETIC_GENERATOR","THERMAL_GENERATOR",
            "VOID_ENERGY","COMBAT_CHARGER",
            "solar_generator","kinetic_generator","thermal_generator",
            "void_energy","combat_charger"
    ));

    // NBT键
    private static final String K_LAST_DOT           = "Punish_LastDot";
    private static final String K_LAST_DEGRADE       = "Punish_LastDegrade";
    private static final String K_LAST_DURABILITY    = "Punish_LastDur";
    private static final String K_CRITICAL_SINCE     = "Punish_CriticalSince";
    private static final String K_SELF_DESTRUCT_DONE = "Punish_SelfDestructDone";
    private static final String K_WARNING_10S        = "Punish_Warning10s";
    private static final String K_WARNING_5S         = "Punish_Warning5s";
    private static final String K_SPECIAL_DEATH_FLAG = "MechanicalCoreDeath";

    // 修复系统键
    private static final String K_ORIGINAL_MAX = "OriginalMax_";
    private static final String K_OWNED_MAX = "OwnedMax_";
    private static final String K_DAMAGE_COUNT = "DamageCount_";
    private static final String K_WAS_PUNISHED = "WasPunished_";

    public static class EnergyDepletionDamage extends DamageSource {
        public EnergyDepletionDamage() {
            super("energy_depletion");
            this.setDamageBypassesArmor();
        }
        @Override
        public ITextComponent getDeathMessage(EntityLivingBase entity) {
            String playerName = entity.getDisplayName().getUnformattedText();
            return new TextComponentString(TextFormatting.RED + playerName + " 因机械核心能量耗尽而死亡");
        }
    }

    public static class MechanicalCoreDeathSource extends DamageSource {
        private final String deathMessage;
        public MechanicalCoreDeathSource(String name, String deathMessage) {
            super(name);
            this.deathMessage = deathMessage;
            this.setDamageBypassesArmor();
            this.setDamageIsAbsolute();
        }
        @Override
        public ITextComponent getDeathMessage(EntityLivingBase entity) {
            String playerName = entity.getDisplayName().getUnformattedText();
            return new TextComponentString(TextFormatting.DARK_RED + playerName + deathMessage);
        }
    }

    private static final DamageSource ENERGY_DEPLETION = new EnergyDepletionDamage();
    private static final DamageSource GEAR_STOP = new MechanicalCoreDeathSource(
            "mechanical_core_selfdestruct", " 不该让齿轮停下...."
    );

    public static void tick(ItemStack core, EntityPlayer player, EnergyDepletionManager.EnergyStatus status) {
        if (core == null || core.isEmpty() || !ItemMechanicalCore.isMechanicalCore(core)) return;
        if (player == null || player.world == null || player.world.isRemote) return;

        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);
        long time = player.world.getTotalWorldTime();

        if (status == EnergyDepletionManager.EnergyStatus.CRITICAL) {
            if (!nbt.hasKey(K_CRITICAL_SINCE)) {
                nbt.setLong(K_CRITICAL_SINCE, time);
                nbt.removeTag(K_WARNING_10S);
                nbt.removeTag(K_WARNING_5S);
                nbt.removeTag(K_SELF_DESTRUCT_DONE);
            }
            handleSelfDestructCountdown(core, player, nbt, time);
        } else {
            if (nbt.hasKey(K_CRITICAL_SINCE)) {
                nbt.removeTag(K_CRITICAL_SINCE);
                nbt.removeTag(K_WARNING_10S);
                nbt.removeTag(K_WARNING_5S);
                nbt.removeTag(K_SELF_DESTRUCT_DONE);
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 自毁序列已取消"), true);
            }
        }

        applyPunishments(core, player, status, nbt, time);
    }

    private static void handleSelfDestructCountdown(ItemStack core, EntityPlayer player, NBTTagCompound nbt, long time) {
        long criticalStart = nbt.getLong(K_CRITICAL_SINCE);
        long elapsed = time - criticalStart;

        if (elapsed >= TICK_10S && !nbt.getBoolean(K_WARNING_10S)) {
            nbt.setBoolean(K_WARNING_10S, true);
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "⚠⚠⚠ 核心自毁倒计时：10秒 ⚠⚠⚠"));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "立即补充能量以取消自毁序列！"));
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_NOTE_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
        }

        if (elapsed >= TICK_15S && !nbt.getBoolean(K_WARNING_5S)) {
            nbt.setBoolean(K_WARNING_5S, true);
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "☠☠☠ 核心自毁倒计时：5秒！！！ ☠☠☠"));
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1.0f, 2.0f);

            for (int i = 0; i < 10; i++) {
                player.world.spawnParticle(EnumParticleTypes.REDSTONE,
                        player.posX + (Math.random() - 0.5) * 2,
                        player.posY + 1,
                        player.posZ + (Math.random() - 0.5) * 2,
                        1.0, 0.0, 0.0);
            }
        }

        if (elapsed >= TICK_20S - 60) {
            long secondsLeft = (TICK_20S - elapsed) / 20;
            if (secondsLeft <= 3 && secondsLeft > 0 && elapsed % 20 == 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "☠ " + secondsLeft + " ☠"
                ), true);
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 1.0f, 0.5f);
            }
        }

        if (elapsed >= TICK_20S && !nbt.getBoolean(K_SELF_DESTRUCT_DONE)) {
            selfDestructSafe(core, player);
            nbt.setBoolean(K_SELF_DESTRUCT_DONE, true);
        }
    }

    private static void applyPunishments(ItemStack core, EntityPlayer player,
                                         EnergyDepletionManager.EnergyStatus status,
                                         NBTTagCompound nbt, long time) {

        if (status == EnergyDepletionManager.EnergyStatus.CRITICAL) {
            if (checkCooldown(nbt, K_LAST_DOT, time, TICK_1S)) {
                player.attackEntityFrom(ENERGY_DEPLETION, DOT_DAMAGE_PER_TICK);
                nbt.setLong(K_LAST_DOT, time);
                player.world.spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR,
                        player.posX, player.posY + player.height / 2, player.posZ,
                        0.0, 0.0, 0.0);
            }
        }

        if (status == EnergyDepletionManager.EnergyStatus.CRITICAL) {
            if (checkCooldown(nbt, K_LAST_DURABILITY, time, TICK_10S)) {
                damageDurability(player);
                nbt.setLong(K_LAST_DURABILITY, time);
            }
        }

        if (status == EnergyDepletionManager.EnergyStatus.CRITICAL) {
            if (checkCooldown(nbt, K_LAST_DEGRADE, time, TICK_10S)) {
                degradeRandomModules(core, player);
                nbt.setLong(K_LAST_DEGRADE, time);
            }
        }
    }

    /**
     * ✅ 完整修复：降级模块 - 支持修复系统
     * ✅ 香巴拉/破碎之神玩家免疫模块降级
     */
    public static void degradeRandomModules(ItemStack core, EntityPlayer player) {
        // ✅ 香巴拉/破碎之神玩家免疫模块降级
        if (isAscensionProtected(player)) {
            return;
        }

        List<String> installed = getInstalledUpgrades(core);
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);

        installed = installed.stream()
                .filter(id -> !isGeneratorModule(id))
                .filter(id -> !ItemMechanicalCore.isUpgradePaused(core, id))
                .filter(id -> getOwnedMax(core, id) > 0)
                .filter(id -> getCurrentLevel(core, id) > 0)
                .collect(Collectors.toList());

        if (installed.isEmpty()) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "没有可降级的活跃模块"
            ), true);
            return;
        }

        Collections.shuffle(installed);
        int degradeCount = Math.min(DEGRADE_MODULE_COUNT, installed.size());

        for (int i = 0; i < degradeCount; i++) {
            String moduleId = installed.get(i);
            String upperId = moduleId.toUpperCase();
            String lowerId = moduleId.toLowerCase();

            // 获取当前的拥有等级
            int currentOwnedMax = getOwnedMax(core, moduleId);

            // 第一次损坏时记录原始等级
            if (!nbt.hasKey(K_ORIGINAL_MAX + upperId)) {
                nbt.setInteger(K_ORIGINAL_MAX + upperId, currentOwnedMax);
                nbt.setInteger(K_ORIGINAL_MAX + moduleId, currentOwnedMax);
                nbt.setInteger(K_ORIGINAL_MAX + lowerId, currentOwnedMax);
            }

            // 关键修复：移到 if 外面，每次降级都写入 WasPunished
            nbt.setBoolean(K_WAS_PUNISHED + upperId, true);
            nbt.setBoolean(K_WAS_PUNISHED + moduleId, true);
            nbt.setBoolean(K_WAS_PUNISHED + lowerId, true);

            // 降级
            int newOwnedMax = Math.max(0, currentOwnedMax - 1);
            setOwnedMaxSafe(core, moduleId, newOwnedMax);

            // 增加损坏计数
            int damageCount = nbt.getInteger(K_DAMAGE_COUNT + upperId);
            nbt.setInteger(K_DAMAGE_COUNT + upperId, damageCount + 1);
            nbt.setInteger(K_DAMAGE_COUNT + moduleId, damageCount + 1);
            nbt.setInteger(K_DAMAGE_COUNT + lowerId, damageCount + 1);

            // 累计总损坏次数（用于修复成本计算）
            int totalDamage = nbt.getInteger("TotalDamageCount_" + upperId);
            nbt.setInteger("TotalDamageCount_" + upperId, totalDamage + 1);
            nbt.setInteger("TotalDamageCount_" + moduleId, totalDamage + 1);
            nbt.setInteger("TotalDamageCount_" + lowerId, totalDamage + 1);

            // 调整当前等级
            int currentLevel = getCurrentLevel(core, moduleId);
            if (currentLevel > newOwnedMax) {
                setLevel(core, moduleId, newOwnedMax);
            }

            // 获取原始最大等级用于显示
            int originalMax = Math.max(
                    nbt.getInteger(K_ORIGINAL_MAX + upperId),
                    Math.max(
                            nbt.getInteger(K_ORIGINAL_MAX + moduleId),
                            nbt.getInteger(K_ORIGINAL_MAX + lowerId)
                    )
            );

            // 通知
            if (newOwnedMax <= 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "⚠ 模块损坏: " + getDisplayName(moduleId) +
                                TextFormatting.YELLOW + " [0/" + originalMax + "] (可修复)"
                ), true);
            } else {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 模块降级: " + getDisplayName(moduleId) +
                                " [" + newOwnedMax + "/" + originalMax + "]" +
                                TextFormatting.YELLOW + " (可修复)"
                ), true);
            }
        }

        if (degradeCount > 0) {
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.6f, 0.8f);
        }
    }

    /**
     * ✅ 获取历史最高等级（缺失时自动补写）
     */
    public static int getItemMaxLevel(ItemStack core, String moduleId) {
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return 0;

        String upperId = moduleId.toUpperCase();
        String lowerId = moduleId.toLowerCase();
        String[] variants = {upperId, moduleId, lowerId};

        // 第1层：尝试读取 OriginalMax
        int originalMax = 0;
        for (String variant : variants) {
            int val = nbt.getInteger(K_ORIGINAL_MAX + variant);
            if (val > 0) {
                originalMax = Math.max(originalMax, val);
            }
        }

        if (originalMax > 0) {
            return originalMax;
        }

        // 第2层：OriginalMax 不存在，检查 OwnedMax
        int ownedMax = getOwnedMax(core, moduleId);
        if (ownedMax == 0) {
            return 0;
        }

        // 使用 OwnedMax 作为 fallback，并回填 OriginalMax
        nbt.setInteger(K_ORIGINAL_MAX + upperId, ownedMax);
        nbt.setInteger(K_ORIGINAL_MAX + moduleId, ownedMax);
        nbt.setInteger(K_ORIGINAL_MAX + lowerId, ownedMax);
        nbt.setBoolean("DataCorruption_" + upperId, true);

        return ownedMax;
    }

    /** 获取损坏次数 */
    public static int getDamageCount(ItemStack core, String moduleId) {
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return 0;

        String[] variants = {moduleId, moduleId.toUpperCase(), moduleId.toLowerCase()};
        int count = 0;

        for (String variant : variants) {
            count = Math.max(count, nbt.getInteger(K_DAMAGE_COUNT + variant));
        }

        return count;
    }

    /** 修复模块（GUI 调用）- 移除经验成本 */
    public static boolean repairModule(ItemStack core, String moduleId, int targetLevel,
                                       EntityPlayer player, boolean consumeResources) {
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);
        String upperId = moduleId.toUpperCase();

        int currentOwnedMax = getOwnedMax(core, moduleId);
        int originalMax = getItemMaxLevel(core, moduleId);

        if (targetLevel > originalMax) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "无法修复到超过原始等级"));
            return false;
        }

        if (currentOwnedMax >= targetLevel) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "已达到或超过目标等级"));
            return false;
        }

        if (consumeResources) {
            int damageCount = getDamageCount(core, moduleId);
            int levelDiff = targetLevel - currentOwnedMax;

            // 计算能量成本（移除经验成本）
            int energyCost = (levelDiff * 10000) + (damageCount * 5000);

            // 检查能量资源
            IEnergyStorage energy = ItemMechanicalCore.getEnergyStorage(core);
            if (energy == null || energy.getEnergyStored() < energyCost) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "能量不足！需要 " + energyCost + " RF"));
                return false;
            }

            // 消耗能量资源
            energy.extractEnergy(energyCost, false);
        }

        // 执行修复
        setOwnedMaxSafe(core, moduleId, targetLevel);

        // 减少损坏计数
        int damageCount = nbt.getInteger(K_DAMAGE_COUNT + upperId);
        if (damageCount > 0) {
            nbt.setInteger(K_DAMAGE_COUNT + upperId, Math.max(0, damageCount - 1));
            nbt.setInteger(K_DAMAGE_COUNT + moduleId, Math.max(0, damageCount - 1));
        }

        // 如果完全修复，清除标记（保留 OriginalMax）
        if (targetLevel >= originalMax) {
            nbt.removeTag(K_WAS_PUNISHED + upperId);
            nbt.removeTag(K_WAS_PUNISHED + moduleId);
        }

        // 恢复等级
        setLevel(core, moduleId, targetLevel);

        return true;
    }

    private static void selfDestructSafe(ItemStack core, EntityPlayer player) {
        // ✅ 香巴拉/破碎之神玩家免疫自毁
        if (isAscensionProtected(player)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "✦ 飞升力量保护了机械核心免于自毁"));
            return;
        }

        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "☠☠☠ 机械核心自毁序列启动 ☠☠☠"));

        // 特效
        player.world.createExplosion(player, player.posX, player.posY, player.posZ, 0.0f, false);
        for (int i = 0; i < 20; i++) {
            player.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                    player.posX + (Math.random() - 0.5) * 2,
                    player.posY + Math.random() * 2,
                    player.posZ + (Math.random() - 0.5) * 2,
                    0, 0, 0);
        }

        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);
        List<String> damaged = new ArrayList<>();

        // 处理所有模块
        for (String id : getInstalledUpgrades(core)) {
            if (isGeneratorModule(id)) {
                continue; // 发电模块保留
            }
            if (ItemMechanicalCore.isUpgradePaused(core, id)) {
                continue; // 暂停的模块保留
            }

            int currentMax = getOwnedMax(core, id);
            if (currentMax > 0) {
                String upperId = id.toUpperCase();
                String lowerId = id.toLowerCase();

                if (!nbt.hasKey(K_ORIGINAL_MAX + upperId)) {
                    nbt.setInteger(K_ORIGINAL_MAX + upperId, currentMax);
                    nbt.setInteger(K_ORIGINAL_MAX + id, currentMax);
                    nbt.setInteger(K_ORIGINAL_MAX + lowerId, currentMax);
                }

                // 标记为惩罚过
                nbt.setBoolean(K_WAS_PUNISHED + upperId, true);
                nbt.setBoolean(K_WAS_PUNISHED + id, true);
                nbt.setBoolean(K_WAS_PUNISHED + lowerId, true);

                // 增加损坏计数
                int damageCount = nbt.getInteger(K_DAMAGE_COUNT + upperId);
                nbt.setInteger(K_DAMAGE_COUNT + upperId, damageCount + currentMax);
                nbt.setInteger(K_DAMAGE_COUNT + id, damageCount + currentMax);
                nbt.setInteger(K_DAMAGE_COUNT + lowerId, damageCount + currentMax);

                // 累计总损坏次数
                int totalDamage = nbt.getInteger("TotalDamageCount_" + upperId);
                nbt.setInteger("TotalDamageCount_" + upperId, totalDamage + currentMax);
                nbt.setInteger("TotalDamageCount_" + id, totalDamage + currentMax);
                nbt.setInteger("TotalDamageCount_" + lowerId, totalDamage + currentMax);

                // 降级到0
                setLevel(core, id, 0);
                setOwnedMaxSafe(core, id, 0);

                damaged.add(getDisplayName(id));
            }
        }

        // 给予应急能量
        try {
            IEnergyStorage es = ItemMechanicalCore.getEnergyStorage(core);
            if (es != null) {
                int emergency = es.getMaxEnergyStored() / 10; // 10%应急能量
                es.receiveEnergy(emergency, false);
            }
        } catch (Exception ignored) {}

        // 杀死玩家
        killPlayerSafely(player);

        // 通知
        if (!damaged.isEmpty()) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "模块严重损坏: " + String.join(", ", damaged)));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "损坏的模块需要花费大量能量修复"));
        }

        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "发电模块和暂停模块未受影响"));
    }

    // ===== 辅助方法 =====

    private static void setOwnedMaxSafe(ItemStack core, String id, int level) {
        if (core == null || core.isEmpty()) return;
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);

        String[] variants = {id, id.toUpperCase(), id.toLowerCase()};
        for (String variant : variants) {
            nbt.setInteger(K_OWNED_MAX + variant, level);
        }
    }

    private static int getOwnedMax(ItemStack core, String id) {
        if (core == null || core.isEmpty()) return 0;
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return 0;

        String[] variants = {id, id.toUpperCase(), id.toLowerCase()};
        int max = 0;

        for (String variant : variants) {
            max = Math.max(max, nbt.getInteger(K_OWNED_MAX + variant));
        }

        if (max <= 0) {
            max = getCurrentLevel(core, id);
            if (max > 0) {
                setOwnedMaxSafe(core, id, max);
            }
        }

        return max;
    }

    private static void setLevel(ItemStack core, String id, int level) {
        if (core == null || core.isEmpty()) return;

        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);
        String[] variants = {id, id.toUpperCase(), id.toLowerCase()};
        for (String var : variants) {
            nbt.setInteger("upgrade_" + var, level);
        }

        try { ItemMechanicalCore.setUpgradeLevel(core, id, level); } catch (Exception ignored) {}
        try { ItemMechanicalCoreExtended.setUpgradeLevel(core, id, level); } catch (Exception ignored) {}
    }

    private static int getCurrentLevel(ItemStack core, String id) {
        if (core == null || core.isEmpty()) return 0;
        int level = 0;

        NBTTagCompound nbt = core.getTagCompound();
        if (nbt != null) {
            String[] variants = {id, id.toUpperCase(), id.toLowerCase()};
            for (String variant : variants) {
                level = Math.max(level, nbt.getInteger("upgrade_" + variant));
            }
        }

        try { level = Math.max(level, ItemMechanicalCore.getUpgradeLevel(core, id)); } catch (Exception ignored) {}
        try { level = Math.max(level, ItemMechanicalCoreExtended.getUpgradeLevel(core, id)); } catch (Exception ignored) {}

        return level;
    }

    private static boolean checkCooldown(NBTTagCompound nbt, String key, long now, long cooldown) {
        return !nbt.hasKey(key) || now - nbt.getLong(key) >= cooldown;
    }

    /**
     * 检查玩家是否为香巴拉或破碎之神（飞升状态）
     * 飞升玩家免疫模块降级和自毁
     */
    private static boolean isAscensionProtected(EntityPlayer player) {
        try {
            if (ShambhalaHandler.isShambhala(player)) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            if (BrokenGodHandler.isBrokenGod(player)) {
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }

    private static boolean isGeneratorModule(String id) {
        if (id == null) return false;
        String upper = id.toUpperCase(Locale.ROOT);
        return GENERATOR_MODULES.contains(id) ||
                GENERATOR_MODULES.contains(upper) ||
                upper.contains("GENERATOR") ||
                upper.contains("CHARGER") ||
                upper.contains("SOLAR") ||
                upper.contains("KINETIC") ||
                upper.contains("THERMAL") ||
                upper.contains("VOID_ENERGY");
    }

    private static List<String> getInstalledUpgrades(ItemStack core) {
        if (core == null || core.isEmpty()) return new ArrayList<>();
        Set<String> upgrades = new HashSet<>();

        NBTTagCompound nbt = core.getTagCompound();
        if (nbt != null) {
            for (String key : nbt.getKeySet()) {
                if (key.startsWith("upgrade_")) {
                    String id = key.substring(8);
                    if (nbt.getInteger(key) > 0) {
                        upgrades.add(id);
                    }
                }
            }
        }

        try {
            for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
                if (ItemMechanicalCore.getUpgradeLevel(core, type) > 0) {
                    upgrades.add(type.getKey());
                }
            }
        } catch (Exception ignored) {}

        try {
            Map<String, ItemMechanicalCoreExtended.UpgradeInfo> all = ItemMechanicalCoreExtended.getAllUpgrades();
            for (String id : all.keySet()) {
                if (ItemMechanicalCoreExtended.getUpgradeLevel(core, id) > 0) {
                    upgrades.add(id);
                }
            }
        } catch (Exception ignored) {}

        return new ArrayList<>(upgrades);
    }

    private static String getDisplayName(String id) {
        try {
            ItemMechanicalCoreExtended.UpgradeInfo info = ItemMechanicalCoreExtended.getUpgradeInfo(id);
            if (info != null && info.displayName != null) {
                return info.displayName;
            }
        } catch (Exception ignored) {}

        try {
            for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
                if (type.getKey().equalsIgnoreCase(id)) {
                    return type.getDisplayName();
                }
            }
        } catch (Exception ignored) {}

        return id.replace("_", " ").toLowerCase();
    }

    private static void killPlayerSafely(EntityPlayer player) {
        if (player.capabilities.isCreativeMode) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "创造模式下免疫自毁伤害"));
            return;
        }

        try {
            player.getEntityData().setBoolean(K_SPECIAL_DEATH_FLAG, true);
            // 使用包装的死亡链，保留 GEAR_STOP 死亡消息，同时正确触发掉落物
            TrueDamageHelper.triggerVanillaDeathChain(player, GEAR_STOP);
        } catch (Exception e) {
            try {
                float damage = player.getMaxHealth() * 100;
                player.attackEntityFrom(DamageSource.OUT_OF_WORLD, damage);
            } catch (Exception ignored) {}
        } finally {
            player.getEntityData().removeTag(K_SPECIAL_DEATH_FLAG);
        }
    }

    private static void damageDurability(EntityPlayer player) {
        int totalDamage = 0;

        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (slot.getSlotType() == EntityEquipmentSlot.Type.ARMOR) {
                ItemStack armor = player.getItemStackFromSlot(slot);
                if (!armor.isEmpty() && armor.isItemStackDamageable()) {
                    armor.damageItem(10, player);
                    totalDamage += 10;
                }
            }
        }

        ItemStack mainHand = player.getHeldItemMainhand();
        if (!mainHand.isEmpty() && mainHand.isItemStackDamageable() && !isProtectedItem(mainHand)) {
            mainHand.damageItem(5, player);
            totalDamage += 5;
        }

        if (totalDamage > 0) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚠ 装备耐久度降低"
            ), true);
        }
    }

    private static boolean isProtectedItem(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (ItemMechanicalCore.isMechanicalCore(stack)) return true;
        if (stack.getItem() instanceof ItemUpgradeComponent) return true;

        if (stack.getItem().getRegistryName() != null) {
            String name = stack.getItem().getRegistryName().toString().toLowerCase();
            return name.contains("core") ||
                    name.contains("battery") ||
                    name.contains("upgrade") ||
                    name.contains("module");
        }
        return false;
    }

    private static ItemStack findEquippedCore(EntityPlayer player) {
        try {
            IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty() && ItemMechanicalCore.isMechanicalCore(stack)) {
                        return stack;
                    }
                }
            }
        } catch (Exception ignored) {}

        return ItemStack.EMPTY;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeathPre(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (player.getEntityData().getBoolean(K_SPECIAL_DEATH_FLAG)) {
            try {
                if (event.getSource() != null) {
                    event.getSource().setDamageIsAbsolute();
                    event.getSource().setDamageBypassesArmor();
                }
            } catch (Exception ignored) {}
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        ItemStack core = findEquippedCore(player);
        if (!ItemMechanicalCore.isMechanicalCore(core)) return;

        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);
        nbt.removeTag(K_SELF_DESTRUCT_DONE);
        nbt.removeTag(K_CRITICAL_SINCE);
        nbt.removeTag(K_WARNING_10S);
        nbt.removeTag(K_WARNING_5S);
        nbt.removeTag(K_LAST_DOT);
        nbt.removeTag(K_LAST_DEGRADE);
        nbt.removeTag(K_LAST_DURABILITY);
        nbt.removeTag(K_SPECIAL_DEATH_FLAG);
    }
}
