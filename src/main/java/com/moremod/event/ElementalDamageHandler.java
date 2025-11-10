package com.moremod.event;

import com.moremod.compat.crafttweaker.GemAffix;
import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.compat.crafttweaker.GemSocketHelper;
import com.moremod.compat.crafttweaker.IdentifiedAffix;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 元素伤害处理器 - 宝石系统（修复版）
 * 
 * ⚠️ 关键修复：
 * 1. 方法必须是 static
 * 2. 使用 GemSocketHelper 获取宝石
 * 3. 使用 @Mod.EventBusSubscriber 自动注册
 * 
 * 使用 LivingDamageEvent（最优方案）：
 * - 在所有伤害修正后触发（护甲/抗性/附魔/难度/药水）
 * - 是实际扣血前的最后修改点
 * - 不会导致重复计算
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ElementalDamageHandler {
    
    private static final boolean DEBUG = true;
    
    /**
     * ✅ 核心修复：方法必须是 static
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        DamageSource source = event.getSource();
        
        // 只处理玩家攻击
        if (!(source.getTrueSource() instanceof EntityPlayer)) {
            return;
        }
        
        EntityPlayer attacker = (EntityPlayer) source.getTrueSource();
        ItemStack weapon = attacker.getHeldItemMainhand();
        
        // 没有武器或没有镶嵌宝石
        if (weapon.isEmpty() || !GemSocketHelper.hasSocketedGems(weapon)) {
            return;
        }
        
        // ✅ 核心修复：使用 GemSocketHelper 获取宝石
        ItemStack[] gems = GemSocketHelper.getAllSocketedGems(weapon);
        if (gems.length == 0) {
            return;
        }
        
        // 获取当前伤害（已经过所有修正）
        float currentDamage = event.getAmount();
        
        // 解析宝石词条
        DamageData damageData = new DamageData(currentDamage);
        
        // 遍历所有宝石
        for (ItemStack gem : gems) {
            if (gem.isEmpty() || !GemNBTHelper.isIdentified(gem)) {
                continue;
            }
            
            // 获取宝石的所有词条
            List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
            
            for (IdentifiedAffix affix : affixes) {
                applyAffix(damageData, affix);
            }
        }
        
        // 计算最终伤害
        float finalDamage = damageData.calculateFinalDamage();
        
        // 调试输出
        if (DEBUG && finalDamage != currentDamage) {
            System.out.println("╔════════════════════════════════════════════════════════╗");
            System.out.println("║        元素伤害系统 - 宝石效果                         ║");
            System.out.println("╠════════════════════════════════════════════════════════╣");
            System.out.println("║ 攻击者: " + attacker.getName());
            System.out.println("║ 受害者: " + event.getEntityLiving().getName());
            System.out.println("║ 武器: " + weapon.getDisplayName());
            System.out.println("║ 镶嵌宝石数: " + gems.length);
            System.out.println("║ ════════════════════════════════════════════════════");
            System.out.println("║ 原始伤害: " + String.format("%.2f", currentDamage));
            System.out.println("║ 最终伤害: " + String.format("%.2f", finalDamage));
            
            if (finalDamage > currentDamage) {
                float increase = (finalDamage / currentDamage - 1) * 100;
                System.out.println("║ 总增伤: +" + String.format("%.0f%%", increase));
            }
            
            System.out.println("╚════════════════════════════════════════════════════════╝");
        }
        
        // 设置最终伤害
        event.setAmount(finalDamage);
    }
    
    /**
     * 应用单个词条效果
     */
    private static void applyAffix(DamageData damageData, IdentifiedAffix affix) {
        float value = affix.getValue();
        GemAffix.AffixType type = affix.getAffix().getType();
        
        switch (type) {
            case DAMAGE_CONVERSION:
                // 伤害转换：物理 → 元素
                String convertType = (String) affix.getAffix().getParameter("damageType");
                if (convertType != null) {
                    damageData.addConversion(convertType, value);
                }
                break;
                
            case DAMAGE_MULTIPLIER:
                // 伤害倍率：元素伤害 ×value
                String multType = (String) affix.getAffix().getParameter("damageType");
                if (multType != null) {
                    damageData.addMultiplier(multType, value);
                }
                break;
                
            case FLAT_DAMAGE:
                // 固定伤害
                String flatType = (String) affix.getAffix().getParameter("damageType");
                if (flatType != null) {
                    damageData.addFlatDamage(flatType, value);
                } else {
                    damageData.physicalDamage += value;
                }
                break;
                
            case SPECIAL_EFFECT:
                // 特殊效果（伤害相关）
                String effectType = (String) affix.getAffix().getParameter("effectType");
                if (effectType != null) {
                    switch (effectType) {
                        case "critDamage":
                            damageData.damageMultiplier += value;
                            break;
                        case "armorPenetration":
                            damageData.damageMultiplier += value * 0.5f;
                            break;
                    }
                }
                break;
        }
    }
    
    /**
     * 伤害数据容器
     */
    private static class DamageData {
        float physicalDamage;
        Map<String, Float> elementalDamages = new HashMap<>();
        Map<String, Float> elementalMultipliers = new HashMap<>();
        float damageMultiplier = 1.0f;

        public DamageData(float originalDamage) {
            this.physicalDamage = originalDamage;
        }

        /**
         * 添加伤害转换（物理→元素）
         */
        public void addConversion(String elementType, float conversionRate) {
            float convertedDamage = physicalDamage * conversionRate;
            physicalDamage -= convertedDamage;

            float current = elementalDamages.getOrDefault(elementType, 0f);
            elementalDamages.put(elementType, current + convertedDamage);
        }

        /**
         * ✅ 修复：直接累加倍率值
         * @param multiplier 倍率值（如 1.5, 2.8）
         */
        public void addMultiplier(String elementType, float multiplier) {
            // 直接累加倍率：2.8 + 2.8 = 5.6
            float current = elementalMultipliers.getOrDefault(elementType, 0.0f);
            elementalMultipliers.put(elementType, current + multiplier);
        }

        /**
         * 添加固定元素伤害
         */
        public void addFlatDamage(String elementType, float damage) {
            float current = elementalDamages.getOrDefault(elementType, 0f);
            elementalDamages.put(elementType, current + damage);
        }

        /**
         * 计算最终伤害
         */
        public float calculateFinalDamage() {
            float totalDamage = physicalDamage;

            for (Map.Entry<String, Float> entry : elementalDamages.entrySet()) {
                String elementType = entry.getKey();
                float elementalDamage = entry.getValue();

                // 获取累加的倍率，如果没有增伤词条则默认1.0
                float multiplier = elementalMultipliers.getOrDefault(elementType, 0.0f);
                if (multiplier == 0.0f) {
                    multiplier = 1.0f;  // 无增伤词条时默认1倍
                }

                totalDamage += elementalDamage * multiplier;
            }

            return totalDamage * damageMultiplier;
        }
    }
}