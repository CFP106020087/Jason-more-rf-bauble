package com.moremod.compat.crafttweaker;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * 多元素伤害计算器（修正版）
 * 
 * 核心机制:
 * 1. 转换率可以叠加 (总和≤100%)
 * 2. 倍率直接累加 (1.5+1.8=3.3，不是1+0.5+0.8=2.3)
 * 3. 不同元素独立计算
 * 
 * 示例:
 * - 火焰转换25% + 火焰增伤1.5倍 + 火焰增伤1.8倍 = 25%物理转为火焰，火焰伤害×3.3倍
 * - 冰霜转换25% + 冰霜增伤1.8倍 = 25%物理转为冰霜，冰霜伤害×1.8倍
 * - 同时存在，各自独立生效
 * 
 * 计算公式:
 * 对每个元素类型:
 *   元素转换伤害 = 原始伤害 × 该元素转换率 × 该元素增伤倍率总和
 * 物理伤害 = 原始伤害 × (1 - 所有转换率总和)
 * 最终伤害 = 物理伤害 + 所有元素伤害之和
 */
public class MultiElementDamageCalculator {
    
    /**
     * 单个元素的伤害数据
     */
    public static class ElementDamageData {
        /** 元素类型 (fire/ice/lightning等) */
        public final String elementType;
        
        /** 转换率总和 (多个转换词条叠加) */
        public float conversionRate;
        
        /** 增伤倍率总和 (直接累加：1.5+1.8=3.3) */
        public float damageBonus;
        
        /** 固定伤害总和 */
        public float flatDamage;
        
        public ElementDamageData(String elementType) {
            this.elementType = elementType;
            this.conversionRate = 0.0f;
            this.damageBonus = 0.0f;
            this.flatDamage = 0.0f;
        }
        
