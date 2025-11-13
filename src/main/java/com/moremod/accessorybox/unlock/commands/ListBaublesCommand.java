package com.moremod.accessorybox.unlock.commands;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

/**
 * 列出當前佩戴的所有飾品及其註冊名
 * 用法：/listbaubles
 */
public class ListBaublesCommand extends CommandBase {

    @Override
    public String getName() {
        return "listbaubles";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/listbaubles - 列出當前佩戴的所有飾品";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;  // 所有玩家都可以使用
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString("§c只有玩家可以使用此命令"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);

        if (handler == null) {
            sender.sendMessage(new TextComponentString("§c無法獲取飾品數據"));
            return;
        }

        sender.sendMessage(new TextComponentString("§e========== 當前佩戴的飾品 =========="));

        boolean foundAny = false;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                String registryName = stack.getItem().getRegistryName().toString();
                int meta = stack.getMetadata();
                String display = stack.getDisplayName();

                sender.sendMessage(new TextComponentString(
                    String.format("§7槽位 %d: §f%s", i, display)
                ));
                sender.sendMessage(new TextComponentString(
                    String.format("  §7註冊名: §a%s", registryName)
                ));
                if (meta > 0) {
                    sender.sendMessage(new TextComponentString(
                        String.format("  §7Meta值: §e%d", meta)
                    ));
                }

                foundAny = true;
            }
        }

        if (!foundAny) {
            sender.sendMessage(new TextComponentString("§7沒有佩戴任何飾品"));
        }

        sender.sendMessage(new TextComponentString("§e====================================="));
    }
}
