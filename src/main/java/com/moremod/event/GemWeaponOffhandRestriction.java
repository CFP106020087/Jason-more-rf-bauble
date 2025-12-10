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

        // 尝试放入背包
        ItemStack toMove = offhand.copy();
        inventory.offHandInventory.set(0, ItemStack.EMPTY);

        if (!inventory.addItemStackToInventory(toMove)) {
            // 背包满了，掉落到地上
            player.dropItem(toMove, false);
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
     * 清理玩家数据（玩家退出时调用）
     */
    public static void cleanupPlayer(UUID playerId) {
        messageCooldown.remove(playerId);
    }
}
