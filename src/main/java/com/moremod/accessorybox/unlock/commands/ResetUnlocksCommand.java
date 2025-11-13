package com.moremod.accessorybox.unlock.commands;

import com.moremod.accessorybox.unlock.SlotUnlockManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

/**
 * 重置玩家的槽位解锁状态
 * 用法：/resetunlocks
 */
public class ResetUnlocksCommand extends CommandBase {

    @Override
    public String getName() {
        return "resetunlocks";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/resetunlocks - 重置所有槽位解锁状态";
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

        sender.sendMessage(new TextComponentString("§e正在重置槽位解锁状态..."));

        SlotUnlockManager.getInstance().resetPlayer(player);

        sender.sendMessage(new TextComponentString("§a✓ 已重置所有槽位解锁状态"));
        sender.sendMessage(new TextComponentString("§7规则系统将在下次检查时重新评估"));
    }
}
