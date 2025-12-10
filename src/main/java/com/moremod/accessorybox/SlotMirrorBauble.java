package com.moremod.accessorybox;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.accessorybox.compat.AccessoryBoxCrTCompat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class SlotMirrorBauble extends SlotItemHandler {
    private final int baubleSlot;
    private final EntityPlayer player;
    private final IBaublesItemHandler baublesHandler;
    private final BaubleType expectedType; // 改用 BaubleType 而不是 tier

    public SlotMirrorBauble(IBaublesItemHandler baublesHandler, EntityPlayer player,
                            int baubleSlot, int x, int y, BaubleType expectedType) {
        super(baublesHandler, baubleSlot, x, y);
        this.baubleSlot = baubleSlot;
        this.player = player;
        this.baublesHandler = baublesHandler;
        this.expectedType = expectedType;
    }

    // 兼容旧版本的构造函数 - 根据槽位ID推断类型
    @Deprecated
    public SlotMirrorBauble(IBaublesItemHandler baublesHandler, EntityPlayer player,
                            int baubleSlot, int x, int y, int tier) {
        this(baublesHandler, player, baubleSlot, x, y, inferTypeFromSlot(baubleSlot, tier));
    }

    // 兼容最旧版本的构造函数（默认为tier 3）
    @Deprecated
    public SlotMirrorBauble(IBaublesItemHandler baublesHandler, EntityPlayer player,
                            int baubleSlot, int x, int y) {
        this(baublesHandler, player, baubleSlot, x, y, 3);
    }

    /**
     * 从旧的槽位ID和tier推断类型（兼容旧代码）
     */
    private static BaubleType inferTypeFromSlot(int slotId, int tier) {
        // 原版槽位 0-6
        if (slotId < 7) {
            switch (slotId) {
                case 0: return BaubleType.AMULET;
                case 1:
                case 2: return BaubleType.RING;
                case 3: return BaubleType.BELT;
                case 4: return BaubleType.HEAD;
                case 5: return BaubleType.BODY;
                case 6: return BaubleType.CHARM;
                default: return BaubleType.TRINKET;
            }
        }

        // 额外槽位 - 从 SlotLayoutHelper 查询
        SlotLayoutHelper.SlotAllocation alloc = SlotLayoutHelper.calculateSlotAllocation();

        if (contains(alloc.amuletSlots, slotId)) return BaubleType.AMULET;
        if (contains(alloc.ringSlots, slotId)) return BaubleType.RING;
        if (contains(alloc.beltSlots, slotId)) return BaubleType.BELT;
        if (contains(alloc.headSlots, slotId)) return BaubleType.HEAD;
        if (contains(alloc.bodySlots, slotId)) return BaubleType.BODY;
        if (contains(alloc.charmSlots, slotId)) return BaubleType.CHARM;

        // 默认为 TRINKET（万能槽位）
        return BaubleType.TRINKET;
    }

    private static boolean contains(int[] array, int value) {
        for (int v : array) {
            if (v == value) return true;
        }
        return false;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IBauble)) {
            return false;
        }

        IBauble bauble = (IBauble) stack.getItem();
        BaubleType type = bauble.getBaubleType(stack);

        // TRINKET 可以放在任何槽位
        if (type == BaubleType.TRINKET) {
            return bauble.canEquip(stack, player);
        }

        // 如果期望类型是 TRINKET（万能槽位），接受任何类型
        if (expectedType == BaubleType.TRINKET) {
            return bauble.canEquip(stack, player);
        }

        // 其他情况需要类型匹配
        return type == expectedType && bauble.canEquip(stack, player);
    }

    @Override
    public void putStack(ItemStack stack) {
        ItemStack oldStack = this.getStack();

        // 检查物品是否真的发生了变化，避免反复触发佩戴事件
        if (ItemStack.areItemStacksEqual(oldStack, stack)) {
            // 物品完全相同，不触发任何事件
            return;
        }

        // 检查是否是相同的物品实例（引用相等）
        if (oldStack == stack) {
            return;
        }

        // 如果有旧物品，触发卸下事件
        if (!oldStack.isEmpty() && oldStack.getItem() instanceof IBauble) {
            boolean canceled = AccessoryBoxCrTCompat.fireUnequipPre(player, baubleSlot, oldStack);

            if (!canceled) {
                ((IBauble) oldStack.getItem()).onUnequipped(oldStack, player);
                AccessoryBoxCrTCompat.fireUnequipPost(player, baubleSlot, oldStack);
            } else {
                return;
            }
        }

        super.putStack(stack);

        // 如果有新物品，触发装备事件
        if (!stack.isEmpty() && stack.getItem() instanceof IBauble) {
            boolean canceled = AccessoryBoxCrTCompat.fireEquipPre(player, baubleSlot, stack);

            if (!canceled) {
                ((IBauble) stack.getItem()).onEquipped(stack, player);
                AccessoryBoxCrTCompat.fireEquipPost(player, baubleSlot, stack);
            } else {
                super.putStack(oldStack);
            }
        }
    }

    @Override
    public ItemStack onTake(EntityPlayer playerIn, ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof IBauble) {
            boolean canceled = AccessoryBoxCrTCompat.fireUnequipPre(playerIn, baubleSlot, stack);

            if (!canceled) {
                ((IBauble) stack.getItem()).onUnequipped(stack, playerIn);
                AccessoryBoxCrTCompat.fireUnequipPost(playerIn, baubleSlot, stack);
                return super.onTake(playerIn, stack);
            } else {
                return ItemStack.EMPTY;
            }
        }
        return super.onTake(playerIn, stack);
    }

    @Override
    public boolean canTakeStack(EntityPlayer playerIn) {
        ItemStack stack = this.getStack();
        if (!stack.isEmpty() && stack.getItem() instanceof IBauble) {
            return ((IBauble) stack.getItem()).canUnequip(stack, playerIn);
        }
        return true;
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }

    public int getBaubleSlot() {
        return baubleSlot;
    }

    public BaubleType getExpectedType() {
        return expectedType;
    }
}