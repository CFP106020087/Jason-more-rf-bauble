package com.moremod.printer;

import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 通用打印模版物品
 * 用于CraftTweaker定义的打印机配方
 *
 * 使用方法:
 * 1. 通过CraftTweaker添加配方: mods.moremod.Printer.addRecipe(...)
 * 2. 通过CraftTweaker创建模板物品: mods.moremod.Printer.createTemplate(templateId)
 * 3. 将模板放入打印机即可打印对应物品
 */
public class ItemPrintTemplate extends Item {

    public ItemPrintTemplate() {
        setMaxStackSize(1);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("print_template");
        setTranslationKey("print_template");
        setHasSubtypes(false);
    }

    /**
     * 获取模版ID
     */
    public static String getTemplateId(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemPrintTemplate)) {
            return "";
        }
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey("TemplateId")) {
            return nbt.getString("TemplateId");
        }
        return "";
    }

    /**
     * 设置模版ID
     */
    public static ItemStack setTemplateId(ItemStack stack, String templateId) {
        if (stack.isEmpty()) return stack;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        nbt.setString("TemplateId", templateId);
        return stack;
    }

    /**
     * 创建带有指定模版ID的物品
     */
    public static ItemStack createTemplate(Item templateItem, String templateId) {
        ItemStack stack = new ItemStack(templateItem);
        return setTemplateId(stack, templateId);
    }

    /**
     * 创建带有指定模版ID的物品（静态便捷方法）
     */
    public static ItemStack createWithId(String templateId) {
        ItemStack stack = new ItemStack(com.moremod.init.ModItems.PRINT_TEMPLATE);
        return setTemplateId(stack, templateId);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            // 在创造模式标签页显示所有已注册的配方模板
            for (PrinterRecipe recipe : PrinterRecipeRegistry.getAllRecipes()) {
                ItemStack stack = new ItemStack(this);
                setTemplateId(stack, recipe.getTemplateId());
                items.add(stack);
            }
            // 如果没有任何配方，添加一个空模板作为示例
            if (PrinterRecipeRegistry.getAllRecipes().isEmpty()) {
                items.add(new ItemStack(this));
            }
        }
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        // 统一使用通用翻译键
        return "item.moremod.print_template";
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String templateId = getTemplateId(stack);
        if (!templateId.isEmpty()) {
            // 尝试获取配方的自定义名称
            PrinterRecipe recipe = PrinterRecipeRegistry.getRecipe(templateId);
            if (recipe != null && recipe.getDisplayName() != null && !recipe.getDisplayName().isEmpty()) {
                return recipe.getDisplayName();
            }
            // 否则显示 "打印模板: <templateId>"
            return I18n.format("item.moremod.print_template.name") + ": " + templateId;
        }
        return I18n.format("item.moremod.print_template.name");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        String templateId = getTemplateId(stack);
        if (!templateId.isEmpty()) {
            tooltip.add("\u00a77" + I18n.format("tooltip.moremod.template_id") + ": \u00a7e" + templateId);

            PrinterRecipe recipe = PrinterRecipeRegistry.getRecipe(templateId);
            if (recipe != null) {
                tooltip.add("\u00a77" + I18n.format("tooltip.moremod.output") + ": \u00a7a" + recipe.getOutput().getDisplayName());
                tooltip.add("\u00a77" + I18n.format("tooltip.moremod.energy_cost") + ": \u00a7c" + formatEnergy(recipe.getEnergyCost()) + " RF");
                tooltip.add("\u00a77" + I18n.format("tooltip.moremod.processing_time") + ": \u00a7b" + (recipe.getProcessingTime() / 20.0) + "s");
            } else {
                tooltip.add("\u00a7c" + I18n.format("tooltip.moremod.unknown_recipe"));
            }
        } else {
            tooltip.add("\u00a7c" + I18n.format("tooltip.moremod.no_template_id"));
        }
        tooltip.add("");
        tooltip.add("\u00a78" + I18n.format("tooltip.moremod.template_desc"));
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        String templateId = getTemplateId(stack);
        if (!templateId.isEmpty()) {
            PrinterRecipe recipe = PrinterRecipeRegistry.getRecipe(templateId);
            if (recipe != null) {
                return recipe.getRarity();
            }
        }
        return EnumRarity.RARE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        String templateId = getTemplateId(stack);
        return !templateId.isEmpty();  // 只有设置了模板ID才发光
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
