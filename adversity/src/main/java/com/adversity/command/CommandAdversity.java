package com.adversity.command;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IPlayerDifficulty;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 逆境难度调整指令 (OP/控制台专用)
 *
 * 用法:
 *   /adversity                                    - 玩家查看自己的难度 (无需权限)
 *   /adversity <玩家名> set <0.0-2.0>            - 设置指定玩家的难度倍率
 *   /adversity <玩家名> preset <预设>            - 为指定玩家使用预设
 *   /adversity <玩家名> disable                  - 为指定玩家禁用难度
 *   /adversity <玩家名> enable                   - 为指定玩家启用难度
 *   /adversity <玩家名> reset                    - 重置指定玩家的难度
 *
 * FTB Quests 可用:
 *   /adversity @p preset hard
 *   /adversity PlayerName preset nightmare
 */
public class CommandAdversity extends CommandBase {

    private static final List<String> SUBCOMMANDS = Arrays.asList("set", "preset", "disable", "enable", "reset");
    private static final List<String> PRESETS = Arrays.asList("peaceful", "easy", "normal", "hard", "nightmare");

    @Override
    public String getName() {
        return "adversity";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        if (sender instanceof EntityPlayer) {
            return "/adversity - View your difficulty\n/adversity <player> <set|preset|disable|enable|reset> [value] (OP only)";
        }
        return "/adversity <player> <set|preset|disable|enable|reset> [value]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        // 基础权限为 0，但修改操作需要 OP
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        // 允许所有人使用（查看功能），修改功能在 execute 中检查权限
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        // 无参数 - 玩家查看自己的难度状态
        if (args.length == 0) {
            if (!(sender instanceof EntityPlayer)) {
                throw new CommandException("Usage: /adversity <player> <set|preset|disable|enable|reset> [value]");
            }
            EntityPlayer player = (EntityPlayer) sender;
            IPlayerDifficulty cap = CapabilityHandler.getPlayerDifficulty(player);
            if (cap == null) {
                throw new CommandException("Failed to get difficulty data");
            }
            showStatus(player, player, cap);
            return;
        }

        // 有参数 - 需要 OP 权限
        if (!hasPermission(server, sender)) {
            throw new CommandException("You don't have permission to modify difficulty settings");
        }

        // 解析目标玩家
        if (args.length < 2) {
            throw new CommandException("Usage: /adversity <player> <set|preset|disable|enable|reset> [value]");
        }

        EntityPlayerMP targetPlayer = getPlayer(server, sender, args[0]);
        IPlayerDifficulty cap = CapabilityHandler.getPlayerDifficulty(targetPlayer);

        if (cap == null) {
            throw new CommandException("Failed to get difficulty data for " + targetPlayer.getName());
        }

        String subcommand = args[1].toLowerCase();

        switch (subcommand) {
            case "set":
                if (args.length < 3) {
                    throw new CommandException("Usage: /adversity <player> set <0.0-2.0>");
                }
                float multiplier = (float) parseDouble(args[2], 0.0, 2.0);
                cap.setDifficultyMultiplier(multiplier);
                notifySuccess(sender, targetPlayer, "Difficulty multiplier set to " + formatMultiplier(multiplier));
                break;

            case "preset":
                if (args.length < 3) {
                    throw new CommandException("Usage: /adversity <player> preset <peaceful|easy|normal|hard|nightmare>");
                }
                applyPreset(sender, targetPlayer, cap, args[2].toLowerCase());
                break;

            case "disable":
                cap.setDifficultyDisabled(true);
                notifySuccess(sender, targetPlayer, "Adversity system " + TextFormatting.RED + "DISABLED");
                break;

            case "enable":
                cap.setDifficultyDisabled(false);
                notifySuccess(sender, targetPlayer, "Adversity system " + TextFormatting.GREEN + "ENABLED");
                break;

            case "reset":
                cap.setDifficultyMultiplier(1.0f);
                cap.setDifficultyDisabled(false);
                cap.resetKillCount();
                notifySuccess(sender, targetPlayer, "Difficulty reset to default (Normal)");
                break;

            default:
                throw new CommandException("Unknown subcommand: " + subcommand + ". Use: set, preset, disable, enable, reset");
        }
    }

