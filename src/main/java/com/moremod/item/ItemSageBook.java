package com.moremod.item;

import com.moremod.moremod;
import com.moremod.client.gui.GuiHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.EnumRarity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemSageBook extends Item {

    public ItemSageBook() {
        this.setRegistryName("sage_book");
        this.setTranslationKey("moremod.sage_book");
        this.setCreativeTab(CreativeTabs.MISC);
        this.setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!world.isRemote) {
            // 打开GUI - 但不消耗物品！
            player.openGui(moremod.instance, GuiHandler.GUI_SAGE_BOOK, world,
                    hand == EnumHand.MAIN_HAND ? 1 : 0,  // 传递手的信息
                    (int)player.posY,
                    (int)player.posZ);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        // 主标题
        tooltip.add(TextFormatting.LIGHT_PURPLE + "『远古智慧的结晶』");
        tooltip.add("");

        // 描述
        tooltip.add(TextFormatting.GRAY + "" + TextFormatting.ITALIC +
                "千年的知识由微微飘逸的书页中，缓缓溢出...");
        tooltip.add("");

        // 功能说明
        tooltip.add(TextFormatting.GOLD + "✦ " + TextFormatting.YELLOW +
                "能够承载三种魔法之力的融合");
        tooltip.add(TextFormatting.GOLD + "✦ " + TextFormatting.YELLOW +
                "突破常规附魔的限制");


        // 检查是否已经储存了附魔
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey("StoredEnchantments")) {
            NBTTagList enchantments = nbt.getTagList("StoredEnchantments", 10);
            if (enchantments.tagCount() > 0) {
                tooltip.add("");
                tooltip.add(TextFormatting.AQUA + "═══ 储存的魔法 ═══");

                for (int i = 0; i < Math.min(3, enchantments.tagCount()); i++) {
                    NBTTagCompound enchTag = enchantments.getCompoundTagAt(i);
                    int enchId = enchTag.getShort("id");
                    int level = enchTag.getShort("lvl");
                    net.minecraft.enchantment.Enchantment ench =
                            net.minecraft.enchantment.Enchantment.getEnchantmentByID(enchId);

                    if (ench != null) {
                        String enchName = ench.getTranslatedName(level);
                        tooltip.add(TextFormatting.DARK_AQUA + "  ◈ " +
                                TextFormatting.AQUA + enchName);
                    }
                }

                // 魔法融合度
                int power = enchantments.tagCount();
                String fusionLevel = power >= 3 ? "完美融合" :
                        power == 2 ? "双重共鸣" : "单一魔法";
                tooltip.add("");
                tooltip.add(TextFormatting.DARK_PURPLE + "融合状态: " +
                        TextFormatting.LIGHT_PURPLE + fusionLevel);
            }
        } else {
            // 空白状态
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC +
                    "〈 空白的书页等待书写 〉");
        }


    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        // 如果有储存的附魔，显示附魔光效
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey("StoredEnchantments");
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        // 根据储存的附魔数量决定稀有度
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey("StoredEnchantments")) {
            NBTTagList enchantments = nbt.getTagList("StoredEnchantments", 10);
            if (enchantments.tagCount() >= 3) {
                return EnumRarity.EPIC; // 紫色
            } else if (enchantments.tagCount() >= 2) {
                return EnumRarity.RARE; // 蓝色
            }
        }
        return EnumRarity.UNCOMMON; // 黄色
    }

    // 检查是否按住Shift键的辅助方法
    @SideOnly(Side.CLIENT)
    private static boolean isShiftKeyDown() {
        return net.minecraft.client.Minecraft.getMinecraft().gameSettings.keyBindSneak.isKeyDown();
    }

    /**
     * 使物品名称根据状态变色
     */
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String baseName = super.getItemStackDisplayName(stack);

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey("StoredEnchantments")) {
            NBTTagList enchantments = nbt.getTagList("StoredEnchantments", 10);
            if (enchantments.tagCount() >= 3) {
                // 三种附魔 - 彩虹色名称
                return TextFormatting.GOLD + "" + TextFormatting.BOLD + baseName +
                        TextFormatting.RESET + TextFormatting.LIGHT_PURPLE + " ✦";
            } else if (enchantments.tagCount() >= 2) {
                // 两种附魔
                return TextFormatting.AQUA + "" + TextFormatting.BOLD + baseName;
            } else if (enchantments.tagCount() >= 1) {
                // 一种附魔
                return TextFormatting.YELLOW + baseName;
            }
        }

        return baseName;
    }
}