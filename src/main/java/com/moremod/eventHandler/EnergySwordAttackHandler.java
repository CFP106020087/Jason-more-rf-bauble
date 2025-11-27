package com.moremod.eventHandler;

import com.moremod.item.ItemEnergySword;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class EnergySwordAttackHandler {

    private static long lastAttackTime = 0;
    private static boolean wasEnergySwordAttack = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntityPlayer() != null) {
            ItemStack weapon = event.getEntityPlayer().getHeldItemMainhand();
            if (weapon.getItem() instanceof ItemEnergySword) {
                IEnergyStorage storage = weapon.getCapability(CapabilityEnergy.ENERGY, null);
                if (storage != null && storage.getEnergyStored() > 0) {
                    NBTTagCompound nbt = weapon.getTagCompound();
                    if (nbt != null && nbt.getBoolean("CanUnsheathe")) {
                        lastAttackTime = System.currentTimeMillis();
                        wasEnergySwordAttack = true;
                    }
                }
            }
        }
    }

    public static boolean isEnergySwordAttackActive() {
        if (System.currentTimeMillis() - lastAttackTime > 100) {
            wasEnergySwordAttack = false;
            return false;
        }
        return wasEnergySwordAttack;
    }
}