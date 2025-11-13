package com.moremod.item;

import java.util.*;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraft.util.text.TextFormatting;

/**
 * 机械核心的扩展方法（只管理“扩展升级”，基础升级交给主核心）
 * 特性：
 *  - 统一规范ID + 别名映射（去重统计）
 *  - 等级0视为暂停，支持 Disabled_/IsPaused_ GUI 键
 *  - 提供 getInstalledUpgradeIds 供主核心去重统计
 *  - 提供 getAllUpgrades / getAllUpgradeIds / getAliasesFor
 *  - 兼容保留 UpgradeCategory.BASIC（不在此注册基础升级）
 */
public class ItemMechanicalCoreExtended {

    /* ===================== 升级信息与注册 ===================== */

    /** 升级信息 */
    public static class UpgradeInfo {
        public final String id;                // 规范ID（全大写）
        public final String displayName;
        public final TextFormatting color;
        public final int maxLevel;
        public final UpgradeCategory category;

        public UpgradeInfo(String id, String displayName, TextFormatting color, int maxLevel, UpgradeCategory category) {
            this.id = id; this.displayName = displayName; this.color = color; this.maxLevel = maxLevel; this.category = category;
        }
    }

    /** 升级类别（仅用于展示/统计） */
    public enum UpgradeCategory {
        SURVIVAL("生存", TextFormatting.GREEN),
        AUXILIARY("辅助", TextFormatting.AQUA),
        COMBAT("战斗", TextFormatting.RED),
        ENERGY("能源", TextFormatting.YELLOW),
        BASIC("基础", TextFormatting.WHITE);   // 兼容占位：不在扩展层注册基础升级

        public final String name;
        public final TextFormatting color;
        UpgradeCategory(String name, TextFormatting color) { this.name = name; this.color = color; }
    }

    /** 规范化：去空白 + 全大写 */
    private static String norm(String id) { return id == null ? "" : id.trim().toUpperCase(java.util.Locale.ROOT); }

    /** 扩展升级注册表（只放“规范ID”） */
    private static final Map<String, UpgradeInfo> REGISTRY = new LinkedHashMap<>();

    /** 别名映射（别名 -> 规范ID） */
    private static final Map<String, String> ALIAS_TO_CANON = new HashMap<>();

    /** waterproof 的别名集合（便于 GUI 键联动与兼容旧存档） */
    private static final Set<String> WP_ALIASES = new HashSet<>(Arrays.asList(
            "WATERPROOF_MODULE","WATERPROOF","waterproof_module","waterproof"
    ));

    static {
        registerUpgrades();

    }

