package com.moremod.item.broken;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.moremod.config.BrokenGodConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.BrokenGodHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 破碎之眼
 * Broken Eye
 *
 * 破碎之神的感知组件：
 * - 完全黑暗视觉（夜视）
 * - 免疫失明效果
 * - 实体高亮（ESP）- 客户端渲染
 * - 减少怪物侦测距离
 *
 * 不可卸下
 */
public class ItemBrokenEye extends Item implements IBauble {

    public ItemBrokenEye() {
        setRegistryName("broken_eye");
        setTranslationKey("broken_eye");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.HEAD;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        // 装备时立即赋予夜视
        if (player instanceof EntityPlayer) {
            grantNightVision(player);
            clearBlindness(player);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 卸下时移除夜视
        if (player instanceof EntityPlayer) {
            player.removePotionEffect(MobEffects.NIGHT_VISION);
        }
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        // 破碎之神不能卸下
        if (player instanceof EntityPlayer) {
            return !BrokenGodHandler.isBrokenGod((EntityPlayer) player);
        }
        return true;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // 每 tick 维护效果
        // 移除失明
        clearBlindness(entity);

        // 维护夜视（每200tick刷新）
        if (entity.ticksExisted % 200 == 0) {
            grantNightVision(entity);
        }

        // 注意：实体高亮（ESP）效果需要在客户端渲染事件中处理
        // 侦测距离减少在 BrokenGodEventHandler 中处理
    }

    /**
     * 赋予持久夜视
     */
    private void grantNightVision(EntityLivingBase entity) {
        // 添加长时间夜视（不显示粒子）
        entity.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 400, 0, true, false));
    }

    /**
     * 清除失明效果
     */
    private void clearBlindness(EntityLivingBase entity) {
        entity.removePotionEffect(MobEffects.BLINDNESS);

        // 尝试清除模组添加的视觉干扰效果
        entity.getActivePotionEffects().removeIf(effect -> {
            String effectName = effect.getPotion().getRegistryName().toString().toLowerCase();
            return effectName.contains("blind") || effectName.contains("darkness");
        });
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_PURPLE + "═════════════════════");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "破碎之眼");
        tooltip.add(TextFormatting.GRAY + "Broken Eye");
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "◆ 完全黑暗视觉");
        tooltip.add(TextFormatting.AQUA + "◆ 免疫失明效果");
        if (BrokenGodConfig.entityOutline) {
            tooltip.add(TextFormatting.GOLD + "◆ 实体高亮显示");
        }
        tooltip.add(TextFormatting.GREEN + "◆ 隐蔽存在");
        tooltip.add(TextFormatting.GRAY + "  (怪物侦测距离 -" + (int)(BrokenGodConfig.detectionReduction * 100) + "%)");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"看见一切\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"却不再理解所见之物\"");
        tooltip.add(TextFormatting.DARK_PURPLE + "═════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 发光效果
    }
}
