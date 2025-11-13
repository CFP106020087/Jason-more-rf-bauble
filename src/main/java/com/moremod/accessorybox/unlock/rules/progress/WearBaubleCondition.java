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
        if (handler == null) {
            if (com.moremod.accessorybox.unlock.rules.UnlockRulesConfig.debugMode) {
                System.out.println("[WearBauble] handler為null: " + player.getName());
            }
            return false;
        }

        String[] parts = baubleId.split(":");
        String modId = parts[0];
        String itemName = parts[1];
        int meta = parts.length > 2 ? Integer.parseInt(parts[2]) : -1;

        Item targetItem = Item.REGISTRY.getObject(new ResourceLocation(modId, itemName));
        if (targetItem == null) {
            if (com.moremod.accessorybox.unlock.rules.UnlockRulesConfig.debugMode) {
                System.out.println("[WearBauble] 物品未註冊: " + baubleId);
            }
            return false;
        }

        if (com.moremod.accessorybox.unlock.rules.UnlockRulesConfig.debugMode) {
            System.out.println("[WearBauble] 檢查玩家: " + player.getName() +
                ", 目標物品: " + baubleId + ", 臨時=" + temporary);
        }

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                if (meta < 0 || stack.getMetadata() == meta) {
                    if (com.moremod.accessorybox.unlock.rules.UnlockRulesConfig.debugMode) {
                        System.out.println("[WearBauble] ✓ 在槽位 " + i + " 找到: " +
                            stack.getDisplayName());
                    }
                    return true;
                }
            }
        }

        if (com.moremod.accessorybox.unlock.rules.UnlockRulesConfig.debugMode) {
            System.out.println("[WearBauble] ✗ 未找到飾品: " + baubleId);
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
