package com.moremod.event.eventHandler;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.config.FleshRejectionConfig;
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

    // 排异系统NBT键
    private static final String NBT_GROUP = "rejection";
    private static final String NBT_REJECTION = "RejectionLevel";
    private static final String NBT_ADAPTATION = "AdaptationLevel";
    private static final String NBT_TRANSCENDED = "RejectionTranscended";
    private static final String NBT_BLEEDING_TICKS = "BleedingTicks";
    private static final String NBT_LAST_STABILIZER = "LastStabilizerUse";

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

        // 清理旧数据
        persisted.removeTag(K_CORE_NBT);
        persisted.removeTag(K_CORE_SLOT);
        persisted.removeTag(K_IN_BAUBLES);
        persisted.setBoolean(K_RESTORE_PENDING, false);

        // 尝试保存核心
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

                        // 处理死亡排异衰减
                        processDeathRejection(player, stack);

                        // 保存核心NBT
                        NBTTagCompound coreNbt = new NBTTagCompound();
                        stack.writeToNBT(coreNbt);
                        persisted.setTag(K_CORE_NBT, coreNbt);
                        persisted.setInteger(K_CORE_SLOT, i);
                        persisted.setBoolean(K_IN_BAUBLES, true);
                        persisted.setBoolean(K_RESTORE_PENDING, true);

                        // 移除核心
                        baubles.setStackInSlot(i, ItemStack.EMPTY);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean saveCoreFromInventory(EntityPlayer player, NBTTagCompound persisted) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ItemMechanicalCore.isMechanicalCore(stack)) {

                // 处理死亡排异衰减
                processDeathRejection(player, stack);

                // 保存核心NBT
                NBTTagCompound coreNbt = new NBTTagCompound();
                stack.writeToNBT(coreNbt);
                persisted.setTag(K_CORE_NBT, coreNbt);
                persisted.setInteger(K_CORE_SLOT, i);
                persisted.setBoolean(K_IN_BAUBLES, false);
                persisted.setBoolean(K_RESTORE_PENDING, true);

                // 移除核心
                player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    /**
     * 处理死亡时的排异值衰减
     */
    private void processDeathRejection(EntityPlayer player, ItemStack core) {
        if (!FleshRejectionConfig.enableRejectionSystem) return;
        if (core.isEmpty()) return;

        NBTTagCompound rejData = core.getOrCreateSubCompound(NBT_GROUP);

        float oldRejection = rejData.getFloat(NBT_REJECTION);
        float adaptation = rejData.getFloat(NBT_ADAPTATION);
        boolean wasTranscended = rejData.getBoolean(NBT_TRANSCENDED);

        // 突破状态处理
        if (FleshRejectionConfig.keepTranscendenceOnDeath) {
            if (wasTranscended) {
                rejData.setFloat(NBT_REJECTION, 0f);
            }
        } else {
            rejData.setBoolean(NBT_TRANSCENDED, false);
        }

        // 计算新的排异值
        if (!wasTranscended || !FleshRejectionConfig.keepTranscendenceOnDeath) {
            float newRejection = (float) (oldRejection * FleshRejectionConfig.deathRejectionRetention);
            newRejection = Math.max(newRejection, (float) FleshRejectionConfig.minRejectionAfterDeath);
            rejData.setFloat(NBT_REJECTION, newRejection);

            // 记录衰减信息
            if (oldRejection > FleshRejectionConfig.stabilizerMinRejection &&
                    Math.abs(oldRejection - newRejection) > 0.01f) {
                rejData.setFloat("PreDeathRejection", oldRejection);
                rejData.setBoolean("DeathProcessed", true);
            }
        }

        // 适应度保留
        rejData.setFloat(NBT_ADAPTATION, adaptation);

        // 清空临时状态
        rejData.setInteger(NBT_BLEEDING_TICKS, 0);
        rejData.setLong(NBT_LAST_STABILIZER, 0);

        if (FleshRejectionConfig.debugMode && !player.world.isRemote) {
            player.sendMessage(new TextComponentString(
                    String.format("§7[死亡处理] 排异 %.1f → %.1f, 适应 %.1f",
                            oldRejection, rejData.getFloat(NBT_REJECTION), adaptation)
            ));
        }
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

            // 复制保存的核心数据
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

        // 尝试恢复到原位置
        if (BAUBLES_LOADED && inBaubles && slot >= 0) {
            restored = restoreToBaubles(player, core, slot);
        }

        if (!restored && !inBaubles && slot >= 0 && slot < player.inventory.getSizeInventory()) {
            if (player.inventory.getStackInSlot(slot).isEmpty()) {
                player.inventory.setInventorySlotContents(slot, core);
                restored = true;
            }
        }

        // 恢复到任意可用位置
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

            // 显示死亡补偿信息
            NBTTagCompound rejData = core.getOrCreateSubCompound(NBT_GROUP);
            if (rejData.getBoolean("DeathProcessed")) {
                float preDeathRej = rejData.getFloat("PreDeathRejection");
                float currentRej = rejData.getFloat(NBT_REJECTION);

                if (preDeathRej > FleshRejectionConfig.stabilizerMinRejection) {
                    int lostPercent = (int)((preDeathRej - currentRej) / preDeathRej * 100);
                    player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "☠ 死亡使排异值降低了 " + lostPercent + "%"
                    ));
                }

                // 清理临时标记
                rejData.removeTag("PreDeathRejection");
                rejData.removeTag("DeathProcessed");
            }

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
                    // 尝试其他槽位
                    for (int i = 0; i < baubles.getSlots(); i++) {
                        if (baubles.getStackInSlot(i).isEmpty() &&
                                baubles.isItemValidForSlot(i, core, player)) {
                            baubles.setStackInSlot(i, core);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void cleanupCoreData(NBTTagCompound persisted) {
        persisted.removeTag(K_CORE_NBT);
        persisted.removeTag(K_CORE_SLOT);
        persisted.removeTag(K_IN_BAUBLES);
        persisted.removeTag(K_RESTORE_PENDING);
    }
}