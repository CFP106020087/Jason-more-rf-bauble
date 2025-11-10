package com.moremod.compat.crafttweaker;

import java.util.HashMap;
import java.util.Map;

public class UpgradeMaterial {
    public final String id;
    public  float attackDamage;
    public  float attackSpeed;
    public Map<String, Double> extraAttributes;

    public UpgradeMaterial(String id, float attackDamage, float attackSpeed) {
        this.id = id;
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.extraAttributes = new HashMap<>();
    }
}