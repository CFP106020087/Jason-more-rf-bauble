package com.moremod.client.render;

import com.moremod.client.model.ModelRiftwarden;
import com.moremod.entity.boss.riftwarden.EntityRiftwarden;
import com.moremod.entity.boss.riftwarden.RiftwardenState;
import com.moremod.entity.boss.riftwarden.RiftwardenAttackType;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

/**
 * Riftwarden 渲染器
 *
 * 更新以支持新的状态系统和视觉效果
 */
@SideOnly(Side.CLIENT)
public class RenderRiftwarden extends GeoEntityRenderer<EntityRiftwarden> {

    // 用于平滑过渡的插值变量
    private float smoothScale = 1.0F;
    private float smoothGlowR = 1.0F;
    private float smoothGlowG = 1.0F;
    private float smoothGlowB = 1.0F;
    private float smoothAlpha = 1.0F;

    // 插值速度
    private static final float LERP_SPEED = 0.1F;

    public RenderRiftwarden(RenderManager renderManager) {
        super(renderManager, new ModelRiftwarden());
        this.shadowSize = 0.7F;
    }

    @Override
    public void renderEarly(EntityRiftwarden entity, float ticks, float red, float green, float blue, float partialTicks) {
        super.renderEarly(entity, ticks, red, green, blue, partialTicks);

        // 禁用背面剔除
        GlStateManager.disableCull();

        // 获取状态信息
        RiftwardenState state = entity.getCurrentState();
        RiftwardenAttackType attack = entity.getStateManager().getCurrentAttack();
        int phase = entity.getPhase();
        float stateProgress = entity.getStateManager().getStateProgress();

        // 计算目标值
        float targetScale = calculateTargetScale(entity, state, phase);
        float[] targetColor = calculateTargetColor(entity, state, attack, phase, stateProgress);
        float targetAlpha = calculateTargetAlpha(entity, state);

        // 平滑插值
        smoothScale = lerp(smoothScale, targetScale, LERP_SPEED);
        smoothGlowR = lerp(smoothGlowR, targetColor[0], LERP_SPEED);
        smoothGlowG = lerp(smoothGlowG, targetColor[1], LERP_SPEED);
        smoothGlowB = lerp(smoothGlowB, targetColor[2], LERP_SPEED);
        smoothAlpha = lerp(smoothAlpha, targetAlpha, LERP_SPEED);

        // 应用缩放
        GlStateManager.scale(smoothScale, smoothScale, smoothScale);

        // 应用颜色
        GlStateManager.color(smoothGlowR, smoothGlowG, smoothGlowB, smoothAlpha);

        // 状态特效
        applyStateEffects(entity, state, attack, phase, stateProgress, partialTicks);

        // 悬浮效果（非移动时）
        if (!isEntityMoving(entity) && state.allowsMovement()) {
            float hover = MathHelper.sin((entity.ticksExisted + partialTicks) * 0.1F) * 0.03F;
            GlStateManager.translate(0, hover, 0);
        }
    }

    /**
     * 计算目标缩放
     */
    private float calculateTargetScale(EntityRiftwarden entity, RiftwardenState state, int phase) {
        float baseScale = 1.0F + phase * 0.1F;

        switch (state) {
            case EXHAUSTED:
                // 虚弱时缩小
                return baseScale * 0.95F;

            case CASTING_EXECUTE:
                // 施法时膨胀
                return baseScale * 1.05F;

            case MELEE_STRIKE:
                // 近战挥击时膨胀
                return baseScale * 1.08F;

            case STAGGERED:
                // 硬直时收缩
                return baseScale * 0.92F;

            case TELEPORTING:
                // 传送时闪烁缩放
                float teleportPulse = MathHelper.sin(entity.ticksExisted * 0.5F) * 0.1F;
                return baseScale + teleportPulse;

            case GATE_ACTIVE:
                // 锁血时护盾波动
                float shieldPulse = MathHelper.sin(entity.ticksExisted * 0.2F) * 0.03F;
                return baseScale + shieldPulse;

            default:
                return baseScale;
        }
    }

