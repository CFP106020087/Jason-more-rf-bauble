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
 * 香巴拉_核心 (Shambhala Core) - 不灭之心
 *
 * 能力：
 * - 最大生命值大幅提升
 * - 有能量时血量不会低于1
 * - 核心防御机制
 */
public class ItemShambhalaCore extends ItemShambhalaBaubleBase {

    private static final UUID HEALTH_BONUS_UUID = UUID.fromString("a1234567-89ab-cdef-0123-456789abcde0");
    private static final UUID KNOCKBACK_RESIST_UUID = UUID.fromString("a1234567-89ab-cdef-0123-456789abcde1");

    public ItemShambhalaCore() {
        setRegistryName("shambhala_core");
        setTranslationKey("shambhala_core");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.AMULET;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            applyHealthBonus((EntityPlayer) player);
            applyKnockbackResist((EntityPlayer) player);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            removeHealthBonus((EntityPlayer) player);
            removeKnockbackResist((EntityPlayer) player);
        }
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        if (player.world.isRemote) return;
        if (!ShambhalaHandler.isShambhala(player)) return;

        // 确保属性加成存在
        if (entity.ticksExisted % 100 == 0) {
            applyHealthBonus(player);
            applyKnockbackResist(player);
        }

        // 血量越低回血越快（每秒处理一次）
        if (entity.ticksExisted % 20 == 0) {
            applyLowHPRegen(player);
        }

        // 血量锁定由 ShambhalaFirstAidCompat 和 ShambhalaEventHandler.onLivingDeath 处理
    }

    /**
     * 血量越低回血越快
     * 公式: healRate = baseRegen + (maxRegen - baseRegen) * (1 - currentHP/maxHP)
     */
    private void applyLowHPRegen(EntityPlayer player) {
        float currentHP = player.getHealth();
        float maxHP = player.getMaxHealth();

        if (currentHP >= maxHP) return; // 满血不回

        float hpRatio = currentHP / maxHP;
        // 血量越低，回血越多
        float healRate = (float) (ShambhalaConfig.coreBaseRegen +
                (ShambhalaConfig.coreMaxRegen - ShambhalaConfig.coreBaseRegen) * (1.0f - hpRatio));

        player.heal(healRate);
    }

    private void applyHealthBonus(EntityPlayer player) {
        IAttributeInstance healthAttr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (healthAttr == null) return;

        AttributeModifier existing = healthAttr.getModifier(HEALTH_BONUS_UUID);
        if (existing == null) {
            healthAttr.applyModifier(new AttributeModifier(
                    HEALTH_BONUS_UUID,
                    "Shambhala Core Health",
                    ShambhalaConfig.coreHealthBonus,
                    0 // 加法
            ));
        }
    }

    private void removeHealthBonus(EntityPlayer player) {
        IAttributeInstance healthAttr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (healthAttr == null) return;

        AttributeModifier existing = healthAttr.getModifier(HEALTH_BONUS_UUID);
        if (existing != null) {
            healthAttr.removeModifier(existing);
        }
    }

    private void applyKnockbackResist(EntityPlayer player) {
        if (!ShambhalaConfig.coreKnockbackImmune) return;

        IAttributeInstance knockbackAttr = player.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr == null) return;

        AttributeModifier existing = knockbackAttr.getModifier(KNOCKBACK_RESIST_UUID);
        if (existing == null) {
            knockbackAttr.applyModifier(new AttributeModifier(
                    KNOCKBACK_RESIST_UUID,
                    "Shambhala Core Knockback Immunity",
                    1.0, // 100% 击退抗性
                    0 // 加法
            ));
        }
    }

    private void removeKnockbackResist(EntityPlayer player) {
        IAttributeInstance knockbackAttr = player.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr == null) return;

        AttributeModifier existing = knockbackAttr.getModifier(KNOCKBACK_RESIST_UUID);
        if (existing != null) {
            knockbackAttr.removeModifier(existing);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.AQUA + "" + TextFormatting.BOLD + "香巴拉_核心");
        tooltip.add(TextFormatting.DARK_GRAY + "Shambhala Core - The Undying Heart");
        tooltip.add("");
        tooltip.add(TextFormatting.GREEN + "◆ 不灭意志");
        tooltip.add(TextFormatting.GRAY + "  最大生命值 +" + (int) ShambhalaConfig.coreHealthBonus);
        tooltip.add(TextFormatting.AQUA + "  有能量时血量不会低于1");
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "◆ 永恒驱动");
        tooltip.add(TextFormatting.GRAY + "  血量越低回血越快");
        tooltip.add(TextFormatting.GRAY + "  满血: " + String.format("%.1f", ShambhalaConfig.coreBaseRegen) +
                " HP/s → 濒死: " + String.format("%.1f", ShambhalaConfig.coreMaxRegen) + " HP/s");
        tooltip.add("");
        if (ShambhalaConfig.coreKnockbackImmune) {
            tooltip.add(TextFormatting.YELLOW + "◆ 磐石之躯");
            tooltip.add(TextFormatting.GRAY + "  免疫击退");
            tooltip.add("");
        }
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"只要齿轮仍在转动\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"这座机械圣像便永不倒下\"");
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
