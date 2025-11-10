package com.moremod.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import java.util.*;

/**
 * 剑升级系统 CraftTweaker API
 *
 * ✅ 优化内容：
 * - 移除对方块右键接口（IOnRightClickEffect）
 * - 只保留对空右键接口（IOnItemUseEffect）
 * - 优化代码结构和注释
 */
@ZenRegister
@ZenClass("mods.moremod.SwordUpgrade")
public class CTSwordUpgrade {

    private static final Map<String, UpgradeMaterial> MATERIALS = new HashMap<>();
    private static final Map<String, List<IUpgradeEffect>> EFFECTS = new HashMap<>();

    // ===================================
    // 材料注册
    // ===================================

    /**
     * 添加升级材料
     */
    @ZenMethod
    public static void addMaterial(String materialId, float attackDamage, float attackSpeed) {
        UpgradeMaterial material = new UpgradeMaterial(materialId, attackDamage, attackSpeed);
        MATERIALS.put(materialId, material);
        CraftTweakerAPI.logInfo("Added sword upgrade material: " + materialId);
    }

    /**
     * 添加带额外属性的升级材料
     */
    @ZenMethod
    public static void addMaterialWithAttributes(String materialId, float attackDamage,
                                                 float attackSpeed, Map<String, Double> attributes) {
        UpgradeMaterial material = new UpgradeMaterial(materialId, attackDamage, attackSpeed);
        material.extraAttributes = attributes;
        MATERIALS.put(materialId, material);
        CraftTweakerAPI.logInfo("Added sword upgrade material with attributes: " + materialId);
    }

    // ===================================
    // 效果注册
    // ===================================

    /**
     * 添加攻击命中效果
     */
    @ZenMethod
    public static void addOnHitEffect(String materialId, IOnHitEffect effect) {
        addEffect(materialId, effect);
        CraftTweakerAPI.logInfo("Added onHit effect for: " + materialId);
    }

    /**
     * 添加击杀效果
     */
    @ZenMethod
    public static void addOnKillEffect(String materialId, IOnKillEffect effect) {
        addEffect(materialId, effect);
        CraftTweakerAPI.logInfo("Added onKill effect for: " + materialId);
    }

    /**
     * 添加破坏方块效果
     */
    @ZenMethod
    public static void addOnBlockBreakEffect(String materialId, IOnBlockBreakEffect effect) {
        addEffect(materialId, effect);
        CraftTweakerAPI.logInfo("Added onBlockBreak effect for: " + materialId);
    }

    /**
     * 添加Tick效果（每秒触发）
     */
    @ZenMethod
    public static void addOnTickEffect(String materialId, IOnTickEffect effect) {
        addEffect(materialId, effect);
        CraftTweakerAPI.logInfo("Added onTick effect for: " + materialId);
    }

    /**
     * 添加对空右键效果
     */
    @ZenMethod
    public static void addOnItemUseEffect(String materialId, IOnItemUseEffect effect) {
        addEffect(materialId, effect);
        CraftTweakerAPI.logInfo("Added onItemUse effect for: " + materialId);
    }

    private static void addEffect(String materialId, IUpgradeEffect effect) {
        EFFECTS.computeIfAbsent(materialId, k -> new ArrayList<>()).add(effect);
    }

    // ===================================
    // 镶嵌数量查询（修正版）
    // ===================================

    /**
     * ✅ 获取剑上特定材料的镶嵌数量（修正版）
     *
     * 修正内容：固定遍历6个槽位，不依赖可能不存在的Count字段
     *
     * @param sword 剑物品
     * @param materialId 材料ID（例如 "minecraft:diamond"）
     * @return 镶嵌数量（0-5）
     */
    @ZenMethod
    public static int getInlayCount(IItemStack sword, String materialId) {
        if (sword == null || sword.isEmpty()) return 0;

        ItemStack mcStack = CraftTweakerMC.getItemStack(sword);
        if (mcStack.isEmpty() || !mcStack.hasTagCompound()) return 0;

        NBTTagCompound tag = mcStack.getTagCompound();
        if (!tag.hasKey("SwordUpgrades")) return 0;

        NBTTagCompound upgradeData = tag.getCompoundTag("SwordUpgrades");
        if (!upgradeData.hasKey("Inlays")) return 0;

        NBTTagCompound inlaysTag = upgradeData.getCompoundTag("Inlays");
        int materialCount = 0;

        // ✅ 核心修正：固定遍历6个槽位，不依赖Count字段
        for (int i = 0; i < 6; i++) {
            String inlayKey = "Inlay_" + i;
            if (inlaysTag.hasKey(inlayKey)) {
                NBTTagCompound inlay = inlaysTag.getCompoundTag(inlayKey);
                if (inlay.hasKey("Material")) {
                    String material = inlay.getString("Material");
                    if (material.equals(materialId)) {
                        materialCount++;
                    }
                }
            }
        }

        return materialCount;
    }

