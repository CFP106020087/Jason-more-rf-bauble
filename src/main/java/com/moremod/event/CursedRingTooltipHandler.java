package com.moremod.event;

import com.moremod.core.CurseDeathHook;
import com.moremod.entity.curse.EmbeddedCurseManager;
import com.moremod.entity.curse.EmbeddedCurseManager.EmbeddedRelicType;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Set;

/**
 * 七咒之戒 Tooltip 显示已净化的诅咒
 */
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
public class CursedRingTooltipHandler {

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        // 检查是否是七咒之戒
        if (stack.getItem().getRegistryName() == null) return;
        if (!stack.getItem().getRegistryName().toString().equals("enigmaticlegacy:cursed_ring")) {
            return;
        }

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        // 获取已嵌入的遗物
        Set<EmbeddedRelicType> embedded = EmbeddedCurseManager.getEmbeddedRelics(player);

        if (embedded.isEmpty()) {
            return;
        }

        List<String> tooltip = event.getToolTip();

        // 添加分隔线
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_PURPLE + "═══ 已净化的诅咒 ═══");

        // 显示每个已嵌入的遗物对应的诅咒净化
        for (EmbeddedRelicType type : EmbeddedRelicType.values()) {
            if (embedded.contains(type)) {
                // 已净化（绿色勾）
                tooltip.add(TextFormatting.GREEN + " ✔ " + TextFormatting.STRIKETHROUGH + getCurseName(type) +
                           TextFormatting.RESET + TextFormatting.GRAY + " ← " + type.getDisplayName());
            } else {
                // 未净化（红色叉）
                tooltip.add(TextFormatting.RED + " ✘ " + TextFormatting.GRAY + getCurseName(type));
            }
        }

        // 显示净化进度
        tooltip.add("");
        int count = embedded.size();
        float percent = count / 7.0f * 100;
        TextFormatting color = count >= 7 ? TextFormatting.GOLD : (count >= 4 ? TextFormatting.YELLOW : TextFormatting.WHITE);
        tooltip.add(TextFormatting.DARK_PURPLE + "净化进度: " + color + count + "/7 " +
                   TextFormatting.GRAY + "(" + String.format("%.0f%%", percent) + ")");

        if (count >= 7) {
            tooltip.add(TextFormatting.GOLD + "★ 七咒已完全净化！");
        }
    }

    /**
     * 获取诅咒名称
     */
    private static String getCurseName(EmbeddedRelicType type) {
        switch (type) {
            case SACRED_HEART:
                return "受到伤害加倍";
            case PEACE_EMBLEM:
                return "中立生物攻击";
            case GUARDIAN_SCALE:
                return "护甲效力降低30%";
            case COURAGE_BLADE:
                return "对怪物伤害降低50%";
            case FROST_DEW:
                return "着火永燃";
            case SOUL_ANCHOR:
                return "死亡灵魂破碎";
            case SLUMBER_SACHET:
                return "失眠症";
            default:
                return "未知诅咒";
        }
    }
}
