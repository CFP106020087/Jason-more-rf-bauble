package com.moremod.item.broken;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.moremod.config.BrokenGodConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.BrokenGodHandler;
import com.moremod.system.ascension.BrokenGodItems;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 破碎之手
 * Broken Hand
 *
 * 破碎之神的武器臂组件：
 * - 近战伤害 +60%
 * - 攻击速度 +15%
 * - 无视无敌帧
 * - 造成真实伤害
 *
 * 不可卸下
 */
public class ItemBrokenHand extends Item implements IBauble {

    private static final UUID DAMAGE_MODIFIER_UUID = UUID.fromString("b1234567-89ab-cdef-0123-456789abcdef");
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("b2345678-9abc-def0-1234-56789abcdef0");

    public ItemBrokenHand() {
        setRegistryName("broken_hand");
        setTranslationKey("broken_hand");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.RING;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            // 应用攻击力加成
            applyModifiers((EntityPlayer) player);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            removeModifiers((EntityPlayer) player);
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
        // 确保修改器存在
        if (entity instanceof EntityPlayer && entity.ticksExisted % 100 == 0) {
            if (BrokenGodHandler.isBrokenGod((EntityPlayer) entity)) {
                applyModifiers((EntityPlayer) entity);
            }
        }
    }

    private void applyModifiers(EntityPlayer player) {
        // 攻击伤害加成
        if (player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE) != null) {
            AttributeModifier existing = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                    .getModifier(DAMAGE_MODIFIER_UUID);
            if (existing == null) {
                player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).applyModifier(
                        new AttributeModifier(DAMAGE_MODIFIER_UUID, "Broken Hand Damage",
                                BrokenGodConfig.meleeDamageBonus, 2)
                );
            }
        }

        // 攻击速度加成
        if (player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED) != null) {
            AttributeModifier existing = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED)
                    .getModifier(SPEED_MODIFIER_UUID);
            if (existing == null) {
                player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED).applyModifier(
                        new AttributeModifier(SPEED_MODIFIER_UUID, "Broken Hand Speed",
                                BrokenGodConfig.attackSpeedBonus, 2)
                );
            }
        }
    }

    private void removeModifiers(EntityPlayer player) {
        if (player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE) != null) {
            player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                    .removeModifier(DAMAGE_MODIFIER_UUID);
        }
        if (player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED) != null) {
            player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED)
                    .removeModifier(SPEED_MODIFIER_UUID);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_PURPLE + "═════════════════════");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "破碎之手");
        tooltip.add(TextFormatting.GRAY + "Broken Hand");
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "◆ 近战伤害 +" + (int)(BrokenGodConfig.meleeDamageBonus * 100) + "%");
        tooltip.add(TextFormatting.GOLD + "◆ 攻击速度 +" + (int)(BrokenGodConfig.attackSpeedBonus * 100) + "%");
        if (BrokenGodConfig.bypassInvulnerability) {
            tooltip.add(TextFormatting.DARK_RED + "◆ 无视无敌帧");
        }
        if (BrokenGodConfig.trueDamage) {
            tooltip.add(TextFormatting.DARK_RED + "◆ 造成真实伤害");
        }
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"手已成为纯粹的武器\"");
        tooltip.add(TextFormatting.DARK_PURPLE + "═════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 发光效果
    }
}
