package com.moremod.item.sawblade.client;

import com.moremod.item.sawblade.potion.PotionBloodEuphoria;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 锯刃剑 - GeckoLib发光控制器
 * 
 * 高级实现方式：
 * 1. 通过GeckoLib的动画系统控制发光层显示/隐藏
 * 2. 发光强度随buff层数变化
 * 3. 脉冲呼吸效果
 * 
 * 使用说明：
 * 1. 在Blockbench中为锯刃剑模型添加发光纹理层（Emissive Texture）
 * 2. 将发光层命名为 "glow" 或 "emissive"
 * 3. 在渲染器中调用此类的方法控制显示
 * 
 * 注意：需要配合SawBladeSwordRenderer使用
 */


@SideOnly(Side.CLIENT)
public class SawBladeEmissiveHandler {
    
    // 发光强度缓存（避免频繁计算）
    private static float cachedGlowIntensity = 0.0f;
    private static long lastUpdateTick = 0L;
    
    /**
     * 检查是否应该显示发光效果
     * 
     * @param stack 锯刃剑
     * @return true=显示发光, false=不显示
     */
    public static boolean shouldGlow(ItemStack stack) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return false;
        
        return PotionBloodEuphoria.hasEffect(player);
    }
    
    /**
     * 获取发光强度（0.0-1.0）
     * 
     * @param stack 锯刃剑
     * @param partialTicks 帧间插值
     * @return 发光强度
     */
    public static float getGlowIntensity(ItemStack stack, float partialTicks) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || !PotionBloodEuphoria.hasEffect(player)) {
            return 0.0f;
        }
        
        // 获取buff层数
        int stacks = PotionBloodEuphoria.getStacks(player);
        if (stacks <= 0) return 0.0f;
        
        // 基础强度（基于层数）
        float baseIntensity = 0.5f + (stacks * 0.25f);  // 1层50%，2层75%，3层100%
        
        // 脉冲效果（呼吸）
        long worldTime = player.world.getTotalWorldTime();
        float pulse = (float)(Math.sin((worldTime + partialTicks) * 0.1) * 0.3 + 0.7);
        
        return baseIntensity * pulse;
    }
    
    /**
     * 获取发光颜色（RGB）
     * 
     * @param stack 锯刃剑
     * @return [R, G, B] 0.0-1.0
     */
    public static float[] getGlowColor(ItemStack stack) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return new float[]{1.0f, 0.0f, 0.0f};
        
        int stacks = PotionBloodEuphoria.getStacks(player);
        
        // 颜色随层数变化
        if (stacks >= 3) {
            // 3层：深红色
            return new float[]{0.8f, 0.0f, 0.0f};
        } else if (stacks >= 2) {
            // 2层：亮红色
            return new float[]{1.0f, 0.0f, 0.0f};
        } else {
            // 1层：浅红色
            return new float[]{1.0f, 0.3f, 0.3f};
        }
    }
    
    /**
     * 在GeckoLib渲染器中调用此方法设置发光效果
     * 
     * 集成示例（在SawBladeSwordRenderer中）：
     * 
     * <pre>
     * {@code
     * @Override
     * public void render(...) {
     *     // 设置发光效果
     *     if (SawBladeEmissiveHandler.shouldGlow(stack)) {
     *         float intensity = SawBladeEmissiveHandler.getGlowIntensity(stack, partialTicks);
     *         float[] color = SawBladeEmissiveHandler.getGlowColor(stack);
     *         
     *         // 应用到GeckoLib渲染
     *         GlStateManager.color(color[0], color[1], color[2], intensity);
     *         // ... 渲染发光层
     *     }
     *     
     *     // 渲染主模型
     *     super.render(...);
     * }
     * }
     * </pre>
     */
    public static void applyGlow(ItemStack stack, float partialTicks) {
        if (!shouldGlow(stack)) return;
        
        float intensity = getGlowIntensity(stack, partialTicks);
        float[] color = getGlowColor(stack);
        
        // 这个方法会在SawBladeSwordRenderer中被调用
        // 实际的渲染逻辑在渲染器中实现
    }
    
    /**
     * 生成粒子效果（可选）
     * 在武器周围生成红色粒子
     */
    public static void spawnParticles(EntityPlayer player, ItemStack stack) {
        if (!shouldGlow(stack)) return;
        if (player.world.isRemote && player.world.rand.nextFloat() < 0.3f) {
            
            int stacks = PotionBloodEuphoria.getStacks(player);
            
            // 粒子数量随层数增加
            int particleCount = stacks;
            
            for (int i = 0; i < particleCount; i++) {
                double offsetX = (player.world.rand.nextDouble() - 0.5) * 0.5;
                double offsetY = player.world.rand.nextDouble() * 0.5;
                double offsetZ = (player.world.rand.nextDouble() - 0.5) * 0.5;
                
                // 手部位置
                double x = player.posX + offsetX;
                double y = player.posY + 1.0 + offsetY;
                double z = player.posZ + offsetZ;
                
                player.world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.REDSTONE,
                    x, y, z,
                    0, 0, 0
                );
            }
        }
    }
}
