package com.moremod.accessorybox.unlock.rules;

import com.moremod.accessorybox.unlock.rules.progress.ProgressTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 物品消耗监听器
 * 专门监听玩家吃东西、喝药水等消耗行为
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ItemConsumeListener {

    /**
     * 监听物品消耗完成
     */
    @SubscribeEvent
    public static void onItemFinishUse(LivingEntityUseItemEvent.Finish event) {
        if (!UnlockRulesConfig.enableRuleSystem) return;
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        ItemStack stack = event.getItem();
        
        if (!stack.isEmpty()) {
            String itemId = getItemId(stack);
            ProgressTracker.recordItemConsume(player, itemId, 1);
            
            if (UnlockRulesConfig.debugMode) {
                System.out.println("[ItemConsume] " + player.getName() + " 消耗: " + itemId);
            }
        }
    }

    /**
     * 获取物品ID字符串
     */
    private static String getItemId(ItemStack stack) {
        ResourceLocation registryName = stack.getItem().getRegistryName();
        if (registryName == null) {
            return "unknown";
        }
        
        String id = registryName.toString();
        
        // 如果有meta值且不为0，加上meta
        if (stack.getHasSubtypes() && stack.getMetadata() != 0) {
            id += ":" + stack.getMetadata();
        }
        
        return id;
    }
}
