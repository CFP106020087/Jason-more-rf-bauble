package com.moremod.system.humanity.intel;

import com.moremod.system.humanity.BiologicalProfile;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 情报整合统计书
 * Intel Statistics Book
 *
 * 显示玩家当前的情报系统状态：
 * - 已激活的档案及其加成
 * - 总体伤害/掉落加成
 * - 已记录的怪物类型列表
 * - 可卸除激活的档案释放槽位
 */
public class ItemIntelStatisticsBook extends Item {

    public ItemIntelStatisticsBook() {
        setRegistryName("intel_statistics_book");
        setTranslationKey("intel_statistics_book");
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        // 播放书本打开音效
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_ITEMFRAME_PLACE, SoundCategory.PLAYERS, 1.0F, 1.0F);

        if (world.isRemote) {
            // 客户端打开GUI
            openGui(player);
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @SideOnly(Side.CLIENT)
    private void openGui(EntityPlayer player) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiIntelStatistics(player));
    }

    /**
     * 获取档案等级名称
     */
    public static String getTierName(BiologicalProfile.Tier tier) {
        switch (tier) {
            case BASIC:
                return "基础";
            case COMPLETE:
                return "完整";
            case MASTERED:
                return "精通";
            default:
                return "未知";
        }
    }

    /**
     * 获取档案等级颜色代码
     */
    public static int getTierColorInt(BiologicalProfile.Tier tier) {
        switch (tier) {
            case BASIC:
                return 0x55FF55;  // 绿色
            case COMPLETE:
                return 0x5555FF;  // 蓝色
            case MASTERED:
                return 0xFF55FF;  // 粉紫色
            default:
                return 0xAAAAAA;  // 灰色
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "══════════════════════════");
        tooltip.add(TextFormatting.AQUA + "" + TextFormatting.BOLD + "情报整合统计书");
        tooltip.add(TextFormatting.DARK_GRAY + "Intel Statistics Book");
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "右键打开管理界面:");
        tooltip.add(TextFormatting.WHITE + "▸ 查看情报槽位状态");
        tooltip.add(TextFormatting.WHITE + "▸ 管理激活的档案");
        tooltip.add(TextFormatting.WHITE + "▸ 卸除档案释放槽位");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"知己知彼，百战不殆\"");
        tooltip.add(TextFormatting.GOLD + "══════════════════════════");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 附魔光效
    }
}
