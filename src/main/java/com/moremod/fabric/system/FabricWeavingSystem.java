package com.moremod.fabric.system;

import com.moremod.fabric.data.UpdatedFabricPlayerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.UUID;

/**
 * 布料织入系统 - 完整保留附魔和所有NBT数据
 */
public class FabricWeavingSystem {

    // NBT标签键
    private static final String TAG_WOVEN_FABRIC = "WovenFabric";
    private static final String TAG_FABRIC_TYPE = "FabricType";
    private static final String TAG_FABRIC_UUID = "FabricUUID";
    private static final String TAG_WEAVE_TIME = "WeaveTime";
    private static final String TAG_FABRIC_POWER = "FabricPower";
    private static final String TAG_FABRIC_DATA = "FabricData";
    private static final String TAG_ORIGINAL_ENCHANTS_BACKUP = "OriginalEnchantsBackup"; // 备份用

    /**
     * 将布料织入盔甲 - 保留所有原有属性
     * @param armor 目标盔甲（会被直接修改）
     * @param fabricStack 布料物品
     * @return 是否成功
     */
    public static boolean weaveIntoArmor(ItemStack armor, ItemStack fabricStack) {
        // 验证是否为盔甲
        if (!(armor.getItem() instanceof ItemArmor)) {
            return false;
        }

        // 检查是否已有布料
        if (hasFabric(armor)) {
            return false;
        }

        // 获取布料类型
        UpdatedFabricPlayerData.FabricType fabricType = getFabricTypeFromItem(fabricStack);
        if (fabricType == null) {
            return false;
        }

        // 获取或创建NBT（保留原有的）
        NBTTagCompound armorTag = armor.getTagCompound();
        if (armorTag == null) {
            armorTag = new NBTTagCompound();
            armor.setTagCompound(armorTag);
        }

        // ===== 重要：保留原有附魔 =====
        // 创建附魔备份（仅用于记录，不移除原附魔）
        if (armor.isItemEnchanted()) {
            NBTTagList enchantments = armor.getEnchantmentTagList();
            if (enchantments != null && enchantments.tagCount() > 0) {
                // 创建备份但不移除原附魔
                armorTag.setTag(TAG_ORIGINAL_ENCHANTS_BACKUP, enchantments.copy());
            }
        }

        // 创建布料数据（独立的NBT结构）
        NBTTagCompound fabricData = new NBTTagCompound();
        fabricData.setString(TAG_FABRIC_TYPE, fabricType.getId());
        fabricData.setString(TAG_FABRIC_UUID, UUID.randomUUID().toString());
        fabricData.setLong(TAG_WEAVE_TIME, System.currentTimeMillis());
        fabricData.setInteger(TAG_FABRIC_POWER, 100);

        // 根据布料类型添加特殊数据
        addFabricSpecificData(fabricData, fabricType);

        // 将布料数据添加到盔甲NBT（不影响其他NBT）
        armorTag.setTag(TAG_WOVEN_FABRIC, fabricData);

        // 添加视觉标记
        armorTag.setBoolean("HasFabric", true);
        armorTag.setInteger("FabricColor", fabricType.getColor());

        // 修改显示名称（保留原名）
        updateArmorDisplayName(armor, fabricType);

        // ===== 验证附魔是否保留 =====
        if (armorTag.hasKey("ench")) {
            System.out.println("[Fabric] Preserved " + armorTag.getTagList("ench", 10).tagCount() + " enchantments");
        }

        return true;
    }

    /**
     * 更新盔甲显示名称（保留原名）
     */
    private static void updateArmorDisplayName(ItemStack armor, UpdatedFabricPlayerData.FabricType type) {
        NBTTagCompound tag = armor.getTagCompound();
        if (tag == null) return;

        // 获取或创建display标签
        NBTTagCompound display;
        if (tag.hasKey("display")) {
            display = tag.getCompoundTag("display");
        } else {
            display = new NBTTagCompound();
            tag.setTag("display", display);
        }

        // 获取当前名称
        String currentName;
        if (display.hasKey("Name")) {
            currentName = display.getString("Name");
            // 保存原始名称
            if (!display.hasKey("OriginalName")) {
                display.setString("OriginalName", currentName);
            }
        } else {
            currentName = armor.getItem().getItemStackDisplayName(armor);
        }

        // 添加前缀
        String prefix = getColoredPrefix(type);
        String newName = prefix + currentName;
        display.setString("Name", newName);
    }

