package com.moremod.accessorybox.unlock.commands;

import com.moremod.accessorybox.unlock.SlotUnlockManager;
import com.moremod.accessorybox.unlock.rules.RuleChecker;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

/**
 * 調試命令：查看槽位解鎖狀態
 * 用法：/unlockdebug
 */
public class UnlockDebugCommand extends CommandBase {

    @Override
    public String getName() {
        return "unlockdebug";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/unlockdebug - 查看當前槽位解鎖狀態";
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

        sender.sendMessage(new TextComponentString("§e========== 槽位解鎖調試信息 =========="));

        // 顯示是否在客戶端
        sender.sendMessage(new TextComponentString("§7當前環境: " +
            (player.world.isRemote ? "§c客戶端" : "§a服務器端")));

        // 強制執行一次檢查
        if (!player.world.isRemote) {
            sender.sendMessage(new TextComponentString("§7正在執行規則檢查..."));
            RuleChecker.forceCheck(player);
        }

        // 打印詳細狀態
        SlotUnlockManager.getInstance().debugPrint(player.getUniqueID());

        sender.sendMessage(new TextComponentString("§e========================================"));
        sender.sendMessage(new TextComponentString("§7調試信息已輸出到控制台/日誌"));
    }
}
