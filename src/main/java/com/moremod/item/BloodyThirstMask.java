// BloodyThirstMask.java - 嗜血面具主类
package com.moremod.items;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.events.BloodyThirstMaskEventHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class BloodyThirstMask extends Item implements IBauble {

    public BloodyThirstMask() {
        super();
        this.setMaxStackSize(1);
        this.setRegistryName("moremod", "bloody_thirst_mask");
        this.setTranslationKey("bloody_thirst_mask");
        setCreativeTab(moremodCreativeTab.moremod_TAB);

    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.HEAD; // 头部饰品
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        // 装备时持续效果（可选）
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        // 装备时效果（可选）
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 卸下时效果（可选）
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> tooltip, ITooltipFlag flag) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        // 使用物种数量而不是击杀数量
        int speciesCount = getSpeciesKilledCount(stack);
        float lifesteal = getLifestealAmount(speciesCount);
        int tier = getMaskTier(speciesCount);

        tooltip.add(TextFormatting.DARK_RED + "☠ 攻击吸血: " +
                String.format("%.1f", lifesteal) + "%");
        tooltip.add(TextFormatting.GRAY + "击杀物种: " + speciesCount + " 种");
        tooltip.add(TextFormatting.GOLD + "等级: " + getMaskTierName(tier));
        tooltip.add("");

        // 显示已击杀的物种列表（最多显示10种）
        Set<String> killedSpeciesNames = BloodyThirstMaskEventHandler.getKilledSpeciesDisplayNames(stack);
        if (!killedSpeciesNames.isEmpty()) {
            tooltip.add(TextFormatting.YELLOW + "已击杀物种:");
            int count = 0;
            for (String speciesName : killedSpeciesNames) {
                if (count >= 10) {
                    tooltip.add(TextFormatting.GRAY + "... 还有 " + (killedSpeciesNames.size() - 10) + " 种");
                    break;
                }
                tooltip.add(TextFormatting.GRAY + "• " + speciesName);
                count++;
            }
            tooltip.add("");
        }

        tooltip.add(TextFormatting.GRAY + "击杀新物种种类提升吸血效果");
        tooltip.add(TextFormatting.GRAY + "重复击杀同种生物无效");
        tooltip.add(TextFormatting.DARK_GRAY + "装备在头部饰品槽中生效");

        super.addInformation(stack, world, tooltip, flag);
    }

    // 根据击杀物种数量计算吸血百分比
    public static float getLifestealAmount(int speciesCount) {
        float base = 10.0f;  // 基础10% (10倍增强：原1%)
        float bonus = speciesCount * 1.0f;  // 每种物种增加1% (10倍增强：原0.1%)
        return Math.min(base + bonus, 200.0f);  // 最大200% (10倍增强：原20%)
    }

    // 获取面具等级（基于物种数量）
    public static int getMaskTier(int speciesCount) {
        if (speciesCount >= 50) return 5; // 传奇 (50种)
        if (speciesCount >= 30) return 4;  // 史诗 (30种)
        if (speciesCount >= 20) return 3;  // 稀有 (20种)
        if (speciesCount >= 10) return 2;  // 优秀 (10种)
        return 1; // 普通
    }

    public static String getMaskTierName(int tier) {
        switch (tier) {
            case 5: return TextFormatting.GOLD + "迸发尖刺的石鬼面 (大屠杀者)";
            case 4: return TextFormatting.DARK_PURPLE + "石鬼面 (物种猎手)";
            case 3: return TextFormatting.BLUE + "滋润的血面具 (生物杀手)";
            case 2: return TextFormatting.GREEN + "稍为滋润的血面具 (狩猎者)";
            default: return TextFormatting.WHITE + "干枯的血面具(新手)";
        }
    }

    // 获取已击杀的物种数量
    private int getSpeciesKilledCount(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || !nbt.hasKey("killedSpecies")) {
            return 0;
        }

        NBTTagList speciesList = nbt.getTagList("killedSpecies", 8);
        return speciesList.tagCount();
    }
}