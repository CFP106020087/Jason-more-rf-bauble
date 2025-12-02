package com.moremod.event.eventHandler;

import com.moremod.compat.crafttweaker.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

/**
 * 武器镶嵌宝石 Tooltip 处理器 - 修复版 ✅
 *
 * 修复内容：
 * 1. ✅ 通过GemSocketHelper获取镶嵌的宝石ItemStack
 * 2. ✅ 使用GemNBTHelper.getAffixes()解析词条
 * 3. ✅ 修复affixId读取问题（从 "id" 读取）
 * 4. ✅ 支持6个宝石槽位
 * 5. ✅ 使用GemAffix的智能格式化，避免百分比误伤
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
public class WeaponGemTooltipHandler {

    private static boolean enableTooltip = true;
    private static final boolean DEBUG = false;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!enableTooltip) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        // ✅ 使用GemSocketHelper检查是否有镶嵌宝石
        if (!GemSocketHelper.hasSocketedGems(stack)) {
            return;
        }

        List<String> tooltip = event.getToolTip();

        // 检查是否按住Shift
        boolean showDetailed = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT) ||
                org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);

        addGemInfo(tooltip, stack, showDetailed);
    }

    /**
     * ✅ 添加宝石镶嵌信息
     */
    private static void addGemInfo(List<String> tooltip, ItemStack weapon, boolean showDetailed) {
        // ✅ 通过GemSocketHelper获取所有镶嵌的宝石
        ItemStack[] gems = GemSocketHelper.getAllSocketedGems(weapon);

        if (gems == null || gems.length == 0) return;

        // 收集所有宝石数据
        List<GemData> gemDataList = new ArrayList<>();
        for (int i = 0; i < gems.length; i++) {
            ItemStack gem = gems[i];
            if (gem.isEmpty()) continue;

            // ✅ 检查宝石是否已鉴定
            if (!GemNBTHelper.isIdentified(gem)) {
                if (DEBUG) {
                    System.out.println("[Tooltip] 槽位 " + i + ": 未鉴定宝石");
                }
                continue;
            }

            GemData gemData = parseGemData(gem, i);
            if (gemData != null) {
                gemDataList.add(gemData);
            }
        }

        if (gemDataList.isEmpty()) return;

        // 添加分隔线
        tooltip.add("");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "━━━ 镶嵌宝石 ━━━");

        // 显示宝石概览
        addGemOverview(tooltip, gemDataList);

        if (showDetailed) {
            // 详细模式：显示每个宝石的词条
            addDetailedGemInfo(tooltip, gemDataList);
        } else {
            // 简化模式：只显示总和效果
            addSummarizedEffects(tooltip, gemDataList);
        }

        // 提示信息
        if (!showDetailed) {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "按住 " +
                    TextFormatting.YELLOW + "Shift" +
                    TextFormatting.DARK_GRAY + " 查看详细词条");
        }
    }

    /**
     * 显示宝石概览
     */
    private static void addGemOverview(List<String> tooltip, List<GemData> gems) {
        int totalGems = gems.size();
        int avgQuality = gems.stream()
                .mapToInt(g -> g.quality)
                .sum() / Math.max(1, totalGems);

        String qualityColor = getQualityColor(avgQuality);

        tooltip.add(TextFormatting.GRAY + "已镶嵌: " +
                TextFormatting.AQUA + totalGems + TextFormatting.GRAY + " 个宝石");
        tooltip.add(TextFormatting.GRAY + "平均品质: " +
                qualityColor + avgQuality + "%");
    }

    /**
     * 添加详细的宝石信息（每个宝石单独显示）
     */
    private static void addDetailedGemInfo(List<String> tooltip, List<GemData> gems) {
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "镶嵌宝石详情:");

        for (GemData gem : gems) {
            String qualityColor = getQualityColor(gem.quality);

            // 宝石标题
            tooltip.add("");
            tooltip.add(TextFormatting.GRAY + "[槽位 " + (gem.slotIndex + 1) + "] " +
                    qualityColor + "宝石 Lv." + gem.level +
                    TextFormatting.GRAY + " (" + qualityColor + gem.quality + "%" + TextFormatting.GRAY + ")");

            // 词条列表
            if (!gem.affixes.isEmpty()) {
                for (IdentifiedAffix affix : gem.affixes) {
                    // ✅ 使用IdentifiedAffix的格式化方法
                    String formatted = affix.getFormattedDescription();
                    String qualityIndicator = getQualityIndicator(affix.getQuality());
                    
                    tooltip.add(TextFormatting.DARK_GRAY + "  • " +
                            TextFormatting.RESET + formatted + 
                            TextFormatting.GRAY + " " + qualityIndicator);
                }
            } else {
                tooltip.add(TextFormatting.DARK_GRAY + "  • " +
                        TextFormatting.GRAY + "无词条");
            }
        }
    }

    /**
     * ✅ 添加总和效果（所有宝石词条叠加）- 修复版
     */
    private static void addSummarizedEffects(List<String> tooltip, List<GemData> gems) {
        // 按词条ID分组统计
        Map<String, EffectSummary> effectSummaries = new HashMap<>();

        // 统计所有词条
        for (GemData gem : gems) {
            for (IdentifiedAffix affix : gem.affixes) {
                String affixId = affix.getAffix().getId();

                EffectSummary summary = effectSummaries.computeIfAbsent(
                        affixId,
                        k -> new EffectSummary(affix.getAffix())  // ✅ 传入GemAffix对象
                );

                summary.addValue(affix.getValue());
                summary.addQuality(affix.getQuality());
            }
        }

        if (effectSummaries.isEmpty()) return;

        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "总体效果:");

        // 按类型分组显示
        Map<GemAffix.AffixType, List<EffectSummary>> groupedEffects = new HashMap<>();
        for (EffectSummary summary : effectSummaries.values()) {
            groupedEffects.computeIfAbsent(summary.gemAffix.getType(), k -> new ArrayList<>())
                    .add(summary);
        }

        // 显示效果（按类型排序）
        displayEffectGroup(tooltip, groupedEffects, GemAffix.AffixType.DAMAGE_CONVERSION, "转换效果");
        displayEffectGroup(tooltip, groupedEffects, GemAffix.AffixType.DAMAGE_MULTIPLIER, "增伤效果");
        displayEffectGroup(tooltip, groupedEffects, GemAffix.AffixType.FLAT_DAMAGE, "固定伤害");
        displayEffectGroup(tooltip, groupedEffects, GemAffix.AffixType.ATTACK_SPEED, "攻击速度");
        displayEffectGroup(tooltip, groupedEffects, GemAffix.AffixType.SPECIAL_EFFECT, "特殊效果");
        displayEffectGroup(tooltip, groupedEffects, GemAffix.AffixType.ATTRIBUTE_BONUS, "属性加成");
    }

    /**
     * 显示效果组
     */
    private static void displayEffectGroup(List<String> tooltip, 
                                          Map<GemAffix.AffixType, List<EffectSummary>> groupedEffects,
                                          GemAffix.AffixType type,
                                          String groupName) {
        List<EffectSummary> effects = groupedEffects.get(type);
        if (effects == null || effects.isEmpty()) return;

        // 按效果名称排序
        effects.sort((a, b) -> a.gemAffix.getDisplayName().compareTo(b.gemAffix.getDisplayName()));

        for (EffectSummary summary : effects) {
            // ✅ 使用GemAffix的智能格式化方法
            String formattedDesc = summary.gemAffix.formatDescription(summary.totalValue);

            // 显示效果（带叠加标记）
            String stackInfo = "";
            if (summary.count > 1) {
                String avgQualityColor = getQualityColor(summary.getAverageQuality());
                stackInfo = TextFormatting.GRAY + " [" + 
                           TextFormatting.YELLOW + "×" + summary.count + 
                           TextFormatting.GRAY + " " + 
                           avgQualityColor + summary.getAverageQuality() + "%" +
                           TextFormatting.GRAY + "]";
            }

            tooltip.add(TextFormatting.DARK_GRAY + "  • " +
                    TextFormatting.RESET + formattedDesc + stackInfo);
        }
    }

    /**
     * ✅ 解析宝石数据（使用GemNBTHelper）
     */
    private static GemData parseGemData(ItemStack gemStack, int slotIndex) {
        if (gemStack.isEmpty()) return null;

        GemData data = new GemData();
        data.slotIndex = slotIndex;

        // ✅ 使用GemNBTHelper获取宝石等级和词条
        data.level = GemNBTHelper.getGemLevel(gemStack);
        data.affixes = GemNBTHelper.getAffixes(gemStack);

        // 计算平均品质
        if (!data.affixes.isEmpty()) {
            data.quality = data.affixes.stream()
                    .mapToInt(IdentifiedAffix::getQuality)
                    .sum() / data.affixes.size();
        }

        if (DEBUG) {
            System.out.println("[Tooltip] 槽位 " + slotIndex + ": Lv." + data.level +
                    ", 品质=" + data.quality + "%, 词条=" + data.affixes.size());
        }

        return data;
    }

    /**
     * 获取品质指示器
     */
    private static String getQualityIndicator(int quality) {
        if (quality >= 90) return TextFormatting.GOLD + "★★★";
        if (quality >= 70) return TextFormatting.LIGHT_PURPLE + "★★☆";
        if (quality >= 50) return TextFormatting.BLUE + "★☆☆";
        if (quality >= 30) return TextFormatting.GREEN + "☆☆☆";
        return "";
    }

    /**
     * 获取品质颜色
     */
    private static String getQualityColor(int quality) {
        if (quality >= 90) return TextFormatting.GOLD.toString();        // 金色
        if (quality >= 70) return TextFormatting.LIGHT_PURPLE.toString(); // 紫色
        if (quality >= 50) return TextFormatting.BLUE.toString();         // 蓝色
        if (quality >= 30) return TextFormatting.GREEN.toString();        // 绿色
        return TextFormatting.GRAY.toString();                            // 灰色
    }

    /**
     * 启用/禁用Tooltip
     */
    public static void setEnableTooltip(boolean enable) {
        enableTooltip = enable;
    }

    /**
     * 宝石数据类
     */
    private static class GemData {
        int slotIndex = 0;
        int level = 1;
        int quality = 0;
        List<IdentifiedAffix> affixes = new ArrayList<>();
    }

    /**
     * ✅ 效果汇总类 - 修复版
     */
    private static class EffectSummary {
        GemAffix gemAffix;  // ✅ 保存GemAffix引用，用于智能格式化
        float totalValue = 0;
        int count = 0;
        int totalQuality = 0;

        EffectSummary(GemAffix gemAffix) {
            this.gemAffix = gemAffix;
        }

        void addValue(float value) {
            this.totalValue += value;
            this.count++;
        }

        void addQuality(int quality) {
            this.totalQuality += quality;
        }

        int getAverageQuality() {
            return count > 0 ? totalQuality / count : 0;
        }
    }
}