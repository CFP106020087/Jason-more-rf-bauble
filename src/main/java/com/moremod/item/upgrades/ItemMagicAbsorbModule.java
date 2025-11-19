package com.moremod.item.upgrades;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 魔力吸收模块（MAGIC_ABSORB）
 *
 * 功能：将 PotionCore 魔法伤害加成转换为物理伤害
 *
 * 等级效果：
 *   Lv1：吸收 45% 法伤转物伤，余灼 +1.0/层
 *   Lv2：吸收 60% 法伤转物伤，余灼 +1.5/层
 *   Lv3：吸收 75% 法伤转物伤，余灼 +2.0/层，≥20层触发魔力爆心
 */
public class ItemMagicAbsorbModule extends com.moremod.item.upgrades.ItemUpgradeComponent {

    private static final String[] DESCRIPTIONS = new String[] {
            TextFormatting.AQUA + "• 将魔法伤害加成吸收并转化为物理力量",
            TextFormatting.GREEN + "• 叠加『余灼』，造成额外伤害",
            TextFormatting.GOLD + "• 高等级可触发『魔力爆心』爆发一击",
            "",
            TextFormatting.DARK_PURPLE + "『反魔者的力量——魔法越强，反噬越烈』"
    };

    public ItemMagicAbsorbModule() {
        super("MAGIC_ABSORB", DESCRIPTIONS, 3);
        setTranslationKey("magic_absorb");
        setRegistryName("magic_absorb");
        setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);

        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "能量消耗: 40 RF/s");

        tooltip.add("");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "━━━ 等级效果 ━━━");

        // 简洁的等级说明
        tooltip.add(TextFormatting.AQUA + "Lv1 " + TextFormatting.GRAY + "吸收 45% 法伤 → 物伤 | 余灼 +1.0/层");
        tooltip.add(TextFormatting.AQUA + "Lv2 " + TextFormatting.GRAY + "吸收 60% 法伤 → 物伤 | 余灼 +1.5/层");
        tooltip.add(TextFormatting.AQUA + "Lv3 " + TextFormatting.GRAY + "吸收 75% 法伤 → 物伤 | 余灼 +2.0/层");
        tooltip.add(TextFormatting.GOLD + "      " + TextFormatting.ITALIC +
                "≥20层触发「魔力爆心」+30% 爆发");

        // Shift 显示详细机制
        if (net.minecraft.client.settings.GameSettings.isKeyDown(
                net.minecraft.client.Minecraft.getMinecraft().gameSettings.keyBindSneak)) {

            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "━━━ 详细机制 ━━━");
            tooltip.add(TextFormatting.DARK_GRAY + "【法伤吸收】");
            tooltip.add(TextFormatting.DARK_GRAY + "  • 检测来自 PotionCore 的魔法伤害倍率");
            tooltip.add(TextFormatting.DARK_GRAY + "  • 将其转换为物理伤害加成");
            tooltip.add(TextFormatting.DARK_GRAY + "  • 转换效率随等级提升");

            tooltip.add(TextFormatting.DARK_GRAY + "【余灼系统】");
            tooltip.add(TextFormatting.DARK_GRAY + "  • 每次攻击叠加余灼层数");
            tooltip.add(TextFormatting.DARK_GRAY + "  • 吸收的法伤越多，叠层越快");
            tooltip.add(TextFormatting.DARK_GRAY + "  • 每层造成固定额外伤害");

            tooltip.add(TextFormatting.DARK_GRAY + "【魔力爆心】(Lv3)");
            tooltip.add(TextFormatting.DARK_GRAY + "  • 余灼 ≥20 层时自动触发");
            tooltip.add(TextFormatting.DARK_GRAY + "  • 造成 +30% 爆发伤害");
            tooltip.add(TextFormatting.DARK_GRAY + "  • 清空余灼并进入 10 秒冷却");

            tooltip.add(TextFormatting.DARK_GRAY + "【特殊交互】");
            tooltip.add(TextFormatting.DARK_GRAY + "  • 元素武器会绕过此模块");
            tooltip.add(TextFormatting.DARK_GRAY + "  • 无法伤时自动清空余灼");

        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC +
                    "<按住 Shift 查看详细机制>");
        }
    }

    @Override
    public net.minecraft.item.EnumRarity getRarity(ItemStack stack) {
        return net.minecraft.item.EnumRarity.EPIC;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}