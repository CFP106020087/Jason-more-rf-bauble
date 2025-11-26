package com.moremod.ritual;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.ArrayList;
import java.util.List;

/**
 * 从 jar 资源加载配方：
 * 扫描 assets/moremod/rituals/ 下所有 .json 并加载为 RitualInfusionRecipe。
 * 不使用 config/ 目录，因此玩家无法修改。
 */
public class RitualRecipeLoader {

    private static final String ROOT_DIR = "assets/moremod/rituals/";

    /** 在 init 或 postInit 调用 */
    public static void loadRecipes() {
        int loaded = 0;
        try {
            ClassLoader cl = RitualRecipeLoader.class.getClassLoader();
            // 1) 尝试列举根目录（兼容 dev 的文件协议）
            Enumeration<URL> roots = cl.getResources(ROOT_DIR);
            boolean anyRoot = false;
            while (roots.hasMoreElements()) {
                anyRoot = true;
                URL root = roots.nextElement();
                String protocol = root.getProtocol();
                if ("file".equals(protocol)) {
                    loaded += loadFromDirectory(root);
                } else if ("jar".equals(protocol)) {
                    loaded += loadFromJar(root);
                } else {
                    System.err.println("[RITUAL] Unsupported protocol: " + protocol + " for URL: " + root);
                }
            }
            // 2) 某些打包方式 getResources(ROOT_DIR) 可能拿不到目录；退化为扫 jar 全表
            if (!anyRoot) {
                URL self = RitualRecipeLoader.class.getProtectionDomain().getCodeSource().getLocation();
                if (self != null && self.getPath().endsWith(".jar")) {
                    loaded += scanWholeJar(self);
                } else {
                    // 开发环境下再尝试以 file 方式扫描
                    URL asFile = cl.getResource(ROOT_DIR);
                    if (asFile != null && "file".equals(asFile.getProtocol())) {
                        loaded += loadFromDirectory(asFile);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[RITUAL] Unexpected error when loading recipes");
            e.printStackTrace();
        }
        System.out.println("[RITUAL] total internal recipes loaded = " + loaded);
    }

    // ========= 扫描实现 =========

    /** 从 resources 目录（dev 环境）加载 */
    private static int loadFromDirectory(URL dirUrl) {
        int count = 0;
        try {
            URI uri = dirUrl.toURI();
            File dir = new File(uri);
            if (!dir.exists() || !dir.isDirectory()) return 0;
            File[] files = dir.listFiles((f, name) -> name.toLowerCase().endsWith(".json"));
            if (files == null) return 0;
            for (File f : files) {
                String name = f.getName();
                try (InputStream in = new FileInputStream(f)) {
                    if (loadOneJson(name, in)) count++;
                } catch (Exception ex) {
                    System.err.println("[RITUAL] Failed to load " + name + " from dir: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("[RITUAL] loadFromDirectory error: " + e.getMessage());
        }
        return count;
    }

    /** 从 jar 内目录加载（JarURLConnection） */
    private static int loadFromJar(URL jarDirUrl) {
        int count = 0;
        try {
            JarURLConnection conn = (JarURLConnection) jarDirUrl.openConnection();
            try (JarFile jar = conn.getJarFile()) {
                count += scanJarEntries(jar, ROOT_DIR);
            }
        } catch (Exception e) {
            System.err.println("[RITUAL] loadFromJar error: " + e.getMessage());
        }
        return count;
    }

    /** 兜底：扫描整个当前 jar（当目录资源不可枚举时） */
    private static int scanWholeJar(URL selfJarUrl) {
        int count = 0;
        try {
            String path = URLDecoder.decode(selfJarUrl.getPath(), "UTF-8");
            File jarFile = new File(path);
            if (jarFile.isFile() && jarFile.getName().endsWith(".jar")) {
                try (JarFile jar = new JarFile(jarFile)) {
                    count += scanJarEntries(jar, ROOT_DIR);
                }
            }
        } catch (Exception e) {
            System.err.println("[RITUAL] scanWholeJar error: " + e.getMessage());
        }
        return count;
    }

    /** 遍历 jar 条目，加载 ROOT_DIR 下的 .json */
    private static int scanJarEntries(JarFile jar, String root) {
        int count = 0;
        Enumeration<JarEntry> en = jar.entries();
        while (en.hasMoreElements()) {
            JarEntry entry = en.nextElement();
            String name = entry.getName();
            if (entry.isDirectory()) continue;
            if (!name.startsWith(root)) continue;
            if (!name.toLowerCase().endsWith(".json")) continue;

            try (InputStream in = RitualRecipeLoader.class.getClassLoader().getResourceAsStream(name)) {
                if (in == null) continue;
                String fileName = name.substring(name.lastIndexOf('/') + 1);
                if (loadOneJson(fileName, in)) count++;
            } catch (Exception ex) {
                System.err.println("[RITUAL] Failed to load " + name + " from jar: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        return count;
    }

    // ========= 单文件解析 =========

    private static boolean loadOneJson(String fileName, InputStream in) {
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            RitualInfusionRecipe recipe = parseRecipe(json);
            if (recipe != null) {
                RitualInfusionAPI.RITUAL_RECIPES.add(recipe);
                System.out.println("[RITUAL] Loaded internal recipe: " + fileName);
                return true;
            }
        } catch (Exception e) {
            System.err.println("[RITUAL] Parse error in " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ========= 你的原有解析逻辑（保持一致） =========

    private static RitualInfusionRecipe parseRecipe(JsonObject json) {
        try {
            Ingredient core = parseIngredient(json.getAsJsonObject("core"));

            List<Ingredient> pedestalItems = new ArrayList<>();
            JsonArray pedestals = json.getAsJsonArray("pedestals");
            for (int i = 0; i < pedestals.size(); i++) {
                pedestalItems.add(parseIngredient(pedestals.get(i).getAsJsonObject()));
            }

            ItemStack output = parseItemStack(json.getAsJsonObject("output"));

            int time = json.get("time").getAsInt();
            int energy = json.get("energy").getAsInt();
            float failChance = json.has("fail_chance") ? json.get("fail_chance").getAsFloat() : 0.0F;

            return new RitualInfusionRecipe(core, pedestalItems, output, time, energy, failChance);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Ingredient parseIngredient(JsonObject json) {
        String itemName = json.get("item").getAsString();
        Item item = Item.getByNameOrId(itemName);
        if (item == null) return Ingredient.EMPTY;

        int meta = json.has("meta") ? json.get("meta").getAsInt() : 0;
        int count = json.has("count") ? json.get("count").getAsInt() : 1;

        return Ingredient.fromStacks(new ItemStack(item, count, meta));
    }

    private static ItemStack parseItemStack(JsonObject json) {
        String itemName = json.get("item").getAsString();
        Item item = Item.getByNameOrId(itemName);
        if (item == null) return ItemStack.EMPTY;

        int meta = json.has("meta") ? json.get("meta").getAsInt() : 0;
        int count = json.has("count") ? json.get("count").getAsInt() : 1;

        return new ItemStack(item, count, meta);
    }
}
