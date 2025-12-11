package com.moremod.integration.jei.ritual;

import com.moremod.init.ModItems;
import com.moremod.moremod;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 特殊仪式信息类 - 用于JEI展示
 * 包含所有特殊仪式的定义
 */
public class SpecialRitualInfo {

    public enum RitualType {
        ENCHANT_INFUSION("注魔仪式", 3, "将多本附魔书的附魔叠加到物品上"),
        CURSE_PURIFICATION("诅咒净化", 2, "移除物品上的诅咒附魔"),
        ENCHANT_TRANSFER("附魔转移", 3, "将物品的附魔转移到另一物品"),
        CURSE_CREATION("诅咒创造", 2, "创造诅咒卷轴"),
        WEAPON_EXP_BOOST("武器经验加速", 2, "加速武器的经验获取"),
        MURAMASA_BOOST("村正攻击提升", 2, "临时提升村正的攻击力"),
        FABRIC_ENHANCE("织印强化", 2, "强化织印的效果"),
        SOUL_BINDING("灵魂绑定", 3, "从头颅创建假玩家核心"),
        DUPLICATION("禁忌复制", 3, "复制诅咒之镜中的物品"),
        EMBEDDING("七圣遗物嵌入", 3, "将七圣遗物嵌入七咒玩家体内"),
        UNBREAKABLE("不可破坏", 3, "使物品变得不可破坏"),
        SOULBOUND("灵魂束缚", 3, "使物品死亡不掉落");

        public final String name;
        public final int requiredTier;
        public final String description;

        RitualType(String name, int requiredTier, String description) {
            this.name = name;
            this.requiredTier = requiredTier;
            this.description = description;
        }

        public String getTierName() {
            switch (requiredTier) {
                case 1: return "基础祭坛";
                case 2: return "进阶祭坛";
                case 3: return "大师祭坛";
                default: return "未知";
            }
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
     */
    public static List<SpecialRitualInfo> getAllSpecialRituals() {
        List<SpecialRitualInfo> rituals = new ArrayList<>();

        // 1. 注魔仪式 (三阶)
        rituals.add(new SpecialRitualInfo(
            RitualType.ENCHANT_INFUSION,
            Arrays.asList(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_CHESTPLATE)),
            createPedestalList(new ItemStack(Items.ENCHANTED_BOOK), 8),
            Arrays.asList(new ItemStack(Items.DIAMOND_SWORD)), // 带附魔
            200, 100000, 0.90f,
            "需要至少3本附魔书\n成功率: 5% (七咒: 10%)\n附魔等级叠加"
        ));

        // 2. 诅咒净化 (二阶)
        rituals.add(new SpecialRitualInfo(
            RitualType.CURSE_PURIFICATION,
            Arrays.asList(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_PICKAXE)),
            createPedestalList(Arrays.asList(
                new ItemStack(Items.GOLDEN_APPLE),
                new ItemStack(Items.GOLDEN_APPLE),
                new ItemStack(Items.GHAST_TEAR),
                new ItemStack(Items.GHAST_TEAR)
            )),
            Arrays.asList(new ItemStack(Items.DIAMOND_SWORD)),
            200, 150000, 0.0f,
            "移除物品上的诅咒附魔\n需要: 金苹果×2 + 恶魂之泪×2"
        ));

        // 3. 附魔转移 (三阶)
        rituals.add(new SpecialRitualInfo(
            RitualType.ENCHANT_TRANSFER,
            Arrays.asList(new ItemStack(Items.DIAMOND_SWORD)), // 带附魔的源
            createPedestalList(Arrays.asList(
                new ItemStack(Items.DIAMOND_SWORD), // 目标
                new ItemStack(Items.BOOK),
                new ItemStack(Items.BOOK),
                new ItemStack(Items.LAPIS_LAZULI),
                new ItemStack(Items.LAPIS_LAZULI)
            )),
            Arrays.asList(new ItemStack(Items.DIAMOND_SWORD)), // 目标带附魔
            300, 200000, 0.10f,
            "将源物品的附魔转移到目标\n源物品附魔被清除"
        ));