    /**
     * 移除布料（恢复原始状态）
     */
    public static boolean removeFabric(ItemStack armor) {
        if (!hasFabric(armor)) return false;

        NBTTagCompound tag = armor.getTagCompound();
        if (tag == null) return false;

        // 移除布料相关标签
        tag.removeTag(TAG_WOVEN_FABRIC);
        tag.removeTag("HasFabric");
        tag.removeTag("FabricColor");
        tag.removeTag(TAG_ORIGINAL_ENCHANTS_BACKUP);

        // 恢复原始名称
        if (tag.hasKey("display")) {
            NBTTagCompound display = tag.getCompoundTag("display");
            if (display.hasKey("OriginalName")) {
                display.setString("Name", display.getString("OriginalName"));
                display.removeTag("OriginalName");
            } else {
                // 如果没有原始名称记录，移除Name标签
                display.removeTag("Name");
            }

            // 如果display标签空了，移除它
            if (display.getKeySet().isEmpty()) {
                tag.removeTag("display");
            }
        }

        // 附魔保持不变（因为我们从未移除它们）

        return true;
    }

    /**
     * 检查盔甲是否有布料
     */
    public static boolean hasFabric(ItemStack armor) {
        if (armor.isEmpty() || !(armor.getItem() instanceof ItemArmor)) {
            return false;
        }

        NBTTagCompound tag = armor.getTagCompound();
        return tag != null && tag.hasKey(TAG_WOVEN_FABRIC);
    }

    /**
     * 获取盔甲的布料类型
     */
    public static UpdatedFabricPlayerData.FabricType getFabricType(ItemStack armor) {
        if (!hasFabric(armor)) return null;

        NBTTagCompound tag = armor.getTagCompound();
        if (tag == null) return null;

        NBTTagCompound fabricData = tag.getCompoundTag(TAG_WOVEN_FABRIC);
        String typeId = fabricData.getString(TAG_FABRIC_TYPE);

        for (UpdatedFabricPlayerData.FabricType type : UpdatedFabricPlayerData.FabricType.values()) {
            if (type.getId().equals(typeId)) {
                return type;
            }
        }

        return null;
    }

    /**
     * 获取布料数据
     */
    public static NBTTagCompound getFabricData(ItemStack armor) {
        if (!hasFabric(armor)) return new NBTTagCompound();

        NBTTagCompound tag = armor.getTagCompound();
        if (tag == null) return new NBTTagCompound();

        return tag.getCompoundTag(TAG_WOVEN_FABRIC);
    }

    /**
     * 从物品获取布料类型
     */
    private static UpdatedFabricPlayerData.FabricType getFabricTypeFromItem(ItemStack item) {
        String itemName = item.getItem().getRegistryName().toString();

        // 高级织布
        if (itemName.contains("abyssal_fabric") ) {
            return UpdatedFabricPlayerData.FabricType.ABYSS;
        }
        if (itemName.contains("chrono_fabric") ) {
            return UpdatedFabricPlayerData.FabricType.TEMPORAL;
        }
        if (itemName.contains("spacetime_fabric") ) {
            return UpdatedFabricPlayerData.FabricType.SPATIAL;
        }
        if (itemName.contains("otherworldly_fiber") ) {
            return UpdatedFabricPlayerData.FabricType.OTHERWORLD;
        }

        // 基础织布
        if (itemName.contains("resilient_fiber") ) {
            return UpdatedFabricPlayerData.FabricType.RESILIENT;
        }
        if (itemName.contains("vital_thread") ) {
            return UpdatedFabricPlayerData.FabricType.VITAL;
        }
        if (itemName.contains("light_weave") ) {
            return UpdatedFabricPlayerData.FabricType.LIGHT;
        }
        if (itemName.contains("predator_cloth") ) {
            return UpdatedFabricPlayerData.FabricType.PREDATOR;
        }
        if (itemName.contains("siphon_wrap") ) {
            return UpdatedFabricPlayerData.FabricType.SIPHON;
        }

        return null;
    }

