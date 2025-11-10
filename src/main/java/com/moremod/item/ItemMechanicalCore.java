// ItemMechanicalCore.java - 完整版本（集成新电池系统）
// Part 1: 导入和主要方法（第1-600行）

package com.moremod.item;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;

import com.moremod.config.EnergyBalanceConfig;
import com.moremod.config.EquipmentTimeConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.event.EquipmentTimeTracker;
import com.moremod.upgrades.UpgradeEffectManager;
import com.moremod.upgrades.auxiliary.AuxiliaryUpgradeManager;
import com.moremod.upgrades.combat.CombatUpgradeManager;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import com.moremod.upgrades.energy.EnergyUpgradeManager;

// ✨ 新增：导入新电池系统
import com.moremod.item.battery.ItemBatteryBase;
import com.moremod.item.battery.ItemBatteryBasic;
import com.moremod.item.battery.ItemBatteryAdvanced;
import com.moremod.item.battery.ItemBatteryElite;
import com.moremod.item.battery.ItemBatteryUltimate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 机械核心 - 完整整合版（含惩罚/冷却/代价解除/Enigmatic冲突检测/新电池系统）
 */
public class ItemMechanicalCore extends Item implements IBauble {

    // ===== 调试与递归保护 =====
    private static final boolean DEBUG_MODE = false;
    private static final ThreadLocal<Boolean> isCalculatingEnergy = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> isCheckingUpgrade  = ThreadLocal.withInitial(() -> false);

    // ===== Enigmatic 冲突配置 =====
    private static final boolean BLOCK_ALL_ENIGMATIC = true;
    private static final boolean VERBOSE_ENIGMATIC_DETECTION = false;

    // ===== 电池缓存 =====
    private static class BatteryCache {
        boolean hasBattery;
        int batteryTier;
        long lastCheck;
        boolean isValid(long currentTime) { return currentTime - lastCheck < 20; }
    }
    private static final Map<UUID, BatteryCache> batteryCache = new ConcurrentHashMap<>();

    // ===== 扩展升级ID（统一管理） =====
    private static final String[] EXTENDED_UPGRADE_IDS = {
            "YELLOW_SHIELD","HEALTH_REGEN","HUNGER_THIRST","THORNS","FIRE_EXTINGUISH","MOVEMENT_SPEED",
            "STEALTH","ORE_VISION","EXP_AMPLIFIER","DAMAGE_BOOST","ATTACK_SPEED","RANGE_EXTENSION",
            "PURSUIT","KINETIC_GENERATOR","SOLAR_GENERATOR","VOID_ENERGY","COMBAT_CHARGER","WATERPROOF_MODULE",
            "POISON_IMMUNITY","NIGHT_VISION","WATER_BREATHING","CRITICAL_STRIKE","ITEM_MAGNET"
    };

    // ===== 升级类型 =====
    public enum UpgradeType {
        ENERGY_CAPACITY("energy_capacity", "能量容量", TextFormatting.BLUE),
        ENERGY_EFFICIENCY("energy_efficiency", "能量效率", TextFormatting.GREEN),
        ARMOR_ENHANCEMENT("armor_enhancement", "护甲强化", TextFormatting.YELLOW),
        SPEED_BOOST("speed_boost", "速度提升", TextFormatting.AQUA),
        REGENERATION("regeneration", "生命恢复", TextFormatting.RED),
        FLIGHT_MODULE("flight_module", "飞行模块", TextFormatting.LIGHT_PURPLE),
        SHIELD_GENERATOR("shield_generator", "护盾发生器", TextFormatting.GOLD),
        TEMPERATURE_CONTROL("temperature_control", "温度调节", TextFormatting.DARK_AQUA);

        private final String key;
        private final String displayName;
        private final TextFormatting color;

        UpgradeType(String key, String displayName, TextFormatting color) {
            this.key = key; this.displayName = displayName; this.color = color;
        }
        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }
        public TextFormatting getColor() { return color; }
    }

    // ===== 速度模式 =====
    public enum SpeedMode {
        NORMAL("标准", 1.0),
        FAST("快速", 1.5),
        ULTRA("极速", 2.0);

        private final String name;
        private final double multiplier;
        SpeedMode(String name, double multiplier) { this.name = name; this.multiplier = multiplier; }
        public String getName() { return name; }
        public double getMultiplier() { return multiplier; }
    }

    // ===== 模块统计 =====
    private static class ModuleStats {
        final int typesInstalled;
        final int totalLevels;
        ModuleStats(int typesInstalled, int totalLevels) { this.typesInstalled = typesInstalled; this.totalLevels = totalLevels; }
    }

    public ItemMechanicalCore() {
        super();
        setTranslationKey("mechanical_core");
        setRegistryName("mechanical_core");
        try { setCreativeTab(moremodCreativeTab.moremod_TAB); } catch (Exception ignored) {}
        setMaxStackSize(1);
    }

    public static void registerEnergyGenerationEvents() {
        MinecraftForge.EVENT_BUS.register(EnergyUpgradeManager.class);
        MinecraftForge.EVENT_BUS.register(EnergyUpgradeManager.KineticGeneratorSystem.class);
        MinecraftForge.EVENT_BUS.register(EnergyUpgradeManager.CombatChargerSystem.class);
        MinecraftForge.EVENT_BUS.register(new ConflictChecker());
    }

    // =====================================================================
    // ✨ 新电池系统集成 - 更新的方法
    // =====================================================================

    /**
     * ✨ 更新：识别新电池系统
     */
    private boolean isBatteryItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        // 新电池系统（优先检测）
        if (stack.getItem() instanceof ItemBatteryBase) return true;

        // 旧电池系统（兼容）
        if (stack.getItem() instanceof ItemBatteryBauble ||
                stack.getItem() instanceof ItemCreativeBatteryBauble) return true;

        // 通用电池检测（注册名识别）
        if (stack.getItem().getRegistryName() != null) {
            String rn = stack.getItem().getRegistryName().toString().toLowerCase();

            // 排除非电池物品
            if (rn.contains("tool") || rn.contains("sword") ||
                    rn.contains("armor") || rn.contains("jetpack") ||
                    rn.contains("drill") || rn.contains("core"))
                return false;

            // 识别电池物品
            if (rn.contains("battery") || rn.contains("cell") ||
                    rn.contains("capacitor")) {
                IEnergyStorage es = stack.getCapability(CapabilityEnergy.ENERGY, null);
                return es != null && es.getMaxEnergyStored() >= 50_000;
            }
        }
        return false;
    }

    /**
     * ✨ 更新：准确识别新电池等级
     */
    private int getBatteryTier(ItemStack stack) {
        if (!isBatteryItem(stack)) return 0;

        // 创造电池：最高等级
        if (stack.getItem() instanceof ItemCreativeBatteryBauble) return 5;

        // 新电池系统：直接通过类型判断（更准确）
        if (stack.getItem() instanceof ItemBatteryUltimate) return 4;
        if (stack.getItem() instanceof ItemBatteryElite) return 3;
        if (stack.getItem() instanceof ItemBatteryAdvanced) return 2;
        if (stack.getItem() instanceof ItemBatteryBasic) return 1;

        // 通用方式：通过容量判断（兼容其他电池）
        IEnergyStorage es = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (es != null) {
            int cap = es.getMaxEnergyStored();

            // 新电池容量等级
            if (cap >= 50_000_000) return 4;  // 终极：50M
            if (cap >= 10_000_000) return 3;  // 精英：10M
            if (cap >= 1_000_000)  return 2;  // 高级：1M
            if (cap >= 100_000)    return 1;  // 基础：100K

            // 旧电池配置（兼容）
            if (cap >= EnergyBalanceConfig.BatterySystem.QUANTUM_BATTERY_CAPACITY) return 3;
            if (cap >= EnergyBalanceConfig.BatterySystem.ADVANCED_BATTERY_CAPACITY) return 2;
            if (cap >= EnergyBalanceConfig.BatterySystem.BASIC_BATTERY_CAPACITY) return 1;
        }
        return 0;
    }

    /**
     * ✨ 更新：根据新电池类型返回传输速率
     */
    private int getTransferRateForBattery(ItemStack batteryStack) {
        // 创造电池：超高速传输
        if (batteryStack.getItem() instanceof ItemCreativeBatteryBauble) {
            return EnergyBalanceConfig.BatterySystem.QUANTUM_BATTERY_OUTPUT * 2;
        }

        // 新电池系统：根据类型返回传输速率
        if (batteryStack.getItem() instanceof ItemBatteryUltimate) {
            return 100_000 / 20;  // 100K RF/t -> 5K RF/tick
        }
        if (batteryStack.getItem() instanceof ItemBatteryElite) {
            return 20_000 / 20;   // 20K RF/t -> 1K RF/tick
        }
        if (batteryStack.getItem() instanceof ItemBatteryAdvanced) {
            return 5_000 / 20;    // 5K RF/t -> 250 RF/tick
        }
        if (batteryStack.getItem() instanceof ItemBatteryBasic) {
            return 1_000 / 20;    // 1K RF/t -> 50 RF/tick
        }

        // 通用方式：根据容量判断（兼容其他电池）
        IEnergyStorage energy = batteryStack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            int capacity = energy.getMaxEnergyStored();

            // 新电池容量判断
            if (capacity >= 50_000_000) return 5_000;   // 终极
            if (capacity >= 10_000_000) return 1_000;   // 精英
            if (capacity >= 1_000_000)  return 250;     // 高级
            if (capacity >= 100_000)    return 50;      // 基础

            // 旧配置兼容
            if (capacity >= 50_000_000)
                return EnergyBalanceConfig.BatterySystem.QUANTUM_BATTERY_OUTPUT * 2;
            else if (capacity >= 10_000_000)
                return EnergyBalanceConfig.BatterySystem.QUANTUM_BATTERY_OUTPUT * 3 / 2;
            else if (capacity >= EnergyBalanceConfig.BatterySystem.QUANTUM_BATTERY_CAPACITY)
                return EnergyBalanceConfig.BatterySystem.QUANTUM_BATTERY_OUTPUT;
            else if (capacity >= EnergyBalanceConfig.BatterySystem.ADVANCED_BATTERY_CAPACITY)
                return EnergyBalanceConfig.BatterySystem.ADVANCED_BATTERY_OUTPUT;
            else
                return EnergyBalanceConfig.BatterySystem.BASIC_BATTERY_OUTPUT;
        }
        return EnergyBalanceConfig.BatterySystem.BASIC_BATTERY_OUTPUT;
    }

    private static final ResourceLocation LOST_ENGINE_ID = new ResourceLocation("enigmaticlegacy", "lost_engine");

    private static boolean isLostEngine(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation registryName = stack.getItem().getRegistryName();
        if (registryName == null) return false;
        return registryName.equals(LOST_ENGINE_ID);
    }

    // 原有的 isEnigmaticItem 方法保持不变（用于识别）
    private static boolean isEnigmaticItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation registryName = stack.getItem().getRegistryName();
        if (registryName == null) return false;
        String modId = registryName.getNamespace().toLowerCase();
        String itemPath = registryName.getPath().toLowerCase();
        return modId.contains("enigma") || itemPath.contains("enigma");
    }

    // ✨ 新增：检测是否是被阻止的 Enigmatic 物品（排除 lost_engine）
    private static boolean isBlockedEnigmaticItem(ItemStack stack) {
        if (!isEnigmaticItem(stack)) return false;
        // Lost Engine 不被阻止
        if (isLostEngine(stack)) return false;
        return true;
    }

    private static boolean isFromKnownEnigmaticMods(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation registryName = stack.getItem().getRegistryName();
        if (registryName == null) return false;
        String modId = registryName.getNamespace();
        return modId.equals("enigmaticlegacy") ||
                modId.equals("enigmaticaddons") ||
                modId.equals("enigmaticgraves") ||
                modId.startsWith("enigmatic");
    }

    private static String getItemDisplayName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "未知物品";
        try {
            return stack.getDisplayName();
        } catch (Exception e) {
            return stack.getItem().getRegistryName() != null ?
                    stack.getItem().getRegistryName().getPath() : "未知物品";
        }
    }

    // ✨ 修改：检测是否装备了被阻止的 Enigmatic 物品（排除 lost_engine）
    private static boolean hasAnyEnigmaticItemEquipped(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles == null) return false;
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack bauble = baubles.getStackInSlot(i);
                // 使用新的检测方法，排除 lost_engine
                if (BLOCK_ALL_ENIGMATIC && isBlockedEnigmaticItem(bauble)) {
                    if (VERBOSE_ENIGMATIC_DETECTION && !player.world.isRemote) {
                        System.out.println("[MechanicalCore] 检测到被阻止的 Enigmatic 物品: " + getItemDisplayName(bauble));
                    }
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    // ✨ 修改：获取装备的被阻止的 Enigmatic 物品列表（排除 lost_engine）
    private static List<ItemStack> getEquippedEnigmaticItems(EntityPlayer player) {
        List<ItemStack> enigmaticItems = new ArrayList<>();
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles == null) return enigmaticItems;
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack bauble = baubles.getStackInSlot(i);
                // 只添加被阻止的 Enigmatic 物品
                if (isBlockedEnigmaticItem(bauble)) {
                    enigmaticItems.add(bauble);
                }
            }
        } catch (Throwable ignored) {}
        return enigmaticItems;
    }

    private static final ResourceLocation CURSED_RING_ID = new ResourceLocation("enigmaticlegacy", "cursed_ring");
    private static final ResourceLocation BLESSED_RING_ID = new ResourceLocation("enigmaticaddons", "blessed_ring");

    private static boolean isCursedRing(ItemStack s) {
        return s != null && !s.isEmpty() && s.getItem().getRegistryName() != null &&
                s.getItem().getRegistryName().equals(CURSED_RING_ID);
    }

    private static boolean isBlessedRing(ItemStack s) {
        return s != null && !s.isEmpty() && s.getItem().getRegistryName() != null &&
                s.getItem().getRegistryName().equals(BLESSED_RING_ID);
    }

    private static boolean isConflictingRing(ItemStack s) {
        return isCursedRing(s) || isBlessedRing(s);
    }

