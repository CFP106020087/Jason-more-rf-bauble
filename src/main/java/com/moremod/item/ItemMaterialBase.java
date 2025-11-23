package com.moremod.item;

import com.moremod.creativetab.MoremodMaterialsTab;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import com.moremod.creativetab.moremodCreativeTab; // 若没有此类，可删去并用 CreativeTabs.MATERIALS

public class ItemMaterialBase extends Item {

    public ItemMaterialBase(String name) {
        setRegistryName(name);           // moremod:name
        setTranslationKey(name);         // lang 用 key
            // 如果你有自定义创造标签
            setCreativeTab(MoremodMaterialsTab.MATERIALS_TAB);

        setMaxStackSize(64);
    }
}
