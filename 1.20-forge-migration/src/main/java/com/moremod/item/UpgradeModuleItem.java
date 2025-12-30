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
 * 升级模块物品 - 1.20 Forge版本
 *
 * 用于安装到机械核心的升级模块
 * 每种升级提供不同的被动效果
 */
public class UpgradeModuleItem extends Item {

    private final ModuleType moduleType;

    public UpgradeModuleItem(ModuleType type) {
        super(new Item.Properties().rarity(type.getRarity()));
        this.moduleType = type;
    }

    public ModuleType getModuleType() {
        return moduleType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(moduleType.getColor() + moduleType.getDescription()));
        tooltip.add(Component.empty());

        tooltip.add(Component.literal(ChatFormatting.AQUA + "效果："));
        for (String effect : moduleType.getEffects()) {
            tooltip.add(Component.literal(ChatFormatting.GRAY + "  • " + effect));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "能耗：" + ChatFormatting.WHITE + moduleType.getEnergyCost() + " RF/t"));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "安装到机械核心使用"));
    }

    /**
     * 升级模块类型枚举
     */
    public enum ModuleType implements StringRepresentable {
        // 防御模块
        YELLOW_SHIELD("yellow_shield", "黄金护盾", ChatFormatting.GOLD,
                Rarity.RARE, 20, "激活时提供伤害减免护盾", "吸收25%伤害"),
        HEALTH_REGEN("health_regen", "生命恢复", ChatFormatting.RED,
                Rarity.UNCOMMON, 15, "持续恢复生命值", "每秒恢复0.5颗心"),
        FIRE_EXTINGUISH("fire_extinguish", "自动灭火", ChatFormatting.BLUE,
                Rarity.COMMON, 5, "着火时自动熄灭", "消耗能量扑灭火焰"),

        // 移动模块
        MOVEMENT_SPEED("movement_speed", "移动加速", ChatFormatting.AQUA,
                Rarity.UNCOMMON, 10, "提升移动速度", "速度+20%"),
        WATER_BREATHING("water_breathing", "水下呼吸", ChatFormatting.DARK_AQUA,
                Rarity.UNCOMMON, 8, "在水下可以呼吸", "无限水下时间"),

        // 战斗模块
        DAMAGE_BOOST("damage_boost", "攻击强化", ChatFormatting.RED,
                Rarity.RARE, 25, "增加攻击伤害", "伤害+15%"),
        ATTACK_SPEED("attack_speed", "攻速提升", ChatFormatting.LIGHT_PURPLE,
                Rarity.UNCOMMON, 12, "提升攻击速度", "攻速+10%"),
        CRITICAL_STRIKE("critical_strike", "暴击强化", ChatFormatting.GOLD,
                Rarity.RARE, 18, "增加暴击率和暴击伤害", "暴击率+15%, 暴击伤害+25%"),

        // 工具模块
        NIGHT_VISION("night_vision", "夜视", ChatFormatting.YELLOW,
                Rarity.COMMON, 5, "在黑暗中看清一切", "永久夜视效果"),
        ORE_VISION("ore_vision", "矿物透视", ChatFormatting.GREEN,
                Rarity.RARE, 30, "高亮显示附近矿物", "范围8格"),
        ITEM_MAGNET("item_magnet", "物品磁铁", ChatFormatting.WHITE,
                Rarity.UNCOMMON, 8, "自动吸取附近物品", "范围5格"),

        // 能量模块
        SOLAR_GENERATOR("solar_generator", "太阳能充电", ChatFormatting.YELLOW,
                Rarity.UNCOMMON, 0, "在阳光下自动充能", "白天+50 RF/t"),
        KINETIC_GENERATOR("kinetic_generator", "动能发电", ChatFormatting.GRAY,
                Rarity.UNCOMMON, 0, "移动时自动充能", "移动时+20 RF/t");

        private final String id;
        private final String displayName;
        private final ChatFormatting color;
        private final Rarity rarity;
        private final int energyCost;
        private final String[] effects;

        ModuleType(String id, String displayName, ChatFormatting color,
                   Rarity rarity, int energyCost, String... effects) {
            this.id = id;
            this.displayName = displayName;
            this.color = color;
            this.rarity = rarity;
            this.energyCost = energyCost;
            this.effects = effects;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return displayName + " 模块";
        }

        public ChatFormatting getColor() {
            return color;
        }

        public Rarity getRarity() {
            return rarity;
        }

        public int getEnergyCost() {
            return energyCost;
        }

        public String[] getEffects() {
            return effects;
        }
    }
}
