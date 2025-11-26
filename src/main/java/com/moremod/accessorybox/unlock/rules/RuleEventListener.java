package com.moremod.accessorybox.unlock.rules;

import com.moremod.accessorybox.unlock.rules.progress.ProgressTracker;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * 规则事件监听器
 * 监听玩家行为并记录到ProgressTracker
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class RuleEventListener {

    /**
     * 监听物品使用(右键)
     */
    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        if (!UnlockRulesConfig.enableRuleSystem) return;
        if (event.getWorld().isRemote) return; // 只在服务器端记录

        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = event.getItemStack();

        if (!stack.isEmpty()) {
            String itemId = getItemId(stack);
            ProgressTracker.recordItemUse(player, itemId);

            if (UnlockRulesConfig.debugMode) {
                System.out.println("[RuleEvent] " + player.getName() + " 使用: " + itemId);
            }
        }
    }

    /**
     * 监听物品拾取
     */
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!UnlockRulesConfig.enableRuleSystem) return;
        if (event.getEntityPlayer().world.isRemote) return;

        EntityPlayer player = event.getEntityPlayer();
        EntityItem entityItem = event.getItem();
        ItemStack stack = entityItem.getItem();

        if (!stack.isEmpty()) {
            String itemId = getItemId(stack);
            int count = stack.getCount();
            ProgressTracker.recordItemPickup(player, itemId, count);

            if (UnlockRulesConfig.debugMode) {
                System.out.println("[RuleEvent] " + player.getName() + " 拾取: " + count + "x " + itemId);
            }
        }
    }

    /**
     * 监听物品合成
     */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!UnlockRulesConfig.enableRuleSystem) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        ItemStack stack = event.crafting;

        if (!stack.isEmpty()) {
            String itemId = getItemId(stack);
            int count = stack.getCount();
            ProgressTracker.recordItemCraft(player, itemId, count);

            if (UnlockRulesConfig.debugMode) {
                System.out.println("[RuleEvent] " + player.getName() + " 合成: " + count + "x " + itemId);
            }
        }
    }

    /**
     * 监听实体死亡(击杀)
     */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (!UnlockRulesConfig.enableRuleSystem) return;
        if (event.getEntity().world.isRemote) return;

        // 检查是否被玩家击杀
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

            // 检查维度限制
            if (!UnlockRulesConfig.crossDimensionCount && player.dimension != 0) {
                return;
            }

            // 获取实体ID
            ResourceLocation entityType = net.minecraft.entity.EntityList.getKey(event.getEntity());

            if (entityType != null) {
                String entityId = entityType.toString();
                ProgressTracker.recordEntityKill(player, entityId);

                if (UnlockRulesConfig.debugMode) {
                    System.out.println("[RuleEvent] " + player.getName() + " 击杀: " + entityId);
                }
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取物品ID字符串
     * 格式: modid:itemname 或 modid:itemname:meta
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