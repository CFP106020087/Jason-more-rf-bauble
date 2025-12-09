package com.moremod.item.ritual;


import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 诅咒之镜 (Cursed Mirror)
 *
 * 复制仪式的核心组件
 * 放在祭坛中心，周围放虚空精华，可以复制镜中存储的物品
 * 但失败时会毁掉原物品
 */
public class ItemCursedMirror extends Item {

    public ItemCursedMirror() {
        setTranslationKey("moremod.cursed_mirror");
        setRegistryName("cursed_mirror");
        setMaxStackSize(1);
        setMaxDamage(10); // 10次使用后损坏
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return hasStoredItem(stack);
    }

    /**
     * 右键存储/取出物品
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack mirror = playerIn.getHeldItem(handIn);

        if (!worldIn.isRemote) {
            ItemStack offhand = playerIn.getHeldItemOffhand();

            if (handIn == EnumHand.MAIN_HAND && !offhand.isEmpty() && !hasStoredItem(mirror)) {
                // 存储副手物品到镜子
                storeItem(mirror, offhand.copy());
                offhand.setCount(0);

                playerIn.sendMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "物品被镜面吸收..."
                ));
            } else if (hasStoredItem(mirror)) {
                // 取出存储的物品
                ItemStack stored = getStoredItem(mirror);
                if (!stored.isEmpty()) {
                    if (!playerIn.inventory.addItemStackToInventory(stored)) {
                        playerIn.dropItem(stored, false);
                    }
                    clearStoredItem(mirror);

                    playerIn.sendMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "物品从镜中浮现..."
                    ));
                }
            } else {
                playerIn.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "将要复制的物品放在副手，然后右键镜子"
                ));
            }
        }

        return ActionResult.newResult(EnumActionResult.SUCCESS, mirror);
    }

    // ========== 物品存储方法 ==========

    public static boolean hasStoredItem(ItemStack mirror) {
        if (!mirror.hasTagCompound()) return false;
        return mirror.getTagCompound().hasKey("StoredItem");
    }

    public static ItemStack getStoredItem(ItemStack mirror) {
        if (!hasStoredItem(mirror)) return ItemStack.EMPTY;
        NBTTagCompound itemTag = mirror.getTagCompound().getCompoundTag("StoredItem");
        return new ItemStack(itemTag);
    }

    public static void storeItem(ItemStack mirror, ItemStack toStore) {
        if (!mirror.hasTagCompound()) {
            mirror.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound itemTag = new NBTTagCompound();
        toStore.writeToNBT(itemTag);
        mirror.getTagCompound().setTag("StoredItem", itemTag);
    }

    public static void clearStoredItem(ItemStack mirror) {
        if (mirror.hasTagCompound()) {
            mirror.getTagCompound().removeTag("StoredItem");
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_PURPLE + "映照虚空的诅咒之镜");
        tooltip.add("");

        if (hasStoredItem(stack)) {
            ItemStack stored = getStoredItem(stack);
            tooltip.add(TextFormatting.GRAY + "镜中之物: " + TextFormatting.WHITE + stored.getDisplayName());
            tooltip.add(TextFormatting.DARK_GRAY + "x" + stored.getCount());
        } else {
            tooltip.add(TextFormatting.GRAY + "镜中空无一物");
            tooltip.add(TextFormatting.DARK_GRAY + "副手持物品右键存入");
        }

        tooltip.add("");
        tooltip.add(TextFormatting.RED + "用于复制仪式");
        tooltip.add(TextFormatting.DARK_GRAY + "将镜子放在祭坛中心");
        tooltip.add(TextFormatting.DARK_GRAY + "周围放置8个虚空精华");
        tooltip.add(TextFormatting.DARK_RED + "成功率: 1%");

        int uses = stack.getMaxDamage() - stack.getItemDamage();
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "剩余使用次数: " + TextFormatting.YELLOW + uses);
    }
}
