// CommandResetEquipTime.java - 完整版本
package com.moremod.commands;

import com.moremod.config.EquipmentTimeConfig;
import com.moremod.event.EquipmentTimeTracker;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 管理员命令：重置玩家的机械核心佩戴时间限制
 * 用法：
 *   /moremod resetequiptime <玩家名> - 重置指定玩家
 *   /moremod resetequiptime all - 重置所有玩家
 *   /moremod resetequiptime check <玩家名> - 查看玩家状态
 */
public class CommandResetEquipTime extends CommandBase {

    @Override
    public String getName() {
        return "resetequiptime";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/moremod resetequiptime <player|all|check> [player]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP权限
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        // 检查功能是否启用
        if (!EquipmentTimeConfig.restriction.enabled) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.RED + "佩戴时间限制功能未启用！"
            ));
            sender.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "请在配置文件中启用 equipment_time_restriction"
            ));
            return;
        }

        // 检查参数
        if (args.length < 1) {
            throw new WrongUsageException(getUsage(sender));
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "all":
                handleResetAll(server, sender);
                break;

            case "check":
                if (args.length < 2) {
                    throw new WrongUsageException("/moremod resetequiptime check <player>");
                }
                handleCheck(server, sender, args[1]);
                break;

            default:
                // 默认为重置单个玩家
                handleResetPlayer(server, sender, args[0]);
                break;
        }
    }

    /**
     * 重置单个玩家
     */
    private void handleResetPlayer(MinecraftServer server, ICommandSender sender, String playerName)
            throws CommandException {

        // 检查是否允许管理员重置
        if (!EquipmentTimeConfig.restriction.allowAdminReset) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.RED + "管理员重置功能已在配置中禁用！"
            ));
            return;
        }

        // 获取目标玩家
        EntityPlayer targetPlayer = getPlayer(server, sender, playerName);

        // 重置限制
        boolean success = EquipmentTimeTracker.resetPlayerRestriction(targetPlayer);

        if (success) {
            // 通知发送者
            sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 已成功重置 " +
                            TextFormatting.YELLOW + targetPlayer.getName() +
                            TextFormatting.GREEN + " 的机械核心佩戴限制！"
            ));

            // 通知目标玩家（如果在线）
            if (targetPlayer instanceof EntityPlayerMP) {
                targetPlayer.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 你的机械核心佩戴限制已被管理员重置！"
                ));
                targetPlayer.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "你现在有 " +
                                formatTime(EquipmentTimeConfig.restriction.timeLimit) +
                                " 的时间来佩戴机械核心。"
                ));

                // 强制同步数据
                if (targetPlayer instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) targetPlayer).connection.sendPacket(
                            new net.minecraft.network.play.server.SPacketEntityProperties(
                                    targetPlayer.getEntityId(),
                                    targetPlayer.getAttributeMap().getAllAttributes()
                            )
                    );
                }
            }

            // 记录到日志
        System.out.println("[MechanicalCore] Admin " + sender.getName() +
                " reset equipment time for player " + targetPlayer.getName());
        } else {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 重置失败！请检查配置设置。"
            ));
        }
    }

    /**
     * 重置所有玩家
     */
    private void handleResetAll(MinecraftServer server, ICommandSender sender) {
        // 检查是否允许管理员重置
        if (!EquipmentTimeConfig.restriction.allowAdminReset) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.RED + "管理员重置功能已在配置中禁用！"
            ));
            return;
        }

        int count = 0;
        List<String> resetPlayers = new ArrayList<>();

        // 重置所有在线玩家
        for (EntityPlayer player : server.getPlayerList().getPlayers()) {
            if (EquipmentTimeTracker.resetPlayerRestriction(player)) {
                count++;
                resetPlayers.add(player.getName());

                // 通知玩家
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 你的机械核心佩戴限制已被管理员重置！"
                ));
            }
        }

        // 显示结果
        sender.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ 成功重置 " + count + " 名玩家的佩戴限制"
        ));

        if (!resetPlayers.isEmpty()) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "重置的玩家: " + String.join(", ", resetPlayers)
            ));
        }

        // 记录到日志
        System.out.println("[MechanicalCore] Admin " + sender.getName() +
                " reset equipment time for " + count + " players");
    }

    /**
     * 检查玩家状态
     */
    private void handleCheck(MinecraftServer server, ICommandSender sender, String playerName)
            throws CommandException {

        EntityPlayer targetPlayer = getPlayer(server, sender, playerName);

        sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "========== 玩家状态 =========="
        ));
        sender.sendMessage(new TextComponentString(
                TextFormatting.WHITE + "玩家: " + TextFormatting.YELLOW + targetPlayer.getName()
        ));

        // 检查是否被永久禁止
        if (EquipmentTimeTracker.isPermanentlyBanned(targetPlayer)) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.RED + "状态: 已被永久禁止佩戴"
            ));
        }
        // 检查是否已在时间内佩戴
        else if (EquipmentTimeTracker.hasEquippedInTime(targetPlayer)) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "状态: 已成功佩戴，无限制"
            ));
        }
        // 显示剩余时间
        else {
            long remaining = EquipmentTimeTracker.getRemainingTime(targetPlayer);
            if (remaining > 0) {
                sender.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "状态: 限制中"
                ));
                sender.sendMessage(new TextComponentString(
                        TextFormatting.WHITE + "剩余时间: " +
                                TextFormatting.AQUA + formatTime((int)remaining)
                ));
            } else if (remaining == 0) {
                sender.sendMessage(new TextComponentString(
                        TextFormatting.RED + "状态: 已超时（待禁止）"
                ));
            } else {
                sender.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "状态: 未知"
                ));
            }
        }

        // 检查当前装备
        boolean hasMechanicalCore = EquipmentTimeTracker.checkHasMechanicalCore(targetPlayer);
        boolean hasEnigmaticItems = EquipmentTimeTracker.checkHasEnigmaticItems(targetPlayer);

        sender.sendMessage(new TextComponentString(
                TextFormatting.WHITE + "已装备机械核心: " +
                        (hasMechanicalCore ? TextFormatting.GREEN + "是" : TextFormatting.GRAY + "否")
        ));
        sender.sendMessage(new TextComponentString(
                TextFormatting.WHITE + "已装备Enigmatic物品: " +
                        (hasEnigmaticItems ? TextFormatting.YELLOW + "是" : TextFormatting.GRAY + "否")
        ));

        sender.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "=============================="
        ));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            // 合并 "all"、"check" 和在线玩家名为一个 String[]
            String[] online = server.getOnlinePlayerNames();
            String[] options = new String[online.length + 2];
            options[0] = "all";
            options[1] = "check";
            System.arraycopy(online, 0, options, 2, online.length);
            return getListOfStringsMatchingLastWord(args, options);

        } else if (args.length == 2 && "check".equalsIgnoreCase(args[0])) {
            // 这里本来就是单独一个 String[]，可直接传
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }


    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        if (index == 0 && !"all".equals(args[0]) && !"check".equals(args[0])) {
            return true;
        }
        return index == 1 && "check".equals(args[0]);
    }

    /**
     * 格式化时间显示
     */
    private String formatTime(int seconds) {
        if (seconds >= 3600) {
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
        } else if (seconds >= 60) {
            int minutes = seconds / 60;
            int secs = seconds % 60;
            return minutes + "分钟" + (secs > 0 ? secs + "秒" : "");
        } else {
            return seconds + "秒";
        }
    }
}