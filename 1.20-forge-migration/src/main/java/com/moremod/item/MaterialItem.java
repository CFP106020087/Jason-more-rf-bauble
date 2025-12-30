package com.moremod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 材料物品 - 1.20 Forge版本
 *
 * 用于合成的各种材料组件
 */
public class MaterialItem extends Item {

    private final MaterialType materialType;

    public MaterialItem(MaterialType type) {
        super(new Item.Properties().rarity(type.getRarity()));
        this.materialType = type;
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return materialType.hasGlint();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(materialType.getColor() + materialType.getDescription()));
    }

    /**
     * 材料类型枚举
     */
    public enum MaterialType implements StringRepresentable {
        // 基础组件
        MECHANICAL_GEAR("mechanical_gear", "机械齿轮", ChatFormatting.GRAY, Rarity.COMMON,
                "基础机械组件", false),
        ENERGY_CORE("energy_core", "能量核心", ChatFormatting.GREEN, Rarity.UNCOMMON,
                "存储能量的核心组件", true),
        CIRCUIT_BOARD("circuit_board", "电路板", ChatFormatting.DARK_GREEN, Rarity.COMMON,
                "基础电子组件", false),
        ADVANCED_CIRCUIT("advanced_circuit", "高级电路", ChatFormatting.GOLD, Rarity.RARE,
                "复杂的电子组件", true),

        // 升级材料
        UPGRADE_BASE("upgrade_base", "升级基座", ChatFormatting.AQUA, Rarity.UNCOMMON,
                "用于制作升级模块的基础", false),
        NEURAL_PROCESSOR("neural_processor", "神经处理器", ChatFormatting.LIGHT_PURPLE, Rarity.RARE,
                "高级信息处理组件", true),
        QUANTUM_CHIP("quantum_chip", "量子芯片", ChatFormatting.DARK_PURPLE, Rarity.EPIC,
                "量子计算核心组件", true),

        // 特殊材料
        VOID_ESSENCE("void_essence", "虚空精华", ChatFormatting.DARK_GRAY, Rarity.RARE,
                "从虚空中提取的神秘物质", true),
        TEMPORAL_DUST("temporal_dust", "时间尘埃", ChatFormatting.YELLOW, Rarity.RARE,
                "凝固的时间碎片", true),
        DIMENSIONAL_FABRIC("dimensional_fabric", "维度织物", ChatFormatting.BLUE, Rarity.EPIC,
                "跨越维度的神奇织物", true),

        // 能量材料
        SOLAR_CELL("solar_cell", "太阳能电池", ChatFormatting.YELLOW, Rarity.UNCOMMON,
                "高效光能转换组件", false),
        KINETIC_COIL("kinetic_coil", "动能线圈", ChatFormatting.WHITE, Rarity.UNCOMMON,
                "将动能转化为电能", false),
        FUSION_CORE("fusion_core", "聚变核心", ChatFormatting.RED, Rarity.EPIC,
                "小型化聚变反应堆核心", true);

        private final String id;
        private final String displayName;
        private final ChatFormatting color;
        private final Rarity rarity;
        private final String description;
        private final boolean hasGlint;

        MaterialType(String id, String displayName, ChatFormatting color, Rarity rarity,
                     String description, boolean hasGlint) {
            this.id = id;
            this.displayName = displayName;
            this.color = color;
            this.rarity = rarity;
            this.description = description;
            this.hasGlint = hasGlint;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public ChatFormatting getColor() {
            return color;
        }

        public Rarity getRarity() {
            return rarity;
        }

        public String getDescription() {
            return description;
        }

        public boolean hasGlint() {
            return hasGlint;
        }
    }
}
