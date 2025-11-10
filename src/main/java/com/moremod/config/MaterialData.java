package com.moremod.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 材料升級數據類
 */
public class MaterialData {
    public float attackDamage;
    public float attackSpeed;
    public Map<String, Double> extraAttributes;

    public MaterialData() {
        this.extraAttributes = new HashMap<>();
    }

    public MaterialData(float attackDamage, float attackSpeed) {
        this();
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
    }

    public void addAttribute(String key, double value) {
        if (extraAttributes == null) {
            extraAttributes = new HashMap<>();
        }
        extraAttributes.put(key, value);
    }

    public double getAttribute(String key) {
        if (extraAttributes == null) {
            return 0.0;
        }
        return extraAttributes.getOrDefault(key, 0.0);
    }
}
