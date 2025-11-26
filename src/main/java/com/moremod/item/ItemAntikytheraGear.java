package com.moremod.item;

import com.moremod.creativetab.MoremodMaterialsTab;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.item.Item;

/**
 * 安提基特拉的齿轮 - 后续高阶机械/工艺材料
 */
public class ItemAntikytheraGear extends Item {

    public static final String NAME = "antikythera_gear";

    public ItemAntikytheraGear() {
        setRegistryName(NAME);
        setTranslationKey("moremod." + NAME);
        setCreativeTab(MoremodMaterialsTab.MATERIALS_TAB); // 按你的项目实际 CreativeTab 调整
        setMaxStackSize(64);
    }
}
