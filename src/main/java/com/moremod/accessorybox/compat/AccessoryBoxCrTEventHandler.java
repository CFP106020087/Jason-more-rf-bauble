package com.moremod.accessorybox.compat;

import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.event.IEventHandle;
import crafttweaker.api.event.IEventManager;
import crafttweaker.util.EventList;
import crafttweaker.util.IEventHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

@ModOnly("crafttweaker")
@ZenRegister
@ZenExpansion("crafttweaker.events.IEventManager")
public class AccessoryBoxCrTEventHandler {

    private static final EventList<CrTAccessoryBoxEvent.PreEquip> EQUIP_PRE = new EventList<>();
    private static final EventList<CrTAccessoryBoxEvent.PostEquip> EQUIP_POST = new EventList<>();
    private static final EventList<CrTAccessoryBoxEvent.PreUnequip> UNEQUIP_PRE = new EventList<>();
    private static final EventList<CrTAccessoryBoxEvent.PostUnequip> UNEQUIP_POST = new EventList<>();
    private static final EventList<CrTAccessoryBoxEvent.WearingTick> WEARING_TICK = new EventList<>();

    @ZenMethod
    public static IEventHandle onAccessoryBoxEquipPre(IEventManager manager, IEventHandler<CrTAccessoryBoxEvent.PreEquip> h) {
        return EQUIP_PRE.add(h);
    }

    @ZenMethod
    public static IEventHandle onAccessoryBoxEquipPost(IEventManager manager, IEventHandler<CrTAccessoryBoxEvent.PostEquip> h) {
        return EQUIP_POST.add(h);
    }

    @ZenMethod
    public static IEventHandle onAccessoryBoxUnequipPre(IEventManager manager, IEventHandler<CrTAccessoryBoxEvent.PreUnequip> h) {
        return UNEQUIP_PRE.add(h);
    }

    @ZenMethod
    public static IEventHandle onAccessoryBoxUnequipPost(IEventManager manager, IEventHandler<CrTAccessoryBoxEvent.PostUnequip> h) {
        return UNEQUIP_POST.add(h);
    }

    @ZenMethod
    public static IEventHandle onAccessoryBoxWearingTick(IEventManager manager, IEventHandler<CrTAccessoryBoxEvent.WearingTick> h) {
        return WEARING_TICK.add(h);
    }

    // 供 Java 代码调用的静态方法

    public static boolean handleEquipPre(EntityPlayer player, int slot, ItemStack stack) {
        if (EQUIP_PRE.hasHandlers()) {
            CrTAccessoryBoxEvent.PreEquip event = new CrTAccessoryBoxEvent.PreEquip(player, slot, stack);
            EQUIP_PRE.publish(event);
            return event.isCanceled();
        }
        return false;
    }

    public static void handleEquipPost(EntityPlayer player, int slot, ItemStack stack) {
        if (EQUIP_POST.hasHandlers()) {
            EQUIP_POST.publish(new CrTAccessoryBoxEvent.PostEquip(player, slot, stack));
        }
    }

    public static boolean handleUnequipPre(EntityPlayer player, int slot, ItemStack stack) {
        if (UNEQUIP_PRE.hasHandlers()) {
            CrTAccessoryBoxEvent.PreUnequip event = new CrTAccessoryBoxEvent.PreUnequip(player, slot, stack);
            UNEQUIP_PRE.publish(event);
            return event.isCanceled();
        }
        return false;
    }

    public static void handleUnequipPost(EntityPlayer player, int slot, ItemStack stack) {
        if (UNEQUIP_POST.hasHandlers()) {
            UNEQUIP_POST.publish(new CrTAccessoryBoxEvent.PostUnequip(player, slot, stack));
        }
    }

    public static void handleWearingTick(EntityPlayer player, int slot, ItemStack stack) {
        if (WEARING_TICK.hasHandlers()) {
            WEARING_TICK.publish(new CrTAccessoryBoxEvent.WearingTick(player, slot, stack));
        }
    }
}