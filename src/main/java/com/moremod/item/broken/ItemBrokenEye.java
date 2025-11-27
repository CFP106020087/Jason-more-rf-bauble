package com.moremod.item.broken;

import baubles.api.BaubleType;
import com.moremod.config.BrokenGodConfig;
import com.moremod.config.BrokenRelicConfig;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 破碎_眼 (Broken Eye)
 *
 * 终局饰品 - 命中与暴击的极致强化
 *
 * 能力1: 攻击必定暴击 + 忽略护甲
 *   - 所有攻击视为必定暴击（×1.5倍）
 *   - 忽略50%护甲
 *
 * 能力2: 攻击距离放宽
 *   - 近战攻击额外1格距离
 *
 * 附带: 夜视、免疫失明、实体高亮
 *
 * 不可卸下，右键自动替换槽位饰品
 */
public class ItemBrokenEye extends ItemBrokenBaubleBase {

    public ItemBrokenEye() {
        setRegistryName("broken_eye");
        setTranslationKey("broken_eye");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.HEAD;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            grantNightVision(player);
            clearBlindness(player);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            player.removePotionEffect(MobEffects.NIGHT_VISION);
        }
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // 移除失明
        clearBlindness(entity);

        // 维护夜视
        if (entity.ticksExisted % 200 == 0) {
            grantNightVision(entity);
        }
    }

    private void grantNightVision(EntityLivingBase entity) {
        entity.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 400, 0, true, false));
    }

    private void clearBlindness(EntityLivingBase entity) {
        entity.removePotionEffect(MobEffects.BLINDNESS);

        entity.getActivePotionEffects().removeIf(effect -> {
            String effectName = effect.getPotion().getRegistryName().toString().toLowerCase();
            return effectName.contains("blind") || effectName.contains("darkness");
        });
    }

    /**
     * 获取暴击伤害倍率（由事件处理器调用）
     */
    public static float getCritMultiplier() {
        return (float) BrokenRelicConfig.eyeCritMultiplier;
    }

    /**
     * 获取护甲忽略比例（由事件处理器调用）
     */
    public static float getArmorIgnoreRatio() {
        return (float) BrokenRelicConfig.eyeArmorIgnore;
    }

    /**
     * 获取攻击距离延长（由攻击逻辑调用）
     */
    public static float getRangeExtension() {
        return (float) BrokenRelicConfig.eyeRangeExtension;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "破碎_眼");
        tooltip.add(TextFormatting.DARK_GRAY + "Broken Eye");
        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "◆ 绝对暴击");
        tooltip.add(TextFormatting.GRAY + "  所有攻击必定暴击 (×" + BrokenRelicConfig.eyeCritMultiplier + ")");
        tooltip.add(TextFormatting.GRAY + "  忽略 " + (int)(BrokenRelicConfig.eyeArmorIgnore * 100) + "% 护甲");
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "◆ 精准打击");
        tooltip.add(TextFormatting.GRAY + "  攻击距离 +" + BrokenRelicConfig.eyeRangeExtension + " 格");
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "◆ 完全黑暗视觉");
        tooltip.add(TextFormatting.AQUA + "◆ 免疫失明效果");
        if (BrokenGodConfig.entityOutline) {
            tooltip.add(TextFormatting.LIGHT_PURPLE + "◆ 实体高亮显示");
        }
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"看穿一切防御\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"每一击都命中要害\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
