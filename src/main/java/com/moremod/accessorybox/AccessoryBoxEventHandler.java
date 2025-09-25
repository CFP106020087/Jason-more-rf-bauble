package com.moremod.accessorybox;

import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = "moremod")
public class AccessoryBoxEventHandler {

    // 追蹤裝備狀態 - 只記錄物品類型，不記錄 NBT
    private static final Map<EntityPlayer, Map<Integer, ItemInfo>> equippedItems = new WeakHashMap<>();

    // 簡單的物品信息類，只比較物品類型
    private static class ItemInfo {
        private final net.minecraft.item.Item item;
        private final int metadata;

        public ItemInfo(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                this.item = null;
                this.metadata = 0;
            } else {
                this.item = stack.getItem();
                this.metadata = stack.getMetadata();
            }
        }

        public boolean isSameItem(ItemInfo other) {
            if (other == null) return false;
            if (this.item == null && other.item == null) return true;
            if (this.item == null || other.item == null) return false;
            return this.item == other.item && this.metadata == other.metadata;
        }

        public boolean isEmpty() {
            return item == null;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.world.isRemote) {
            EntityPlayer player = event.player;
            IBaublesItemHandler baublesHandler = BaublesApi.getBaublesHandler(player);

            if (baublesHandler != null) {
                Map<Integer, ItemInfo> playerEquipped = equippedItems.computeIfAbsent(player, k -> new HashMap<>());

                // 處理額外槽位的飾品效果
                for (int i = 14; i <= 21; i++) {
                    if (i < baublesHandler.getSlots()) {
                        ItemStack currentStack = baublesHandler.getStackInSlot(i);
                        ItemInfo currentInfo = new ItemInfo(currentStack);
                        ItemInfo previousInfo = playerEquipped.get(i);

                        // 只在物品類型改變時觸發，忽略 NBT 更新
                        boolean itemTypeChanged = (previousInfo == null) || !currentInfo.isSameItem(previousInfo);

                        if (itemTypeChanged) {
                            // 卸下舊物品
                            if (previousInfo != null && !previousInfo.isEmpty()) {
                                // 需要獲取實際的舊物品來觸發事件
                                // 由於我們只存了類型信息，這裡使用一個簡化的方法
                                if (currentInfo.isEmpty() || currentInfo.item != previousInfo.item) {
                                    // 真的換了不同的物品，觸發卸下事件
                                    handleUnequip(player, previousInfo, i);
                                }
                            }

                            // 裝備新物品
                            if (!currentInfo.isEmpty()) {
                                if (previousInfo == null || previousInfo.isEmpty() || previousInfo.item != currentInfo.item) {
                                    // 真的裝備了新物品，觸發裝備事件
                                    if (currentStack.getItem() instanceof IBauble) {
                                        ((IBauble) currentStack.getItem()).onEquipped(currentStack, player);
                                    }
                                    handleAttributeModifierBauble(player, currentStack, i, true);
                                }
                            }

                            // 更新記錄
                            playerEquipped.put(i, currentInfo);
                        }

                        // 觸發 onWornTick（每 tick 都要觸發）
                        if (!currentStack.isEmpty() && currentStack.getItem() instanceof IBauble) {
                            ((IBauble) currentStack.getItem()).onWornTick(currentStack, player);
                        }
                    }
                }

                // 防作弊：檢查是否有飾品盒
                if (!hasAccessoryBox(player)) {
                    for (int i = 14; i <= 21; i++) {
                        if (i < baublesHandler.getSlots()) {
                            ItemStack stack = baublesHandler.getStackInSlot(i);
                            if (!stack.isEmpty()) {
                                // 觸發卸下事件
                                if (stack.getItem() instanceof IBauble) {
                                    ((IBauble) stack.getItem()).onUnequipped(stack, player);
                                }
                                handleAttributeModifierBauble(player, stack, i, false);

                                // 將物品放回背包或掉落
                                if (player.inventory.addItemStackToInventory(stack)) {
                                    baublesHandler.setStackInSlot(i, ItemStack.EMPTY);
                                } else {
                                    player.dropItem(stack, false);
                                    baublesHandler.setStackInSlot(i, ItemStack.EMPTY);
                                }

                                playerEquipped.remove(i);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void handleUnequip(EntityPlayer player, ItemInfo itemInfo, int slot) {
        // 嘗試從當前槽位獲取物品來觸發卸下事件
        // 如果槽位已經空了，就創建一個臨時的 ItemStack
        IBaublesItemHandler baublesHandler = BaublesApi.getBaublesHandler(player);
        if (baublesHandler != null && slot < baublesHandler.getSlots()) {
            ItemStack currentStack = baublesHandler.getStackInSlot(slot);

            // 如果槽位已經是空的或不同物品，創建臨時 stack
            if (currentStack.isEmpty() || currentStack.getItem() != itemInfo.item) {
                ItemStack tempStack = new ItemStack(itemInfo.item, 1, itemInfo.metadata);
                if (tempStack.getItem() instanceof IBauble) {
                    ((IBauble) tempStack.getItem()).onUnequipped(tempStack, player);
                }
                handleAttributeModifierBauble(player, tempStack, slot, false);
            }
        }
    }

    /**
     * 處理 AttributeModifierBauble 類型的飾品
     */
    private static void handleAttributeModifierBauble(EntityPlayer player, ItemStack stack, int slot, boolean equip) {
        if (stack == null || stack.isEmpty()) return;

        // 檢查是否是 AttributeModifierBauble
        String className = stack.getItem().getClass().getName();
        if (className.contains("AttributeModifierBauble")) {
            try {
                // 使用反射獲取 applyModifiers 方法
                Class<?> itemClass = stack.getItem().getClass();
                Method applyModifiersMethod = null;

                // 嘗試在當前類和父類中查找方法
                Class<?> currentClass = itemClass;
                while (currentClass != null && applyModifiersMethod == null) {
                    try {
                        applyModifiersMethod = currentClass.getDeclaredMethod(
                                "applyModifiers",
                                ItemStack.class,
                                EntityPlayer.class
                        );
                        break;
                    } catch (NoSuchMethodException e) {
                        currentClass = currentClass.getSuperclass();
                    }
                }

                if (applyModifiersMethod != null) {
                    applyModifiersMethod.setAccessible(true);

                    if (equip) {
                        // 裝備時，排除參數傳 null
                        applyModifiersMethod.invoke(stack.getItem(), (ItemStack)null, player);
                    } else {
                        // 卸下時，排除參數傳當前物品
                        applyModifiersMethod.invoke(stack.getItem(), stack, player);
                    }
                }
            } catch (Exception e) {
                // 靜默處理，避免刷屏
            }
        }
    }

    private static boolean hasAccessoryBox(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemAccessoryBox) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerDeath(PlayerDropsEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        IBaublesItemHandler baublesHandler = BaublesApi.getBaublesHandler(player);

        if (baublesHandler != null) {
            for (int i = 14; i <= 21; i++) {
                if (i < baublesHandler.getSlots()) {
                    ItemStack stack = baublesHandler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        // 觸發卸下事件
                        if (stack.getItem() instanceof IBauble) {
                            ((IBauble) stack.getItem()).onUnequipped(stack, player);
                        }
                        handleAttributeModifierBauble(player, stack, i, false);

                        // 掉落物品
                        EntityItem item = new EntityItem(player.world,
                                player.posX, player.posY, player.posZ,
                                stack.copy());
                        event.getDrops().add(item);
                        baublesHandler.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }

            // 清除記錄
            equippedItems.remove(player);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getEntityItem().getItem().getItem() instanceof ItemAccessoryBox) {
            event.getPlayer().sendMessage(new TextComponentString(
                    TextFormatting.RED + "Warning: Extra slots need this box to access!"
            ));
        }
    }
}