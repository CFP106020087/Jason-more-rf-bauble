package com.moremod.client.render;

import com.moremod.client.model.ModelCursedKnight;
import com.moremod.entity.EntityCursedKnight;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
@SideOnly(Side.CLIENT)

public class RenderCursedKnight extends GeoEntityRenderer<EntityCursedKnight> {

    public RenderCursedKnight(RenderManager renderManager) {
        super(renderManager, new ModelCursedKnight());
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(EntityCursedKnight entity, double x, double y, double z, float entityYaw, float partialTicks) {
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }
}