//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.moremod.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    }

    public boolean hitEntity(ItemStack stack, EntityLivingBase target, @Nonnull EntityLivingBase attacker) {
        if (target != null && !target.world.isRemote && attacker instanceof EntityPlayer) {
            target.hurtResistantTime = 0;
            float base = 0.0F;
            base += 3.0F;
            base += 2.0F;
            target.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer)attacker).setDamageBypassesArmor().setDamageIsAbsolute(), base);
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
            double soulDamage = mod != null ? mod.getAmount() : (double)0.0F;
            soulDamage -= 0.1;
            if (soulDamage < (double)-1.0F) {
                soulDamage = (double)-1.0F;
            }

            attr.removeModifier(SOUL_DAMAGE_UUID);
            attr.applyModifier(new AttributeModifier(SOUL_DAMAGE_UUID, "Soul Damage", soulDamage, 2));
            if (soulDamage <= (double)-1.0F) {
                target.attackEntityFrom(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);
            }

        }
    }

    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> map = HashMultimap.create();
        if (slot == EntityEquipmentSlot.MAINHAND) {
            map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTRIBUTE_UUID_1, "Base Attack", (double)30.0F, 0));
            map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTRIBUTE_UUID_2, "Attack Speed", 2.4, 0));
        }

        return map;
    }

    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§a 怨念：每次攻击永久削减敌人生命上限");
        tooltip.add("§b 鬼泣：此武器能穿透无敌帧");
        tooltip.add("§c 咒怨：10,9,8......");


    }
}
