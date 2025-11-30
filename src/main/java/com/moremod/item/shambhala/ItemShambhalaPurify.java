package com.moremod.item.shambhala;

import baubles.api.BaubleType;
import com.moremod.config.ShambhalaConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.ShambhalaHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 香巴拉_净化 (Shambhala Purify) - 被动免疫负面效果
 *
 * 能力：
 * - 被动免疫所有负面效果（包括模组效果）
 * - 每秒消耗能量维持免疫
 * - 能量不足时失去免疫
 */
@Mod.EventBusSubscriber(modid = "moremod")
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

        // 每秒消耗能量（每20tick = 1秒）
        if (entity.ticksExisted % 20 == 0) {
            // 尝试消耗能量
            if (ShambhalaHandler.consumeEnergy(player, ShambhalaConfig.purifyEnergyPerSecond)) {
                // 成功消耗能量，清除所有现有的负面效果
                removeAllBadEffects(player);
            }
        }
    }

    /**
     * 清除所有负面效果
     */
    private void removeAllBadEffects(EntityPlayer player) {
        Collection<PotionEffect> effects = new ArrayList<>(player.getActivePotionEffects());

        for (PotionEffect effect : effects) {
            Potion potion = effect.getPotion();

            // 检查是否是负面效果
            if (potion.isBadEffect()) {
                player.removeActivePotionEffect(potion);
            }
        }
    }

    /**
     * 检查玩家是否有足够能量维持免疫
     */
    public static boolean hasImmunityEnergy(EntityPlayer player) {
        if (!ShambhalaHandler.isShambhala(player)) return false;
        return ShambhalaHandler.hasEnergy(player, ShambhalaConfig.purifyEnergyPerSecond);
    }

    /**
     * 阻止负面效果被施加 - 事件处理
     */
    @SubscribeEvent
    public static void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 服务端检查
        if (player.world.isRemote) return;

        // 检查是否是机巧香巴拉
        if (!ShambhalaHandler.isShambhala(player)) return;

        // 检查药水是否是负面效果
        PotionEffect effect = event.getPotionEffect();
        if (effect == null) return;

        Potion potion = effect.getPotion();
        if (!potion.isBadEffect()) return;

        // 检查是否有足够能量
        if (hasImmunityEnergy(player)) {
            // 阻止负面效果被施加
            event.setResult(Event.Result.DENY);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.AQUA + "" + TextFormatting.BOLD + "香巴拉_净化");
        tooltip.add(TextFormatting.DARK_GRAY + "Shambhala Purify - Absolute Immunity");
        tooltip.add("");
        tooltip.add(TextFormatting.GREEN + "◆ 绝对净化");
        tooltip.add(TextFormatting.GRAY + "  被动免疫所有负面效果");
        if (ShambhalaConfig.purifyImmuneAll) {
            tooltip.add(TextFormatting.DARK_AQUA + "  包括模组添加的负面效果");
        }
        tooltip.add(TextFormatting.YELLOW + "  能量消耗: " + ShambhalaConfig.purifyEnergyPerSecond + " RF/秒");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"与世独立，不受侵扰\"");
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
