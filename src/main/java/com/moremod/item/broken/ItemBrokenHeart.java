package com.moremod.item.broken;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.moremod.config.BrokenGodConfig;
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
 * 破碎之心
 * Broken Heart
 *
 * 破碎之神的核心组件：
 * - 永久免疫恐惧
 * - 免疫 Wither、Poison、Bleeding
 * - 免疫 FirstAid 部位伤害
 * - 生命值不会低于最小值（除停机模式）
 *
 * 不可卸下
 */
public class ItemBrokenHeart extends Item implements IBauble {

    public ItemBrokenHeart() {
        setRegistryName("broken_heart");
        setTranslationKey("broken_heart");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.AMULET;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        // 装备时移除负面效果
        if (player instanceof EntityPlayer) {
            clearNegativeEffects(player);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 卸下时无特殊操作
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

        // 每 tick 检查并移除负面效果
        clearNegativeEffects(entity);

        // 确保最小血量（除非在停机模式）
        if (!BrokenGodHandler.isInShutdown(player)) {
            float minHealth = (float) BrokenGodConfig.minimumHealth;
            if (entity.getHealth() < minHealth && entity.getHealth() > 0) {
                entity.setHealth(minHealth);
            }
        }
    }

    /**
     * 清除特定负面效果
     */
    private void clearNegativeEffects(EntityLivingBase entity) {
        // Wither
        entity.removePotionEffect(net.minecraft.init.MobEffects.WITHER);

        // Poison
        entity.removePotionEffect(net.minecraft.init.MobEffects.POISON);

        // Blindness (由破碎之眼处理，这里也加一层保护)
        // entity.removePotionEffect(net.minecraft.init.MobEffects.BLINDNESS);

        // Nausea (恐惧/混乱)
        entity.removePotionEffect(net.minecraft.init.MobEffects.NAUSEA);

        // Hunger (模拟"无痛")
        // entity.removePotionEffect(net.minecraft.init.MobEffects.HUNGER);

        // 尝试清除模组添加的出血效果
        entity.getActivePotionEffects().removeIf(effect -> {
            String effectName = effect.getPotion().getRegistryName().toString().toLowerCase();
            return effectName.contains("bleed") || effectName.contains("bleeding");
        });
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_PURPLE + "═════════════════════");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "破碎之心");
        tooltip.add(TextFormatting.GRAY + "Broken Heart");
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "◆ 永久免疫恐惧");
        tooltip.add(TextFormatting.AQUA + "◆ 无痛模式");
        tooltip.add(TextFormatting.GREEN + "◆ 免疫凋零效果");
        tooltip.add(TextFormatting.GREEN + "◆ 免疫中毒效果");
        tooltip.add(TextFormatting.GREEN + "◆ 免疫出血效果");
        tooltip.add(TextFormatting.YELLOW + "◆ 生命值稳定器");
        tooltip.add(TextFormatting.GRAY + "  (最低保持 " + BrokenGodConfig.minimumHealth + " HP)");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"心脏不再跳动\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"但齿轮永不停止\"");
        tooltip.add(TextFormatting.DARK_PURPLE + "═════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 发光效果
    }
}
