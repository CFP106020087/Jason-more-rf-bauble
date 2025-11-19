package com.moremod.system;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class RejectionPotionBlockSystem {
    
    /**
     * 阻止獲得正面藥水效果（而非移除已有的）
     */
    @SubscribeEvent
    public static void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        
        if (player.world.isRemote) return;
        
        PotionEffect effect = event.getPotionEffect();
        if (effect == null || effect.getPotion().isBadEffect()) return;
        
        // 已突破的玩家免疫此限制
        if (FleshRejectionSystem.hasTranscended(player)) return;
        
        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        
        // 排異值影響獲得正面效果的機率
        if (rejection >= 40) {
            // 40排異 = 10% 失敗率
            // 60排異 = 40% 失敗率  
            // 80排異 = 70% 失敗率
            // 100排異 = 100% 失敗率
            float blockChance = Math.min(1.0f, (rejection - 40) * 0.0167f);
            
            if (player.world.rand.nextFloat() < blockChance) {
                event.setResult(Event.Result.DENY);
                
                // 根據排異程度顯示不同訊息
                String message;
                if (rejection >= 80) {
                    message = TextFormatting.DARK_RED + "⚠ 血肉完全排斥藥劑！";
                } else if (rejection >= 60) {
                    message = TextFormatting.RED + "✖ 身體抗拒藥水效果";
                } else {
                    message = TextFormatting.GOLD + "⚡ 藥水效果被削弱";
                }
                
                player.sendStatusMessage(new TextComponentString(message), true);
                
                // 粒子效果提示失敗
                player.world.playSound(null, player.getPosition(),
                    net.minecraft.init.SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    net.minecraft.util.SoundCategory.PLAYERS, 0.5f, 2.0f);
            }
        }
    }
    
    /**
     * 特殊情況：允許某些關鍵藥水通過（如牛奶解毒）
     */
    @SubscribeEvent
    public static void onPotionRemove(PotionEvent.PotionRemoveEvent event) {
        // 允許移除負面效果，即使排異值很高
        if (event.getPotionEffect() != null && 
            event.getPotionEffect().getPotion().isBadEffect()) {
            // 不阻止負面效果的移除
        }
    }
}