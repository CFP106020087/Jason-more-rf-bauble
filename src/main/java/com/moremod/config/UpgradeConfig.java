package com.moremod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.moremod.compat.crafttweaker.CTSwordUpgrade;
import com.moremod.compat.crafttweaker.UpgradeMaterial;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UpgradeConfig {
    private static Gson GSON;
    private static final Map<String, SwordMaterialData> MATERIAL_DATA = new HashMap<>();
    private static boolean configLoaded = false;

    static {
        try {
            GSON = new GsonBuilder().setPrettyPrinting().create();
        } catch (Exception e) {
            System.out.println("[SwordUpgradeConfig] GsonBuilder failed, using default Gson");
            GSON = new Gson();
        }
    }

    public static void loadConfigs() {
        if (configLoaded) return;

        File configDir = new File(Loader.instance().getConfigDir(), "moremod/sword_upgrades");
        if (!configDir.exists()) {
            configDir.mkdirs();
            createDefaultConfigs(configDir);
        }

        loadJsonConfigs(configDir);
        configLoaded = true;
        System.out.println("[MoreMod] Loaded " + MATERIAL_DATA.size() + " sword upgrade materials");

        // 调试：打印所有已载入的材料
        for (String key : MATERIAL_DATA.keySet()) {
            System.out.println("[MoreMod]   - " + key);
        }
    }

    private static void loadJsonConfigs(File configDir) {
        File[] files = configDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".json");
            }
        });

        if (files != null) {
            for (File file : files) {
                try {
                    SwordMaterialData data = GSON.fromJson(new FileReader(file), SwordMaterialData.class);

                    // 修正：正确处理文件名到物品ID的转换
                    String fileName = file.getName().replace(".json", "");
                    String materialId;

                    // 特殊处理：只替换第一个下划线为冒号
                    int firstUnderscore = fileName.indexOf('_');
                    if (firstUnderscore > 0) {
                        materialId = fileName.substring(0, firstUnderscore) + ":" +
                                fileName.substring(firstUnderscore + 1);
                    } else {
                        materialId = fileName;
                    }

                    if (data != null && data.validate()) {
                        // 如果JSON中没有itemId，使用materialId作为itemId
                        if (data.itemId == null || data.itemId.isEmpty()) {
                            data.itemId = materialId;
                        }
                        
                        // 解析物品对象
                        data.resolveItem();
                        
                        // 同时存储两种格式，确保兼容性
                        MATERIAL_DATA.put(materialId, data);
                        MATERIAL_DATA.put(fileName, data); // 也用原始文件名存储
                        System.out.println("[MoreMod] Loaded material: " + materialId + " (from " + fileName + ".json)");
                    }
                } catch (Exception e) {
                    System.err.println("[MoreMod] Failed to load: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    private static void createDefaultConfigs(File dir) {
        System.out.println("[MoreMod] Creating default sword upgrade configs...");

        // 铁锭
        SwordMaterialData iron = new SwordMaterialData(
                "iron", 2.0f, 0.2f,
                "armor", 1.0,
                "max_health", 2.0
        );
        iron.itemId = "minecraft:iron_ingot";
        iron.removalCost = 3;
        createDefaultConfig(dir, "minecraft_iron_ingot", iron);

        // 金锭
        SwordMaterialData gold = new SwordMaterialData(
                "gold", 1.0f, 0.5f,
                "movement_speed", 0.05,
                "max_health", 1.0
        );
        gold.itemId = "minecraft:gold_ingot";
        gold.removalCost = 2;
        createDefaultConfig(dir, "minecraft_gold_ingot", gold);

        // 钻石
        SwordMaterialData diamond = new SwordMaterialData(
                "diamond", 4.0f, 0.3f,
                "armor", 2.0,
                "armor_toughness", 1.0,
                "max_health", 4.0
        );
        diamond.itemId = "minecraft:diamond";
        diamond.removalCost = 5;
        createDefaultConfig(dir, "minecraft_diamond", diamond);

        // 绿宝石
        SwordMaterialData emerald = new SwordMaterialData(
                "emerald", 3.0f, 0.4f,
                "max_health", 3.0
        );
        emerald.itemId = "minecraft:emerald";
        emerald.removalCost = 4;
        createDefaultConfig(dir, "minecraft_emerald", emerald);

        // 地狱之星
        SwordMaterialData netherStar = new SwordMaterialData(
                "nether_star", 8.0f, 0.5f,
                "max_health", 10.0,
                "armor", 5.0,
                "movement_speed", 0.1,
                "armor_toughness", 2.0,
                "knockback_resistance", 0.1
        );
        netherStar.itemId = "minecraft:nether_star";
        netherStar.removalCost = 10;
        createDefaultConfig(dir, "minecraft_nether_star", netherStar);
    }

    private static void createDefaultConfig(File dir, String fileName, SwordMaterialData data) {
        try {
            File file = new File(dir, fileName + ".json");
            if (!file.exists()) {
                // 在保存前解析物品
                data.resolveItem();
                
                FileWriter writer = new FileWriter(file);
                GSON.toJson(data, writer);
                writer.close();
                System.out.println("[MoreMod] Created default config: " + file.getName());
            }
        } catch (Exception e) {
            System.err.println("[MoreMod] Failed to create config: " + fileName);
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════
    // 支持CraftTweaker材料
    // ═══════════════════════════════════════════════════════

    public static boolean isValidMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;

        String id = stack.getItem().getRegistryName().toString();
        System.out.println("[SwordUpgradeConfig] Checking material: " + id);

        // 首先检查JSON配置
        boolean validInConfig = MATERIAL_DATA.containsKey(id) ||
                MATERIAL_DATA.containsKey(id.replace(":", "_"));

        // 如果JSON配置中没有，检查CraftTweaker注册的材料
        boolean validInCT = false;
        try {
            UpgradeMaterial ctMaterial = CTSwordUpgrade.getMaterial(id);
            validInCT = (ctMaterial != null);

            if (validInCT) {
                System.out.println("[SwordUpgradeConfig] Found in CraftTweaker materials: " + id);
            }
        } catch (Exception e) {
            // CraftTweaker可能还没加载
        }

        boolean valid = validInConfig || validInCT;

        if (!valid) {
            System.out.println("[SwordUpgradeConfig] Material not found. Available materials:");
            for (String key : MATERIAL_DATA.keySet()) {
                System.out.println("  - " + key);
            }

            try {
                Map<String, UpgradeMaterial> ctMaterials = CTSwordUpgrade.getAllMaterials();
                for (String key : ctMaterials.keySet()) {
                    System.out.println("  - " + key + " (CraftTweaker)");
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        return valid;
    }

    /**
     * 从ItemStack获取材料ID
     * ⚠️ 这个方法是TileEntity需要的！
     */
    public static String getMaterialId(ItemStack stack) {
        if (stack.isEmpty()) return null;
        
        // 获取物品的注册名（格式：modid:itemname）
        String registryName = stack.getItem().getRegistryName().toString();
        
        // 检查是否有meta值
        if (stack.getMetadata() != 0) {
            // 如果有meta，加上meta值（格式：modid:itemname:meta）
            return registryName + ":" + stack.getMetadata();
        }
        
        return registryName;
    }

    public static SwordMaterialData getMaterialData(String materialId) {
        SwordMaterialData data = MATERIAL_DATA.get(materialId);
        if (data == null) {
            data = MATERIAL_DATA.get(materialId.replace(":", "_"));
        }

        // 如果JSON中没有，尝试从CraftTweaker获取
        if (data == null) {
            try {
                UpgradeMaterial ctMaterial = CTSwordUpgrade.getMaterial(materialId);
                if (ctMaterial != null) {
                    // 使用静态方法转换
                    data = SwordMaterialData.fromCraftTweaker(ctMaterial);
                    System.out.println("[SwordUpgradeConfig] Converted CraftTweaker material: " + materialId);
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        return data;
    }

    public static Map<String, SwordMaterialData> getAllMaterials() {
        Map<String, SwordMaterialData> allMaterials = new HashMap<>(MATERIAL_DATA);

        // 合并CraftTweaker材料
        try {
            Map<String, UpgradeMaterial> ctMaterials = CTSwordUpgrade.getAllMaterials();
            for (Map.Entry<String, UpgradeMaterial> entry : ctMaterials.entrySet()) {
                if (!allMaterials.containsKey(entry.getKey())) {
                    SwordMaterialData data = SwordMaterialData.fromCraftTweaker(entry.getValue());
                    allMaterials.put(entry.getKey(), data);
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        return allMaterials;
    }

    public static void reloadConfigs() {
        MATERIAL_DATA.clear();
        configLoaded = false;
        loadConfigs();
    }
}