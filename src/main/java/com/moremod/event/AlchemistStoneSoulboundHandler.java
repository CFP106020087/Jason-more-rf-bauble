package com.moremod.event;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemAlchemistStone;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

/**
 * 炼药师术石专用死亡保护处理器
 *
 * 功能：
 * - 死亡时保护术石不掉落
 * - 复活时归还到原来的饰品栏位
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class AlchemistStoneSoulboundHandler {

    // PlayerPersisted 键
    private static final String PERSISTED = "PlayerPersisted";
    private static final String K_STONE_NBT = "moremod_AlchemistStoneNbt";
    private static final String K_STONE_SLOT = "moremod_AlchemistStoneSlot";

    /**
     * 死亡时：保存术石并从饰品栏/背包移除（防止掉落）
     * 参考 CoreDropProtection 的实现方式
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        NBTTagCompound ed = player.getEntityData();
        if (!ed.hasKey(PERSISTED, 10)) {
            ed.setTag(PERSISTED, new NBTTagCompound());
        }
        NBTTagCompound persisted = ed.getCompoundTag(PERSISTED);

        // 清理旧数据
        persisted.removeTag(K_STONE_NBT);
        persisted.removeTag(K_STONE_SLOT);

        // 1. 优先从饰品栏查找并保存
        try {
            IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof ItemAlchemistStone) {
                        // 保存术石 NBT
                        NBTTagCompound stoneNbt = new NBTTagCompound();
                        stack.writeToNBT(stoneNbt);
                        persisted.setTag(K_STONE_NBT, stoneNbt);
                        persisted.setInteger(K_STONE_SLOT, i);

                        // 直接从饰品栏移除（防止掉落）
                        handler.setStackInSlot(i, ItemStack.EMPTY);

                        System.out.println("[moremod] AlchemistStone: 从饰品栏保存了炼药师术石 (slot " + i + ") for " + player.getName());
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 2. 从背包查找并保存
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemAlchemistStone) {
                // 保存术石 NBT
                NBTTagCompound stoneNbt = new NBTTagCompound();
                stack.writeToNBT(stoneNbt);
                persisted.setTag(K_STONE_NBT, stoneNbt);
                persisted.setInteger(K_STONE_SLOT, -1);  // -1 表示来自背包

                // 直接从背包移除（防止掉落）
                player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);

                System.out.println("[moremod] AlchemistStone: 从背包保存了炼药师术石 for " + player.getName());
                return;
            }
        }
    }

    /**
     * 掉落时：移除任何漏网的术石（作为备用）
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDrops(PlayerDropsEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        // 移除掉落列表中的术石（以防万一）
        event.getDrops().removeIf(ei -> {
            ItemStack stack = ei.getItem();
            if (!stack.isEmpty() && stack.getItem() instanceof ItemAlchemistStone) {
                // 如果还没保存，现在保存
                NBTTagCompound ed = player.getEntityData();
                if (!ed.hasKey(PERSISTED, 10)) {
                    ed.setTag(PERSISTED, new NBTTagCompound());
                }
                NBTTagCompound persisted = ed.getCompoundTag(PERSISTED);

                if (!persisted.hasKey(K_STONE_NBT, 10)) {
                    NBTTagCompound stoneNbt = new NBTTagCompound();
                    stack.writeToNBT(stoneNbt);
                    persisted.setTag(K_STONE_NBT, stoneNbt);
                    persisted.setInteger(K_STONE_SLOT, -1);
                    System.out.println("[moremod] AlchemistStone: 从掉落列表补救了炼药师术石 for " + player.getName());
                }

                ei.setDead();
                return true;
            }
            return false;
        });
    }

    /**
     * 复活时：从 PlayerPersisted 恢复术石到饰品栏
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        EntityPlayer newPlayer = event.getEntityPlayer();
        if (newPlayer.world.isRemote) return;

        NBTTagCompound ed = newPlayer.getEntityData();
        if (!ed.hasKey(PERSISTED, 10)) return;
        NBTTagCompound persisted = ed.getCompoundTag(PERSISTED);

        if (!persisted.hasKey(K_STONE_NBT, 10)) return;

        // 恢复术石
        ItemStack stone = new ItemStack(persisted.getCompoundTag(K_STONE_NBT));
        if (stone.isEmpty() || !(stone.getItem() instanceof ItemAlchemistStone)) return;

        int savedSlot = persisted.getInteger(K_STONE_SLOT);
        boolean placed = false;

        // 尝试放回原饰品栏位
        try {
            IBaublesItemHandler handler = BaublesApi.getBaublesHandler(newPlayer);
            if (handler != null) {
                // 优先放回原槽位
                if (savedSlot >= 0 && savedSlot < handler.getSlots() && handler.getStackInSlot(savedSlot).isEmpty()) {
                    handler.setStackInSlot(savedSlot, stone.copy());
                    placed = true;
                } else {
                    // 原槽位被占用，找其他空槽位
                    for (int i = 0; i < handler.getSlots(); i++) {
                        if (handler.getStackInSlot(i).isEmpty()) {
                            handler.setStackInSlot(i, stone.copy());
                            placed = true;
                            break;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 饰品栏满了才放背包
        if (!placed) {
            placed = newPlayer.inventory.addItemStackToInventory(stone.copy());
        }

        // 清理 NBT
        if (placed) {
            persisted.removeTag(K_STONE_NBT);
            persisted.removeTag(K_STONE_SLOT);

            newPlayer.sendMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "✦ " +
                    TextFormatting.LIGHT_PURPLE + "炼药师的术石与你的灵魂一同复生"));
        }
    }

    /**
     * 玩家退出时清理（防止内存泄漏）
     */
    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        // PlayerPersisted 会随玩家数据保存，不需要额外清理
    }
}