    /**
     * 检查是否有修改权限 (OP level 2 或控制台)
     */
    private boolean hasPermission(MinecraftServer server, ICommandSender sender) {
        if (!(sender instanceof EntityPlayer)) {
            return true; // 控制台始终有权限
        }
        return server.getPlayerList().canSendCommands(((EntityPlayer) sender).getGameProfile());
    }

    private void showStatus(ICommandSender sender, EntityPlayer target, IPlayerDifficulty cap) {
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== " + target.getName() + "'s Adversity Difficulty ==="));

        String status = cap.isDifficultyDisabled()
            ? TextFormatting.RED + "DISABLED"
            : TextFormatting.GREEN + "ENABLED";
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Status: " + status));

        String multiplierStr = formatMultiplier(cap.getDifficultyMultiplier());
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Multiplier: " + TextFormatting.WHITE + multiplierStr));

        String presetName = getPresetName(cap.getDifficultyMultiplier());
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Preset: " + TextFormatting.WHITE + presetName));

        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Kill Count: " + TextFormatting.WHITE + cap.getKillCount()));
    }

    private void applyPreset(ICommandSender sender, EntityPlayer target, IPlayerDifficulty cap, String preset) throws CommandException {
        float multiplier;
        String displayName;

        switch (preset) {
            case "peaceful":
                multiplier = 0.0f;
                displayName = TextFormatting.AQUA + "Peaceful";
                cap.setDifficultyDisabled(true);
                break;
            case "easy":
                multiplier = 0.5f;
                displayName = TextFormatting.GREEN + "Easy";
                cap.setDifficultyDisabled(false);
                break;
            case "normal":
                multiplier = 1.0f;
                displayName = TextFormatting.YELLOW + "Normal";
                cap.setDifficultyDisabled(false);
                break;
            case "hard":
                multiplier = 1.5f;
                displayName = TextFormatting.RED + "Hard";
                cap.setDifficultyDisabled(false);
                break;
            case "nightmare":
                multiplier = 2.0f;
                displayName = TextFormatting.DARK_RED + "Nightmare";
                cap.setDifficultyDisabled(false);
                break;
            default:
                throw new CommandException("Unknown preset: " + preset + ". Available: peaceful, easy, normal, hard, nightmare");
        }

        cap.setDifficultyMultiplier(multiplier);
        notifySuccess(sender, target, "Difficulty set to " + displayName);
    }

    private String formatMultiplier(float multiplier) {
        if (multiplier == 0.0f) return "0.0x (Peaceful)";
        if (multiplier == 0.5f) return "0.5x (Easy)";
        if (multiplier == 1.0f) return "1.0x (Normal)";
        if (multiplier == 1.5f) return "1.5x (Hard)";
        if (multiplier == 2.0f) return "2.0x (Nightmare)";
        return String.format("%.1fx", multiplier);
    }

    private String getPresetName(float multiplier) {
        if (multiplier == 0.0f) return "Peaceful";
        if (multiplier <= 0.5f) return "Easy";
        if (multiplier <= 1.0f) return "Normal";
        if (multiplier <= 1.5f) return "Hard";
        return "Nightmare";
    }

    /**
     * 通知操作成功
     */
    private void notifySuccess(ICommandSender sender, EntityPlayer target, String message) {
        // 通知执行者
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "[Adversity] " + target.getName() + ": " + message));

        // 如果执行者不是目标玩家，也通知目标玩家
        if (sender != target) {
            target.sendMessage(new TextComponentString(TextFormatting.GREEN + "[Adversity] " + message));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            // 第一个参数：玩家名
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 2) {
            // 第二个参数：子命令
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("preset")) {
            // preset 的第三个参数：预设名
            return getListOfStringsMatchingLastWord(args, PRESETS);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }
}
