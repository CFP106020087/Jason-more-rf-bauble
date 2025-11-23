package com.moremod.fabric.handler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 确保布料NBT数据同步到客户端
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class FabricNBTSyncHandler {

    /**
     * 玩家登录时同步
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            syncArmorToClient((EntityPlayerMP) event.player);
        }
    }

    /**
     * 装备变化时同步
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END &&
                !event.player.world.isRemote &&
                event.player.ticksExisted % 20 == 0) { // 每秒检查一次

            if (event.player instanceof EntityPlayerMP) {
                syncArmorToClient((EntityPlayerMP) event.player);
            }
        }
    }

    /**
     * 从其他维度返回时同步
     */


    /**
     * 复活时同步
\
    /**
     * 强制同步盔甲NBT到客户端
     */
    private static void syncArmorToClient(EntityPlayerMP player) {
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (slot.getSlotType() == EntityEquipmentSlot.Type.ARMOR) {
                ItemStack armor = player.getItemStackFromSlot(slot);
                if (!armor.isEmpty() && armor.hasTagCompound()) {
                    // 强制更新物品栏以同步NBT
                    player.inventoryContainer.detectAndSendChanges();
                }
            }
        }
    }

    /**
     * 调试方法：打印客户端看到的NBT
     */
    @SideOnly(Side.CLIENT)
    public static void debugClientNBT(ItemStack stack) {
        if (stack.hasTagCompound()) {
            System.out.println("[Client NBT] " + stack.getDisplayName());
            System.out.println("[Client NBT] Tags: " + stack.getTagCompound());
            if (stack.getTagCompound().hasKey("WovenFabric")) {
                System.out.println("[Client NBT] Has WovenFabric!");
            }
        }
    }
}