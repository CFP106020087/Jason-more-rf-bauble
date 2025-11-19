package com.moremod.client.model;

import com.moremod.item.ItemHeroSword;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * 勇者之剑的 GeckoLib 模型
 * 对应 sword2.json (geometry.sword3)
 */
@SideOnly(Side.CLIENT)
public class HeroSwordModel extends AnimatedGeoModel<ItemHeroSword> {
    
    @Override
    public ResourceLocation getModelLocation(ItemHeroSword object) {
        return new ResourceLocation("moremod", "geo/hero_sword.geo.json");
    }
    
    @Override
    public ResourceLocation getTextureLocation(ItemHeroSword object) {
        return new ResourceLocation("moremod", "textures/item/hero_sword.png");
    }
    
    @Override
    public ResourceLocation getAnimationFileLocation(ItemHeroSword object) {
        return new ResourceLocation("moremod", "animations/hero_sword.animation.json");
    }
}