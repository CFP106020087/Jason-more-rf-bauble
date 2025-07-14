package com.moremod.network;

import baubles.api.BaublesApi;
import com.moremod.item.ItemJetpackBauble;
import com.moremod.item.ItemCreativeJetpackBauble;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageToggleJetpackMode implements IMessage {

    private int toggleMode; // 0=hover, 1=jetpack, 2=speed

    public MessageToggleJetpackMode() {}

    public MessageToggleJetpackMode(boolean toggleJetpack) {
        this.toggleMode = toggleJetpack ? 1 : 0;
    }

    public MessageToggleJetpackMode(int mode) {
        this.toggleMode = mode;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        toggleMode = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(toggleMode);
    }

    public static class Handler implements IMessageHandler<MessageToggleJetpackMode, IMessage> {
        @Override
        public IMessage onMessage(MessageToggleJetpackMode msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            player.getServerWorld().addScheduledTask(() -> {
                for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
                    ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);

                    // 检查是否是喷气背包类型的物品
                    if (stack.getItem() instanceof ItemJetpackBauble || stack.getItem() instanceof ItemCreativeJetpackBauble) {
                        NBTTagCompound tag = stack.getTagCompound();
                        if (tag == null) {
                            tag = new NBTTagCompound();
                            stack.setTagCompound(tag);
                        }

                        if (msg.toggleMode == 1) { // 喷气背包开关
                            boolean current = tag.getBoolean("JetpackEnabled");
                            tag.setBoolean("JetpackEnabled", !current);
                            player.sendMessage(new net.minecraft.util.text.TextComponentString("Jetpack: " + (!current ? "ON" : "OFF")));

                        } else if (msg.toggleMode == 0) { // 悬停模式
                            boolean current = tag.getBoolean("HoverEnabled");
                            tag.setBoolean("HoverEnabled", !current);
                            player.sendMessage(new net.minecraft.util.text.TextComponentString("Hover: " + (!current ? "ON" : "OFF")));

                        } else if (msg.toggleMode == 2) { // 速度模式切换
                            if (stack.getItem() instanceof ItemCreativeJetpackBauble) {
                                ItemCreativeJetpackBauble creativeJetpack = (ItemCreativeJetpackBauble) stack.getItem();
                                creativeJetpack.nextSpeedMode(stack, player);
                            }
                            // 普通喷气背包暂时不支持速度切换，如果需要可以添加
                        }

                        // 同步 NBT 到客户端
                        PacketHandler.INSTANCE.sendTo(
                                new MessageSyncJetpackTagToClient(i, stack),
                                player
                        );
                        break;
                    }
                }
            });

            return null;
        }
    }
}