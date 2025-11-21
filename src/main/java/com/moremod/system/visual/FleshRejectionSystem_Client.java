package com.moremod.system.visual;

import com.moremod.item.ItemMechanicalCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class FleshRejectionSystem_Client {

    /**
     * 客户端直接从机械核心读取排异值
     * 不依赖 EntityData，不依赖封包，不依赖缓存
     */
    public static float getRejectionClient(EntityPlayer player) {

        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return 0;

        NBTTagCompound group = core.getSubCompound("rejection");
        if (group == null) return 0;

        return group.getFloat("RejectionLevel");
    }

    public static float getAdaptationClient(EntityPlayer player) {

        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return 0;

        NBTTagCompound group = core.getSubCompound("rejection");
        if (group == null) return 0;

        return group.getFloat("AdaptationLevel");
    }
}
