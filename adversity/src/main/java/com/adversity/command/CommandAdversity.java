package com.adversity.command;

import com.adversity.Adversity;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IPlayerDifficulty;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 逆境难度调整指令
 *
 * 用法:
 *   /adversity                     - 显示当前难度设置
 *   /adversity set <multiplier>    - 设置难度倍率 (0.0-2.0)
 *   /adversity preset <name>       - 使用预设难度
 *   /adversity disable             - 禁用难度系统
 *   /adversity enable              - 启用难度系统
 *   /adversity reset               - 重置为默认
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
        return "/adversity [set <0.0-2.0> | preset <peaceful|easy|normal|hard|nightmare> | disable | enable | reset]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // 所有玩家都可以使用
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender instanceof EntityPlayer;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            throw new CommandException("This command can only be used by players");
        }

        EntityPlayer player = (EntityPlayer) sender;
        IPlayerDifficulty cap = CapabilityHandler.getPlayerDifficulty(player);

        if (cap == null) {
            throw new CommandException("Failed to get player difficulty data");
        }

        // 无参数 - 显示当前状态
        if (args.length == 0) {
            showStatus(player, cap);
            return;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "set":
                if (args.length < 2) {
                    throw new CommandException("Usage: /adversity set <0.0-2.0>");
                }
                float multiplier = (float) parseDouble(args[1], 0.0, 2.0);
                cap.setDifficultyMultiplier(multiplier);
                sendSuccess(player, "Difficulty multiplier set to " + formatMultiplier(multiplier));
                break;

            case "preset":
                if (args.length < 2) {
                    throw new CommandException("Usage: /adversity preset <peaceful|easy|normal|hard|nightmare>");
                }
                applyPreset(player, cap, args[1].toLowerCase());
                break;

            case "disable":
                cap.setDifficultyDisabled(true);
                sendSuccess(player, "Adversity system " + TextFormatting.RED + "DISABLED" + TextFormatting.GREEN + " for you");
                break;

            case "enable":
                cap.setDifficultyDisabled(false);
                sendSuccess(player, "Adversity system " + TextFormatting.GREEN + "ENABLED" + TextFormatting.GREEN + " for you");
                break;

            case "reset":
                cap.setDifficultyMultiplier(1.0f);
                cap.setDifficultyDisabled(false);
                cap.resetKillCount();
                sendSuccess(player, "Difficulty reset to default (Normal)");
                break;

            default:
                throw new CommandException("Unknown subcommand: " + subcommand);
        }
    }

    private void showStatus(EntityPlayer player, IPlayerDifficulty cap) {
        player.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Adversity Difficulty ==="));

        String status = cap.isDifficultyDisabled()
            ? TextFormatting.RED + "DISABLED"
            : TextFormatting.GREEN + "ENABLED";
        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Status: " + status));

        String multiplierStr = formatMultiplier(cap.getDifficultyMultiplier());
        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Multiplier: " + TextFormatting.WHITE + multiplierStr));

        String presetName = getPresetName(cap.getDifficultyMultiplier());
        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Preset: " + TextFormatting.WHITE + presetName));

        player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Kill Count: " + TextFormatting.WHITE + cap.getKillCount()));
    }

    private void applyPreset(EntityPlayer player, IPlayerDifficulty cap, String preset) throws CommandException {
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
        sendSuccess(player, "Difficulty set to " + displayName);
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

    private void sendSuccess(EntityPlayer player, String message) {
        player.sendMessage(new TextComponentString(TextFormatting.GREEN + "[Adversity] " + message));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("preset")) {
            return getListOfStringsMatchingLastWord(args, PRESETS);
        }
        return Collections.emptyList();
    }
}
