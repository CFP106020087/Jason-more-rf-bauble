package com.moremod.accessorybox.unlock.example;

import com.moremod.accessorybox.unlock.SlotUnlockAPI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 吃金苹果 → 解锁一个额外戒指槽位
 * 版本：Forge 1.12.2
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class GoldenAppleRingUnlock {

    /**
     * 监听“物品食用完成”事件：
     * 只在服务端触发，物品为金苹果时尝试解锁一个额外戒指。
     */
    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 仅服务端执行，避免双发
        if (player.world.isRemote) return;

        ItemStack used = event.getItem();
        if (used.isEmpty() || used.getItem() != Items.GOLDEN_APPLE) return;

        // 依次尝试解锁第 0,1,2,... 个额外戒指；成功即停止
        boolean unlocked = false;
        int unlockedIndex = -1;
        // 给个合理上限；如果你在配置里增加更多戒指，继续尝试也安全
        for (int i = 0; i < 16; i++) {
            if (SlotUnlockAPI.unlockExtraRing(player, i)) {
                unlocked = true;
                unlockedIndex = i;
                break;
            }
        }

        if (unlocked) {
            player.sendMessage(new TextComponentString("§a已解锁一个额外戒指槽位（索引 " + (unlockedIndex + 1) + "）！"));
        } else {
            player.sendMessage(new TextComponentString("§e没有可解锁的额外戒指槽位（可能已经全部解锁）。"));
        }
    }
}
