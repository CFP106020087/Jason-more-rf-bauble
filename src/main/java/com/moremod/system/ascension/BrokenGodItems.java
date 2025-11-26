package com.moremod.system.ascension;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.broken.ItemBrokenEye;
import com.moremod.item.broken.ItemBrokenHand;
import com.moremod.item.broken.ItemBrokenHeart;
import com.moremod.moremod;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * 破碎之神物品管理
 * Broken God Items Manager
 *
 * 管理破碎三件套的装备和卸除逻辑
 */
public class BrokenGodItems {

    // 物品实例（在 ModItems 中注册）
    public static ItemBrokenHand BROKEN_HAND;
    public static ItemBrokenHeart BROKEN_HEART;
    public static ItemBrokenEye BROKEN_EYE;

    /**
     * 替换玩家的所有饰品为破碎三件套
     * 保留机械核心，移除其他所有饰品
     */
    public static void replacePlayerBaubles(EntityPlayer player) {
        if (player.world.isRemote) return;

        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) {
            return;
        }

        // 第一步：移除所有非核心饰品，掉落到地上
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty()) {
                // 检查是否是机械核心 - 保留
                if (stack.getItem() instanceof ItemMechanicalCore) {
                    continue;
                }
                // 检查是否已经是破碎套装 - 保留
                if (stack.getItem() instanceof ItemBrokenHand ||
                    stack.getItem() instanceof ItemBrokenHeart ||
                    stack.getItem() instanceof ItemBrokenEye) {
                    continue;
                }

                // 移除并掉落
                baubles.setStackInSlot(i, ItemStack.EMPTY);
                EntityItem entityItem = new EntityItem(player.world,
                        player.posX, player.posY + 0.5, player.posZ,
                        stack.copy());
                entityItem.setPickupDelay(20);
                player.world.spawnEntity(entityItem);
            }
        }

        // 第二步：装备破碎三件套
        // 破碎之手 - RING 槽位
        if (BROKEN_HAND != null) {
            int ringSlot = findEmptySlotForType(baubles, BaubleType.RING);
            if (ringSlot >= 0) {
                baubles.setStackInSlot(ringSlot, new ItemStack(BROKEN_HAND));
            }
        }

        // 破碎之心 - AMULET 槽位
        if (BROKEN_HEART != null) {
            int amuletSlot = findEmptySlotForType(baubles, BaubleType.AMULET);
            if (amuletSlot >= 0) {
                baubles.setStackInSlot(amuletSlot, new ItemStack(BROKEN_HEART));
            }
        }

        // 破碎之眼 - HEAD 槽位
        if (BROKEN_EYE != null) {
            int headSlot = findEmptySlotForType(baubles, BaubleType.HEAD);
            if (headSlot >= 0) {
                baubles.setStackInSlot(headSlot, new ItemStack(BROKEN_EYE));
            }
        }

    }

    /**
     * 找到指定类型的空槽位
     */
    private static int findEmptySlotForType(IBaublesItemHandler baubles, BaubleType type) {
        for (int i = 0; i < baubles.getSlots(); i++) {
            if (baubles.getStackInSlot(i).isEmpty()) {
                // 检查槽位是否接受该类型
                // Baubles 的槽位映射: 0=AMULET, 1-2=RING, 3=BELT, 4=HEAD, 5=BODY, 6=CHARM
                switch (type) {
                    case AMULET:
                        if (i == 0) return i;
                        break;
                    case RING:
                        if (i == 1 || i == 2) return i;
                        break;
                    case BELT:
                        if (i == 3) return i;
                        break;
                    case HEAD:
                        if (i == 4) return i;
                        break;
                    case BODY:
                        if (i == 5) return i;
                        break;
                    case CHARM:
                        if (i == 6) return i;
                        break;
                    case TRINKET:
                        return i; // 任意槽位
                }
            }
        }
        return -1;
    }

    /**
     * 检查玩家是否装备了完整的破碎套装
     */
    public static boolean hasFullBrokenSet(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return false;

        boolean hasHand = false;
        boolean hasHeart = false;
        boolean hasEye = false;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (stack.getItem() instanceof ItemBrokenHand) hasHand = true;
            if (stack.getItem() instanceof ItemBrokenHeart) hasHeart = true;
            if (stack.getItem() instanceof ItemBrokenEye) hasEye = true;
        }

        return hasHand && hasHeart && hasEye;
    }

    /**
     * 检查某物品是否是破碎套装
     */
    public static boolean isBrokenItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof ItemBrokenHand ||
               stack.getItem() instanceof ItemBrokenHeart ||
               stack.getItem() instanceof ItemBrokenEye;
    }
}
