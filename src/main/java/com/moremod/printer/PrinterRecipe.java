package com.moremod.printer;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * 打印配方类
 * 定义一个打印配方需要的模版、材料、能量和输出
 * 完全由CraftTweaker脚本控制
 */
public class PrinterRecipe {

    private final String templateId;           // 模版ID（用于匹配模版物品）
    private final String displayName;          // 显示名称（可选）
    private final List<ItemStack> materials;   // 所需材料
    private final int energyCost;              // 能量消耗 (RF)
    private final int processingTime;          // 处理时间 (ticks)
    private final ItemStack output;            // 输出物品
    private final EnumRarity rarity;           // 稀有度

    public PrinterRecipe(String templateId, String displayName, List<ItemStack> materials,
                         int energyCost, int processingTime, ItemStack output, EnumRarity rarity) {
        this.templateId = templateId;
        this.displayName = displayName;
        this.materials = new ArrayList<>(materials);
        this.energyCost = energyCost;
        this.processingTime = processingTime;
        this.output = output.copy();
        this.rarity = rarity;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<ItemStack> getMaterials() {
        return new ArrayList<>(materials);
    }

    public int getEnergyCost() {
        return energyCost;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    public EnumRarity getRarity() {
        return rarity;
    }

    /**
     * 检查给定的材料是否满足配方要求
     */
    public boolean matchesMaterials(List<ItemStack> inputMaterials) {
        for (ItemStack required : materials) {
            int found = 0;
            for (ItemStack input : inputMaterials) {
                if (ItemStack.areItemsEqual(required, input) &&
                    ItemStack.areItemStackTagsEqual(required, input)) {
                    found += input.getCount();
                }
            }
            if (found < required.getCount()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 构建器模式
     */
    public static class Builder {
        private String templateId;
        private String displayName = "";
        private List<ItemStack> materials = new ArrayList<>();
        private int energyCost = 100000;  // 默认100k RF
        private int processingTime = 200; // 默认10秒
        private ItemStack output = ItemStack.EMPTY;
        private EnumRarity rarity = EnumRarity.RARE;

        public Builder setTemplateId(String templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder setDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder addMaterial(ItemStack material) {
            this.materials.add(material.copy());
            return this;
        }

        public Builder setEnergyCost(int energyCost) {
            this.energyCost = energyCost;
            return this;
        }

        public Builder setProcessingTime(int ticks) {
            this.processingTime = ticks;
            return this;
        }

        public Builder setOutput(ItemStack output) {
            this.output = output.copy();
            return this;
        }

        public Builder setRarity(EnumRarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public Builder setRarity(String rarityName) {
            switch (rarityName.toLowerCase()) {
                case "common":
                    this.rarity = EnumRarity.COMMON;
                    break;
                case "uncommon":
                    this.rarity = EnumRarity.UNCOMMON;
                    break;
                case "rare":
                    this.rarity = EnumRarity.RARE;
                    break;
                case "epic":
                    this.rarity = EnumRarity.EPIC;
                    break;
                default:
                    this.rarity = EnumRarity.RARE;
            }
            return this;
        }

        public PrinterRecipe build() {
            if (templateId == null || templateId.isEmpty()) {
                throw new IllegalStateException("Template ID is required");
            }
            if (output.isEmpty()) {
                throw new IllegalStateException("Output is required");
            }
            return new PrinterRecipe(templateId, displayName, materials, energyCost, processingTime, output, rarity);
        }
    }
}
