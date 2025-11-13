// com/moremod/item/upgrades/ItemNeuralSynchronizer.java
package com.moremod.item.upgrades;

import com.moremod.system.FleshRejectionSystem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * 神经同步器升级模块
 */
public class ItemNeuralSynchronizer extends com.moremod.item.upgrades.ItemUpgradeComponent {

    private static final String[] DESCRIPTIONS = new String[] {
            TextFormatting.AQUA + "• 适应度 +100",
            TextFormatting.GREEN + "• 缓慢减少排异值 (-0.005/秒)",
            TextFormatting.YELLOW + "• 清除负面效果",
            TextFormatting.GOLD + "• 减少出血时间",
            "",
            TextFormatting.DARK_AQUA + "『突破血肉排异的关键』"
    };

    public ItemNeuralSynchronizer() {
        // 使用字符串构造函数
        super("NEURAL_SYNCHRONIZER", DESCRIPTIONS, 1);

        setTranslationKey("neural_synchronizer");
        setRegistryName("neural_synchronizer");
        setMaxStackSize(1);
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        // 先调用父类的基础信息
        super.addInformation(stack, worldIn, tooltip, flagIn);

        // 添加额外的状态信息
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "能量消耗: 50 RF/s");

        // 显示当前排异状态
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;
        if (player != null) {
            FleshRejectionSystem.RejectionStatus status = FleshRejectionSystem.getStatus(player);
            if (status != null) {
                tooltip.add("");
                tooltip.add(TextFormatting.GRAY + "━━━ 当前状态 ━━━");

                // 适应度
                TextFormatting adaptColor = status.adaptation >= 120 ? TextFormatting.GREEN :
                        status.adaptation >= 80 ? TextFormatting.YELLOW :
                                TextFormatting.RED;
                tooltip.add(TextFormatting.GRAY + "适应度: " + adaptColor +
                        String.format("%.0f", status.adaptation) + " / 120");

                // 排异值
                TextFormatting rejectColor = status.rejection >= 80 ? TextFormatting.DARK_RED :
                        status.rejection >= 60 ? TextFormatting.RED :
                                status.rejection >= 40 ? TextFormatting.YELLOW :
                                        TextFormatting.GREEN;
                tooltip.add(TextFormatting.GRAY + "排异值: " + rejectColor +
                        String.format("%.1f%%", status.rejection));

                // 预测效果
                if (!status.hasSynchronizer) {
                    float newAdaptation = status.adaptation + 100;
                    if (newAdaptation >= 120) {
                        tooltip.add("");
                        tooltip.add(TextFormatting.GREEN + "✓ 安装后将突破血肉排异！");
                    } else {
                        float needed = 120 - newAdaptation;
                        tooltip.add("");
                        tooltip.add(TextFormatting.YELLOW + "安装后还需 " +
                                String.format("%.0f", needed) + " 点适应度");
                    }
                } else {
                    tooltip.add("");
                    tooltip.add(TextFormatting.AQUA + "✓ 神经同步器已激活");

                    if (status.transcended) {
                        tooltip.add(TextFormatting.GOLD + "✦ 已突破血肉排异");
                    }
                }
            }
        }
    }

    @Override
    public net.minecraft.item.EnumRarity getRarity(ItemStack stack) {
        return net.minecraft.item.EnumRarity.EPIC;  // 紫色
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;  // 附魔光效
    }
}