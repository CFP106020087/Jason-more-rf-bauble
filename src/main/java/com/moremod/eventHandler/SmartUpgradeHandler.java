package com.moremod.eventHandler;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.upgrades.ItemUpgradeComponent;
import com.moremod.util.UpgradeKeys;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 统一升级处理器（累进消耗版）
 * - 右键道具 => 安装/升级
 * - 升级到n级需要n个道具
 * - 支持"惩罚锁"解锁（使用相应模块自动清锁）
 * - 统一键名/写法，避免大小写重复键
 * - 升级后强制同步佩戴槽，GUI基本立即刷新
 */
public class SmartUpgradeHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote) return; // 服务端处理

        EntityPlayer player = event.getEntityPlayer();
        ItemStack heldItem = event.getItemStack();

        if (!(heldItem.getItem() instanceof ItemUpgradeComponent)) return;

        // 阻止升级组件自己的 onItemRightClick 逻辑，统一走本处理器
        event.setCanceled(true);

        // 找到装备的机械核心
        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (!ItemMechanicalCore.isMechanicalCore(coreStack)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "未找到装备的机械核心！请先装备到头部饰品栏。"
            ));
            return;
        }

        ItemUpgradeComponent upgradeItem = (ItemUpgradeComponent) heldItem.getItem();

        // 计算需要的道具数量
        int requiredAmount = calculateRequiredAmount(coreStack, upgradeItem);
        if (requiredAmount <= 0) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "该升级已达到最大等级！"
            ));
            return;
        }

        // 检查道具数量（创造模式跳过）
        if (!player.isCreative() && heldItem.getCount() < requiredAmount) {
            String upgradeName = getUpgradeDisplayName(upgradeItem);
            int currentLevel = getCurrentUpgradeLevel(coreStack, upgradeItem);
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "升级 " + upgradeName +
                            " 到 Lv." + (currentLevel + 1) + " 需要 " + requiredAmount +
                            " 个道具，你只有 " + heldItem.getCount() + " 个！"
            ));
            return;
        }

        boolean ok = performUpgrade(player, coreStack, heldItem, upgradeItem, requiredAmount);
        if (ok) {
            if (!player.isCreative()) heldItem.shrink(requiredAmount); // 消耗所需数量
            playUpgradeEffects(player);
            // 强制同步佩戴槽，推动客户端立刻拿到最新 NBT（GUI 可立即反映）
            forceSyncCore(player);
        }
    }

    /** 计算升级所需的道具数量 */
    private int calculateRequiredAmount(ItemStack coreStack, ItemUpgradeComponent upgradeItem) {
        String rawId = upgradeItem.getUpgradeType();
        String cid = UpgradeKeys.foldAlias(rawId);
        String registryName = upgradeItem.getRegistryName() != null ?
                upgradeItem.getRegistryName().toString() : "";

        // 特殊处理：飞行模块（分级套件）
        if (registryName.contains("flight_module")) {
            int current = getFlightLevel(coreStack);
            if (registryName.contains("basic") && current == 0) return 1;
            if (registryName.contains("advanced") && current == 1) return 2;
            if (registryName.contains("ultimate") && current == 2) return 3;
            return 0; // 已达最大或不满足前置条件
        }

        // 特殊处理：防水模块（分级套件）
        if (registryName.contains("waterproof_module") || UpgradeKeys.isWaterproof(cid)) {
            int current = getWaterproofLevel(coreStack);
            if (registryName.contains("basic") && current == 0) return 1;
            if (registryName.contains("advanced") && current == 1) return 2;
            if (registryName.contains("deep_sea") && current == 2) return 3;
            return 0; // 已达最大或不满足前置条件
        }

        // 特殊处理：组合套装（每次消耗固定数量）
        if (rawId.contains("PACKAGE") || registryName.contains("_package") ||
                registryName.contains("omnipotent_package")) {
            // 套装类型检查是否可以应用
            if (canApplyPackage(coreStack, rawId, registryName)) {
                return 1; // 套装固定消耗1个
            }
            return 0;
        }

        // 常规升级：下一级需要的道具数 = 下一级的等级数
        int currentLevel = lvOf(coreStack, cid);
        int maxLevel = maxOf(coreStack, cid);

        if (currentLevel >= maxLevel) return 0;
        return currentLevel + 1; // 升到n级需要n个道具
    }

    /** 获取升级的显示名称 */
    private String getUpgradeDisplayName(ItemUpgradeComponent upgradeItem) {
        String rawId = upgradeItem.getUpgradeType();
        String cid = UpgradeKeys.foldAlias(rawId);
        return getDisplayName(cid);
    }

    /** 获取当前升级等级 */
    private int getCurrentUpgradeLevel(ItemStack coreStack, ItemUpgradeComponent upgradeItem) {
        String rawId = upgradeItem.getUpgradeType();
        String cid = UpgradeKeys.foldAlias(rawId);
        String registryName = upgradeItem.getRegistryName() != null ?
                upgradeItem.getRegistryName().toString() : "";

        if (registryName.contains("flight_module")) {
            return getFlightLevel(coreStack);
        }
        if (registryName.contains("waterproof_module") || UpgradeKeys.isWaterproof(cid)) {
            return getWaterproofLevel(coreStack);
        }

        return lvOf(coreStack, cid);
    }

    /** 检查套装是否可以应用 */
    private boolean canApplyPackage(ItemStack core, String rawType, String registryName) {
        boolean isSurvival = rawType.equalsIgnoreCase("SURVIVAL_PACKAGE") ||
                registryName.contains("survival_enhancement_package");
        boolean isCombat = rawType.equalsIgnoreCase("COMBAT_PACKAGE") ||
                registryName.contains("combat_enhancement_package");
        boolean isOmni = rawType.equalsIgnoreCase("OMNIPOTENT_PACKAGE") ||
                registryName.contains("omnipotent_package");

        if (!isSurvival && !isCombat && !isOmni) return false;

        String[] targetList = isSurvival ? new String[]{"YELLOW_SHIELD", "HEALTH_REGEN", "HUNGER_THIRST"} :
                (isCombat ? new String[]{"DAMAGE_BOOST", "ATTACK_SPEED", "RANGE_EXTENSION"} :
                        new String[]{"ENERGY_CAPACITY", "ENERGY_EFFICIENCY", "ARMOR_ENHANCEMENT"});

        // 检查是否任一模块已满
        for (String u : targetList) {
            int cur = lvOf(core, u);
            int max = maxOf(core, u);
            if (cur >= max) return false;
        }
        return true;
    }

    /** 执行升级（总入口） */
    private boolean performUpgrade(EntityPlayer player, ItemStack coreStack,
                                   ItemStack upgradeStack, ItemUpgradeComponent upgradeItem,
                                   int consumeAmount) {
        // 原始字符串
        String rawId = upgradeItem.getUpgradeType();
        // 规范ID（全大写统一）
        String cid = UpgradeKeys.foldAlias(rawId);

        int upgradeValue = upgradeItem.getUpgradeValue();
        String registryName = upgradeItem.getRegistryName() != null ? upgradeItem.getRegistryName().toString() : "";

        // 1) 特殊：飞行模块（分级套件）
        if (registryName.contains("flight_module")) {
            return handleFlightModule(player, coreStack, registryName, consumeAmount);
        }

        // 2) 特殊：防水模块（分级套件）
        if (registryName.contains("waterproof_module") || UpgradeKeys.isWaterproof(cid)) {
            return handleWaterproofModule(player, coreStack, registryName, cid, upgradeValue, consumeAmount);
        }

        // 3) 特殊：组合套装（含 OMNIPOTENT_PACKAGE）
        if (rawId.equalsIgnoreCase("SURVIVAL_PACKAGE")
                || rawId.equalsIgnoreCase("COMBAT_PACKAGE")
                || rawId.equalsIgnoreCase("OMNIPOTENT_PACKAGE")
                || registryName.contains("_package")
                || registryName.contains("omnipotent_package")
                || registryName.contains("omnipotent_package_chip")) {
            return handlePackageUpgrade(player, coreStack, rawId, registryName, consumeAmount);
        }

        // 4) 常规：基础 or 扩展升级
        if (isBasicUpgrade(cid)) {
            return handleBasicUpgrade(player, coreStack, cid, upgradeValue, consumeAmount);
        } else {
            return handleExtendedUpgrade(player, coreStack, cid, upgradeValue, consumeAmount);
        }
    }

    // =======================
    // 基础/扩展 升级处理
    // =======================

    /** 基础升级（枚举存在） */
    private boolean handleBasicUpgrade(EntityPlayer player, ItemStack coreStack,
                                       String cid, int upVal, int consumeAmount) {
        // 惩罚锁：允许"使用对应模块"直接解锁
        unlockIfLocked(player, coreStack, cid);

        ItemMechanicalCore.UpgradeType enumType = null;
        for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
            if (t.getKey().equalsIgnoreCase(cid) || t.name().equalsIgnoreCase(cid)) {
                enumType = t; break;
            }
        }
        if (enumType == null) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "未知基础升级: " + cid));
            return false;
        }

        int cur = ItemMechanicalCore.getUpgradeLevel(coreStack, enumType);
        int max = getMaxLevel(enumType);
        if (cur >= max) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + enumType.getDisplayName() + " 已达最大等级！"));
            return false;
        }

        int newLv = cur + 1; // 每次只升1级

        // 写回到旧系统（枚举）
        ItemMechanicalCore.setUpgradeLevel(coreStack, enumType, newLv);
        // 写回扩展系统（保证可见）
        ItemMechanicalCoreExtended.setUpgradeLevel(coreStack, enumType.getKey(), newLv);
        ItemMechanicalCoreExtended.setUpgradeLevel(coreStack, enumType.name(), newLv);
        // 规范键
        UpgradeKeys.setLevel(coreStack, cid, newLv);
        UpgradeKeys.markOwnedActive(coreStack, cid, newLv);

        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ " + enumType.getColor() + enumType.getDisplayName() +
                        TextFormatting.WHITE + " 升级至 Lv." + newLv +
                        TextFormatting.GRAY + " (消耗 " + consumeAmount + " 个道具)"
        ));
        if (newLv == max) {
            player.sendMessage(new TextComponentString(TextFormatting.GOLD + "⭐ " + enumType.getDisplayName() + " 已达到最大等级！"));
        }
        return true;
    }

    /** 扩展升级（ItemMechanicalCoreExtended） */
    private boolean handleExtendedUpgrade(EntityPlayer player, ItemStack coreStack,
                                          String cid, int upVal, int consumeAmount) {
        // 惩罚锁：允许"使用对应模块"直接解锁
        unlockIfLocked(player, coreStack, cid);

        ItemMechanicalCoreExtended.UpgradeInfo info =
                ItemMechanicalCoreExtended.getUpgradeInfo(cid);
        if (info == null) info = ItemMechanicalCoreExtended.getUpgradeInfo(cid.toUpperCase(Locale.ROOT));
        if (info == null) info = ItemMechanicalCoreExtended.getUpgradeInfo(cid.toLowerCase(Locale.ROOT));
        if (info == null) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "未知的升级类型: " + cid));
            return false;
        }

        int cur = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, cid);
        int max = info.maxLevel;
        if (cur >= max) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + info.displayName + " 已达到最大等级！"));
            return false;
        }

        int newLv = cur + 1; // 每次只升1级
        ItemMechanicalCoreExtended.setUpgradeLevel(coreStack, cid, newLv);
        UpgradeKeys.setLevel(coreStack, cid, newLv);
        UpgradeKeys.markOwnedActive(coreStack, cid, newLv);

        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ " + info.color + info.displayName +
                        TextFormatting.WHITE + " 升级至 Lv." + newLv +
                        TextFormatting.GRAY + " (消耗 " + consumeAmount + " 个道具)"
        ));
        if (newLv == max) {
            player.sendMessage(new TextComponentString(TextFormatting.GOLD + "⭐ " + info.displayName + " 已达到最大等级！"));
        }
        return true;
    }

    /** 是否为基础升级（匹配旧枚举或其 key） */
    private boolean isBasicUpgrade(String cid) {
        for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
            if (t.getKey().equalsIgnoreCase(cid) || t.name().equalsIgnoreCase(cid)) return true;
        }
        return false;
    }

    // =======================
    // 防水 模块（分级）
    // =======================
    private boolean handleWaterproofModule(EntityPlayer player, ItemStack coreStack,
                                           String registryName, String cid, int upVal, int consumeAmount) {
        // 统一成 WATERPROOF_MODULE
        cid = "WATERPROOF_MODULE";
        unlockIfLocked(player, coreStack, cid);

        int cur = getWaterproofLevel(coreStack);
        int target;
        if (registryName.contains("waterproof_module_basic") || cid.contains("BASIC")) {
            if (cur > 0) return msg(player, TextFormatting.RED + "已安装防水模块！", false);
            target = 1;
        } else if (registryName.contains("waterproof_module_advanced") || cid.contains("ADVANCED")) {
            if (cur != 1) return msg(player, TextFormatting.RED + (cur == 0 ? "需要先安装基础防水模块！" : "已安装更高级的防水模块！"), false);
            target = 2;
        } else if (registryName.contains("waterproof_module_deep_sea") || cid.contains("DEEP_SEA")) {
            if (cur != 2) return msg(player, TextFormatting.RED + (cur < 2 ? "需要先安装高级防水模块！" : "已达到最高等级！"), false);
            target = 3;
        } else {
            target = Math.min(cur + Math.max(1, upVal), 3);
            if (target == cur) return msg(player, TextFormatting.RED + "防水模块已达到最大等级！", false);
        }

        setWaterproofLevel(coreStack, target);
        switch (target) {
            case 1:
                msg(player, TextFormatting.AQUA + "💧 基础防水涂层已应用！" +
                        TextFormatting.GRAY + " (消耗 " + consumeAmount + " 个道具)", true);
                msg(player, TextFormatting.GRAY + "核心现在可以安全接触水体", true);
                break;
            case 2:
                msg(player, TextFormatting.BLUE + "💧 高级防水系统已安装！" +
                        TextFormatting.GRAY + " (消耗 " + consumeAmount + " 个道具)", true);
                msg(player, TextFormatting.GRAY + "获得水下呼吸能力", true);
                break;
            case 3:
                msg(player, TextFormatting.DARK_AQUA + "🌊 深海适应模块已激活！" +
                        TextFormatting.GRAY + " (消耗 " + consumeAmount + " 个道具)", true);
                msg(player, TextFormatting.GRAY + "完整的水下作业能力已解锁", true);
                break;
        }
        return true;
    }

    private int getWaterproofLevel(ItemStack core) {
        int lv = Math.max(lvOf(core, "WATERPROOF_MODULE"), lvOf(core, "WATERPROOF"));
        return lv;
    }
    private void setWaterproofLevel(ItemStack core, int lv) {
        // 同步到扩展 & 规范键
        ItemMechanicalCoreExtended.setUpgradeLevel(core, "WATERPROOF_MODULE", lv);
        UpgradeKeys.setLevel(core, "WATERPROOF_MODULE", lv);
        UpgradeKeys.markOwnedActive(core, "WATERPROOF_MODULE", lv);
        // 兼容另一个别名也写一份扩展层
        ItemMechanicalCoreExtended.setUpgradeLevel(core, "WATERPROOF", lv);

        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);
        nbt.setBoolean("hasWaterproofModule", lv > 0);
        nbt.setInteger("waterproofLevel", lv);
    }

    // =======================
    // 飞行 模块（分级）
    // =======================
    private boolean handleFlightModule(EntityPlayer player, ItemStack coreStack, String registryName, int consumeAmount) {
        String cid = "FLIGHT_MODULE";
        unlockIfLocked(player, coreStack, cid);

        int cur = getFlightLevel(coreStack);
        int target;
        if (registryName.contains("flight_module_basic")) {
            if (cur > 0) return msg(player, TextFormatting.RED + "已安装飞行模块！", false);
            target = 1;
        } else if (registryName.contains("flight_module_advanced")) {
            if (cur != 1) return msg(player, TextFormatting.RED + (cur == 0 ? "需要先安装基础飞行模块！" : "已安装更高级的飞行模块！"), false);
            target = 2;
        } else if (registryName.contains("flight_module_ultimate")) {
            if (cur != 2) return msg(player, TextFormatting.RED + (cur < 2 ? "需要先安装高级飞行模块！" : "已达到最高等级！"), false);
            target = 3;
        } else {
            return false;
        }

        // 写回所有系统 + 规范键
        ItemMechanicalCore.setUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE, target);
        ItemMechanicalCoreExtended.setUpgradeLevel(coreStack, "FLIGHT_MODULE", target);
        UpgradeKeys.setLevel(coreStack, "FLIGHT_MODULE", target);
        UpgradeKeys.markOwnedActive(coreStack, "FLIGHT_MODULE", target);

        // 初始化飞行控制参数
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(coreStack);
        nbt.setBoolean("FlightModuleEnabled", true);
        if (target >= 2 && !nbt.hasKey("FlightHoverMode")) nbt.setBoolean("FlightHoverMode", false);
        if (target >= 3 && !nbt.hasKey("CoreSpeedMode"))    nbt.setInteger("CoreSpeedMode", 0);

        switch (target) {
            case 1:
                msg(player, TextFormatting.LIGHT_PURPLE + "✦ 飞行系统已激活！" +
                        TextFormatting.GRAY + " (消耗 " + consumeAmount + " 个道具)", true);
                msg(player, TextFormatting.GRAY + "按住空格上升，Shift下降", true);
                break;
            case 2:
                msg(player, TextFormatting.GOLD + "✦ 飞行系统升级！悬停模式已解锁！" +
                        TextFormatting.GRAY + " (消耗 " + consumeAmount + " 个道具)", true);
                msg(player, TextFormatting.GRAY + "按H键切换悬停模式", true);
                break;
            case 3:
                msg(player, TextFormatting.DARK_PURPLE + "✦✦ 终极飞行系统已启动！速度模式已解锁！" +
                        TextFormatting.GRAY + " (消耗 " + consumeAmount + " 个道具)", true);
                msg(player, TextFormatting.GRAY + "按G键切换速度模式", true);
                break;
        }
        return true;
    }

    private int getFlightLevel(ItemStack core) {
        int lv = 0;
        // 旧系统
        lv = Math.max(lv, ItemMechanicalCore.getUpgradeLevel(core, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE));
        // 新系统
        lv = Math.max(lv, ItemMechanicalCoreExtended.getUpgradeLevel(core, "FLIGHT_MODULE"));
        return lv;
    }

    // =======================
    // 组合套装（含 OMNIPOTENT_PACKAGE）
    // =======================
    private boolean handlePackageUpgrade(EntityPlayer player, ItemStack core,
                                         String rawType, String registryName, int consumeAmount) {
        boolean isSurvival = rawType.equalsIgnoreCase("SURVIVAL_PACKAGE") || registryName.contains("survival_enhancement_package");
        boolean isCombat   = rawType.equalsIgnoreCase("COMBAT_PACKAGE")   || registryName.contains("combat_enhancement_package");
        boolean isOmni     = rawType.equalsIgnoreCase("OMNIPOTENT_PACKAGE")
                || registryName.contains("omnipotent_package")
                || registryName.contains("omnipotent_package_chip");

        if (!isSurvival && !isCombat && !isOmni) {
            // registryName.contains("_package") 情况下，但没识别出具体类型
            return msg(player, TextFormatting.RED + "未知的套装类型: " + rawType, false);
        }

        // 定义套装的模块清单
        String[] survivalUps = {"YELLOW_SHIELD", "HEALTH_REGEN", "HUNGER_THIRST"};
        String[] combatUps   = {"DAMAGE_BOOST", "ATTACK_SPEED", "RANGE_EXTENSION"};
        // 全能强化芯片：一次 +1 到三个基础项（与你物品描述一致）
        String[] omniUps     = {"ENERGY_CAPACITY", "ENERGY_EFFICIENCY", "ARMOR_ENHANCEMENT"};

        Map<String, Integer> before = new HashMap<>();
        String[] targetList = isSurvival ? survivalUps : (isCombat ? combatUps : omniUps);

        // 统一预检查：任一模块已满 => 整套无法应用
        for (String u : targetList) {
            unlockIfLocked(player, core, u);
            int cur = lvOf(core, u);
            before.put(u, cur);
            int max = maxOf(core, u);
            if (cur >= max) {
                return msg(player, TextFormatting.RED + getDisplayName(u) + " 已达最大等级，无法应用套装！", false);
            }
        }

        // 应用：全部 +1 级
        for (String u : targetList) {
            applyUpgrade(core, u, before.get(u) + 1);
        }

        // 提示与效果
        if (isSurvival) {
            msg(player, TextFormatting.GREEN + "✦ 生存强化套装已应用！" +
                    TextFormatting.GRAY + " (消耗 " + consumeAmount + " 个道具)", true);
            msg(player, TextFormatting.YELLOW + "黄条护盾 Lv." + (before.get("YELLOW_SHIELD") + 1), true);
            msg(player, TextFormatting.RED + "生命恢复 Lv." + (before.get("HEALTH_REGEN") + 1), true);
            msg(player, TextFormatting.GREEN + "饥饿管理 Lv." + (before.get("HUNGER_THIRST") + 1), true);
        } else if (isCombat) {
            msg(player, TextFormatting.RED + "✦ 战斗强化套装已应用！" +
                    TextFormatting.GRAY + " (消耗 " + consumeAmount + " 个道具)", true);
            msg(player, TextFormatting.DARK_RED + "伤害提升 Lv." + (before.get("DAMAGE_BOOST") + 1), true);
            msg(player, TextFormatting.YELLOW + "攻击速度 Lv." + (before.get("ATTACK_SPEED") + 1), true);
            msg(player, TextFormatting.BLUE + "范围拓展 Lv." + (before.get("RANGE_EXTENSION") + 1), true);
        } else { // isOmni
            msg(player, TextFormatting.LIGHT_PURPLE + "✦ 全能强化芯片已应用！" +
                    TextFormatting.GRAY + " (消耗 " + consumeAmount + " 个道具)", true);
            msg(player, TextFormatting.GOLD + "能量容量 Lv." + (before.get("ENERGY_CAPACITY") + 1), true);
            msg(player, TextFormatting.GREEN + "能量效率 Lv." + (before.get("ENERGY_EFFICIENCY") + 1), true);
            msg(player, TextFormatting.BLUE + "护甲强化 Lv." + (before.get("ARMOR_ENHANCEMENT") + 1), true);
        }

        playPackageUpgradeEffects(player);
        return true;
    }

    // =======================
    // 通用小工具
    // =======================

    /** 如果该升级被"锁"，先解锁（使用对应模块=维修行为） */
    private void unlockIfLocked(EntityPlayer player, ItemStack core, String id) {
        if (UpgradeKeys.unlock(core, id)) {
            msg(player, TextFormatting.AQUA + "已修复损坏模块，允许重新安装。", true);
        }
    }

    /** 获取当前等级（兼容各系统 & 规范键） */
    private int lvOf(ItemStack core, String id) {
        int lv = 0;
        lv = Math.max(lv, ItemMechanicalCoreExtended.getUpgradeLevel(core, id));
        lv = Math.max(lv, ItemMechanicalCoreExtended.getUpgradeLevel(core, id.toLowerCase(Locale.ROOT)));
        try {
            ItemMechanicalCore.UpgradeType t = ItemMechanicalCore.UpgradeType.valueOf(UpgradeKeys.canon(id));
            lv = Math.max(lv, ItemMechanicalCore.getUpgradeLevel(core, t));
        } catch (Throwable ignored) {}
        lv = Math.max(lv, UpgradeKeys.getLevel(core, id));
        return lv;
    }

    /** 设置等级（同时写回扩展/旧系统/规范键，清除暂停/记录拥有） */
    private void applyUpgrade(ItemStack core, String id, int level) {
        String cid = UpgradeKeys.foldAlias(id);
        // 扩展系统
        ItemMechanicalCoreExtended.setUpgradeLevel(core, cid, level);
        // 旧系统（若有枚举）
        try {
            ItemMechanicalCore.UpgradeType t = ItemMechanicalCore.UpgradeType.valueOf(cid);
            ItemMechanicalCore.setUpgradeLevel(core, t, level);
        } catch (Throwable ignored) {}
        // 规范键
        UpgradeKeys.setLevel(core, cid, level);
        UpgradeKeys.markOwnedActive(core, cid, level);
    }

    /** 获取最大等级（尽量从定义拿；拿不到给默认） */
    private int maxOf(ItemStack core, String id) {
        ItemMechanicalCoreExtended.UpgradeInfo info = ItemMechanicalCoreExtended.getUpgradeInfo(id);
        if (info == null) info = ItemMechanicalCoreExtended.getUpgradeInfo(id.toUpperCase(Locale.ROOT));
        if (info == null) info = ItemMechanicalCoreExtended.getUpgradeInfo(id.toLowerCase(Locale.ROOT));
        if (info != null) return info.maxLevel;

        try {
            ItemMechanicalCore.UpgradeType t = ItemMechanicalCore.UpgradeType.valueOf(UpgradeKeys.canon(id));
            return getMaxLevel(t);
        } catch (Throwable ignored) {}
        return 3;
    }

    /** 旧系统最大等级（与你GUI里保持一致） */
    private int getMaxLevel(ItemMechanicalCore.UpgradeType type) {
        switch (type) {
            case ENERGY_CAPACITY: return 10;
            case ENERGY_EFFICIENCY: return 5;
            case ARMOR_ENHANCEMENT: return 5;
            case SPEED_BOOST: return 3;
            case REGENERATION: return 3;
            case FLIGHT_MODULE: return 3;
            case SHIELD_GENERATOR: return 3;
            case TEMPERATURE_CONTROL: return 5;
            default:
                if (type.name().contains("WATERPROOF")) return 3;
                return 5;
        }
    }

    /** 友好名称（用于提示） */
    private String getDisplayName(String id) {
        ItemMechanicalCoreExtended.UpgradeInfo info = ItemMechanicalCoreExtended.getUpgradeInfo(id);
        if (info != null) return info.displayName;

        // 尝试从基础升级获取
        for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
            if (t.getKey().equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)) {
                return t.getDisplayName();
            }
        }

        return UpgradeKeys.canon(id).replace("_", " ");
    }

    /** 特殊提示（护盾/伤害/速度/防水 等） */


    /** 升级动画/音效 */
    private void playUpgradeEffects(EntityPlayer player) {
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
        for (int i = 0; i < 20; i++) {
            double d0 = player.world.rand.nextGaussian() * 0.02D;
            double d1 = player.world.rand.nextGaussian() * 0.02D;
            double d2 = player.world.rand.nextGaussian() * 0.02D;
            player.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                    player.posX + player.world.rand.nextFloat() * 2.0F - 1.0F,
                    player.posY + 1.0D + player.world.rand.nextFloat(),
                    player.posZ + player.world.rand.nextFloat() * 2.0F - 1.0F,
                    d0, d1, d2);
        }
    }

    /** 套装的更强特效 */
    private void playPackageUpgradeEffects(EntityPlayer player) {
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.5F, 1.5F);
        for (int i = 0; i < 50; i++) {
            double angle = (Math.PI * 2) * i / 50;
            double radius = 2.0;
            double x = player.posX + Math.cos(angle) * radius;
            double z = player.posZ + Math.sin(angle) * radius;
            player.world.spawnParticle(EnumParticleTypes.PORTAL, x, player.posY + 1.0, z, 0, 0.1, 0);
            player.world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, x, player.posY + 1.0, z,
                    (player.posX - x) * 0.5, 0.5, (player.posZ - z) * 0.5);
        }
    }

    /** 立刻把"机械核心"这个饰品槽强制写回一次，以触发服务端→客户端同步（1.12最稳的立刷办法） */
    private void forceSyncCore(EntityPlayer player) {
        try {
            IBaublesItemHandler h = BaublesApi.getBaublesHandler(player);
            if (h != null) {
                for (int i = 0; i < h.getSlots(); i++) {
                    ItemStack s = h.getStackInSlot(i);
                    if (ItemMechanicalCore.isMechanicalCore(s)) {
                        h.setStackInSlot(i, s.copy());
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}
        player.inventory.markDirty();
        player.openContainer.detectAndSendChanges();
    }

    /** 简化消息 */
    private boolean msg(EntityPlayer p, String s, boolean ret) {
        p.sendMessage(new TextComponentString(s));
        return ret;
    }
}