// ItemMechanicalCore.java - Part 2
// 包含：电池充电、显示方法、Baubles接口等

    // =====================================================================
    // GUI 暂停/禁用统一拦截（保持原样）
    // =====================================================================

    private static final java.util.Set<String> WATERPROOF_ALIASES = new HashSet<>(
            Arrays.asList("WATERPROOF_MODULE","WATERPROOF","waterproof_module","waterproof")
    );

    private static String norm(String id) {
        return id == null ? "" : id.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public static boolean isTemporarilyBlockedByGui(ItemStack stack, String upgradeId) {
        if (stack == null || stack.isEmpty() || upgradeId == null) return false;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return false;

        String up = upgradeId.toUpperCase();
        String lo = upgradeId.toLowerCase();

        java.util.List<String> keys = new java.util.ArrayList<>();
        keys.add(upgradeId); keys.add(up); keys.add(lo);

        if (up.contains("WATERPROOF") || WATERPROOF_ALIASES.contains(upgradeId)
                || WATERPROOF_ALIASES.contains(up) || WATERPROOF_ALIASES.contains(lo)) {
            for (String alias : WATERPROOF_ALIASES) {
                keys.add(alias);
                keys.add(alias.toUpperCase());
                keys.add(alias.toLowerCase());
            }
        }

        for (String k : keys) {
            if (nbt.getBoolean("Disabled_" + k) || nbt.getBoolean("IsPaused_" + k)) {
                return true;
            }
        }
        return false;
    }

    // ===== 惩罚/冷却方法（保持原样） =====

    public static boolean isPenalized(ItemStack core, String id) {
        if (core == null || core.isEmpty() || !core.hasTagCompound()) return false;
        long exp = core.getTagCompound().getLong("PenaltyExpire_" + id);
        return exp > System.currentTimeMillis();
    }

    public static int getPenaltyCap(ItemStack core, String id) {
        if (core == null || core.isEmpty() || !core.hasTagCompound()) return 0;
        return core.getTagCompound().getInteger("PenaltyCap_" + id);
    }

    public static int getPenaltySecondsLeft(ItemStack core, String id) {
        if (core == null || core.isEmpty() || !core.hasTagCompound()) return 0;
        long exp = core.getTagCompound().getLong("PenaltyExpire_" + id);
        long left = exp - System.currentTimeMillis();
        return left > 0 ? (int)(left / 1000) : 0;
    }

    public static int getPenaltyTier(ItemStack core, String id) {
        if (core == null || core.isEmpty() || !core.hasTagCompound()) return 0;
        return core.getTagCompound().getInteger("PenaltyTier_" + id);
    }

    public static void applyPenalty(ItemStack core, String id, int cap, int seconds, int tierInc, int debtFE, int debtXP) {
        if (core == null || core.isEmpty()) return;
        NBTTagCompound nbt = getOrCreateNBT(core);
        long expire = System.currentTimeMillis() + Math.max(1000, seconds * 1000L);
        int newTier = Math.max(0, nbt.getInteger("PenaltyTier_" + id) + Math.max(0, tierInc));
        nbt.setInteger("PenaltyCap_" + id, Math.max(1, cap));
        nbt.setLong("PenaltyExpire_" + id, expire);
        nbt.setInteger("PenaltyTier_" + id, newTier);
        if (debtFE > 0) nbt.setInteger("PenaltyDebtFE_" + id, debtFE);
        if (debtXP > 0) nbt.setInteger("PenaltyDebtXP_" + id, debtXP);
    }

    public static void clearPenalty(ItemStack core, String id) {
        if (core == null || core.isEmpty() || !core.hasTagCompound()) return;
        NBTTagCompound nbt = core.getTagCompound();
        nbt.removeTag("PenaltyCap_" + id);
        nbt.removeTag("PenaltyExpire_" + id);
        nbt.removeTag("PenaltyDebtFE_" + id);
        nbt.removeTag("PenaltyDebtXP_" + id);
        nbt.removeTag("PenaltyTier_" + id);
    }

    public static boolean tryPayPenaltyDebt(EntityPlayer p, ItemStack core, String id) {
        if (core == null || core.isEmpty() || !core.hasTagCompound()) return false;
        NBTTagCompound nbt = core.getTagCompound();
        int fe = nbt.getInteger("PenaltyDebtFE_" + id);
        int xp = nbt.getInteger("PenaltyDebtXP_" + id);

        boolean ok = true;
        if (ok && fe > 0) {
            IEnergyStorage es = getEnergyStorage(core);
            if (es == null || es.extractEnergy(fe, true) < fe) ok = false;
            if (ok) es.extractEnergy(fe, false);
        }
        if (ok && xp > 0) {
            if (p.experienceTotal < xp) ok = false;
            else p.addExperience(-xp);
        }
        if (ok) clearPenalty(core, id);
        return ok;
    }

    // ===== 升级激活判定（保持原样） =====

    public static boolean isUpgradeActive(ItemStack stack, String upgradeId) {
        if (!isMechanicalCore(stack)) return false;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return false;

        if (isTemporarilyBlockedByGui(stack, upgradeId)) return false;

        if (isCheckingUpgrade.get()) {
            return getUpgradeLevelDirect(stack, upgradeId) > 0 && !nbt.getBoolean("Disabled_" + upgradeId);
        }

        try {
            isCheckingUpgrade.set(true);

            if (nbt.getBoolean("Disabled_" + upgradeId)) return false;

            int level = getUpgradeLevelDirect(stack, upgradeId);
            if (level <= 0) return false;

            if (isEnergyGeneratorUpgrade(upgradeId) || "energy_capacity".equalsIgnoreCase(upgradeId)) {
                return true;
            }

            return isEnergyDepletionAllowed(stack, upgradeId);
        } finally {
            isCheckingUpgrade.set(false);
        }
    }

    private static boolean isEnergyGeneratorUpgrade(String upgradeId) {
        String id = upgradeId.toUpperCase();
        return id.contains("KINETIC_GENERATOR") ||
                id.contains("SOLAR_GENERATOR")   ||
                id.contains("VOID_ENERGY")       ||
                id.contains("COMBAT_CHARGER")    ||
                id.contains("GENERATOR")         ||
                id.contains("CHARGER")           ||
                id.contains("ENERGY_GEN");
    }

    private boolean isEnergyGeneratorActive(ItemStack stack, String upgradeId) {
        if (!isMechanicalCore(stack)) return false;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return false;
        if (nbt.getBoolean("Disabled_" + upgradeId)) return false;
        return getUpgradeLevelDirect(stack, upgradeId) > 0;
    }

    private static boolean isEnergyDepletionAllowed(ItemStack stack, String upgradeId) {
        if (isCalculatingEnergy.get()) return true;
        try {
            return EnergyDepletionManager.isUpgradeActive(stack, upgradeId);
        } catch (Throwable t) { return true; }
    }

    private static int getUpgradeLevelDirect(ItemStack stack, String upgradeId) {
        if (!stack.hasTagCompound()) return 0;

        int level = stack.getTagCompound().getInteger("upgrade_" + upgradeId);
        if (level > 0) {
            if (isTemporarilyBlockedByGui(stack, upgradeId)) return 0;
            return level;
        }

        try {
            if (!isCheckingUpgrade.get()) level = ItemMechanicalCoreExtended.getUpgradeLevel(stack, upgradeId);
        } catch (Throwable ignored) {}

        if (level > 0 && isTemporarilyBlockedByGui(stack, upgradeId)) return 0;
        return level;
    }

    public static int getEffectiveUpgradeLevel(ItemStack stack, String upgradeId) {
        return isUpgradeActive(stack, upgradeId) ? getUpgradeLevelDirect(stack, upgradeId) : 0;
    }

    public static int getEffectiveUpgradeLevel(ItemStack stack, UpgradeType type) {
        return getEffectiveUpgradeLevel(stack, type.getKey());
    }

    // ===== 电池/充电（已更新集成新电池系统） =====

    private boolean hasBatteryEquippedOrCarried(@Nullable EntityPlayer player) {
        if (player == null) return false;

        long wt = player.world.getTotalWorldTime();
        BatteryCache cache = batteryCache.computeIfAbsent(player.getUniqueID(), k -> new BatteryCache());

        if (cache.isValid(wt)) {
            return cache.hasBattery;
        }

        cache.hasBattery = actuallyCheckBatteryWithCharge(player);
        cache.batteryTier = detectBatteryTier(player);
        cache.lastCheck = wt;

        return cache.hasBattery;
    }

    private boolean actuallyCheckBatteryWithCharge(EntityPlayer player) {
        for (ItemStack stack : player.inventory.mainInventory) {
            if (isBatteryItemWithCharge(stack)) return true;
        }
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    if (isBatteryItemWithCharge(baubles.getStackInSlot(i))) return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean isBatteryItemWithCharge(ItemStack stack) {
        if (!isBatteryItem(stack)) return false;
        if (stack.getItem() instanceof ItemCreativeBatteryBauble) return true;
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        return energy != null && energy.getEnergyStored() > 0;
    }

    private int detectBatteryTier(EntityPlayer player) {
        int maxTier = 0;
        for (ItemStack s : player.inventory.mainInventory) {
            if (isBatteryItemWithCharge(s)) { maxTier = Math.max(maxTier, getBatteryTier(s)); }
        }
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack s = baubles.getStackInSlot(i);
                    if (isBatteryItemWithCharge(s)) { maxTier = Math.max(maxTier, getBatteryTier(s)); }
                }
            }
        } catch (Throwable ignored) {}
        return maxTier;
    }

    private ItemStack findChargedBattery(EntityPlayer player) {
        ItemStack bestBattery = ItemStack.EMPTY;
        int maxCharge = 0;

        for (ItemStack stack : player.inventory.mainInventory) {
            if (isBatteryItem(stack)) {
                if (stack.getItem() instanceof ItemCreativeBatteryBauble) {
                    return stack;
                }
                IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
                if (energy != null && energy.getEnergyStored() > maxCharge) {
                    maxCharge = energy.getEnergyStored();
                    bestBattery = stack;
                }
            }
        }

        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (isBatteryItem(stack)) {
                        if (stack.getItem() instanceof ItemCreativeBatteryBauble) {
                            return stack;
                        }
                        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
                        if (energy != null && energy.getEnergyStored() > maxCharge) {
                            maxCharge = energy.getEnergyStored();
                            bestBattery = stack;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return bestBattery;
    }

    /**
     * 修复的电池充电方法 —— 不播放音效
     */
    private void applyBatteryGeneration(ItemStack core, EntityPlayer player) {
        if (!hasBatteryEquippedOrCarried(player)) return;

        IEnergyStorage coreEnergy = getEnergyStorage(core);
        if (coreEnergy == null) return;

        float percent = (float) coreEnergy.getEnergyStored() / Math.max(1, coreEnergy.getMaxEnergyStored());
        if (percent >= 1.0f) return;

        ItemStack batteryStack = findChargedBattery(player);
        if (batteryStack == null || batteryStack.isEmpty()) return;

        if (batteryStack.getItem() instanceof ItemCreativeBatteryBauble) {
            int transferRate = getTransferRateForBattery(batteryStack);
            coreEnergy.receiveEnergy(transferRate / 20, false);
            return;
        }

        IEnergyStorage batteryEnergy = batteryStack.getCapability(CapabilityEnergy.ENERGY, null);
        if (batteryEnergy == null) return;
        if (batteryEnergy.getEnergyStored() <= 0) return;

        int transferRate = getTransferRateForBattery(batteryStack) / 20;

        int extracted = batteryEnergy.extractEnergy(transferRate, true);
        if (extracted > 0) {
            int actualExtracted = batteryEnergy.extractEnergy(extracted, false);
            if (actualExtracted > 0) {
                coreEnergy.receiveEnergy(actualExtracted, false);
            }
        }
    }

    /**
     * ✨ 更新：显示新电池系统的充电状态
     */
    private void displayBatteryChargingStatus(EntityPlayer player, ItemStack core) {
        ItemStack battery = findChargedBattery(player);
        if (!battery.isEmpty()) {
            IEnergyStorage batteryEnergy = battery.getCapability(CapabilityEnergy.ENERGY, null);
            IEnergyStorage coreEnergy = getEnergyStorage(core);

            // 创造电池
            if (battery.getItem() instanceof ItemCreativeBatteryBauble) {
                if (coreEnergy != null) {
                    float corePercent = (float) coreEnergy.getEnergyStored() / Math.max(1, coreEnergy.getMaxEnergyStored());
                    if (corePercent < 0.95f) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.LIGHT_PURPLE + "⚡ 创造电池充电中 [∞]"
                        ), true);
                    }
                }
            }
            // 新电池系统显示
            else if (battery.getItem() instanceof ItemBatteryBase) {
                if (batteryEnergy != null && coreEnergy != null) {
                    float batteryPercent = (float) batteryEnergy.getEnergyStored() / Math.max(1, batteryEnergy.getMaxEnergyStored());
                    float corePercent = (float) coreEnergy.getEnergyStored() / Math.max(1, coreEnergy.getMaxEnergyStored());

                    String batteryType = "";
                    TextFormatting batteryColor = TextFormatting.GRAY;

                    if (battery.getItem() instanceof ItemBatteryUltimate) {
                        batteryType = "终极";
                        batteryColor = TextFormatting.LIGHT_PURPLE;
                    } else if (battery.getItem() instanceof ItemBatteryElite) {
                        batteryType = "精英";
                        batteryColor = TextFormatting.AQUA;
                    } else if (battery.getItem() instanceof ItemBatteryAdvanced) {
                        batteryType = "高级";
                        batteryColor = TextFormatting.YELLOW;
                    } else if (battery.getItem() instanceof ItemBatteryBasic) {
                        batteryType = "基础";
                        batteryColor = TextFormatting.GRAY;
                    }

                    if (batteryPercent > 0.1f && corePercent < 0.95f) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.GREEN + "⚡ 充电中 " +
                                        batteryColor + "[" + batteryType + "电池: " +
                                        TextFormatting.AQUA + String.format("%.0f%%", batteryPercent * 100) +
                                        batteryColor + "]"
                        ), true);
                    } else if (batteryPercent <= 0.1f && corePercent < 0.5f) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.RED + "⚠ " + batteryType + "电池电量低，请充电！"
                        ), true);
                    }
                }
            }
            // 旧电池系统兼容
            else if (batteryEnergy != null && coreEnergy != null) {
                float batteryPercent = (float) batteryEnergy.getEnergyStored() / Math.max(1, batteryEnergy.getMaxEnergyStored());
                float corePercent = (float) coreEnergy.getEnergyStored() / Math.max(1, coreEnergy.getMaxEnergyStored());

                if (batteryPercent > 0.1f && corePercent < 0.95f) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.GREEN + "⚡ 充电中 " +
                                    TextFormatting.GRAY + "[电池: " +
                                    TextFormatting.AQUA + String.format("%.0f%%", batteryPercent * 100) +
                                    TextFormatting.GRAY + "]"
                    ), true);
                } else if (batteryPercent <= 0.1f && corePercent < 0.5f) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.RED + "⚠ 电池电量低，请充电！"
                    ), true);
                }
            }
        }
    }// ItemMechanicalCore.java - Part 3
