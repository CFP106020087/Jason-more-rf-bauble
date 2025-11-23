package com.moremod.core;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import java.util.UUID;

public class SwordAttributeHandler {

    private static final UUID UPGRADE_ATK_UUID   = UUID.fromString("1b7d9a64-0b37-4a3b-9e8f-6c0f9a5d5a01");
    private static final UUID UPGRADE_AS_UUID    = UUID.fromString("1b7d9a64-0b37-4a3b-9e8f-6c0f9a5d5a02");
    private static final UUID UPGRADE_ARMOR_UUID = UUID.fromString("1b7d9a64-0b37-4a3b-9e8f-6c0f9a5d5a03");
    private static final UUID UPGRADE_HP_UUID    = UUID.fromString("1b7d9a64-0b37-4a3b-9e8f-6c0f9a5d5a04");
    private static final UUID UPGRADE_MS_UUID    = UUID.fromString("1b7d9a64-0b37-4a3b-9e8f-6c0f9a5d5a05");
    private static final UUID UPGRADE_TOUGH_UUID = UUID.fromString("1b7d9a64-0b37-4a3b-9e8f-6c0f9a5d5a06");
    private static final UUID UPGRADE_KB_UUID    = UUID.fromString("1b7d9a64-0b37-4a3b-9e8f-6c0f9a5d5a07");

    public static Multimap<String, AttributeModifier> getUpgradeModifiers(ItemStack stack, EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> out = HashMultimap.create();

        // 只在主手時生效
        if (slot != EntityEquipmentSlot.MAINHAND) return out;

        // 只對劍生效
        if (!(stack.getItem() instanceof ItemSword)) return out;

        // 獲取升級NBT
        NBTTagCompound upg = getUpgradeTag(stack);
        if (upg == null) return out;

        // 基礎攻擊/攻速增益
        float atk = upg.hasKey("AttackBonus") ? upg.getFloat("AttackBonus") : 0f;
        float asp = upg.hasKey("SpeedBonus") ? upg.getFloat("SpeedBonus") : 0f;

        if (atk != 0f) {
            out.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(UPGRADE_ATK_UUID, "Sword Upgrade Attack", atk, 0));
        }
        if (asp != 0f) {
            out.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(UPGRADE_AS_UUID, "Sword Upgrade Speed", asp, 0));
        }

        // 其他屬性（ExtraAttributes）
        if (upg.hasKey("ExtraAttributes")) {
            NBTTagCompound ex = upg.getCompoundTag("ExtraAttributes");

            if (ex.hasKey("max_health")) {
                double val = ex.getDouble("max_health");
                out.put(SharedMonsterAttributes.MAX_HEALTH.getName(),
                        new AttributeModifier(UPGRADE_HP_UUID, "Sword Upgrade Health", val, 0));
            }
            if (ex.hasKey("movement_speed")) {
                double val = ex.getDouble("movement_speed");
                out.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(),
                        new AttributeModifier(UPGRADE_MS_UUID, "Sword Upgrade Speed", val, 0));
            }
            if (ex.hasKey("armor")) {
                double val = ex.getDouble("armor");
                out.put(SharedMonsterAttributes.ARMOR.getName(),
                        new AttributeModifier(UPGRADE_ARMOR_UUID, "Sword Upgrade Armor", val, 0));
            }
            if (ex.hasKey("armor_toughness")) {
                double val = ex.getDouble("armor_toughness");
                out.put(SharedMonsterAttributes.ARMOR_TOUGHNESS.getName(),
                        new AttributeModifier(UPGRADE_TOUGH_UUID, "Sword Upgrade Toughness", val, 0));
            }
            if (ex.hasKey("knockback_resistance")) {
                double val = ex.getDouble("knockback_resistance");
                out.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(),
                        new AttributeModifier(UPGRADE_KB_UUID, "Sword Upgrade KB Resist", val, 0));
            }
        }

        return out;
    }

    // 修正：檢查正確的 NBT key
    private static NBTTagCompound getUpgradeTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return null;
        }

        NBTTagCompound tag = stack.getTagCompound();

        // 主要檢查 "SwordUpgrades" (這是 TileEntitySwordUpgradeStation 使用的)
        if (tag.hasKey("SwordUpgrades")) {
            return tag.getCompoundTag("SwordUpgrades");
        }

        // 備用檢查其他可能的名稱
        if (tag.hasKey("MoreModUpgrades")) {
            return tag.getCompoundTag("MoreModUpgrades");
        }

        if (tag.hasKey("Upgrades")) {
            return tag.getCompoundTag("Upgrades");
        }

        return null;
    }
}