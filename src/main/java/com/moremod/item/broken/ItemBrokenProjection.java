package com.moremod.item.broken;

import baubles.api.BaubleType;
import com.moremod.config.BrokenGodConfig;
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
        return (float) BrokenGodConfig.projectionTrueDamageRatio;
    }

    /**
     * 获取斩杀阈值
     */
    public static float getExecuteThreshold() {
        return (float) BrokenGodConfig.projectionExecuteThreshold;
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
        tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "破碎_投影");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "◆ 幻影: " + TextFormatting.YELLOW + "+" + (int)(BrokenGodConfig.projectionTrueDamageRatio * 100) + "%真伤 " + TextFormatting.GRAY + "无视无敌帧");
        tooltip.add(TextFormatting.DARK_RED + "◆ 斩杀: " + TextFormatting.GRAY + "目标<" + (int)(BrokenGodConfig.projectionExecuteThreshold * 100) + "%HP直接击杀");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
