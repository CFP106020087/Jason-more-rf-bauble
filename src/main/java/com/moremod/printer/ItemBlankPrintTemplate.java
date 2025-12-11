package com.moremod.printer;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.init.ModItems;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 空白打印模版物品
 *
 * 这是一个未定义的模版，必须通过CraftTweaker脚本来定义其用途。
 *
 * CRT使用方法:
 *
 * // 1. 首先定义一个打印配方
 * mods.moremod.Printer.addRecipe("my_custom_item", <minecraft:diamond>, 100000, 200,
 *     [<minecraft:iron_ingot> * 4, <minecraft:gold_ingot> * 2]);
 *
 * // 2. 定义空白模版的合成配方 (将空白模版转换为特定模版)
 * val blankTemplate = <moremod:blank_print_template>;
 * val definedTemplate = mods.moremod.Printer.createTemplate("my_custom_item");
 *
 * // 3. 添加合成配方 (空白模版 + 材料 = 定义好的模版)
 * recipes.addShaped("define_my_template", definedTemplate,
 *     [[<minecraft:paper>, <minecraft:diamond>, <minecraft:paper>],
 *      [<minecraft:redstone>, blankTemplate, <minecraft:redstone>],
 *      [<minecraft:paper>, <minecraft:gold_ingot>, <minecraft:paper>]]);
 *
 * // 或者使用快捷方法直接创建带合成的模版
 * mods.moremod.Printer.defineTemplate("my_recipe", blankTemplate,
 *     [[<minecraft:paper>, <minecraft:diamond>, <minecraft:paper>],
 *      [<minecraft:redstone>, blankTemplate, <minecraft:redstone>],
 *      [<minecraft:paper>, <minecraft:gold_ingot>, <minecraft:paper>]]);
 */
public class ItemBlankPrintTemplate extends Item {

    public ItemBlankPrintTemplate() {
        setMaxStackSize(16);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("blank_print_template");
        setTranslationKey("blank_print_template");
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return I18n.format("item.moremod.blank_print_template.name");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.moremod.blank_template.desc1"));
        tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.moremod.blank_template.desc2"));
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_AQUA + I18n.format("tooltip.moremod.blank_template.hint"));

        // 显示当前已注册的配方数量
        int recipeCount = PrinterRecipeRegistry.getRecipeCount();
        if (recipeCount > 0) {
            tooltip.add("");
            tooltip.add(TextFormatting.GREEN + I18n.format("tooltip.moremod.blank_template.available", recipeCount));
        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.RED + I18n.format("tooltip.moremod.blank_template.no_recipes"));
        }
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return false; // 空白模版不发光
    }

    /**
     * 右键显示可用配方列表
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        if (!worldIn.isRemote && playerIn.isSneaking()) {
            // Shift+右键显示可用配方
            java.util.Collection<PrinterRecipe> recipes = PrinterRecipeRegistry.getAllRecipes();
            if (recipes.isEmpty()) {
                playerIn.sendMessage(new TextComponentString(
                    TextFormatting.RED + I18n.format("message.moremod.no_printer_recipes")));
            } else {
                playerIn.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "=== " + I18n.format("message.moremod.available_recipes") + " ==="));
                for (PrinterRecipe recipe : recipes) {
                    String name = recipe.getDisplayName().isEmpty() ?
                        recipe.getTemplateId() : recipe.getDisplayName();
                    String output = recipe.getOutput().getDisplayName();
                    playerIn.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "- " + name +
                        TextFormatting.GRAY + " -> " +
                        TextFormatting.GREEN + output));
                }
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
    }

    /**
     * 检查物品是否是空白模版
     */
    public static boolean isBlankTemplate(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof ItemBlankPrintTemplate;
    }

    /**
     * 将空白模版转换为定义好的打印模版
     * @param blankStack 空白模版
     * @param templateId 要设置的模版ID
     * @return 新的打印模版物品
     */
    public static ItemStack convertToDefinedTemplate(ItemStack blankStack, String templateId) {
        if (!isBlankTemplate(blankStack)) {
            return ItemStack.EMPTY;
        }

        // 检查配方是否存在
        if (!PrinterRecipeRegistry.hasRecipe(templateId)) {
            System.out.println("[Printer] 警告: 尝试转换到不存在的配方: " + templateId);
            return ItemStack.EMPTY;
        }

        // 创建新的定义模版
        if (ModItems.PRINT_TEMPLATE != null) {
            return ItemPrintTemplate.createTemplate(ModItems.PRINT_TEMPLATE, templateId);
        }

        return ItemStack.EMPTY;
    }
}
