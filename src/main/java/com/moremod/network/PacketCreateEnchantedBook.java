package com.moremod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class PacketCreateEnchantedBook implements IMessage {

    private List<Integer> enchantmentIds;

    public PacketCreateEnchantedBook() {}

    public PacketCreateEnchantedBook(List<Integer> enchantmentIds) {
        this.enchantmentIds = enchantmentIds;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        enchantmentIds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            enchantmentIds.add(buf.readInt());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(enchantmentIds.size());
        for (int id : enchantmentIds) {
            buf.writeInt(id);
        }
    }

    public static class Handler implements IMessageHandler<PacketCreateEnchantedBook, IMessage> {
        @Override
        public IMessage onMessage(PacketCreateEnchantedBook message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // 创建附魔书
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);

                for (int id : message.enchantmentIds) {
                    Enchantment ench = Enchantment.getEnchantmentByID(id);
                    if (ench != null) {
                        ItemEnchantedBook.addEnchantment(book, new EnchantmentData(ench, 5));
                    }
                }

                // 给予玩家
                if (!player.inventory.addItemStackToInventory(book)) {
                    player.dropItem(book, false);
                }
            });
            return null;
        }
    }
}