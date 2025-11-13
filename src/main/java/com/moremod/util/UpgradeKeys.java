package com.moremod.util;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 统一并规范化 升级ID 与 NBT 键 的工具类。
 *
 * 约定：
 * - 规范化ID（cid）：全大写 + 下划线，如 "ENERGY_EFFICIENCY"、"FLIGHT_MODULE"
 * - upgrade_ 存储键：使用小写（upgrade_energy_efficiency）
 * - 状态/标志键：HasUpgrade_/Disabled_/OwnedMax_/LastLevel_/IsPaused_/UpgradeLock_/Destroyed_ + 规范化ID（大写）
 *
 * 这样可读性强、避免重复大小写键，且不破坏旧代码（我们在读写时兼容大小写与旧别名）。
 */
public final class UpgradeKeys {

    private UpgradeKeys() {}

    /** 一些已知ID的同义写法（可按需增补） */
    private static final Set<String> WATERPROOF_IDS = new HashSet<>(Arrays.asList(
            "WATERPROOF_MODULE", "WATERPROOF"
    ));
    private static final Set<String> SHIELD_IDS = new HashSet<>(Arrays.asList(
            "YELLOW_SHIELD", "SHIELD_GENERATOR"
    ));

    /** 发电模块列表（在自毁时受保护） */
    private static final Set<String> GENERATOR_IDS = new HashSet<>(Arrays.asList(
            "SOLAR_GENERATOR", "KINETIC_GENERATOR", "THERMAL_GENERATOR"
    ));

    /** 将任意id规范化为 全大写 + 下划线 */
    public static String canon(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";
        s = s.replace('-', '_').replace(' ', '_');
        s = s.replace("__", "_");
        return s.toUpperCase(Locale.ROOT);
    }

    /** 是否属于防水类（用于统一显示/联动写入等） */
    public static boolean isWaterproof(String idOrCid) {
        String cid = canon(idOrCid);
        return WATERPROOF_IDS.contains(cid) || cid.contains("WATERPROOF");
    }

    /** 是否属于护盾类（YELLOW_SHIELD 与 SHIELD_GENERATOR 视为一类） */
    public static boolean isShield(String idOrCid) {
        String cid = canon(idOrCid);
        return SHIELD_IDS.contains(cid);
    }

    /** 是否属于发电模块（自毁时保护） */
    public static boolean isGenerator(String idOrCid) {
        String cid = canon(idOrCid);
        return GENERATOR_IDS.contains(cid) || cid.contains("GENERATOR");
    }

    // ===== NBT键名定义 =====

    /** upgrade_ 小写键（数值等级存这里） */
    public static String kUpgrade(String cid) {
        return "upgrade_" + canon(cid).toLowerCase(Locale.ROOT);
    }

    /** 拥有标记（大写ID） */
    public static String kHasUpgrade(String cid) { return "HasUpgrade_" + canon(cid); }

    /** 手动禁用标记（大写ID） */
    public static String kDisabled(String cid)   { return "Disabled_" + canon(cid); }

    /** 记录最大拥有等级（大写ID） */
    public static String kOwnedMax(String cid)   { return "OwnedMax_" + canon(cid); }

    /** 原始最大等级（历史最高值，用于修复系统）（大写ID） */
    public static String kOriginalMax(String cid) { return "OriginalMax_" + canon(cid); }

    /** 暂停前的等级（大写ID） */
    public static String kLastLevel(String cid)  { return "LastLevel_" + canon(cid); }

    /** 是否被暂停（大写ID） */
    public static String kPaused(String cid)     { return "IsPaused_" + canon(cid); }

    /** 惩罚写入的"损坏/锁"标记（大写ID） */
    public static String kLock(String cid)       { return "UpgradeLock_" + canon(cid); }

    /** 模块破坏标记（大写ID） - 与GUI集成 */
    public static String kDestroyed(String cid)  { return "Destroyed_" + canon(cid); }

    /** 破坏时间戳（大写ID） */
    public static String kDestroyTime(String cid) { return "DestroyTime_" + canon(cid); }

