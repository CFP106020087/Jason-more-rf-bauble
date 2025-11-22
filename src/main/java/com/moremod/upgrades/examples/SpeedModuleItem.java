package com.moremod.upgrades.examples;

import com.moremod.upgrades.platform.ModuleUpgradeItem;
import net.minecraft.util.text.TextFormatting;

/**
 * 速度模块物品示例
 *
 * 展示如何使用 ModuleUpgradeItem 基类创建模块物品。
 *
 * 用法：
 * 1. 继承 ModuleUpgradeItem
 * 2. 在构造函数中传入对应的运行时模块
 * 3. 可选：设置自定义描述
 * 4. 可选：重写方法以自定义行为
 *
 * @author Module Platform
 * @see SpeedModule
 * @see ModuleUpgradeItem
 */
public class SpeedModuleItem extends ModuleUpgradeItem {

    /**
     * 构造函数 - 最简版本
     */
    public SpeedModuleItem() {
        // 关联到 SpeedModule 运行时模块，每个物品提供 1 级升级
        super(SpeedModule.INSTANCE, 1);

        // 设置自定义描述（可选）
        this.setDescriptions(
                TextFormatting.GRAY + "提升玩家移动速度",
                TextFormatting.GRAY + "等级越高速度越快",
                TextFormatting.GOLD + "• Lv.1-2: 速度 I",
                TextFormatting.GOLD + "• Lv.3-4: 速度 II",
                TextFormatting.GOLD + "• Lv.5: 速度 III"
        );
    }

    /**
     * 示例：重写 addModuleDescription 方法以提供更详细的描述
     */
    /*
    @Override
    @SideOnly(Side.CLIENT)
    protected void addModuleDescription(List<String> tooltip) {
        tooltip.add(TextFormatting.GRAY + "装备后自动提升移动速度");
        tooltip.add(TextFormatting.GRAY + "每级消耗 5 RF/tick");

        // 添加等级对应的效果
        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "等级效果:");
        tooltip.add(TextFormatting.GRAY + "• Lv.1-2: 速度 I (20%)");
        tooltip.add(TextFormatting.GRAY + "• Lv.3-4: 速度 II (40%)");
        tooltip.add(TextFormatting.GRAY + "• Lv.5: 速度 III (60%)");
    }
    */

    /**
     * 示例：重写 onUpgradeSuccess 方法以发送自定义消息
     */
    /*
    @Override
    protected void onUpgradeSuccess(@Nonnull EntityPlayer player, @Nonnull ItemStack coreStack, int newLevel) {
        // 先调用父类默认消息
        super.onUpgradeSuccess(player, coreStack, newLevel);

        // 添加额外的提示
        if (newLevel == 1) {
            player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "提示：速度模块会持续消耗能量"
            ));
        } else if (newLevel == 5) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "✦ 速度模块已达到最高等级！"
            ));
        }
    }
    */

    /**
     * 示例：重写 canUpgrade 方法以添加自定义条件
     */
    /*
    @Override
    protected boolean canUpgrade(@Nonnull EntityPlayer player, @Nonnull ItemStack coreStack) {
        // 先检查默认条件
        if (!super.canUpgrade(player, coreStack)) {
            return false;
        }

        // 添加自定义条件（例如：需要其他模块）
        int energyLevel = ModuleDataStorage.getModuleLevel(coreStack, "ENERGY_CAPACITY");
        if (energyLevel < 2) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "需要先将能量容量升级到 Lv.2！"
            ));
            return false;
        }

        return true;
    }
    */
}
