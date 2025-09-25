package com.moremod.item;

import net.minecraft.item.Item;

public final class ModMaterialItems {
    private ModMaterialItems() {}

    // 基础材料（不含齿轮）
    public static final Item ANCIENT_COMPONENT = new ItemMaterialBase("ancient_component");
    public static final Item MYSTERIOUS_DUST   = new ItemMaterialBase("mysterious_dust");
    public static final Item RARE_CRYSTAL      = new ItemMaterialBase("rare_crystal");

    // 二级材料
    public static final Item BLANK_TEMPLATE    = new ItemMaterialBase("blank_template");
}
