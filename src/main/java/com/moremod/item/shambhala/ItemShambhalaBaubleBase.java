package com.moremod.item.shambhala;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.system.ascension.ShambhalaHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

/**
 * 香巴拉饰品基类
 * Base class for all Shambhala baubles
 *
 * 特性：
 * - 右键装备时自动替换槽位中的现有饰品
 * - 香巴拉无法摘下
 * - 只有香巴拉能装备
 */
public abstract class ItemShambhalaBaubleBase extends Item implements IBauble {

    public ItemShambhalaBaubleBase() {
        this.setMaxStackSize(1);
    }

    /**
     * 右键使用 - 强制装备并替换现有饰品
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack heldStack = player.getHeldItem(hand);

        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, heldStack);
        }

        // 必须是香巴拉才能装备
        if (!ShambhalaHandler.isShambhala(player)) {
            return new ActionResult<>(EnumActionResult.FAIL, heldStack);
        }

        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) {
            return new ActionResult<>(EnumActionResult.FAIL, heldStack);
        }

        BaubleType type = getBaubleType(heldStack);
        int[] validSlots = type.getValidSlots();

        for (int slot : validSlots) {
            if (!isValidSlot(slot, type)) continue;

            ItemStack existing = baubles.getStackInSlot(slot);

            if (!existing.isEmpty() && existing.getItem() == this) {
                continue;
            }

            if (!existing.isEmpty()) {
                if (!player.inventory.addItemStackToInventory(existing.copy())) {
                    player.dropItem(existing.copy(), false);
                }
                baubles.setStackInSlot(slot, ItemStack.EMPTY);
            }

            ItemStack toEquip = heldStack.copy();
            toEquip.setCount(1);
            baubles.setStackInSlot(slot, toEquip);

            if (toEquip.getItem() instanceof IBauble) {
                ((IBauble) toEquip.getItem()).onEquipped(toEquip, player);
            }

            heldStack.shrink(1);
            return new ActionResult<>(EnumActionResult.SUCCESS, heldStack);
        }

        return new ActionResult<>(EnumActionResult.FAIL, heldStack);
    }

    private boolean isValidSlot(int slot, BaubleType type) {
        for (int validSlot : type.getValidSlots()) {
            if (validSlot == slot) return true;
        }
        return false;
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            return ShambhalaHandler.isShambhala((EntityPlayer) player);
        }
        return false;
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            return !ShambhalaHandler.isShambhala((EntityPlayer) player);
        }
        return true;
    }

    @Override
    public abstract BaubleType getBaubleType(ItemStack itemstack);
}