        // 4. 诅咒创造 (二阶)
        rituals.add(new SpecialRitualInfo(
            RitualType.CURSE_CREATION,
            Arrays.asList(new ItemStack(Items.PAPER)),
            createPedestalList(Arrays.asList(
                new ItemStack(Items.DYE, 1, 0), // 墨囊
                new ItemStack(Items.DYE, 1, 0),
                new ItemStack(Items.FERMENTED_SPIDER_EYE),
                new ItemStack(Items.FERMENTED_SPIDER_EYE)
            )),
            Arrays.asList(new ItemStack(ModItems.CURSE_SCROLL != null ? ModItems.CURSE_SCROLL : Items.PAPER)),
            200, 100000, 0.0f,
            "创造诅咒卷轴\n可用于给物品附加诅咒"
        ));

        // 5. 武器经验加速 (二阶)
        rituals.add(new SpecialRitualInfo(
            RitualType.WEAPON_EXP_BOOST,
            Arrays.asList(new ItemStack(Items.DIAMOND_SWORD)),
            createPedestalList(Arrays.asList(
                new ItemStack(Items.EXPERIENCE_BOTTLE),
                new ItemStack(Items.EXPERIENCE_BOTTLE),
                new ItemStack(Items.EXPERIENCE_BOTTLE),
                new ItemStack(Items.EXPERIENCE_BOTTLE)
            )),
            Arrays.asList(new ItemStack(Items.DIAMOND_SWORD)),
            150, 100000, 0.0f,
            "为武器添加经验加速效果\n持续10分钟"
        ));

        // 6. 村正攻击提升 (二阶)
        rituals.add(new SpecialRitualInfo(
            RitualType.MURAMASA_BOOST,
            Arrays.asList(new ItemStack(ModItems.MURAMASA != null ? ModItems.MURAMASA : Items.DIAMOND_SWORD)),
            createPedestalList(Arrays.asList(
                new ItemStack(Items.BLAZE_POWDER),
                new ItemStack(Items.BLAZE_POWDER),
                new ItemStack(Items.MAGMA_CREAM),
                new ItemStack(Items.MAGMA_CREAM)
            )),
            Arrays.asList(new ItemStack(ModItems.MURAMASA != null ? ModItems.MURAMASA : Items.DIAMOND_SWORD)),
            150, 100000, 0.0f,
            "临时提升村正攻击力\n持续10分钟"
        ));

        // 7. 织印强化 (二阶)
        rituals.add(new SpecialRitualInfo(
            RitualType.FABRIC_ENHANCE,
            Arrays.asList(new ItemStack(ModItems.FABRIC_STAMP != null ? ModItems.FABRIC_STAMP : Items.PAPER)),
            createPedestalList(Arrays.asList(
                new ItemStack(Items.GLOWSTONE_DUST),
                new ItemStack(Items.GLOWSTONE_DUST),
                new ItemStack(Items.REDSTONE),
                new ItemStack(Items.REDSTONE)
            )),
            Arrays.asList(new ItemStack(ModItems.FABRIC_STAMP != null ? ModItems.FABRIC_STAMP : Items.PAPER)),
            200, 120000, 0.0f,
            "强化织印的效果等级"
        ));

        // 8. 灵魂绑定 (三阶)
        rituals.add(new SpecialRitualInfo(
            RitualType.SOUL_BINDING,
            Arrays.asList(new ItemStack(Items.SKULL, 1, 3)), // 玩家头颅
            createPedestalList(Arrays.asList(
                ModItems.SOUL_FRUIT != null ? new ItemStack(ModItems.SOUL_FRUIT) : new ItemStack(Items.GHAST_TEAR),
                ModItems.VOID_ESSENCE != null ? new ItemStack(ModItems.VOID_ESSENCE) : new ItemStack(Items.ENDER_PEARL),
                ModItems.GAZE_FRAGMENT != null ? new ItemStack(ModItems.GAZE_FRAGMENT) : new ItemStack(Items.ENDER_EYE),
                ModItems.SOUL_ANCHOR != null ? new ItemStack(ModItems.SOUL_ANCHOR) : new ItemStack(Items.NETHER_STAR)
            )),
            Arrays.asList(new ItemStack(ModItems.FAKE_PLAYER_CORE != null ? ModItems.FAKE_PLAYER_CORE : Items.SKULL)),
            400, 200000, 0.50f,
            "从头颅创建假玩家核心\n成功率: 50%"
        ));

