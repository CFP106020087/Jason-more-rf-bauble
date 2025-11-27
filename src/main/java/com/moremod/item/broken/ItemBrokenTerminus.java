package com.moremod.item.broken;

import baubles.api.BaubleType;
import com.moremod.config.BrokenRelicConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.BrokenGodHandler;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.HumanitySpectrumSystem;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 破碎_终结 (Broken Terminus)
 *
 * 终局饰品 - 整个破碎系列的顶点
 *
 * 能力1: 极限总伤害放大 + 真伤比例
 *   - 所有造成的伤害 ×1.5
 *   - 额外追加30%包装真伤
 *   - 实际输出约 ≈ 1.8倍
 *
 * 能力2: 人性疯狂流失 + 持续HP流血
 *   - 每5秒消耗人性值
 *   - 每秒损失少量生命
 *
 * 不可卸下，右键自动替换槽位饰品
 */
public class ItemBrokenTerminus extends ItemBrokenBaubleBase {

    public ItemBrokenTerminus() {
        setRegistryName("broken_terminus");
        setTranslationKey("broken_terminus");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.BODY;
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
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        if (player.world.isRemote) return;
        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // 人性值消耗（按配置间隔）
        int drainInterval = BrokenRelicConfig.terminusHumanityDrainInterval;
        if (entity.ticksExisted % drainInterval == 0) {
            drainHumanity(player);
        }

        // HP流血（每秒）
        if (entity.ticksExisted % 20 == 0) {
            float bleed = (float) BrokenRelicConfig.terminusHPBleedPerSec;
            if (player.getHealth() > bleed + 1) { // 保留至少1点血
                player.attackEntityFrom(DamageSource.STARVE, bleed);
            }
        }
    }

    /**
     * 消耗人性值
     */
    private void drainHumanity(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        float currentHumanity = data.getHumanity();
        float minHumanity = (float) BrokenRelicConfig.terminusMinHumanity;
        float drainAmount = (float) BrokenRelicConfig.terminusHumanityDrainAmount;

        if (currentHumanity > minHumanity) {
            float newHumanity = Math.max(minHumanity, currentHumanity - drainAmount);
            HumanitySpectrumSystem.modifyHumanity(player, newHumanity - currentHumanity);
        }
    }

    /**
     * 获取伤害倍率
     */
    public static float getDamageMultiplier() {
        return (float) BrokenRelicConfig.terminusDamageMultiplier;
    }

    /**
     * 获取追加真伤比例
     */
    public static float getTrueDamageRatio() {
        return (float) BrokenRelicConfig.terminusTrueDamageRatio;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "" + TextFormatting.BOLD + "破碎_终结");
        tooltip.add(TextFormatting.DARK_GRAY + "Broken Terminus");
        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "◆ 极限伤害放大");
        tooltip.add(TextFormatting.GRAY + "  所有伤害 ×" + BrokenRelicConfig.terminusDamageMultiplier);
        tooltip.add(TextFormatting.GRAY + "  追加 " + (int)(BrokenRelicConfig.terminusTrueDamageRatio * 100) + "% 包装真伤");
        tooltip.add(TextFormatting.YELLOW + "  实际输出 ≈ ×" + String.format("%.1f",
                BrokenRelicConfig.terminusDamageMultiplier + BrokenRelicConfig.terminusTrueDamageRatio));
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_RED + "◆ 代价");
        tooltip.add(TextFormatting.RED + "  每 " + (BrokenRelicConfig.terminusHumanityDrainInterval / 20) +
                " 秒消耗 " + BrokenRelicConfig.terminusHumanityDrainAmount + " 人性值");
        tooltip.add(TextFormatting.RED + "  每秒流失 " + BrokenRelicConfig.terminusHPBleedPerSec + " HP");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"终结一切的力量\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"包括自己\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 破碎系列顶点饰品");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
