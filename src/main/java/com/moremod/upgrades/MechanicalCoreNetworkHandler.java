package com.moremod.upgrades;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.network.MessageJetpackJumping;
import com.moremod.network.MessageJetpackSneaking;
import com.moremod.network.MessageToggleJetpackMode;
import com.moremod.network.PacketHandler;
import com.moremod.eventHandler.EventHandlerJetpack;
import com.moremod.client.JetpackKeyHandler;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

/**
 * 机械核心网络处理器（仅按键同步/模式切换）
 * - 不拦截双击空格、不修改/授予/回收 allowFlying/isFlying
 * - 与喷气背包共用按键状态与数据包
 */
@Mod.EventBusSubscriber
public class MechanicalCoreNetworkHandler {

    /**
     * 处理按键输入事件：模块开关 / 悬停 / 速度模式
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onKeyInputForCore(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;

        ItemStack core = findMechanicalCoreWithFlight(mc.player);
        if (core.isEmpty()) return;

        // ✅ Set player context for upgrade reads (client-side)
        ItemMechanicalCore.setPlayerContext(mc.player);
        int flightLevel;
        try {
            flightLevel = ItemMechanicalCore.getUpgradeLevel(core, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE);
        } finally {
            ItemMechanicalCore.clearPlayerContext();
        }

        // V：机械核心飞行开关
        if (JetpackKeyHandler.keyToggleJetpack.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new MessageToggleJetpackMode(3)); // 3 = 机械核心飞行开关
        }

        // H：悬停模式
        if (JetpackKeyHandler.keyToggleHover.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new MessageToggleJetpackMode(4)); // 4 = 机械核心悬停模式
        }

        // G：速度模式（仅3级飞行）
        if (flightLevel >= 3 && JetpackKeyHandler.keyToggleSpeedMode.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new MessageToggleJetpackMode(5)); // 5 = 机械核心速度模式
        }
    }

    /**
     * 客户端Tick：同步跳跃/潜行状态给服务器（仅当玩家装备了带飞行模块的机械核心）
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onClientTickForCore(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;

        ItemStack core = findMechanicalCoreWithFlight(mc.player);
        if (core.isEmpty()) return;

        UUID playerId = mc.player.getUniqueID();

        boolean isJumping  = mc.gameSettings.keyBindJump.isKeyDown();
        boolean isSneaking = mc.gameSettings.keyBindSneak.isKeyDown();

        Boolean lastJump  = EventHandlerJetpack.jetpackJumping.get(playerId);
        Boolean lastSneak = EventHandlerJetpack.jetpackSneaking.get(playerId);

        boolean jumpChanged  = lastJump == null  || lastJump  != isJumping;
        boolean sneakChanged = lastSneak == null || lastSneak != isSneaking;

        if (jumpChanged || sneakChanged) {
            // 更新本地共享状态（喷气背包/机械核心共用）
            EventHandlerJetpack.jetpackJumping.put(playerId, isJumping);
            EventHandlerJetpack.jetpackSneaking.put(playerId, isSneaking);

            // 发送到服务端
            if (jumpChanged) {
                PacketHandler.INSTANCE.sendToServer(new MessageJetpackJumping(isJumping));
            }
            if (sneakChanged) {
                PacketHandler.INSTANCE.sendToServer(new MessageJetpackSneaking(isSneaking));
            }
        }

        // 注意：不再拦截双击空格进入创造飞行，也不修改玩家飞行能力位
    }

    /**
     * 查找装备的机械核心（带飞行模块）
     */
    private static ItemStack findMechanicalCoreWithFlight(EntityPlayer player) {
        IBaublesItemHandler h = BaublesApi.getBaublesHandler(player);
        if (h == null) return ItemStack.EMPTY;

        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack stack = h.getStackInSlot(i);
            if (ItemMechanicalCore.isMechanicalCore(stack)) {
                // ✅ Set player context for upgrade reads
                ItemMechanicalCore.setPlayerContext(player);
                int level;
                try {
                    level = ItemMechanicalCore.getUpgradeLevel(stack, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE);
                } finally {
                    ItemMechanicalCore.clearPlayerContext();
                }
                if (level > 0) return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
