package com.moremod.integration.jei.ritual;

import com.moremod.init.ModItems;
import com.moremod.moremod;
import com.moremod.ritual.LegacyRitualConfig;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 特殊仪式信息类 - 用于JEI展示
 * 包含所有特殊仪式的定义
 *
 * 支持 CraftTweaker 动态配置：
 * - 从 LegacyRitualConfig 读取时间、能量、失败率、材料
 * - 被禁用的仪式不会显示在 JEI 中
 */
public class SpecialRitualInfo {

    public enum RitualType {
        ENCHANT_INFUSION("注魔仪式", LegacyRitualConfig.ENCHANT_INFUSION, "将多本附魔书的附魔叠加到物品上"),
        CURSE_PURIFICATION("诅咒净化", LegacyRitualConfig.CURSE_PURIFICATION, "移除物品上的诅咒附魔"),
        ENCHANT_TRANSFER("附魔转移", LegacyRitualConfig.ENCHANT_TRANSFER, "将物品的附魔转移到另一物品"),
        CURSE_CREATION("诅咒创造", LegacyRitualConfig.CURSE_CREATION, "创造诅咒卷轴"),
        WEAPON_EXP_BOOST("武器经验加速", LegacyRitualConfig.WEAPON_EXP_BOOST, "加速武器的经验获取"),
        MURAMASA_BOOST("村正攻击提升", LegacyRitualConfig.MURAMASA_BOOST, "临时提升村正的攻击力"),
        FABRIC_ENHANCE("织印强化", LegacyRitualConfig.FABRIC_ENHANCE, "强化织印的效果"),
        SOUL_BINDING("灵魂绑定", LegacyRitualConfig.SOUL_BINDING, "从头颅创建假玩家核心"),
        DUPLICATION("禁忌复制", LegacyRitualConfig.DUPLICATION, "复制诅咒之镜中的物品"),
        EMBEDDING("七圣遗物嵌入", LegacyRitualConfig.EMBEDDING, "将七圣遗物嵌入七咒玩家体内"),
        UNBREAKABLE("不可破坏", LegacyRitualConfig.UNBREAKABLE, "使物品变得不可破坏"),
        SOULBOUND("灵魂束缚", LegacyRitualConfig.SOULBOUND, "使物品死亡不掉落");

        public final String name;
        public final String ritualId;  // LegacyRitualConfig 中的 ID
        public final String description;

        RitualType(String name, String ritualId, String description) {
            this.name = name;
            this.ritualId = ritualId;
            this.description = description;
        }

        /** 从 LegacyRitualConfig 动态获取所需阶层 */
        public int getRequiredTier() {
            return LegacyRitualConfig.getRequiredTier(ritualId);
        }

        public String getTierName() {
            int tier = getRequiredTier();
            switch (tier) {
                case 1: return "基础祭坛";
                case 2: return "进阶祭坛";
                case 3: return "大师祭坛";
                default: return "未知";
            }
        }

        /** 检查仪式是否启用 */
        public boolean isEnabled() {
            return LegacyRitualConfig.isEnabled(ritualId);
        }
    }

    private final RitualType type;
    private final List<ItemStack> centerItems;
    private final List<List<ItemStack>> pedestalItems;
    private final List<ItemStack> outputItems;
    private final int time;
    private final int energyPerPedestal;
    private final float failChance;
    private final String extraInfo;

    public SpecialRitualInfo(RitualType type, List<ItemStack> centerItems,
                            List<List<ItemStack>> pedestalItems, List<ItemStack> outputItems,
                            int time, int energyPerPedestal, float failChance, String extraInfo) {
        this.type = type;
        this.centerItems = centerItems;
        this.pedestalItems = pedestalItems;
        this.outputItems = outputItems;
        this.time = time;
        this.energyPerPedestal = energyPerPedestal;
        this.failChance = failChance;
        this.extraInfo = extraInfo;
    }