        /**
         * 计算该元素造成的伤害
         * 
         * @param baseDamage 原始伤害
         * @return 该元素的最终伤害
         */
        public float calculateDamage(float baseDamage) {
            // ✅ 修正：如果没有增伤词条，默认1.0倍；有增伤则直接累加倍率
            float multiplier = (damageBonus > 0) ? damageBonus : 1.0f;
            
            // 转换伤害 = 原始伤害 × 转换率 × 倍率总和
            float convertedDamage = baseDamage * conversionRate * multiplier;
            
            // 加上固定伤害
            return convertedDamage + flatDamage;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Element[%s]: conv=%.0f%%, multiplier=×%.2f, flat=%.1f",
                elementType,
                conversionRate * 100,
                damageBonus > 0 ? damageBonus : 1.0f,
                flatDamage
            );
        }
    }
    
    /**
     * 完整的伤害计算结果
     */
    public static class DamageResult {
        /** 物理伤害 */
        public final float physicalDamage;
        
        /** 各元素伤害 (元素类型 -> 伤害值) */
        public final Map<String, Float> elementalDamages;
        
        /** 总伤害 */
        public final float totalDamage;
        
        public DamageResult(float physicalDamage, Map<String, Float> elementalDamages) {
            this.physicalDamage = physicalDamage;
            this.elementalDamages = elementalDamages;
            
            // 计算总伤害
            float total = physicalDamage;
            for (float damage : elementalDamages.values()) {
                total += damage;
            }
            this.totalDamage = total;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Total: %.1f (Physical: %.1f", totalDamage, physicalDamage));
            for (Map.Entry<String, Float> entry : elementalDamages.entrySet()) {
                sb.append(String.format(", %s: %.1f", entry.getKey(), entry.getValue()));
            }
            sb.append(")");
            return sb.toString();
        }
    }
    
    // ==========================================
    // 核心计算方法
    // ==========================================
    
    /**
     * 从武器读取词条并计算伤害
     * 
     * @param weapon 武器ItemStack
     * @param baseDamage 基础伤害
     * @return 计算结果
     */
    public static DamageResult calculateDamage(ItemStack weapon, float baseDamage) {
        // 读取武器上的所有词条
        Map<String, ElementDamageData> elementMap = parseAffixesFromWeapon(weapon);
        
        // 计算总转换率
        float totalConversion = 0.0f;
        for (ElementDamageData data : elementMap.values()) {
            totalConversion += data.conversionRate;
        }
        
        // 限制总转换率≤100%
        if (totalConversion > 1.0f) {
            // 按比例缩放每个元素的转换率
            float scale = 1.0f / totalConversion;
            for (ElementDamageData data : elementMap.values()) {
                data.conversionRate *= scale;
            }
            totalConversion = 1.0f;
        }
        
        // 计算物理伤害
        float physicalDamage = baseDamage * (1.0f - totalConversion);
        
        // 计算各元素伤害
        Map<String, Float> elementalDamages = new HashMap<>();
        for (Map.Entry<String, ElementDamageData> entry : elementMap.entrySet()) {
            String elementType = entry.getKey();
            ElementDamageData data = entry.getValue();
            
            float elementDamage = data.calculateDamage(baseDamage);
            if (elementDamage > 0) {
                elementalDamages.put(elementType, elementDamage);
            }
        }
        
        return new DamageResult(physicalDamage, elementalDamages);
    }
    
    /**
     * 从武器NBT解析词条数据
     * 
     * @param weapon 武器
     * @return 元素类型 -> 元素数据
     */
    private static Map<String, ElementDamageData> parseAffixesFromWeapon(ItemStack weapon) {
        Map<String, ElementDamageData> elementMap = new HashMap<>();
        
        if (!weapon.hasTagCompound()) {
            return elementMap;
        }
        
        NBTTagCompound nbt = weapon.getTagCompound();
        if (!nbt.hasKey("GemAffixes")) {
            return elementMap;
        }
        
        NBTTagCompound gemData = nbt.getCompoundTag("GemAffixes");
        NBTTagList affixList = gemData.getTagList("affixes", Constants.NBT.TAG_COMPOUND);
        
        // 遍历所有词条
        for (int i = 0; i < affixList.tagCount(); i++) {
            NBTTagCompound affixTag = affixList.getCompoundTagAt(i);
            
            String affixType = affixTag.getString("type");
            float value = affixTag.getFloat("value");
            String damageType = affixTag.getString("damageType");
            
            // 根据词条类型处理
            switch (affixType) {
                case "damage_conversion":
                    // 转换率 - 按元素类型叠加
                    ElementDamageData convData = elementMap.computeIfAbsent(
                            damageType, 
                            ElementDamageData::new
                    );
                    convData.conversionRate += value;
                    break;
                    
                case "damage_multiplier":
                    // ✅ 修正：增伤倍率直接累加（1.5+1.8=3.3）
                    ElementDamageData multData = elementMap.computeIfAbsent(
                            damageType, 
                            ElementDamageData::new
                    );
                    multData.damageBonus += value;  // 不再减1
                    break;
                    
                case "flat_damage":
                    // 固定伤害 - 直接叠加
                    ElementDamageData flatData = elementMap.computeIfAbsent(
                            damageType, 
                            ElementDamageData::new
                    );
                    flatData.flatDamage += value;
                    break;
                    
                // 其他类型(攻速/属性/特效)在这里不处理
            }
        }
        
        return elementMap;
    }
    
    // ==========================================
    // 调试工具
    // ==========================================
    
    /**
     * 调试输出 - 显示武器的所有元素数据
     */
    public static void debugPrintWeapon(ItemStack weapon) {
        System.out.println("========== 武器词条分析 ==========");
        System.out.println("物品: " + weapon.getDisplayName());
        
        Map<String, ElementDamageData> elementMap = parseAffixesFromWeapon(weapon);
        
        if (elementMap.isEmpty()) {
            System.out.println("  (无元素词条)");
        } else {
            for (ElementDamageData data : elementMap.values()) {
                System.out.println("  " + data);
            }
            
            // 计算总转换率
            float totalConv = elementMap.values().stream()
                    .map(d -> d.conversionRate)
                    .reduce(0.0f, Float::sum);
            System.out.println("  总转换率: " + (totalConv * 100) + "%");
        }
        
        System.out.println("===================================");
    }
    
    /**
     * 模拟伤害计算示例
     */
    public static void exampleCalculation() {
        System.out.println("\n========== 伤害计算示例（修正版） ==========");
        
        // 示例1: 单元素转换+单个增伤
        System.out.println("\n【示例1】火焰转换25% + 火焰增伤1.5倍");
        System.out.println("原始伤害: 100");
        
        ElementDamageData fire = new ElementDamageData("fire");
        fire.conversionRate = 0.25f;
        fire.damageBonus = 1.5f;  // 直接存1.5
        
        float fireDamage = fire.calculateDamage(100);
        float physicalDamage = 100 * (1 - 0.25f);
        
        System.out.println("  物理伤害: " + physicalDamage);
        System.out.println("  火焰伤害: " + fireDamage + " (100×25%×1.5)");
        System.out.println("  总伤害: " + (physicalDamage + fireDamage));
        
        // 示例2: 单元素多个增伤宝石
        System.out.println("\n【示例2】火焰转换25% + 火焰增伤1.5倍 + 火焰增伤1.8倍");
        System.out.println("原始伤害: 100");
        
        fire.damageBonus = 1.5f + 1.8f;  // 直接累加 = 3.3
        
        fireDamage = fire.calculateDamage(100);
        physicalDamage = 100 * (1 - 0.25f);
        
        System.out.println("  物理伤害: " + physicalDamage);
        System.out.println("  火焰伤害: " + fireDamage + " (100×25%×3.3)");
        System.out.println("  总伤害: " + (physicalDamage + fireDamage));
        
        // 示例3: 多元素同时生效
        System.out.println("\n【示例3】火焰转换25%+增伤3.3倍 + 冰霜转换25%+增伤1.8倍");
        System.out.println("原始伤害: 100");
        
        ElementDamageData ice = new ElementDamageData("ice");
        ice.conversionRate = 0.25f;
        ice.damageBonus = 1.8f;
        
        float fireDmg = fire.calculateDamage(100);
        float iceDmg = ice.calculateDamage(100);
        float physDmg = 100 * (1 - 0.25f - 0.25f);
        
        System.out.println("  物理伤害: " + physDmg);
        System.out.println("  火焰伤害: " + fireDmg + " (100×25%×3.3)");
        System.out.println("  冰霜伤害: " + iceDmg + " (100×25%×1.8)");
        System.out.println("  总伤害: " + (physDmg + fireDmg + iceDmg));
        
        // 示例4: 只有转换没有增伤
        System.out.println("\n【示例4】火焰转换25%（无增伤宝石）");
        System.out.println("原始伤害: 100");
        
        ElementDamageData fireNoBonus = new ElementDamageData("fire");
        fireNoBonus.conversionRate = 0.25f;
        fireNoBonus.damageBonus = 0.0f;  // 无增伤
        
        fireDamage = fireNoBonus.calculateDamage(100);
        physicalDamage = 100 * (1 - 0.25f);
        
        System.out.println("  物理伤害: " + physicalDamage);
        System.out.println("  火焰伤害: " + fireDamage + " (100×25%×1.0)");
        System.out.println("  总伤害: " + (physicalDamage + fireDamage));
        
        // 示例5: 转换率超过100%
        System.out.println("\n【示例5】火焰转换60%+增伤2.0倍 + 冰霜转换60%+增伤1.5倍 (总120%→缩放到100%)");
        System.out.println("原始伤害: 100");
        
        fire.conversionRate = 0.6f;
        fire.damageBonus = 2.0f;
        ice.conversionRate = 0.6f;
        ice.damageBonus = 1.5f;
        
        // 缩放: 60%/120% = 50%, 60%/120% = 50%
        float scaledFire = 0.6f * (1.0f / 1.2f);
        float scaledIce = 0.6f * (1.0f / 1.2f);
        
        fire.conversionRate = scaledFire;
        ice.conversionRate = scaledIce;
        
        fireDmg = fire.calculateDamage(100);
        iceDmg = ice.calculateDamage(100);
        
        System.out.println("  火焰转换率: 60% → " + (scaledFire * 100) + "%");
        System.out.println("  冰霜转换率: 60% → " + (scaledIce * 100) + "%");
        System.out.println("  物理伤害: 0 (完全转换)");
        System.out.println("  火焰伤害: " + fireDmg + " (100×50%×2.0)");
        System.out.println("  冰霜伤害: " + iceDmg + " (100×50%×1.5)");
        System.out.println("  总伤害: " + (fireDmg + iceDmg));
        
        System.out.println("\n============================================");
    }
}
