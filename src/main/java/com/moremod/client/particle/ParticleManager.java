package com.moremod.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 粒子管理器
 * 处理自定义粒子的创建和贴图绑定
 */
@SideOnly(Side.CLIENT)
public class ParticleManager {

    // 你购买的粒子贴图位置
    private static final ResourceLocation TEXTURE_LXS_LIGHT = 
        new ResourceLocation("moremod", "textures/particle/lxs_light1.png");

    /**
     * 创建螺旋粒子
     * 
     * @param world 世界
     * @param x 中心X坐标
     * @param y 中心Y坐标
     * @param z 中心Z坐标
     * @param red 红色 (0.0-1.0)
     * @param green 绿色 (0.0-1.0)
     * @param blue 蓝色 (0.0-1.0)
     * @param scale 粒子缩放
     */
    public static void spawnSpiralParticle(World world, double x, double y, double z,
                                          float red, float green, float blue, float scale) {
        if (world.isRemote) {
            ParticleSwordBeamSpiral particle = new ParticleSwordBeamSpiral(
                world, x, y, z,
                0, 0, 0,  // 运动向量（螺旋粒子会自己计算）
                red, green, blue, scale
            );
            
            Minecraft.getMinecraft().effectRenderer.addEffect(particle);
        }
    }

    /**
     * 批量生成螺旋粒子
     * 模仿基岩版的 spawn_rate: 100
     */
    public static void spawnSpiralParticleBurst(World world, double x, double y, double z,
                                               float red, float green, float blue, float scale) {
        if (world.isRemote) {
            // 每次生成5个粒子（100粒子/秒 ≈ 5粒子/tick）
            for (int i = 0; i < 5; i++) {
                spawnSpiralParticle(world, x, y, z, red, green, blue, scale);
            }
        }
    }

    /**
     * 绑定自定义粒子贴图
     * 在渲染粒子前调用
     */
    public static void bindParticleTexture() {
        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        textureManager.bindTexture(TEXTURE_LXS_LIGHT);
        
        // 设置贴图参数
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, 
                                GlStateManager.DestFactor.ONE); // Additive 混合
    }
}