    // Getters
    public RitualType getType() { return type; }
    public List<ItemStack> getCenterItems() { return centerItems; }
    public List<List<ItemStack>> getPedestalItems() { return pedestalItems; }
    public List<ItemStack> getOutputItems() { return outputItems; }
    public int getTime() { return time; }
    public int getEnergyPerPedestal() { return energyPerPedestal; }
    public float getFailChance() { return failChance; }
    public String getExtraInfo() { return extraInfo; }

    /**
     * 获取所有特殊仪式的信息列表
     * 动态从 LegacyRitualConfig 读取配置，支持 CraftTweaker 修改
     */
    public static List<SpecialRitualInfo> getAllSpecialRituals() {
        List<SpecialRitualInfo> rituals = new ArrayList<>();

        // 遍历所有仪式类型，动态创建信息
        for (RitualType type : RitualType.values()) {
            // 跳过被禁用的仪式
            if (!type.isEnabled()) {
                continue;
            }

            SpecialRitualInfo info = createRitualInfo(type);
            if (info != null) {
                rituals.add(info);
            }
        }

        return rituals;
    }

    /**
     * 为指定仪式类型创建信息对象
     * 从 LegacyRitualConfig 读取动态配置
     */
    private static SpecialRitualInfo createRitualInfo(RitualType type) {
        String ritualId = type.ritualId;

        // 从配置读取动态值
        int time = LegacyRitualConfig.getDuration(ritualId);
        int energyPerPedestal = LegacyRitualConfig.getEnergyPerPedestal(ritualId);
        float failChance = LegacyRitualConfig.getFailChance(ritualId);

        // 获取材料（优先使用自定义配置）
        List<List<ItemStack>> pedestalItems = getMaterialsForRitual(type);

        // 获取中心物品和输出物品（这些通常是固定的）
        List<ItemStack> centerItems = getDefaultCenterItems(type);
        List<ItemStack> outputItems = getDefaultOutputItems(type);

        // 生成额外信息
        String extraInfo = generateExtraInfo(type, failChance);

        return new SpecialRitualInfo(type, centerItems, pedestalItems, outputItems,
                time, energyPerPedestal, failChance, extraInfo);
    }

    /**
     * 获取仪式的材料列表
     * 如果有 CraftTweaker 自定义配置，使用配置值
     * 否则使用默认硬编码值
     */
    private static List<List<ItemStack>> getMaterialsForRitual(RitualType type) {
        String ritualId = type.ritualId;

        // 检查是否有自定义材料配置
        if (LegacyRitualConfig.hasCustomMaterials(ritualId)) {
            List<LegacyRitualConfig.MaterialRequirement> requirements =
                    LegacyRitualConfig.getMaterialRequirements(ritualId);
            return convertMaterialRequirements(requirements);
        }

        // 使用默认硬编码材料
        return getDefaultPedestalItems(type);
    }

    /**
     * 将 MaterialRequirement 列表转换为 JEI 显示格式
     */
    private static List<List<ItemStack>> convertMaterialRequirements(
            List<LegacyRitualConfig.MaterialRequirement> requirements) {
        List<List<ItemStack>> result = new ArrayList<>();

        for (LegacyRitualConfig.MaterialRequirement req : requirements) {
            // 每个需求的数量决定添加多少个槽位
            for (int i = 0; i < req.getCount(); i++) {
                // 尝试获取匹配的物品示例
                List<ItemStack> examples = getExamplesForRequirement(req);
                if (!examples.isEmpty()) {
                    result.add(examples);
                }
            }
        }

        return result;
    }

