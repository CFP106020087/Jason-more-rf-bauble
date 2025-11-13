package com.moremod.eventHandler;

import com.moremod.item.ItemMechanicalCore;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;

public class CoreDropProtection {

    private static final boolean BAUBLES_LOADED = Loader.isModLoaded("baubles");
    private static final String PERSISTED = "PlayerPersisted";
    private static final String K_CORE_NBT = "moremod_SavedCore";
    private static final String K_CORE_SLOT = "moremod_SavedSlot";
    private static final String K_IN_BAUBLES = "moremod_InBaubles";
    private static final String K_RESTORE_PENDING = "moremod_RestorePending";

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        NBTTagCompound entityData = player.getEntityData();
        if (!entityData.hasKey(PERSISTED, 10)) {
            entityData.setTag(PERSISTED, new NBTTagCompound());
        }
        NBTTagCompound persisted = entityData.getCompoundTag(PERSISTED);

        persisted.removeTag(K_CORE_NBT);
        persisted.removeTag(K_CORE_SLOT);
        persisted.removeTag(K_IN_BAUBLES);
        persisted.setBoolean(K_RESTORE_PENDING, false);

        boolean saved = false;
        if (BAUBLES_LOADED) saved = saveCoreFromBaubles(player, persisted);
        if (!saved) saveCoreFromInventory(player, persisted);
    }

    private boolean saveCoreFromBaubles(EntityPlayer player, NBTTagCompound persisted) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty() && ItemMechanicalCore.isMechanicalCore(stack)) {
                        NBTTagCompound coreNbt = new NBTTagCompound();
                        stack.writeToNBT(coreNbt);
                        persisted.setTag(K_CORE_NBT, coreNbt);
                        persisted.setInteger(K_CORE_SLOT, i);
                        persisted.setBoolean(K_IN_BAUBLES, true);
                        persisted.setBoolean(K_RESTORE_PENDING, true);
                        baubles.setStackInSlot(i, ItemStack.EMPTY);
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean saveCoreFromInventory(EntityPlayer player, NBTTagCompound persisted) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ItemMechanicalCore.isMechanicalCore(stack)) {
                NBTTagCompound coreNbt = new NBTTagCompound();
                stack.writeToNBT(coreNbt);
                persisted.setTag(K_CORE_NBT, coreNbt);
                persisted.setInteger(K_CORE_SLOT, i);
                persisted.setBoolean(K_IN_BAUBLES, false);
                persisted.setBoolean(K_RESTORE_PENDING, true);
                player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDrops(PlayerDropsEvent event) {
        event.getDrops().removeIf(item -> ItemMechanicalCore.isMechanicalCore(item.getItem()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            event.getDrops().removeIf(item -> ItemMechanicalCore.isMechanicalCore(item.getItem()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onItemToss(ItemTossEvent event) {
        ItemStack tossed = event.getEntityItem().getItem();
        if (ItemMechanicalCore.isMechanicalCore(tossed)) {
            event.setCanceled(true);
            if (event.getPlayer() != null && !event.getPlayer().world.isRemote) {
                event.getPlayer().sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "⚠ 机械核心与你的灵魂绑定，无法丢弃。"
                ));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onItemSpawn(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityItem) {
            EntityItem item = (EntityItem) event.getEntity();
            ItemStack stack = item.getItem();
            if (ItemMechanicalCore.isMechanicalCore(stack)) {
                if (!isAnyoneRestoring(event.getWorld().getMinecraftServer())) {
                    event.setCanceled(true);
                }
            }
        }
    }

    private boolean isAnyoneRestoring(MinecraftServer server) {
        if (server == null) return false;
        for (EntityPlayer player : server.getPlayerList().getPlayers()) {
            NBTTagCompound persisted = player.getEntityData().getCompoundTag(PERSISTED);
            if (persisted.getBoolean(K_RESTORE_PENDING)) return true;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        EntityPlayer oldPlayer = event.getOriginal();
        EntityPlayer newPlayer = event.getEntityPlayer();
        NBTTagCompound oldPersisted = oldPlayer.getEntityData().getCompoundTag(PERSISTED);
        if (oldPersisted.hasKey(K_CORE_NBT)) {
            NBTTagCompound newEntityData = newPlayer.getEntityData();
            if (!newEntityData.hasKey(PERSISTED, 10)) {
                newEntityData.setTag(PERSISTED, new NBTTagCompound());
            }
            NBTTagCompound newPersisted = newEntityData.getCompoundTag(PERSISTED);
            newPersisted.setTag(K_CORE_NBT, oldPersisted.getCompoundTag(K_CORE_NBT));
            newPersisted.setInteger(K_CORE_SLOT, oldPersisted.getInteger(K_CORE_SLOT));
            newPersisted.setBoolean(K_IN_BAUBLES, oldPersisted.getBoolean(K_IN_BAUBLES));
            newPersisted.setBoolean(K_RESTORE_PENDING, oldPersisted.getBoolean(K_RESTORE_PENDING));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;
        NBTTagCompound persisted = player.getEntityData().getCompoundTag(PERSISTED);
        if (!persisted.hasKey(K_CORE_NBT)) return;
        if (!persisted.getBoolean(K_RESTORE_PENDING)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        server.addScheduledTask(() -> restoreCore(player));
    }

    private void restoreCore(EntityPlayer player) {
        NBTTagCompound persisted = player.getEntityData().getCompoundTag(PERSISTED);
        if (!persisted.hasKey(K_CORE_NBT)) return;

        NBTTagCompound coreNbt = persisted.getCompoundTag(K_CORE_NBT);
        ItemStack core = new ItemStack(coreNbt);
        int slot = persisted.getInteger(K_CORE_SLOT);
        boolean inBaubles = persisted.getBoolean(K_IN_BAUBLES);
        persisted.setBoolean(K_RESTORE_PENDING, false);

        if (core.isEmpty()) {
            cleanupCoreData(persisted);
            return;
        }

        boolean restored = false;
        if (BAUBLES_LOADED && inBaubles && slot >= 0) {
            restored = restoreToBaubles(player, core, slot);
        }

        if (!restored && !inBaubles && slot >= 0 && slot < player.inventory.getSizeInventory()) {
            if (player.inventory.getStackInSlot(slot).isEmpty()) {
                player.inventory.setInventorySlotContents(slot, core);
                restored = true;
            }
        }

        if (!restored) {
            if (player.inventory.addItemStackToInventory(core)) {
                restored = true;
            } else {
                player.inventory.setInventorySlotContents(0, core);
                restored = true;
            }
        }

        if (restored) {
            cleanupCoreData(persisted);
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_AQUA + "⚙ 机械核心已安全恢复" +
                            (inBaubles ? "到饰品栏" : "到背包") + "。"
            ));
        }
    }

    private boolean restoreToBaubles(EntityPlayer player, ItemStack core, int slot) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null && slot < baubles.getSlots()) {
                if (baubles.getStackInSlot(slot).isEmpty()) {
                    baubles.setStackInSlot(slot, core);
                    return true;
                } else {
                    for (int i = 0; i < baubles.getSlots(); i++) {
                        if (baubles.getStackInSlot(i).isEmpty() && baubles.isItemValidForSlot(i, core, player)) {
                            baubles.setStackInSlot(i, core);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void cleanupCoreData(NBTTagCompound persisted) {
        persisted.removeTag(K_CORE_NBT);
        persisted.removeTag(K_CORE_SLOT);
        persisted.removeTag(K_IN_BAUBLES);
        persisted.removeTag(K_RESTORE_PENDING);
    }

    @SubscribeEvent
    public void onPlayerLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        // PlayerPersisted 自动保存，无需操作
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void debugDrops(PlayerDropsEvent event) {}
}
