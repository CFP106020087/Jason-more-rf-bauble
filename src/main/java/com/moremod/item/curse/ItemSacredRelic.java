package com.moremod.item.curse;

import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 七圣遗物 - 用于在三阶祭坛嵌入抵消七咒效果
 *
 * 1. 圣光之心 (sacred_heart) → 抵消受伤加倍
 * 2. 和平徽章 (peace_emblem) → 抵消中立生物攻击
 * 3. 守护鳞片 (guardian_scale) → 抵消护甲降低30%
 * 4. 勇气之刃 (courage_blade) → 抵消伤害降低50%
 * 5. 霜华之露 (frost_dew) → 抵消永燃
 * 6. 灵魂锚点 (soul_anchor) → 抵消灵魂破碎
 * 7. 安眠香囊 (slumber_sachet) → 抵消失眠症
 */
public class ItemSacredRelic extends Item {

    public enum RelicType {
        SACRED_HEART("sacred_heart", "圣光之心", "抵消: 受到伤害加倍", TextFormatting.GOLD),
        PEACE_EMBLEM("peace_emblem", "和平徽章", "抵消: 中立生物攻击", TextFormatting.GREEN),
        GUARDIAN_SCALE("guardian_scale", "守护鳞片", "抵消: 护甲效力降低30%", TextFormatting.AQUA),
        COURAGE_BLADE("courage_blade", "勇气之刃", "抵消: 对怪物伤害降低50%", TextFormatting.RED),
        FROST_DEW("frost_dew", "霜华之露", "抵消: 着火永燃", TextFormatting.BLUE),
        SOUL_ANCHOR("soul_anchor", "灵魂锚点", "抵消: 死亡灵魂破碎", TextFormatting.LIGHT_PURPLE),
        SLUMBER_SACHET("slumber_sachet", "安眠香囊", "抵消: 失眠症", TextFormatting.DARK_PURPLE);

        private final String id;
        private final String displayName;
        private final String effect;
        private final TextFormatting color;

        RelicType(String id, String displayName, String effect, TextFormatting color) {
            this.id = id;
            this.displayName = displayName;
            this.effect = effect;
            this.color = color;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getEffect() { return effect; }
        public TextFormatting getColor() { return color; }

        public static RelicType fromId(String id) {
            for (RelicType type : values()) {
                if (type.id.equals(id)) return type;
            }
            return null;
        }
    }

    private final RelicType relicType;

    public ItemSacredRelic(RelicType type) {
        this.relicType = type;
        setMaxStackSize(1);
        setRegistryName("moremod", type.getId());
        setTranslationKey("moremod." + type.getId());
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    public RelicType getRelicType() {
        return relicType;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 发光效果
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("");
        tooltip.add(relicType.getColor() + "七圣遗物");
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + relicType.getEffect());
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_PURPLE + "在三阶祭坛嵌入体内");
        tooltip.add(TextFormatting.DARK_PURPLE + "可抵消七咒之戒的诅咒");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "蹲下右键祭坛核心坐上");
    }
}
