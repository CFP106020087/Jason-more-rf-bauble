package com.moremod.eventHandler;

import baubles.api.BaublesApi;
import com.moremod.item.ItemEnergyBarrier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class EnergyBarrierEventHandler {

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 遍历饰品栏
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemEnergyBarrier) {
                ItemEnergyBarrier barrier = (ItemEnergyBarrier) stack.getItem();
                if (barrier.getEnergyStored(stack) >= ItemEnergyBarrier.COST_PER_BLOCK) {
                    barrier.consumeEnergy(stack, ItemEnergyBarrier.COST_PER_BLOCK);
                    event.setCanceled(true); // 取消伤害
                    player.world.playSound(null, player.posX, player.posY, player.posZ,
                            net.minecraft.init.SoundEvents.BLOCK_ANVIL_LAND,
                            player.getSoundCategory(), 0.3F, 1.4F); // 播放防护音效
                }
                break; // 只处理一件
            }
        }
    }
}