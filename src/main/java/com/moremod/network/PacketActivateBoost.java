package com.moremod.network;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.EnchantmentBoostBauble;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 激活附魔增强的数据包 - 完全修复版
 * ✅ 直接调用饰品的激活方法，而不是单独调用Helper
 */
public class PacketActivateBoost implements IMessage {

    public PacketActivateBoost() {}

    @Override
    public void fromBytes(ByteBuf buf) {
        // 不需要参数
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 不需要参数
    }

    public static class Handler implements IMessageHandler<PacketActivateBoost, IMessage> {

        @Override
        public IMessage onMessage(PacketActivateBoost message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            player.getServerWorld().addScheduledTask(() -> {
                try {
                    // ✅ 1. 查找玩家佩戴的附魔增强饰品
                    IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
                    if (baubles == null) {
                        player.sendMessage(new TextComponentString(
                                TextFormatting.RED + "无法访问饰品栏！"
                        ));
                        return;
                    }

                    EnchantmentBoostBauble boostBauble = null;
                    ItemStack baubleStack = ItemStack.EMPTY;

                    // 遍历所有饰品槽位
                    for (int i = 0; i < baubles.getSlots(); i++) {
                        ItemStack stack = baubles.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof EnchantmentBoostBauble) {
                            boostBauble = (EnchantmentBoostBauble) stack.getItem();
                            baubleStack = stack;
                            break;
                        }
                    }

                    // ✅ 2. 检查是否找到饰品
                    if (boostBauble == null || baubleStack.isEmpty()) {
                        player.sendMessage(new TextComponentString(
                                TextFormatting.RED + "需要佩戴附魔增强饰品！"
                        ));
                        System.out.println("[moremod] 激活失败：玩家 " + player.getName() + " 未佩戴饰品");
                        return;
                    }

                    // ✅ 3. 直接调用饰品的激活方法（包含所有检查和冷却逻辑）
                    boolean success = boostBauble.tryActivateBoost(player, baubleStack);

                    if (success) {
                        System.out.println("[moremod] 按键激活成功：玩家 " + player.getName() +
                                ", 增幅值 " + boostBauble.getRawBoostAmount());
                    } else {
                        System.out.println("[moremod] 按键激活失败：玩家 " + player.getName());
                    }

                } catch (Exception e) {
                    System.err.println("[moremod] 激活附魔增强时出错: " + e.getMessage());
                    e.printStackTrace();
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "激活失败：内部错误"
                    ));
                }
            });

            return null;
        }
    }
}