// 包含：能量消耗、被动消耗计算、Baubles接口实现

    // ===== 能量消耗方法（保持原样） =====

    public static boolean consumeEnergy(ItemStack stack, int baseAmount) {
        return consumeEnergy(stack, baseAmount, true);
    }

    public static boolean consumeEnergy(ItemStack stack, int baseAmount, boolean applyEfficiency) {
        IEnergyStorage energy = getEnergyStorage(stack);
        if (energy == null || baseAmount <= 0) return false;

        int actual = baseAmount;
        if (applyEfficiency && isUpgradeActive(stack, "energy_efficiency"))
            actual = calculateActualEnergyCost(stack, baseAmount);

        if (energy.extractEnergy(actual, true) >= actual) {
            energy.extractEnergy(actual, false);
            if (applyEfficiency && baseAmount > actual)
                recordEnergySaved(stack, baseAmount - actual);
            return true;
        }
        return false;
    }

    public static boolean consumeEnergyForUpgradeBalanced(ItemStack stack, String upgradeId, int baseAmount) {
        if (!isUpgradeActive(stack, upgradeId)) return false;

        int configured = baseAmount > 0 ? baseAmount : getDefaultConsumptionForUpgrade(upgradeId);
        int actual = configured;
        if (isUpgradeActive(stack, "energy_efficiency"))
            actual = calculateActualEnergyCost(stack, configured);

        IEnergyStorage energy = getEnergyStorage(stack);
        if (energy == null || actual <= 0) return false;

        if (energy.extractEnergy(actual, true) >= actual) {
            energy.extractEnergy(actual, false);
            if (configured > actual) recordEnergySaved(stack, configured - actual);
            return true;
        }
        return false;
    }

    private static int getDefaultConsumptionForUpgrade(String upgradeId) {
        switch (upgradeId.toUpperCase()) {
            case "ORE_VISION":      return EnergyBalanceConfig.AuxiliaryActive.ORE_VISION_BASE;
            case "STEALTH":         return Math.max(1, EnergyBalanceConfig.AuxiliaryActive.STEALTH_LEVEL_1 / 20);
            case "EXP_AMPLIFIER":   return EnergyBalanceConfig.AuxiliaryActive.EXP_AMPLIFIER_BASE;
            case "DAMAGE_BOOST":    return EnergyBalanceConfig.CombatActive.DAMAGE_BOOST_PER_HIT;
            case "CRITICAL_STRIKE": return EnergyBalanceConfig.CombatActive.CRITICAL_STRIKE;
            case "PURSUIT":         return EnergyBalanceConfig.CombatActive.PURSUIT_MARK;
            case "RANGE_EXTENSION": return EnergyBalanceConfig.CombatActive.RANGE_INDICATOR;
            case "YELLOW_SHIELD":   return EnergyBalanceConfig.SurvivalActive.SHIELD_MAINTAIN_PER_LEVEL;
            case "HEALTH_REGEN":    return EnergyBalanceConfig.SurvivalActive.HEALTH_REGEN_PER_LEVEL;
            case "HUNGER_THIRST":   return EnergyBalanceConfig.SurvivalActive.HUNGER_RESTORE;
            case "FIRE_EXTINGUISH": return EnergyBalanceConfig.SurvivalActive.FIRE_EXTINGUISH;
            default: return 100;
        }
    }

    public static int calculateActualEnergyCost(ItemStack stack, int baseAmount) {
        if (baseAmount <= 0) return 0;
        int efficiencyLevel = getEffectiveUpgradeLevel(stack, "energy_efficiency");
        double mul = getEfficiencyMultiplier(efficiencyLevel);
        int actual = (int) (baseAmount * mul);
        if (baseAmount > 0 && actual <= 0) actual = 1;
        return actual;
    }

    // ===== 主动消耗管理器（保持原样） =====

    public static class ActiveEnergyConsumption {
        public static boolean consumeForOreVision(ItemStack stack, int level, boolean isScanning) {
            int cost = EnergyBalanceConfig.AuxiliaryActive.ORE_VISION_BASE + (level * EnergyBalanceConfig.AuxiliaryActive.ORE_VISION_PER_LEVEL);
            if (isScanning) cost += EnergyBalanceConfig.AuxiliaryActive.ORE_VISION_SCAN;
            return consumeEnergyForUpgradeBalanced(stack, "ORE_VISION", cost);
        }
        public static boolean consumeForStealth(ItemStack stack, int level) {
            int perSec = level == 1 ? EnergyBalanceConfig.AuxiliaryActive.STEALTH_LEVEL_1 :
                    level == 2 ? EnergyBalanceConfig.AuxiliaryActive.STEALTH_LEVEL_2 :
                            EnergyBalanceConfig.AuxiliaryActive.STEALTH_LEVEL_3;
            return consumeEnergyForUpgradeBalanced(stack, "STEALTH", Math.max(1, perSec / 20));
        }
        public static boolean consumeForExpAmplifier(ItemStack stack, int expAmount) {
            int cost = EnergyBalanceConfig.AuxiliaryActive.EXP_AMPLIFIER_BASE + expAmount * EnergyBalanceConfig.AuxiliaryActive.EXP_AMPLIFIER_MULTIPLIER;
            return consumeEnergyForUpgradeBalanced(stack, "EXP_AMPLIFIER", cost);
        }
        public static boolean consumeForDamageBoost(ItemStack s) { return consumeEnergyForUpgradeBalanced(s, "DAMAGE_BOOST", EnergyBalanceConfig.CombatActive.DAMAGE_BOOST_PER_HIT); }
        public static boolean consumeForCriticalStrike(ItemStack s){ return consumeEnergyForUpgradeBalanced(s, "CRITICAL_STRIKE", EnergyBalanceConfig.CombatActive.CRITICAL_STRIKE); }
        public static boolean consumeForPursuitMark(ItemStack s)   { return consumeEnergyForUpgradeBalanced(s, "PURSUIT", EnergyBalanceConfig.CombatActive.PURSUIT_MARK); }
        public static boolean consumeForPursuitDash(ItemStack s)   { return consumeEnergyForUpgradeBalanced(s, "PURSUIT", EnergyBalanceConfig.CombatActive.PURSUIT_DASH); }
        public static boolean consumeForRangeIndicator(ItemStack s){ return consumeEnergyForUpgradeBalanced(s, "RANGE_EXTENSION", EnergyBalanceConfig.CombatActive.RANGE_INDICATOR); }
        public static boolean consumeForShieldMaintain(ItemStack s,int lvl){ return consumeEnergyForUpgradeBalanced(s, "YELLOW_SHIELD", lvl * EnergyBalanceConfig.SurvivalActive.SHIELD_MAINTAIN_PER_LEVEL); }
        public static boolean consumeForShieldRestore(ItemStack s,int pts){ return consumeEnergyForUpgradeBalanced(s, "YELLOW_SHIELD", pts * EnergyBalanceConfig.SurvivalActive.SHIELD_RESTORE_PER_POINT); }
        public static boolean consumeForHealthRegen(ItemStack s,int lvl){ return consumeEnergyForUpgradeBalanced(s, "HEALTH_REGEN", lvl * EnergyBalanceConfig.SurvivalActive.HEALTH_REGEN_PER_LEVEL); }
        public static boolean consumeForHungerRestore(ItemStack s) { return consumeEnergyForUpgradeBalanced(s, "HUNGER_THIRST", EnergyBalanceConfig.SurvivalActive.HUNGER_RESTORE); }
        public static boolean consumeForThirstRestore(ItemStack s) { return consumeEnergyForUpgradeBalanced(s, "HUNGER_THIRST", EnergyBalanceConfig.SurvivalActive.THIRST_RESTORE); }
        public static boolean consumeForFireExtinguish(ItemStack s){ return consumeEnergyForUpgradeBalanced(s, "FIRE_EXTINGUISH", EnergyBalanceConfig.SurvivalActive.FIRE_EXTINGUISH); }
    }

    // ===== 被动消耗（含电池/过载/泄漏/效率等）=====

    private int calculateActivePassiveConsumption(ItemStack stack, @Nullable EntityPlayer player) {
        int baseConsumption = 0;

        // 基础升级
        for (UpgradeType type : UpgradeType.values()) {
            String id = type.getKey();
            if (isUpgradeActive(stack, id)) {
                int lv = getUpgradeLevel(stack, id);
                if (lv > 0) {
                    int c = EnergyBalanceConfig.getPassiveDrain(id, lv);
                    baseConsumption += c;
                }
            }
        }

        // 扩展升级（统一常量）
        for (String id : EXTENDED_UPGRADE_IDS) {
            if (isUpgradeActive(stack, id)) {
                int lv = 0;
                try { lv = ItemMechanicalCoreExtended.getUpgradeLevel(stack, id); } catch (Throwable ignored) {}
                if (lv > 0) {
                    int c = EnergyBalanceConfig.getPassiveDrain(id, lv);
                    baseConsumption += c;
                }
            }
        }

        if (getTotalInstalledUpgrades(stack) > 0)
            baseConsumption += EnergyBalanceConfig.BASE_PASSIVE_DRAIN;

        ModuleStats stats = collectModuleStats(stack);
        IEnergyStorage es = getEnergyStorage(stack);
        float energyPercent = 0f;
        if (es != null && es.getMaxEnergyStored() > 0)
            energyPercent = (float) es.getEnergyStored() / es.getMaxEnergyStored();

        boolean hasBattery = hasBatteryEquippedOrCarried(player);
        int total = EnergyBalanceConfig.calculateTotalDrain(baseConsumption, stats.totalLevels, stats.typesInstalled, energyPercent, hasBattery);

        // 电池等级额外优化（只在有电电池时）
        if (hasBattery && player != null) {
            ItemStack chargedBattery = findChargedBattery(player);
            if (!chargedBattery.isEmpty()) {
                int batteryTier = getBatteryTier(chargedBattery);
                float bonus = 1.0f;
                switch (batteryTier) {
                    case 1: bonus = 0.95f; break;
                    case 2: bonus = 0.90f; break;
                    case 3: bonus = 0.85f; break;
                    case 4: bonus = 0.80f; break;
                    case 5: bonus = 0.70f; break;
                }
                total = (int)(total * bonus);
            }
        }

        if (isUpgradeActive(stack, "energy_efficiency"))
            total = calculateActualEnergyCost(stack, total);

        if (DEBUG_MODE)
            System.out.println("[MechanicalCore] 最终被动消耗: " + total + " RF/s");
        return Math.max(0, total);
    }

    private ModuleStats collectModuleStats(ItemStack stack) {
        int types = 0, levels = 0;
        NBTTagCompound nbt = stack.getTagCompound();
        Set<String> seen = new HashSet<>();

        // 基础枚举
        for (UpgradeType t : UpgradeType.values()) {
            String id = t.getKey();
            int lv = getUpgradeLevel(stack, id);
            boolean installed = (nbt != null && nbt.getBoolean("HasUpgrade_" + id)) || lv > 0;
            if (installed && seen.add(norm(id))) types++;
            if (lv > 0) levels += lv;
        }

        // 扩展
        for (String id : EXTENDED_UPGRADE_IDS) {
            int lv = 0;
            try { lv = ItemMechanicalCoreExtended.getUpgradeLevel(stack, id); } catch (Throwable ignored) {}
            boolean installed = (nbt != null && nbt.getBoolean("HasUpgrade_" + id)) || lv > 0;
            if (installed && seen.add(norm(id))) types++;
            if (lv > 0) levels += lv;
        }
        return new ModuleStats(types, levels);
    }

    // ===== Baubles 接口实现 =====

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.HEAD;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) entity;

        // 电池供能（无音效）
        applyBatteryGeneration(itemstack, player);

        if (entity.world.getTotalWorldTime() % 20 == 0) {
            EnergyDepletionManager.handleEnergyDepletion(itemstack, player);

            int passive = calculateActivePassiveConsumption(itemstack, player);
            if (passive > 0 && !consumeEnergy(itemstack, passive, false)) {
                handleInsufficientEnergy(itemstack, player, passive);
            }

            // 显示电池充电状态（文本提示，不发声）
            if (entity.world.getTotalWorldTime() % 100 == 0) {
                displayBatteryChargingStatus(player, itemstack);
            }

            if (entity.world.getTotalWorldTime() % 200 == 0) {
                displayEnergyStatusToPlayer(player, itemstack);
            }
        }

        applyUpgradeEffects(itemstack, player);
    }

    private void applyUpgradeEffects(ItemStack stack, EntityPlayer player) {
        try {
            // 不授予/不修改飞行能力，飞行由推力逻辑负责
            UpgradeEffectManager.applyAllEffects(player, stack);

            // 辅助：没开潜行/隐形则确保关闭隐形之类的效果
            try {
                if (!isUpgradeActive(stack, "STEALTH")) {
                    AuxiliaryUpgradeManager.StealthSystem.disableStealth(player);
                }
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            System.err.println("[moremod] 应用升级效果时出错: " + t.getMessage());
        }
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase entity) {
        if (entity.world.isRemote || !(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        // ✅ 时间限制检查（装备成功后的二次验证）
        if (EquipmentTimeConfig.restriction.enabled) {
            // 再次检查是否被禁止（双重保险）
            if (EquipmentTimeTracker.isPermanentlyBanned(player)) {
                forceUnequipCore(player, itemstack);
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "✗ 你已被永久禁止佩戴机械核心！"
                ));
                return;
            }

            // 检查当前是否超时
            long remainingTime = EquipmentTimeTracker.getRemainingTime(player);

            if (remainingTime == 0 && !EquipmentTimeTracker.hasEquippedInTime(player)) {
                // ⚠️ 超时了！立即卸下
                forceUnequipCore(player, itemstack);

                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "✗ 时间已到！你已无法佩戴机械核心！"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "⏰ 佩戴时限已过期"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "机械核心拒绝与你建立连接，已退回背包..."
                ));

                if (EquipmentTimeConfig.restriction.allowAdminReset) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "联系管理员使用 /moremod resetequiptime 重置限制。"
                    ));
                }

                // 标记为禁止
                EquipmentTimeTracker.markAsBanned(player);
                return;  // ✅ 直接返回，不执行后续的初始化逻辑
            }

            // ✅ 在时间内成功装备
            EquipmentTimeTracker.onCoreEquipped(player);
        }

        // 自动卸下冲突物品
        removeConflictingItems(player, false);

        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_AQUA + "⚙ 机械核心已激活！系统开始启动..."
        ));

        IEnergyStorage energy = getEnergyStorage(itemstack);
        if (energy != null && energy.getEnergyStored() == 0) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚡ 警告：能量耗尽！能源发电模块仍可工作"
            ));
            checkAndNotifyEnergyGenerators(itemstack, player);
        } else {
            EnergyDepletionManager.displayDetailedEnergyStatus(player, itemstack);
        }
    }

