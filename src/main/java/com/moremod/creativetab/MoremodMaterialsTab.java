package com.moremod.creativetab;

import com.moremod.item.RegisterItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * moremod 材料与工具标签页
 * 用于展示材料、工具、组件等物品
 */
public class MoremodMaterialsTab extends CreativeTabs {

    public static final MoremodMaterialsTab MATERIALS_TAB = new MoremodMaterialsTab();

    private MoremodMaterialsTab() {
        super("moremod_materials");
        System.out.println("[moremod] 材料标签页初始化完成");
    }

    /**
     * 获取标签页的图标物品
     * 使用机械核心或其他代表性物品作为图标
     */
    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack createIcon() {
        // 使用机械核心作为标签页图标
        return new ItemStack(RegisterItem.MECHANICAL_CORE);
    }

    /**
     * 获取标签页的显示名称
     */
    @Override
    public String getTranslationKey() {
        return "Jason's Material";
    }

    /**
     * 不显示搜索栏（如果物品较少）
     */
    @Override
    public boolean hasSearchBar() {
        return false;
    }
}