package com.moremod.config;

import com.moremod.compat.crafttweaker.UpgradeMaterial;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class SwordMaterialData {
    public String tierName;
    public float attackDamage;
    public float attackSpeed;
    public Map<String, Double> extraAttributes;

    // 額外配置選項
    public int maxUpgradeLevel = 1;      // 最大升級次數
    public boolean requiresExperience = false;  // 是否需要經驗
    public int experienceCost = 0;       // 經驗消耗
    public float successRate = 1.0f;     // 成功率

    // ⚠️ 新增：拆除和物品識別相關字段
    public int removalCost = 5;          // 拆除單個寶石的經驗消耗（默認5級）
    public transient Item item;          // 對應的物品（transient = 不序列化到JSON）
    public int meta = 0;                 // 物品的metadata

    // 用於JSON配置的物品ID字段
    public String itemId;                // 物品ID（如 "minecraft:diamond"）

    public SwordMaterialData() {
        this.extraAttributes = new HashMap<>();
    }

    public SwordMaterialData(String tierName, float attackDamage, float attackSpeed) {
        this();
        this.tierName = tierName;
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
    }

    public SwordMaterialData(String tierName, float attackDamage, float attackSpeed, Object... attributes) {
        this(tierName, attackDamage, attackSpeed);

        // 解析屬性對（key, value, key, value...）
        for (int i = 0; i < attributes.length - 1; i += 2) {
            if (attributes[i] instanceof String && attributes[i + 1] instanceof Number) {
                addAttribute((String) attributes[i], ((Number) attributes[i + 1]).doubleValue());
            }
        }
    }

    public void addAttribute(String key, double value) {
        if (extraAttributes == null) {
            extraAttributes = new HashMap<>();
        }
        extraAttributes.put(key, value);
    }

    public double getAttribute(String key) {
        if (extraAttributes == null) return 0.0;
        return extraAttributes.getOrDefault(key, 0.0);
    }

    // 從 CraftTweaker 材料轉換
    public static SwordMaterialData fromCraftTweaker(UpgradeMaterial ctMaterial) {
        SwordMaterialData data = new SwordMaterialData(
                ctMaterial.id,
                ctMaterial.attackDamage,
                ctMaterial.attackSpeed
        );

        if (ctMaterial.extraAttributes != null) {
            data.extraAttributes.putAll(ctMaterial.extraAttributes);
        }

        // CraftTweaker材料使用默认拆除成本
        data.removalCost = 5;

        return data;
    }

    /**
     * 从itemId字符串加载对应的Item对象
     * 在配置加载后调用
     */
    public void resolveItem() {
        if (itemId != null && !itemId.isEmpty()) {
            String[] parts = itemId.split(":");
            if (parts.length >= 2) {
                String modId = parts[0];
                String itemName = parts[1];
                String fullId = modId + ":" + itemName;

                Item foundItem = ForgeRegistries.ITEMS.getValue(
                        new net.minecraft.util.ResourceLocation(fullId)
                );

                if (foundItem != null) {
                    this.item = foundItem;

                    // 如果有第三部分，那是meta值
                    if (parts.length >= 3) {
                        try {
                            this.meta = Integer.parseInt(parts[2]);
                        } catch (NumberFormatException e) {
                            this.meta = 0;
                        }
                    }

                    System.out.println("[SwordMaterialData] Resolved item: " + fullId +
                            (meta != 0 ? ":" + meta : ""));
                } else {
                    System.err.println("[SwordMaterialData] Failed to resolve item: " + fullId);
                }
            }
        }
    }

    // 驗證配置合法性
    public boolean validate() {
        if (tierName == null || tierName.isEmpty()) {
            return false;
        }

        if (attackDamage < 0 || attackSpeed < -10 || attackSpeed > 10) {
            return false;
        }

        if (maxUpgradeLevel < 1 || maxUpgradeLevel > 10) {
            maxUpgradeLevel = 1;
        }

        if (successRate < 0 || successRate > 1) {
            successRate = 1.0f;
        }

        if (removalCost < 0) {
            removalCost = 5;
        }

        return true;
    }

    @Override
    public String toString() {
        return "SwordMaterialData{" +
                "tier='" + tierName + '\'' +
                ", damage=" + attackDamage +
                ", speed=" + attackSpeed +
                ", attributes=" + extraAttributes +
                ", removalCost=" + removalCost +
                '}';
    }
}