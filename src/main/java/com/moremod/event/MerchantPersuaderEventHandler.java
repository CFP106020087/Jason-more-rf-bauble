package com.moremod.event;

import com.moremod.item.MerchantPersuader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.Event;

@Mod.EventBusSubscriber(modid = "moremod")
public class MerchantPersuaderEventHandler {

    // Raids mod 的村庄英雄效果 Registry Name
    private static final String HERO_OF_THE_VILLAGE_ID = "raids:hero_of_the_village";

    /**
     * 阻止持有村民说服器的玩家获得村庄英雄效果
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPotionApply(PotionEvent.PotionApplicableEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        PotionEffect effect = event.getPotionEffect();

        if (effect == null) {
            return;
        }

        // 检查是否是村庄英雄效果
        if (isHeroOfTheVillage(effect.getPotion())) {
            // 检查玩家是否持有村民说服器
            if (!MerchantPersuader.getActivePersuader(player).isEmpty()) {
                // 阻止效果添加
                event.setResult(Event.Result.DENY);

                // 提示玩家
                if (!player.world.isRemote) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.YELLOW + "⚠ 商人说服器与村庄英雄效果冲突，无法获得该效果"
                    ), true);
                }
            }
        }
    }

    /**
     * 当玩家装备村民说服器时，清除已有的村庄英雄效果
     * 通过 tick 检查实现
     */
    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START) {
            return;
        }

        EntityPlayer player = event.player;

        // 服务端处理
        if (player.world.isRemote) {
            return;
        }

        // 每秒检查一次 (20 ticks)
        if (player.ticksExisted % 20 != 0) {
            return;
        }

        // 检查玩家是否持有村民说服器
        if (!MerchantPersuader.getActivePersuader(player).isEmpty()) {
            // 尝试移除村庄英雄效果
            removeHeroOfTheVillage(player);
        }
    }

    /**
     * 检查药水是否是村庄英雄效果
     */
    private static boolean isHeroOfTheVillage(Potion potion) {
        if (potion == null) {
            return false;
        }

        // 通过 Registry Name 检查
        if (potion.getRegistryName() != null) {
            String registryName = potion.getRegistryName().toString();
            if (HERO_OF_THE_VILLAGE_ID.equals(registryName)) {
                return true;
            }
        }

        // 备用检查：通过名称
        String potionName = potion.getName();
        return potionName != null && potionName.toLowerCase().contains("hero_of_the_village");
    }

    /**
     * 移除玩家的村庄英雄效果
     */
    private static void removeHeroOfTheVillage(EntityPlayer player) {
        // 遍历所有活跃效果
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (isHeroOfTheVillage(effect.getPotion())) {
                player.removePotionEffect(effect.getPotion());

                // 提示玩家
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.YELLOW + "⚠ 村庄英雄效果与商人说服器冲突，效果已移除"
                ), true);

                break; // 一次只移除一个，避免并发修改
            }
        }
    }
}