package com.moremod.network;

import com.moremod.enchantment.EnchantmentBoostHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 激活附魔增强的数据包
 */
public class PacketActivateBoost implements IMessage {

    private int boostAmount;

    public PacketActivateBoost() {}

    public PacketActivateBoost(int boostAmount) {
        this.boostAmount = boostAmount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        boostAmount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(boostAmount);
    }

    public static class Handler implements IMessageHandler<PacketActivateBoost, IMessage> {

        @Override
        public IMessage onMessage(PacketActivateBoost message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            player.getServerWorld().addScheduledTask(() -> {
                // 验证玩家是否佩戴饰品
                if (!EnchantmentBoostHelper.hasBoostBauble(player)) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "需要佩戴附魔增强饰品！"
                    ));
                    return;
                }

                // 检查是否已激活
                if (EnchantmentBoostHelper.hasActiveBoost(player)) {
                    return;
                }

                // 激活增强
                EnchantmentBoostHelper.activateBoost(player, message.boostAmount, 60);

                // 播放音效给附近玩家
                player.world.playSound(
                        null,
                        player.posX, player.posY, player.posZ,
                        net.minecraft.init.SoundEvents.ENTITY_PLAYER_LEVELUP,
                        net.minecraft.util.SoundCategory.PLAYERS,
                        1.0F, 1.0F
                );

                // 通知玩家
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✦ 附魔增强已激活！"
                ));
            });

            return null;
        }
    }
}