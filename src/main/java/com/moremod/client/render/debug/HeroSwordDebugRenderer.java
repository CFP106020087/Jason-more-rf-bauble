package com.moremod.client.render.debug;

import com.moremod.client.model.HeroSwordModel;
import com.moremod.item.ItemHeroSword;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

/**
 * 勇者之剑的调试渲染器
 *
 * ⚠️ 这是临时调试文件！
 * 调试完成后可以直接删除这个文件，改用 HeroSwordRenderer
 *
 * 使用方法：
 * 1. 临时把物品的渲染器改成这个类
 * 2. 手持剑按 NumPad * 开启调试
 * 3. 用数字小键盘调整位置
 * 4. NumPad Enter 获取代码
 * 5. 复制代码到 HeroSwordRenderer
 * 6. 删除这个文件和整个debug包
 */
@SideOnly(Side.CLIENT)
public class HeroSwordDebugRenderer extends GeoItemRenderer<ItemHeroSword> {
    public static final HeroSwordDebugRenderer INSTANCE = new HeroSwordDebugRenderer();

    public HeroSwordDebugRenderer() {
        super(new HeroSwordModel());
    }

    @Override
    public void renderByItem(ItemStack stack) {
        RenderDebugConfig.setCurrentItem("moremod:hero_sword");

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