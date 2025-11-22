package com.moremod.capability.module;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.impl.ThornsModule;
import com.moremod.item.ItemMechanicalCore;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 模块事件处理器
 *
 * 处理需要监听 Forge 事件的模块逻辑
 */
@Mod.EventBusSubscriber
public class ModuleEventHandler {

    /**
     * 处理玩家受伤事件 - 反伤系统
     *
     * 优先级：NORMAL
     * 在伤害计算后、护盾耗尽检测前触发
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerHurt(LivingHurtEvent event) {
        // 只处理玩家受伤
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        // 获取机械核心
        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return;

        // 获取 Capability 数据
        IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);
        if (data == null) return;

        // 检查攻击者
        if (event.getSource().getTrueSource() instanceof EntityLivingBase) {
            EntityLivingBase attacker = (EntityLivingBase) event.getSource().getTrueSource();

            // 应用反伤效果
            ThornsModule.INSTANCE.applyThorns(player, data, attacker, event.getAmount());
        }
    }
}
