package com.moremod.item.broken;

import baubles.api.BaubleType;
import com.moremod.config.BrokenRelicConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.BrokenGodHandler;
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
 * 破碎_臂 (Broken Arm)
 *
 * 终局饰品 - 精准与毁灭的极致
 *
 * 能力1: 绝对暴击
 *   - 所有攻击必定暴击
 *   - 暴击伤害 ×3
 *
 * 能力2: 极限延伸
 *   - 攻击距离 +3 格
 *
 * 能力3: 护甲粉碎
 *   - 100% 无视护甲
 *
 * 不可卸下，右键自动替换槽位饰品
 */
public class ItemBrokenArm extends ItemBrokenBaubleBase {

    public ItemBrokenArm() {
        setRegistryName("broken_arm");
        setTranslationKey("broken_arm");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.RING;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        // 无特殊效果
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 无特殊效果
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        // 效果在事件处理器中处理
    }

    /**
     * 获取暴击伤害倍率
     */
    public static float getCritMultiplier() {
        return (float) BrokenRelicConfig.armCritMultiplier;
    }

    /**
     * 获取护甲穿透比例（1.0 = 100%无视护甲）
     */
    public static float getArmorPenetration() {
        return (float) BrokenRelicConfig.armArmorPenetration;
    }

    /**
     * 获取攻击距离延长
     */
    public static float getRangeExtension() {
        return (float) BrokenRelicConfig.armRangeExtension;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "破碎_臂");
        tooltip.add(TextFormatting.DARK_GRAY + "Broken Arm");
        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "◆ 绝对暴击");
        tooltip.add(TextFormatting.GRAY + "  所有攻击必定暴击");
        tooltip.add(TextFormatting.YELLOW + "  暴击伤害 ×" + BrokenRelicConfig.armCritMultiplier);
        tooltip.add("");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "◆ 极限延伸");
        tooltip.add(TextFormatting.GRAY + "  攻击距离 +" + (int) BrokenRelicConfig.armRangeExtension + " 格");
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "◆ 护甲粉碎");
        tooltip.add(TextFormatting.GRAY + "  " + (int)(BrokenRelicConfig.armArmorPenetration * 100) + "% 无视护甲");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"机械之臂，粉碎一切防御\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"每一击都是致命的精准打击\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