// ===== 新增方法：强制卸下机械核心（方案3 - 最安全） =====
    /**
     * ✅ 强制卸下机械核心（永不掉落，强制放入背包）
     * @param player 玩家
     * @param coreStack 要卸下的核心（用于日志记录）
     */
    private void forceUnequipCore(EntityPlayer player, ItemStack coreStack) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles == null) {
                System.err.println("[MechanicalCore] 无法获取 Baubles 处理器");
                return;
            }

            // 查找并移除机械核心
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack bauble = baubles.getStackInSlot(i);

                if (isMechanicalCore(bauble)) {
                    ItemStack removed = baubles.extractItem(i, 1, false);

                    if (!removed.isEmpty()) {
                        boolean placed = false;

                        // ✅ 策略1：尝试正常放入背包
                        if (player.inventory.addItemStackToInventory(removed)) {
                            placed = true;
                            if (DEBUG_MODE) {
                                System.out.println("[MechanicalCore] 核心已返还到背包");
                            }
                        }

                        // ✅ 策略2：找第一个空槽位
                        if (!placed) {
                            for (int slot = 0; slot < player.inventory.mainInventory.size(); slot++) {
                                if (player.inventory.mainInventory.get(slot).isEmpty()) {
                                    player.inventory.mainInventory.set(slot, removed);
                                    placed = true;
                                    if (DEBUG_MODE) {
                                        System.out.println("[MechanicalCore] 核心已放入空槽位 " + slot);
                                    }
                                    break;
                                }
                            }
                        }

                        // ✅ 策略3：强制替换第一个槽位
                        if (!placed) {
                            ItemStack displaced = player.inventory.mainInventory.get(0);
                            player.inventory.mainInventory.set(0, removed);

                            // 被替换的物品处理
                            if (!displaced.isEmpty()) {
                                // 尝试放入其他位置
                                if (!player.inventory.addItemStackToInventory(displaced)) {
                                    // 实在没办法，生成掉落物（这不是机械核心，不会被保护拦截）
                                    EntityItem entityItem = new EntityItem(
                                            player.world,
                                            player.posX,
                                            player.posY,
                                            player.posZ,
                                            displaced
                                    );
                                    entityItem.setNoPickupDelay();
                                    player.world.spawnEntity(entityItem);

                                    if (DEBUG_MODE) {
                                        System.out.println("[MechanicalCore] 被替换的物品已掉落: " +
                                                displaced.getDisplayName());
                                    }
                                }
                            }

                            player.sendMessage(new TextComponentString(
                                    TextFormatting.YELLOW + "⚠ 背包已满，机械核心已强制放入第一格"
                            ));
                            placed = true;
                        }

                        if (placed) {
                            player.sendMessage(new TextComponentString(
                                    TextFormatting.GRAY + "机械核心已返回背包"
                            ));
                        }

                        if (DEBUG_MODE) {
                            System.out.println("[MechanicalCore] 成功强制卸下核心: " + player.getName());
                        }
                    }
                    break;
                }
            }
        } catch (Throwable t) {
            System.err.println("[MechanicalCore] 强制卸下失败: " + t.getMessage());
            t.printStackTrace();

            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 系统错误！请联系管理员"
            ));
        }
    }

