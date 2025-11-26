package com.moremod.compat.crafttweaker;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 词条池注册表 (完整版)
 * 补充了所有CTGemAffixes需要的方法
 */
public class AffixPoolRegistry {

    private static final Map<String, GemAffix> AFFIX_REGISTRY = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static boolean debugMode = false;

    // ==========================================
    // 基础方法
    // ==========================================

    /**
     * 注册词条
     */
    public static void registerAffix(GemAffix affix) {
        if (affix == null || affix.getId() == null) {
            throw new IllegalArgumentException("Invalid affix or affix ID");
        }

        AFFIX_REGISTRY.put(affix.getId(), affix);

        if (debugMode) {
            System.out.println("[AffixPool] 注册词条: " + affix.getId());
        }
    }

    /**
     * 获取词条
     */
    public static GemAffix getAffix(String id) {
        return AFFIX_REGISTRY.get(id);
    }

    /**
     * 移除词条
     */
    public static boolean removeAffix(String id) {
        return AFFIX_REGISTRY.remove(id) != null;
    }

    /**
     * 清空所有词条
     */
    public static void clearAll() {
        AFFIX_REGISTRY.clear();
    }

    // ==========================================
    // 查询方法
    // ==========================================

    /**
     * 获取词条数量
     */
    public static int getAffixCount() {
        return AFFIX_REGISTRY.size();
    }

    /**
     * 获取所有词条ID
     */
    public static List<String> getAllAffixIds() {
        return new ArrayList<>(AFFIX_REGISTRY.keySet());
    }

    /**
     * 获取所有词条
     */
    public static List<GemAffix> getAllAffixes() {
        return new ArrayList<>(AFFIX_REGISTRY.values());
    }

