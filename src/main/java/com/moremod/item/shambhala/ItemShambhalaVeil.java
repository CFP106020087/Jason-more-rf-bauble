package com.moremod.item.shambhala;

import baubles.api.BaubleType;
import com.moremod.config.ShambhalaConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.ShambhalaHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 香巴拉_隐匿 (Shambhala Veil) - 反侦察
 *
 * 能力：
 * - 大幅减少怪物侦测距离
 * - 潜行时完全隐身
 */
public class ItemShambhalaVeil extends ItemShambhalaBaubleBase {

    public ItemShambhalaVeil() {
        setRegistryName("shambhala_veil");
        setTranslationKey("shambhala_veil");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.HEAD;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        // 侦测减少在事件处理器中
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        // 逻辑在 ShambhalaEventHandler.onLivingSetAttackTarget 中
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.AQUA + "" + TextFormatting.BOLD + "香巴拉_隐匿");
        tooltip.add(TextFormatting.DARK_GRAY + "Shambhala Veil - Counter-Detection");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_PURPLE + "◆ 存在屏蔽");
        tooltip.add(TextFormatting.GRAY + "  怪物侦测距离 -" + (int)(ShambhalaConfig.veilDetectionReduction * 100) + "%");
        if (ShambhalaConfig.veilSneakInvisible) {
            tooltip.add(TextFormatting.DARK_AQUA + "  潜行时对怪物完全隐身");
        }
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"他们看不见我\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"因为我选择不被看见\"");
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
