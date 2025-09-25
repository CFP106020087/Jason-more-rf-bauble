package com.moremod.eventHandler;

import baubles.api.BaublesApi;
import com.moremod.item.ItemCleansingBauble;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "moremod")
public class CleansingBaubleEventHandler {

    private static final int FLIGHT_DURATION = 40; // 持续时间 2 秒（每 tick 执行，建议短）

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemCleansingBauble) {
                ItemCleansingBauble bauble = (ItemCleansingBauble) stack.getItem();

                if (bauble.getEnergyStored(stack) >= ItemCleansingBauble.ENERGY_COST_PER_TICK) {
                    // 移除所有负面效果
                    List<PotionEffect> toRemove = new ArrayList<>();
                    for (PotionEffect effect : player.getActivePotionEffects()) {
                        if (effect.getPotion().isBadEffect()) {
                            toRemove.add(effect);
                        }
                    }
                    for (PotionEffect effect : toRemove) {
                        player.removePotionEffect(effect.getPotion());
                    }

                    // 添加 potioncore:flight 效果（持续短时间）
                    Potion flight = Potion.getPotionFromResourceLocation("potioncore:flight");
                    if (flight != null) {
                        player.addPotionEffect(new PotionEffect(flight, FLIGHT_DURATION, 0, true, false));
                    }

                    // 扣除能量
                    bauble.consumeEnergy(stack, ItemCleansingBauble.ENERGY_COST_PER_TICK);
                }

                break; // 只处理一个净化饰品
            }
        }
    }
}
