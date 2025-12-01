package com.moremod.system.humanity.intel;

import com.moremod.moremodCreativeTab;
import com.moremod.system.humanity.BiologicalProfile;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 情报整合统计书
 * Intel Statistics Book
 *
 * 显示玩家当前的情报系统状态：
 * - 已激活的档案及其加成
 * - 总体伤害/掉落加成
 * - 已记录的怪物类型列表
 */
public class ItemIntelStatisticsBook extends Item {

    public ItemIntelStatisticsBook() {
        setRegistryName("intel_statistics_book");
        setTranslationKey("intel_statistics_book");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        // 播放书本打开音效
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0F, 1.0F);

        // 获取玩家情报数据
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "无法读取情报数据！"));
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        // 显示统计信息
        displayIntelStatistics(player, data);

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /**
     * 显示情报统计信息
     */
    private void displayIntelStatistics(EntityPlayer player, IHumanityData data) {
        float humanity = data.getHumanity();
        int maxSlots = data.getMaxActiveProfiles();
        Set<ResourceLocation> activeProfiles = data.getActiveProfiles();
        Map<ResourceLocation, BiologicalProfile> allProfiles = data.getAllProfiles();

        // ========== 标题 ==========
        player.sendMessage(new TextComponentString(""));
        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "══════════════════════════════"));
        player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "" + TextFormatting.BOLD + "    情报整合统计书"));
        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "══════════════════════════════"));

        // ========== 基础状态 ==========
        player.sendMessage(new TextComponentString(""));
        player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "【基础状态】"));
        player.sendMessage(new TextComponentString(
                TextFormatting.WHITE + "人性值: " + TextFormatting.GREEN + String.format("%.1f%%", humanity)));
        player.sendMessage(new TextComponentString(
                TextFormatting.WHITE + "情报槽位: " + TextFormatting.AQUA + activeProfiles.size() + "/" + maxSlots));
        player.sendMessage(new TextComponentString(
                TextFormatting.WHITE + "已记录生物: " + TextFormatting.LIGHT_PURPLE + allProfiles.size() + "种"));

        // ========== 激活的档案 ==========
        if (!activeProfiles.isEmpty()) {
            player.sendMessage(new TextComponentString(""));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "【激活的情报档案】"));

            for (ResourceLocation entityId : activeProfiles) {
                BiologicalProfile profile = data.getProfile(entityId);
                if (profile != null) {
                    String tierName = getTierName(profile.getCurrentTier());
                    String tierColor = getTierColor(profile.getCurrentTier());
                    float dmgBonus = profile.getDamageBonus() * 100;
                    float dropBonus = profile.getDropBonus() * 100;

                    // 获取生物名称（简化）
                    String entityName = entityId.getPath().replace("_", " ");

                    player.sendMessage(new TextComponentString(
                            tierColor + "▸ " + TextFormatting.WHITE + entityName +
                                    TextFormatting.GRAY + " [" + tierColor + tierName + TextFormatting.GRAY + "]" +
                                    TextFormatting.RED + " +" + (int) dmgBonus + "%伤害" +
                                    TextFormatting.GREEN + " +" + (int) dropBonus + "%掉落"));
                }
            }
        } else {
            player.sendMessage(new TextComponentString(""));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "（无激活的情报档案）"));
        }

        // ========== 所有记录的生物 ==========
        if (!allProfiles.isEmpty()) {
            player.sendMessage(new TextComponentString(""));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "【已记录的生物】"));

            int count = 0;
            StringBuilder sb = new StringBuilder();
            for (ResourceLocation entityId : allProfiles.keySet()) {
                BiologicalProfile profile = allProfiles.get(entityId);
                String tierColor = getTierColor(profile.getCurrentTier());
                String entityName = entityId.getPath().replace("_", " ");

                if (count > 0) sb.append(TextFormatting.GRAY + ", ");
                sb.append(tierColor).append(entityName);
                count++;

                // 每行显示3个
                if (count % 3 == 0) {
                    player.sendMessage(new TextComponentString("  " + sb.toString()));
                    sb = new StringBuilder();
                }
            }

            // 剩余的
            if (sb.length() > 0) {
                player.sendMessage(new TextComponentString("  " + sb.toString()));
            }
        }

        // ========== 使用提示 ==========
        player.sendMessage(new TextComponentString(""));
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_GRAY + "提示: 使用情报书可激活/切换档案"));
        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "══════════════════════════════"));
    }

    /**
     * 获取档案等级名称
     */
    private String getTierName(BiologicalProfile.Tier tier) {
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
     * 获取档案等级颜色
     */
    private String getTierColor(BiologicalProfile.Tier tier) {
        switch (tier) {
            case BASIC:
                return TextFormatting.GREEN.toString();
            case COMPLETE:
                return TextFormatting.BLUE.toString();
            case MASTERED:
                return TextFormatting.LIGHT_PURPLE.toString();
            default:
                return TextFormatting.GRAY.toString();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "══════════════════════════");
        tooltip.add(TextFormatting.AQUA + "" + TextFormatting.BOLD + "情报整合统计书");
        tooltip.add(TextFormatting.DARK_GRAY + "Intel Statistics Book");
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "右键使用查看:");
        tooltip.add(TextFormatting.WHITE + "▸ 当前情报槽位状态");
        tooltip.add(TextFormatting.WHITE + "▸ 激活档案的加成效果");
        tooltip.add(TextFormatting.WHITE + "▸ 已记录的生物列表");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"知己知彼，百战不殆\"");
        tooltip.add(TextFormatting.GOLD + "══════════════════════════");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 附魔光效
    }
}
