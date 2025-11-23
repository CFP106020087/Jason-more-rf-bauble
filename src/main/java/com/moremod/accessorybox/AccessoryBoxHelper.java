package com.moremod.accessorybox;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.BaublesCapabilities;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.accessorybox.unlock.SlotUnlockManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class AccessoryBoxHelper {

    /**
     * 检查物品是否可以放入指定槽位
     */
    public static boolean isValidForExtraSlot(int slot, ItemStack stack, EntityLivingBase entity) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }

        if (!(entity instanceof EntityPlayer)) {
            return false;
        }

        EntityPlayer player = (EntityPlayer) entity;

        // 检查是否为 Bauble
        if (!stack.hasCapability(BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null)) {
            return false;
        }

        IBauble bauble = stack.getCapability(BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null);
        if (bauble == null) {
            return false;
        }

        // 检查是否可装备
        if (!bauble.canEquip(stack, player)) {
            return false;
        }

        BaubleType itemType = bauble.getBaubleType(stack);

        // ⭐ TRINKET 类型的物品可以放在任何槽位（包括原版和额外槽位）
        if (itemType == BaubleType.TRINKET) {
            // 如果是额外槽位，需要检查是否解锁
            if (SlotLayoutHelper.isExtraSlot(slot)) {
                return SlotUnlockManager.getInstance().isSlotUnlocked(player, slot);
            }
            return true;
        }

        // 原版槽位（0-6）
        if (!SlotLayoutHelper.isExtraSlot(slot)) {
            return isValidForVanillaSlot(slot, itemType);
        }

        // 额外槽位：检查是否解锁
        if (!SlotUnlockManager.getInstance().isSlotUnlocked(player, slot)) {
            return false;
        }

        // 获取槽位期望类型
        BaubleType slotType = SlotLayoutHelper.getExpectedTypeForSlot(slot);

        // TRINKET 类型的槽位可以接受任何类型的饰品
        if (slotType == BaubleType.TRINKET) {
            return true;
        }

        // 其他情况需要类型匹配
        return itemType == slotType;
    }

    /**
     * 检查物品类型是否匹配原版槽位
     */
    private static boolean isValidForVanillaSlot(int slot, BaubleType itemType) {
        switch(slot) {
            case 0: return itemType == BaubleType.AMULET;
            case 1:
            case 2: return itemType == BaubleType.RING;
            case 3: return itemType == BaubleType.BELT;
            case 4: return itemType == BaubleType.HEAD;
            case 5: return itemType == BaubleType.BODY;
            case 6: return itemType == BaubleType.CHARM;
            default: return false;
        }
    }

    /**
     * @deprecated 使用新的解锁系统
     */
    @Deprecated
    public static int getHighestAccessoryBoxTier(EntityPlayer player) {
        int maxTier = 0;

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemAccessoryBox) {
                ItemAccessoryBox box = (ItemAccessoryBox) stack.getItem();
                maxTier = Math.max(maxTier, box.getTier());
            }
        }

        ItemStack mouseStack = player.inventory.getItemStack();
        if (!mouseStack.isEmpty() && mouseStack.getItem() instanceof ItemAccessoryBox) {
            ItemAccessoryBox box = (ItemAccessoryBox) mouseStack.getItem();
            maxTier = Math.max(maxTier, box.getTier());
        }

        return maxTier;
    }

    /**
     * @deprecated 使用 SlotUnlockManager.isSlotUnlocked()
     */
    @Deprecated
    public static boolean isSlotAllowedForTier(int slot, int tier) {
        return false;
    }

    public static boolean checkEnigmaticAmuletProperly(EntityPlayer player, int requiredMeta) {
        try {
            Class<?> enigmaticClass = Class.forName("keletu.enigmaticlegacy.EnigmaticLegacy");
            Item ascensionAmulet = (Item) enigmaticClass.getField("ascensionAmulet").get(null);
            Item enigmaticAmulet = (Item) enigmaticClass.getField("enigmaticAmulet").get(null);

            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles == null) return false;

            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == ascensionAmulet) {
                    return true;
                }
            }

            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == enigmaticAmulet) {
                    if (stack.getMetadata() == requiredMeta) {
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            // 静默处理
        }

        return false;
    }

    public static int findItemInAllBaubleSlots(EntityPlayer player, Item item) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return -1;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }

        return -1;
    }

    public static int findItemWithMetaInBaubleSlots(EntityPlayer player, Item item, int meta) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return -1;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item && stack.getMetadata() == meta) {
                return i;
            }
        }

        return -1;
    }

    public static int[] getAvailableSlots(EntityPlayer player) {
        return SlotUnlockManager.getInstance().getAvailableSlots(player.getUniqueID())
                .stream().mapToInt(Integer::intValue).sorted().toArray();
    }

    public static boolean hasEmptySlotOfType(EntityPlayer player, BaubleType type) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return false;

        int[] slots = SlotLayoutHelper.getSlotIdsForType(type.name());

        for (int slot : slots) {
            if (SlotUnlockManager.getInstance().isSlotUnlocked(player, slot)) {
                ItemStack stack = baubles.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    public static int getAvailableSlotCountForType(EntityPlayer player, BaubleType type) {
        int[] slots = SlotLayoutHelper.getSlotIdsForType(type.name());
        int count = 0;

        for (int slot : slots) {
            if (SlotUnlockManager.getInstance().isSlotUnlocked(player, slot)) {
                count++;
            }
        }

        return count;
    }
}