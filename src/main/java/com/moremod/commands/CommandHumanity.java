package com.moremod.commands;

import com.moremod.config.BrokenGodConfig;
import com.moremod.system.humanity.*;
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
import java.util.List;

/**
 * 人性值系统命令
 * /humanity - 查看当前状态
 * /humanity status - 查看详细状态
 * /humanity ascend - 尝试升格
 * /humanity set <value> - 设置人性值（OP权限）
 */
public class CommandHumanity extends CommandBase {

    @Override
    public String getName() {
        return "humanity";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/humanity [status|ascend|set <value>]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // 所有玩家都可以使用基本功能
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString("§c此命令只能由玩家执行"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        IHumanityData data = HumanityCapabilityHandler.getData(player);

        if (data == null) {
            player.sendMessage(new TextComponentString("§c无法获取人性值数据"));
            return;
        }

        if (!data.isSystemActive()) {
            player.sendMessage(new TextComponentString("§7人性值系统尚未激活。完成排异系统突破后自动激活。"));
            return;
        }

        if (args.length == 0) {
            // 简要状态
            showBriefStatus(player, data);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "status":
                showDetailedStatus(player, data);
                break;

            case "ascend":
                tryAscend(player, data);
                break;

            case "set":
                if (args.length < 2) {
                    player.sendMessage(new TextComponentString("§c用法: /humanity set <0-100>"));
                    return;
                }
                if (!sender.canUseCommand(2, "humanity.set")) {
                    player.sendMessage(new TextComponentString("§c你没有权限使用此命令"));
                    return;
                }
                try {
                    float value = Float.parseFloat(args[1]);
                    value = Math.max(0, Math.min(100, value));
                    // 使用 HumanitySpectrumSystem 设置，会自动触发同步
                    HumanitySpectrumSystem.setHumanity(player, value);
                    // 立即同步到客户端（不等待 tick）
                    HumanitySpectrumSystem.syncNow(player);
                    player.sendMessage(new TextComponentString("§a已将人性值设置为: " + String.format("%.1f%%", value)));
                } catch (NumberFormatException e) {
                    player.sendMessage(new TextComponentString("§c无效的数值"));
                }
                break;

            case "debug":
                if (!sender.canUseCommand(2, "humanity.debug")) {
                    player.sendMessage(new TextComponentString("§c你没有权限使用此命令"));
                    return;
                }
                showDebugInfo(player, data);
                break;

            default:
                player.sendMessage(new TextComponentString("§c未知子命令: " + subCommand));
                player.sendMessage(new TextComponentString("§7用法: " + getUsage(sender)));
        }
    }

    /**
     * 显示简要状态
     */
    private void showBriefStatus(EntityPlayer player, IHumanityData data) {
        float humanity = data.getHumanity();
        AscensionRoute route = data.getAscensionRoute();

        String routeStr = route == AscensionRoute.NONE ? "" :
                " §d[" + route.getDisplayNameCN() + "]";

        String tierColor = getTierColor(humanity);
        String tierName = getTierName(humanity);

        player.sendMessage(new TextComponentString(
                tierColor + "【人性值】" + String.format("%.1f%%", humanity) +
                        " §7- " + tierColor + tierName + routeStr
        ));

        // 简要效果提示
        if (route == AscensionRoute.BROKEN_GOD) {
            player.sendMessage(new TextComponentString("§5  ▸ 药水免疫 | 破碎遗物 | 无法使用链结站"));
        } else {
            if (humanity <= 10f) {
                player.sendMessage(new TextComponentString("§4  ▸ -50% MaxHP | +60%伤害 | 异常场Lv3"));
            } else if (humanity <= 25f) {
                player.sendMessage(new TextComponentString("§c  ▸ -30% MaxHP | +40%伤害 | 异常场Lv2"));
            } else if (humanity <= 40f) {
                player.sendMessage(new TextComponentString("§6  ▸ -15% MaxHP | +20%伤害 | 异常场Lv1"));
            } else if (humanity >= 80f) {
                player.sendMessage(new TextComponentString("§a  ▸ 研究协议加强 | NPC信任"));
            }
        }
    }

    /**
     * 显示详细状态
     */
    private void showDetailedStatus(EntityPlayer player, IHumanityData data) {
        float humanity = data.getHumanity();
        AscensionRoute route = data.getAscensionRoute();

        player.sendMessage(new TextComponentString("§d═══════ 人性值系统状态 ═══════"));

        // 基本信息
        String tierColor = getTierColor(humanity);
        player.sendMessage(new TextComponentString(
                tierColor + "人性值: " + String.format("%.2f%%", humanity) + " (" + getTierName(humanity) + ")"
        ));

        // 升格状态
        if (route != AscensionRoute.NONE) {
            player.sendMessage(new TextComponentString(
                    "§d升格路线: " + route.getDisplayNameCN()
            ));
        }

        // 低人性累计时间
        long lowHumanitySeconds = data.getLowHumanityTicks() / 20;
        if (lowHumanitySeconds > 0) {
            player.sendMessage(new TextComponentString(
                    "§5低人性累计: " + formatSeconds((int)lowHumanitySeconds)
            ));
        }

        // 崩解状态
        if (data.isDissolutionActive()) {
            int ticks = data.getDissolutionTicks();
            player.sendMessage(new TextComponentString(
                    "§4【警告】崩解进行中! 剩余: " + (ticks / 20) + " 秒"
            ));
        }

        // 存在锚定
        if (data.isExistenceAnchored(player.world.getTotalWorldTime())) {
            player.sendMessage(new TextComponentString(
                    "§b存在锚定: 激活中（人性不会低于10%）"
            ));
        }

        // 当前效果
        player.sendMessage(new TextComponentString("§7当前效果:"));

        // MaxHP 效果
        float hpMod = getHPModifier(humanity, route);
        if (hpMod != 0) {
            String hpStr = hpMod > 0 ? "§a+" + (int)(hpMod * 100) + "%" : "§c" + (int)(hpMod * 100) + "%";
            player.sendMessage(new TextComponentString("  §7▸ MaxHP: " + hpStr));
        }

        // 伤害效果
        float dmgMod = getDamageModifier(humanity, route);
        if (dmgMod != 0) {
            String dmgStr = dmgMod > 0 ? "§a+" + (int)(dmgMod * 100) + "%" : "§c" + (int)(dmgMod * 100) + "%";
            player.sendMessage(new TextComponentString("  §7▸ 伤害: " + dmgStr));
        }

        // NPC交互
        HumanityEffectsManager.NPCInteractionLevel npcLevel =
                HumanityEffectsManager.getNPCInteractionLevel(player);
        player.sendMessage(new TextComponentString(
                "  §7▸ NPC交互: " + getNPCLevelDescription(npcLevel)
        ));

        // 链结站访问
        boolean canUseSynergy = HumanityEffectsManager.canUseSynergyStation(player);
        player.sendMessage(new TextComponentString(
                "  §7▸ 链结站: " + (canUseSynergy ? "§a可用" : "§c禁止")
        ));

        // 升格条件检查
        if (route == AscensionRoute.NONE) {
            player.sendMessage(new TextComponentString("§7─────────────────────────"));
            checkAscensionConditions(player, data);
        }

        player.sendMessage(new TextComponentString("§d════════════════════════════"));
    }

    /**
     * 检查升格条件
     */
    private void checkAscensionConditions(EntityPlayer player, IHumanityData data) {
        float humanity = data.getHumanity();
        long lowHumanitySeconds = data.getLowHumanityTicks() / 20;
        int requiredSeconds = BrokenGodConfig.requiredLowHumanitySeconds;
        int activeModules = HumanityEffectsManager.getActiveModuleCount(player);
        int requiredModules = BrokenGodConfig.requiredModuleCount;

        // 破碎之神条件
        player.sendMessage(new TextComponentString("§5【破碎之神】升格条件:"));
        player.sendMessage(new TextComponentString(
                "  " + (humanity <= BrokenGodConfig.ascensionHumanityThreshold ? "§a✓" : "§c✗") +
                " §7人性值 ≤ " + (int)BrokenGodConfig.ascensionHumanityThreshold + "% (当前: " + String.format("%.1f%%", humanity) + ")"
        ));
        player.sendMessage(new TextComponentString(
                "  " + (lowHumanitySeconds >= requiredSeconds ? "§a✓" : "§c✗") +
                " §7低人性累计 ≥ " + formatSeconds(requiredSeconds) + " (当前: " + formatSeconds((int)lowHumanitySeconds) + ")"
        ));
        player.sendMessage(new TextComponentString(
                "  " + (activeModules >= requiredModules ? "§a✓" : "§c✗") +
                " §7模块安装数 ≥ " + requiredModules + " (当前: " + activeModules + ")"
        ));
    }

    private String formatSeconds(int totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + "秒";
        } else if (totalSeconds < 3600) {
            return (totalSeconds / 60) + "分" + (totalSeconds % 60) + "秒";
        } else {
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            return hours + "时" + minutes + "分";
        }
    }

