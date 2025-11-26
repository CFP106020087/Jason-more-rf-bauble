package com.moremod.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

public final class SoulbindUtil {
    private SoulbindUtil() {}

    public static final String NBT_SOULBOUND = "Soulbound";
    public static final String NBT_OWNER_UUID = "OwnerUUID";
    public static final String NBT_ORIGINAL_OWNER = "OriginalOwner";

    private static NBTTagCompound tag(ItemStack s) {
        if (!s.hasTagCompound()) s.setTagCompound(new NBTTagCompound());
        return s.getTagCompound();
    }

    public static void bindToOwner(ItemStack s, EntityPlayer p) {
        NBTTagCompound nbt = tag(s);
        nbt.setBoolean(NBT_SOULBOUND, true);
        nbt.setString(NBT_OWNER_UUID, p.getUniqueID().toString());
        nbt.setString(NBT_ORIGINAL_OWNER, p.getName());
    }

    public static boolean isSoulbound(ItemStack s) {
        return s != null && !s.isEmpty() && s.hasTagCompound() && s.getTagCompound().getBoolean(NBT_SOULBOUND);
    }

    public static boolean isOwner(ItemStack s, EntityPlayer p) {
        if (s == null || s.isEmpty() || !s.hasTagCompound()) return false;
        String u = s.getTagCompound().getString(NBT_OWNER_UUID);
        if (u == null || u.isEmpty()) return false; // 没写 OwnerUUID 就视为未绑定
        try {
            return UUID.fromString(u).equals(p.getUniqueID());
        } catch (Exception e) {
            return false;
        }
    }
}
