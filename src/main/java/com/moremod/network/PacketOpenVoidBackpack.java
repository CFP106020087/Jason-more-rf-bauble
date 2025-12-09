package com.moremod.network;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemVoidBackpackLink;
import com.moremod.moremod;  // ⚠️ 添加这个导入
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketOpenVoidBackpack implements IMessage {

    public PacketOpenVoidBackpack() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketOpenVoidBackpack, IMessage> {
        @Override
        public IMessage onMessage(PacketOpenVoidBackpack message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            player.getServerWorld().addScheduledTask(() -> {
                System.out.println("========================================");
                System.out.println("[MoreMod] 🌌 虚空背包打开请求处理");
                System.out.println("[MoreMod] 玩家: " + player.getName());

                try {
                    // 步骤1：查找装备的虚空背包
                    ItemStack backpack = getEquippedBackpack(player);
                    if (backpack.isEmpty()) {
                        System.out.println("[MoreMod] ❌ 未找到装备的虚空背包");
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.RED + "⚠ 需要装备虚空背包链接"), true);
                        return;
                    }
                    System.out.println("[MoreMod] ✅ 找到虚空背包");

                    // 步骤2：获取容量
                    int size = ItemVoidBackpackLink.getCachedSizeStatic(backpack);
                    if (size < 9) {
                        size = 9;
                        System.out.println("[MoreMod] ⚠️ 容量小于9，使用默认值9");
                    }
                    System.out.println("[MoreMod] 容量: " + size + " 格");

                    // 步骤3：检查模组实例
                    if (moremod.instance == null) {
                        System.err.println("[MoreMod] ❌ 错误：模组实例为null！");
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.RED + "⚠ 服务器错误：模组实例未初始化"), true);
                        return;
                    }
                    System.out.println("[MoreMod] ✅ 模组实例正常");

                    // 步骤4：打开GUI
                    System.out.println("[MoreMod] 准备打开GUI...");
                    System.out.println("[MoreMod] GUI ID: " + com.moremod.client.gui.GuiHandler.VOID_BACKPACK_GUI);

                    player.openGui(
                            moremod.instance,  // ✅ 修正：使用正确的实例引用
                            com.moremod.client.gui.GuiHandler.VOID_BACKPACK_GUI,
                            player.world,
                            size,  // x = 容量
                            0,     // y = 未使用
                            0      // z = 未使用
                    );

                    System.out.println("[MoreMod] ✅ GUI打开命令已发送");
                    System.out.println("========================================");

                } catch (Exception e) {
                    System.err.println("[MoreMod] ❌ 打开虚空背包GUI时发生异常：");
                    e.printStackTrace();
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.RED + "⚠ 打开虚空背包失败: " + e.getMessage()), true);
                }
            });

            return null;
        }

        private static ItemStack getEquippedBackpack(EntityPlayer player) {
            try {
                IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
                if (baubles == null) {
                    System.out.println("[MoreMod] ❌ Baubles处理器为null");
                    return ItemStack.EMPTY;
                }

                System.out.println("[MoreMod] 开始搜索装备槽位，总槽位数: " + baubles.getSlots());
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        System.out.println("[MoreMod] 槽位 " + i + ": " + stack.getItem().getRegistryName());
                        if (stack.getItem() instanceof ItemVoidBackpackLink) {
                            System.out.println("[MoreMod] ✅ 在槽位 " + i + " 找到虚空背包");
                            return stack;
                        }
                    }
                }

                System.out.println("[MoreMod] ❌ 未在任何槽位找到虚空背包");
            } catch (Exception e) {
                System.err.println("[MoreMod] ❌ 获取装备的虚空背包时发生异常：");
                e.printStackTrace();
            }

            return ItemStack.EMPTY;
        }
    }
}