    // ===== 修复/惩罚系统键名 =====

    /** 是否被惩罚过（大写ID） */
    public static String kWasPunished(String cid) { return "WasPunished_" + canon(cid); }

    /** 损坏次数（当前周期）（大写ID） */
    public static String kDamageCount(String cid) { return "DamageCount_" + canon(cid); }

    /** 总损坏次数（累计）（大写ID） */
    public static String kTotalDamageCount(String cid) { return "TotalDamageCount_" + canon(cid); }

    /** 惩罚上限等级（大写ID） */
    public static String kPenaltyCap(String cid) { return "PenaltyCap_" + canon(cid); }

    /** 惩罚过期时间戳（大写ID） */
    public static String kPenaltyExpire(String cid) { return "PenaltyExpire_" + canon(cid); }

    /** 惩罚层级（大写ID） */
    public static String kPenaltyTier(String cid) { return "PenaltyTier_" + canon(cid); }

    /** 惩罚能量债务（FE）（大写ID） */
    public static String kPenaltyDebtFE(String cid) { return "PenaltyDebtFE_" + canon(cid); }

    /** 惩罚经验债务（XP）（大写ID） */
    public static String kPenaltyDebtXP(String cid) { return "PenaltyDebtXP_" + canon(cid); }

    // ===== 能量耗尽惩罚系统的时间戳键（全局） =====

    /** 上次DoT伤害时间 */
    public static final String K_LAST_DOT = "Punish_LastDot";

    /** 上次降级时间 */
    public static final String K_LAST_DEGRADE = "Punish_LastDegrade";

    /** 上次耐久损耗时间 */
    public static final String K_LAST_DURABILITY = "Punish_LastDur";

    /** 进入临界状态的时间戳 */
    public static final String K_CRITICAL_SINCE = "Punish_CriticalSince";

    /** 10秒警告已触发标记 */
    public static final String K_WARNING_10S = "Punish_Warning10s";

    /** 5秒警告已触发标记 */
    public static final String K_WARNING_5S = "Punish_Warning5s";

    /** 自毁已执行标记 */
    public static final String K_SELF_DESTRUCT_DONE = "Punish_SelfDestruct";

    // ===== 能量状态标记（全局） =====

    /** 省电模式标记 */
    public static final String K_POWER_SAVING_MODE = "PowerSavingMode";

    /** 紧急模式标记 */
    public static final String K_EMERGENCY_MODE = "EmergencyMode";

    /** 临界模式标记 */
    public static final String K_CRITICAL_MODE = "CriticalMode";

    /** 核心自毁标记 */
    public static final String K_CORE_DESTROYED = "CoreDestroyed";

    /** 上一次能量状态 */
    public static final String K_PREVIOUS_ENERGY_STATUS = "PreviousEnergyStatus";

    // ===== 读取方法 =====

    /** 读取升级等级：兼容大小写/旧键 */
    public static int getLevel(ItemStack stack, String id) {
        if (stack == null || stack.isEmpty()) return 0;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return 0;

        String cid = canon(id);

        // 如果被破坏，返回0
        if (isDestroyed(stack, id)) return 0;

        int v = nbt.getInteger(kUpgrade(cid));           // 规范键
        if (v > 0) return v;

        // 兼容历史写法：upgrade_ID / upgrade_id
        v = nbt.getInteger("upgrade_" + cid);            // 大写尾
        if (v > 0) return v;
        v = nbt.getInteger("upgrade_" + cid.toLowerCase(Locale.ROOT)); // 小写尾
        if (v > 0) return v;
        v = nbt.getInteger("upgrade_" + id);             // 原始ID
        if (v > 0) return v;

        return 0;
    }

