package com.moremod.item.broken;

import baubles.api.BaubleType;
import com.moremod.config.BrokenRelicConfig;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 破碎_投影 (Broken Projection)
 *
 * 终局饰品 - 死亡幻影
 *
 * 能力1: 幻影打击
 *   - 每次攻击额外造成 100% 真伤
 *   - 无视无敌帧
 *
 * 能力2: 斩杀执行
 *   - 目标血量 < 50% 时
 *   - 直接击杀
 *
 * 不可卸下，右键自动替换槽位饰品
 */
public class ItemBrokenProjection extends ItemBrokenBaubleBase {

    public ItemBrokenProjection() {
        setRegistryName("broken_projection");
        setTranslationKey("broken_projection");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.CHARM;
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
     * 获取幻影打击真伤比例
     */
    public static float getTrueDamageRatio() {
        return (float) BrokenRelicConfig.projectionTrueDamageRatio;
    }

    /**
     * 获取斩杀阈值
     */
    public static float getExecuteThreshold() {
        return (float) BrokenRelicConfig.projectionExecuteThreshold;
    }

    /**
     * 检查目标是否在斩杀范围内
     */
    public static boolean canExecute(EntityLivingBase target) {
        if (target == null) return false;
        float healthRatio = target.getHealth() / target.getMaxHealth();
        return healthRatio <= getExecuteThreshold();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "破碎_投影");
        tooltip.add(TextFormatting.DARK_GRAY + "Broken Projection");
        tooltip.add("");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "◆ 幻影打击");
        tooltip.add(TextFormatting.GRAY + "  每次攻击额外造成");
        tooltip.add(TextFormatting.YELLOW + "  +" + (int)(BrokenRelicConfig.projectionTrueDamageRatio * 100) + "% 真伤");
        tooltip.add(TextFormatting.GRAY + "  无视无敌帧");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_RED + "◆ 斩杀执行");
        tooltip.add(TextFormatting.GRAY + "  目标血量 < " + (int)(BrokenRelicConfig.projectionExecuteThreshold * 100) + "%");
        tooltip.add(TextFormatting.RED + "  直接击杀");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"死亡幻影，必杀之击\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"没有目标能承受两次打击\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