    /**
     * 为材料需求获取示例物品（用于JEI显示）
     */
    private static List<ItemStack> getExamplesForRequirement(LegacyRitualConfig.MaterialRequirement req) {
        List<ItemStack> examples = new ArrayList<>();

        // ★ 优先使用精确匹配的物品（CRT配置的模组物品会正确显示）
        ItemStack exactItem = req.getExampleItem();
        if (exactItem != null && !exactItem.isEmpty()) {
            examples.add(exactItem.copy());
            return examples;
        }

        // 谓词匹配模式：尝试常见的物品
        ItemStack[] testItems = {
                new ItemStack(Items.NETHER_STAR),
                new ItemStack(Items.DIAMOND),
                new ItemStack(Blocks.OBSIDIAN),
                new ItemStack(Items.ENDER_PEARL),
                new ItemStack(Items.GHAST_TEAR),
                new ItemStack(Blocks.GOLD_BLOCK),
                new ItemStack(Items.GOLDEN_APPLE),
                new ItemStack(Items.ENCHANTED_BOOK),
                new ItemStack(Items.DYE, 1, 4), // 青金石
                new ItemStack(Items.DYE, 1, 0), // 墨囊
                new ItemStack(Items.DRAGON_BREATH),
                new ItemStack(Items.EXPERIENCE_BOTTLE),
                new ItemStack(Items.BLAZE_POWDER),
                new ItemStack(Items.MAGMA_CREAM),
                new ItemStack(Items.FERMENTED_SPIDER_EYE),
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.SPIDER_EYE),
                new ItemStack(Items.ENDER_EYE),
                new ItemStack(Items.PRISMARINE_SHARD),
                new ItemStack(Items.GLOWSTONE_DUST),
                new ItemStack(Items.REDSTONE),
                new ItemStack(Items.EMERALD),
                new ItemStack(Items.BOOK),
                new ItemStack(Items.GOLD_NUGGET),
                // ModItems
                ModItems.SOUL_FRUIT != null ? new ItemStack(ModItems.SOUL_FRUIT) : ItemStack.EMPTY,
                ModItems.VOID_ESSENCE != null ? new ItemStack(ModItems.VOID_ESSENCE) : ItemStack.EMPTY,
                ModItems.GAZE_FRAGMENT != null ? new ItemStack(ModItems.GAZE_FRAGMENT) : ItemStack.EMPTY,
                ModItems.SOUL_ANCHOR != null ? new ItemStack(ModItems.SOUL_ANCHOR) : ItemStack.EMPTY,
                ModItems.SPECTRAL_DUST != null ? new ItemStack(ModItems.SPECTRAL_DUST) : ItemStack.EMPTY,
                ModItems.ETHEREAL_SHARD != null ? new ItemStack(ModItems.ETHEREAL_SHARD) : ItemStack.EMPTY
        };

        for (ItemStack test : testItems) {
            if (!test.isEmpty() && req.matches(test)) {
                examples.add(test.copy());
            }
        }

        // 如果没找到匹配的，添加一个占位符
        if (examples.isEmpty()) {
            examples.add(new ItemStack(Items.BARRIER)); // 屏障方块作为"未知材料"占位符
        }

