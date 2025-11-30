package com.moremod.item.shambhala;

import baubles.api.BaubleType;
import com.moremod.config.ShambhalaConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.ShambhalaHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 香巴拉_壁垒 (Shambhala Bastion) - 绝对防御
 *
 * 能力：
 * - 大幅增加护甲和韧性
 * - 基础伤害减免
 */
public class ItemShambhalaBastion extends ItemShambhalaBaubleBase {

    private static final UUID ARMOR_UUID = UUID.fromString("a2234567-89ab-cdef-0123-456789abcde1");
    private static final UUID TOUGHNESS_UUID = UUID.fromString("a2234567-89ab-cdef-0123-456789abcde2");

    public ItemShambhalaBastion() {
        setRegistryName("shambhala_bastion");
        setTranslationKey("shambhala_bastion");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.RING;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            applyArmorBonus((EntityPlayer) player);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            removeArmorBonus((EntityPlayer) player);
        }
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        if (player.world.isRemote) return;
        if (!ShambhalaHandler.isShambhala(player)) return;

        if (entity.ticksExisted % 100 == 0) {
            applyArmorBonus(player);
        }
    }

    private void applyArmorBonus(EntityPlayer player) {
        // 护甲
        IAttributeInstance armorAttr = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armorAttr != null && armorAttr.getModifier(ARMOR_UUID) == null) {
            armorAttr.applyModifier(new AttributeModifier(
                    ARMOR_UUID, "Shambhala Bastion Armor",
                    ShambhalaConfig.bastionArmorBonus, 0));
        }

        // 韧性
        IAttributeInstance toughAttr = player.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);
        if (toughAttr != null && toughAttr.getModifier(TOUGHNESS_UUID) == null) {
            toughAttr.applyModifier(new AttributeModifier(
                    TOUGHNESS_UUID, "Shambhala Bastion Toughness",
                    ShambhalaConfig.bastionToughnessBonus, 0));
        }
    }

    private void removeArmorBonus(EntityPlayer player) {
        IAttributeInstance armorAttr = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armorAttr != null) {
            AttributeModifier mod = armorAttr.getModifier(ARMOR_UUID);
            if (mod != null) armorAttr.removeModifier(mod);
        }

        IAttributeInstance toughAttr = player.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);
        if (toughAttr != null) {
            AttributeModifier mod = toughAttr.getModifier(TOUGHNESS_UUID);
            if (mod != null) toughAttr.removeModifier(mod);
        }
    }

    /**
     * 获取伤害减免比例（由事件处理器调用）
     */
    public static float getDamageReduction() {
        return (float) ShambhalaConfig.bastionDamageReduction;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.AQUA + "" + TextFormatting.BOLD + "香巴拉_壁垒");
        tooltip.add(TextFormatting.DARK_GRAY + "Shambhala Bastion - Absolute Defense");
        tooltip.add("");
        tooltip.add(TextFormatting.BLUE + "◆ 绝对防御");
        tooltip.add(TextFormatting.GRAY + "  护甲 +" + (int) ShambhalaConfig.bastionArmorBonus);
        tooltip.add(TextFormatting.GRAY + "  韧性 +" + (int) ShambhalaConfig.bastionToughnessBonus);
        tooltip.add(TextFormatting.AQUA + "  伤害减免 " + (int)(ShambhalaConfig.bastionDamageReduction * 100) + "%");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"无物可破此壁\"");
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