    /**
     * 尝试升格
     */
    private void tryAscend(EntityPlayer player, IHumanityData data) {
        if (data.getAscensionRoute() != AscensionRoute.NONE) {
            player.sendMessage(new TextComponentString(
                    "§c你已经升格为 " + data.getAscensionRoute().getDisplayNameCN()
            ));
            return;
        }

        // 检查破碎之神条件
        if (HumanityEffectsManager.canAscendToBrokenGod(player)) {
            player.sendMessage(new TextComponentString("§5你满足破碎之神的升格条件!"));
            player.sendMessage(new TextComponentString("§7输入 §5/humanity ascend brokengod§7 确认升格"));
            return;
        }

        player.sendMessage(new TextComponentString("§c你尚未满足升格条件"));
        player.sendMessage(new TextComponentString("§7使用 /humanity status 查看详细条件"));
    }

    /**
     * 显示调试信息
     */
    private void showDebugInfo(EntityPlayer player, IHumanityData data) {
        player.sendMessage(new TextComponentString("§6=== 人性值调试信息 ==="));
        player.sendMessage(new TextComponentString("§7人性值: " + data.getHumanity()));
        player.sendMessage(new TextComponentString("§7升格路线: " + data.getAscensionRoute()));
        player.sendMessage(new TextComponentString("§7崩解存活: " + data.getDissolutionSurvivals()));
        player.sendMessage(new TextComponentString("§7低人性累计: " + formatSeconds((int)(data.getLowHumanityTicks() / 20))));
        player.sendMessage(new TextComponentString("§7系统激活: " + data.isSystemActive()));
        player.sendMessage(new TextComponentString("§7崩解中: " + data.isDissolutionActive()));
        player.sendMessage(new TextComponentString("§7崩解tick: " + data.getDissolutionTicks()));
        player.sendMessage(new TextComponentString("§7同步统计: " + HumanitySpectrumSystem.getSyncStats()));
    }

