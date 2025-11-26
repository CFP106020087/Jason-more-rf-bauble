package com.moremod.event;

import com.moremod.item.ItemUpgradeSelector;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

/**
 * 升级选择器的“真正灵魂绑定”事件集：
 * - 死亡不掉落：在 PlayerDropsEvent 把物品移出掉落列表并暂存
 * - 复活归还：在 PlayerRespawnEvent 还给新玩家（背包满则掉在脚边）
 * - 禁止丢弃/禁止他人捡起
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class SoulboundEvents {

    // 暂存：死亡到复活之间的灵魂绑定选择器
    private static final Map<UUID, List<ItemStack>> STASH = new HashMap<>();

    /** 死亡掉落阶段：拦截并暂存灵魂绑定的升级选择器 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDrops(PlayerDropsEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        UUID id = player.getUniqueID();

        List<ItemStack> saved = STASH.computeIfAbsent(id, k -> new ArrayList<>());

        // 遍历掉落物，把需要保护的移除并存起来
        Iterator<EntityItem> it = event.getDrops().iterator();
        while (it.hasNext()) {
            EntityItem ei = it.next();
            ItemStack s = ei.getItem();
            if (isBoundSelectorOwnedBy(s, player)) {
                saved.add(s.copy());
                it.remove();
            }
        }

        // （可选）调试
        // System.out.println("[moremod] Soulbound stash for " + player.getName() + ": " + saved.size());
    }

    /** 复活时归还之前暂存的物品 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        UUID id = player.getUniqueID();

        List<ItemStack> saved = STASH.remove(id);
        if (saved == null || saved.isEmpty()) return;

        for (ItemStack s : saved) {
            // 尝试塞进背包，不行就掉脚边
            if (!player.inventory.addItemStackToInventory(s.copy())) {
                player.dropItem(s.copy(), false);
            }
        }

        // 提示一下
        if (!player.world.isRemote) {
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.DARK_AQUA +
                            "⚙ 已恢复你的灵魂绑定升级选择器 (" + saved.size() + ")"), true);
        }
    }

    /** Q 键/背包丢弃：禁止主人丢出 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        EntityPlayer player = event.getPlayer();
        ItemStack s = event.getEntityItem().getItem();

        if (isBoundSelectorOwnedBy(s, player) && !player.isCreative()) {
            event.setCanceled(true);
            if (!player.world.isRemote) {
                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.YELLOW + "灵魂绑定物品不可丢弃"), true);
            }
        }
    }

    /** 防止他人捡起（理论上不会掉地上，但兜底一下） */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPickup(EntityItemPickupEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack s = event.getItem().getItem();

        // 是灵魂绑定选择器，但不是该玩家的 -> 禁止捡起（创造例外）
        if (isBoundSelector(s) && !isOwner(s, player) && !player.isCreative()) {
            event.setCanceled(true);
        }
    }

    // ===== 帮助方法 =====

    private static boolean isBoundSelector(ItemStack s) {
        return s != null && !s.isEmpty()
                && s.getItem() instanceof ItemUpgradeSelector
                && ItemUpgradeSelector.isSoulbound(s);
    }

    private static boolean isOwner(ItemStack s, EntityPlayer p) {
        return ItemUpgradeSelector.isOwner(s, p);
    }

    private static boolean isBoundSelectorOwnedBy(ItemStack s, EntityPlayer p) {
        return isBoundSelector(s) && isOwner(s, p);
    }
}
