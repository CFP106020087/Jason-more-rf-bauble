package com.moremod.system.ascension;

import com.moremod.system.humanity.AscensionRoute;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 升格Tooltip管理器
 * Ascension Tooltip Manager
 *
 * 为两条升格路线提供专属的物品描述文案
 */
public class AscensionTooltips {

    // ═══════════════════════════════════════════════════════════
    // 破碎之神 - 绝对的矛
    // Broken God - The Absolute Spear
    // ═══════════════════════════════════════════════════════════

    /**
     * 破碎之神 - 机械核心的tooltip
     * "身上的机械" - 取代了血肉的齿轮
     */
    public static final String[] BROKEN_GOD_CORE = {
            "",
            TextFormatting.DARK_RED + "══════ " + TextFormatting.RED + TextFormatting.BOLD + "破碎之神" + TextFormatting.DARK_RED + " ══════",
            "",
            TextFormatting.GRAY + "肉体已成齿轮。",
            TextFormatting.GRAY + "思维已成代码。",
            TextFormatting.GRAY + "曾经的你，已不复存在。",
            "",
            TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"我曾是谁？\"",
            TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"...这个问题已无意义。\"",
            "",
            TextFormatting.RED + "◆ " + TextFormatting.WHITE + "绝对的矛",
            TextFormatting.DARK_GRAY + "无坚不摧，万物皆可破碎。",
            TextFormatting.DARK_GRAY + "唯有毁灭，是永恒的真理。"
    };

    /**
     * 破碎之神 - 机械之心的tooltip
     * "心" - 不再跳动的心脏
     */
    public static final String[] BROKEN_GOD_HEART = {
            "",
            TextFormatting.DARK_RED + "══════ " + TextFormatting.RED + TextFormatting.BOLD + "破碎之心" + TextFormatting.DARK_RED + " ══════",
            "",
            TextFormatting.GRAY + "此心已不再跳动。",
            TextFormatting.GRAY + "冰冷的运算取代了温热的血液。",
            TextFormatting.GRAY + "没有恐惧，没有犹豫，没有怜悯。",
            "",
            TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"你还记得...心跳的感觉吗？\"",
            TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"...不。\"",
            "",
            TextFormatting.DARK_RED + "◆ " + TextFormatting.GRAY + "人性：" + TextFormatting.RED + "0%",
            TextFormatting.DARK_GRAY + "你已超脱凡俗的枷锁。",
            TextFormatting.DARK_GRAY + "或者说，你已失去了成为人的资格。"
    };

    // ═══════════════════════════════════════════════════════════
    // 香巴拉 - 绝对的盾
    // Shambhala - The Absolute Shield
    // ═══════════════════════════════════════════════════════════

    /**
     * 香巴拉 - 机械核心的tooltip
     * "身上的机械" - 守护人性的永恒齿轮
     */
    public static final String[] SHAMBHALA_CORE = {
            "",
            TextFormatting.GOLD + "══════ " + TextFormatting.AQUA + TextFormatting.BOLD + "香巴拉" + TextFormatting.GOLD + " ══════",
            "",
            TextFormatting.GRAY + "齿轮转动，非为毁灭。",
            TextFormatting.GRAY + "机关运行，皆为守护。",
            TextFormatting.GRAY + "你是人性与机械的完美容器。",
            "",
            TextFormatting.DARK_AQUA + "" + TextFormatting.ITALIC + "\"机械的尽头，不是虚无。\"",
            TextFormatting.DARK_AQUA + "" + TextFormatting.ITALIC + "\"而是涅磐。\"",
            "",
            TextFormatting.AQUA + "◆ " + TextFormatting.WHITE + "绝对的盾",
            TextFormatting.DARK_GRAY + "只要齿轮仍在转动，",
            TextFormatting.DARK_GRAY + "你便永不倒下。"
    };

    /**
     * 香巴拉 - 机械之心的tooltip
     * "心" - 被永恒齿轮护佑的心脏
     */
    public static final String[] SHAMBHALA_HEART = {
            "",
            TextFormatting.GOLD + "══════ " + TextFormatting.AQUA + TextFormatting.BOLD + "圆满之心" + TextFormatting.GOLD + " ══════",
            "",
            TextFormatting.GRAY + "此心仍在跳动。",
            TextFormatting.GRAY + "每一次脉搏，都与齿轮共鸣。",
            TextFormatting.GRAY + "温度犹存，人性圆满。",
            "",
            TextFormatting.DARK_AQUA + "" + TextFormatting.ITALIC + "\"你选择保留了什么？\"",
            TextFormatting.DARK_AQUA + "" + TextFormatting.ITALIC + "\"...一切值得守护之物。\"",
            "",
            TextFormatting.YELLOW + "◆ " + TextFormatting.GRAY + "人性：" + TextFormatting.AQUA + "100%",
            TextFormatting.DARK_GRAY + "你已证道机械涅磐。",
            TextFormatting.DARK_GRAY + "永恒的齿轮，守护着你跳动的心。"
    };

    // ═══════════════════════════════════════════════════════════
    // 工具方法
    // ═══════════════════════════════════════════════════════════

    /**
     * 获取当前客户端玩家的升格路线
     * @return 升格路线，如果无法获取则返回 NONE
     */
    @SideOnly(Side.CLIENT)
    public static AscensionRoute getClientAscensionRoute() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return AscensionRoute.NONE;

        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null) return AscensionRoute.NONE;

        return data.getAscensionRoute();
    }

    /**
     * 添加机械核心的升格tooltip
     * @param tooltip tooltip列表
     * @return true 如果添加了升格tooltip
     */
    @SideOnly(Side.CLIENT)
    public static boolean addCoreAscensionTooltip(List<String> tooltip) {
        AscensionRoute route = getClientAscensionRoute();

        if (route == AscensionRoute.BROKEN_GOD) {
            for (String line : BROKEN_GOD_CORE) {
                tooltip.add(line);
            }
            return true;
        } else if (route == AscensionRoute.SHAMBHALA) {
            for (String line : SHAMBHALA_CORE) {
                tooltip.add(line);
            }
            return true;
        }

        return false;
    }

    /**
     * 添加机械之心的升格tooltip
     * @param tooltip tooltip列表
     * @return true 如果添加了升格tooltip
     */
    @SideOnly(Side.CLIENT)
    public static boolean addHeartAscensionTooltip(List<String> tooltip) {
        AscensionRoute route = getClientAscensionRoute();

        if (route == AscensionRoute.BROKEN_GOD) {
            for (String line : BROKEN_GOD_HEART) {
                tooltip.add(line);
            }
            return true;
        } else if (route == AscensionRoute.SHAMBHALA) {
            for (String line : SHAMBHALA_HEART) {
                tooltip.add(line);
            }
            return true;
        }

        return false;
    }

    /**
     * 检查玩家是否已升格
     */
    @SideOnly(Side.CLIENT)
    public static boolean isAscended() {
        return getClientAscensionRoute() != AscensionRoute.NONE;
    }
}
