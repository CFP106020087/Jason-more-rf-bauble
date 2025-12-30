package com.moremod.integration.jei.ritual;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * JEI特殊仪式包装器
 * 包装SpecialRitualInfo用于JEI显示
 */
public class SpecialRitualWrapper implements IRecipeWrapper {

    private final SpecialRitualInfo ritualInfo;

    public SpecialRitualWrapper(SpecialRitualInfo ritualInfo) {
        this.ritualInfo = ritualInfo;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        List<List<ItemStack>> inputs = new ArrayList<>();

        // 添加核心物品（可能有多种选择）
        inputs.add(new ArrayList<>(ritualInfo.getCenterItems()));

        // 添加基座物品
        for (List<ItemStack> pedestalItem : ritualInfo.getPedestalItems()) {
            inputs.add(new ArrayList<>(pedestalItem));
        }

        // 设置输入
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);

        // 设置输出（如果有）
        if (!ritualInfo.getOutputItems().isEmpty()) {
            ingredients.setOutputs(VanillaTypes.ITEM, ritualInfo.getOutputItems());
        }
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        FontRenderer fontRenderer = minecraft.fontRenderer;

        // 显示仪式名称（顶部）
        String name = ritualInfo.getType().name;
        int nameWidth = fontRenderer.getStringWidth(name);
        fontRenderer.drawString(name, (recipeWidth - nameWidth) / 2, 2, 0x8B0000);

        // 显示祭坛阶层要求
        String tierText = "需要: " + ritualInfo.getType().getTierName();
        int tierColor = getTierColor(ritualInfo.getType().requiredTier);
        fontRenderer.drawString(tierText, 5, 14, tierColor);

        // 显示时间（底部区域）
        int y = recipeHeight - 35;
        if (ritualInfo.getTime() > 0) {
            String timeText = String.format("时间: %ds", ritualInfo.getTime() / 20);
            fontRenderer.drawString(timeText, 5, y, Color.GRAY.getRGB());
        }

        // 显示能量需求
        if (ritualInfo.getEnergyPerPedestal() > 0) {
            int pedestalCount = ritualInfo.getPedestalItems().size();
            int totalEnergy = ritualInfo.getEnergyPerPedestal() * Math.max(pedestalCount, 1);
            String energyText;
            if (totalEnergy >= 1000000) {
                energyText = String.format("能量: %.1fM RF", totalEnergy / 1000000.0);
            } else if (totalEnergy >= 1000) {
                energyText = String.format("能量: %.0fK RF", totalEnergy / 1000.0);
            } else {
                energyText = String.format("能量: %d RF", totalEnergy);
            }
            fontRenderer.drawString(energyText, 5, y + 10, Color.GRAY.getRGB());
        }

        // 显示成功率/失败概率
        if (ritualInfo.getFailChance() > 0) {
            float successRate = (1 - ritualInfo.getFailChance()) * 100;
            String successText;
            int successColor;
            if (successRate >= 80) {
                successText = String.format("成功: %.0f%%", successRate);
                successColor = 0x00AA00; // 绿色
            } else if (successRate >= 50) {
                successText = String.format("成功: %.0f%%", successRate);
                successColor = 0xAAAA00; // 黄色
            } else if (successRate >= 10) {
                successText = String.format("成功: %.0f%%", successRate);
                successColor = 0xFF8800; // 橙色
            } else {
                successText = String.format("成功: %.0f%%", successRate);
                successColor = 0xAA0000; // 红色
            }
            fontRenderer.drawString(successText, recipeWidth - 55, y, successColor);
        }

        // 显示额外信息（多行）
        String extraInfo = ritualInfo.getExtraInfo();
        if (extraInfo != null && !extraInfo.isEmpty()) {
            String[] lines = extraInfo.split("\n");
            int infoY = y + 20;
            for (String line : lines) {
                if (infoY < recipeHeight - 5) {
                    // 截断过长的行
                    if (fontRenderer.getStringWidth(line) > recipeWidth - 10) {
                        line = fontRenderer.trimStringToWidth(line, recipeWidth - 15) + "...";
                    }
                    fontRenderer.drawString(line, 5, infoY, 0x555555);
                    infoY += 9;
                }
            }
        }
    }

    /**
     * 根据祭坛阶层返回颜色
     */
    private int getTierColor(int tier) {
        switch (tier) {
            case 1: return 0x666666; // 灰色 - 基础祭坛
            case 2: return 0x0066AA; // 蓝色 - 进阶祭坛
            case 3: return 0xAA00AA; // 紫色 - 大师祭坛
            default: return 0x000000;
        }
    }

    public SpecialRitualInfo getRitualInfo() {
        return ritualInfo;
    }
}
