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
 * 灵魂绑定物品的"真正灵魂绑定"事件集：
 * - 死亡不掉落：在 PlayerDropsEvent 把物品移出掉落列表并暂存
 * - 复活归还：在 PlayerRespawnEvent 还给新玩家（背包满则掉在脚边）
 * - 禁止丢弃/禁止他人捡起
 *
 * ✅ 保护范围：所有带 Soulbound NBT 标记的物品（包括机械核心、升级选择器等）
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class SoulboundEvents {

    // 暂存：死亡到复活之间的灵魂绑定物品
    private static final Map<UUID, List<ItemStack>> STASH = new HashMap<>();

    /** 死亡掉落阶段：拦截并暂存所有灵魂绑定物品 */
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
            // ✅ 保护所有带 Soulbound 标记的物品（包括机械核心和升级选择器）
            if (isSoulboundItem(s, player)) {
                saved.add(s.copy());
                it.remove();
            }
        }

        // 调试信息
        if (!saved.isEmpty()) {
            System.out.println("[moremod] Soulbound保护 " + player.getName() + " 的 " + saved.size() + " 个物品");
        }
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
                            "⚙ 已恢复你的灵魂绑定物品 (" + saved.size() + " 个)"), true);
        }
    }

    /** Q 键/背包丢弃：禁止主人丢出 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        EntityPlayer player = event.getPlayer();
        ItemStack s = event.getEntityItem().getItem();

        // ✅ 保护所有灵魂绑定物品
        if (isSoulboundItem(s, player) && !player.isCreative()) {
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

        // ✅ 灵魂绑定物品只能被主人捡起
        if (hasSoulboundTag(s) && !isItemOwner(s, player) && !player.isCreative()) {
            event.setCanceled(true);
        }
    }

    // ===== 帮助方法 =====

    /**
     * ✅ 检查物品是否是灵魂绑定物品（通用方法）
     * 支持：机械核心、升级选择器等所有带 Soulbound NBT 的物品
     */
    private static boolean isSoulboundItem(ItemStack s, EntityPlayer player) {
        if (s == null || s.isEmpty()) return false;

        // 检查是否有 Soulbound 标记
        if (!hasSoulboundTag(s)) return false;

        // 检查是否属于该玩家
        return isItemOwner(s, player);
    }

    /**
     * ✅ 检查物品是否有 Soulbound NBT 标记
     */
    private static boolean hasSoulboundTag(ItemStack s) {
        if (s == null || s.isEmpty() || !s.hasTagCompound()) return false;
        return s.getTagCompound().getBoolean("Soulbound");
    }

    /**
     * ✅ 检查物品是否属于该玩家（通用方法）
     */
    private static boolean isItemOwner(ItemStack s, EntityPlayer player) {
        if (s == null || s.isEmpty() || !s.hasTagCompound()) return false;

        net.minecraft.nbt.NBTTagCompound nbt = s.getTagCompound();

        // 优先检查 OwnerUUID
        if (nbt.hasKey("OwnerUUID")) {
            String ownerUUID = nbt.getString("OwnerUUID");
            return player.getUniqueID().toString().equals(ownerUUID);
        }

        // 降级检查 OriginalOwner（名字可能会变，但作为兜底）
        if (nbt.hasKey("OriginalOwner")) {
            String ownerName = nbt.getString("OriginalOwner");
            return player.getName().equals(ownerName);
        }

        // 特殊处理：升级选择器的所有权检查
        if (s.getItem() instanceof ItemUpgradeSelector) {
            return ItemUpgradeSelector.isOwner(s, player);
        }

        // 如果没有所有者标记，则允许（兼容旧物品）
        return true;
    }

    // ===== 保留旧方法用于兼容 =====

    @Deprecated
    private static boolean isBoundSelector(ItemStack s) {
        return s != null && !s.isEmpty()
                && s.getItem() instanceof ItemUpgradeSelector
                && ItemUpgradeSelector.isSoulbound(s);
    }

    @Deprecated
    private static boolean isOwner(ItemStack s, EntityPlayer p) {
        return isItemOwner(s, p);
    }

    @Deprecated
    private static boolean isBoundSelectorOwnedBy(ItemStack s, EntityPlayer p) {
        return isSoulboundItem(s, p);
    }
}
