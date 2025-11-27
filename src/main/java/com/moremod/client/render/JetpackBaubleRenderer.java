package com.moremod.client.render;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemJetpackBauble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 修复版喷气背包渲染器 - 解决模型位置问题
 */
@SideOnly(Side.CLIENT)
public class JetpackBaubleRenderer {

    // 控制使用哪种渲染方式
    private static final boolean USE_FLYBAG_MODEL = true;

    // 修复版 Flybag 模型 - 调整原点位置
    private static class ModelFlybagFixed extends ModelBase {
        private final ModelRenderer bb_main;
        private final ModelRenderer leftTank;
        private final ModelRenderer rightTank;
        private final ModelRenderer leftThruster;
        private final ModelRenderer rightThruster;

        public ModelFlybagFixed() {
            textureWidth = 64;
            textureHeight = 64;

            // 主体容器 - 调整原点到合适位置
            bb_main = new ModelRenderer(this);
            bb_main.setRotationPoint(0.0F, 0.0F, 0.0F); // 改为 0,0,0 原点

            // 控制面板
            ModelRenderer controlPanel = new ModelRenderer(this, 24, 13);
            controlPanel.addBox(-1.0F, 1.0F, 2.0F, 2, 2, 2, 0.0F); // Z改为正值
            bb_main.addChild(controlPanel);

            // 连接器
            ModelRenderer connector = new ModelRenderer(this, 24, 4);
            connector.addBox(-1.0F, 3.0F, 2.0F, 2, 6, 3, 0.0F); // Z改为正值
            bb_main.addChild(connector);

            // 右侧燃料罐
            rightTank = new ModelRenderer(this, 0, 0);
            rightTank.addBox(1.0F, 0.5F, 2.0F, 4, 9, 4, 0.0F); // Z改回正值
            bb_main.addChild(rightTank);

            // 左侧燃料罐
            leftTank = new ModelRenderer(this, 0, 13);
            leftTank.addBox(-5.0F, 0.5F, 2.0F, 4, 9, 4, 0.0F); // Z改回正值
            bb_main.addChild(leftTank);

            // 左喷射器
            leftThruster = new ModelRenderer(this, 16, 0);
            leftThruster.addBox(-4.0F, 0.0F, 3.0F, 2, 11, 2, 0.0F); // Z改为正值
            bb_main.addChild(leftThruster);

            // 右喷射器
            rightThruster = new ModelRenderer(this, 16, 13);
            rightThruster.addBox(2.0F, 0.0F, 3.0F, 2, 11, 2, 0.0F); // Z改为正值
            bb_main.addChild(rightThruster);

            // 顶部装饰板
            ModelRenderer topPlate = new ModelRenderer(this, 24, 0);
            topPlate.addBox(-4.0F, -3.0F, 4.0F, 8, 4, 0, 0.0F); // Z保持正值
            bb_main.addChild(topPlate);
        }

        @Override
        public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
            // 添加动画效果
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (!player.onGround && player.motionY > 0) {
                    // 飞行时喷射器轻微振动
                    float vibration = (float)Math.sin(f2 * 0.5) * 0.02F;
                    leftThruster.rotateAngleZ = vibration;
                    rightThruster.rotateAngleZ = -vibration;
                }
            }

