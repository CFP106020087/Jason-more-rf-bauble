package com.moremod.eventHandler;

import com.moremod.item.ItemMechanicalCore;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 加强版机械核心掉落保护器
 * 专门处理Baubles API的掉落机制
 */
public class CoreDropProtection {

    private static final boolean BAUBLES_LOADED = Loader.isModLoaded("baubles");

    // 存储玩家死亡时的机械核心，用于复活时恢复
    private static final Map<UUID, ItemStack> savedCores = new HashMap<>();
    private static final Map<UUID, Integer> savedCoreSlots = new HashMap<>();

    /**
     * 在玩家死亡前保存机械核心
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 保存Baubles槽位中的机械核心
        if (BAUBLES_LOADED) {
            try {
                IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
                if (baubles != null) {
                    for (int i = 0; i < baubles.getSlots(); i++) {
                        ItemStack stack = baubles.getStackInSlot(i);
                        if (!stack.isEmpty() && ItemMechanicalCore.isMechanicalCore(stack)) {
                            // 保存核心和槽位
                            savedCores.put(player.getUniqueID(), stack.copy());
                            savedCoreSlots.put(player.getUniqueID(), i);

                            // 立即清空槽位，防止掉落
                            baubles.setStackInSlot(i, ItemStack.EMPTY);

                            System.out.println("[moremod] 💾 保存了玩家 " + player.getName() + " 的机械核心");
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[moremod] ❌ 保存机械核心失败: " + e.getMessage());
            }
        }

        // 同时检查普通物品栏
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ItemMechanicalCore.isMechanicalCore(stack)) {
                savedCores.put(player.getUniqueID(), stack.copy());
                savedCoreSlots.put(player.getUniqueID(), -1); // -1 表示在普通物品栏
                player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
                System.out.println("[moremod] 💾 保存了玩家 " + player.getName() + " 物品栏中的机械核心");
                break;
            }
        }
    }

    /**
     * 玩家复活时恢复机械核心
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        UUID playerId = player.getUniqueID();

        if (savedCores.containsKey(playerId)) {
            ItemStack savedCore = savedCores.get(playerId);
            Integer slotIndex = savedCoreSlots.get(playerId);

            boolean restored = false;

            // 尝试恢复到原来的槽位
            if (BAUBLES_LOADED && slotIndex != null && slotIndex >= 0) {
                try {
                    IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
                    if (baubles != null && slotIndex < baubles.getSlots()) {
                        baubles.setStackInSlot(slotIndex, savedCore);
                        restored = true;
                        System.out.println("[moremod] ✅ 恢复机械核心到Baubles槽位 " + slotIndex);
                    }
                } catch (Exception e) {
                    System.err.println("[moremod] ❌ 恢复到Baubles槽位失败: " + e.getMessage());
                }
            }

            // 如果无法恢复到原槽位，尝试放入物品栏
            if (!restored) {
                if (player.inventory.addItemStackToInventory(savedCore)) {
                    System.out.println("[moremod] ✅ 恢复机械核心到物品栏");
                } else {
                    // 如果物品栏满了，强制放入第一个槽位
                    player.inventory.setInventorySlotContents(0, savedCore);
                    System.out.println("[moremod] ⚠️ 物品栏已满，强制恢复到第一个槽位");
                }
            }

            // 清理保存的数据
            savedCores.remove(playerId);
            savedCoreSlots.remove(playerId);

            // 发送恢复消息
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_AQUA + "⚙ 机械核心已自动恢复！它永远不会离开你。"
            ));
        }
    }

    /**
     * 阻止玩家死亡时掉落机械核心
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDrops(PlayerDropsEvent event) {
        // 移除所有机械核心掉落
        event.getDrops().removeIf(entityItem -> {
            boolean isCore = ItemMechanicalCore.isMechanicalCore(entityItem.getItem());
            if (isCore) {
                System.out.println("[moremod] 🛡️ 移除了掉落的机械核心");
            }
            return isCore;
        });
    }

    /**
     * 阻止通过其他方式掉落机械核心
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            event.getDrops().removeIf(entityItem -> {
                boolean isCore = ItemMechanicalCore.isMechanicalCore(entityItem.getItem());
                if (isCore) {
                    System.out.println("[moremod] 🛡️ LivingDrops保护生效");
                }
                return isCore;
            });
        }
    }

    /**
     * 阻止玩家手动丢弃机械核心
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onItemToss(ItemTossEvent event) {
        ItemStack tossedItem = event.getEntityItem().getItem();

        if (ItemMechanicalCore.isMechanicalCore(tossedItem)) {
            event.setCanceled(true);

            if (event.getPlayer() != null) {
                event.getPlayer().sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "⚠ 机械核心无法被丢弃！它已与你的生命力绑定。"
                ));
            }

            System.out.println("[moremod] 🛡️ 阻止了机械核心手动丢弃");
        }
    }

    /**
     * 终极保护：监控世界中的机械核心掉落物并清除
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onItemSpawn(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityItem) {
            EntityItem entityItem = (EntityItem) event.getEntity();
            ItemStack stack = entityItem.getItem();

            if (ItemMechanicalCore.isMechanicalCore(stack)) {
                event.setCanceled(true);
                System.out.println("[moremod] 🛡️ 终极保护：阻止了机械核心掉落物生成");
            }
        }
    }

    /**
     * 玩家克隆事件（用于模组兼容）
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            EntityPlayer oldPlayer = event.getOriginal();
            EntityPlayer newPlayer = event.getEntityPlayer();

            // 检查旧玩家的Baubles槽位
            if (BAUBLES_LOADED) {
                try {
                    IBaublesItemHandler oldBaubles = BaublesApi.getBaublesHandler(oldPlayer);
                    IBaublesItemHandler newBaubles = BaublesApi.getBaublesHandler(newPlayer);

                    if (oldBaubles != null && newBaubles != null) {
                        for (int i = 0; i < oldBaubles.getSlots(); i++) {
                            ItemStack stack = oldBaubles.getStackInSlot(i);
                            if (!stack.isEmpty() && ItemMechanicalCore.isMechanicalCore(stack)) {
                                newBaubles.setStackInSlot(i, stack.copy());
                                System.out.println("[moremod] 📋 通过Clone事件恢复了机械核心");
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[moremod] ❌ Clone事件处理失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 清理离线玩家的保存数据
     */
    @SubscribeEvent
    public void onPlayerLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        // 如果玩家退出游戏时还有保存的核心，清理掉避免内存泄漏
        UUID playerId = event.player.getUniqueID();
        if (savedCores.containsKey(playerId)) {
            savedCores.remove(playerId);
            savedCoreSlots.remove(playerId);
            System.out.println("[moremod] 🧹 清理了离线玩家的保存数据");
        }
    }

    /**
     * 调试：打印掉落事件信息
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void debugDrops(PlayerDropsEvent event) {
        if (event.getDrops().size() > 0) {
            System.out.println("[moremod] 🔍 玩家掉落物调试:");
            for (EntityItem item : event.getDrops()) {
                ItemStack stack = item.getItem();
                System.out.println("  - " + stack.getDisplayName() + " x" + stack.getCount());

                if (ItemMechanicalCore.isMechanicalCore(stack)) {
                    System.err.println("  ❌ 警告：发现机械核心在掉落列表中！");
                }
            }
        }
    }
}