package com.moremod.causal;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class GatebandHooks {
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent e){
        EntityLivingBase mob = e.getEntityLiving();
        if (mob.world.isRemote) return;
        if ((mob.ticksExisted & 3) != 0) return; // 每 4 tick
        CombinedSuppressor.tick(mob);
    }
}
