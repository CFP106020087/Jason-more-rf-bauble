package com.moremod.network;

import com.moremod.item.ItemSageBook;
import io.netty.buffer.ByteBuf;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class PacketCreateEnchantedBook implements IMessage {

    private List<Integer> enchantmentIds;
    private boolean isMainHand;

    public PacketCreateEnchantedBook() {}

    public PacketCreateEnchantedBook(List<Integer> enchantmentIds, boolean isMainHand) {
        this.enchantmentIds = enchantmentIds;
        this.isMainHand = isMainHand;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        enchantmentIds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            enchantmentIds.add(buf.readInt());
        }
        isMainHand = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(enchantmentIds.size());
        for (int id : enchantmentIds) {
            buf.writeInt(id);
        }
        buf.writeBoolean(isMainHand);
    }

    public static class Handler implements IMessageHandler<PacketCreateEnchantedBook, IMessage> {
        @Override
        public IMessage onMessage(PacketCreateEnchantedBook message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // 在主线程执行，避免延迟
            player.getServerWorld().addScheduledTask(() -> {
                EnumHand hand = message.isMainHand ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
                ItemStack heldItem = player.getHeldItem(hand);

                // 验证玩家仍然持有贤者之书
                if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemSageBook)) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "错误：你没有持有贤者之书！"));
                    return;
                }

                // 验证附魔ID
                if (message.enchantmentIds == null || message.enchantmentIds.size() != 3) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "错误：必须选择3个附魔！"));
                    return;
                }

                // 创建附魔书
                ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
                boolean success = true;

                for (int id : message.enchantmentIds) {
                    Enchantment ench = Enchantment.getEnchantmentByID(id);
                    if (ench != null) {
                        ItemEnchantedBook.addEnchantment(enchantedBook, new EnchantmentData(ench, 10));
                    } else {
                        success = false;
                        break;
                    }
                }

                if (!success) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "错误：无效的附魔选择！"));
                    return;
                }

                // 现在才消耗贤者之书
                heldItem.shrink(1);

                // 给予附魔书
                if (!player.inventory.addItemStackToInventory(enchantedBook)) {
                    // 如果背包满了，掉落物品
                    player.dropItem(enchantedBook, false);
                }

                // 播放成功音效和消息
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.ENTITY_PLAYER_LEVELUP,
                        SoundCategory.PLAYERS, 1.0F, 1.0F);

                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✔ 成功创建附魔书！"));
            });

            return null;
        }
    }
}