package com.moremod.item.broken;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.moremod.config.BrokenRelicConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.BrokenGodHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
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
 * 终局饰品 - 幻象分身 + 斩杀执行
 *
 * 能力1: 幻象分身 (Phantom Twin)
 *   - 每次近战攻击生成一次幻象打击
 *   - 造成原伤害50%的包装真伤
 *   - 无视无敌帧
 *
 * 能力2: 幻影溢出
 *   - 当目标生命值 < 35% 时
 *   - 幻象打击变为斩杀执行（直接归零）
 *
 * 不可卸下
 */
public class ItemBrokenProjection extends Item implements IBauble {

    public ItemBrokenProjection() {
        setRegistryName("broken_projection");
        setTranslationKey("broken_projection");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
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
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            return !BrokenGodHandler.isBrokenGod((EntityPlayer) player);
        }
        return true;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        // 效果在事件处理器中处理
    }

    /**
     * 获取幻象分身伤害比例
     */
    public static float getTwinDamageRatio() {
        return (float) BrokenRelicConfig.projectionTwinDamageRatio;
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
        tooltip.add(TextFormatting.LIGHT_PURPLE + "◆ 幻象分身");
        tooltip.add(TextFormatting.GRAY + "  每次攻击生成幻象打击");
        tooltip.add(TextFormatting.GRAY + "  造成 " + (int)(BrokenRelicConfig.projectionTwinDamageRatio * 100) + "% 包装真伤");
        tooltip.add(TextFormatting.GRAY + "  无视无敌帧");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_RED + "◆ 幻影溢出");
        tooltip.add(TextFormatting.GRAY + "  目标生命 < " + (int)(BrokenRelicConfig.projectionExecuteThreshold * 100) + "% 时");
        tooltip.add(TextFormatting.RED + "  幻象打击 → 斩杀执行");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"影子比本体更致命\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"没有目标能承受两次打击\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