    /**
     * 计算目标颜色
     */
    private float[] calculateTargetColor(EntityRiftwarden entity, RiftwardenState state,
                                         RiftwardenAttackType attack, int phase, float stateProgress) {
        float r = 1.0F, g = 1.0F, b = 1.0F;

        // 基础阶段颜色
        switch (phase) {
            case 0:
                // 紫色基调
                r = 0.95F; g = 0.9F; b = 1.0F;
                break;
            case 1:
                // 蓝紫色
                r = 0.9F; g = 0.85F; b = 1.0F;
                break;
            case 2:
                // 偏蓝
                r = 0.85F; g = 0.9F; b = 1.0F;
                break;
            case 3:
                // 红紫色 - 狂暴
                r = 1.0F; g = 0.75F; b = 0.85F;
                break;
        }

        // 状态覆盖
        switch (state) {
            case EXHAUSTED:
                // 虚弱 - 暗淡灰紫
                r = 0.6F; g = 0.5F; b = 0.7F;
                break;

            case GATE_ACTIVE:
                // 锁血 - 紫色护盾
                float gatePulse = 0.5F + MathHelper.sin(entity.ticksExisted * 0.1F) * 0.3F;
                r = 0.5F + gatePulse * 0.2F;
                g = 0.0F + gatePulse * 0.1F;
                b = 0.8F + gatePulse * 0.2F;
                break;

            case CASTING_PREPARE:
            case CASTING_LOCKED:
                // 施法准备 - 逐渐发光
                float chargeGlow = 0.5F + stateProgress * 0.5F;
                r = chargeGlow;
                g = chargeGlow * 0.8F;
                b = 1.0F;
                break;

            case CASTING_EXECUTE:
                // 施法执行 - 根据攻击类型
                float[] attackColor = getAttackColor(attack);
                r = attackColor[0];
                g = attackColor[1];
                b = attackColor[2];
                break;

            case MELEE_WINDUP:
                // 近战蓄力 - 橙红色
                float meleeCharge = 0.7F + stateProgress * 0.3F;
                r = 1.0F;
                g = 0.6F + (1.0F - stateProgress) * 0.2F;
                b = 0.4F;
                break;

            case MELEE_STRIKE:
                // 近战挥击 - 明亮橙色
                r = 1.0F; g = 0.7F; b = 0.3F;
                break;

            case TELEPORTING:
                // 传送 - 闪烁紫色
                float teleportFlash = MathHelper.sin(entity.ticksExisted * 0.8F);
                r = 0.5F + teleportFlash * 0.3F;
                g = 0.0F;
                b = 0.8F + teleportFlash * 0.2F;
                break;

            case STAGGERED:
                // 硬直 - 闪烁白色
                float staggerFlash = MathHelper.sin(entity.ticksExisted * 0.6F) * 0.5F + 0.5F;
                r = 1.0F; g = staggerFlash; b = staggerFlash;
                break;
        }

        return new float[] { r, g, b };
    }

    /**
     * 根据攻击类型获取颜色
     */
    private float[] getAttackColor(RiftwardenAttackType attack) {
        switch (attack) {
            case LASER_BEAM:
                // 激光 - 亮蓝白
                return new float[] { 0.8F, 0.9F, 1.0F };

            case LIGHTNING_STRIKE:
            case LIGHTNING_ARC:
            case CHAIN_LIGHTNING:
                // 闪电 - 电蓝色
                return new float[] { 0.6F, 0.8F, 1.0F };

            case CHARGE_SHOOT:
                // 蓄力射击 - 紫色
                return new float[] { 0.9F, 0.5F, 1.0F };

            case SPIRAL_BULLETS:
            case WAVE_BULLETS:
                // 弹幕 - 深紫
                return new float[] { 0.7F, 0.4F, 0.9F };

            case BURST_BULLETS:
            case PREDICTIVE_SHOT:
                // 爆发/预判 - 亮紫
                return new float[] { 0.85F, 0.6F, 1.0F };

            default:
                return new float[] { 0.9F, 0.8F, 1.0F };
        }
    }

