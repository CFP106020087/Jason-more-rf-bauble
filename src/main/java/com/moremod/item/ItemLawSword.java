package com.moremod.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class ItemLawSword extends ItemSword {
    private static final UUID SOUL_DAMAGE_UUID = UUID.fromString("abcdefab-cdef-abcd-efab-cdefabcdef12");
    private static final UUID ATTRIBUTE_UUID_1 = UUID.fromString("12345678-1234-1234-1234-1234567890aa");
    private static final UUID ATTRIBUTE_UUID_2 = UUID.fromString("12345678-1234-1234-1234-1234567890bb");

    public ItemLawSword(String name) {
        super(ToolMaterial.DIAMOND);
        this.setMaxStackSize(1);
        this.setMaxDamage(0);
        this.setRegistryName(name);
        this.setTranslationKey(name);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    public boolean hitEntity(ItemStack stack, EntityLivingBase target, @Nonnull EntityLivingBase attacker) {
        if (target != null && !target.world.isRemote && attacker instanceof EntityPlayer) {
            target.hurtResistantTime = 0;
            // 固定傷害值改為 30
            float damage = 30.0F;
            target.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer)attacker).setDamageBypassesArmor().setDamageIsAbsolute(), damage);
            this.applySoulDamage(target);
            return true;
        } else {
            return false;
        }
    }

    private void applySoulDamage(EntityLivingBase target) {
        IAttributeInstance attr = target.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (attr != null) {
            AttributeModifier mod = attr.getModifier(SOUL_DAMAGE_UUID);
            double soulDamage = mod != null ? mod.getAmount() : 0.0;
            soulDamage -= 0.1;
            if (soulDamage < -1.0) {
                soulDamage = -1.0;
            }

            attr.removeModifier(SOUL_DAMAGE_UUID);
            attr.applyModifier(new AttributeModifier(SOUL_DAMAGE_UUID, "Soul Damage", soulDamage, 2));
            if (soulDamage <= -1.0) {
                target.attackEntityFrom(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);
            }
        }
    }

    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> map = HashMultimap.create();
        if (slot == EntityEquipmentSlot.MAINHAND) {
            map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTRIBUTE_UUID_1, "Base Attack", 30.0, 0));
            map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTRIBUTE_UUID_2, "Attack Speed", 2.4, 0));
        }
        return map;
    }

    // 重写此方法使物品可以附魔
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    // 重写此方法设置附魔能力（值越高，能获得更好的附魔）
    @Override
    public int getItemEnchantability() {
        return 22; // 钻石剑的默认值是10，金剑是22，这里设置为22使其容易获得好附魔
    }

    // 如果你想让它在生存模式下也能接受任何附魔（包括通过铁砧添加附魔书）
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    // 可选：如果你想让这把剑可以接受特定等级的附魔
    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000; // 如果需要的话，这个值用于某些附魔的计算
    }

    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§a 怨念：每次攻击永久削减敌人生命上限");
        tooltip.add("§b 鬼泣：此武器能穿透无敌帧");
        tooltip.add("§c 咒怨：10,9,8......");
    }
}