    /** 仅注册“扩展升级” —— 基础升级不要在这里登记，避免和主核心重复统计 */
    private static void registerUpgrades() {
        // ===== 生存类 =====
        register("YELLOW_SHIELD",   "黄条护盾",   TextFormatting.YELLOW,      3, UpgradeCategory.SURVIVAL);
        register("HEALTH_REGEN",    "纳米修复",   TextFormatting.RED,         3, UpgradeCategory.SURVIVAL);
        register("HUNGER_THIRST",   "代谢调节",   TextFormatting.GREEN,       3, UpgradeCategory.SURVIVAL);
        register("THORNS",          "反应装甲",   TextFormatting.DARK_RED,    3, UpgradeCategory.SURVIVAL);
        register("FIRE_EXTINGUISH", "自动灭火",   TextFormatting.BLUE,        3, UpgradeCategory.SURVIVAL);

        // ===== 辅助类 =====
        register("WATERPROOF_MODULE","防水模块",  TextFormatting.AQUA,        3, UpgradeCategory.AUXILIARY);
        // 防水别名（不作为独立升级，仅映射到规范ID）
        registerAlias("waterproof_module","WATERPROOF_MODULE");
        registerAlias("waterproof",       "WATERPROOF_MODULE");

        register("ORE_VISION",      "矿物透视",   TextFormatting.GOLD,        3, UpgradeCategory.AUXILIARY);
        register("MOVEMENT_SPEED",  "伺服电机",   TextFormatting.AQUA,        3, UpgradeCategory.AUXILIARY);
        register("STEALTH",         "光学迷彩",   TextFormatting.DARK_GRAY,   3, UpgradeCategory.AUXILIARY);
        register("EXP_AMPLIFIER",   "经验矩阵",   TextFormatting.GREEN,       3, UpgradeCategory.AUXILIARY);
        register("POISON_IMMUNITY", "毒免疫",     TextFormatting.DARK_GREEN,  1, UpgradeCategory.AUXILIARY);
        register("NIGHT_VISION",    "夜视",       TextFormatting.YELLOW,      1, UpgradeCategory.AUXILIARY);
        register("WATER_BREATHING", "水下呼吸",   TextFormatting.AQUA,        1, UpgradeCategory.AUXILIARY);
        register("ITEM_MAGNET",     "物品磁铁",   TextFormatting.LIGHT_PURPLE,3, UpgradeCategory.AUXILIARY);
        register("NEURAL_SYNCHRONIZER", "神经同步器", TextFormatting.AQUA, 1, UpgradeCategory.AUXILIARY); // 第86行

        // ===== 战斗类 =====
        register("DAMAGE_BOOST",    "力量增幅",   TextFormatting.DARK_RED,    5, UpgradeCategory.COMBAT);
        register("ATTACK_SPEED",    "反应增强",   TextFormatting.YELLOW,      3, UpgradeCategory.COMBAT);
        register("RANGE_EXTENSION", "范围拓展",   TextFormatting.BLUE,        3, UpgradeCategory.COMBAT);
        register("PURSUIT",         "追击系统",   TextFormatting.LIGHT_PURPLE,3, UpgradeCategory.COMBAT);
        register("CRITICAL_STRIKE", "暴击",       TextFormatting.GOLD,        3, UpgradeCategory.COMBAT);

        // ===== 能源类 =====
        register("KINETIC_GENERATOR","动能发电",  TextFormatting.GRAY,        3, UpgradeCategory.ENERGY);
        register("SOLAR_GENERATOR",  "太阳能板",  TextFormatting.YELLOW,      3, UpgradeCategory.ENERGY);
        register("VOID_ENERGY",      "虚空共振",  TextFormatting.DARK_PURPLE, 3, UpgradeCategory.ENERGY);
        register("COMBAT_CHARGER",   "战斗充能",  TextFormatting.RED,         3, UpgradeCategory.ENERGY);
    }

    private static void register(String id, String name, TextFormatting color, int max, UpgradeCategory cat) {
        String canon = norm(id);
        REGISTRY.put(canon, new UpgradeInfo(canon, name, color, max, cat));
    }
    private static void registerAlias(String alias, String canonicalId) {
        ALIAS_TO_CANON.put(norm(alias), norm(canonicalId));
    }

    /** 规范化到注册用的“规范ID”（若无别名映射，则返回大写本身） */
    private static String canonical(String id) {
        String n = norm(id);
        return ALIAS_TO_CANON.getOrDefault(n, n);
    }

    /* ===================== GUI 暂停/禁用判断（含别名） ===================== */

    private static boolean blockedByPauseOrDisable(ItemStack stack, String id) {
        if (stack == null || stack.isEmpty() || id == null) return false;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return false;

        // 规范ID + 所有别名 + 三种大小写键都检查
        Set<String> keys = new LinkedHashSet<>();
        String canon = canonical(id);
        keys.add(canon); keys.add(canon.toUpperCase()); keys.add(canon.toLowerCase());

        // 防水：把所有别名一起检查
        if (WP_ALIASES.contains(id) || WP_ALIASES.contains(canon) || canon.contains("WATERPROOF")) {
            for (String a : WP_ALIASES) {
                String na = norm(a);
                keys.add(na); keys.add(na.toUpperCase()); keys.add(na.toLowerCase());
            }
        }
        // 别名映射表里的同族别名
        for (Map.Entry<String,String> e : ALIAS_TO_CANON.entrySet()) {
            if (e.getValue().equals(canon)) {
                String a = e.getKey();
                keys.add(a); keys.add(a.toUpperCase()); keys.add(a.toLowerCase());
            }
        }

        for (String k : keys) {
            if (nbt.getBoolean("Disabled_" + k) || nbt.getBoolean("IsPaused_" + k)) return true;
        }
        return false;
    }

    /* ===================== 基础访问器 ===================== */

    public static @Nullable UpgradeInfo getUpgradeInfo(String upgradeId) { return REGISTRY.get(canonical(upgradeId)); }

    /** 未注册则返回 5 作为兜底最大等级 */
    public static int getMaxLevel(String upgradeId) {
        UpgradeInfo i = getUpgradeInfo(upgradeId);
        return i != null ? i.maxLevel : 5;
    }

