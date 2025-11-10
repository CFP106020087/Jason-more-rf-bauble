package com.moremod.item;

import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.compat.crafttweaker.IdentifiedAffix;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 宝石物品 - 可镶嵌到装备上的词条载体
 *
 * 特性：
 * - 未鉴定：显示等级和词条数量
 * - 已鉴定：显示所有词条效果
 * - ✨ 动态稀有度：根据等级期望值显示相对品质
 */
public class ItemGem extends Item {

    public ItemGem() {
        super();
        setRegistryName("gem");
        setTranslationKey("gem");
        setMaxStackSize(1);
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.MISC);

        registerQualityProperty();
    }

    /**
     * ✨ 注册动态品质属性
     */
    private void registerQualityProperty() {
        if (net.minecraftforge.fml.common.FMLCommonHandler.instance().getSide().isServer()) {
            return;
        }

        this.addPropertyOverride(new ResourceLocation("quality"), (stack, world, entity) -> {
            if (!GemNBTHelper.isGem(stack)) {
                return 0.0F;
            }

            if (GemNBTHelper.isUnidentified(stack)) {
                return 0.0F; // 未鉴定
            }

            List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(stack);
            if (affixes.isEmpty()) {
                return 0.0F;
            }

            // 计算平均品质
            int totalQuality = 0;
            for (IdentifiedAffix affix : affixes) {
                totalQuality += affix.getQuality();
            }
            int avgQuality = totalQuality / affixes.size();

            // 获取宝石等级
            int gemLevel = GemNBTHelper.getGemLevel(stack);

            // ✨ 使用相对品质映射
            return getRelativeQualityColor(avgQuality, gemLevel);
        });
    }

    /**
     * ✨ 根据等级期望计算相对品质，返回对应的贴图索引
     *
     * 设计理念：
     * - 每个等级的宝石都能看到所有6种稀有度
     * - 品质相对于期望值的偏差决定稀有度
     * - 保证稀有度分布平衡
     *
     * @param actualQuality 实际品质 (0-100)
     * @param gemLevel 宝石等级 (1-100)
     * @return 贴图索引 (0.0, 0.2, 0.4, 0.6, 0.8, 1.0)
     */
    private float getRelativeQualityColor(int actualQuality, int gemLevel) {
        // 1. 计算该等级的期望品质
        float expectedQuality = calculateExpectedQuality(gemLevel);

        // 2. 计算实际品质与期望的差距（标准差）
        float deviation = actualQuality - expectedQuality;

        // 3. 标准化偏差（除以期望值，转换为百分比）
        float normalizedDeviation = deviation / Math.max(expectedQuality, 20); // 防止除以0

        // 4. 根据标准化偏差分配稀有度
        // 使用更宽松的阈值，确保所有颜色都能出现

        if (normalizedDeviation < -0.40) {
            return 0.0F; // 灰色 - 远低于期望（垃圾）
        } else if (normalizedDeviation < -0.20) {
            return 0.2F; // 白色 - 低于期望（较差）
        } else if (normalizedDeviation < -0.05) {
            return 0.4F; // 绿色 - 略低于期望（普通）
        } else if (normalizedDeviation < 0.15) {
            return 0.6F; // 蓝色 - 接近期望（良好）
        } else if (normalizedDeviation < 0.35) {
            return 0.8F; // 紫色 - 高于期望（优秀）
        } else {
            return 1.0F; // 金色 - 远高于期望（完美）
        }
    }

    /**
     * ✨ 计算指定等级宝石的期望品质
     *
     * 公式：考虑等级加成 + 规则保底（平均）
     *
     * 假设：
     * - 无规则保底时，均匀分布在 [levelBonus, 100] 范围
     * - 期望值 = (levelBonus + 100) / 2
     *
     * @param gemLevel 宝石等级
     * @return 期望品质 (0-100)
     */
    private float calculateExpectedQuality(int gemLevel) {
        // 等级加成公式（与GemNBTHelper中一致）
        float levelBonus = gemLevel * 0.003f; // 每级 +0.3%

        // 转换为百分比
        float levelBonusPercent = levelBonus * 100;

        // 计算期望值（假设在 [levelBonus, 100] 之间均匀分布）
        float expectedQuality = (levelBonusPercent + 100) / 2.0f;

        return expectedQuality;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        if (!GemNBTHelper.isGem(stack)) {
            return;
        }

        if (GemNBTHelper.isUnidentified(stack)) {
            int gemLevel = GemNBTHelper.getGemLevel(stack);
            tooltip.add(TextFormatting.GRAY + "未鉴定的宝石");
            tooltip.add(TextFormatting.YELLOW + "等级: " + TextFormatting.WHITE + gemLevel);
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "使用鉴定卷轴查看词条");
        }
        // 已鉴定状态的Lore在NBT中
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return GemNBTHelper.isIdentified(stack);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (stack.hasDisplayName()) {
            return stack.getDisplayName();
        }
        return super.getItemStackDisplayName(stack);
    }
}
