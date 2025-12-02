package com.moremod.event.eventHandler;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.util.BaublesCompatibility;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 简化的机械核心事件处理器
 * 只包含核心功能，避免版本兼容性问题
 */
public class SimpleCoreHandler {

    private static final int CHECK_INTERVAL = 10;
    private int tickCounter = 0;

    /**
     * 防止机械核心死亡掉落
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDrops(PlayerDropsEvent event) {
        EntityPlayer player = event.getEntityPlayer();

        boolean removedCore = event.getDrops().removeIf(entityItem ->
                ItemMechanicalCore.isMechanicalCore(entityItem.getItem())
        );

        if (removedCore && !player.world.isRemote) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_AQUA + "⚙ 机械核心受到诅咒保护，永远不会丢失！"
            ));
        }
    }

    /**
     * 额外保护：防止通过其他方式掉落机械核心
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            event.getDrops().removeIf(entityItem ->
                    ItemMechanicalCore.isMechanicalCore(entityItem.getItem())
            );
        }
    }

    /**
     * 玩家Tick事件 - 应用机械核心的持续效果
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }

        if (++tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        EntityPlayer player = event.player;
        ItemStack coreStack = getEquippedCore(player);

        if (ItemMechanicalCore.isMechanicalCore(coreStack)) {
            applyCoreEffects(player, coreStack);
        }
    }

    /**
     * 应用机械核心的效果
     */
    private void applyCoreEffects(EntityPlayer player, ItemStack coreStack) {
        com.moremod.upgrades.UpgradeEffectManager.applyAllEffects(player, coreStack);
    }

    /**
     * 获取玩家装备的机械核心
     */
    private ItemStack getEquippedCore(EntityPlayer player) {
        return BaublesCompatibility.findBaubleOfType(player, ItemMechanicalCore.class);
    }
}