package com.moremod.system.ascension;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.shambhala.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 香巴拉物品管理
 * Shambhala Items Manager
 *
 * 管理香巴拉六件套的装备和卸除逻辑
 */
public class ShambhalaItems {

    private static final Logger LOGGER = LogManager.getLogger("moremod");

    // 物品实例（在 RegisterItem 中注册）
    public static ItemShambhalaCore SHAMBHALA_CORE;           // 不灭之心 - AMULET
    public static ItemShambhalaBastion SHAMBHALA_BASTION;     // 绝对防御 - RING
    public static ItemShambhalaThorns SHAMBHALA_THORNS;       // 因果反噬 - RING
    public static ItemShambhalaPurify SHAMBHALA_PURIFY;       // 净化之力 - BELT
    public static ItemShambhalaVeil SHAMBHALA_VEIL;           // 反侦察 - HEAD
    public static ItemShambhalaSanctuary SHAMBHALA_SANCTUARY; // 圣域护盾 - BODY

    /**
     * 替换玩家的所有饰品为香巴拉六件套
     * 保留机械核心，移除其他所有饰品
     */
    public static void replacePlayerBaubles(EntityPlayer player) {
        if (player.world.isRemote) return;

        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) {
            LOGGER.warn("[Shambhala] Cannot get baubles handler for player {}", player.getName());
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
                // 检查是否已经是香巴拉套装 - 保留
                if (isShambhalaItem(stack)) {
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

        // 第二步：装备香巴拉六件套
        // 香巴拉_核心 - AMULET 槽位
        if (SHAMBHALA_CORE != null) {
            int amuletSlot = findEmptySlotForType(baubles, BaubleType.AMULET);
            if (amuletSlot >= 0) {
                baubles.setStackInSlot(amuletSlot, new ItemStack(SHAMBHALA_CORE));
            }
        }

        // 香巴拉_壁垒 - RING 槽位
        if (SHAMBHALA_BASTION != null) {
            int ringSlot = findEmptySlotForType(baubles, BaubleType.RING);
            if (ringSlot >= 0) {
                baubles.setStackInSlot(ringSlot, new ItemStack(SHAMBHALA_BASTION));
            }
        }

        // 香巴拉_棘刺 - 第二个 RING 槽位
        if (SHAMBHALA_THORNS != null) {
            int ringSlot2 = findSecondRingSlot(baubles);
            if (ringSlot2 >= 0) {
                baubles.setStackInSlot(ringSlot2, new ItemStack(SHAMBHALA_THORNS));
            }
        }

        // 香巴拉_净化 - BELT 槽位
        if (SHAMBHALA_PURIFY != null) {
            int beltSlot = findEmptySlotForType(baubles, BaubleType.BELT);
            if (beltSlot >= 0) {
                baubles.setStackInSlot(beltSlot, new ItemStack(SHAMBHALA_PURIFY));
            }
        }

        // 香巴拉_隐匿 - HEAD 槽位
        if (SHAMBHALA_VEIL != null) {
            int headSlot = findEmptySlotForType(baubles, BaubleType.HEAD);
            if (headSlot >= 0) {
                baubles.setStackInSlot(headSlot, new ItemStack(SHAMBHALA_VEIL));
            }
        }

        // 香巴拉_圣域 - BODY 槽位
        if (SHAMBHALA_SANCTUARY != null) {
            int bodySlot = findEmptySlotForType(baubles, BaubleType.BODY);
            if (bodySlot >= 0) {
                baubles.setStackInSlot(bodySlot, new ItemStack(SHAMBHALA_SANCTUARY));
            }
        }

        LOGGER.info("[Shambhala] Equipped Shambhala set for player {}", player.getName());
    }

    /**
     * 检查是否是香巴拉物品
     */
    public static boolean isShambhalaItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof ItemShambhalaBaubleBase;
    }

    /**
     * 查找指定类型的空槽位
     */
    private static int findEmptySlotForType(IBaublesItemHandler baubles, BaubleType type) {
        for (int i = 0; i < baubles.getSlots(); i++) {
            if (baubles.getStackInSlot(i).isEmpty()) {
                // 检查槽位类型
                if (isSlotValidForType(i, type)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 查找第二个RING槽位
     */
    private static int findSecondRingSlot(IBaublesItemHandler baubles) {
        int foundFirst = -1;
        for (int i = 0; i < baubles.getSlots(); i++) {
            if (isSlotValidForType(i, BaubleType.RING)) {
                if (foundFirst < 0) {
                    if (!baubles.getStackInSlot(i).isEmpty()) {
                        foundFirst = i;
                    }
                } else if (baubles.getStackInSlot(i).isEmpty()) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 检查槽位是否适合指定类型
     * Baubles标准槽位布局：
     * 0 = AMULET
     * 1-2 = RING
     * 3 = BELT
     * 4 = HEAD (如果启用)
     * 5 = BODY (如果启用)
     * 6 = CHARM (如果启用)
     */
    private static boolean isSlotValidForType(int slot, BaubleType type) {
        switch (type) {
            case AMULET:
                return slot == 0;
            case RING:
                return slot == 1 || slot == 2;
            case BELT:
                return slot == 3;
            case HEAD:
                return slot == 4;
            case BODY:
                return slot == 5;
            case CHARM:
                return slot == 6;
            default:
                return false;
        }
    }

    /**
     * 检查玩家是否装备了圣域（Sanctuary）
     */
    public static boolean hasSanctuary(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return false;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty() && SHAMBHALA_SANCTUARY != null && stack.getItem() == SHAMBHALA_SANCTUARY) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查玩家是否装备了完整香巴拉套装
     */
    public static boolean hasFullShambhalaSet(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return false;

        boolean hasCore = false;
        boolean hasBastion = false;
        boolean hasThorns = false;
        boolean hasPurify = false;
        boolean hasVeil = false;
        boolean hasSanctuary = false;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (SHAMBHALA_CORE != null && stack.getItem() == SHAMBHALA_CORE) hasCore = true;
            if (SHAMBHALA_BASTION != null && stack.getItem() == SHAMBHALA_BASTION) hasBastion = true;
            if (SHAMBHALA_THORNS != null && stack.getItem() == SHAMBHALA_THORNS) hasThorns = true;
            if (SHAMBHALA_PURIFY != null && stack.getItem() == SHAMBHALA_PURIFY) hasPurify = true;
            if (SHAMBHALA_VEIL != null && stack.getItem() == SHAMBHALA_VEIL) hasVeil = true;
            if (SHAMBHALA_SANCTUARY != null && stack.getItem() == SHAMBHALA_SANCTUARY) hasSanctuary = true;
        }

        return hasCore && hasBastion && hasThorns && hasPurify && hasVeil && hasSanctuary;
    }
}
