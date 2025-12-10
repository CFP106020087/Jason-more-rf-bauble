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
 * 打印模版物品
 * 从废墟中获取，用于打印机打印特定物品
 */
public class ItemPrintTemplate extends Item {

    // 预定义的模版类型
    public static final String[] TEMPLATE_TYPES = {
        "gear_iron",           // 铁齿轮
        "ancient_component",   // 远古组件
        "mystery_crystal",     // 神秘水晶
        "spacetime_shard",     // 时空碎片
        "ancient_core_fragment" // 远古核心碎片
    };

    public ItemPrintTemplate() {
        setMaxStackSize(1);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("print_template");
        setTranslationKey("print_template");
        setHasSubtypes(true);
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
        // 使用damage值作为模版类型索引
        int meta = stack.getMetadata();
        if (meta >= 0 && meta < TEMPLATE_TYPES.length) {
            return TEMPLATE_TYPES[meta];
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

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            for (int i = 0; i < TEMPLATE_TYPES.length; i++) {
                ItemStack stack = new ItemStack(this, 1, i);
                setTemplateId(stack, TEMPLATE_TYPES[i]);
                items.add(stack);
            }
        }
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        String templateId = getTemplateId(stack);
        if (!templateId.isEmpty()) {
            return "item.moremod.print_template." + templateId;
        }
        return super.getTranslationKey(stack);
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
        }
        tooltip.add("");
        tooltip.add("\u00a78" + I18n.format("tooltip.moremod.template_desc"));
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true;  // 发光效果
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