// ===== 新增方法：格式化剩余时间 =====
    /**
     * 格式化剩余时间为可读字符串
     * @param seconds 剩余秒数
     * @return 格式化后的字符串
     */


    /**
     * ===== 新增辅助方法 =====
     * 格式化剩余时间
     */


    private static void removeConflictingItems(EntityPlayer player, boolean isUnequippingCore) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles == null) return;

            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack bauble = baubles.getStackInSlot(i);
                if (bauble.isEmpty()) continue;

                if (isUnequippingCore && isMechanicalCore(bauble)) {
                    continue;
                }

                // ✨ 修改：检测并移除被阻止的 Enigmatic 物品（lost_engine 不会被移除）
                if (!isUnequippingCore && (BLOCK_ALL_ENIGMATIC && isBlockedEnigmaticItem(bauble))) {
                    ItemStack removed = baubles.extractItem(i, 1, false);
                    if (!removed.isEmpty()) {
                        if (!player.inventory.addItemStackToInventory(removed)) {
                            player.dropItem(removed, false);
                        }
                        String itemName = getItemDisplayName(bauble);
                        player.sendMessage(new TextComponentString(
                                TextFormatting.YELLOW + itemName + " 已自动卸下 - 与机械核心不兼容"
                        ));
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[moremod] 移除冲突饰品时出错: " + t.getMessage());
        }
    }

    private void checkAndNotifyEnergyGenerators(ItemStack stack, EntityPlayer player) {
        List<String> gens = new ArrayList<>();
        if (isEnergyGeneratorActive(stack, "KINETIC_GENERATOR")) gens.add("动能发电（移动充能）");
        if (isEnergyGeneratorActive(stack, "SOLAR_GENERATOR"))   gens.add("太阳能（日光充能）");
        if (isEnergyGeneratorActive(stack, "VOID_ENERGY"))       gens.add("虚空能量（深层/末地充能）");
        if (isEnergyGeneratorActive(stack, "COMBAT_CHARGER"))    gens.add("战斗充能（击杀充能）");

        if (!gens.isEmpty()) {
            player.sendMessage(new TextComponentString(TextFormatting.GREEN + "可用能源模块："));
            for (String g : gens)
                player.sendMessage(new TextComponentString(TextFormatting.AQUA + "  • " + g));
        } else {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 无可用能源模块！请安装并激活能源升级"
            ));
        }

        ItemStack battery = findChargedBattery(player);
        if (!battery.isEmpty()) {
            if (battery.getItem() instanceof ItemCreativeBatteryBauble) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "✦ 检测到创造电池 - 无限能量供应"
                ));
            } else {
                IEnergyStorage batteryEnergy = battery.getCapability(CapabilityEnergy.ENERGY, null);
                if (batteryEnergy != null) {
                    float percent = (float) batteryEnergy.getEnergyStored() / Math.max(1, batteryEnergy.getMaxEnergyStored());
                    TextFormatting color = percent > 0.5f ? TextFormatting.GREEN :
                            percent > 0.2f ? TextFormatting.YELLOW : TextFormatting.RED;
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + "电池状态: " + color + String.format("%.0f%%", percent * 100)
                    ));
                }
            }
        } else {
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_GRAY + "未检测到有电的电池"
            ));
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase entity) {
        if (entity.world.isRemote || !(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "⚙ 机械核心已停止运行..."
        ));
        removeAllUpgradeEffects(itemstack, player);
    }

    private void removeAllUpgradeEffects(ItemStack stack, EntityPlayer player) {
        if (!player.isCreative() && !player.isSpectator()) {
            player.capabilities.allowFlying = false;
            player.capabilities.isFlying = false;
            player.sendPlayerAbilities();
        }
        try {
            AuxiliaryUpgradeManager.MovementSpeedSystem.resetSpeed(player);
            AuxiliaryUpgradeManager.StealthSystem.disableStealth(player);
            AuxiliaryUpgradeManager.OreVisionSystem.toggleOreVision(player, false);
            CombatUpgradeManager.AttackSpeedSystem.removeAttackSpeed(player);
            CombatUpgradeManager.RangeExtensionSystem.removeReachExtension(player);
        } catch (Throwable t) {
            System.err.println("[moremod] 清理升级效果时发生错误: " + t.getMessage());
        }
    }
    // ItemMechanicalCore.java - Part 4 (最终部分)
