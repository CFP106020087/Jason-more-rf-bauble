package com.moremod.item;

import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 鉴定卷轴 - 用于鉴定未鉴定的宝石
 * 
 * 使用方法：
 * 1. 手持未鉴定宝石
 * 2. 右键使用鉴定卷轴
 * 3. 消耗卷轴，宝石变为已鉴定
 */
public class ItemIdentifyScroll extends Item {

    public ItemIdentifyScroll() {
        super();
        setRegistryName("identify_scroll");
        setTranslationKey("identify_scroll");
        setMaxStackSize(64);
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack scroll = player.getHeldItem(hand);
        ItemStack offhand = player.getHeldItem(hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);

        // 客户端不处理
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, scroll);
        }

        // 检查副手是否为未鉴定宝石
        if (offhand.isEmpty() || !GemNBTHelper.isUnidentified(offhand)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "请在副手持有未鉴定的宝石！"
            ));
            return new ActionResult<>(EnumActionResult.FAIL, scroll);
        }

        // 鉴定宝石
        ItemStack identifiedGem = GemNBTHelper.identifyGem(offhand);
        
        if (identifiedGem.isEmpty()) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "鉴定失败！宝石可能已损坏。"
            ));
            return new ActionResult<>(EnumActionResult.FAIL, scroll);
        }

        // 替换副手的宝石
        player.setHeldItem(hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, identifiedGem);

        // 消耗卷轴
        scroll.shrink(1);

        // 播放音效
        player.playSound(
                net.minecraft.init.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                1.0F,
                1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F
        );

        // 粒子效果
        if (!world.isRemote) {
            world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.ENCHANTMENT_TABLE,
                    player.posX, player.posY + 1, player.posZ,
                    0.5, 0, 0, 0
                    , 20
            );
        }

        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ " + 
                TextFormatting.GOLD + "鉴定成功！"
        ));

        return new ActionResult<>(EnumActionResult.SUCCESS, scroll);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GOLD + I18n.format("tooltip.moremod.identifyscroll.desc"));
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + I18n.format("tooltip.moremod.identifyscroll.usage_title"));
        tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.moremod.identifyscroll.usage_1"));
        tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.moremod.identifyscroll.usage_2"));
    }

}