    /**
     * 按类型获取词条
     */
    public static List<GemAffix> getAffixesByType(GemAffix.AffixType type) {
        return AFFIX_REGISTRY.values().stream()
                .filter(affix -> affix.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * 获取总权重
     */
    public static int getTotalWeight() {
        return AFFIX_REGISTRY.values().stream()
                .filter(GemAffix::isEnabled)
                .mapToInt(GemAffix::getWeight)
                .sum();
    }

    /**
     * 获取符合等级要求的词条
     */
    public static List<GemAffix> getAffixesByLevel(int playerLevel) {
        return AFFIX_REGISTRY.values().stream()
                .filter(GemAffix::isEnabled)
                .filter(affix -> affix.getLevelRequirement() <= playerLevel)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 批量操作方法
    // ==========================================

    /**
     * 按类型移除词条
     */
    public static int removeAffixesByType(GemAffix.AffixType type) {
        List<String> toRemove = AFFIX_REGISTRY.entrySet().stream()
                .filter(entry -> entry.getValue().getType() == type)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        toRemove.forEach(AFFIX_REGISTRY::remove);
        return toRemove.size();
    }

    /**
     * 批量启用/禁用词条
     */
    public static void setAllEnabled(boolean enabled) {
        AFFIX_REGISTRY.values().forEach(affix -> affix.setEnabled(enabled));
    }

    /**
     * 按类型启用/禁用词条
     */
    public static void setTypeEnabled(GemAffix.AffixType type, boolean enabled) {
        AFFIX_REGISTRY.values().stream()
                .filter(affix -> affix.getType() == type)
                .forEach(affix -> affix.setEnabled(enabled));
    }

    // ==========================================
    // 抽取方法
    // ==========================================

    /**
     * 随机抽取词条
     */
    public static GemAffix rollAffix(int playerLevel, Random random) {
        List<GemAffix> available = getAffixesByLevel(playerLevel);
        if (available.isEmpty()) {
            return null;
        }

        int totalWeight = available.stream()
                .mapToInt(GemAffix::getWeight)
                .sum();

        int roll = random.nextInt(totalWeight);
        int current = 0;

        for (GemAffix affix : available) {
            current += affix.getWeight();
            if (roll < current) {
                return affix;
            }
        }

        return available.get(available.size() - 1);
    }

    /**
     * 抽取多个词条 (不重复)
     */
    public static List<GemAffix> rollMultiple(int count, int playerLevel, Random random) {
        List<GemAffix> available = new ArrayList<>(getAffixesByLevel(playerLevel));
        List<GemAffix> result = new ArrayList<>();

        for (int i = 0; i < count && !available.isEmpty(); i++) {
            int totalWeight = available.stream()
                    .mapToInt(GemAffix::getWeight)
                    .sum();

            if (totalWeight <= 0) break;

            int roll = random.nextInt(totalWeight);
            int current = 0;

            GemAffix selected = null;
            for (GemAffix affix : available) {
                current += affix.getWeight();
                if (roll < current) {
                    selected = affix;
                    break;
                }
            }

            if (selected != null) {
                result.add(selected);
                available.remove(selected);
            }
        }

        return result;
    }

    /**
     * ✅ 新增方法：抽取并实例化词条（用于宝石鉴定）
     * 
     * 与 rollMultiple 的区别：
     * - rollMultiple 返回 List<GemAffix> (词条定义)
     * - rollAffixes 返回 List<IdentifiedAffix> (已实例化，包含随机数值)
     * 
     * @param count 词条数量
     * @param gemLevel 宝石等级（用于过滤词条池）
     * @return 已鉴定的词条列表
     */
    public static List<IdentifiedAffix> rollAffixes(int count, int gemLevel) {
        return rollAffixes(count, gemLevel, RANDOM);
    }

    /**
     * ✅ 抽取并实例化词条（支持自定义Random）
     */
    public static List<IdentifiedAffix> rollAffixes(int count, int gemLevel, Random random) {
        List<IdentifiedAffix> result = new ArrayList<>();
        
        // 使用现有的 rollMultiple 获取不重复的词条定义
        List<GemAffix> rolledAffixes = rollMultiple(count, gemLevel, random);
        
        // 为每个词条生成随机数值并实例化
        for (GemAffix affix : rolledAffixes) {
            float rolledValue = affix.rollValue(); // 使用 GemAffix 自带的 rollValue()
            IdentifiedAffix identified = new IdentifiedAffix(affix, rolledValue);
            result.add(identified);
            
            if (debugMode) {
                System.out.println(String.format(
                    "[AffixPool] 鉴定词条: %s = %.2f (品质: %d%%)",
                    affix.getId(),
                    rolledValue,
                    identified.getQuality()
                ));
            }
        }
        
        return result;
    }

    // ==========================================
    // 导入/导出方法
    // ==========================================

    /**
     * 导出到文件
     */
    public static void exportToFile(String filename) throws IOException {
        File file = new File(filename);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("# 宝石词条配置\n");
            writer.write("# 格式: ID|类型|显示名|最小值|最大值|权重|等级要求|参数\n\n");

            for (GemAffix affix : AFFIX_REGISTRY.values()) {
                StringBuilder line = new StringBuilder();
                line.append(affix.getId()).append("|");
                line.append(affix.getType().name()).append("|");
                line.append(affix.getDisplayName()).append("|");
                line.append(affix.getMinValue()).append("|");
                line.append(affix.getMaxValue()).append("|");
                line.append(affix.getWeight()).append("|");
                line.append(affix.getLevelRequirement()).append("|");

                // 添加参数
                if (affix.hasParameter("damageType")) {
                    line.append("damageType=").append(affix.getParameter("damageType"));
                }
                if (affix.hasParameter("effectType")) {
                    line.append("effectType=").append(affix.getParameter("effectType"));
                }
                if (affix.hasParameter("attribute")) {
                    line.append("attribute=").append(affix.getParameter("attribute"));
                }

                writer.write(line.toString());
                writer.newLine();
            }
        }
    }

    /**
     * 从文件导入
     */
    public static void importFromFile(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new FileNotFoundException("配置文件不存在: " + filename);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();

                // 跳过注释和空行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                try {
                    String[] parts = line.split("\\|");
                    if (parts.length < 7) {
                        System.err.println("行 " + lineNum + " 格式错误: " + line);
                        continue;
                    }

                    String id = parts[0];
                    GemAffix.AffixType type = GemAffix.AffixType.valueOf(parts[1]);
                    String displayName = parts[2];
                    float minValue = Float.parseFloat(parts[3]);
                    float maxValue = Float.parseFloat(parts[4]);
                    int weight = Integer.parseInt(parts[5]);
                    int levelReq = Integer.parseInt(parts[6]);

                    GemAffix affix = new GemAffix(id)
                            .setDisplayName(displayName)
                            .setType(type)
                            .setValueRange(minValue, maxValue)
                            .setWeight(weight)
                            .setLevelRequirement(levelReq);

                    // 解析参数
                    if (parts.length > 7) {
                        String[] params = parts[7].split(",");
                        for (String param : params) {
                            String[] kv = param.split("=");
                            if (kv.length == 2) {
                                affix.setParameter(kv[0].trim(), kv[1].trim());
                            }
                        }
                    }

                    registerAffix(affix);

                } catch (Exception e) {
                    System.err.println("行 " + lineNum + " 解析失败: " + e.getMessage());
                }
            }
        }
    }

    // ==========================================
    // 调试方法
    // ==========================================

    /**
     * 启用调试模式
     */
    public static void setDebugMode(boolean enable) {
        debugMode = enable;
    }

    /**
     * 检查是否启用调试模式
     */
    public static boolean isDebugMode() {
        return debugMode;
    }

    /**
     * 调试输出所有词条
     */
    public static void debugPrintAll() {
        System.out.println("========================================");
        System.out.println("  词条池调试信息");
        System.out.println("========================================");
        System.out.println("  总词条数: " + AFFIX_REGISTRY.size());
        System.out.println("  总权重: " + getTotalWeight());
        System.out.println("========================================");

        // 按类型分组
        Map<GemAffix.AffixType, List<GemAffix>> byType = AFFIX_REGISTRY.values().stream()
                .collect(Collectors.groupingBy(GemAffix::getType));

        for (Map.Entry<GemAffix.AffixType, List<GemAffix>> entry : byType.entrySet()) {
            System.out.println("\n  " + entry.getKey().name() + " (" + entry.getValue().size() + "个):");
            for (GemAffix affix : entry.getValue()) {
                System.out.println(String.format(
                        "    %s: 权重=%d, 等级=%d, 范围=%.2f-%.2f, 启用=%s",
                        affix.getId(),
                        affix.getWeight(),
                        affix.getLevelRequirement(),
                        affix.getMinValue(),
                        affix.getMaxValue(),
                        affix.isEnabled()
                ));
            }
        }

        System.out.println("========================================");
    }

    /**
     * 重新加载
     */
    public static void reload() {
        // 清空并重新加载(通常配合配置文件使用)
        clearAll();
        System.out.println("[AffixPool] 词条池已重新加载");
    }

    /**
     * 验证词条池
     */
    public static boolean validate() {
        boolean valid = true;

        for (GemAffix affix : AFFIX_REGISTRY.values()) {
            // 检查ID
            if (affix.getId() == null || affix.getId().isEmpty()) {
                System.err.println("词条ID为空!");
                valid = false;
            }

            // 检查数值范围
            if (affix.getMinValue() > affix.getMaxValue()) {
                System.err.println("词条 " + affix.getId() + " 数值范围无效!");
                valid = false;
            }

            // 检查权重
            if (affix.getWeight() <= 0) {
                System.err.println("词条 " + affix.getId() + " 权重无效!");
                valid = false;
            }

            // 检查等级要求
            if (affix.getLevelRequirement() < 0) {
                System.err.println("词条 " + affix.getId() + " 等级要求无效!");
                valid = false;
            }
        }

        return valid;
    }
}