    /** 获取拥有的最大等级 */
    public static int getOwnedMax(ItemStack stack, String id) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return 0;
        String cid = canon(id);
        return nbt.getInteger(kOwnedMax(cid));
    }

    /** 获取暂停前的等级 */
    public static int getLastLevel(ItemStack stack, String id) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return 0;
        String cid = canon(id);
        return nbt.getInteger(kLastLevel(cid));
    }

    /** 获取原始最大等级（历史最高值） */
    public static int getOriginalMax(ItemStack stack, String id) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return 0;
        String cid = canon(id);
        return nbt.getInteger(kOriginalMax(cid));
    }

    /** 获取损坏次数 */
    public static int getDamageCount(ItemStack stack, String id) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return 0;
        String cid = canon(id);
        return nbt.getInteger(kDamageCount(cid));
    }

    /** 获取总损坏次数（累计） */
    public static int getTotalDamageCount(ItemStack stack, String id) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return 0;
        String cid = canon(id);
        return nbt.getInteger(kTotalDamageCount(cid));
    }

    /** 检查是否被惩罚过 */
    public static boolean wasPunished(ItemStack stack, String id) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return false;
        String cid = canon(id);
        return nbt.getBoolean(kWasPunished(cid));
    }

    // ===== 写入方法 =====

    /** 写入等级（只写规范键） */
    public static void setLevel(ItemStack stack, String id, int level) {
        if (stack == null || stack.isEmpty()) return;
        NBTTagCompound nbt = getOrCreate(stack);
        String cid = canon(id);
        nbt.setInteger(kUpgrade(cid), Math.max(0, level));
    }

    /** 标准化：设置拥有、取消暂停、更新 OwnedMax、清空 LastLevel */
    public static void markOwnedActive(ItemStack stack, String id, int newLevel) {
        NBTTagCompound nbt = getOrCreate(stack);
        String cid = canon(id);
        nbt.setBoolean(kHasUpgrade(cid), true);
        nbt.setBoolean(kPaused(cid), false);
        if (newLevel > 0) {
            int owned = nbt.getInteger(kOwnedMax(cid));
            if (newLevel > owned) nbt.setInteger(kOwnedMax(cid), newLevel);
            if (nbt.hasKey(kLastLevel(cid))) nbt.removeTag(kLastLevel(cid));
        }
    }

    /** 标准化暂停：把当前等级记到 LastLevel，并把等级置0、置 Paused=true */
    public static void pause(ItemStack stack, String id, int oldLevel) {
        NBTTagCompound nbt = getOrCreate(stack);
        String cid = canon(id);
        if (oldLevel > 0) {
            nbt.setInteger(kLastLevel(cid), oldLevel);
            int owned = nbt.getInteger(kOwnedMax(cid));
            if (oldLevel > owned) nbt.setInteger(kOwnedMax(cid), oldLevel);
        }
        nbt.setInteger(kUpgrade(cid), 0);
        nbt.setBoolean(kPaused(cid), true);
        nbt.setBoolean(kHasUpgrade(cid), true);
    }

    // ===== 修复/惩罚系统写入方法 =====

    /** 设置原始最大等级（只在更高时更新） */
    public static void setOriginalMax(ItemStack stack, String id, int maxLevel) {
        NBTTagCompound nbt = getOrCreate(stack);
        String cid = canon(id);
        int current = nbt.getInteger(kOriginalMax(cid));
        if (maxLevel > current) {
            nbt.setInteger(kOriginalMax(cid), maxLevel);
        }
    }

    /** 设置拥有最大等级 */
    public static void setOwnedMax(ItemStack stack, String id, int maxLevel) {
        NBTTagCompound nbt = getOrCreate(stack);
        String cid = canon(id);
        nbt.setInteger(kOwnedMax(cid), maxLevel);
    }

    /** 标记为被惩罚过 */
    public static void markWasPunished(ItemStack stack, String id, boolean punished) {
        NBTTagCompound nbt = getOrCreate(stack);
        String cid = canon(id);
        nbt.setBoolean(kWasPunished(cid), punished);
    }

    /** 增加损坏次数 */
    public static void incrementDamageCount(ItemStack stack, String id) {
        NBTTagCompound nbt = getOrCreate(stack);
        String cid = canon(id);
        int current = nbt.getInteger(kDamageCount(cid));
        nbt.setInteger(kDamageCount(cid), current + 1);

        // 同时更新总损坏次数
        int total = nbt.getInteger(kTotalDamageCount(cid));
        nbt.setInteger(kTotalDamageCount(cid), total + 1);
    }

    /** 重置损坏次数（当前周期） */
    public static void resetDamageCount(ItemStack stack, String id) {
        NBTTagCompound nbt = getOrCreate(stack);
        String cid = canon(id);
        nbt.setInteger(kDamageCount(cid), 0);
    }

    // ===== 锁定/解锁方法 =====

    /** 设置锁定状态 */
    public static void setLocked(ItemStack stack, String id, boolean locked) {
        NBTTagCompound nbt = getOrCreate(stack);
        String cid = canon(id);
        nbt.setBoolean(kLock(cid), locked);

        // 兼容性：同时写入大小写变体
        nbt.setBoolean("UpgradeLock_" + cid.toLowerCase(), locked);
        nbt.setBoolean("Locked_" + cid, locked);
    }

    /** 解除锁定（惩罚写入的升级锁） */
    public static boolean unlock(ItemStack stack, String id) {
        NBTTagCompound nbt = getOrCreate(stack);
        String cid = canon(id);

        System.out.println("[DEBUG] UpgradeKeys.unlock() 调用: " + id + " -> " + cid);

        boolean wasLocked = isLocked(stack, id);
        boolean wasDestroyed = isDestroyed(stack, id);
        boolean wasLockedOrDestroyed = wasLocked || wasDestroyed;

        System.out.println("[DEBUG] 状态检查: locked=" + wasLocked + ", destroyed=" + wasDestroyed);

        if (!wasLockedOrDestroyed) {
            System.out.println("[DEBUG] 模块既没有被锁定也没有被破坏，跳过解锁");
            return false;
        }

        // 清除所有可能的标记变体（更彻底）
        String[] baseIds = {id, cid, id.toLowerCase(), id.toUpperCase(),
                cid.toLowerCase(), cid.toUpperCase()};

        for (String baseId : baseIds) {
            // 清除锁定标记
            String[] lockKeys = {
                    "UpgradeLock_" + baseId,
                    "Locked_" + baseId,
                    "HardLocked_" + baseId,
                    kLock(baseId)
            };

            for (String key : lockKeys) {
                if (nbt.hasKey(key)) {
                    System.out.println("[DEBUG] 清除锁: " + key);
                    nbt.removeTag(key);
                }
            }

            // 清除破坏标记
            String[] destroyKeys = {
                    "Destroyed_" + baseId,
                    kDestroyed(baseId),
                    "DestroyTime_" + baseId,
                    kDestroyTime(baseId)
            };

            for (String key : destroyKeys) {
                if (nbt.hasKey(key)) {
                    System.out.println("[DEBUG] 清除破坏: " + key);
                    nbt.removeTag(key);
                }
            }
        }

        // 清除暂停标记（如果有的话）
        for (String baseId : baseIds) {
            if (nbt.hasKey("IsPaused_" + baseId)) {
                System.out.println("[DEBUG] 清除暂停: IsPaused_" + baseId);
                nbt.removeTag("IsPaused_" + baseId);
            }
        }

        // 恢复等级
        int currentLevel = getLevel(stack, id);
        System.out.println("[DEBUG] 修复前等级: " + currentLevel);

        if (currentLevel == 0) {
            // 尝试恢复到之前的等级
            int restoreLevel = 0;

            // 优先从LastLevel恢复
            for (String baseId : baseIds) {
                int lastLevel = nbt.getInteger("LastLevel_" + baseId);
                if (lastLevel > restoreLevel) {
                    restoreLevel = lastLevel;
                    System.out.println("[DEBUG] 从LastLevel_" + baseId + "找到等级: " + lastLevel);
                }
            }

            // 其次从OwnedMax恢复
            if (restoreLevel == 0) {
                for (String baseId : baseIds) {
                    int ownedMax = nbt.getInteger("OwnedMax_" + baseId);
                    if (ownedMax > restoreLevel) {
                        restoreLevel = ownedMax;
                        System.out.println("[DEBUG] 从OwnedMax_" + baseId + "找到等级: " + ownedMax);
                    }
                }
            }

            // 默认恢复到1级
            if (restoreLevel == 0) {
                restoreLevel = 1;
                System.out.println("[DEBUG] 使用默认等级: 1");
            }

            System.out.println("[DEBUG] 恢复到等级: " + restoreLevel);

            // 写入等级到所有系统
            // 1. NBT直接写入（所有变体）
            for (String baseId : baseIds) {
                nbt.setInteger("upgrade_" + baseId, restoreLevel);
                nbt.setBoolean("HasUpgrade_" + baseId, true);
            }

            // 2. 使用setLevel方法
            setLevel(stack, id, restoreLevel);

            // 3. 标记为激活状态
            markOwnedActive(stack, id, restoreLevel);

            // 4. 同步到基础系统
            try {
                for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
                    if (type.getKey().equalsIgnoreCase(cid) || type.name().equalsIgnoreCase(cid)) {
                        ItemMechanicalCore.setUpgradeLevel(stack, type, restoreLevel);
                        System.out.println("[DEBUG] 同步到基础系统: " + type.name());
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("[DEBUG] 同步基础系统失败: " + e.getMessage());
            }

            // 5. 同步到扩展系统
            try {
                for (String baseId : baseIds) {
                    ItemMechanicalCoreExtended.setUpgradeLevel(stack, baseId, restoreLevel);
                }
                System.out.println("[DEBUG] 同步到扩展系统完成");
            } catch (Exception e) {
                System.err.println("[DEBUG] 同步扩展系统失败: " + e.getMessage());
            }
        }

        System.out.println("[DEBUG] 解锁完成，最终等级: " + getLevel(stack, id));
        return true;
    }

    /** 是否已上锁 */
    public static boolean isLocked(ItemStack stack, String id) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return false;
        String cid = canon(id);
        return nbt.getBoolean(kLock(cid)) ||
                nbt.getBoolean("Locked_" + cid) ||
                nbt.getBoolean("Locked_" + cid.toLowerCase());
    }

    // ===== 破坏/修复方法 =====

    /** 标记模块为破坏状态 */
    public static void markDestroyed(ItemStack stack, String id) {
        NBTTagCompound nbt = getOrCreate(stack);
        String cid = canon(id);

        System.out.println("[DEBUG] markDestroyed: " + id + " -> " + cid);

        // 设置破坏标记（多个变体以兼容GUI）
        String[] variants = {cid, cid.toLowerCase(), cid.toUpperCase(), id};
        for (String var : variants) {
            nbt.setBoolean("Destroyed_" + var, true);
            System.out.println("[DEBUG] 设置破坏标记: Destroyed_" + var);
        }

        // 记录破坏时间
        nbt.setLong("DestroyTime_" + cid, System.currentTimeMillis());

        // 同时设置锁定
        setLocked(stack, id, true);

        // 清零等级
        setLevel(stack, id, 0);
    }

    /** 清除破坏状态（修复） */
    public static void clearDestroyed(ItemStack stack, String id) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return;

        String cid = canon(id);

        // 清除所有破坏相关标记的变体
        String[] variants = {cid, cid.toLowerCase(), cid.toUpperCase(), id};
        for (String var : variants) {
            nbt.removeTag(kDestroyed(var));
            nbt.removeTag("Destroyed_" + var);
            nbt.removeTag(kDestroyTime(var));
            nbt.removeTag("DestroyTime_" + var);
        }
    }

    /** 检查是否被破坏 */
    public static boolean isDestroyed(ItemStack stack, String id) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return false;

        String cid = canon(id);

        // 检查特定模块破坏
        if (nbt.getBoolean(kDestroyed(cid)) ||
                nbt.getBoolean("Destroyed_" + cid.toLowerCase()) ||
                nbt.getBoolean("Destroyed_" + id)) {
            return true;
        }

        // 检查核心自毁（但发电模块除外）
        if (nbt.getBoolean(K_CORE_DESTROYED) && !isGenerator(id)) {
            return true;
        }

        return false;
    }

    // ===== 禁用/启用方法 =====

    /** 设置手动禁用状态 */
    public static void setDisabled(ItemStack stack, String id, boolean disabled) {
        NBTTagCompound nbt = getOrCreate(stack);
        String cid = canon(id);
        nbt.setBoolean(kDisabled(cid), disabled);

        // 如果是防水模块，同步所有别名
        if (isWaterproof(id)) {
            for (String wid : WATERPROOF_IDS) {
                nbt.setBoolean(kDisabled(wid), disabled);
            }
        }
    }

    /** 检查是否被手动禁用 */
    public static boolean isDisabled(ItemStack stack, String id) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return false;
        String cid = canon(id);
        return nbt.getBoolean(kDisabled(cid));
    }

    // ===== 暂停状态方法 =====

    /** 检查是否暂停 */
    public static boolean isPaused(ItemStack stack, String id) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return false;
        String cid = canon(id);
        return nbt.getBoolean(kPaused(cid));
    }

    // ===== 工具方法 =====

    /** 只要存在就返回，否则创建 */
    public static NBTTagCompound getOrCreate(ItemStack s) {
        if (!s.hasTagCompound()) s.setTagCompound(new NBTTagCompound());
        return s.getTagCompound();
    }

    /** 将"同类ID"（如 WATERPROOF_MODULE / WATERPROOF）折叠为一个规范cid */
    public static String foldAlias(String id) {
        String cid = canon(id);
        if (isWaterproof(cid)) return "WATERPROOF_MODULE";
        if (isShield(cid)) return "YELLOW_SHIELD"; // 以 YELLOW_SHIELD 为护盾规范id
        return cid;
    }

    /** 获取升级的状态（用于GUI显示） */
    public enum UpgradeStatus {
        ACTIVE,       // 正常运行
        PAUSED,       // 暂停（可恢复）
        DISABLED,     // 手动禁用
        DESTROYED,    // 已破坏（需修复）
        LOCKED,       // 已锁定
        NOT_OWNED     // 未拥有
    }

    /** 获取升级的完整状态 */
    public static UpgradeStatus getStatus(ItemStack stack, String id) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return UpgradeStatus.NOT_OWNED;

        String cid = canon(id);

        // 1. 检查是否被破坏
        if (isDestroyed(stack, id)) {
            return UpgradeStatus.DESTROYED;
        }

        // 2. 检查是否被锁定
        if (isLocked(stack, id)) {
            return UpgradeStatus.LOCKED;
        }

        // 3. 检查是否暂停
        if (isPaused(stack, id)) {
            return UpgradeStatus.PAUSED;
        }

        // 4. 检查是否被禁用
        if (isDisabled(stack, id)) {
            return UpgradeStatus.DISABLED;
        }

        // 5. 检查是否拥有
        int level = getLevel(stack, id);
        if (level > 0) {
            return UpgradeStatus.ACTIVE;
        }

        // 6. 检查是否曾经拥有
        if (getOwnedMax(stack, id) > 0 || nbt.getBoolean(kHasUpgrade(cid))) {
            return UpgradeStatus.PAUSED;
        }

        return UpgradeStatus.NOT_OWNED;
    }

    /** 清理所有惩罚相关标记（能量恢复后调用） */
    public static void clearAllPunishments(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return;

        // 清理所有Disabled_开头的键
        Set<String> keysToRemove = new HashSet<>();
        for (String key : nbt.getKeySet()) {
            if (key.startsWith("Disabled_")) {
                keysToRemove.add(key);
            }
        }
        for (String key : keysToRemove) {
            nbt.removeTag(key);
        }

        // 恢复效率倍率
        nbt.setFloat("EfficiencyMultiplier", 1.0f);

        // 清除能量状态标记
        nbt.removeTag("PowerSavingMode");
        nbt.removeTag("EmergencyMode");
        nbt.removeTag("CriticalMode");
    }
}