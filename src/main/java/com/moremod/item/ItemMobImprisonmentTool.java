package com.moremod.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;

public class ItemMobImprisonmentTool extends Item {

    public ItemMobImprisonmentTool() {
        setTranslationKey("mob_imprisonment_tool");
        setRegistryName("mob_imprisonment_tool");
        setMaxStackSize(1);
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player,
                                            EntityLivingBase target, EnumHand hand) {
        if (!player.world.isRemote && target instanceof EntityVillager) {
            // 檢查是否已經有實體
            if (hasEntity(stack)) {
                return false;
            }

            // 捕捉村民
            EntityVillager villager = (EntityVillager) target;
            captureEntity(stack, villager);
            target.setDead();

            return true;
        }
        return false;
    }

    public static void captureEntity(ItemStack stack, EntityVillager villager) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }

        NBTTagCompound entityData = new NBTTagCompound();
        villager.writeToNBT(entityData);
        tag.setTag("EntityData", entityData);
        tag.setString("EntityType", "minecraft:villager");
    }

    public static boolean hasEntity(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey("EntityData");
    }

    public static EntityVillager releaseEntity(ItemStack stack, EntityPlayer player) {
        if (!hasEntity(stack)) return null;

        NBTTagCompound tag = stack.getTagCompound();
        NBTTagCompound entityData = tag.getCompoundTag("EntityData");

        EntityVillager villager = new EntityVillager(player.world);
        villager.readFromNBT(entityData);

        return villager;
    }
}