    // ========== 辅助方法 ==========

    private String getTierColor(float humanity) {
        if (humanity <= 10f) return "§4";      // 深红
        if (humanity <= 25f) return "§c";      // 红
        if (humanity <= 40f) return "§6";      // 金
        if (humanity <= 60f) return "§e";      // 黄
        if (humanity <= 80f) return "§a";      // 绿
        return "§b";                           // 青
    }

    private String getTierName(float humanity) {
        if (humanity <= 10f) return "临界崩解";
        if (humanity <= 25f) return "深度异化";
        if (humanity <= 40f) return "异常协议";
        if (humanity <= 60f) return "灰域";
        if (humanity <= 80f) return "稳定人机";
        return "研究协议";
    }

    private float getHPModifier(float humanity, AscensionRoute route) {
        if (route == AscensionRoute.BROKEN_GOD) return 0f; // HP由破碎心核管理
        if (humanity <= 10f) return -0.5f;
        if (humanity <= 25f) return -0.3f;
        if (humanity <= 40f) return -0.15f;
        return 0f;
    }

    private float getDamageModifier(float humanity, AscensionRoute route) {
        if (route == AscensionRoute.BROKEN_GOD) return 0.5f;
        if (humanity <= 10f) return 0.6f;
        if (humanity <= 25f) return 0.4f;
        if (humanity <= 40f) return 0.2f;
        return 0f;
    }

    private String getNPCLevelDescription(HumanityEffectsManager.NPCInteractionLevel level) {
        switch (level) {
            case TRUSTED: return "§a信任（-30%价格）";
            case NORMAL: return "§f正常";
            case SUSPICIOUS: return "§e怀疑（+50%价格）";
            case HOSTILE: return "§c敌对（拒绝交易）";
            case INVISIBLE: return "§8无视（无法交互）";
            default: return "§7未知";
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "status", "ascend", "set", "debug");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("ascend")) {
            return getListOfStringsMatchingLastWord(args, "brokengod");
        }
        return super.getTabCompletions(server, sender, args, targetPos);
    }
}