    /**
     * 检查剑上是否有特定材料
     */
    @ZenMethod
    public static boolean hasInlay(IItemStack sword, String materialId) {
        return getInlayCount(sword, materialId) > 0;
    }

    /**
     * 检查是否有特定数量的材料
     */
    @ZenMethod
    public static boolean hasInlayCount(IItemStack sword, String materialId, int minCount) {
        return getInlayCount(sword, materialId) >= minCount;
    }

    /**
     * 检查是否有材料组合
     */
    @ZenMethod
    public static boolean hasCombo(IItemStack sword, String material1, int count1,
                                   String material2, int count2) {
        return getInlayCount(sword, material1) >= count1 &&
                getInlayCount(sword, material2) >= count2;
    }

    // ===================================
    // 实用辅助方法
    // ===================================

    /**
     * 获取剑上所有镶嵌的材料ID列表（带槽位信息，用于调试）
     *
     * @param sword 剑物品
     * @return 材料ID数组，例如：["Slot 0: minecraft:diamond", "Slot 2: minecraft:emerald"]
     */
    @ZenMethod
    public static String[] getAllInlays(IItemStack sword) {
        List<String> materials = new ArrayList<>();

        if (sword == null || sword.isEmpty()) return new String[0];

        ItemStack mcStack = CraftTweakerMC.getItemStack(sword);
        if (mcStack.isEmpty() || !mcStack.hasTagCompound()) return new String[0];

        NBTTagCompound tag = mcStack.getTagCompound();
        if (!tag.hasKey("SwordUpgrades")) return new String[0];

        NBTTagCompound upgradeData = tag.getCompoundTag("SwordUpgrades");
        if (!upgradeData.hasKey("Inlays")) return new String[0];

        NBTTagCompound inlaysTag = upgradeData.getCompoundTag("Inlays");

        for (int i = 0; i < 6; i++) {
            String inlayKey = "Inlay_" + i;
            if (inlaysTag.hasKey(inlayKey)) {
                NBTTagCompound inlay = inlaysTag.getCompoundTag(inlayKey);
                if (inlay.hasKey("Material")) {
                    String material = inlay.getString("Material");
                    materials.add("Slot " + i + ": " + material);
                }
            }
        }

        return materials.toArray(new String[0]);
    }

    /**
     * 获取剑上镶嵌的总数量
     *
     * @param sword 剑物品
     * @return 总镶嵌数量（0-5）
     */
    @ZenMethod
    public static int getTotalInlayCount(IItemStack sword) {
        if (sword == null || sword.isEmpty()) return 0;

        ItemStack mcStack = CraftTweakerMC.getItemStack(sword);
        if (mcStack.isEmpty() || !mcStack.hasTagCompound()) return 0;

        NBTTagCompound tag = mcStack.getTagCompound();
        if (!tag.hasKey("SwordUpgrades")) return 0;

        NBTTagCompound upgradeData = tag.getCompoundTag("SwordUpgrades");
        if (!upgradeData.hasKey("Inlays")) return 0;

        NBTTagCompound inlaysTag = upgradeData.getCompoundTag("Inlays");
        int count = 0;

        for (int i = 0; i < 6; i++) {
            String inlayKey = "Inlay_" + i;
            if (inlaysTag.hasKey(inlayKey)) {
                NBTTagCompound inlay = inlaysTag.getCompoundTag(inlayKey);
                if (inlay.hasKey("Material")) {
                    count++;
                }
            }
        }

        return count;
    }

    // ===================================
    // 内部使用的获取方法
    // ===================================

    public static UpgradeMaterial getMaterial(String id) {
        return MATERIALS.get(id);
    }

    public static Map<String, UpgradeMaterial> getAllMaterials() {
        return new HashMap<>(MATERIALS);
    }

    public static List<IUpgradeEffect> getEffects(String id) {
        return EFFECTS.getOrDefault(id, new ArrayList<>());
    }
}