package com.moremod.item.sawblade.client;

import com.moremod.item.sawblade.potion.PotionBloodEuphoria;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 鲜血欢愉 - 玩家红色光晕渲染层
 * 
 * 高级实现方式：
 * 1. 使用LayerRenderer叠加红色半透明玩家模型
 * 2. 强度随buff层数变化
 * 3. 脉冲呼吸效果
 * 4. 发光材质（不受光照影响）
 */
@SideOnly(Side.CLIENT)
public class BloodEuphoriaRenderer implements LayerRenderer<AbstractClientPlayer> {
    
    private final RenderPlayer playerRenderer;
    
    // 红色发光纹理（可以使用玩家本身的纹理但染色）
    private static final ResourceLocation GLOW_TEXTURE = new ResourceLocation("textures/entity/steve.png");
    
    public BloodEuphoriaRenderer(RenderPlayer playerRenderer) {
        this.playerRenderer = playerRenderer;
    }
    
    @Override
    public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, 
                            float partialTicks, float ageInTicks, float netHeadYaw, 
                            float headPitch, float scale) {
        
        // 检查是否有鲜血欢愉效果
        if (!PotionBloodEuphoria.hasEffect(player)) {
            return;
        }
        
        // 获取buff层数
        int stacks = PotionBloodEuphoria.getStacks(player);
        if (stacks <= 0) return;
        
        // 计算透明度（基于层数）
        float baseAlpha = 0.3f + (stacks * 0.2f);  // 1层30%，2层50%，3层70%
        
        // 脉冲效果（呼吸）
        float pulse = (float)(Math.sin(ageInTicks * 0.1f) * 0.2f + 0.8f);
        float alpha = baseAlpha * pulse;
        
        // 保存状态
        GlStateManager.pushMatrix();
        
        // 启用混合
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);  // 加法混合
        
        // 禁用光照（让它自己发光）
        GlStateManager.disableLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0f, 240.0f);
        
        // 设置颜色（红色 + 透明度）
        GlStateManager.color(1.0f, 0.0f, 0.0f, alpha);
        
        // 绑定玩家纹理（会被染成红色）
        this.playerRenderer.bindTexture(player.getLocationSkin());
        
        // 渲染模型（稍微放大以产生光晕效果）
        float scaleBoost = 1.0f + (stacks * 0.02f);  // 每层+2%大小
        GlStateManager.scale(scaleBoost, scaleBoost, scaleBoost);
        
        // 获取模型并渲染
        ModelPlayer model = this.playerRenderer.getMainModel();
        model.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, player);
        model.render(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        
        // 恢复状态
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        
        GlStateManager.popMatrix();
    }
    
    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
    
    /**
     * 注册渲染层到玩家渲染器
     * 需要在ClientProxy中调用
     */
    public static void registerLayer() {
        Minecraft mc = Minecraft.getMinecraft();
        
        // 获取玩家渲染器
        RenderPlayer renderPlayer = mc.getRenderManager().getSkinMap().get("default");
        RenderPlayer renderPlayerSlim = mc.getRenderManager().getSkinMap().get("slim");
        
        if (renderPlayer != null) {
            renderPlayer.addLayer(new BloodEuphoriaRenderer(renderPlayer));
        }
        
        if (renderPlayerSlim != null) {
            renderPlayerSlim.addLayer(new BloodEuphoriaRenderer(renderPlayerSlim));
        }
    }
}
