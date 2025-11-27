package com.moremod.item.broken;

import baubles.api.BaubleType;
import com.moremod.config.BrokenRelicConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.BrokenGodHandler;
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
 * 破碎_手 (Broken Hand)
 *
 * 终局饰品 - 无尽之握
 *
 * 能力1: 疯狂攻速
 *   - 攻击速度 ×3
 *
 * 能力2: 近战强化
 *   - 近战伤害 +100%
 *
 * 能力3: 无冷却
 *   - 攻击后立即重置冷却
 *
 * 不可卸下，右键自动替换槽位饰品
 */
public class ItemBrokenHand extends ItemBrokenBaubleBase {

    private static final UUID DAMAGE_MODIFIER_UUID = UUID.fromString("b1234567-89ab-cdef-0123-456789abcdef");
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("b2345678-9abc-def0-1234-56789abcdef0");

    public ItemBrokenHand() {
        setRegistryName("broken_hand");
        setTranslationKey("broken_hand");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.RING;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
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
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        // 确保修改器存在
        if (entity.ticksExisted % 100 == 0) {
            if (BrokenGodHandler.isBrokenGod(player)) {
                applyModifiers(player);
            }
        }
    }

    private void applyModifiers(EntityPlayer player) {
        // 近战伤害加成
        IAttributeInstance damageAttr = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            AttributeModifier existing = damageAttr.getModifier(DAMAGE_MODIFIER_UUID);
            if (existing == null) {
                damageAttr.applyModifier(new AttributeModifier(
                        DAMAGE_MODIFIER_UUID,
                        "Broken Hand Damage",
                        BrokenRelicConfig.handMeleeDamageBonus,
                        2 // 乘法
                ));
            }
        }

        // 攻击速度加成
        IAttributeInstance speedAttr = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (speedAttr != null) {
            AttributeModifier existing = speedAttr.getModifier(SPEED_MODIFIER_UUID);
            if (existing == null) {
                // 攻速×3 = 原来的基础上+200%
                double speedBonus = BrokenRelicConfig.handSpeedMultiplier - 1.0;
                speedAttr.applyModifier(new AttributeModifier(
                        SPEED_MODIFIER_UUID,
                        "Broken Hand Speed",
                        speedBonus,
                        2 // 乘法
                ));
            }
        }
    }

    private void removeModifiers(EntityPlayer player) {
        IAttributeInstance damageAttr = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.removeModifier(DAMAGE_MODIFIER_UUID);
        }

        IAttributeInstance speedAttr = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_UUID);
        }
    }

    /**
     * 获取攻速倍率
     */
    public static float getSpeedMultiplier() {
        return (float) BrokenRelicConfig.handSpeedMultiplier;
    }

    /**
     * 获取近战伤害加成
     */
    public static float getMeleeDamageBonus() {
        return (float) BrokenRelicConfig.handMeleeDamageBonus;
    }

    /**
     * 是否重置攻击冷却
     */
    public static boolean shouldResetCooldown() {
        return BrokenRelicConfig.handResetCooldown;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "破碎_手");
        tooltip.add(TextFormatting.DARK_GRAY + "Broken Hand");
        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "◆ 疯狂攻速");
        tooltip.add(TextFormatting.YELLOW + "  攻击速度 ×" + (int) BrokenRelicConfig.handSpeedMultiplier);
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "◆ 近战强化");
        tooltip.add(TextFormatting.GRAY + "  近战伤害 +" + (int)(BrokenRelicConfig.handMeleeDamageBonus * 100) + "%");
        tooltip.add("");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "◆ 无冷却");
        tooltip.add(TextFormatting.GRAY + "  攻击后立即重置冷却");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"无尽之握，速度的极致\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"攻击如同呼吸般自然\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
