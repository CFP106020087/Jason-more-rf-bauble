package com.moremod.event;

import com.moremod.util.SoulbindUtil;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.*;

/**
 * 通用灵魂束缚处理器
 * 处理所有带 "Soulbound" NBT 标签的物品的死亡保护
 *
 * 与 SoulboundEvents（仅处理升级选择器）不同，这个处理器处理所有物品
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class GenericSoulboundHandler {

    // 暂存：死亡到复活之间的灵魂束缚物品
    private static final Map<UUID, List<ItemStack>> SOULBOUND_STASH = new HashMap<>();

    /**
     * 死亡掉落阶段：拦截并暂存所有带 Soulbound 标签的物品
     */
    @SubscribeEvent(priority = EventPriority.HIGH)  // HIGH 而非 HIGHEST，让其他特殊处理先执行
    public static void onPlayerDrops(PlayerDropsEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        UUID playerId = player.getUniqueID();
        List<ItemStack> saved = SOULBOUND_STASH.computeIfAbsent(playerId, k -> new ArrayList<>());

        // 遍历掉落物，把带 Soulbound 标签的移除并存起来
        Iterator<EntityItem> it = event.getDrops().iterator();
        while (it.hasNext()) {
            EntityItem ei = it.next();
            ItemStack stack = ei.getItem();

            // 检查是否有 Soulbound 标签（使用 SoulbindUtil 的检测方法）
            if (hasSoulboundTag(stack)) {
                saved.add(stack.copy());
                it.remove();
                ei.setDead();
            }
        }

        if (!saved.isEmpty()) {
            // 调试输出
            System.out.println("[moremod] GenericSoulbound: 保护了 " + saved.size() + " 个灵魂束缚物品 for " + player.getName());
        }
    }

    /**
     * 复活时归还之前暂存的灵魂束缚物品
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        UUID playerId = player.getUniqueID();
        List<ItemStack> saved = SOULBOUND_STASH.remove(playerId);

        if (saved == null || saved.isEmpty()) return;

        int restored = 0;
        for (ItemStack stack : saved) {
            if (stack.isEmpty()) continue;

            // 尝试塞进背包，不行就掉脚边
            if (!player.inventory.addItemStackToInventory(stack.copy())) {
                player.dropItem(stack.copy(), false);
            }
            restored++;
        }

        // 提示玩家
        if (restored > 0) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "✦ " +
                    TextFormatting.LIGHT_PURPLE + "灵魂束缚物品已随你复生 (" + restored + ")"));
        }
    }

    /**
     * 检查物品是否有 Soulbound 标签
     * 支持多种标签格式以兼容不同来源
     */
    private static boolean hasSoulboundTag(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return false;
        }

        // 检查各种可能的标签格式
        if (stack.getTagCompound().getBoolean("Soulbound")) {
            return true;
        }
        if (stack.getTagCompound().getBoolean("soulbound")) {
            return true;
        }
        if (stack.getTagCompound().getBoolean("SoulBound")) {
            return true;
        }

        // 也使用 SoulbindUtil 的检测方法（它检查 NBT_SOULBOUND = "Soulbound"）
        return SoulbindUtil.isSoulbound(stack);
    }

    /**
     * 玩家退出时清理缓存（防止内存泄漏）
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.player.getUniqueID();
        SOULBOUND_STASH.remove(playerId);
    }
}
