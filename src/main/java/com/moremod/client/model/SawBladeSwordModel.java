package com.moremod.client.model;

import com.moremod.item.ItemSawBladeSword;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * 鋸刃剑的 GeckoLib 模型
 * 对应 sword.json (geometry.unknown)
 */
@SideOnly(Side.CLIENT)
public class SawBladeSwordModel extends AnimatedGeoModel<ItemSawBladeSword> {
    
    @Override
    public ResourceLocation getModelLocation(ItemSawBladeSword object) {
        return new ResourceLocation("moremod", "geo/saw_blade_sword.geo.json");
    }
    
    @Override
    public ResourceLocation getTextureLocation(ItemSawBladeSword object) {
        return new ResourceLocation("moremod", "textures/item/saw_blade_sword.png");
    }
    
    @Override
    public ResourceLocation getAnimationFileLocation(ItemSawBladeSword object) {
        return new ResourceLocation("moremod", "animations/saw_blade_sword.animation.json");
    }
}