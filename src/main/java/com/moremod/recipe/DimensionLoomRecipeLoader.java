package com.moremod.recipe;

import com.google.gson.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class DimensionLoomRecipeLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String RECIPE_DIR = "dimension_loom_recipes";

    /**
     * 加载所有配方
     */
    public static void loadRecipes() {
        System.out.println("[MoreMod] Loading Dimension Loom recipes...");

        // 清空现有配方
        DimensionLoomRecipes.clearRecipes();

        // 1. 从模组JAR内加载默认配方
        loadDefaultRecipes();

        // 2. 从config文件夹加载自定义配方
        loadCustomRecipes();

        System.out.println("[MoreMod] Loaded " + DimensionLoomRecipes.getRecipeCount() + " Dimension Loom recipes");
    }

    /**
     * 从模组资源文件加载默认配方
     */
    private static void loadDefaultRecipes() {
        try {
            // 尝试加载配方列表
            InputStream listStream = DimensionLoomRecipeLoader.class.getResourceAsStream("/assets/moremod/recipes/dimension_loom/recipe_list.json");
            if (listStream != null) {
                String content = IOUtils.toString(listStream, StandardCharsets.UTF_8);
                JsonObject listJson = GSON.fromJson(content, JsonObject.class);
                JsonArray recipes = listJson.getAsJsonArray("recipes");

                for (JsonElement element : recipes) {
                    String recipeName = element.getAsString();
                    loadRecipeFromResource("/assets/moremod/recipes/dimension_loom/" + recipeName + ".json");
                }
                listStream.close();
            } else {
                System.out.println("[MoreMod] No default recipes found (this is normal if you haven't added any)");
            }
        } catch (Exception e) {
            System.out.println("[MoreMod] No default recipes to load: " + e.getMessage());
        }
    }

    /**
     * 从资源文件加载单个配方
     */
    private static void loadRecipeFromResource(String resourcePath) {
        try {
            InputStream stream = DimensionLoomRecipeLoader.class.getResourceAsStream(resourcePath);
            if (stream != null) {
                String content = IOUtils.toString(stream, StandardCharsets.UTF_8);
                parseAndAddRecipe(content, resourcePath);
                stream.close();
            }
        } catch (Exception e) {
            System.err.println("[MoreMod] Error loading recipe from " + resourcePath + ": " + e.getMessage());
        }
    }

    /**
     * 从config文件夹加载自定义配方
     */
    private static void loadCustomRecipes() {
        File configDir = new File(Loader.instance().getConfigDir(), "moremod/" + RECIPE_DIR);

        // 如果文件夹不存在，创建它
        if (!configDir.exists()) {
            configDir.mkdirs();
            System.out.println("[MoreMod] Created recipe directory: " + configDir.getAbsolutePath());
            createExampleRecipe(configDir);
        }

        // 加载所有JSON文件
        File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null && files.length > 0) {
            for (File file : files) {
                loadRecipeFromFile(file);
            }
        } else {
            System.out.println("[MoreMod] No custom recipes found in " + configDir.getAbsolutePath());
        }
    }

    /**
     * 从文件加载配方
     */
    private static void loadRecipeFromFile(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            parseAndAddRecipe(content, file.getName());
        } catch (Exception e) {
            System.err.println("[MoreMod] Error loading recipe from " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * 解析JSON并添加配方
     */
    private static void parseAndAddRecipe(String jsonContent, String source) {
        try {
            JsonObject json = GSON.fromJson(jsonContent, JsonObject.class);

            // 检查是否启用
            if (json.has("enabled") && !json.get("enabled").getAsBoolean()) {
                return;
            }

            // 解析输出
            JsonObject outputJson = json.getAsJsonObject("output");
            ItemStack output = parseItemStack(outputJson);

            if (output.isEmpty()) {
                System.err.println("[MoreMod] Recipe " + source + " has invalid output!");
                return;
            }

            // 解析输入（3x3）
            JsonArray inputArray = json.getAsJsonArray("input");
            if (inputArray.size() != 9) {
                System.err.println("[MoreMod] Recipe " + source + " must have exactly 9 inputs!");
                return;
            }

            ItemStack[] inputs = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                JsonElement element = inputArray.get(i);
                if (element.isJsonObject()) {
                    inputs[i] = parseItemStack(element.getAsJsonObject());
                } else if (element.isJsonNull() ||
                        (element.isJsonPrimitive() && element.getAsString().equals("empty"))) {
                    inputs[i] = ItemStack.EMPTY;
                } else {
                    // 简化格式：直接使用物品ID
                    Item item = getItem(element.getAsString());
                    inputs[i] = item != null ? new ItemStack(item, 1) : ItemStack.EMPTY;
                }
            }

            // 添加配方
            DimensionLoomRecipes.addRecipe(output, inputs);

            // 记录配方信息
            if (json.has("description")) {
                System.out.println("[MoreMod] Loaded recipe: " + json.get("description").getAsString());
            } else {
                System.out.println("[MoreMod] Loaded recipe from " + source);
            }

        } catch (Exception e) {
            System.err.println("[MoreMod] Failed to parse recipe " + source + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 解析ItemStack
     */
    private static ItemStack parseItemStack(JsonObject json) {
        String itemName = JsonUtils.getString(json, "item");
        int count = JsonUtils.getInt(json, "count", 1);
        int meta = JsonUtils.getInt(json, "data", 0);

        Item item = getItem(itemName);
        if (item == null) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item, count, meta);
    }

    /**
     * 根据名称获取物品
     */
    private static Item getItem(String name) {
        ResourceLocation location = new ResourceLocation(name);
        Item item = ForgeRegistries.ITEMS.getValue(location);

        if (item == null) {
            System.err.println("[MoreMod] Unknown item: " + name);
        }

        return item;
    }

    /**
     * 创建一个示例配方文件
     */
    private static void createExampleRecipe(File dir) {
        JsonObject example = new JsonObject();
        example.addProperty("enabled", true);
        example.addProperty("description", "Example: 9 Iron Ingots to Iron Block");

        JsonObject output = new JsonObject();
        output.addProperty("item", "minecraft:iron_block");
        output.addProperty("count", 1);
        example.add("output", output);

        JsonArray input = new JsonArray();
        for (int i = 0; i < 9; i++) {
            input.add("minecraft:iron_ingot");
        }
        example.add("input", input);

        try {
            File file = new File(dir, "example_recipe.json");
            FileWriter writer = new FileWriter(file);
            GSON.toJson(example, writer);
            writer.close();
            System.out.println("[MoreMod] Created example recipe: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[MoreMod] Failed to create example recipe: " + e.getMessage());
        }
    }
}