    /**
     * 计算目标透明度
     */
    private float calculateTargetAlpha(EntityRiftwarden entity, RiftwardenState state) {
        switch (state) {
            case TELEPORTING:
                // 传送时半透明闪烁
                return 0.5F + MathHelper.sin(entity.ticksExisted * 0.5F) * 0.3F;

            case GATE_ACTIVE:
                // 锁血时微透明
                return 0.7F + MathHelper.sin(entity.ticksExisted * 0.1F) * 0.2F;

            case EXHAUSTED:
                // 虚弱时略透明
                return 0.85F;

            default:
                return 1.0F;
        }
    }

    /**
     * 应用状态特效
     */
    private void applyStateEffects(EntityRiftwarden entity, RiftwardenState state,
                                   RiftwardenAttackType attack, int phase,
                                   float stateProgress, float partialTicks) {

        float time = entity.ticksExisted + partialTicks;

        switch (state) {
            case CASTING_PREPARE:
            case CASTING_LOCKED:
                // 蓄力时的震动效果
                if (stateProgress > 0.5F) {
                    float intensity = (stateProgress - 0.5F) * 0.06F;
                    applyShake(intensity, time);
                }
                break;

            case CASTING_EXECUTE:
                // 施法时的强震动
                if (attack == RiftwardenAttackType.LASER_BEAM) {
                    applyShake(0.08F, time * 2);
                } else {
                    applyShake(0.04F, time);
                }
                break;

            case MELEE_STRIKE:
                // 挥击时的震动
                applyShake(0.05F, time * 1.5F);
                break;

            case EXHAUSTED:
                // 虚弱时的呼吸起伏
                float breathe = MathHelper.sin(time * 0.15F) * 0.02F;
                GlStateManager.translate(0, breathe, 0);
                // 略微前倾
                GlStateManager.rotate(5.0F, 1, 0, 0);
                break;

            case STAGGERED:
                // 硬直时的晃动
                float staggerAngle = MathHelper.sin(time * 0.3F) * 8.0F;
                GlStateManager.rotate(staggerAngle, 0, 0, 1);
                break;

            case TELEPORTING:
                // 传送时的旋转消散
                float teleportSpin = stateProgress * 720F;
                GlStateManager.rotate(teleportSpin, 0, 1, 0);
                // 压扁效果
                float squash = 1.0F - stateProgress * 0.5F;
                GlStateManager.scale(1.0F + stateProgress * 0.3F, squash, 1.0F + stateProgress * 0.3F);
                break;

            case GATE_ACTIVE:
                // 锁血时启用混合
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(
                        GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
                );
                break;
        }

        // 球体光晕效果（阶段2+）
        if (phase >= 2) {
            float ballGlow = 0.9F + MathHelper.sin(time * 0.05F) * 0.1F;
            // 这会影响整体亮度，可以考虑分离球体渲染
        }
    }

    /**
     * 应用震动效果
     */
    private void applyShake(float intensity, float time) {
        // 使用时间生成伪随机但平滑的震动
        float shakeX = MathHelper.sin(time * 1.7F) * intensity;
        float shakeY = MathHelper.sin(time * 2.3F) * intensity * 0.5F;
        float shakeZ = MathHelper.sin(time * 1.9F) * intensity;

        GlStateManager.translate(shakeX, shakeY, shakeZ);
    }

    /**
     * 检查实体是否在移动
     */
    private boolean isEntityMoving(EntityRiftwarden entity) {
        double speedSq = entity.motionX * entity.motionX + entity.motionZ * entity.motionZ;
        return speedSq > 0.001;
    }

