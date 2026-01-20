package com.moremod.accessorybox.unlock.commands;

import com.moremod.accessorybox.SlotLayoutHelper;
import com.moremod.accessorybox.unlock.SlotUnlockManager;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 解锁饰品栏位指令（OP权限）
 *
 * 用法：
 *   /unlockslot <player> <type> <n>    - 解锁指定类型的第N个额外槽位
 *   /unlockslot <player> <type> all    - 解锁指定类型的所有额外槽位
 *   /unlockslot <player> <type> list   - 列出指定类型的槽位状态
 *   /unlockslot <player> all           - 解锁所有额外槽位
 *   /unlockslot <player> reset         - 重置玩家解锁状态
 *
 * 类型：AMULET, RING, BELT, HEAD, BODY, CHARM, TRINKET
 */
public class UnlockSlotCommand extends CommandBase {

    private static final String[] SLOT_TYPES = {"AMULET", "RING", "BELT", "HEAD", "BODY", "CHARM", "TRINKET"};

    // 每种类型的原版槽位数量
    private static final int[] VANILLA_COUNTS = {1, 2, 1, 1, 1, 1, 7};

    @Override
    public String getName() {
        return "unlockslot";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/unlockslot <player> <type> <n|all|list> 或 /unlockslot <player> <all|reset>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;  // OP权限
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(getUsage(sender));
        }

        // 获取目标玩家
        EntityPlayerMP player = getPlayer(server, sender, args[0]);

        String action = args[1].toUpperCase();

        // 处理 /unlockslot <player> all
        if (action.equals("ALL")) {
            unlockAllSlots(sender, player);
            return;
        }

        // 处理 /unlockslot <player> reset
        if (action.equals("RESET")) {
            resetPlayer(sender, player);
            return;
        }

        // 验证类型
        if (!isValidType(action)) {
            throw new CommandException("无效的槽位类型: " + action + "。有效类型: " + String.join(", ", SLOT_TYPES));
        }

        String type = action;

        if (args.length < 3) {
            throw new WrongUsageException("/unlockslot <player> " + type + " <n|all|list>");
        }

        String subAction = args[2].toLowerCase();

