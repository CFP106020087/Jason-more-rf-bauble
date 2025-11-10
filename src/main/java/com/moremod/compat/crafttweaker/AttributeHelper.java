package com.moremod.compat.crafttweaker;

import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.item.ItemStack;

import java.util.*;

/**
 * 属性修改助手 - 基于Minecraft原生AttributeModifier
 * 
 * 支持的属性：
 * - movementSpeed - 移动速度
 * - health - 最大生命值
 * - armor - 护甲值
 * - attackDamage - 攻击伤害
 * - attackSpeed - 攻击速度
 * - knockbackResistance - 击退抗性
 * - luck - 幸运值
 */
public class AttributeHelper {
    
    // UUID前缀（用于区分宝石属性修改器）
    private static final String UUID_PREFIX = "moremod-gem-";
    
    // 属性映射表
    private static final Map<String, IAttribute> ATTRIBUTE_MAP = new HashMap<>();
    
    static {
        // 注册所有支持的属性
        ATTRIBUTE_MAP.put("movementSpeed", SharedMonsterAttributes.MOVEMENT_SPEED);
        ATTRIBUTE_MAP.put("health", SharedMonsterAttributes.MAX_HEALTH);
        ATTRIBUTE_MAP.put("armor", SharedMonsterAttributes.ARMOR);
        ATTRIBUTE_MAP.put("attackDamage", SharedMonsterAttributes.ATTACK_DAMAGE);
        ATTRIBUTE_MAP.put("attackSpeed", SharedMonsterAttributes.ATTACK_SPEED);
        ATTRIBUTE_MAP.put("knockbackResistance", SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
        ATTRIBUTE_MAP.put("luck", SharedMonsterAttributes.LUCK);
    }
    
    /**
     * 应用属性修改（从宝石）
     * 
     * @param player 玩家
     * @param weapon 武器
     */
    public static void applyAttributesFromGems(EntityPlayer player, ItemStack weapon) {
        if (player == null || weapon.isEmpty()) return;
        
        // 先移除旧的属性修改
        removeAllGemAttributes(player);
        
        // 检查是否有镶嵌宝石
        if (!GemSocketHelper.hasSocketedGems(weapon)) {
            return;
        }
        
        // 收集所有属性加成
        Map<String, Double> attributeBonuses = new HashMap<>();
        
        // 获取所有镶嵌的宝石
        ItemStack[] gems = GemSocketHelper.getAllSocketedGems(weapon);
        for (ItemStack gem : gems) {
            if (gem.isEmpty() || !GemNBTHelper.isIdentified(gem)) {
                continue;
            }
            
            // 获取宝石词条
            List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
            for (IdentifiedAffix affix : affixes) {
                // 只处理 ATTRIBUTE_BONUS 类型
                if (affix.getAffix().getType() != GemAffix.AffixType.ATTRIBUTE_BONUS) {
                    continue;
                }
                
                // 获取属性类型和数值
                String attributeName = (String) affix.getAffix().getParameter("attribute");
                if (attributeName == null) continue;
                
                double value = affix.getValue();
                
                // 累加同类型属性
                attributeBonuses.merge(attributeName, value, Double::sum);
            }
        }
        
        // 应用所有属性修改
        for (Map.Entry<String, Double> entry : attributeBonuses.entrySet()) {
            String attributeName = entry.getKey();
            double totalBonus = entry.getValue();
            
            applyAttributeBonus(player, attributeName, totalBonus);
        }
    }
    
    /**
     * 应用单个属性加成
     */
    private static void applyAttributeBonus(EntityPlayer player, String attributeName, double bonus) {
        IAttribute attribute = ATTRIBUTE_MAP.get(attributeName);
        if (attribute == null) {
            System.err.println("[AttributeHelper] 未知属性: " + attributeName);
            return;
        }
        
        IAttributeInstance instance = player.getEntityAttribute(attribute);
        if (instance == null) {
            System.err.println("[AttributeHelper] 无法获取属性实例: " + attributeName);
            return;
        }
        
        // 创建唯一的UUID（基于属性名）
        UUID modifierUUID = UUID.nameUUIDFromBytes((UUID_PREFIX + attributeName).getBytes());
        
        // 移除旧的修改器（如果存在）
        AttributeModifier oldModifier = instance.getModifier(modifierUUID);
        if (oldModifier != null) {
            instance.removeModifier(oldModifier);
        }
        
        // 创建新的修改器
        int operation = getAttributeOperation(attributeName);
        AttributeModifier modifier = new AttributeModifier(
            modifierUUID,
            "moremod.gem." + attributeName,  // 修改器名称
            bonus,
            operation
        );
        
        // 应用修改器
        instance.applyModifier(modifier);
    }
    
    /**
     * 移除所有宝石属性修改
     */
    public static void removeAllGemAttributes(EntityPlayer player) {
        if (player == null) return;
        
        for (Map.Entry<String, IAttribute> entry : ATTRIBUTE_MAP.entrySet()) {
            String attributeName = entry.getKey();
            IAttribute attribute = entry.getValue();
            
            IAttributeInstance instance = player.getEntityAttribute(attribute);
            if (instance == null) continue;
            
            UUID modifierUUID = UUID.nameUUIDFromBytes((UUID_PREFIX + attributeName).getBytes());
            AttributeModifier modifier = instance.getModifier(modifierUUID);
            
            if (modifier != null) {
                instance.removeModifier(modifier);
            }
        }
    }
    
    /**
     * 获取属性修改器的操作类型
     * 
     * 0 = ADD (直接加法)
     * 1 = MULTIPLY_BASE (基于基础值的乘法，应用在所有加法之后)
     * 2 = MULTIPLY_TOTAL (基于总值的乘法，最后应用)
     */
    private static int getAttributeOperation(String attributeName) {
        switch (attributeName) {
            case "movementSpeed":
                return 2;  // MULTIPLY_TOTAL - 移动速度用百分比增幅
            
            case "health":
            case "armor":
            case "attackDamage":
                return 0;  // ADD - 生命、护甲、攻击用直接加法
            
            case "attackSpeed":
            case "knockbackResistance":
            case "luck":
                return 0;  // ADD
            
            default:
                return 0;  // 默认ADD
        }
    }
    
    /**
     * 获取玩家的某个属性当前值
     */
    public static double getAttributeValue(EntityPlayer player, String attributeName) {
        IAttribute attribute = ATTRIBUTE_MAP.get(attributeName);
        if (attribute == null) return 0;
        
        IAttributeInstance instance = player.getEntityAttribute(attribute);
        if (instance == null) return 0;
        
        return instance.getAttributeValue();
    }
    
    /**
     * 获取宝石提供的属性加成（不包括其他来源）
     */
    public static double getGemAttributeBonus(EntityPlayer player, String attributeName) {
        IAttribute attribute = ATTRIBUTE_MAP.get(attributeName);
        if (attribute == null) return 0;
        
        IAttributeInstance instance = player.getEntityAttribute(attribute);
        if (instance == null) return 0;
        
        UUID modifierUUID = UUID.nameUUIDFromBytes((UUID_PREFIX + attributeName).getBytes());
        AttributeModifier modifier = instance.getModifier(modifierUUID);
        
        return modifier != null ? modifier.getAmount() : 0;
    }
    
    /**
     * 刷新玩家的属性（切换武器时调用）
     */
    public static void refreshPlayerAttributes(EntityPlayer player) {
        if (player == null || player.world.isRemote) return;
        
        ItemStack mainHand = player.getHeldItemMainhand();
        applyAttributesFromGems(player, mainHand);
    }
}