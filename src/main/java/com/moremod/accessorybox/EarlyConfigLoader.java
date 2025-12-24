package com.moremod.accessorybox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * 早期配置加载器 - 智能版
 * 自动尝试多个配置文件位置
 */
public class EarlyConfigLoader {

    private static boolean loaded = false;

    // 配置值 - 饰品槽位
    public static boolean enableExtraSlots = true;
    public static int extraAmulets = 0;
    public static int extraRings = 0;
    public static int extraBelts = 0;
    public static int extraHeads = 0;
    public static int extraBodies = 0;
    public static int extraCharms = 0;
    public static int extraTrinkets = 0;

    // 配置值 - 赞助者武器
    public static boolean enableSponsorWeapons = true;  // 总开关
    public static boolean enableZhuxianSword = true;    // 诛仙剑开关

    /**
     * 早期加载配置
     */
    public static void loadEarly() {
        if (loaded) {
            return;
        }

        System.out.println("[EarlyConfigLoader] ========================================");
        System.out.println("[EarlyConfigLoader] 早期加载配置文件...");

        try {
            // 尝试多个配置文件位置
            File configFile = null;

            // 位置 1: config/moremod/extra_baubles.cfg (Forge @Config 指定的)
            File file1 = new File("config/moremod/extra_baubles.cfg");
            if (file1.exists()) {
                configFile = file1;
                System.out.println("[EarlyConfigLoader] 找到配置: config/moremod/extra_baubles.cfg");
            }

            // 位置 2: config/moremod.cfg (主配置文件)
            if (configFile == null) {
                File file2 = new File("config/moremod.cfg");
                if (file2.exists()) {
                    configFile = file2;
                    System.out.println("[EarlyConfigLoader] 找到配置: config/moremod.cfg");
                }
            }

            if (configFile == null) {
                System.out.println("[EarlyConfigLoader] 配置文件不存在，使用默认值");
            } else {
                // 读取主配置
                readConfig(configFile);
            }

            // ========== 读取赞助物品配置 ==========
            File sponsorConfigFile = new File("config/moremod/sponsor_items.cfg");
            if (sponsorConfigFile.exists()) {
                System.out.println("[EarlyConfigLoader] 找到赞助物品配置: config/moremod/sponsor_items.cfg");
                readSponsorConfig(sponsorConfigFile);
            } else {
                System.out.println("[EarlyConfigLoader] 赞助物品配置文件不存在，使用默认值");
            }

            printConfig();
            loaded = true;

        } catch (Exception e) {
            System.err.println("[EarlyConfigLoader] 配置加载失败，使用默认值");
            e.printStackTrace();
            loaded = true;
        }
    }

