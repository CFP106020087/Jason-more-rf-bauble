package com.moremod.accessorybox.compat;

import baubles.api.BaubleType;
import com.moremod.accessorybox.SlotLayoutHelper;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.entity.IEntityLivingBase;
import crafttweaker.api.event.ILivingEvent;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

public class CrTAccessoryBoxEvent {

    @ZenRegister
    @ZenClass("mods.accessorybox.events.AccessoryBoxEquipPreEvent")
    public static class PreEquip implements ILivingEvent {
        private final EntityPlayer player;
        private final ItemStack stack;
        private final int slot;
        private boolean canceled = false;

        public PreEquip(EntityPlayer player, int slot, ItemStack stack) {
            this.player = player;
            this.slot = slot;
            this.stack = stack;
        }

        @Override
        public IEntityLivingBase getEntityLivingBase() {
            return CraftTweakerMC.getIEntityLivingBase(player);
        }

        @ZenGetter("entity")
        public IEntityLivingBase getEntity() {
            return CraftTweakerMC.getIEntityLivingBase(player);
        }

        @ZenGetter("item")
        public IItemStack getItem() {
            return CraftTweakerMC.getIItemStack(stack);
        }

        @ZenGetter("slot")
        public int getSlot() {
            return slot;
        }

        /**
         * 获取槽位类型名称（AMULET, RING, BELT等）
         */
        @ZenGetter("slotType")
        public String getSlotType() {
            BaubleType type = SlotLayoutHelper.getExpectedTypeForSlot(slot);
            return type != null ? type.name() : "UNKNOWN";
        }

        /**
         * 检查是否为额外槽位
         */
        @ZenGetter("isExtraSlot")
        public boolean isExtraSlot() {
            return SlotLayoutHelper.isExtraSlot(slot);
        }

        @ZenMethod
        public void cancel() {
            this.canceled = true;
        }

        @ZenGetter("canceled")
        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    @ZenRegister
    @ZenClass("mods.accessorybox.events.AccessoryBoxEquipPostEvent")
    public static class PostEquip implements ILivingEvent {
        private final EntityPlayer player;
        private final ItemStack stack;
        private final int slot;

        public PostEquip(EntityPlayer player, int slot, ItemStack stack) {
            this.player = player;
            this.slot = slot;
            this.stack = stack;
        }

        @Override
        public IEntityLivingBase getEntityLivingBase() {
            return CraftTweakerMC.getIEntityLivingBase(player);
        }

        @ZenGetter("entity")
        public IEntityLivingBase getEntity() {
            return CraftTweakerMC.getIEntityLivingBase(player);
        }

        @ZenGetter("item")
        public IItemStack getItem() {
            return CraftTweakerMC.getIItemStack(stack);
        }

        @ZenGetter("slot")
        public int getSlot() {
            return slot;
        }

        @ZenGetter("slotType")
        public String getSlotType() {
            BaubleType type = SlotLayoutHelper.getExpectedTypeForSlot(slot);
            return type != null ? type.name() : "UNKNOWN";
        }

        @ZenGetter("isExtraSlot")
        public boolean isExtraSlot() {
            return SlotLayoutHelper.isExtraSlot(slot);
        }
    }

    @ZenRegister
    @ZenClass("mods.accessorybox.events.AccessoryBoxUnequipPreEvent")
    public static class PreUnequip implements ILivingEvent {
        private final EntityPlayer player;
        private final ItemStack stack;
        private final int slot;
        private boolean canceled = false;

        public PreUnequip(EntityPlayer player, int slot, ItemStack stack) {
            this.player = player;
            this.slot = slot;
            this.stack = stack;
        }

        @Override
        public IEntityLivingBase getEntityLivingBase() {
            return CraftTweakerMC.getIEntityLivingBase(player);
        }

        @ZenGetter("entity")
        public IEntityLivingBase getEntity() {
            return CraftTweakerMC.getIEntityLivingBase(player);
        }

        @ZenGetter("item")
        public IItemStack getItem() {
            return CraftTweakerMC.getIItemStack(stack);
        }

        @ZenGetter("slot")
        public int getSlot() {
            return slot;
        }

        @ZenGetter("slotType")
        public String getSlotType() {
            BaubleType type = SlotLayoutHelper.getExpectedTypeForSlot(slot);
            return type != null ? type.name() : "UNKNOWN";
        }

        @ZenGetter("isExtraSlot")
        public boolean isExtraSlot() {
            return SlotLayoutHelper.isExtraSlot(slot);
        }

        @ZenMethod
        public void cancel() {
            this.canceled = true;
        }

        @ZenGetter("canceled")
        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }

    @ZenRegister
    @ZenClass("mods.accessorybox.events.AccessoryBoxUnequipPostEvent")
    public static class PostUnequip implements ILivingEvent {
        private final EntityPlayer player;
        private final ItemStack stack;
        private final int slot;

        public PostUnequip(EntityPlayer player, int slot, ItemStack stack) {
            this.player = player;
            this.slot = slot;
            this.stack = stack;
        }

        @Override
        public IEntityLivingBase getEntityLivingBase() {
            return CraftTweakerMC.getIEntityLivingBase(player);
        }

        @ZenGetter("entity")
        public IEntityLivingBase getEntity() {
            return CraftTweakerMC.getIEntityLivingBase(player);
        }

        @ZenGetter("item")
        public IItemStack getItem() {
            return CraftTweakerMC.getIItemStack(stack);
        }

        @ZenGetter("slot")
        public int getSlot() {
            return slot;
        }

        @ZenGetter("slotType")
        public String getSlotType() {
            BaubleType type = SlotLayoutHelper.getExpectedTypeForSlot(slot);
            return type != null ? type.name() : "UNKNOWN";
        }

        @ZenGetter("isExtraSlot")
        public boolean isExtraSlot() {
            return SlotLayoutHelper.isExtraSlot(slot);
        }
    }

    @ZenRegister
    @ZenClass("mods.accessorybox.events.AccessoryBoxWearingTickEvent")
    public static class WearingTick implements ILivingEvent {
        private final EntityPlayer player;
        private final ItemStack stack;
        private final int slot;

        public WearingTick(EntityPlayer player, int slot, ItemStack stack) {
            this.player = player;
            this.slot = slot;
            this.stack = stack;
        }

        @Override
        public IEntityLivingBase getEntityLivingBase() {
            return CraftTweakerMC.getIEntityLivingBase(player);
        }

        @ZenGetter("entity")
        public IEntityLivingBase getEntity() {
            return CraftTweakerMC.getIEntityLivingBase(player);
        }

        @ZenGetter("item")
        public IItemStack getItem() {
            return CraftTweakerMC.getIItemStack(stack);
        }

        @ZenGetter("slot")
        public int getSlot() {
            return slot;
        }

        @ZenGetter("slotType")
        public String getSlotType() {
            BaubleType type = SlotLayoutHelper.getExpectedTypeForSlot(slot);
            return type != null ? type.name() : "UNKNOWN";
        }

        @ZenGetter("isExtraSlot")
        public boolean isExtraSlot() {
            return SlotLayoutHelper.isExtraSlot(slot);
        }
    }
}