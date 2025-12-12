package com.moremod.printer;

import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义打印模版物品
 *
 * 与普通打印模版不同，此模版将配方数据（能量、材料、产出）直接存储在物品NBT中
 * 无需预先在配方注册表中注册，适用于一次性或动态生成的模版
 *
 * NBT结构:
 * - DisplayName: 显示名称
 * - EnergyCost: 能量消耗 (RF)
 * - ProcessingTime: 处理时间 (ticks)
 * - Output: 输出物品 (ItemStack NBT)
 * - Materials: 材料列表 (NBTTagList)
 *
 * CRT使用方法:
 *
 * // 创建自定义模版
 * val customTemplate = mods.moremod.Printer.createCustomTemplate(
 *     "我的自定义模版",           // 显示名称
 *     <minecraft:diamond> * 5,   // 输出物品
 *     50000,                     // 能量消耗 (RF)
 *     100,                       // 处理时间 (ticks)
 *     [<minecraft:iron_ingot> * 4, <minecraft:gold_ingot> * 2]  // 材料
 * );
 *
 * // 将自定义模版作为战利品或合成产物使用
 * recipes.addShapeless("get_custom_template", customTemplate,
 *     [<minecraft:paper>, <minecraft:diamond>]);
 */
public class ItemCustomPrintTemplate extends Item {

    // NBT键名常量
    public static final String NBT_DISPLAY_NAME = "DisplayName";
    public static final String NBT_ENERGY_COST = "EnergyCost";
    public static final String NBT_PROCESSING_TIME = "ProcessingTime";
    public static final String NBT_OUTPUT = "Output";
    public static final String NBT_MATERIALS = "Materials";
    public static final String NBT_RARITY = "Rarity";

    public ItemCustomPrintTemplate() {
        setMaxStackSize(1);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("custom_print_template");
        setTranslationKey("custom_print_template");
    }

    // ==================== NBT读写方法 ====================