    /**
     * 添加布料特定数据
     */
    private static void addFabricSpecificData(NBTTagCompound data, UpdatedFabricPlayerData.FabricType type) {
        switch (type) {
            // 高级织布
            case ABYSS:
                data.setInteger("AbyssKills", 0);
                data.setFloat("AbyssPower", 0);
                data.setLong("LastKillTime", 0);
                break;
            case TEMPORAL:
                data.setInteger("RewindCount", 0);
                data.setFloat("TemporalEnergy", 100);
                data.setLong("LastTimeStop", 0);
                break;
            case SPATIAL:
                data.setFloat("StoredDamage", 0);
                data.setInteger("PhaseStrikeCount", 0);
                data.setFloat("DimensionalEnergy", 100);
                break;
            case OTHERWORLD:
                data.setInteger("Insight", 0);
                data.setInteger("Sanity", 100);
                data.setInteger("ForbiddenKnowledge", 0);
                break;

            // 基础织布（简单数据）
            case RESILIENT:
                // 坚韧纤维：无特殊数据，纯属性加成
                break;
            case VITAL:
                data.setLong("LastRegenTick", 0);
                break;
            case LIGHT:
                // 轻盈织物：无特殊数据，纯属性加成
                break;
            case PREDATOR:
                data.setLong("LastBleedTime", 0);
                break;
            case SIPHON:
                data.setInteger("TotalKills", 0);
                break;
        }
    }

    /**
     * 获取彩色前缀
     */
    private static String getColoredPrefix(UpdatedFabricPlayerData.FabricType type) {
        switch (type) {
            // 高级织布
            case ABYSS:
                return TextFormatting.DARK_RED + "[深渊] " + TextFormatting.RESET;
            case TEMPORAL:
                return TextFormatting.AQUA + "[时序] " + TextFormatting.RESET;
            case SPATIAL:
                return TextFormatting.LIGHT_PURPLE + "[时空] " + TextFormatting.RESET;
            case OTHERWORLD:
                return TextFormatting.DARK_PURPLE + "[异界] " + TextFormatting.RESET;

            // 基础织布
            case RESILIENT:
                return TextFormatting.GRAY + "[坚韧] " + TextFormatting.RESET;
            case VITAL:
                return TextFormatting.LIGHT_PURPLE + "[活力] " + TextFormatting.RESET;
            case LIGHT:
                return TextFormatting.AQUA + "[轻盈] " + TextFormatting.RESET;
            case PREDATOR:
                return TextFormatting.RED + "[掠食] " + TextFormatting.RESET;
            case SIPHON:
                return TextFormatting.DARK_GREEN + "[吸魂] " + TextFormatting.RESET;
            default:
                return "";
        }
    }

    /**
     * 更新布料数据（保持其他NBT不变）
     */
    public static void updateFabricData(ItemStack armor, NBTTagCompound newData) {
        if (!hasFabric(armor)) return;

        NBTTagCompound tag = armor.getTagCompound();
        if (tag == null) return;

        NBTTagCompound fabricData = tag.getCompoundTag(TAG_WOVEN_FABRIC);

        // 合并新数据到布料数据中
        for (String key : newData.getKeySet()) {
            // 复制各种类型的NBT标签
            fabricData.setTag(key, newData.getTag(key).copy());
        }

        tag.setTag(TAG_WOVEN_FABRIC, fabricData);
    }

    /**
     * 获取玩家装备的布料件数
     */
    public static int countPlayerFabric(EntityPlayer player, UpdatedFabricPlayerData.FabricType type) {
        int count = 0;
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (getFabricType(armor) == type) {
                count++;
            }
        }
        return count;
    }

    /**
     * 调试方法：打印物品的所有NBT
     */
    public static void debugPrintNBT(ItemStack stack) {
        if (stack.hasTagCompound()) {
            System.out.println("[Fabric Debug] Item: " + stack.getDisplayName());
            System.out.println("[Fabric Debug] NBT: " + stack.getTagCompound().toString());
            if (stack.isItemEnchanted()) {
                System.out.println("[Fabric Debug] Enchantments: " + stack.getEnchantmentTagList());
            }
        }
    }
}