    /**
     * 线性插值
     */
    private float lerp(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    @Override
    public void doRender(EntityRiftwarden entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.disableCull();

        // 渲染主体
        super.doRender(entity, x, y, z, entityYaw, partialTicks);

        // 渲染额外效果层
        renderOverlayEffects(entity, x, y, z, partialTicks);

        GlStateManager.enableCull();
        GlStateManager.popMatrix();

        // 重置状态
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }

    /**
     * 渲染覆盖效果层
     */
    private void renderOverlayEffects(EntityRiftwarden entity, double x, double y, double z, float partialTicks) {
        RiftwardenState state = entity.getCurrentState();

        // 锁血护盾效果
        if (state == RiftwardenState.GATE_ACTIVE || entity.isGateActive()) {
            renderGateShield(entity, x, y, z, partialTicks);
        }

        // 虚弱光环
        if (state == RiftwardenState.EXHAUSTED) {
            renderExhaustionAura(entity, x, y, z, partialTicks);
        }

        // 激光瞄准线
        if (entity.getCombatController().isFiringLaser()) {
            renderLaserBeam(entity, x, y, z, partialTicks);
        }
    }

    /**
     * 渲染锁血护盾
     */


    /**
     * 渲染虚弱光环
     */
    private void renderExhaustionAura(EntityRiftwarden entity, double x, double y, double z, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + 0.1, z);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );

        // 脚下的虚弱光环
        float pulse = MathHelper.sin((entity.ticksExisted + partialTicks) * 0.1F) * 0.2F + 0.5F;
        GlStateManager.color(0.8F, 0.6F, 0.2F, pulse * 0.5F);

        // 实际绘制代码...

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 渲染激光束
     */
    private void renderLaserBeam(EntityRiftwarden entity, double x, double y, double z, float partialTicks) {
        net.minecraft.entity.player.EntityPlayer target = entity.getCombatController().getLaserTarget();
        if (target == null) return;

        // 计算手部位置
        float yawRad = (float) Math.toRadians(entity.rotationYaw);
        double handOffsetX = -Math.sin(yawRad + Math.PI / 2) * 0.5;
        double handOffsetZ = Math.cos(yawRad + Math.PI / 2) * 0.5;

        Vec3d startPos = new Vec3d(
                handOffsetX,
                entity.height * 0.7,
                handOffsetZ
        );

        Vec3d endPos = new Vec3d(
                target.posX - entity.posX,
                target.posY + target.height * 0.5 - entity.posY,
                target.posZ - entity.posZ
        );

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        float time = entity.ticksExisted + partialTicks;
        float pulse = MathHelper.sin(time * 0.5F) * 0.2F + 0.8F;

        LaserBeamRenderer.renderBeam(
                startPos, endPos,
                0.3F,  // 宽度
                time,
                pulse, pulse, 1.0F, 0.9F  // 蓝白色
        );

        GlStateManager.popMatrix();
    }

    /**
     * 渲染锁血护盾 - 使用辅助类
     */
    private void renderGateShield(EntityRiftwarden entity, double x, double y, double z, float partialTicks) {
        float progress = entity.getGateProgress();
        if (progress <= 0) return;

        float time = entity.ticksExisted + partialTicks;
        float rotation = time * 3.0F;
        float alpha = 0.3F + progress * 0.4F;
        float radius = 1.5F + MathHelper.sin(time * 0.2F) * 0.2F;

        // 渲染多层护盾环
        for (int i = 0; i < 3; i++) {
            float layerOffset = i * 0.3F;
            float layerRotation = rotation + i * 60F;
            float layerAlpha = alpha * (1.0F - i * 0.2F);

            LaserBeamRenderer.renderShieldRing(
                    x, y + entity.height * 0.5 + layerOffset, z,
                    radius + i * 0.2F,
                    layerRotation,
                    0.5F, 0.0F, 1.0F, layerAlpha
            );
        }
    }

    @Override
    public void renderLate(EntityRiftwarden entity, float ticks, float red, float green, float blue, float partialTicks) {
        super.renderLate(entity, ticks, red, green, blue, partialTicks);

        // 恢复状态
        GlStateManager.enableCull();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }

    /**
     * 获取纹理位置 - 可根据状态切换纹理
     */
    @Override
    public net.minecraft.util.ResourceLocation getTextureLocation(EntityRiftwarden entity) {
        // 可以根据状态返回不同纹理
        // 例如虚弱状态使用暗化纹理
        return super.getTextureLocation(entity);
    }
}