package com.moremod.core.network;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.core.api.IMechanicalCoreData;
import com.moremod.core.capability.MechanicalCoreCapability;
import com.moremod.core.registry.UpgradeRegistry;
import com.moremod.item.ItemMechanicalCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 网络包：暂停/恢复升级
 *
 * 客户端 -> 服务端
 */
public class PacketCorePauseResume implements IMessage {

    private String upgradeId;
    private boolean pause; // true = 暂停, false = 恢复

    // 无参构造函数（Forge需要）
    public PacketCorePauseResume() {}

    public PacketCorePauseResume(String upgradeId, boolean pause) {
        this.upgradeId = upgradeId;
        this.pause = pause;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        upgradeId = ByteBufUtils.readUTF8String(buf);
        pause = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, upgradeId);
        buf.writeBoolean(pause);
    }

    public static class Handler implements IMessageHandler<PacketCorePauseResume, IMessage> {
        @Override
        public IMessage onMessage(PacketCorePauseResume message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // 必须在主线程执行
            player.getServerWorld().addScheduledTask(() -> {
                handlePacket(message, player);
            });

            return null;
        }

        private void handlePacket(PacketCorePauseResume message, EntityPlayerMP player) {
            // 查找装备的核心
            ItemStack core = findEquippedCore(player);
            if (core.isEmpty()) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "未装备机械核心"));
                return;
            }

            // 获取Capability
            IMechanicalCoreData data = core.getCapability(
                    MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);
            if (data == null) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "核心数据错误"));
                return;
            }

            // 规范化升级ID
            String canonId = UpgradeRegistry.canonicalIdOf(message.upgradeId);

            // 检查升级是否已安装
            if (!data.isInstalled(canonId)) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "升级未安装"));
                return;
            }

            String displayName = UpgradeRegistry.getDisplayName(canonId);

            // 执行暂停或恢复
            if (message.pause) {
                // 暂停
                if (data.isPaused(canonId)) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + displayName + " 已经处于暂停状态"));
                    return;
                }

                data.pause(canonId);
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "已暂停 " + displayName));
            } else {
                // 恢复
                if (!data.isPaused(canonId)) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + displayName + " 未处于暂停状态"));
                    return;
                }

                data.resume(canonId);
                int restoredLevel = data.getLevel(canonId);
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "已恢复 " + displayName +
                        " (等级 " + restoredLevel + ")"));
            }

            // 同步到客户端
            if (core.hasTagCompound()) {
                core.getTagCompound().setTag("CoreData", data.serializeNBT());
            }
        }

        /**
         * 查找装备的机械核心
         */
        private ItemStack findEquippedCore(EntityPlayerMP player) {
            try {
                IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
                if (baubles != null) {
                    for (int i = 0; i < baubles.getSlots(); i++) {
                        ItemStack stack = baubles.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof ItemMechanicalCore) {
                            return stack;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ItemStack.EMPTY;
        }
    }
}