            bb_main.render(f5);
        }
    }

    private final ModelFlybagFixed flybagModel = new ModelFlybagFixed();

    // 纹理路径
    private static final ResourceLocation TEXTURE_T1 = new ResourceLocation("moremod", "textures/models/jetpack_t1.png");
    private static final ResourceLocation TEXTURE_T2 = new ResourceLocation("moremod", "textures/models/jetpack_t2.png");
    private static final ResourceLocation TEXTURE_T3 = new ResourceLocation("moremod", "textures/models/jetpack_t3.png");
    private static final ResourceLocation TEXTURE_CREATIVE = new ResourceLocation("moremod", "textures/models/jetpack_creative.png");
    private static final ResourceLocation TEXTURE_DEFAULT = new ResourceLocation("moremod", "textures/models/jetpack.png");

    @SubscribeEvent
    public void onPlayerRender(RenderPlayerEvent.Post event) {
        EntityPlayer player = event.getEntityPlayer();
        RenderPlayer renderer = event.getRenderer();

        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return;

        ItemStack stack = baubles.getStackInSlot(5);
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemJetpackBauble)) return;

        ItemJetpackBauble jetpack = (ItemJetpackBauble) stack.getItem();

        if (USE_FLYBAG_MODEL) {
            renderJetpackWithFlybagModel(player, stack, jetpack, event.getPartialRenderTick(), renderer);
        } else {
            renderJetpackWithItemModel(player, stack, jetpack, event.getPartialRenderTick(), renderer);
        }
    }

    /**
     * 使用 Flybag 模型渲染 - 修复版
     */
    private void renderJetpackWithFlybagModel(EntityPlayer player, ItemStack stack, ItemJetpackBauble jetpack,
                                              float partialTicks, RenderPlayer renderer) {
        GlStateManager.pushMatrix();

        // 获取纹理
        ResourceLocation texture = getTextureForJetpack(stack, jetpack);
        Minecraft.getMinecraft().renderEngine.bindTexture(texture);

        // 计算插值位置和旋转
        float yaw = interpolateRotation(player.prevRenderYawOffset, player.renderYawOffset, partialTicks);
        double x = interpolate(player.prevPosX, player.posX, partialTicks) - Minecraft.getMinecraft().getRenderManager().viewerPosX;
        double y = interpolate(player.prevPosY, player.posY, partialTicks) - Minecraft.getMinecraft().getRenderManager().viewerPosY;
        double z = interpolate(player.prevPosZ, player.posZ, partialTicks) - Minecraft.getMinecraft().getRenderManager().viewerPosZ;

        // 移动到玩家位置
        GlStateManager.translate(x, y, z);

        // 旋转以匹配玩家朝向
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);

        // 调整到背部位置
        // Y轴：调整高度使其在背部中间
        // Z轴：负值向前（胸前），正值向后（背部）
        if (player.isSneaking()) {
            // 潜行时的调整
            GlStateManager.translate(0.0F, 1.1F, -0.1F); // Z从-0.3改为-0.2，更贴近背部
            GlStateManager.rotate(30.0F, 1.0F, 0.0F, 0.0F); // 潜行时模型前倾
        } else {
            // 站立时的位置
            GlStateManager.translate(0.0F, 1.3F, -0.1F); // Z从-0.35改为-0.25，更贴近背部
        }

        // 缩放模型（Blockbench 模型通常需要缩小）
        float modelScale = 0.0625F; // 1/16 - Minecraft 标准缩放
        GlStateManager.scale(modelScale, modelScale, modelScale);

        // 额外的微调旋转 - 不需要180度旋转了
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F); // 绕X轴旋转180度，翻转模型

        // 启用混合和颜色
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        // 如果喷气背包启用，添加发光效果
        if (isJetpackEnabled(stack)) {
            float pulse = (float)(Math.sin(System.currentTimeMillis() * 0.002) * 0.1 + 0.9);
            GlStateManager.color(pulse, pulse, pulse, 1.0F);

            // 速度模式的颜色效果
            ItemJetpackBauble.SpeedMode speedMode = jetpack.getSpeedMode(stack);
            if (speedMode == ItemJetpackBauble.SpeedMode.ULTRA) {
                GlStateManager.color(pulse, pulse * 0.8F, pulse, 1.0F); // 紫色调
            } else if (speedMode == ItemJetpackBauble.SpeedMode.FAST) {
                GlStateManager.color(pulse, pulse * 0.95F, pulse * 0.7F, 1.0F); // 金色调
            }
        }

        // 渲染模型
        flybagModel.render(player, 0, 0, player.ticksExisted, 0, 0, 1.0F);

        // 恢复状态
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        // 渲染粒子效果
        if (isJetpackEnabled(stack)) {
            renderJetpackParticles(player, jetpack, stack);
        }
    }

    /**
     * 使用物品模型渲染（原方法保持不变）
     */
    private void renderJetpackWithItemModel(EntityPlayer player, ItemStack stack, ItemJetpackBauble jetpack,
                                            float partialTicks, RenderPlayer renderer) {
        GlStateManager.pushMatrix();

        float yaw = interpolateRotation(player.prevRenderYawOffset, player.renderYawOffset, partialTicks);
        double x = interpolate(player.prevPosX, player.posX, partialTicks) - Minecraft.getMinecraft().getRenderManager().viewerPosX;
        double y = interpolate(player.prevPosY, player.posY, partialTicks) - Minecraft.getMinecraft().getRenderManager().viewerPosY;
        double z = interpolate(player.prevPosZ, player.posZ, partialTicks) - Minecraft.getMinecraft().getRenderManager().viewerPosZ;

        GlStateManager.translate(x, y + 0.6F, z);
        GlStateManager.rotate(-yaw + 180.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(0.0F, -0.0825F, 0.75F);
        GlStateManager.scale(1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);

        Minecraft.getMinecraft().getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);
        net.minecraft.client.renderer.block.model.IBakedModel bakedModel =
                Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(stack, player.world, player);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        GlStateManager.disableCull();

        if (isJetpackEnabled(stack)) {
            float pulse = (float)(Math.sin(System.currentTimeMillis() * 0.002) * 0.1 + 0.9);
            GlStateManager.color(pulse, pulse, pulse, 1.0F);
        }

        Minecraft.getMinecraft().getRenderItem().renderItem(stack,
                net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.NONE);

        GlStateManager.enableCull();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        if (isJetpackEnabled(stack)) {
            renderJetpackParticles(player, jetpack, stack);
        }
    }

    /**
     * 根据喷气背包获取纹理
     */
    private ResourceLocation getTextureForJetpack(ItemStack stack, ItemJetpackBauble jetpack) {
        int tier = jetpack.getTier();
        String itemName = stack.getItem().getRegistryName().getPath();

        if (itemName.contains("creative")) {
            return TEXTURE_CREATIVE;
        }

        // 先尝试使用特定等级纹理，如果不存在就用默认纹理
        switch (tier) {
            case 1: return TEXTURE_T1;
            case 2: return TEXTURE_T2;
            case 3: return TEXTURE_T3;
            default: return TEXTURE_DEFAULT;
        }
    }

    private boolean isJetpackEnabled(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.getBoolean("JetpackEnabled");
    }

    // ========== 粒子效果代码保持不变 ==========

    private void renderJetpackParticles(EntityPlayer player, ItemJetpackBauble jetpack, ItemStack stack) {
        if (!player.world.isRemote) return;

        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        double backOffset = 0.3; // 减小偏移，使粒子更贴近模型
        double heightOffset = player.height * 0.5; // 调整高度

        double backX = Math.sin(yawRad) * backOffset;
        double backZ = -Math.cos(yawRad) * backOffset;

        double jetpackWidth = 0.2; // 喷口间距

        double leftRightX = Math.cos(yawRad);
        double leftRightZ = Math.sin(yawRad);

        double leftX = player.posX + backX + leftRightX * jetpackWidth;
        double leftY = player.posY + heightOffset;
        double leftZ = player.posZ + backZ + leftRightZ * jetpackWidth;

        double rightX = player.posX + backX - leftRightX * jetpackWidth;
        double rightY = player.posY + heightOffset;
        double rightZ = player.posZ + backZ - leftRightZ * jetpackWidth;

        if (player.isSneaking()) {
            leftY -= 0.25;
            rightY -= 0.25;
        }

        double jetVelocityX = Math.sin(yawRad) * Math.sin(pitchRad) * 0.3;
        double jetVelocityY = -0.5 - Math.cos(pitchRad) * 0.2;
        double jetVelocityZ = -Math.cos(yawRad) * Math.sin(pitchRad) * 0.3;

        boolean isFlying = !player.onGround || player.motionY > 0.01;
        NBTTagCompound nbt = stack.getTagCompound();
        boolean isHovering = nbt != null && nbt.getBoolean("HoverEnabled");

        if (isFlying) {
            spawnThrusterParticles(player.world, leftX, leftY, leftZ, jetVelocityX, jetVelocityY, jetVelocityZ, player.motionY);
            spawnThrusterParticles(player.world, rightX, rightY, rightZ, jetVelocityX, jetVelocityY, jetVelocityZ, player.motionY);
        } else if (isHovering) {
            spawnHoverParticles(player.world, leftX, leftY, leftZ);
            spawnHoverParticles(player.world, rightX, rightY, rightZ);
        }
    }

    private void spawnThrusterParticles(net.minecraft.world.World world, double x, double y, double z,
                                        double vx, double vy, double vz, double playerMotionY) {
        int particleCount = playerMotionY > 0.3 ? 6 : (playerMotionY > 0.1 ? 4 : 2);

        for (int i = 0; i < particleCount; i++) {
            double offsetRadius = 0.05;
            double offsetX = (Math.random() - 0.5) * offsetRadius;
            double offsetY = (Math.random() - 0.5) * offsetRadius;
            double offsetZ = (Math.random() - 0.5) * offsetRadius;

            double velocityVariation = 0.1;
            double particleVx = vx + (Math.random() - 0.5) * velocityVariation;
            double particleVy = vy + (Math.random() - 0.5) * velocityVariation * 0.5;
            double particleVz = vz + (Math.random() - 0.5) * velocityVariation;

            world.spawnParticle(EnumParticleTypes.FLAME,
                    x + offsetX, y + offsetY, z + offsetZ,
                    particleVx, particleVy, particleVz);
        }

        if (Math.random() < 0.3) {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    x, y - 0.1, z,
                    vx * 0.2, vy * 0.3, vz * 0.2);
        }

        if (playerMotionY > 0.5 && Math.random() < 0.5) {
            world.spawnParticle(EnumParticleTypes.CLOUD,
                    x, y - 0.2, z,
                    0, -0.2, 0);
        }
    }

    private void spawnHoverParticles(net.minecraft.world.World world, double x, double y, double z) {
        for (int i = 0; i < 2; i++) {
            double offsetRadius = 0.03;
            double offsetX = (Math.random() - 0.5) * offsetRadius;
            double offsetZ = (Math.random() - 0.5) * offsetRadius;

            world.spawnParticle(EnumParticleTypes.FLAME,
                    x + offsetX, y, z + offsetZ,
                    0, -0.3, 0);
        }

        if (Math.random() < 0.2) {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    x, y - 0.1, z,
                    0, -0.2, 0);
        }
    }

    private float interpolate(float prev, float current, float partialTicks) {
        return prev + (current - prev) * partialTicks;
    }

    private double interpolate(double prev, double current, float partialTicks) {
        return prev + (current - prev) * partialTicks;
    }

    private float interpolateRotation(float prevYaw, float yaw, float partialTicks) {
        float f;
        for (f = yaw - prevYaw; f < -180.0F; f += 360.0F) {}
        while (f >= 180.0F) { f -= 360.0F; }
        return prevYaw + partialTicks * f;
    }
}