// 包含：Tooltip、辅助方法、升级管理、能量Provider、冲突检测器

    // ===== Tooltip =====

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, net.minecraft.client.util.ITooltipFlag flagIn) {
        NBTTagCompound nbt = getOrCreateNBT(stack);

        tooltip.add("");
        tooltip.add(TextFormatting.DARK_AQUA + "═══ 机械核心 ═══");
        tooltip.add(TextFormatting.GOLD + "饰品类型: " + TextFormatting.WHITE + "头部");

        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            int stored = energy.getEnergyStored();
            int max = energy.getMaxEnergyStored();
            float pct = max > 0 ? (float) stored / max * 100f : 0f;
            TextFormatting ec = pct > 60 ? TextFormatting.GREEN : pct > 30 ? TextFormatting.YELLOW : TextFormatting.RED;
            tooltip.add(TextFormatting.GRAY + "能量: " + ec + formatEnergy(stored) + " / " + formatEnergy(max) + " RF " +
                    TextFormatting.GRAY + "(" + String.format("%.1f%%", pct) + ")");
        }

        if (isUpgradeActive(stack, "energy_efficiency")) {
            int effLv = getUpgradeLevel(stack, UpgradeType.ENERGY_EFFICIENCY);
            int percent = getEfficiencyPercentage(stack);
            TextFormatting col = percent >= 75 ? TextFormatting.LIGHT_PURPLE :
                    percent >= 60 ? TextFormatting.GOLD : TextFormatting.GREEN;
            tooltip.add(col + "⚡ 能量效率 Lv." + effLv + " (" + percent + "% 节能)");
        }

        int installed = getTotalInstalledUpgrades(stack);
        int active = getTotalActiveUpgradeLevel(stack);
        if (installed > 0) {
            tooltip.add(TextFormatting.GRAY + "升级状态: " + TextFormatting.GREEN + active + TextFormatting.GRAY +
                    " 激活 / " + TextFormatting.WHITE + installed + TextFormatting.GRAY + " 已安装");
        }

        if (nbt.getBoolean("EmergencyMode"))
            tooltip.add(TextFormatting.DARK_RED + "⚠ 紧急省电模式");

        if (GuiScreen.isShiftKeyDown()) {
            // ✨ 更新：新电池系统 Tooltip 显示
            EntityPlayer p = Minecraft.getMinecraft().player;
            if (p != null) {
                boolean hasBattery = false;
                String info = TextFormatting.DARK_GRAY + "未检测到";

                for (ItemStack s : p.inventory.mainInventory) {
                    // 新电池系统
                    if (s.getItem() instanceof ItemBatteryBase) {
                        hasBattery = true;
                        IEnergyStorage bs = s.getCapability(CapabilityEnergy.ENERGY, null);
                        if (bs != null) {
                            float bp = (float) bs.getEnergyStored() / Math.max(1, bs.getMaxEnergyStored());
                            TextFormatting bc = bp > 0.5f ? TextFormatting.GREEN :
                                    bp > 0.2f ? TextFormatting.YELLOW : TextFormatting.RED;

                            String type = "";
                            TextFormatting typeColor = TextFormatting.GRAY;
                            if (s.getItem() instanceof ItemBatteryUltimate) {
                                type = "终极";
                                typeColor = TextFormatting.LIGHT_PURPLE;
                            } else if (s.getItem() instanceof ItemBatteryElite) {
                                type = "精英";
                                typeColor = TextFormatting.AQUA;
                            } else if (s.getItem() instanceof ItemBatteryAdvanced) {
                                type = "高级";
                                typeColor = TextFormatting.YELLOW;
                            } else if (s.getItem() instanceof ItemBatteryBasic) {
                                type = "基础";
                                typeColor = TextFormatting.GRAY;
                            }

                            info = typeColor + type + " " + bc + String.format("%.0f%%", bp * 100f);
                        }
                        break;
                    }
                    // 创造电池
                    else if (s.getItem() instanceof ItemCreativeBatteryBauble) {
                        hasBattery = true;
                        info = TextFormatting.LIGHT_PURPLE + "∞ 创造";
                        break;
                    }
                    // 旧电池系统兼容
                    else if (s.getItem() instanceof ItemBatteryBauble) {
                        hasBattery = true;
                        IEnergyStorage bs = s.getCapability(CapabilityEnergy.ENERGY, null);
                        if (bs != null) {
                            float bp = (float) bs.getEnergyStored() / Math.max(1, bs.getMaxEnergyStored());
                            TextFormatting bc = bp > 0.5f ? TextFormatting.GREEN :
                                    bp > 0.2f ? TextFormatting.YELLOW : TextFormatting.RED;
                            info = bc + String.format("%.0f%%", bp * 100f);
                        }
                        break;
                    }
                }
                tooltip.add(TextFormatting.GRAY + "电池: " + (hasBattery ? info : TextFormatting.DARK_GRAY + "未检测到"));
            }

            tooltip.add("");
            tooltip.add(TextFormatting.GOLD + "基础升级:");
            for (UpgradeType type : UpgradeType.values()) {
                if (nbt.getBoolean("HasUpgrade_" + type.getKey()) || getUpgradeLevel(stack, type) > 0) {
                    int lv = getUpgradeLevel(stack, type);
                    boolean act = isUpgradeActive(stack, type.getKey());
                    TextFormatting sc = act ? TextFormatting.GREEN : (lv == 0 ? TextFormatting.YELLOW : TextFormatting.RED);
                    String status = act ? "✓" : (lv == 0 ? "⏸" : "✗");
                    tooltip.add(sc + status + " " + type.getColor() + type.getDisplayName() + " Lv." + lv);
                }
            }

            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "控制说明:");
            tooltip.add(TextFormatting.GRAY + "• 按 H 键打开控制面板");
            tooltip.add(TextFormatting.GRAY + "• 可调整升级等级（0=暂停）");
            tooltip.add(TextFormatting.GRAY + "• 可手动开关升级");

            tooltip.add("");
            tooltip.add(TextFormatting.DARK_RED + "⚠ 诅咒特性:");
            tooltip.add(TextFormatting.RED + "• 一旦装备永远无法摘下");
            tooltip.add(TextFormatting.RED + "• 死亡时不会掉落");
            tooltip.add(TextFormatting.YELLOW + "• 与 Enigmatic 系列模组物品互斥");

        } else {
            tooltip.add(TextFormatting.DARK_PURPLE + "使用 Redstone Flux 驱动的机械核心");
            if (installed > 0) {
                int paused = 0, disabled = 0;
                for (UpgradeType type : UpgradeType.values()) {
                    if (nbt.getBoolean("HasUpgrade_" + type.getKey()) || getUpgradeLevel(stack, type) > 0) {
                        if (getUpgradeLevel(stack, type) == 0) paused++;
                        else if (nbt.getBoolean("Disabled_" + type.getKey())) disabled++;
                    }
                }
                if (paused > 0) tooltip.add(TextFormatting.YELLOW + "⏸ " + paused + " 个升级暂停中");
                if (disabled > 0) tooltip.add(TextFormatting.RED + "✗ " + disabled + " 个升级已禁用");
            }
            tooltip.add("");
            tooltip.add(TextFormatting.GRAY + "按住 Shift 查看详情");
        }
    }

    // ===== 辅助 & 统计方法 =====

    private void handleInsufficientEnergy(ItemStack stack, EntityPlayer player, int requiredEnergy) {
        EnergyDepletionManager.EnergyStatus status = EnergyDepletionManager.getCurrentEnergyStatus(stack);
        boolean hasGen = isEnergyGeneratorActive(stack, "KINETIC_GENERATOR") ||
                isEnergyGeneratorActive(stack, "SOLAR_GENERATOR")   ||
                isEnergyGeneratorActive(stack, "VOID_ENERGY")       ||
                isEnergyGeneratorActive(stack, "COMBAT_CHARGER");

        boolean hasBatteryCharging = false;
        ItemStack battery = findChargedBattery(player);
        if (!battery.isEmpty()) {
            if (battery.getItem() instanceof ItemCreativeBatteryBauble) {
                hasBatteryCharging = true;
            } else {
                IEnergyStorage batteryEnergy = battery.getCapability(CapabilityEnergy.ENERGY, null);
                hasBatteryCharging = batteryEnergy != null && batteryEnergy.getEnergyStored() > 0;
            }
        }

        switch (status) {
            case POWER_SAVING:
                if (player.world.getTotalWorldTime() % 100 == 0)
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.YELLOW + "⚡ 省电模式 - 部分功能性能降低" +
                                    (hasGen ? TextFormatting.GREEN + " [充电中]" : "") +
                                    (hasBatteryCharging ? TextFormatting.AQUA + " [电池供电]" : "")
                    ), true);
                break;
            case EMERGENCY:
                if (player.world.getTotalWorldTime() % 80 == 0)
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.RED + "⚠ 紧急模式 - 高耗能功能已关闭" +
                                    (hasGen ? TextFormatting.YELLOW + " [缓慢充电]" : "") +
                                    (hasBatteryCharging ? TextFormatting.YELLOW + " [电池供电]" : "")
                    ), true);
                break;
            case CRITICAL:
                if (player.world.getTotalWorldTime() % 60 == 0)
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.DARK_RED + "💀 生命支持模式" +
                                    (hasGen || hasBatteryCharging ? TextFormatting.YELLOW + " - 能源模块工作中" : " - 立即充能！")
                    ), true);
                break;
        }
    }

    private void displayEnergyStatusToPlayer(EntityPlayer player, ItemStack stack) {
        EnergyDepletionManager.EnergyStatus status = EnergyDepletionManager.getCurrentEnergyStatus(stack);
        if (status != EnergyDepletionManager.EnergyStatus.NORMAL) {
            IEnergyStorage energy = getEnergyStorage(stack);
            if (energy != null) {
                int pct = (int) ((float) energy.getEnergyStored() / Math.max(1, energy.getMaxEnergyStored()) * 100);
                player.sendStatusMessage(new TextComponentString(
                        status.color + status.icon + " " + status.displayName + " - " + pct + "%"
                ), true);
            }
        }
    }

    private String formatEnergy(int energy) {
        if (energy >= 1_000_000) return String.format("%.1fM", energy / 1_000_000.0);
        if (energy >= 1_000)     return String.format("%.1fk", energy / 1_000.0);
        return String.valueOf(energy);
    }

    public static String getEnergyThresholdStatus(ItemStack stack) {
        IEnergyStorage e = getEnergyStorage(stack);
        if (e == null) return "UNKNOWN";
        float p = (float) e.getEnergyStored() / Math.max(1, e.getMaxEnergyStored());
        if (p <= EnergyBalanceConfig.EnergyThresholds.SHUTDOWN)   return "SHUTDOWN";
        else if (p <= EnergyBalanceConfig.EnergyThresholds.CRITICAL) return "CRITICAL";
        else if (p <= EnergyBalanceConfig.EnergyThresholds.EMERGENCY) return "EMERGENCY";
        else if (p <= EnergyBalanceConfig.EnergyThresholds.POWER_SAVING) return "POWER_SAVING";
        else return "NORMAL";
    }

    // ===== NBT & 升级访问 =====

    private static NBTTagCompound getOrCreateNBT(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }

    public static int getUpgradeLevel(ItemStack stack, UpgradeType type) {
        return getUpgradeLevelDirect(stack, type.getKey());
    }

    public static int getUpgradeLevel(ItemStack stack, String upgradeId) {
        return getUpgradeLevelDirect(stack, upgradeId);
    }

    public static void setUpgradeLevel(ItemStack stack, UpgradeType type, int level) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        nbt.setInteger("upgrade_" + type.getKey(), level);
        if (level > 0) nbt.setBoolean("HasUpgrade_" + type.getKey(), true);
    }

    public static void setUpgradeLevel(ItemStack stack, String upgradeId, int level) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        nbt.setInteger("upgrade_" + upgradeId, level);
        if (level > 0) nbt.setBoolean("HasUpgrade_" + upgradeId, true);
    }

    public static int getTotalInstalledUpgrades(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;

        Set<String> installed = new HashSet<>();
        NBTTagCompound nbt = stack.getTagCompound();

        // a) 基础枚举
        for (UpgradeType type : UpgradeType.values()) {
            String id = type.getKey();
            if (nbt.getBoolean("HasUpgrade_" + id) || getUpgradeLevelDirect(stack, id) > 0) {
                installed.add(norm(id));
            }
        }

        // b) NBT 直接扫描
        for (String k : nbt.getKeySet()) {
            if (k.startsWith("upgrade_") && nbt.getInteger(k) > 0) {
                installed.add(norm(k.substring("upgrade_".length())));
            } else if (k.startsWith("HasUpgrade_") && nbt.getBoolean(k)) {
                installed.add(norm(k.substring("HasUpgrade_".length())));
            }
        }

        // c) 扩展来源
        boolean gotExtIds = false;
        try {
            java.util.List<String> extIds = ItemMechanicalCoreExtended.getInstalledUpgradeIds(stack);
            if (extIds != null) {
                for (String id : extIds) installed.add(norm(id));
                gotExtIds = true;
            }
        } catch (Throwable ignored) {}

        if (!gotExtIds) {
            for (String id : EXTENDED_UPGRADE_IDS) {
                int lv = 0;
                try { lv = ItemMechanicalCoreExtended.getUpgradeLevel(stack, id); } catch (Throwable ignored) {}
                if (lv > 0 || (nbt.getBoolean("HasUpgrade_" + id))) {
                    installed.add(norm(id));
                }
            }
        }

        return installed.size();
    }

    // ===== 安全的等级设置方法 =====

    public static void setUpgradeLevelSafe(ItemStack stack, String upgradeId, int newLevel, boolean isManualOperation) {
        if (stack == null || stack.isEmpty()) return;
        NBTTagCompound nbt = getOrCreateNBT(stack);

        String normalizedId = upgradeId.toUpperCase();
        int currentOwnedMax = nbt.getInteger("OwnedMax_" + normalizedId);
        int currentLevel = nbt.getInteger("upgrade_" + normalizedId);

        if (isManualOperation) {
            if (currentOwnedMax <= 0) {
                int initialMax = Math.max(Math.max(currentLevel, newLevel), 1);
                nbt.setInteger("OwnedMax_" + normalizedId, initialMax);
                nbt.setInteger("OwnedMax_" + upgradeId, initialMax);
                currentOwnedMax = initialMax;
            }

            if (newLevel > currentOwnedMax) {
                nbt.setInteger("OwnedMax_" + normalizedId, newLevel);
                nbt.setInteger("OwnedMax_" + upgradeId, newLevel);
            }

            if (newLevel == 0) {
                nbt.setBoolean("IsPaused_" + normalizedId, true);
                nbt.setBoolean("IsPaused_" + upgradeId, true);
            } else {
                nbt.removeTag("IsPaused_" + normalizedId);
                nbt.removeTag("IsPaused_" + upgradeId);
            }
        }

        String[] variants = {upgradeId, normalizedId, upgradeId.toLowerCase()};
        for (String variant : variants) {
            nbt.setInteger("upgrade_" + variant, newLevel);
            if (newLevel > 0) {
                nbt.setBoolean("HasUpgrade_" + variant, true);
            }
        }
    }

    public static int getSafeOwnedMax(ItemStack stack, String upgradeId) {
        if (stack == null || stack.isEmpty()) return 0;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return 0;

        String[] variants = {upgradeId, upgradeId.toUpperCase(), upgradeId.toLowerCase()};
        int max = 0;

        for (String variant : variants) {
            max = Math.max(max, nbt.getInteger("OwnedMax_" + variant));
        }

        if (max <= 0) {
            max = getUpgradeLevel(stack, upgradeId);
            if (max > 0) {
                for (String variant : variants) {
                    nbt.setInteger("OwnedMax_" + variant, max);
                }
            }
        }

        return max;
    }

    public static boolean isUpgradePaused(ItemStack stack, String upgradeId) {
        if (stack == null || stack.isEmpty()) return false;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return false;

        String[] variants = {upgradeId, upgradeId.toUpperCase(), upgradeId.toLowerCase()};
        for (String variant : variants) {
            if (nbt.getBoolean("IsPaused_" + variant)) return true;
        }
        return false;
    }

    public static int getTotalActiveUpgradeLevel(ItemStack stack) {
        if (isCheckingUpgrade.get()) return 0;
        try {
            isCheckingUpgrade.set(true);
            int total = 0;
            Set<String> seen = new HashSet<>();

            for (UpgradeType type : UpgradeType.values()) {
                String id = type.getKey();
                if (seen.add(norm(id))) total += getEffectiveUpgradeLevel(stack, id);
            }
            for (String id : EXTENDED_UPGRADE_IDS) {
                if (seen.add(norm(id))) total += getEffectiveUpgradeLevel(stack, id);
            }
            return total;
        } finally {
            isCheckingUpgrade.set(false);
        }
    }

    public static int getTotalUpgradeLevel(ItemStack stack) {
        int total = 0;
        for (UpgradeType type : UpgradeType.values()) total += getUpgradeLevel(stack, type);
        try { total += ItemMechanicalCoreExtended.getTotalUpgradeLevel(stack); } catch (Throwable ignored) {}
        return total;
    }

    // ===== 兼容方法 =====

    @Deprecated
    public static boolean isUpgradeEnabled(ItemStack stack, String upgradeId) {
        return isUpgradeActive(stack, upgradeId);
    }

    public static EnergyDepletionManager.EnergyStatus getEnergyStatus(ItemStack stack) {
        return EnergyDepletionManager.getCurrentEnergyStatus(stack);
    }

    public static boolean consumeEnergyForUpgrade(ItemStack stack, String upgradeId, int baseAmount) {
        return consumeEnergyForUpgradeBalanced(stack, upgradeId, baseAmount);
    }

    public static double getEfficiencyMultiplier(int lv) {
        switch (lv) {
            case 0: return 1.00;
            case 1: return 0.85;
            case 2: return 0.70;
            case 3: return 0.55;
            case 4: return 0.40;
            case 5: return 0.25;
            default: return lv > 5 ? Math.max(0.10, 0.25 - (lv - 5) * 0.05) : 1.00;
        }
    }

    public static int getEfficiencyPercentage(ItemStack stack) {
        int lv = getEffectiveUpgradeLevel(stack, "energy_efficiency");
        double mul = getEfficiencyMultiplier(lv);
        return (int) ((1.0 - mul) * 100);
    }

    private static void recordEnergySaved(ItemStack stack, int saved) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        nbt.setLong("TotalEnergySaved", nbt.getLong("TotalEnergySaved") + saved);
        nbt.setInteger("SessionEnergySaved", nbt.getInteger("SessionEnergySaved") + saved);
    }

    public static long getTotalEnergySaved(ItemStack stack) {
        return !stack.hasTagCompound() ? 0 : stack.getTagCompound().getLong("TotalEnergySaved");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack core = player.getHeldItem(hand);
        if (!world.isRemote) {
            ItemStack equipped = findEquippedMechanicalCore(player);
            if (!isMechanicalCore(equipped)) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "请先将机械核心装备到头部饰品栏！"
                ));
            } else {
                EnergyDepletionManager.displayDetailedEnergyStatus(player, equipped);
            }
        }
        return new ActionResult<>(EnumActionResult.PASS, core);
    }

    public static ItemStack findEquippedMechanicalCore(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack s = baubles.getStackInSlot(i);
                    if (isMechanicalCore(s) && s.getItem() instanceof IBauble &&
                            ((IBauble) s.getItem()).getBaubleType(s) == BaubleType.HEAD) {
                        return s;
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[moremod] 查找装备的机械核心时出错: " + t.getMessage());
        }
        return ItemStack.EMPTY;
    }

    public static IEnergyStorage getEnergyStorage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return stack.getCapability(CapabilityEnergy.ENERGY, null);
    }

    public static void addEnergy(ItemStack stack, int amount) {
        IEnergyStorage e = getEnergyStorage(stack);
        if (e != null) e.receiveEnergy(amount, false);
    }

    public static boolean isMechanicalCore(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemMechanicalCore;
    }

    public static ItemStack getCoreFromPlayer(EntityPlayer player) {
        return findEquippedMechanicalCore(player);
    }

    // 速度模式
    public static SpeedMode getSpeedMode(ItemStack stack) {
        if (!stack.hasTagCompound()) return SpeedMode.NORMAL;
        int mode = stack.getTagCompound().getInteger("CoreSpeedMode");
        return SpeedMode.values()[Math.min(Math.max(0, mode), SpeedMode.values().length - 1)];
    }

    public static void setSpeedMode(ItemStack stack, SpeedMode mode) {
        getOrCreateNBT(stack).setInteger("CoreSpeedMode", mode.ordinal());
    }

    public static void cycleSpeedMode(ItemStack stack) {
        SpeedMode cur = getSpeedMode(stack);
        SpeedMode next = SpeedMode.values()[(cur.ordinal() + 1) % SpeedMode.values().length];
        setSpeedMode(stack, next);
    }

    // ===== canEquip 方法 =====

    // ItemMechanicalCore.java - 只修改 canEquip 方法

    /**
     * ✅ 修复：检查装备条件
     */
    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return false;

        EntityPlayer player = (EntityPlayer) entity;

        // ✅ 只在服务端做检查，客户端直接通过
        if (player.world.isRemote) {
            return true;
        }

        // ========== 服务端检查 ==========

        // 检查佩戴时间限制
        if (EquipmentTimeConfig.restriction.enabled) {
            if (EquipmentTimeTracker.isPermanentlyBanned(player)) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "✗ 你已被永久禁止佩戴机械核心！"
                ));
                return false;
            }

            long remainingTime = EquipmentTimeTracker.getRemainingTime(player);

            if (remainingTime == 0 && !EquipmentTimeTracker.hasEquippedInTime(player)) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "✗ 时间已到！你已无法佩戴机械核心！"
                ));
                EquipmentTimeTracker.markAsBanned(player);
                return false;
            }

            if (remainingTime > 0) {
                String timeStr = formatRemainingTime((int) remainingTime);
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "⏰ 提醒：佩戴时间还剩 " +
                                TextFormatting.AQUA + timeStr
                ));
            }
        }

        return true;
    }

    // 辅助方法
    private String formatRemainingTime(int seconds) {
        if (seconds >= 3600) {
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
        } else if (seconds >= 60) {
            int minutes = seconds / 60;
            int secs = seconds % 60;
            return minutes + "分钟" + (secs > 0 ? secs + "秒" : "");
        } else {
            return seconds + "秒";
        }
    }

    /**
     * 辅助方法：格式化剩余时间
     */


    @Override
    public boolean willAutoSync(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            EntityPlayer ep = (EntityPlayer) player;
            return ep.isCreative() || ep.isSpectator();
        }
        return false;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getTotalActiveUpgradeLevel(stack) > 0;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (isMechanicalCore(oldStack) && isMechanicalCore(newStack)) return false;
        return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage e = getEnergyStorage(stack);
        if (e == null || e.getMaxEnergyStored() == 0) return 1.0;
        return 1.0 - ((double) e.getEnergyStored() / e.getMaxEnergyStored());
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage e = getEnergyStorage(stack);
        if (e != null) {
            float r = (float) e.getEnergyStored() / Math.max(1, e.getMaxEnergyStored());
            NBTTagCompound nbt = getOrCreateNBT(stack);
            if (nbt.getBoolean("EmergencyMode")) return 0x8B0000;
            if (r > 0.6f) return 0x00FF00;
            else if (r > 0.3f) return 0xFFFF00;
            else return 0xFF0000;
        }
        return 0x696969;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity, int itemSlot, boolean isSelected) {
        if (!world.isRemote && world.getTotalWorldTime() % 100 == 0 && entity instanceof EntityPlayer) {
            IEnergyStorage e = getEnergyStorage(stack);
            if (e != null && e.getEnergyStored() == 0 && getTotalActiveUpgradeLevel(stack) > 0) {
                ((EntityPlayer) entity).sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚡ 机械核心能量耗尽！"
                ), true);
            }
        }
        super.onUpdate(stack, world, entity, itemSlot, isSelected);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new MechanicalCoreEnergyProvider(stack);
    }

    public static void clearPlayerCache(EntityPlayer player) {
        if (player != null) {
            batteryCache.remove(player.getUniqueID());
            try { EnergyUpgradeManager.resetPlayerData(player); } catch (Throwable ignored) {}
        }
    }

    // ===== 增强的冲突检测器 =====
    public static class ConflictChecker {
        private static final Map<UUID, Long> lastCheckTime = new WeakHashMap<>();

        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (event.player.world.isRemote) return;
            if (event.player.world.getTotalWorldTime() % 10 != 0) return;

            EntityPlayer player = event.player;
            UUID uuid = player.getUniqueID();
            long currentTime = System.currentTimeMillis();

            Long lastTime = lastCheckTime.get(uuid);
            if (lastTime != null && currentTime - lastTime < 1000) return;
            lastCheckTime.put(uuid, currentTime);

            try {
                IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
                if (baubles == null) return;

                boolean hasMechanicalCore = false;
                List<Integer> enigmaticSlots = new ArrayList<>();
                List<String> enigmaticNames = new ArrayList<>();

                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack bauble = baubles.getStackInSlot(i);

                    if (isMechanicalCore(bauble)) {
                        hasMechanicalCore = true;
                    }

                    // ✨ 修改：只检测被阻止的 Enigmatic 物品（lost_engine 不会被移除）
                    if (BLOCK_ALL_ENIGMATIC && isBlockedEnigmaticItem(bauble)) {
                        enigmaticSlots.add(i);
                        enigmaticNames.add(getItemDisplayName(bauble));
                    }
                }

                if (hasMechanicalCore && !enigmaticSlots.isEmpty()) {
                    for (int i = 0; i < enigmaticSlots.size(); i++) {
                        int slot = enigmaticSlots.get(i);
                        ItemStack removed = baubles.extractItem(slot, 1, false);

                        if (!removed.isEmpty()) {
                            if (!player.inventory.addItemStackToInventory(removed)) {
                                player.dropItem(removed, false);
                            }

                            String itemName = enigmaticNames.get(i);
                            player.sendMessage(new TextComponentString(
                                    TextFormatting.YELLOW + "⚠ " + itemName +
                                            " 与机械核心不兼容，已自动卸下"
                            ));
                            player.sendMessage(new TextComponentString(
                                    TextFormatting.GRAY + "（Lost Engine 可以同时装备）"
                            ));
                        }
                    }
                }

            } catch (Throwable t) {
                if (DEBUG_MODE) {
                    System.err.println("[MechanicalCore] 冲突检测时出错: " + t.getMessage());
                }
            }
        }
    }

    // ===== 能量 Provider/Storage =====

    private static class MechanicalCoreEnergyProvider implements ICapabilityProvider {
        private final MechanicalCoreEnergyStorage storage;
        MechanicalCoreEnergyProvider(ItemStack stack) {
            this.storage = new MechanicalCoreEnergyStorage(stack);
        }
        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY;
        }
        @Override
        @Nullable
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(storage) : null;
        }
    }

    private static class MechanicalCoreEnergyStorage implements IEnergyStorage {
        private static final String NBT_ENERGY = "Energy";
        private final ItemStack container;

        MechanicalCoreEnergyStorage(ItemStack stack) {
            this.container = stack;
            initNBT();
        }

        private void initNBT() {
            if (!container.hasTagCompound()) container.setTagCompound(new NBTTagCompound());
            if (!container.getTagCompound().hasKey(NBT_ENERGY))
                container.getTagCompound().setInteger(NBT_ENERGY, 0);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energy = getEnergyStored();
            int maxEnergy = getMaxEnergyStored();
            int received = Math.min(maxEnergy - energy, Math.min(maxReceive, EnergyBalanceConfig.BASE_ENERGY_TRANSFER));
            if (!simulate && received > 0) setEnergy(energy + received);
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energy = getEnergyStored();
            int extracted = Math.min(energy, Math.min(maxExtract, EnergyBalanceConfig.BASE_ENERGY_TRANSFER));
            if (!simulate && extracted > 0) setEnergy(energy - extracted);
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return container.hasTagCompound() ? container.getTagCompound().getInteger(NBT_ENERGY) : 0;
        }

        @Override
        public int getMaxEnergyStored() {
            if (isCalculatingEnergy.get()) return EnergyBalanceConfig.BASE_ENERGY_CAPACITY;
            try {
                isCalculatingEnergy.set(true);
                int capacityLevel = 0;
                if (container.hasTagCompound()) {
                    NBTTagCompound nbt = container.getTagCompound();
                    if (!nbt.getBoolean("Disabled_energy_capacity") && !nbt.getBoolean("IsPaused_energy_capacity")) {
                        capacityLevel = nbt.getInteger("upgrade_energy_capacity");
                        if (capacityLevel < 0) capacityLevel = 0;
                    }
                }
                return EnergyBalanceConfig.BASE_ENERGY_CAPACITY + capacityLevel * EnergyBalanceConfig.ENERGY_PER_CAPACITY_LEVEL;
            } finally {
                isCalculatingEnergy.set(false);
            }
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        private void setEnergy(int energy) {
            getOrCreateNBT(container).setInteger(NBT_ENERGY, Math.max(0, Math.min(getMaxEnergyStored(), energy)));
        }
    }// 加个清理机制防止内存泄漏
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        batteryCache.remove(event.player.getUniqueID());
    }
}