    /**
     * 获取原始等级（不考虑主核心能量门槛），但会把 GUI 暂停/禁用视作0
     * 会在 NBT 中按 规范ID + 别名 + 三种大小写 的 "upgrade_*" 取最大值
     */
    public static int getUpgradeLevel(ItemStack stack, String upgradeId) {
        if (stack == null || stack.isEmpty() || upgradeId == null) return 0;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return 0;

        String canon = canonical(upgradeId);
        int level = 0;

        // 规范ID 三种大小写
        level = Math.max(level, nbt.getInteger("upgrade_" + canon));
        level = Math.max(level, nbt.getInteger("upgrade_" + canon.toUpperCase()));
        level = Math.max(level, nbt.getInteger("upgrade_" + canon.toLowerCase()));

        // 全部别名 三种大小写
        for (Map.Entry<String,String> e : ALIAS_TO_CANON.entrySet()) {
            if (e.getValue().equals(canon)) {
                String a = e.getKey();
                level = Math.max(level, nbt.getInteger("upgrade_" + a));
                level = Math.max(level, nbt.getInteger("upgrade_" + a.toUpperCase()));
                level = Math.max(level, nbt.getInteger("upgrade_" + a.toLowerCase()));
            }
        }

        // GUI 暂停/禁用 → 强制0
        if (level > 0 && blockedByPauseOrDisable(stack, canon)) return 0;
        return level;
    }

    /** 设置等级（只写“规范ID”的键；读取时仍能兼容别名） */
    public static void setUpgradeLevel(ItemStack stack, String upgradeId, int level) {
        String canon = canonical(upgradeId);
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) { nbt = new NBTTagCompound(); stack.setTagCompound(nbt); }

        UpgradeInfo info = getUpgradeInfo(canon);
        if (info != null) level = Math.max(0, Math.min(level, info.maxLevel)); else level = Math.max(0, level);