        return examples;
    }

    /**
     * 生成仪式的额外信息文本
     */
    private static String generateExtraInfo(RitualType type, float failChance) {
        StringBuilder info = new StringBuilder();

        // 成功率信息
        float successRate = (1.0f - failChance) * 100;
        if (failChance > 0) {
            info.append(String.format("成功率: %.0f%%\n", successRate));
        }

        // 特定仪式的额外说明
        switch (type) {
            case ENCHANT_INFUSION:
                info.append("需要至少3本附魔书\n附魔等级叠加\n七咒玩家成功率翻倍");
                break;
            case CURSE_PURIFICATION:
                info.append("移除物品上的诅咒附魔");
                break;
            case ENCHANT_TRANSFER:
                info.append("将源物品的附魔转移到目标\n源物品附魔被清除");
                break;
            case CURSE_CREATION:
                info.append("创造带诅咒的附魔书\n可用于给物品附加诅咒");
                break;
            case WEAPON_EXP_BOOST:
                info.append("为武器添加经验加速效果\n持续10分钟");
                break;
            case MURAMASA_BOOST:
                info.append("临时提升村正攻击力\n持续10分钟");
                break;
            case FABRIC_ENHANCE:
                info.append("强化织印盔甲的效果等级");
                break;
            case SOUL_BINDING:
                info.append("从头颅创建假玩家核心");
                break;
            case DUPLICATION:
                info.append("复制诅咒之镜中存储的物品\n失败将毁掉镜中物品");
                break;
            case EMBEDDING:
                info.append("将七圣遗物嵌入七咒玩家体内\n需要佩戴七咒之戒\n玩家需站在祭坛上");
                break;
            case UNBREAKABLE:
                info.append("使物品变得不可破坏\n保留所有NBT数据");
                break;
            case SOULBOUND:
                info.append("使物品死亡时不掉落\n保留所有NBT数据\n失败时物品被虚空吞噬");
                break;
        }

        return info.toString();
    }

    /**
     * 获取默认的中心物品（不受CRT配置影响）
     */
    private static List<ItemStack> getDefaultCenterItems(RitualType type) {
        switch (type) {
            case ENCHANT_INFUSION:
                return Arrays.asList(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_CHESTPLATE));
            case CURSE_PURIFICATION:
                return Arrays.asList(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_PICKAXE));
            case ENCHANT_TRANSFER:
                return Arrays.asList(new ItemStack(Items.DIAMOND_SWORD));
            case CURSE_CREATION:
                return Arrays.asList(new ItemStack(Items.BOOK));
            case WEAPON_EXP_BOOST:
                return Arrays.asList(new ItemStack(Items.DIAMOND_SWORD));
            case MURAMASA_BOOST:
                return Arrays.asList(new ItemStack(Items.DIAMOND_SWORD));
            case FABRIC_ENHANCE:
                return Arrays.asList(new ItemStack(Items.LEATHER_CHESTPLATE));
            case SOUL_BINDING:
                return Arrays.asList(new ItemStack(Items.SKULL, 1, 3));
            case DUPLICATION:
                return Arrays.asList(new ItemStack(ModItems.CURSED_MIRROR != null ? ModItems.CURSED_MIRROR : Items.ENDER_EYE));
            case EMBEDDING:
                return Arrays.asList(ModItems.SACRED_HEART != null ? new ItemStack(ModItems.SACRED_HEART) : new ItemStack(Items.NETHER_STAR));
            case UNBREAKABLE:
            case SOULBOUND:
                return Arrays.asList(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_CHESTPLATE));
            default:
                return new ArrayList<>();
        }
    }

    /**
     * 获取默认的输出物品（不受CRT配置影响）
     */
    private static List<ItemStack> getDefaultOutputItems(RitualType type) {
        switch (type) {
            case ENCHANT_INFUSION:
            case CURSE_PURIFICATION:
            case ENCHANT_TRANSFER:
            case WEAPON_EXP_BOOST:
            case MURAMASA_BOOST:
            case UNBREAKABLE:
            case SOULBOUND:
                return Arrays.asList(new ItemStack(Items.DIAMOND_SWORD));
            case CURSE_CREATION:
                return Arrays.asList(new ItemStack(Items.ENCHANTED_BOOK));
            case FABRIC_ENHANCE:
                return Arrays.asList(new ItemStack(Items.LEATHER_CHESTPLATE));
            case SOUL_BINDING:
                return Arrays.asList(new ItemStack(ModItems.FAKE_PLAYER_CORE != null ? ModItems.FAKE_PLAYER_CORE : Items.SKULL));
            case DUPLICATION:
                return Arrays.asList(new ItemStack(Items.DIAMOND));
            case EMBEDDING:
                return new ArrayList<>();
            default:
                return new ArrayList<>();
        }
    }

    /**
     * 获取默认的基座物品（当没有CRT自定义配置时使用）
     */
    private static List<List<ItemStack>> getDefaultPedestalItems(RitualType type) {
        switch (type) {
            case ENCHANT_INFUSION:
                return createPedestalList(new ItemStack(Items.ENCHANTED_BOOK), 8);

            case CURSE_PURIFICATION:
                return createPedestalList(Arrays.asList(
                        new ItemStack(Items.GOLDEN_APPLE),
                        new ItemStack(Items.GOLDEN_APPLE),
                        new ItemStack(Items.GHAST_TEAR),
                        new ItemStack(Items.GHAST_TEAR)
                ));

            case ENCHANT_TRANSFER:
                return createPedestalList(Arrays.asList(
                        new ItemStack(Items.DIAMOND_SWORD),
                        new ItemStack(Items.BOOK),
                        new ItemStack(Items.BOOK),
                        new ItemStack(Items.DYE, 1, 4),
                        new ItemStack(Items.DYE, 1, 4)
                ));

            case CURSE_CREATION:
                return createPedestalList(Arrays.asList(
                        new ItemStack(Items.DYE, 1, 0),
                        new ItemStack(Items.DYE, 1, 0),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE)
                ));

            case WEAPON_EXP_BOOST:
                return createPedestalList(Arrays.asList(
                        new ItemStack(Items.EXPERIENCE_BOTTLE),
                        new ItemStack(Items.EXPERIENCE_BOTTLE),
                        new ItemStack(Items.EXPERIENCE_BOTTLE),
                        new ItemStack(Items.EXPERIENCE_BOTTLE)
                ));

            case MURAMASA_BOOST:
                return createPedestalList(Arrays.asList(
                        new ItemStack(Items.BLAZE_POWDER),
                        new ItemStack(Items.BLAZE_POWDER),
                        new ItemStack(Items.MAGMA_CREAM),
                        new ItemStack(Items.MAGMA_CREAM)
                ));

            case FABRIC_ENHANCE:
                return createPedestalList(Arrays.asList(
                        new ItemStack(Items.GLOWSTONE_DUST),
                        new ItemStack(Items.GLOWSTONE_DUST),
                        new ItemStack(Items.REDSTONE),
                        new ItemStack(Items.REDSTONE)
                ));

            case SOUL_BINDING:
                return createPedestalList(Arrays.asList(
                        ModItems.SOUL_FRUIT != null ? new ItemStack(ModItems.SOUL_FRUIT) : new ItemStack(Items.GHAST_TEAR),
                        ModItems.VOID_ESSENCE != null ? new ItemStack(ModItems.VOID_ESSENCE) : new ItemStack(Items.ENDER_PEARL),
                        ModItems.GAZE_FRAGMENT != null ? new ItemStack(ModItems.GAZE_FRAGMENT) : new ItemStack(Items.ENDER_EYE),
                        ModItems.SOUL_ANCHOR != null ? new ItemStack(ModItems.SOUL_ANCHOR) : new ItemStack(Items.NETHER_STAR)
                ));

            case DUPLICATION:
                return createPedestalList(ModItems.VOID_ESSENCE != null ? new ItemStack(ModItems.VOID_ESSENCE) : new ItemStack(Items.ENDER_PEARL), 8);

            case EMBEDDING:
                return new ArrayList<>();

            case UNBREAKABLE:
                return createPedestalList(Arrays.asList(
                        new ItemStack(Items.NETHER_STAR),
                        new ItemStack(Items.NETHER_STAR),
                        new ItemStack(Blocks.OBSIDIAN),
                        new ItemStack(Blocks.OBSIDIAN),
                        new ItemStack(Items.DIAMOND),
                        new ItemStack(Items.DIAMOND),
                        new ItemStack(Items.DIAMOND),
                        new ItemStack(Items.DIAMOND)
                ));

            case SOULBOUND:
                return createPedestalList(Arrays.asList(
                        new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(Items.ENDER_PEARL),
                        new ItemStack(Items.GHAST_TEAR),
                        new ItemStack(Items.GHAST_TEAR),
                        new ItemStack(Blocks.GOLD_BLOCK),
                        new ItemStack(Blocks.GOLD_BLOCK)
                ));

            default:
                return new ArrayList<>();
        }
    }

    private static List<List<ItemStack>> createPedestalList(ItemStack item, int count) {
        List<List<ItemStack>> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(Arrays.asList(item.copy()));
        }
        return list;
    }

    private static List<List<ItemStack>> createPedestalList(List<ItemStack> items) {
        List<List<ItemStack>> list = new ArrayList<>();
        for (ItemStack item : items) {
            list.add(Arrays.asList(item.copy()));
        }
        return list;
    }
}
