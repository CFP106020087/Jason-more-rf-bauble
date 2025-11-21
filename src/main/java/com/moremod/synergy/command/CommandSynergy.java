package com.moremod.synergy.command;

import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.core.SynergyRegistry;
import com.moremod.synergy.data.PlayerSynergyData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Synergy 激活/停用命令
 *
 * 说明：
 * - 临时实现：在 GUI 系统完成前，使用命令激活/停用 Synergy
 * - 命令格式：
 *   /synergy list - 列出所有可用的 Synergy
 *   /synergy activate <ID> - 激活指定 Synergy
 *   /synergy deactivate <ID> - 停用指定 Synergy
 *   /synergy active - 查看当前激活的 Synergy
 *   /synergy clear - 清空所有激活状态
 *
 * 注意：
 * - 仅限玩家使用
 * - 创造模式或 OP 权限可用
 * - 数据会持久化到玩家 NBT
 */
public class CommandSynergy extends CommandBase {

    @Override
    public String getName() {
        return "synergy";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/synergy <list|activate|deactivate|active|clear> [ID]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        // 要求 OP 权限或创造模式
        return 2;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        if (!(sender instanceof EntityPlayer)) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) sender;
        return player.isCreative() || super.checkPermission(server, sender);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "此命令仅限玩家使用"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;

        if (args.length == 0) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + getUsage(sender)));
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                listAllSynergies(player);
                break;

            case "activate":
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "用法: /synergy activate <ID>"));
                    return;
                }
                activateSynergy(player, args[1]);
                break;

            case "deactivate":
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "用法: /synergy deactivate <ID>"));
                    return;
                }
                deactivateSynergy(player, args[1]);
                break;

            case "active":
                listActiveSynergies(player);
                break;

            case "clear":
                clearAllSynergies(player);
                break;

            default:
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "未知子命令: " + subCommand));
                sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + getUsage(sender)));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "list", "activate", "deactivate", "active", "clear");
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("activate") || args[0].equalsIgnoreCase("deactivate"))) {
            List<String> synergyIds = SynergyRegistry.getInstance().getAllSynergies().stream()
                    .map(SynergyDefinition::getId)
                    .collect(Collectors.toList());
            return getListOfStringsMatchingLastWord(args, synergyIds);
        }

        return Collections.emptyList();
    }

    private void listAllSynergies(EntityPlayer player) {
        List<SynergyDefinition> allSynergies = new ArrayList<>(SynergyRegistry.getInstance().getAllSynergies());

        if (allSynergies.isEmpty()) {
            player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "没有注册任何 Synergy"));
            return;
        }

        player.sendMessage(new TextComponentString(TextFormatting.GOLD + "========== 可用 Synergy =========="));

        PlayerSynergyData data = PlayerSynergyData.get(player);

        for (SynergyDefinition synergy : allSynergies) {
            boolean isActivated = data.isSynergyActivated(synergy.getId());
            String status = isActivated ? TextFormatting.GREEN + "[已激活]" : TextFormatting.GRAY + "[未激活]";

            player.sendMessage(new TextComponentString(
                    status + TextFormatting.WHITE + " " + synergy.getId() +
                            " - " + TextFormatting.AQUA + synergy.getDisplayName()
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "  " + synergy.getDescription()
            ));
        }
    }

    private void activateSynergy(EntityPlayer player, String synergyId) {
        SynergyDefinition synergy = SynergyRegistry.getInstance().getSynergy(synergyId);

        if (synergy == null) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "未找到 Synergy: " + synergyId
            ));
            return;
        }

        PlayerSynergyData data = PlayerSynergyData.get(player);

        if (data.isSynergyActivated(synergyId)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "Synergy 已经激活: " + synergy.getDisplayName()
            ));
            return;
        }

        data.activateSynergy(synergyId);
        data.saveToPlayer(player);

        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ 已激活 Synergy: " + synergy.getDisplayName()
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "  " + synergy.getDescription()
        ));
    }

    private void deactivateSynergy(EntityPlayer player, String synergyId) {
        SynergyDefinition synergy = SynergyRegistry.getInstance().getSynergy(synergyId);

        if (synergy == null) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "未找到 Synergy: " + synergyId
            ));
            return;
        }

        PlayerSynergyData data = PlayerSynergyData.get(player);

        if (!data.isSynergyActivated(synergyId)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "Synergy 尚未激活: " + synergy.getDisplayName()
            ));
            return;
        }

        data.deactivateSynergy(synergyId);
        data.saveToPlayer(player);

        player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "✗ 已停用 Synergy: " + synergy.getDisplayName()
        ));
    }

    private void listActiveSynergies(EntityPlayer player) {
        PlayerSynergyData data = PlayerSynergyData.get(player);
        List<String> activatedIds = new ArrayList<>(data.getActivatedSynergies());

        if (activatedIds.isEmpty()) {
            player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "你当前没有激活任何 Synergy"));
            return;
        }

        player.sendMessage(new TextComponentString(TextFormatting.GOLD + "========== 已激活 Synergy =========="));

        for (String id : activatedIds) {
            SynergyDefinition synergy = SynergyRegistry.getInstance().getSynergy(id);
            if (synergy != null) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + id +
                                " - " + TextFormatting.AQUA + synergy.getDisplayName()
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "  " + synergy.getDescription()
                ));
            }
        }

        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "共 " + activatedIds.size() + " 个激活的 Synergy"
        ));
    }

    private void clearAllSynergies(EntityPlayer player) {
        PlayerSynergyData data = PlayerSynergyData.get(player);
        int count = data.getActivatedSynergies().size();

        data.clearAll();
        data.saveToPlayer(player);

        player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "已清空所有激活的 Synergy（共 " + count + " 个）"
        ));
    }
}