    /**
     * 获取显示名称
     */
    public static String getDisplayName(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey(NBT_DISPLAY_NAME)) {
            return nbt.getString(NBT_DISPLAY_NAME);
        }
        return "";
    }

    /**
     * 设置显示名称
     */
    public static void setDisplayName(ItemStack stack, String name) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        nbt.setString(NBT_DISPLAY_NAME, name);
    }

    /**
     * 获取能量消耗
     */
    public static int getEnergyCost(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey(NBT_ENERGY_COST)) {
            return nbt.getInteger(NBT_ENERGY_COST);
        }
        return 0;
    }

    /**
     * 设置能量消耗
     */
    public static void setEnergyCost(ItemStack stack, int energy) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        nbt.setInteger(NBT_ENERGY_COST, energy);
    }

    /**
     * 获取处理时间
     */
    public static int getProcessingTime(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey(NBT_PROCESSING_TIME)) {
            return nbt.getInteger(NBT_PROCESSING_TIME);
        }
        return 200; // 默认10秒
    }

    /**
     * 设置处理时间
     */
    public static void setProcessingTime(ItemStack stack, int ticks) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        nbt.setInteger(NBT_PROCESSING_TIME, ticks);
    }

    /**
     * 获取输出物品
     */
    public static ItemStack getOutput(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey(NBT_OUTPUT)) {
            return new ItemStack(nbt.getCompoundTag(NBT_OUTPUT));
        }
        return ItemStack.EMPTY;
    }

    /**
     * 设置输出物品
     */
    public static void setOutput(ItemStack stack, ItemStack output) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        NBTTagCompound outputNbt = new NBTTagCompound();
        output.writeToNBT(outputNbt);
        nbt.setTag(NBT_OUTPUT, outputNbt);
    }

    /**
     * 获取材料列表
     */
    public static List<ItemStack> getMaterials(ItemStack stack) {
        List<ItemStack> materials = new ArrayList<>();
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey(NBT_MATERIALS)) {
            NBTTagList list = nbt.getTagList(NBT_MATERIALS, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                ItemStack material = new ItemStack(list.getCompoundTagAt(i));
                if (!material.isEmpty()) {
                    materials.add(material);
                }
            }
        }
        return materials;
    }

    /**
     * 设置材料列表
     */
    public static void setMaterials(ItemStack stack, List<ItemStack> materials) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        NBTTagList list = new NBTTagList();
        for (ItemStack material : materials) {
            if (!material.isEmpty()) {
                NBTTagCompound materialNbt = new NBTTagCompound();
                material.writeToNBT(materialNbt);
                list.appendTag(materialNbt);
            }
        }
        nbt.setTag(NBT_MATERIALS, list);
    }

    /**
     * 添加单个材料
     */
    public static void addMaterial(ItemStack stack, ItemStack material) {
        List<ItemStack> materials = getMaterials(stack);
        materials.add(material);
        setMaterials(stack, materials);
    }

    /**
     * 获取稀有度字符串
     */
    public static String getRarityString(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey(NBT_RARITY)) {
            return nbt.getString(NBT_RARITY);
        }
        return "rare";
    }

    /**
     * 设置稀有度
     */
    public static void setRarityString(ItemStack stack, String rarity) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        nbt.setString(NBT_RARITY, rarity);
    }

    // ==================== 辅助方法 ====================

    private static NBTTagCompound getOrCreateNBT(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        return nbt;
    }

    /**
     * 检查模版是否有效（包含必要数据）
     */
    public static boolean isValidTemplate(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemCustomPrintTemplate)) {
            return false;
        }
        ItemStack output = getOutput(stack);
        return !output.isEmpty() && getEnergyCost(stack) > 0;
    }

    /**
     * 检查物品是否是自定义模版
     */
    public static boolean isCustomTemplate(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemCustomPrintTemplate;
    }

    /**
     * 创建完整的自定义模版
     */
    public static ItemStack createTemplate(Item templateItem, String displayName, ItemStack output,
                                            int energyCost, int processingTime, List<ItemStack> materials) {
        ItemStack stack = new ItemStack(templateItem);
        setDisplayName(stack, displayName);
        setOutput(stack, output);
        setEnergyCost(stack, energyCost);
        setProcessingTime(stack, processingTime);
        setMaterials(stack, materials);
        return stack;
    }

    // ==================== Item覆写 ====================

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String customName = getDisplayName(stack);
        if (!customName.isEmpty()) {
            return customName;
        }
        ItemStack output = getOutput(stack);
        if (!output.isEmpty()) {
            return "自定义模版: " + output.getDisplayName();
        }
        return "空白自定义模版";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (!isValidTemplate(stack)) {
            tooltip.add(TextFormatting.RED + "⚠ 未配置的空白模版");
            tooltip.add(TextFormatting.GRAY + "使用CraftTweaker配置此模版");
            return;
        }

        ItemStack output = getOutput(stack);
        int energy = getEnergyCost(stack);
        int time = getProcessingTime(stack);
        List<ItemStack> materials = getMaterials(stack);

        tooltip.add(TextFormatting.AQUA + "=== 自定义打印模版 ===");
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "产出: " + TextFormatting.GREEN + output.getDisplayName() +
                   (output.getCount() > 1 ? " x" + output.getCount() : ""));
        tooltip.add(TextFormatting.GRAY + "能量: " + TextFormatting.RED + formatEnergy(energy) + " RF");
        tooltip.add(TextFormatting.GRAY + "时间: " + TextFormatting.YELLOW + String.format("%.1f秒", time / 20.0));

        if (!materials.isEmpty()) {
            tooltip.add("");
            tooltip.add(TextFormatting.GRAY + "所需材料:");
            for (ItemStack material : materials) {
                tooltip.add(TextFormatting.DARK_GRAY + "  • " + TextFormatting.WHITE +
                           material.getDisplayName() +
                           (material.getCount() > 1 ? " x" + material.getCount() : ""));
            }
        }
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        String rarity = getRarityString(stack);
        switch (rarity.toLowerCase()) {
            case "common": return EnumRarity.COMMON;
            case "uncommon": return EnumRarity.UNCOMMON;
            case "epic": return EnumRarity.EPIC;
            default: return EnumRarity.RARE;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return isValidTemplate(stack);
    }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.1fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fk", energy / 1000.0);
        }
        return String.valueOf(energy);
    }
}
