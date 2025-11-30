package com.moremod.item.shambhala;

import baubles.api.BaubleType;
import com.moremod.config.ShambhalaConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.ShambhalaHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 香巴拉_净化 (Shambhala Purify) - 免疫与净化
 *
 * 能力：
 * - 自动清除负面效果
 * - 免疫凋零和中毒
 */
public class ItemShambhalaPurify extends ItemShambhalaBaubleBase {

    public ItemShambhalaPurify() {
        setRegistryName("shambhala_purify");
        setTranslationKey("shambhala_purify");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.BELT;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        if (player.world.isRemote) return;
        if (!ShambhalaHandler.isShambhala(player)) return;

        // 定期清除负面效果
        if (entity.ticksExisted % ShambhalaConfig.purifyCleanseInterval == 0) {
            cleanseBadEffects(player);
        }
    }

    private void cleanseBadEffects(EntityPlayer player) {
        Collection<PotionEffect> effects = new ArrayList<>(player.getActivePotionEffects());

        for (PotionEffect effect : effects) {
            Potion potion = effect.getPotion();

            // 免疫凋零
            if (ShambhalaConfig.purifyWitherImmune && potion == MobEffects.WITHER) {
                player.removeActivePotionEffect(potion);
                consumeCleanseEnergy(player);
                continue;
            }

            // 免疫中毒
            if (ShambhalaConfig.purifyPoisonImmune && potion == MobEffects.POISON) {
                player.removeActivePotionEffect(potion);
                consumeCleanseEnergy(player);
                continue;
            }

            // 清除其他负面效果
            if (potion.isBadEffect()) {
                if (ShambhalaHandler.consumeEnergy(player, ShambhalaConfig.energyPerCleanse)) {
                    player.removeActivePotionEffect(potion);
                }
            }
        }
    }

    private void consumeCleanseEnergy(EntityPlayer player) {
        ShambhalaHandler.consumeEnergy(player, ShambhalaConfig.energyPerCleanse / 2);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.AQUA + "" + TextFormatting.BOLD + "香巴拉_净化");
        tooltip.add(TextFormatting.DARK_GRAY + "Shambhala Purify - Cleansing Light");
        tooltip.add("");
        tooltip.add(TextFormatting.GREEN + "◆ 净化之力");
        tooltip.add(TextFormatting.GRAY + "  每 " + (ShambhalaConfig.purifyCleanseInterval / 20f) + " 秒自动清除负面效果");
        if (ShambhalaConfig.purifyWitherImmune) {
            tooltip.add(TextFormatting.DARK_PURPLE + "  免疫凋零");
        }
        if (ShambhalaConfig.purifyPoisonImmune) {
            tooltip.add(TextFormatting.DARK_GREEN + "  免疫中毒");
        }
        tooltip.add(TextFormatting.YELLOW + "  消耗能量: " + ShambhalaConfig.energyPerCleanse + " RF/效果");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"不洁之物，远离此躯\"");
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
