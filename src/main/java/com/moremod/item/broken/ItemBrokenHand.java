package com.moremod.item.broken;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.moremod.config.BrokenGodConfig;
import com.moremod.config.BrokenRelicConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.BrokenGodHandler;
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
 * 破碎_手 (Broken Hand)
 *
 * 终局饰品 - 攻速 + 多段打击
 *
 * 能力1: 攻速与冷却重置
 *   - 攻击冷却缩短50%
 *   - 大幅提升攻击速度
 *
 * 能力2: 幻象打击
 *   - 每次攻击追加一次额外伤害（原伤害40%）
 *   - 追加伤害走真伤系统，无视无敌帧
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
        if (player instanceof EntityPlayer) {
            return !BrokenGodHandler.isBrokenGod((EntityPlayer) player);
        }
        return true;
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

        // 强制减少攻击冷却（每tick）
        if (!player.world.isRemote && BrokenGodHandler.isBrokenGod(player)) {
            // 加速攻击冷却恢复
            float cooldownReduction = (float) BrokenRelicConfig.handCooldownReduction;
            // MC的攻击冷却每tick自然恢复，我们额外加速
            // 这里通过直接操作ticksSinceLastSwing来实现
            // 但该字段私有，我们改用其他方式：在事件中处理
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

        // 攻击速度大幅加成
        if (player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED) != null) {
            AttributeModifier existing = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED)
                    .getModifier(SPEED_MODIFIER_UUID);
            if (existing == null) {
                // 使用更高的攻速加成
                double speedBonus = 1.0 - BrokenRelicConfig.handCooldownReduction; // 0.5 -> +50% 攻速
                player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED).applyModifier(
                        new AttributeModifier(SPEED_MODIFIER_UUID, "Broken Hand Speed",
                                speedBonus, 2)
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

    /**
     * 获取幻象打击伤害比例（由事件处理器调用）
     */
    public static float getPhantomDamageRatio() {
        return (float) BrokenRelicConfig.handPhantomDamageRatio;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "破碎_手");
        tooltip.add(TextFormatting.DARK_GRAY + "Broken Hand");
        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "◆ 疯狂攻速");
        tooltip.add(TextFormatting.GRAY + "  攻击冷却 -" + (int)((1.0 - BrokenRelicConfig.handCooldownReduction) * 100) + "%");
        tooltip.add(TextFormatting.GRAY + "  近战伤害 +" + (int)(BrokenGodConfig.meleeDamageBonus * 100) + "%");
        tooltip.add("");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "◆ 幻象打击");
        tooltip.add(TextFormatting.GRAY + "  每次攻击追加 " + (int)(BrokenRelicConfig.handPhantomDamageRatio * 100) + "% 真伤");
        tooltip.add(TextFormatting.GRAY + "  无视无敌帧");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"手已成为纯粹的武器\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"攻击如同呼吸般自然\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
