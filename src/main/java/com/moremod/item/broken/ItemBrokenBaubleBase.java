package com.moremod.item.broken;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.system.ascension.BrokenGodHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

/**
 * 破碎饰品基类
 * Base class for all Broken God baubles
 *
 * 特性：
 * - 右键装备时自动替换槽位中的现有饰品
 * - 破碎之神无法摘下
 * - 只有破碎之神能装备
 */
public abstract class ItemBrokenBaubleBase extends Item implements IBauble {

    public ItemBrokenBaubleBase() {
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

        // 必须是破碎之神才能装备
        if (!BrokenGodHandler.isBrokenGod(player)) {
            return new ActionResult<>(EnumActionResult.FAIL, heldStack);
        }

        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) {
            return new ActionResult<>(EnumActionResult.FAIL, heldStack);
        }

        BaubleType type = getBaubleType(heldStack);
        int[] validSlots = type.getValidSlots();

        for (int slot : validSlots) {
            // 检查该槽位是否可以放置此类型
            if (!isValidSlot(slot, type)) continue;

            ItemStack existing = baubles.getStackInSlot(slot);

            // 如果槽位已有同类物品，跳过
            if (!existing.isEmpty() && existing.getItem() == this) {
                continue;
            }

            // 取出旧饰品，退回物品栏或掉落
            if (!existing.isEmpty()) {
                // 先尝试放入物品栏
                if (!player.inventory.addItemStackToInventory(existing.copy())) {
                    // 物品栏满了，掉落到地上
                    player.dropItem(existing.copy(), false);
                }
                baubles.setStackInSlot(slot, ItemStack.EMPTY);
            }

            // 装入新的破碎饰品
            ItemStack toEquip = heldStack.copy();
            toEquip.setCount(1);
            baubles.setStackInSlot(slot, toEquip);

            // 触发装备回调
            if (toEquip.getItem() instanceof IBauble) {
                ((IBauble) toEquip.getItem()).onEquipped(toEquip, player);
            }

            // 消耗手中物品
            heldStack.shrink(1);

            return new ActionResult<>(EnumActionResult.SUCCESS, heldStack);
        }

        return new ActionResult<>(EnumActionResult.FAIL, heldStack);
    }

    /**
     * 检查槽位是否适合此饰品类型
     */
    private boolean isValidSlot(int slot, BaubleType type) {
        for (int validSlot : type.getValidSlots()) {
            if (validSlot == slot) return true;
        }
        return false;
    }

    /**
     * 只有破碎之神才能装备
     */
    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            return BrokenGodHandler.isBrokenGod((EntityPlayer) player);
        }
        return false;
    }

    /**
     * 破碎之神无法摘下
     */
    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            return !BrokenGodHandler.isBrokenGod((EntityPlayer) player);
        }
        return true;
    }

    /**
     * 子类必须实现：返回饰品类型
     */
    @Override
    public abstract BaubleType getBaubleType(ItemStack itemstack);
}