        switch (subAction) {
            case "all":
                unlockAllOfType(sender, player, type);
                break;
            case "list":
                listSlots(sender, player, type);
                break;
            default:
                // 尝试解析为数字
                try {
                    int n = parseInt(subAction, 1, 100);
                    unlockNthSlot(sender, player, type, n);
                } catch (NumberFormatException e) {
                    throw new WrongUsageException("无效的参数: " + subAction + "。使用 <n>, 'all', 或 'list'");
                }
        }
    }

    /**
     * 解锁指定类型的第N个额外槽位
     */
    private void unlockNthSlot(ICommandSender sender, EntityPlayerMP player, String type, int n) throws CommandException {
        int[] slots = SlotLayoutHelper.getSlotIdsForType(type);
        int vanillaCount = getVanillaCount(type);

        // 计算额外槽位数量
        int extraCount = slots.length - vanillaCount;

        if (extraCount <= 0) {
            throw new CommandException(type + " 类型没有配置额外槽位！");
        }

        if (n < 1 || n > extraCount) {
            throw new CommandException(type + " 类型只有 " + extraCount + " 个额外槽位，无法解锁第 " + n + " 个！");
        }

        // 获取第N个额外槽位的ID
        int slotId = slots[vanillaCount + n - 1];

        // 检查是否已解锁
        if (SlotUnlockManager.getInstance().isSlotUnlocked(player, slotId)) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "槽位 " + type + " #" + n + " (ID:" + slotId + ") 已经解锁！"
            ));
            return;
        }

        // 解锁
        boolean success = SlotUnlockManager.getInstance().unlockSlotPermanent(player, slotId);

        if (success) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 已为 " + player.getName() + " 解锁 " + type + " 第 " + n + " 个额外槽位 (ID:" + slotId + ")"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "✓ 你的 " + getTypeName(type) + " 第 " + n + " 个额外槽位已解锁！"
            ));
        } else {
            throw new CommandException("解锁失败！");
        }
    }

    /**
     * 解锁指定类型的所有额外槽位
     */
    private void unlockAllOfType(ICommandSender sender, EntityPlayerMP player, String type) {
        int[] slots = SlotLayoutHelper.getSlotIdsForType(type);
        int vanillaCount = getVanillaCount(type);
        int unlocked = 0;

        for (int i = vanillaCount; i < slots.length; i++) {
            int slotId = slots[i];
            if (SlotUnlockManager.getInstance().unlockSlotPermanent(player, slotId)) {
                unlocked++;
            }
        }

        if (unlocked > 0) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 已为 " + player.getName() + " 解锁 " + type + " 类型的 " + unlocked + " 个额外槽位"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "✓ 你的 " + getTypeName(type) + " 全部额外槽位已解锁！(" + unlocked + "个)"
            ));
        } else {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + type + " 类型没有可解锁的额外槽位（可能已全部解锁或未配置）"
            ));
        }
    }

    /**
     * 解锁所有额外槽位
     */
    private void unlockAllSlots(ICommandSender sender, EntityPlayerMP player) {
        int totalUnlocked = 0;

        for (String type : SLOT_TYPES) {
            int[] slots = SlotLayoutHelper.getSlotIdsForType(type);
            int vanillaCount = getVanillaCount(type);

            for (int i = vanillaCount; i < slots.length; i++) {
                int slotId = slots[i];
                if (SlotUnlockManager.getInstance().unlockSlotPermanent(player, slotId)) {
                    totalUnlocked++;
                }
            }
        }

        if (totalUnlocked > 0) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 已为 " + player.getName() + " 解锁全部 " + totalUnlocked + " 个额外槽位"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "✓ 你的全部额外饰品槽位已解锁！(" + totalUnlocked + "个)"
            ));
        } else {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "没有可解锁的额外槽位（可能已全部解锁或未配置）"
            ));
        }
    }

    /**
     * 重置玩家解锁状态
     */
    private void resetPlayer(ICommandSender sender, EntityPlayerMP player) {
        SlotUnlockManager.getInstance().resetPlayer(player);
        sender.sendMessage(new TextComponentString(
                TextFormatting.RED + "⚠ 已重置 " + player.getName() + " 的所有槽位解锁状态"
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.RED + "⚠ 你的额外饰品槽位解锁状态已被重置！"
        ));
    }

    /**
     * 列出指定类型的槽位状态
     */
    private void listSlots(ICommandSender sender, EntityPlayerMP player, String type) {
        int[] slots = SlotLayoutHelper.getSlotIdsForType(type);
        int vanillaCount = getVanillaCount(type);
        Set<Integer> available = SlotUnlockManager.getInstance().getAvailableSlots(player.getUniqueID());

        sender.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "===== " + type + " 槽位状态 (" + player.getName() + ") ====="
        ));

        // 原版槽位
        sender.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "原版槽位 (" + vanillaCount + "个): " + TextFormatting.GREEN + "始终可用"
        ));

        // 额外槽位
        int extraCount = slots.length - vanillaCount;
        if (extraCount > 0) {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "额外槽位 (" + extraCount + "个):"
            ));

            for (int i = 0; i < extraCount; i++) {
                int slotId = slots[vanillaCount + i];
                boolean unlocked = available.contains(slotId);
                String status = unlocked ?
                        TextFormatting.GREEN + "✓ 已解锁" :
                        TextFormatting.RED + "✗ 锁定";

                sender.sendMessage(new TextComponentString(
                        "  " + TextFormatting.YELLOW + "#" + (i + 1) +
                                TextFormatting.GRAY + " (ID:" + slotId + ") - " + status
                ));
            }
        } else {
            sender.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "额外槽位: " + TextFormatting.YELLOW + "未配置"
            ));
        }

        sender.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "=========================================="
        ));
    }

    /**
     * 获取类型的原版槽位数量
     */
    private int getVanillaCount(String type) {
        for (int i = 0; i < SLOT_TYPES.length; i++) {
            if (SLOT_TYPES[i].equals(type)) {
                return VANILLA_COUNTS[i];
            }
        }
        return 0;
    }

    /**
     * 获取类型的中文名称
     */
    private String getTypeName(String type) {
        switch (type) {
            case "AMULET": return "项链";
            case "RING": return "戒指";
            case "BELT": return "腰带";
            case "HEAD": return "头部";
            case "BODY": return "身体";
            case "CHARM": return "护身符";
            case "TRINKET": return "万用槽位";
            default: return type;
        }
    }

    /**
     * 验证类型是否有效
     */
    private boolean isValidType(String type) {
        for (String t : SLOT_TYPES) {
            if (t.equals(type)) return true;
        }
        return false;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            // 玩家名补全
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        } else if (args.length == 2) {
            // 类型或 all/reset 补全
            return getListOfStringsMatchingLastWord(args,
                    "AMULET", "RING", "BELT", "HEAD", "BODY", "CHARM", "TRINKET", "all", "reset");
        } else if (args.length == 3) {
            String type = args[1].toUpperCase();
            if (isValidType(type)) {
                // 数字或 all/list 补全
                return getListOfStringsMatchingLastWord(args, "1", "2", "3", "all", "list");
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }
}
