package com.moremod.event;

import com.moremod.compat.crafttweaker.GemSocketHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 宝石武器副手限制
 *
 * 当玩家尝试将带有镶嵌宝石的武器放入副手时，
 * 自动将其移回背包并显示提示信息。
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class GemWeaponOffhandRestriction {

    // 冷却时间，防止消息刷屏
    private static final Map<UUID, Long> messageCooldown = new HashMap<>();
    private static final long COOLDOWN_TICKS = 40; // 2秒冷却

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        ItemStack offhand = player.getHeldItemOffhand();

        // 检查副手是否有物品
        if (offhand.isEmpty()) return;

        // 检查是否有镶嵌宝石
        if (!GemSocketHelper.hasSocketedGems(offhand)) return;

        // 有宝石镶嵌，移回背包
        InventoryPlayer inventory = player.inventory;

        // 先尝试找到一个空槽位
        int targetSlot = findSuitableSlot(inventory, offhand);

        if (targetSlot >= 0) {
            // 找到合适的槽位，使用原子操作：直接交换
            // 这样可以防止物品丢失或复制
            ItemStack removed = inventory.offHandInventory.set(0, ItemStack.EMPTY);

            // 验证：确保我们移除的确实是预期的物品
            if (!removed.isEmpty() && GemSocketHelper.hasSocketedGems(removed)) {
                inventory.setInventorySlotContents(targetSlot, removed);
            } else if (!removed.isEmpty()) {
                // 如果物品不是我们预期的，放回副手
                inventory.offHandInventory.set(0, removed);
                return;
            }
        } else {
            // 背包满了，直接掉落到地上
            ItemStack removed = inventory.offHandInventory.set(0, ItemStack.EMPTY);

            // 验证并掉落
            if (!removed.isEmpty() && GemSocketHelper.hasSocketedGems(removed)) {
                player.entityDropItem(removed, 0.5F);
            } else if (!removed.isEmpty()) {
                // 不是预期的物品，放回副手
                inventory.offHandInventory.set(0, removed);
                return;
            }
        }

        // 显示提示信息（带冷却）
        UUID playerId = player.getUniqueID();
        long currentTime = player.world.getTotalWorldTime();
        Long lastMessage = messageCooldown.get(playerId);

        if (lastMessage == null || currentTime - lastMessage >= COOLDOWN_TICKS) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "宝石的重量太重，你无法用左手拿起"
            ), true);
            messageCooldown.put(playerId, currentTime);
        }
    }

    /**
     * 查找合适的背包槽位
     * @return 槽位索引，-1 表示没有找到
     */
    private static int findSuitableSlot(InventoryPlayer inventory, ItemStack stack) {
        // 优先查找空槽位（主背包 0-35，不包括盔甲和副手）
        for (int i = 0; i < 36; i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                return i;
            }
        }

        // 如果没有空槽位，查找可堆叠的位置（虽然武器通常不堆叠）
        if (stack.isStackable()) {
            for (int i = 0; i < 36; i++) {
                ItemStack slotStack = inventory.getStackInSlot(i);
                if (!slotStack.isEmpty() &&
                        slotStack.isItemEqual(stack) &&
                        ItemStack.areItemStackTagsEqual(slotStack, stack) &&
                        slotStack.getCount() < slotStack.getMaxStackSize()) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * 清理玩家数据（玩家退出时调用）
     */
    public static void cleanupPlayer(UUID playerId) {
        messageCooldown.remove(playerId);
    }
}