        nbt.setInteger("upgrade_" + canon, level);
        if (level > 0) nbt.setBoolean("HasUpgrade_" + canon, true);
    }

    /** 启/禁用（只写规范键；读取兼容别名） */
    public static void setUpgradeDisabled(ItemStack stack, String upgradeId, boolean disabled) {
        String canon = canonical(upgradeId);
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) { nbt = new NBTTagCompound(); stack.setTagCompound(nbt); }
        nbt.setBoolean("Disabled_" + canon, disabled);
    }

    /** 是否禁用（规范 + 别名 都算禁用） */
    public static boolean isUpgradeDisabled(ItemStack stack, String upgradeId) {
        if (!stack.hasTagCompound()) return false;
        NBTTagCompound nbt = stack.getTagCompound();
        String canon = canonical(upgradeId);

        if (nbt.getBoolean("Disabled_" + canon)) return true;
        if (nbt.getBoolean("Disabled_" + canon.toUpperCase())) return true;
        if (nbt.getBoolean("Disabled_" + canon.toLowerCase())) return true;

        for (Map.Entry<String,String> e : ALIAS_TO_CANON.entrySet()) {
            if (e.getValue().equals(canon)) {
                String a = e.getKey();
                if (nbt.getBoolean("Disabled_" + a)) return true;
                if (nbt.getBoolean("Disabled_" + a.toUpperCase())) return true;
                if (nbt.getBoolean("Disabled_" + a.toLowerCase())) return true;
            }
        }
        return false;
    }

    /** 是否真正激活：等级>0 & 未禁用/暂停 & 主核心允许（能量/状态） */
    public static boolean isUpgradeActive(ItemStack stack, String upgradeId) {
        int lv = getUpgradeLevel(stack, upgradeId);
        if (lv <= 0) return false;
        if (isUpgradeDisabled(stack, upgradeId)) return false;
        // 交给主核心做最终门槛判断（能耗、状态等）
        return ItemMechanicalCore.isUpgradeActive(stack, canonical(upgradeId));
    }

    /** 有效等级（激活才返回等级，否则返回0） */
    public static int getEffectiveUpgradeLevel(ItemStack stack, String upgradeId) {
        return isUpgradeActive(stack, upgradeId) ? getUpgradeLevel(stack, upgradeId) : 0;
    }

    /* ===================== 统计与工具 ===================== */

    /** 返回“已安装”的规范ID列表（去重；等级>0 或 HasUpgrade_*为true） */
    public static List<String> getInstalledUpgradeIds(ItemStack stack) {
        List<String> result = new ArrayList<>();
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) return result;

        NBTTagCompound nbt = stack.getTagCompound();
        Set<String> installedCanon = new LinkedHashSet<>();

        // 1) 以注册表为基准，检查是否安装
        for (String canon : REGISTRY.keySet()) {
            if (nbt.getBoolean("HasUpgrade_" + canon) || getUpgradeLevel(stack, canon) > 0) {
                installedCanon.add(canon);
            }
        }

        // 2) 兜底：扫描 NBT 中的 upgrade_*/HasUpgrade_*，把能映射到规范ID的也算进去
        for (String k : nbt.getKeySet()) {
            if (k.startsWith("upgrade_") && nbt.getInteger(k) > 0) {
                String raw = k.substring("upgrade_".length());
                installedCanon.add(canonical(raw));
            } else if (k.startsWith("HasUpgrade_") && nbt.getBoolean(k)) {
                String raw = k.substring("HasUpgrade_".length());
                installedCanon.add(canonical(raw));
            }
        }

        result.addAll(installedCanon);
        return result;
    }

    /** 已安装数量（去重） */
    public static int getInstalledUpgradeCount(ItemStack stack) {
        return getInstalledUpgradeIds(stack).size();
    }

    /** 激活总等级（所有扩展升级；去重后逐个取有效等级相加） */
    public static int getTotalActiveUpgradeLevel(ItemStack stack) {
        int total = 0;
        for (String canon : REGISTRY.keySet()) {
            total += getEffectiveUpgradeLevel(stack, canon);
        }
        return total;
    }

    /** 总等级（包括暂停/禁用，但对同一升级只取一个最大等级） */
    public static int getTotalUpgradeLevel(ItemStack stack) {
        int total = 0;
        for (String canon : REGISTRY.keySet()) {
            total += getUpgradeLevel(stack, canon);
        }
        return total;
    }

    /** 某个类别的激活总等级（若是 BASIC，目前为0，因为未在此注册基础升级） */
    public static int getCategoryActiveUpgradeLevel(ItemStack stack, UpgradeCategory category) {
        int total = 0;
        for (UpgradeInfo info : REGISTRY.values()) {
            if (info.category == category) total += getEffectiveUpgradeLevel(stack, info.id);
        }
        return total;
    }

    /** 某个类别的总等级（若是 BASIC，目前为0，因为未在此注册基础升级） */
    public static int getCategoryUpgradeLevel(ItemStack stack, UpgradeCategory category) {
        int total = 0;
        for (UpgradeInfo info : REGISTRY.values()) {
            if (info.category == category) total += getUpgradeLevel(stack, info.id);
        }
        return total;
    }

    /** 是否还能升级（不超过最大等级） */
    public static boolean canUpgrade(ItemStack stack, String upgradeId) {
        return getUpgradeLevel(stack, upgradeId) < getMaxLevel(upgradeId);
    }

    /** 是否能降级（不低于0） */
    public static boolean canDowngrade(ItemStack stack, String upgradeId) {
        return getUpgradeLevel(stack, upgradeId) > 0;
    }

    /** 增减等级（允许降到0，不允许越过上限） */
    public static boolean addUpgradeLevel(ItemStack stack, String upgradeId, int amount) {
        int cur = getUpgradeLevel(stack, upgradeId);
        int max = getMaxLevel(upgradeId);
        int next = Math.max(0, Math.min(cur + amount, max));
        if (next == cur) return false;
        setUpgradeLevel(stack, upgradeId, next);
        return true;
    }

    /** 重置为0并清除禁用标记（保留 HasUpgrade_ 以示曾经安装） */
    public static void resetUpgrade(ItemStack stack, String upgradeId) {
        setUpgradeLevel(stack, upgradeId, 0);
        setUpgradeDisabled(stack, upgradeId, false);
    }

    /** 完全移除（删除 upgrade_/HasUpgrade_/Disabled_ 规范键；别名键保留以兼容旧存档） */
    public static void removeUpgrade(ItemStack stack, String upgradeId) {
        if (!stack.hasTagCompound()) return;
        NBTTagCompound nbt = stack.getTagCompound();
        String canon = canonical(upgradeId);
        nbt.removeTag("upgrade_" + canon);
        nbt.removeTag("Disabled_" + canon);
        nbt.removeTag("HasUpgrade_" + canon);
    }

    /** 升级状态字符串用于显示 */
    public static String getUpgradeStatus(ItemStack stack, String upgradeId) {
        int lv = getUpgradeLevel(stack, upgradeId);
        if (lv == 0) return TextFormatting.YELLOW + "暂停";
        if (isUpgradeDisabled(stack, upgradeId)) return TextFormatting.RED + "禁用";
        if (!isUpgradeActive(stack, upgradeId)) return TextFormatting.GOLD + "能量不足";
        return TextFormatting.GREEN + "激活";
    }

    /** 升级描述（示例，按需扩展） */
    public static List<String> getUpgradeDescription(String upgradeId, int currentLevel, ItemStack stack) {
        List<String> desc = new ArrayList<>();
        UpgradeInfo info = getUpgradeInfo(upgradeId);
        if (info == null) return desc;

        desc.add(info.color + info.displayName);
        desc.add(TextFormatting.GRAY + "类别: " + info.category.color + info.category.name);
        if (currentLevel == 0) desc.add(TextFormatting.YELLOW + "等级: 暂停 (0/" + info.maxLevel + ")");
        else desc.add(TextFormatting.GRAY + "等级: " + TextFormatting.WHITE + currentLevel + "/" + info.maxLevel);
        desc.add(TextFormatting.GRAY + "状态: " + getUpgradeStatus(stack, upgradeId));

        if (currentLevel > 0) {
            desc.add("");
            switch (canonical(upgradeId)) {
                case "YELLOW_SHIELD":
                    desc.add(TextFormatting.YELLOW + "护盾容量: " + (currentLevel * 4) + " 点");
                    desc.add(TextFormatting.GRAY + "破碎后30秒冷却");
                    break;
                case "HEALTH_REGEN":
                    desc.add(TextFormatting.RED + "每秒恢复: " + (currentLevel * 0.5) + " 生命值");
                    break;
                case "DAMAGE_BOOST":
                    desc.add(TextFormatting.RED + "伤害加成: +" + (currentLevel * 25) + "%");
                    desc.add(TextFormatting.GOLD + "暴击几率: " + (currentLevel * 10) + "%");
                    break;
                case "MOVEMENT_SPEED":
                    desc.add(TextFormatting.AQUA + "移动速度: +" + (currentLevel * 20) + "%");
                    break;
                case "ORE_VISION":
                    desc.add(TextFormatting.GOLD + "透视范围: " + (currentLevel * 16) + " 格");
                    break;
                case "STEALTH":
                    desc.add(TextFormatting.DARK_GRAY + "隐身效果等级 " + currentLevel);
                    break;
                case "KINETIC_GENERATOR":
                    desc.add(TextFormatting.GREEN + "移动产能: " + (currentLevel * 50) + " FE/格");
                    break;
                case "SOLAR_GENERATOR":
                    desc.add(TextFormatting.YELLOW + "太阳能: " + (currentLevel * 100) + " FE/秒");
                    break;
                case "VOID_ENERGY":
                    desc.add(TextFormatting.DARK_PURPLE + "虚空能量: " + (currentLevel * 250) + " FE/充能");
                    break;
                case "COMBAT_CHARGER":
                    desc.add(TextFormatting.RED + "战斗充能: " + (currentLevel * 50) + " FE/生命值");
                    break;
                default:
                    desc.add(TextFormatting.GRAY + "等级 " + currentLevel + " 效果");
            }
        } else {
            desc.add("");
            desc.add(TextFormatting.YELLOW + "升级暂停中");
            desc.add(TextFormatting.GRAY + "调整等级至1以上以激活");
        }
        return desc;
    }

    /* ===================== 列表/别名 辅助接口 ===================== */

    /** 所有升级（只包含规范ID，避免别名重复） */
    public static Map<String, UpgradeInfo> getAllUpgrades() {
        return java.util.Collections.unmodifiableMap(REGISTRY);
    }

    /** 所有规范ID */
    public static Set<String> getAllUpgradeIds() {
        return java.util.Collections.unmodifiableSet(REGISTRY.keySet());
    }

    /** 查询某规范ID的全部别名（返回规范ID本身 + 已登记别名；均为大写规范化） */
    public static Set<String> getAliasesFor(String upgradeId) {
        String canon = canonical(upgradeId);
        Set<String> out = new LinkedHashSet<>();
        out.add(canon);
        for (Map.Entry<String,String> e : ALIAS_TO_CANON.entrySet()) {
            if (e.getValue().equals(canon)) out.add(e.getKey());
        }
        if (WP_ALIASES.contains(canon)) {
            for (String a : WP_ALIASES) out.add(norm(a));
        }
        return java.util.Collections.unmodifiableSet(out);
    }

    /* ===================== 兼容工具 ===================== */

    public static ItemStack getCoreFromPlayer(EntityPlayer player) {
        return ItemMechanicalCore.findEquippedMechanicalCore(player);
    }

    public static void addEnergy(ItemStack stack, int amount) {
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) energy.receiveEnergy(amount, false);
    }
}