    /**
     * 读取赞助物品配置文件 (sponsor_items.cfg)
     */
    private static void readSponsorConfig(File file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String currentSection = "";

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 跳过注释和空行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // 检测区块开始
                if (line.endsWith("{")) {
                    // 提取区块名称，如 "general {" -> "general"
                    currentSection = line.replace("{", "").replace("\"", "").trim();
                    continue;
                }

                // 检测区块结束
                if (line.equals("}")) {
                    currentSection = "";
                    continue;
                }

                // 解析配置项
                // 格式: B:enabled=false 或 B:"enabled"=false
                if (currentSection.equals("general") && line.contains("enabled")) {
                    // 主开关
                    enableSponsorWeapons = parseBoolean(line);
                    System.out.println("[EarlyConfigLoader] 解析赞助物品: enabled = " + enableSponsorWeapons);
                }
                else if (currentSection.equals("weapons") && line.contains("enabled")) {
                    // 武器开关 - 用于更细粒度控制
                    boolean weaponsEnabled = parseBoolean(line);
                    // 如果主开关关闭，武器也关闭
                    if (!enableSponsorWeapons) {
                        weaponsEnabled = false;
                    }
                    // 诛仙剑跟随武器开关
                    enableZhuxianSword = weaponsEnabled;
                    System.out.println("[EarlyConfigLoader] 解析赞助物品: weapons.enabled = " + weaponsEnabled);
                }
            }
        }
    }

    /**
     * 读取 Forge 配置文件
     */
    private static void readConfig(File file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean inGeneralSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 检测区块
                if (line.equals("general {") || line.equals("\"general\" {")) {
                    inGeneralSection = true;
                    System.out.println("[EarlyConfigLoader] 进入 general 区块");
                    continue;
                }
                if (line.equals("}") && inGeneralSection) {
                    inGeneralSection = false;
                    System.out.println("[EarlyConfigLoader] 离开 general 区块");
                    continue;
                }

                // 跳过注释和空行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // ⭐ 解析配置行（支持多种格式）
                // 格式 1: I:"额外戒指 | Extra Rings"=3
                // 格式 2: I:extraRings=3
                // 格式 3: B:"启用系统 | Enable System"=true

                if (line.contains("启用系统") || line.contains("Enable System") || line.contains("enableExtraSlots")) {
                    enableExtraSlots = parseBoolean(line);
                    System.out.println("[EarlyConfigLoader] 解析: enableExtraSlots = " + enableExtraSlots);
                }
                else if (line.contains("额外项链") || line.contains("Extra Amulets") || line.contains("extraAmulets")) {
                    extraAmulets = parseInt(line);
                    System.out.println("[EarlyConfigLoader] 解析: extraAmulets = " + extraAmulets);
                }
                else if (line.contains("额外戒指") || line.contains("Extra Rings") || line.contains("extraRings")) {
                    extraRings = parseInt(line);
                    System.out.println("[EarlyConfigLoader] 解析: extraRings = " + extraRings);
                }
                else if (line.contains("额外腰带") || line.contains("Extra Belts") || line.contains("extraBelts")) {
                    extraBelts = parseInt(line);
                    System.out.println("[EarlyConfigLoader] 解析: extraBelts = " + extraBelts);
                }
                else if (line.contains("额外头部") || line.contains("Extra Heads") || line.contains("extraHeads")) {
                    extraHeads = parseInt(line);
                    System.out.println("[EarlyConfigLoader] 解析: extraHeads = " + extraHeads);
                }
                else if (line.contains("额外身体") || line.contains("Extra Bodies") || line.contains("extraBodies")) {
                    extraBodies = parseInt(line);
                    System.out.println("[EarlyConfigLoader] 解析: extraBodies = " + extraBodies);
                }
                else if (line.contains("额外挂饰") || line.contains("Extra Charms") || line.contains("extraCharms")) {
                    extraCharms = parseInt(line);
                    System.out.println("[EarlyConfigLoader] 解析: extraCharms = " + extraCharms);
                }
                else if (line.contains("额外万能") || line.contains("Extra Trinkets") || line.contains("extraTrinkets")) {
                    extraTrinkets = parseInt(line);
                    System.out.println("[EarlyConfigLoader] 解析: extraTrinkets = " + extraTrinkets);
                }
                // ========== 赞助者武器配置 ==========
                else if (line.contains("赞助者武器") || line.contains("Sponsor Weapons") || line.contains("enableSponsorWeapons")) {
                    enableSponsorWeapons = parseBoolean(line);
                    System.out.println("[EarlyConfigLoader] 解析: enableSponsorWeapons = " + enableSponsorWeapons);
                }
                else if (line.contains("诛仙剑") || line.contains("Zhuxian Sword") || line.contains("enableZhuxianSword")) {
                    enableZhuxianSword = parseBoolean(line);
                    System.out.println("[EarlyConfigLoader] 解析: enableZhuxianSword = " + enableZhuxianSword);
                }
            }
        }

        // 验证配置
        int total = getTotalExtraSlots();
        if (total > 42) {
            System.err.println("[EarlyConfigLoader] 警告: 总槽位数 " + total + " 超过限制 42");
        }
    }

    /**
     * 解析布尔值
     */
    private static boolean parseBoolean(String line) {
        int equalPos = line.indexOf('=');
        if (equalPos == -1) return true;
        String value = line.substring(equalPos + 1).trim();
        return Boolean.parseBoolean(value);
    }

    /**
     * 解析整数
     */
    private static int parseInt(String line) {
        int equalPos = line.indexOf('=');
        if (equalPos == -1) return 0;
        String value = line.substring(equalPos + 1).trim();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 打印配置信息
     */
    private static void printConfig() {
        System.out.println("[EarlyConfigLoader] 配置加载成功:");
        System.out.println("[EarlyConfigLoader]   启用: " + enableExtraSlots);
        System.out.println("[EarlyConfigLoader]   项链: " + extraAmulets);
        System.out.println("[EarlyConfigLoader]   戒指: " + extraRings);
        System.out.println("[EarlyConfigLoader]   腰带: " + extraBelts);
        System.out.println("[EarlyConfigLoader]   头部: " + extraHeads);
        System.out.println("[EarlyConfigLoader]   身体: " + extraBodies);
        System.out.println("[EarlyConfigLoader]   挂饰: " + extraCharms);
        System.out.println("[EarlyConfigLoader]   万能: " + extraTrinkets);
        System.out.println("[EarlyConfigLoader]   总计: " + getTotalExtraSlots());
        System.out.println("[EarlyConfigLoader] --- 赞助者武器 ---");
        System.out.println("[EarlyConfigLoader]   赞助者武器总开关: " + enableSponsorWeapons);
        System.out.println("[EarlyConfigLoader]   诛仙剑: " + enableZhuxianSword);
        System.out.println("[EarlyConfigLoader] ========================================");
    }

    /**
     * 检查诛仙剑是否启用（总开关 && 单独开关）
     */
    public static boolean isZhuxianSwordEnabled() {
        return enableSponsorWeapons && enableZhuxianSword;
    }

    /**
     * 获取总额外槽位数
     */
    public static int getTotalExtraSlots() {
        if (!enableExtraSlots) return 0;
        return extraAmulets + extraRings + extraBelts +
                extraHeads + extraBodies + extraCharms + extraTrinkets;
    }

    /**
     * 获取总槽位数（含原版）
     */
    public static int getTotalSlots() {
        return 7 + getTotalExtraSlots();
    }
}