package com.moremod.client.render;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.client.model.MechanicalCoreModel;
import com.moremod.item.ItemMechanicalCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 机械核心渲染层 - 稳定版
 * 关键修复：
 * 1) setLivingAnimations 使用 partialTicks
 * 2) ageInTicks 做取模后用于动画相位，避免长时精度劣化
 * 3) 渲染侧激活判定降权，避免因客户端不同步而停动画
 */
@SideOnly(Side.CLIENT)
public class RenderLayerMechanicalCore implements LayerRenderer<EntityLivingBase> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("moremod", "textures/models/mechanical_core.png");
    private final MechanicalCoreModel model = new MechanicalCoreModel();

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (!(entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) entity;
        ItemStack coreStack = getMechanicalCore(player);
        if (!coreStack.isEmpty()) {
            renderMechanicalCore(player, coreStack, limbSwing, limbSwingAmount, partialTicks,
                    ageInTicks, netHeadYaw, headPitch, scale);
        }
    }

    /**
     * 获取玩家装备的机械核心
     */
    private ItemStack getMechanicalCore(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (stack.getItem() instanceof ItemMechanicalCore) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 渲染机械核心模型
     */
    private void renderMechanicalCore(EntityPlayer player, ItemStack stack, float limbSwing,
                                      float limbSwingAmount, float partialTicks, float ageInTicks,
                                      float netHeadYaw, float headPitch, float scale) {

        GlStateManager.pushMatrix();

        if (player.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);

        GlStateManager.rotate(netHeadYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(headPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.0F, -0.35F, 0.0F);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        // === 渲染侧轻量激活判定（避免因不同步而熄灭） ===
        boolean isActive = false;
        float energyRatio = 0f;

        int stored = 0, max = 0;
        net.minecraftforge.energy.IEnergyStorage energy =
                stack.getCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            stored = energy.getEnergyStored();
            max = energy.getMaxEnergyStored();
        } else if (stack.hasTagCompound()) {
            // 兜底：直接读 NBT 镜像
            stored = stack.getTagCompound().getInteger("Energy");
        }
        boolean hasEnergy = stored > 0;
        if (max > 0) energyRatio = (float) stored / (float) max;

        boolean hasAnyGenerator = stack.hasTagCompound() && (
                stack.getTagCompound().getInteger("upgrade_KINETIC_GENERATOR") > 0
                        || stack.getTagCompound().getInteger("upgrade_SOLAR_GENERATOR")   > 0
                        || stack.getTagCompound().getInteger("upgrade_VOID_ENERGY")       > 0
                        || stack.getTagCompound().getInteger("upgrade_COMBAT_CHARGER")    > 0
        );

        isActive = hasEnergy || hasAnyGenerator;
        model.setActive(isActive);

        // === 动画时间取模，避免 float 精度劣化 & 极端跳变 ===
        float t = looped(ageInTicks);

        // ✅ 这里必须用 partialTicks（而不是 ageInTicks）
        model.setLivingAnimations(player, limbSwing, limbSwingAmount, partialTicks);

        // 发光/颜色
        if (isActive) {
            if (energyRatio > 0.6f) {
                GlStateManager.color(0.5f, 1.0f, 1.0f, 1.0f);
            } else if (energyRatio > 0.3f) {
                GlStateManager.color(1.0f, 1.0f, 0.5f, 1.0f);
            } else {
                GlStateManager.color(1.0f, 0.5f, 0.5f, 1.0f);
            }

            // 脉冲：使用取模后的时间 t
            float pulse = (float) (Math.sin(t * 0.1) * 0.1 + 0.9);
            GlStateManager.scale(pulse, pulse, pulse);
        } else {
            GlStateManager.color(0.6f, 0.6f, 0.6f, 0.8f);
        }

        // ✅ render 的第4参是 ageInTicks；这里用 t 更稳
        model.render(player, limbSwing, limbSwingAmount, t, netHeadYaw, headPitch, scale);

        if (isActive) {
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

            float glowScale = 1.05f;
            GlStateManager.scale(glowScale, glowScale, glowScale);
            GlStateManager.color(0.5f, 0.8f, 1.0f, 0.3f);

            model.render(player, limbSwing, limbSwingAmount, t, netHeadYaw, headPitch, scale);

            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 时间取模（一天 24000 tick）
     */
    private static float looped(float ageInTicks) {
        double loop = 24000.0;
        double x = ageInTicks % loop;
        if (x < 0) x += loop;
        return (float) x;
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
