package com.moremod.accessorybox;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.moremod;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemAccessoryBox extends Item {

    private final int tier; // 1=基础, 2=进阶, 3=高级

    // 三个等级的构造函数
    public ItemAccessoryBox(int tier) {
        this.tier = tier;
        String tierName = getTierName();
        this.setTranslationKey("accessory_box_" + tierName);
        this.setRegistryName("accessory_box_" + tierName);
        this.setMaxStackSize(1);
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    // 用于兼容旧版本的构造函数（默认为T3）
    public ItemAccessoryBox() {
        this(3);
    }

    private String getTierName() {
        switch(tier) {
            case 1: return "t1";
            case 2: return "t2";
            case 3: return "t3";
            default: return "t3";
        }
    }

    public int getTier() {
        return tier;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);

        if (!worldIn.isRemote && handIn == EnumHand.MAIN_HAND) {
            // 标记使用箱子类型
            playerIn.getEntityData().setInteger("AccessoryBoxTier", tier);

            // 打开GUI - 根据tier使用不同的GUI ID
            int guiId;
            switch(tier) {
                case 1: guiId = 11; break;  // ACCESSORY_BOX_T1_GUI
                case 2: guiId = 12; break;  // ACCESSORY_BOX_T2_GUI
                case 3: guiId = 13; break;  // ACCESSORY_BOX_T3_GUI
                default: guiId = 13; break; // 默认T3
            }
            playerIn.openGui(moremod.INSTANCE, guiId, worldIn, 0, 0, 0);

            // 播放音效
            worldIn.playSound(null, playerIn.posX, playerIn.posY, playerIn.posZ,
                    SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.PLAYERS,
                    0.5F, worldIn.rand.nextFloat() * 0.1F + 0.9F);
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        switch(tier) {
            case 1: return EnumRarity.UNCOMMON; // 绿色
            case 2: return EnumRarity.RARE;     // 蓝色
            case 3: return EnumRarity.EPIC;     // 紫色
            default: return EnumRarity.COMMON;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return tier >= 2; // T2和T3有附魔光效
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        switch(tier) {
            case 1:
                tooltip.add(TextFormatting.GRAY + "从盒子的锁眼中看去，隐约能见到平行世界中佩戴不同饰品套装的你");
                tooltip.add("");
                tooltip.add(TextFormatting.GRAY + "让你佩戴:");
                tooltip.add(TextFormatting.GREEN + " • 额外1个戒指");
                tooltip.add(TextFormatting.GREEN + " • 额外1个护身符");
                tooltip.add(TextFormatting.GREEN + " • 额外1个腰带");
                tooltip.add("");
                tooltip.add(TextFormatting.YELLOW + "必须时刻观测才能拓展");
                break;

            case 2:
                tooltip.add(TextFormatting.BLUE + "从盒子的锁眼中看去，隐约能见到平行世界中佩戴不同饰品套装的你");
                tooltip.add("");
                tooltip.add(TextFormatting.GRAY + "让你佩戴:");
                tooltip.add(TextFormatting.AQUA + " • 额外1个戒指");
                tooltip.add(TextFormatting.AQUA + " • 额外1个护身符");
                tooltip.add(TextFormatting.AQUA + " • 额外1个腰带");
                tooltip.add(TextFormatting.AQUA + " • 额外1个坠饰");
                tooltip.add(TextFormatting.AQUA + " • 额外1个头部");
                tooltip.add("");
                tooltip.add(TextFormatting.YELLOW + "必须时刻观测才能拓展");
                break;

            case 3:
                tooltip.add(TextFormatting.LIGHT_PURPLE + "从盒子的锁眼中看去，隐约能见到平行世界中佩戴不同饰品套装的你");
                tooltip.add("");
                tooltip.add(TextFormatting.GRAY + "让你佩戴:");
                tooltip.add(TextFormatting.YELLOW + " • 额外1个项链");
                tooltip.add(TextFormatting.YELLOW + " • 额外1个戒指");
                tooltip.add(TextFormatting.YELLOW + " • 额外1个腰带");
                tooltip.add(TextFormatting.YELLOW + " • 额外1个头部");
                tooltip.add(TextFormatting.YELLOW + " • 额外1个坠饰");
                tooltip.add(TextFormatting.YELLOW + " • 额外1个身体");

                tooltip.add(TextFormatting.GOLD + " • 额外2个万能槽位(可放任何饰品)");
                tooltip.add("");
                tooltip.add(TextFormatting.RED + "必须时刻观测才能拓展!");
                break;
        }
    }
}