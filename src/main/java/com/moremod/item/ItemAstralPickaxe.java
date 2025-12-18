package com.moremod.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 星芒镐 - 可以挖掘 Astral Sorcery 的特殊方块
 * 挖石头概率掉落大理石，挖铁矿概率掉落星辉锭
 */
public class ItemAstralPickaxe extends ItemPickaxe {
    
    public ItemAstralPickaxe() {
        super(ToolMaterial.DIAMOND);
        setTranslationKey("astral_pickaxe");
        setRegistryName("astral_pickaxe");
        setMaxStackSize(1);
    }
    
    /**
     * 标记方法 - 供 Mixin 和事件处理器检测
     */
    public boolean canMineAstralCrystals() {
        return true;
    }
    
    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }
    
    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 附魔光效
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add("\u00a7b" + I18n.format("item.moremod.astral_pickaxe.desc1"));
        tooltip.add("\u00a77" + I18n.format("item.moremod.astral_pickaxe.desc2"));
        tooltip.add("\u00a77" + I18n.format("item.moremod.astral_pickaxe.desc3"));
        tooltip.add("\u00a77" + I18n.format("item.moremod.astral_pickaxe.desc4"));
    }
}