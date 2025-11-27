package com.moremod.system.ascension;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemMechanicalCore;
<<<<<<< HEAD
import com.moremod.item.broken.ItemBrokenArm;
import com.moremod.item.broken.ItemBrokenHand;
import com.moremod.item.broken.ItemBrokenHeart;
import com.moremod.item.broken.ItemBrokenShackles;
import com.moremod.item.broken.ItemBrokenProjection;
import com.moremod.item.broken.ItemBrokenTerminus;
=======
import com.moremod.item.broken.ItemBrokenEye;
import com.moremod.item.broken.ItemBrokenHand;
import com.moremod.item.broken.ItemBrokenHeart;
>>>>>>> origin/newest
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 破碎之神物品管理
 * Broken God Items Manager
 *
 * 管理破碎三件套的装备和卸除逻辑
 */
public class BrokenGodItems {

    private static final Logger LOGGER = LogManager.getLogger("moremod");

<<<<<<< HEAD
    // 物品实例（在 RegisterItem 中注册）
    public static ItemBrokenHand BROKEN_HAND;
    public static ItemBrokenHeart BROKEN_HEART;
    public static ItemBrokenArm BROKEN_ARM;
    public static ItemBrokenShackles BROKEN_SHACKLES;
    public static ItemBrokenProjection BROKEN_PROJECTION;
    public static ItemBrokenTerminus BROKEN_TERMINUS;
=======
    // 物品实例（在 ModItems 中注册）
    public static ItemBrokenHand BROKEN_HAND;
    public static ItemBrokenHeart BROKEN_HEART;
    public static ItemBrokenEye BROKEN_EYE;
>>>>>>> origin/newest

    /**
     * 替换玩家的所有饰品为破碎三件套
     * 保留机械核心，移除其他所有饰品
     */
    public static void replacePlayerBaubles(EntityPlayer player) {
        if (player.world.isRemote) return;

        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) {
            LOGGER.warn("[BrokenGod] Cannot get baubles handler for player {}", player.getName());
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
<<<<<<< HEAD
                if (isBrokenItem(stack)) {
=======
                if (stack.getItem() instanceof ItemBrokenHand ||
                    stack.getItem() instanceof ItemBrokenHeart ||
                    stack.getItem() instanceof ItemBrokenEye) {
>>>>>>> origin/newest
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

<<<<<<< HEAD
        // 破碎之臂 - 第二个 RING 槽位
        if (BROKEN_ARM != null) {
            int ringSlot2 = findSecondRingSlot(baubles);
            if (ringSlot2 >= 0) {
                baubles.setStackInSlot(ringSlot2, new ItemStack(BROKEN_ARM));
            }
        }

        // 破碎_枷锁 - BELT 槽位
        if (BROKEN_SHACKLES != null) {
            int beltSlot = findEmptySlotForType(baubles, BaubleType.BELT);
            if (beltSlot >= 0) {
                baubles.setStackInSlot(beltSlot, new ItemStack(BROKEN_SHACKLES));
            }
        }

        // 破碎_投影 - CHARM 槽位
        if (BROKEN_PROJECTION != null) {
            int charmSlot = findEmptySlotForType(baubles, BaubleType.CHARM);
            if (charmSlot >= 0) {
                baubles.setStackInSlot(charmSlot, new ItemStack(BROKEN_PROJECTION));
            }
        }

        // 破碎_终结 - BODY 槽位
        if (BROKEN_TERMINUS != null) {
            int bodySlot = findEmptySlotForType(baubles, BaubleType.BODY);
            if (bodySlot >= 0) {
                baubles.setStackInSlot(bodySlot, new ItemStack(BROKEN_TERMINUS));
            }
        }

        LOGGER.info("[BrokenGod] Replaced baubles for player {} with full broken set", player.getName());
    }

    /**
     * 找到第二个 RING 槽位（槽位2）
     */
    private static int findSecondRingSlot(IBaublesItemHandler baubles) {
        // Baubles 的 RING 槽位是 1 和 2，返回第二个
        if (baubles.getStackInSlot(2).isEmpty()) {
            return 2;
        }
        // 如果槽位2被占用，尝试槽位1
        if (baubles.getStackInSlot(1).isEmpty()) {
            return 1;
        }
        return -1;
=======
        // 破碎之眼 - HEAD 槽位
        if (BROKEN_EYE != null) {
            int headSlot = findEmptySlotForType(baubles, BaubleType.HEAD);
            if (headSlot >= 0) {
                baubles.setStackInSlot(headSlot, new ItemStack(BROKEN_EYE));
            }
        }

        LOGGER.info("[BrokenGod] Replaced baubles for player {}", player.getName());
>>>>>>> origin/newest
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
<<<<<<< HEAD
     * 检查玩家是否装备了完整的破碎套装（基础三件）
=======
     * 检查玩家是否装备了完整的破碎套装
>>>>>>> origin/newest
     */
    public static boolean hasFullBrokenSet(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return false;

        boolean hasHand = false;
        boolean hasHeart = false;
<<<<<<< HEAD
        boolean hasArm = false;
=======
        boolean hasEye = false;
>>>>>>> origin/newest

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (stack.getItem() instanceof ItemBrokenHand) hasHand = true;
            if (stack.getItem() instanceof ItemBrokenHeart) hasHeart = true;
<<<<<<< HEAD
            if (stack.getItem() instanceof ItemBrokenArm) hasArm = true;
        }

        return hasHand && hasHeart && hasArm;
    }

    /**
     * 检查玩家是否装备了完整的破碎终局套装（全部六件）
     */
    public static boolean hasFullEndgameSet(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return false;

        boolean hasHand = false;
        boolean hasHeart = false;
        boolean hasArm = false;
        boolean hasShackles = false;
        boolean hasProjection = false;
        boolean hasTerminus = false;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (stack.getItem() instanceof ItemBrokenHand) hasHand = true;
            if (stack.getItem() instanceof ItemBrokenHeart) hasHeart = true;
            if (stack.getItem() instanceof ItemBrokenArm) hasArm = true;
            if (stack.getItem() instanceof ItemBrokenShackles) hasShackles = true;
            if (stack.getItem() instanceof ItemBrokenProjection) hasProjection = true;
            if (stack.getItem() instanceof ItemBrokenTerminus) hasTerminus = true;
        }

        return hasHand && hasHeart && hasArm && hasShackles && hasProjection && hasTerminus;
=======
            if (stack.getItem() instanceof ItemBrokenEye) hasEye = true;
        }

        return hasHand && hasHeart && hasEye;
>>>>>>> origin/newest
    }

    /**
     * 检查某物品是否是破碎套装
     */
    public static boolean isBrokenItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof ItemBrokenHand ||
               stack.getItem() instanceof ItemBrokenHeart ||
<<<<<<< HEAD
               stack.getItem() instanceof ItemBrokenArm ||
               stack.getItem() instanceof ItemBrokenShackles ||
               stack.getItem() instanceof ItemBrokenProjection ||
               stack.getItem() instanceof ItemBrokenTerminus;
=======
               stack.getItem() instanceof ItemBrokenEye;
>>>>>>> origin/newest
    }
}
