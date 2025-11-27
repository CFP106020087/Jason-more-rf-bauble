package com.moremod.creativetab;

import com.moremod.item.RegisterItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * moremod 创造模式标签页
 * 专门展示所有 moremod 物品的创造标签页
 */
public class moremodCreativeTab extends CreativeTabs {

    public static final moremodCreativeTab moremod_TAB = new moremodCreativeTab();

    private moremodCreativeTab() {
        super("moremod");
        System.out.println("[moremod] 创造模式标签页初始化完成");
    }

    /**
     * 获取标签页的图标物品
     * 这里使用能量戒指作为代表图标
     */
    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack createIcon() {
        // 使用能量戒指作为标签页图标
        return new ItemStack(RegisterItem.ENERGY_RING);
    }

    /**
     * 获取标签页的显示名称
     * 这个方法在某些版本中可能需要
     */
    @Override
    public String getTranslationKey() {
        return "Jason's Bauble";
    }

    /**
     * 是否有搜索栏
     * 如果物品很多，建议开启搜索功能
     */
    @Override
    public boolean hasSearchBar() {
        return true;
    }

    /**
     * 搜索栏的背景纹理
     * 只有在 hasSearchBar() 返回 true 时才需要
     */
    @Override
    @SideOnly(Side.CLIENT)
    public String getBackgroundImageName() {
        return "item_search.png";
    }
}