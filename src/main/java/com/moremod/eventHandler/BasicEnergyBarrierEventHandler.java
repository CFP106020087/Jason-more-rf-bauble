package com.moremod.eventHandler;

import baubles.api.BaublesApi;
import com.moremod.item.ItemBasicEnergyBarrier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class BasicEnergyBarrierEventHandler {

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemBasicEnergyBarrier) {
                ItemBasicEnergyBarrier barrier = (ItemBasicEnergyBarrier) stack.getItem();

                if (!ItemBasicEnergyBarrier.shouldBlockDamage(event.getSource())) continue;

                if (barrier.getEnergyStored(stack) >= ItemBasicEnergyBarrier.COST_PER_BLOCK) {
                    barrier.consumeEnergy(stack, ItemBasicEnergyBarrier.COST_PER_BLOCK);
                    event.setCanceled(true);

                    player.world.playSound(null, player.posX, player.posY, player.posZ,
                            net.minecraft.init.SoundEvents.BLOCK_ANVIL_LAND,
                            player.getSoundCategory(), 0.3F, 1.2F);

                    if (!player.world.isRemote) {
                        player.sendStatusMessage(
                                new net.minecraft.util.text.TextComponentString(
                                        net.minecraft.util.text.TextFormatting.LIGHT_PURPLE +
                                                "屏蔽了近战伤害，剩余能量：" +
                                                ItemBasicEnergyBarrier.getEnergyStored(stack) + " RF"),
                                true
                        );
                    }
                }
                break;
            }
        }
    }
}