        // 9. 禁忌复制 (三阶)
        rituals.add(new SpecialRitualInfo(
            RitualType.DUPLICATION,
            Arrays.asList(new ItemStack(ModItems.CURSED_MIRROR != null ? ModItems.CURSED_MIRROR : Items.ENDER_EYE)),
            createPedestalList(ModItems.VOID_ESSENCE != null ? new ItemStack(ModItems.VOID_ESSENCE) : new ItemStack(Items.ENDER_PEARL), 8),
            Arrays.asList(new ItemStack(Items.DIAMOND)), // 示例复制品
            300, 250000, 0.99f,
            "复制诅咒之镜中存储的物品\n成功率: 1%\n失败将毁掉镜中物品"
        ));

        // 10. 七圣遗物嵌入 (三阶)
        rituals.add(new SpecialRitualInfo(
            RitualType.EMBEDDING,
            Arrays.asList(
                ModItems.SACRED_RELIC_HEART != null ? new ItemStack(ModItems.SACRED_RELIC_HEART) : new ItemStack(Items.NETHER_STAR)
            ),
            new ArrayList<>(), // 嵌入仪式不需要基座物品，需要玩家站在祭坛上
            new ArrayList<>(), // 无物品输出，效果是嵌入玩家
            100, 0, 0.0f,
            "将七圣遗物嵌入七咒玩家体内\n需要佩戴七咒之戒\n玩家需站在祭坛上"
        ));

        // 11. 不可破坏 (三阶) - 新增
        rituals.add(new SpecialRitualInfo(
            RitualType.UNBREAKABLE,
            Arrays.asList(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_CHESTPLATE)),
            createPedestalList(Arrays.asList(
                new ItemStack(Items.NETHER_STAR),
                new ItemStack(Items.NETHER_STAR),
                new ItemStack(Blocks.OBSIDIAN),
                new ItemStack(Blocks.OBSIDIAN),
                new ItemStack(Items.DIAMOND),
                new ItemStack(Items.DIAMOND),
                new ItemStack(Items.DIAMOND),
                new ItemStack(Items.DIAMOND)
            )),
            Arrays.asList(new ItemStack(Items.DIAMOND_SWORD)), // 带Unbreakable标签
            400, 300000, 0.20f,
            "使物品变得不可破坏\n保留所有NBT数据\n成功率: 80%"
        ));

        // 12. 灵魂束缚 (三阶) - 死亡不掉落
        rituals.add(new SpecialRitualInfo(
            RitualType.SOULBOUND,
            Arrays.asList(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_CHESTPLATE)),
            createPedestalList(Arrays.asList(
                new ItemStack(Items.ENDER_PEARL),
                new ItemStack(Items.ENDER_PEARL),
                new ItemStack(Items.ENDER_PEARL),
                new ItemStack(Items.ENDER_PEARL),
                new ItemStack(Items.GHAST_TEAR),
                new ItemStack(Items.GHAST_TEAR),
                new ItemStack(Blocks.GOLD_BLOCK),
                new ItemStack(Blocks.GOLD_BLOCK)
            )),
            Arrays.asList(new ItemStack(Items.DIAMOND_SWORD)), // 带Soulbound标签
            300, 200000, 0.10f,
            "使物品死亡时不掉落\n保留所有NBT数据\n成功率: 90%\n失败时物品被虚空吞噬"
        ));

        return rituals;
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
