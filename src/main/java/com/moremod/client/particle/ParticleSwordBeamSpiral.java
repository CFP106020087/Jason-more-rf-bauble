package com.moremod.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * 自定义螺旋粒子
 * 使用购买的 lxs_light 贴图
 */
@SideOnly(Side.CLIENT)
public class ParticleSwordBeamSpiral extends Particle {

    // 粒子的初始数据
    private final float initialScale;
    private final float red;
    private final float green;
    private final float blue;
    
    // 螺旋参数
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final float spiralRadius = 0.8F;
    
    public ParticleSwordBeamSpiral(World world, 
                                   double x, double y, double z,
                                   double motionX, double motionY, double motionZ,
                                   float red, float green, float blue, float scale) {
        super(world, x, y, z, motionX, motionY, motionZ);
        
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.initialScale = scale;
        
        // 设置粒子属性
        this.particleMaxAge = 44; // 2.2秒 = 44 ticks
        this.particleScale = scale * 0.2F;
        this.particleAlpha = 1.0F;
        
        // 设置贴图（稍后在渲染时绑定）
        this.setParticleTextureIndex(0);
        
        // 记录中心点（用于螺旋运动）
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        
        // 不受重力影响
        this.particleGravity = 0.0F;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
            return;
        }

        // 计算粒子年龄进度 (0.0 到 1.0)
        float ageProgress = (float) this.particleAge / (float) this.particleMaxAge;

        // 螺旋运动（模仿基岩版公式）
        // cos(age*1440)*0.8 和 sin(age*1440)*0.8
        double angle = ageProgress * 1440.0 * Math.PI / 180.0; // 转换为弧度
        
        double offsetX = Math.cos(angle) * spiralRadius * (1.0 - ageProgress * 0.5);
        double offsetZ = Math.sin(angle) * spiralRadius * (1.0 - ageProgress * 0.5);
        
        // 向上运动（模仿 pow(age*5, 1.1)）
        double offsetY = Math.pow(ageProgress * 5.0, 1.1);

        // 应用螺旋位置
        this.posX = centerX + offsetX;
        this.posY = centerY + offsetY;
        this.posZ = centerZ + offsetZ;

        // 透明度淡出
        this.particleAlpha = 1.0F - ageProgress;
        
        // 缩放变化（可选）
        this.particleScale = initialScale * 0.2F * (1.0F + ageProgress * 0.5F);
    }

    @Override
    public int getFXLayer() {
        // 使用自定义贴图层
        return 3;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn,
                              float partialTicks, float rotationX, float rotationZ,
                              float rotationYZ, float rotationXY, float rotationXZ) {
        
        // 计算插值位置
        float x = (float) (this.prevPosX + (this.posX - this.prevPosX) * partialTicks - interpPosX);
        float y = (float) (this.prevPosY + (this.posY - this.prevPosY) * partialTicks - interpPosY);
        float z = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - interpPosZ);

        // 设置颜色
        buffer.pos(x - rotationX * particleScale - rotationXY * particleScale,
                  y - rotationZ * particleScale,
                  z - rotationYZ * particleScale - rotationXZ * particleScale)
              .tex(0, 1)
              .color(red, green, blue, particleAlpha)
              .lightmap(240, 240) // 自发光
              .endVertex();

        buffer.pos(x - rotationX * particleScale + rotationXY * particleScale,
                  y + rotationZ * particleScale,
                  z - rotationYZ * particleScale + rotationXZ * particleScale)
              .tex(1, 1)
              .color(red, green, blue, particleAlpha)
              .lightmap(240, 240)
              .endVertex();

        buffer.pos(x + rotationX * particleScale + rotationXY * particleScale,
                  y + rotationZ * particleScale,
                  z + rotationYZ * particleScale + rotationXZ * particleScale)
              .tex(1, 0)
              .color(red, green, blue, particleAlpha)
              .lightmap(240, 240)
              .endVertex();

        buffer.pos(x + rotationX * particleScale - rotationXY * particleScale,
                  y - rotationZ * particleScale,
                  z + rotationYZ * particleScale - rotationXZ * particleScale)
              .tex(0, 0)
              .color(red, green, blue, particleAlpha)
              .lightmap(240, 240)
              .endVertex();
    }
}