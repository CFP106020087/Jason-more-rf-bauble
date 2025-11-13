package com.moremod.accessorybox.unlock.rules.progress;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.accessorybox.unlock.rules.UnlockCondition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
/**
 * 佩戴饰品条件
 */
public class WearBaubleCondition implements UnlockCondition {
    private final String baubleId;
    private final boolean temporary;

    public WearBaubleCondition(String baubleId, boolean temporary) {
        this.baubleId = baubleId;
        this.temporary = temporary;
    }

    @Override
    public boolean check(EntityPlayer player) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return false;

        String[] parts = baubleId.split(":");
        String modId = parts[0];
        String itemName = parts[1];
        int meta = parts.length > 2 ? Integer.parseInt(parts[2]) : -1;

        Item targetItem = Item.REGISTRY.getObject(new ResourceLocation(modId, itemName));
        if (targetItem == null) return false;

        // ⭐ 只检查原版7个槽位（0-6），避免循环依赖
        // 如果检查所有槽位，玩家可以把触发物品放入临时解锁的槽位，导致条件永远为真
        int maxSlot = Math.min(7, handler.getSlots());
        for (int i = 0; i < maxSlot; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                if (meta < 0 || stack.getMetadata() == meta) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isTemporary() {
        return temporary;
    }

    @Override
    public String getType() {
        return "wear_bauble";
    }

    @Override
    public String getDescription() {
        return "佩戴 " + baubleId + (temporary ? " (临时)" : "");
    }
}
