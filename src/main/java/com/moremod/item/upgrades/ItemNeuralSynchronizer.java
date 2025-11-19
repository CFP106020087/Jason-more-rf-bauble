package com.moremod.item.upgrades;

import com.moremod.system.FleshRejectionSystem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;           // ✅ 添加
import net.minecraftforge.fml.relauncher.SideOnly;      // ✅ 添加

import java.util.List;

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
        super("NEURAL_SYNCHRONIZER", DESCRIPTIONS, 1);
        setTranslationKey("neural_synchronizer");
        setRegistryName("neural_synchronizer");
        setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)  // ✅ 添加这个注解
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "能量消耗: 50 RF/s");

        // ✅ 现在这行代码不会在服务器端被加载
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;
        if (player != null) {
            FleshRejectionSystem.RejectionStatus status = FleshRejectionSystem.getStatus(player);
            if (status != null) {
                tooltip.add("");
                tooltip.add(TextFormatting.GRAY + "━━━ 当前状态 ━━━");

                TextFormatting adaptColor = status.adaptation >= 120 ? TextFormatting.GREEN :
                        status.adaptation >= 80 ? TextFormatting.YELLOW : TextFormatting.RED;
                tooltip.add(TextFormatting.GRAY + "适应度: " + adaptColor +
                        String.format("%.0f", status.adaptation) + " / 120");

                TextFormatting rejectColor = status.rejection >= 80 ? TextFormatting.DARK_RED :
                        status.rejection >= 60 ? TextFormatting.RED :
                                status.rejection >= 40 ? TextFormatting.YELLOW : TextFormatting.GREEN;
                tooltip.add(TextFormatting.GRAY + "排异值: " + rejectColor +
                        String.format("%.1f%%", status.rejection));

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
        return net.minecraft.item.EnumRarity.EPIC;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}