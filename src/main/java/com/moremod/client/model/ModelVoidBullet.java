package com.moremod.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelVoidBullet extends ModelBase {
    public final ModelRenderer core;

    public ModelVoidBullet() {
        this.textureWidth = 32;
        this.textureHeight = 32;
        this.core = new ModelRenderer(this, 0, 0);
        // 4x4x4 小方块
        this.core.addBox(-2, -2, -2, 4, 4, 4);
    }

    public void renderAll(float scale) {
        this.core.render(scale);
    }
}
