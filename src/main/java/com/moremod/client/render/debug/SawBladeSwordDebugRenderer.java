package com.moremod.client.render.debug;

import com.moremod.client.model.SawBladeSwordModel;
import com.moremod.item.ItemSawBladeSword;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

@SideOnly(Side.CLIENT)
public class SawBladeSwordDebugRenderer extends GeoItemRenderer<ItemSawBladeSword> {
    public static final SawBladeSwordDebugRenderer INSTANCE = new SawBladeSwordDebugRenderer();

    public SawBladeSwordDebugRenderer() {
        super(new SawBladeSwordModel());
    }

    @Override
    public void renderByItem(ItemStack stack) {

        RenderDebugConfig.setCurrentItem("moremod:saw_blade_sword");

        GlStateManager.pushMatrix();

        if (RenderDebugConfig.isDebugEnabled()) {
            RenderDebugConfig.RenderParams p = RenderDebugConfig.getCurrentParams();

            GlStateManager.translate(p.translateX, p.translateY, p.translateZ);
            GlStateManager.rotate(p.rotateX, 1, 0, 0);
            GlStateManager.rotate(p.rotateY, 0, 1, 0);
            GlStateManager.rotate(p.rotateZ, 0, 0, 1);
            GlStateManager.scale(p.scale, p.scale, p.scale);
        }

        super.renderByItem(stack);

        GlStateManager.popMatrix();
    }
}
