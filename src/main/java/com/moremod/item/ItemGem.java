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
 * ✅ 修复：使用绝对品质映射，符合玩家直觉
 * - 低等级宝石自然显示低劣颜色
 * - 高等级宝石自然显示高级颜色
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
     * ✅ 注册动态品质属性（绝对品质映射版）
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

            // 分离布尔和数值词条
            int numericQualitySum = 0;
            int numericCount = 0;
            int booleanCount = 0;
            
            for (IdentifiedAffix affix : affixes) {
                int quality = affix.getQuality();
                
                // 检测布尔词条（极端值）
                if (quality <= 5 || quality >= 95) {
                    booleanCount++;
                } else {
                    numericQualitySum += quality;
                    numericCount++;
                }
            }
            
            // 根据词条组成决定计算方式
            if (numericCount == 0) {
                // 全是布尔词条 - 按数量给予固定稀有度
                return getBooleanGemQuality(booleanCount, affixes.size());
            }
            
            // 有数值词条 - 使用数值词条的平均品质
            int avgQuality = numericQualitySum / numericCount;
            
            // 布尔词条加成：每个布尔词条给 +5% 品质加成
            int booleanBonus = booleanCount * 5;
            avgQuality = Math.min(100, avgQuality + booleanBonus);
            
            // ⭐ 使用绝对品质映射
            return getAbsoluteQualityColor(avgQuality);
        });
    }

    /**
     * 纯布尔词条宝石的品质计算
     */
    private float getBooleanGemQuality(int booleanCount, int totalCount) {
        // 布尔词条比较特殊，根据数量直接给稀有度
        if (totalCount >= 5) {
            return 1.0F; // 金色 - 5+个布尔词条极其稀有
        } else if (totalCount >= 4) {
            return 0.8F; // 紫色 - 4个布尔词条
        } else if (totalCount >= 3) {
            return 0.6F; // 蓝色 - 3个布尔词条
        } else if (totalCount >= 2) {
            return 0.4F; // 绿色 - 2个布尔词条
        } else {
            return 0.2F; // 白色 - 1个布尔词条
        }
    }

    /**
     * ✅ 绝对品质映射（符合直觉）
     * 
     * 设计理念：
     * - 品质分段固定，不随等级变化
     * - 低等级宝石因为品质范围低，自然大部分是低劣颜色
     * - 高等级宝石因为品质范围高，自然大部分是高级颜色
     * - 符合玩家"低级=垃圾，高级=极品"的直觉
     *
     * 品质分段（可调整）：
     * - 0-14%:   灰色（垃圾）
     * - 15-29%:  白色（较差）
     * - 30-49%:  绿色（普通）
     * - 50-69%:  蓝色（良好）
     * - 70-84%:  紫色（优秀）
     * - 85-100%: 金色（完美）
     *
     * @param actualQuality 实际品质 (0-100)
     * @return 贴图索引 (0.0, 0.2, 0.4, 0.6, 0.8, 1.0)
     */
    private float getAbsoluteQualityColor(int actualQuality) {
        if (actualQuality < 15) {
            return 0.0F; // 灰色
        } else if (actualQuality < 30) {
            return 0.2F; // 白色
        } else if (actualQuality < 50) {
            return 0.4F; // 绿色
        } else if (actualQuality < 70) {
            return 0.6F; // 蓝色
        } else if (actualQuality < 85) {
            return 0.8F; // 紫色
        } else {
            return 1.0F; // 金色
        }
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