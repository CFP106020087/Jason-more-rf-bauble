package com.moremod.eventHandler;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.upgrades.ItemUpgradeComponent;
import com.moremod.util.UpgradeKeys;
import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.IMechCoreModule;
import com.moremod.upgrades.ModuleRegistry;
import com.moremod.network.NetworkHandler;
import com.moremod.network.PacketSyncMechCoreData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
 * 统一升级处理器（分级模块版）
 * ✅ 修复：确保所有升级路径都记录 OriginalMax
 */
public class SmartUpgradeHandler {

    // ✅ 添加常量
    private static final String K_ORIGINAL_MAX = "OriginalMax_";
    private static final String K_OWNED_MAX = "OwnedMax_";

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote) return;

        EntityPlayer player = event.getEntityPlayer();
        ItemStack heldItem = event.getItemStack();

        if (!(heldItem.getItem() instanceof ItemUpgradeComponent)) return;

        event.setCanceled(true);

        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (!ItemMechanicalCore.isMechanicalCore(coreStack)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "未找到装备的机械核心！请先装备到头部饰品栏。"
            ));
            return;
        }

        ItemUpgradeComponent upgradeItem = (ItemUpgradeComponent) heldItem.getItem();

        UpgradeValidation validation = validateUpgrade(coreStack, upgradeItem);
        if (!validation.canUpgrade) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + validation.message));
            return;
        }

        boolean ok = performUpgrade(player, coreStack, heldItem, upgradeItem, validation);
        if (ok) {
            if (!player.isCreative()) heldItem.shrink(1);
            playUpgradeEffects(player);
            forceSyncCore(player);
        }
    }

    // ================= ✅ 核心：记录 OriginalMax =================

    /**
     * ✅ 记录/更新 OriginalMax（历史最高值）
     *
     * 规则：
     * 1. 如果 OriginalMax 不存在，设置为 newLevel
     * 2. 如果 OriginalMax < newLevel，更新为 newLevel
     * 3. 永不降低 OriginalMax
     */
    private void recordOriginalMax(ItemStack coreStack, String upgradeId, int newLevel) {
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(coreStack);

        String upperId = upgradeId.toUpperCase();
        String lowerId = upgradeId.toLowerCase();
        String[] variants = {upgradeId, upperId, lowerId};

        // 获取当前的 OriginalMax
        int currentOriginalMax = 0;
        for (String variant : variants) {
            int val = nbt.getInteger(K_ORIGINAL_MAX + variant);
            currentOriginalMax = Math.max(currentOriginalMax, val);
        }

        // ✅ 只在新等级更高时更新
        if (newLevel > currentOriginalMax) {
            System.out.println("[RecordOriginalMax] 更新历史最高值: " + upgradeId);
            System.out.println("  旧值: " + currentOriginalMax);
            System.out.println("  新值: " + newLevel);

            // 写入所有三个变体
            nbt.setInteger(K_ORIGINAL_MAX + upgradeId, newLevel);
            nbt.setInteger(K_ORIGINAL_MAX + upperId, newLevel);
            nbt.setInteger(K_ORIGINAL_MAX + lowerId, newLevel);
        } else if (currentOriginalMax > 0) {
            System.out.println("[RecordOriginalMax] 保持历史最高值: " + upgradeId + " = " + currentOriginalMax);
        } else {
            // 第一次记录
            System.out.println("[RecordOriginalMax] 首次记录: " + upgradeId + " = " + newLevel);
            nbt.setInteger(K_ORIGINAL_MAX + upgradeId, newLevel);
            nbt.setInteger(K_ORIGINAL_MAX + upperId, newLevel);
            nbt.setInteger(K_ORIGINAL_MAX + lowerId, newLevel);
        }

        // ✅ 验证写入成功
        int verify = nbt.getInteger(K_ORIGINAL_MAX + upperId);
        if (verify < newLevel) {
            System.err.println("[RecordOriginalMax] ⚠️ 警告：写入验证失败！");
            System.err.println("  预期: " + newLevel);
            System.err.println("  实际: " + verify);
        }
    }

    // ================= 升级验证 =================

    private static class UpgradeValidation {
        boolean canUpgrade;
        String message;
        int requiredLevel;
        int currentLevel;
        int moduleLevel;
        String upgradeType;

        UpgradeValidation(boolean can, String msg) {
            this.canUpgrade = can;
            this.message = msg;
        }
    }

    private UpgradeValidation validateUpgrade(ItemStack coreStack, ItemUpgradeComponent upgradeItem) {
        String rawId = upgradeItem.getUpgradeType();
        String cid = UpgradeKeys.foldAlias(rawId);
        String registryName = upgradeItem.getRegistryName() != null ?
                upgradeItem.getRegistryName().toString() : "";

        int moduleLevel = getModuleLevel(upgradeItem, registryName);

        if (registryName.contains("flight_module")) {
            return validateFlightModule(coreStack, registryName, moduleLevel);
        }

        if (registryName.contains("waterproof_module") || UpgradeKeys.isWaterproof(cid)) {
            return validateWaterproofModule(coreStack, registryName, moduleLevel);
        }

        if (rawId.contains("PACKAGE") || registryName.contains("_package") ||
                registryName.contains("omnipotent_package")) {
            return validatePackage(coreStack, rawId, registryName);
        }

        int currentLevel = lvOf(coreStack, cid);
        int maxLevel = maxOf(coreStack, cid);

        if (currentLevel >= maxLevel) {
            return new UpgradeValidation(false, getDisplayName(cid) + " 已达到最大等级！");
        }

        int requiredLevel = currentLevel + 1;
        if (moduleLevel != requiredLevel) {
            return new UpgradeValidation(false,
                    String.format("升级到 Lv.%d 需要 %d 级模块，当前模块为 %d 级！",
                            requiredLevel, requiredLevel, moduleLevel));
        }

        UpgradeValidation result = new UpgradeValidation(true, "");
        result.requiredLevel = requiredLevel;
        result.currentLevel = currentLevel;
        result.moduleLevel = moduleLevel;
        result.upgradeType = cid;
        return result;
    }

    private int getModuleLevel(ItemUpgradeComponent item, String registryName) {
        ItemStack stack = new ItemStack(item);
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt.hasKey("ModuleLevel")) {
                return nbt.getInteger("ModuleLevel");
            }
            if (nbt.hasKey("Level")) {
                return nbt.getInteger("Level");
            }
        }

        if (registryName.contains("_lv") || registryName.contains("_level")) {
            String[] parts = registryName.split("_");
            for (String part : parts) {
                if (part.startsWith("lv") || part.startsWith("level")) {
                    String numStr = part.replaceAll("[^0-9]", "");
                    try {
                        return Integer.parseInt(numStr);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (registryName.contains("basic") || registryName.contains("tier1")) return 1;
        if (registryName.contains("advanced") || registryName.contains("tier2")) return 2;
        if (registryName.contains("ultimate") || registryName.contains("tier3")) return 3;
        if (registryName.contains("legendary") || registryName.contains("tier4")) return 4;
        if (registryName.contains("mythic") || registryName.contains("tier5")) return 5;

        int upVal = item.getUpgradeValue();
        if (upVal > 0 && upVal <= 10) {
            return upVal;
        }

        return 1;
    }

    private UpgradeValidation validateFlightModule(ItemStack coreStack, String registryName, int moduleLevel) {
        int current = getFlightLevel(coreStack);

        if (registryName.contains("basic") || moduleLevel == 1) {
            if (current >= 1) {
                return new UpgradeValidation(false, "已安装飞行模块！");
            }
            UpgradeValidation result = new UpgradeValidation(true, "");
            result.requiredLevel = 1;
            result.currentLevel = current;
            result.moduleLevel = 1;
            return result;
        }

        if (registryName.contains("advanced") || moduleLevel == 2) {
            if (current != 1) {
                return new UpgradeValidation(false,
                        current == 0 ? "需要先安装基础飞行模块！" : "已安装更高级的飞行模块！");
            }
            UpgradeValidation result = new UpgradeValidation(true, "");
            result.requiredLevel = 2;
            result.currentLevel = current;
            result.moduleLevel = 2;
            return result;
        }

        if (registryName.contains("ultimate") || moduleLevel == 3) {
            if (current != 2) {
                return new UpgradeValidation(false,
                        current < 2 ? "需要先安装高级飞行模块！" : "已达到最高等级！");
            }
            UpgradeValidation result = new UpgradeValidation(true, "");
            result.requiredLevel = 3;
            result.currentLevel = current;
            result.moduleLevel = 3;
            return result;
        }

        return new UpgradeValidation(false, "未知的飞行模块等级！");
    }

    private UpgradeValidation validateWaterproofModule(ItemStack coreStack, String registryName, int moduleLevel) {
        int current = getWaterproofLevel(coreStack);

        if (registryName.contains("basic") || moduleLevel == 1) {
            if (current >= 1) {
                return new UpgradeValidation(false, "已安装防水模块！");
            }
            UpgradeValidation result = new UpgradeValidation(true, "");
            result.requiredLevel = 1;
            result.currentLevel = current;
            result.moduleLevel = 1;
            return result;
        }

        if (registryName.contains("advanced") || moduleLevel == 2) {
            if (current != 1) {
                return new UpgradeValidation(false,
                        current == 0 ? "需要先安装基础防水模块！" : "已安装更高级的防水模块！");
            }
            UpgradeValidation result = new UpgradeValidation(true, "");
            result.requiredLevel = 2;
            result.currentLevel = current;
            result.moduleLevel = 2;
            return result;
        }

        if (registryName.contains("deep_sea") || moduleLevel == 3) {
            if (current != 2) {
                return new UpgradeValidation(false,
                        current < 2 ? "需要先安装高级防水模块！" : "已达到最高等级！");
            }
            UpgradeValidation result = new UpgradeValidation(true, "");
            result.requiredLevel = 3;
            result.currentLevel = current;
            result.moduleLevel = 3;
            return result;
        }

        return new UpgradeValidation(false, "未知的防水模块等级！");
    }

    private UpgradeValidation validatePackage(ItemStack core, String rawType, String registryName) {
        boolean isSurvival = rawType.equalsIgnoreCase("SURVIVAL_PACKAGE") ||
                registryName.contains("survival_enhancement_package");
        boolean isCombat = rawType.equalsIgnoreCase("COMBAT_PACKAGE") ||
                registryName.contains("combat_enhancement_package");
        boolean isOmni = rawType.equalsIgnoreCase("OMNIPOTENT_PACKAGE") ||
                registryName.contains("omnipotent_package");

        if (!isSurvival && !isCombat && !isOmni) {
            return new UpgradeValidation(false, "未知的套装类型！");
        }

        String[] targetList = isSurvival ? new String[]{"YELLOW_SHIELD", "HEALTH_REGEN", "HUNGER_THIRST"} :
                (isCombat ? new String[]{"DAMAGE_BOOST", "ATTACK_SPEED", "RANGE_EXTENSION"} :
                        new String[]{"ENERGY_CAPACITY", "ENERGY_EFFICIENCY", "ARMOR_ENHANCEMENT"});

        for (String u : targetList) {
            int cur = lvOf(core, u);
            int max = maxOf(core, u);
            if (cur >= max) {
                return new UpgradeValidation(false,
                        getDisplayName(u) + " 已达最大等级，无法应用套装！");
            }
        }

        UpgradeValidation result = new UpgradeValidation(true, "");
        result.upgradeType = rawType;
        return result;
    }

    // ================= 升级执行 =================

    private boolean performUpgrade(EntityPlayer player, ItemStack coreStack,
                                   ItemStack upgradeStack, ItemUpgradeComponent upgradeItem,
                                   UpgradeValidation validation) {
        String rawId = upgradeItem.getUpgradeType();
        String cid = UpgradeKeys.foldAlias(rawId);
        String registryName = upgradeItem.getRegistryName() != null ? upgradeItem.getRegistryName().toString() : "";

        if (registryName.contains("flight_module")) {
            return handleFlightModule(player, coreStack, registryName, validation.moduleLevel);
        }

        if (registryName.contains("waterproof_module") || UpgradeKeys.isWaterproof(cid)) {
            return handleWaterproofModule(player, coreStack, registryName, cid, validation.moduleLevel);
        }

        if (rawId.equalsIgnoreCase("SURVIVAL_PACKAGE")
                || rawId.equalsIgnoreCase("COMBAT_PACKAGE")
                || rawId.equalsIgnoreCase("OMNIPOTENT_PACKAGE")
                || registryName.contains("_package")
                || registryName.contains("omnipotent_package")
                || registryName.contains("omnipotent_package_chip")) {
            return handlePackageUpgrade(player, coreStack, rawId, registryName);
        }

        if (isBasicUpgrade(cid)) {
            return handleBasicUpgrade(player, coreStack, cid, validation.moduleLevel);
        } else {
            return handleExtendedUpgrade(player, coreStack, cid, validation.moduleLevel);
        }
    }

    /**
     * ✅ 基础升级处理（纯 Capability 模式）
     */
    private boolean handleBasicUpgrade(EntityPlayer player, ItemStack coreStack,
                                       String cid, int moduleLevel) {
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

        // 从 Capability 读取当前等级
        IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);
        if (data == null) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "机械核心数据未初始化！"));
            return false;
        }

        String moduleId = cid.toUpperCase();
        int cur = data.getModuleLevel(moduleId);
        int max = getMaxLevel(enumType);
        if (cur >= max) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + enumType.getDisplayName() + " 已达最大等级！"));
            return false;
        }

        int newLv = moduleLevel;

        // ✅ 记录 OriginalMax（用于修复系统）
        recordOriginalMax(coreStack, cid, newLv);
        recordOriginalMax(coreStack, enumType.getKey(), newLv);
        recordOriginalMax(coreStack, enumType.name(), newLv);

        // ✅ 纯 Capability 写入（不写 NBT）
        syncToCapability(player, cid, newLv);

        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ " + enumType.getColor() + enumType.getDisplayName() +
                        TextFormatting.WHITE + " 升级至 Lv." + newLv +
                        TextFormatting.GRAY + " (使用 " + moduleLevel + " 级模块)"
        ));
        if (newLv == max) {
            player.sendMessage(new TextComponentString(TextFormatting.GOLD + "⭐ " + enumType.getDisplayName() + " 已达到最大等级！"));
        }
        return true;
    }

    /**
     * ✅ 扩展升级处理（纯 Capability 模式）
     */
    private boolean handleExtendedUpgrade(EntityPlayer player, ItemStack coreStack,
                                          String cid, int moduleLevel) {
        unlockIfLocked(player, coreStack, cid);

        ItemMechanicalCoreExtended.UpgradeInfo info =
                ItemMechanicalCoreExtended.getUpgradeInfo(cid);
        if (info == null) info = ItemMechanicalCoreExtended.getUpgradeInfo(cid.toUpperCase(Locale.ROOT));
        if (info == null) info = ItemMechanicalCoreExtended.getUpgradeInfo(cid.toLowerCase(Locale.ROOT));
        if (info == null) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "未知的升级类型: " + cid));
            return false;
        }

        // 从 Capability 读取当前等级
        IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);
        if (data == null) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "机械核心数据未初始化！"));
            return false;
        }

        String moduleId = cid.toUpperCase();
        int cur = data.getModuleLevel(moduleId);
        int max = info.maxLevel;
        if (cur >= max) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + info.displayName + " 已达到最大等级！"));
            return false;
        }

        int newLv = moduleLevel;

        // ✅ 记录 OriginalMax（用于修复系统）
        recordOriginalMax(coreStack, cid, newLv);

        // ✅ 纯 Capability 写入（不写 NBT）
        syncToCapability(player, cid, newLv);

        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ " + info.color + info.displayName +
                        TextFormatting.WHITE + " 升级至 Lv." + newLv +
                        TextFormatting.GRAY + " (使用 " + moduleLevel + " 级模块)"
        ));
        if (newLv == max) {
            player.sendMessage(new TextComponentString(TextFormatting.GOLD + "⭐ " + info.displayName + " 已达到最大等级！"));
        }
        return true;
    }

    /**
     * ✅ 飞行模块处理（纯 Capability 模式）
     */
    private boolean handleFlightModule(EntityPlayer player, ItemStack coreStack, String registryName, int moduleLevel) {
        String cid = "FLIGHT_MODULE";
        unlockIfLocked(player, coreStack, cid);

        // 从 Capability 读取当前等级
        IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);
        if (data == null) {
            return msg(player, TextFormatting.RED + "机械核心数据未初始化！", false);
        }

        int cur = data.getModuleLevel("FLIGHT_MODULE");
        int target = moduleLevel;

        if (target <= cur) {
            return msg(player, TextFormatting.RED + "已安装相同或更高级的飞行模块！", false);
        }

        // ✅ 记录 OriginalMax（用于修复系统）
        recordOriginalMax(coreStack, cid, target);

        // ✅ 纯 Capability 写入（不写 NBT）
        syncToCapability(player, "FLIGHT_MODULE", target);

        // ✅ 保留模块配置标志（这些是设置，不是等级数据）
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(coreStack);
        nbt.setBoolean("FlightModuleEnabled", true);
        if (target >= 2 && !nbt.hasKey("FlightHoverMode")) nbt.setBoolean("FlightHoverMode", false);
        if (target >= 3 && !nbt.hasKey("CoreSpeedMode"))    nbt.setInteger("CoreSpeedMode", 0);

        switch (target) {
            case 1:
                msg(player, TextFormatting.LIGHT_PURPLE + "✦ 飞行系统已激活！" +
                        TextFormatting.GRAY + " (使用 1 级模块)", true);
                msg(player, TextFormatting.GRAY + "按住空格上升，Shift下降", true);
                break;
            case 2:
                msg(player, TextFormatting.GOLD + "✦ 飞行系统升级！悬停模式已解锁！" +
                        TextFormatting.GRAY + " (使用 2 级模块)", true);
                msg(player, TextFormatting.GRAY + "按H键切换悬停模式", true);
                break;
            case 3:
                msg(player, TextFormatting.DARK_PURPLE + "✦✦ 终极飞行系统已启动！速度模式已解锁！" +
                        TextFormatting.GRAY + " (使用 3 级模块)", true);
                msg(player, TextFormatting.GRAY + "按G键切换速度模式", true);
                break;
        }
        return true;
    }

    /**
     * ✅ 防水模块处理（纯 Capability 模式）
     */
    private boolean handleWaterproofModule(EntityPlayer player, ItemStack coreStack,
                                           String registryName, String cid, int moduleLevel) {
        cid = "WATERPROOF_MODULE";
        unlockIfLocked(player, coreStack, cid);

        // 从 Capability 读取当前等级
        IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);
        if (data == null) {
            return msg(player, TextFormatting.RED + "机械核心数据未初始化！", false);
        }

        int cur = data.getModuleLevel("WATERPROOF_MODULE");
        int target = moduleLevel;

        if (target <= cur) {
            return msg(player, TextFormatting.RED + "已安装相同或更高级的防水模块！", false);
        }

        // ✅ 记录 OriginalMax（用于修复系统）
        recordOriginalMax(coreStack, cid, target);
        recordOriginalMax(coreStack, "WATERPROOF", target);

        // ✅ 纯 Capability 写入（不写 NBT）
        syncToCapability(player, "WATERPROOF_MODULE", target);

        // ✅ 保留模块配置标志（这些是设置，不是等级数据）
        NBTTagCompound nbt = UpgradeKeys.getOrCreate(coreStack);
        nbt.setBoolean("hasWaterproofModule", target > 0);
        nbt.setInteger("waterproofLevel", target);

        switch (target) {
            case 1:
                msg(player, TextFormatting.AQUA + "💧 基础防水涂层已应用！" +
                        TextFormatting.GRAY + " (使用 1 级模块)", true);
                msg(player, TextFormatting.GRAY + "核心现在可以安全接触水体", true);
                break;
            case 2:
                msg(player, TextFormatting.BLUE + "💧 高级防水系统已安装！" +
                        TextFormatting.GRAY + " (使用 2 级模块)", true);
                msg(player, TextFormatting.GRAY + "获得水下呼吸能力", true);
                break;
            case 3:
                msg(player, TextFormatting.DARK_AQUA + "🌊 深海适应模块已激活！" +
                        TextFormatting.GRAY + " (使用 3 级模块)", true);
                msg(player, TextFormatting.GRAY + "完整的水下作业能力已解锁", true);
                break;
        }
        return true;
    }

    /**
     * ✅ 套装升级处理（纯 Capability 模式）
     */
    private boolean handlePackageUpgrade(EntityPlayer player, ItemStack core,
                                         String rawType, String registryName) {
        boolean isSurvival = rawType.equalsIgnoreCase("SURVIVAL_PACKAGE") || registryName.contains("survival_enhancement_package");
        boolean isCombat   = rawType.equalsIgnoreCase("COMBAT_PACKAGE")   || registryName.contains("combat_enhancement_package");
        boolean isOmni     = rawType.equalsIgnoreCase("OMNIPOTENT_PACKAGE")
                || registryName.contains("omnipotent_package")
                || registryName.contains("omnipotent_package_chip");

        if (!isSurvival && !isCombat && !isOmni) {
            return msg(player, TextFormatting.RED + "未知的套装类型: " + rawType, false);
        }

        String[] survivalUps = {"YELLOW_SHIELD", "HEALTH_REGEN", "HUNGER_THIRST"};
        String[] combatUps   = {"DAMAGE_BOOST", "ATTACK_SPEED", "RANGE_EXTENSION"};
        String[] omniUps     = {"ENERGY_CAPACITY", "ENERGY_EFFICIENCY", "ARMOR_ENHANCEMENT"};

        // 从 Capability 读取当前等级
        IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);
        if (data == null) {
            return msg(player, TextFormatting.RED + "机械核心数据未初始化！", false);
        }

        Map<String, Integer> before = new HashMap<>();
        String[] targetList = isSurvival ? survivalUps : (isCombat ? combatUps : omniUps);

        for (String u : targetList) {
            unlockIfLocked(player, core, u);
            String moduleId = u.toUpperCase();
            int cur = data.getModuleLevel(moduleId);
            before.put(u, cur);
            int max = maxOf(core, u);
            if (cur >= max) {
                return msg(player, TextFormatting.RED + getDisplayName(u) + " 已达最大等级，无法应用套装！", false);
            }
        }

        // 应用：全部 +1 级
        Map<String, Integer> upgradedModules = new HashMap<>();
        for (String u : targetList) {
            int newLevel = before.get(u) + 1;

            // ✅ 记录 OriginalMax（用于修复系统）
            recordOriginalMax(core, u, newLevel);

            upgradedModules.put(u, newLevel);
        }

        // ✅ 批量同步到 Capability（纯 Capability 写入，不写 NBT）
        syncMultipleToCapability(player, upgradedModules);

        if (isSurvival) {
            msg(player, TextFormatting.GREEN + "✦ 生存强化套装已应用！", true);
            msg(player, TextFormatting.YELLOW + "黄条护盾 Lv." + (before.get("YELLOW_SHIELD") + 1), true);
            msg(player, TextFormatting.RED + "生命恢复 Lv." + (before.get("HEALTH_REGEN") + 1), true);
            msg(player, TextFormatting.GREEN + "饥饿管理 Lv." + (before.get("HUNGER_THIRST") + 1), true);
        } else if (isCombat) {
            msg(player, TextFormatting.RED + "✦ 战斗强化套装已应用！", true);
            msg(player, TextFormatting.DARK_RED + "伤害提升 Lv." + (before.get("DAMAGE_BOOST") + 1), true);
            msg(player, TextFormatting.YELLOW + "攻击速度 Lv." + (before.get("ATTACK_SPEED") + 1), true);
            msg(player, TextFormatting.BLUE + "范围拓展 Lv." + (before.get("RANGE_EXTENSION") + 1), true);
        } else {
            msg(player, TextFormatting.LIGHT_PURPLE + "✦ 全能强化芯片已应用！", true);
            msg(player, TextFormatting.GOLD + "能量容量 Lv." + (before.get("ENERGY_CAPACITY") + 1), true);
            msg(player, TextFormatting.GREEN + "能量效率 Lv." + (before.get("ENERGY_EFFICIENCY") + 1), true);
            msg(player, TextFormatting.BLUE + "护甲强化 Lv." + (before.get("ARMOR_ENHANCEMENT") + 1), true);
        }

        playPackageUpgradeEffects(player);
        return true;
    }

    // ================= 工具方法 =================

    private void unlockIfLocked(EntityPlayer player, ItemStack core, String id) {
        if (UpgradeKeys.unlock(core, id)) {
            msg(player, TextFormatting.AQUA + "已修复损坏模块，允许重新安装。", true);
        }
    }

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

    private void applyUpgrade(ItemStack core, String id, int level) {
        String cid = UpgradeKeys.foldAlias(id);
        ItemMechanicalCoreExtended.setUpgradeLevel(core, cid, level);
        try {
            ItemMechanicalCore.UpgradeType t = ItemMechanicalCore.UpgradeType.valueOf(cid);
            ItemMechanicalCore.setUpgradeLevel(core, t, level);
        } catch (Throwable ignored) {}
        UpgradeKeys.setLevel(core, cid, level);
        UpgradeKeys.markOwnedActive(core, cid, level);
    }

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

    private String getDisplayName(String id) {
        ItemMechanicalCoreExtended.UpgradeInfo info = ItemMechanicalCoreExtended.getUpgradeInfo(id);
        if (info != null) return info.displayName;

        for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
            if (t.getKey().equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)) {
                return t.getDisplayName();
            }
        }

        return UpgradeKeys.canon(id).replace("_", " ");
    }

    private boolean isBasicUpgrade(String cid) {
        for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
            if (t.getKey().equalsIgnoreCase(cid) || t.name().equalsIgnoreCase(cid)) return true;
        }
        return false;
    }

    private int getWaterproofLevel(ItemStack core) {
        int lv = Math.max(lvOf(core, "WATERPROOF_MODULE"), lvOf(core, "WATERPROOF"));
        return lv;
    }

    private void setWaterproofLevel(ItemStack core, int lv) {
        ItemMechanicalCoreExtended.setUpgradeLevel(core, "WATERPROOF_MODULE", lv);
        UpgradeKeys.setLevel(core, "WATERPROOF_MODULE", lv);
        UpgradeKeys.markOwnedActive(core, "WATERPROOF_MODULE", lv);
        ItemMechanicalCoreExtended.setUpgradeLevel(core, "WATERPROOF", lv);

        NBTTagCompound nbt = UpgradeKeys.getOrCreate(core);
        nbt.setBoolean("hasWaterproofModule", lv > 0);
        nbt.setInteger("waterproofLevel", lv);
    }

    private int getFlightLevel(ItemStack core) {
        int lv = 0;
        lv = Math.max(lv, ItemMechanicalCore.getUpgradeLevel(core, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE));
        lv = Math.max(lv, ItemMechanicalCoreExtended.getUpgradeLevel(core, "FLIGHT_MODULE"));
        return lv;
    }

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

    private boolean msg(EntityPlayer p, String s, boolean ret) {
        p.sendMessage(new TextComponentString(s));
        return ret;
    }

    // ================= ✅ 新系统集成：同步到 Capability =================

    /**
     * 同步升级数据到 Capability 系统
     *
     * 桥接旧的 NBT 升级系统和新的 Capability 系统
     *
     * @param player 玩家
     * @param moduleId 模块 ID（标准化大写格式）
     * @param newLevel 新等级
     */
    private void syncToCapability(EntityPlayer player, String moduleId, int newLevel) {
        System.out.println("[SmartUpgradeHandler] syncToCapability 开始: player=" + player.getName() + ", moduleId=" + moduleId + ", newLevel=" + newLevel);

        IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);
        if (data == null) {
            System.err.println("[SmartUpgradeHandler] ❌ Capability 未附加到玩家: " + player.getName());
            return; // Capability 未附加（不应该发生）
        }

        int oldLevel = data.getModuleLevel(moduleId);
        System.out.println("[SmartUpgradeHandler] 模块等级变化: " + oldLevel + " -> " + newLevel);

        // 更新 Capability 中的模块等级
        data.setModuleLevel(moduleId, newLevel);

        // 激活模块（如果之前未激活）
        if (!data.isModuleActive(moduleId)) {
            data.setModuleActive(moduleId, true);
            System.out.println("[SmartUpgradeHandler] 激活模块: " + moduleId);
        }

        // 触发新模块系统的回调
        IMechCoreModule module = ModuleRegistry.getNew(moduleId);
        if (module != null) {
            System.out.println("[SmartUpgradeHandler] 找到模块实例: " + module.getModuleId());
            try {
                // 触发等级变化回调
                module.onLevelChanged(player, data, oldLevel, newLevel);

                // 如果是首次激活，触发激活回调
                if (oldLevel == 0) {
                    module.onActivate(player, data, newLevel);
                }
            } catch (Exception e) {
                System.err.println("[SmartUpgradeHandler] 模块回调失败: " + moduleId);
                e.printStackTrace();
            }
        } else {
            System.out.println("[SmartUpgradeHandler] ⚠️ 未找到模块实例: " + moduleId);
        }

        // 标记为需要同步到客户端
        data.markDirty();
        System.out.println("[SmartUpgradeHandler] 已标记为 dirty");

        // ✅ 立即同步到客户端（修复：升级后客户端看不到更新的问题）
        System.out.println("[SmartUpgradeHandler] 检查玩家类型: " + player.getClass().getName());
        System.out.println("[SmartUpgradeHandler] 是否为 EntityPlayerMP: " + (player instanceof EntityPlayerMP));

        if (player instanceof EntityPlayerMP) {
            System.out.println("[SmartUpgradeHandler] 准备发送网络包...");
            try {
                PacketSyncMechCoreData packet = new PacketSyncMechCoreData(data);
                System.out.println("[SmartUpgradeHandler] 网络包已创建，准备发送...");
                NetworkHandler.CHANNEL.sendTo(packet, (EntityPlayerMP) player);
                System.out.println("[SmartUpgradeHandler] ✅ 网络包已发送");
            } catch (Exception e) {
                System.err.println("[SmartUpgradeHandler] ❌ 同步 Capability 失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("[SmartUpgradeHandler] ⚠️ 玩家不是 EntityPlayerMP，跳过网络同步");
        }
    }

    /**
     * 批量同步多个模块（用于套装升级）
     */
    private void syncMultipleToCapability(EntityPlayer player, Map<String, Integer> upgrades) {
        for (Map.Entry<String, Integer> entry : upgrades.entrySet()) {
            syncToCapability(player, entry.getKey(), entry.getValue());
        }
    }
}