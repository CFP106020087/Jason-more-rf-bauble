package com.moremod.client.render.fx;

import com.moremod.entity.fx.EntityLaserBeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RenderLaserBeam extends Render<EntityLaserBeam> {

    private static final ResourceLocation TEX =
            new ResourceLocation("moremod", "textures/effects/laser_beam.png");

    private static final int SEGMENTS = 16;

    public RenderLaserBeam(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0F;
    }

    @Override
    public void doRender(EntityLaserBeam beam, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        Entity owner = beam.getOwnerEntity();
        if (owner == null) return;

        // 获取起点和终点
        Vec3d start = getBeamStartPoint(owner, partialTicks);
        Vec3d end = getBeamEndPoint(beam, partialTicks);
        if (end == null) return;

        // 计算距离
        double distance = start.distanceTo(end);

        // 获取激光属性
        LaserProperties props = getLaserProperties(beam, owner);

        // 只在范围内渲染
        if (distance > props.maxRenderDistance) {
            return;
        }

        // 转换到渲染坐标
        double sx = start.x - renderManager.viewerPosX;
        double sy = start.y - renderManager.viewerPosY;
        double sz = start.z - renderManager.viewerPosZ;
        double ex = end.x - renderManager.viewerPosX;
        double ey = end.y - renderManager.viewerPosY;
        double ez = end.z - renderManager.viewerPosZ;

        // 设置OpenGL状态
        setupGL();

        // 计算方向向量
        Vec3d direction = end.subtract(start).normalize();
        Vec3d up = getPerpendicularVector(direction);
        Vec3d right = direction.crossProduct(up).normalize();

        // 渲染激光
        renderLaserBeam(sx, sy, sz, ex, ey, ez, right, up, props, beam, distance, partialTicks);

        // 恢复OpenGL状态
        restoreGL();
    }

    private Vec3d getBeamStartPoint(Entity owner, float partialTicks) {
        Vec3d basePos = interpolatePosition(owner, partialTicks);
        String ownerClass = owner.getClass().getSimpleName();

        switch(ownerClass) {
            case "EntityPlayer":
                EntityPlayer player = (EntityPlayer) owner;

                // 获取玩家的旋转角度（插值）
                float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
                float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;

                // 转换为弧度
                float yawRad = yaw * (float)Math.PI / 180.0F;
                float pitchRad = pitch * (float)Math.PI / 180.0F;

                // 计算视线方向
                Vec3d lookVec = player.getLookVec();

                // 计算右向量
                Vec3d up = new Vec3d(0, 1, 0);
                Vec3d right = lookVec.crossProduct(up).normalize();

                // 右手偏移量（与EntityLaserBeam中保持一致）
                double sideOffset = 0.36D;    // 向右偏移
                double vertOffset = -0.15D;   // 向下偏移
                double forwardOffset = 0.4D;  // 向前偏移

                // 如果玩家在潜行，调整偏移
                if (player.isSneaking()) {
                    vertOffset -= 0.08D;
                }

                // 计算右手位置
                return new Vec3d(
                        basePos.x + right.x * sideOffset + lookVec.x * forwardOffset,
                        basePos.y + player.getEyeHeight() + vertOffset + lookVec.y * forwardOffset,
                        basePos.z + right.z * sideOffset + lookVec.z * forwardOffset
                );

            case "EntityVoidRipper":
                // VoidRipper从胸部双手间发射
                yaw = owner.rotationYaw * (float)Math.PI / 180.0F;
                return new Vec3d(
                        basePos.x - Math.sin(yaw) * 0.5D,
                        basePos.y + owner.height * 0.7D,
                        basePos.z + Math.cos(yaw) * 0.5D
                );

            case "EntityRiftwarden":
                // Riftwarden从手部发射
                return getRiftwardenHandPosition(owner, basePos);

            default:
                return new Vec3d(basePos.x, basePos.y + owner.getEyeHeight(), basePos.z);
        }
    }

    private Vec3d getRiftwardenHandPosition(Entity owner, Vec3d basePos) {
        // 简化的手部位置计算
        float yaw = owner.rotationYaw * (float)Math.PI / 180.0F;
        double sideOffset = 0.5D; // 可以根据需要调整左右手
        return new Vec3d(
                basePos.x + Math.cos(yaw) * sideOffset,
                basePos.y + owner.height * 0.65D,
                basePos.z + Math.sin(yaw) * sideOffset
        );
    }

    private Vec3d getBeamEndPoint(EntityLaserBeam beam, float partialTicks) {
        if (beam.isBlocked() && beam.getActualEndPos() != null) {
            return beam.getActualEndPos();
        } else if (beam.getTargetPos() != null) {
            return beam.getTargetPos();
        } else {
            Entity target = beam.getTargetEntity();
            if (target == null) return null;
            Vec3d pos = interpolatePosition(target, partialTicks);
            return new Vec3d(pos.x, pos.y + target.height * 0.5D, pos.z);
        }
    }

    private LaserProperties getLaserProperties(EntityLaserBeam beam, Entity owner) {
        LaserProperties props = new LaserProperties();
        int colorType = beam.getColorType();
        String ownerClass = owner.getClass().getSimpleName();

        // 基础属性设置
        switch(ownerClass) {
            case "EntityPlayer":
                // 玩家镭射炮 - 紫色，最远
                props.maxRenderDistance = 80.0F;
                props.baseWidth = 0.5F;
                props.fadeStartDistance = 30.0F;
                props.particleFrequency = 3;
                break;

            case "EntityVoidRipper":
                // VoidRipper - 蓝色，超远
                props.maxRenderDistance = 120.0F;
                props.baseWidth = 0.35F;
                props.fadeStartDistance = 50.0F;
                props.particleFrequency = 2;
                break;

            case "EntityRiftwarden":
                // Riftwarden - 多种颜色，中等距离
                props.maxRenderDistance = 60.0F;
                props.baseWidth = 0.8F;
                props.fadeStartDistance = 25.0F;
                props.particleFrequency = 4;
                break;

            default:
                props.maxRenderDistance = 40.0F;
                props.baseWidth = 0.4F;
                props.fadeStartDistance = 15.0F;
                props.particleFrequency = 5;
                break;
        }

        // 颜色属性设置
        switch(colorType) {
            case EntityLaserBeam.COLOR_PURPLE:
                // 紫色 - 虚空能量
                props.coreR = 0.6F; props.coreG = 0.0F; props.coreB = 0.9F;
                props.glowR = 0.8F; props.glowG = 0.2F; props.glowB = 1.0F;
                props.particleType = EnumParticleTypes.PORTAL;
                props.hasSpiral = true;
                break;

            case EntityLaserBeam.COLOR_BLUE:
                // 蓝色 - 冷冻/电能
                props.coreR = 0.1F; props.coreG = 0.5F; props.coreB = 1.0F;
                props.glowR = 0.3F; props.glowG = 0.7F; props.glowB = 1.0F;
                props.particleType = EnumParticleTypes.SPELL_INSTANT;
                props.hasElectricArcs = true;
                props.baseWidth *= 0.8F; // 蓝色激光更细
                break;

            case EntityLaserBeam.COLOR_BLACK:
                // 黑色 - 虚空吸收
                props.coreR = 0.1F; props.coreG = 0.0F; props.coreB = 0.2F;
                props.glowR = 0.3F; props.glowG = 0.0F; props.glowB = 0.4F;
                props.particleType = EnumParticleTypes.SMOKE_LARGE;
                props.hasDarkAura = true;
                break;

            case EntityLaserBeam.COLOR_RED:
                // 红色 - 火焰/毁灭
                props.coreR = 1.0F; props.coreG = 0.2F; props.coreB = 0.1F;
                props.glowR = 1.0F; props.glowG = 0.5F; props.glowB = 0.3F;
                props.particleType = EnumParticleTypes.FLAME;
                break;

            case EntityLaserBeam.COLOR_GREEN:
                // 绿色 - 腐蚀/毒素
                props.coreR = 0.1F; props.coreG = 0.9F; props.coreB = 0.2F;
                props.glowR = 0.3F; props.glowG = 1.0F; props.glowB = 0.4F;
                props.particleType = EnumParticleTypes.VILLAGER_HAPPY;
                break;

            default:
                props.coreR = 1.0F; props.coreG = 1.0F; props.coreB = 1.0F;
                props.glowR = 1.0F; props.glowG = 1.0F; props.glowB = 1.0F;
                props.particleType = EnumParticleTypes.CRIT;
                break;
        }

        return props;
    }

    private void renderLaserBeam(double sx, double sy, double sz,
                                 double ex, double ey, double ez,
                                 Vec3d right, Vec3d up,
                                 LaserProperties props, EntityLaserBeam beam,
                                 double distance, float partialTicks) {

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        // 计算时间相关的动画
        float time = (Minecraft.getSystemTime() % 1000L) / 1000.0F;
        float pulse = (float)(Math.sin(Minecraft.getSystemTime() * 0.002) * 0.05 + 1.0);

        // 计算基础透明度衰减
        float baseFade = calculateDistanceFade(distance, props);

        // 渲染外层光晕
        if (baseFade > 0.05F) {
            float glowWidth = props.baseWidth * 2.5F * pulse;
            float glowAlpha = 0.2F * baseFade;
            renderCylinder(buf, sx, sy, sz, ex, ey, ez, right, up,
                    glowWidth, time * 0.5F, distance,
                    props.glowR, props.glowG, props.glowB, glowAlpha);
        }

        // 渲染主激光束（分段以实现渐变）
        renderSegmentedLaser(buf, sx, sy, sz, ex, ey, ez, right, up,
                props.baseWidth * pulse, time, distance, props);

        // 渲染核心亮光
        float coreWidth = props.baseWidth * 0.4F;
        float coreAlpha = Math.min(0.9F, baseFade * 1.2F);
        renderCylinder(buf, sx, sy, sz, ex, ey, ez, right, up,
                coreWidth, time * 2, distance,
                Math.min(1.0F, props.coreR + 0.3F),
                Math.min(1.0F, props.coreG + 0.3F),
                Math.min(1.0F, props.coreB + 0.3F),
                coreAlpha);

        // 渲染特殊效果
        renderSpecialEffects(sx, sy, sz, ex, ey, ez, right, up, props, distance, time);

        // 渲染粒子
        if (beam.world.rand.nextInt(props.particleFrequency) == 0) {
            spawnLaserParticles(beam, props, distance);
        }

        // 如果被阻挡，渲染碰撞效果
        if (beam.isBlocked()) {
            renderBlockedEffect(buf, ex, ey, ez, right, up, props, time);
        }
    }

    private void renderSegmentedLaser(BufferBuilder buf,
                                      double sx, double sy, double sz,
                                      double ex, double ey, double ez,
                                      Vec3d right, Vec3d up,
                                      float baseWidth, float texOffset, double totalDistance,
                                      LaserProperties props) {

        int segments = Math.min(20, Math.max(5, (int)(totalDistance / 5.0)));

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (int seg = 0; seg < segments; seg++) {
            double t1 = (double)seg / segments;
            double t2 = (double)(seg + 1) / segments;

            // 段的起点和终点
            double segSx = sx + (ex - sx) * t1;
            double segSy = sy + (ey - sy) * t1;
            double segSz = sz + (ez - sz) * t1;
            double segEx = sx + (ex - sx) * t2;
            double segEy = sy + (ey - sy) * t2;
            double segEz = sz + (ez - sz) * t2;

            // 计算该段的距离和透明度
            double segDistance = totalDistance * t2;
            float segAlpha = calculateSegmentAlpha(segDistance, totalDistance, props);

            // 每段可能有不同的宽度（渐细效果）
            float segWidth = baseWidth * (1.0F - (float)t2 * 0.2F);

            // 渲染该段的圆柱
            for (int i = 0; i < SEGMENTS; i++) {
                double angle1 = (Math.PI * 2 * i) / SEGMENTS;
                double angle2 = (Math.PI * 2 * (i + 1)) / SEGMENTS;

                double cos1 = Math.cos(angle1);
                double sin1 = Math.sin(angle1);
                double cos2 = Math.cos(angle2);
                double sin2 = Math.sin(angle2);

                Vec3d offset1 = right.scale(cos1 * segWidth).add(up.scale(sin1 * segWidth));
                Vec3d offset2 = right.scale(cos2 * segWidth).add(up.scale(sin2 * segWidth));

                float u1 = (float)i / SEGMENTS;
                float u2 = (float)(i + 1) / SEGMENTS;
                float v1 = texOffset + (float)t1 * 3.0F;
                float v2 = texOffset + (float)t2 * 3.0F;

                buf.pos(segSx + offset1.x, segSy + offset1.y, segSz + offset1.z)
                        .tex(u1, v1).color(props.coreR, props.coreG, props.coreB, segAlpha).endVertex();
                buf.pos(segSx + offset2.x, segSy + offset2.y, segSz + offset2.z)
                        .tex(u2, v1).color(props.coreR, props.coreG, props.coreB, segAlpha).endVertex();
                buf.pos(segEx + offset2.x, segEy + offset2.y, segEz + offset2.z)
                        .tex(u2, v2).color(props.coreR, props.coreG, props.coreB, segAlpha * 0.9F).endVertex();
                buf.pos(segEx + offset1.x, segEy + offset1.y, segEz + offset1.z)
                        .tex(u1, v2).color(props.coreR, props.coreG, props.coreB, segAlpha * 0.9F).endVertex();
            }
        }

        Tessellator.getInstance().draw();
    }

    private void renderCylinder(BufferBuilder buf,
                                double sx, double sy, double sz,
                                double ex, double ey, double ez,
                                Vec3d right, Vec3d up,
                                float radius, float texOffset, double length,
                                float r, float g, float b, float a) {

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (int i = 0; i < SEGMENTS; i++) {
            double angle1 = (Math.PI * 2 * i) / SEGMENTS;
            double angle2 = (Math.PI * 2 * (i + 1)) / SEGMENTS;

            Vec3d offset1 = right.scale(Math.cos(angle1) * radius)
                    .add(up.scale(Math.sin(angle1) * radius));
            Vec3d offset2 = right.scale(Math.cos(angle2) * radius)
                    .add(up.scale(Math.sin(angle2) * radius));

            float u1 = (float)i / SEGMENTS;
            float u2 = (float)(i + 1) / SEGMENTS;
            float v1 = texOffset;
            float v2 = texOffset + (float)length * 0.1F;

            buf.pos(sx + offset1.x, sy + offset1.y, sz + offset1.z)
                    .tex(u1, v1).color(r, g, b, a).endVertex();
            buf.pos(sx + offset2.x, sy + offset2.y, sz + offset2.z)
                    .tex(u2, v1).color(r, g, b, a).endVertex();
            buf.pos(ex + offset2.x, ey + offset2.y, ez + offset2.z)
                    .tex(u2, v2).color(r, g, b, a).endVertex();
            buf.pos(ex + offset1.x, ey + offset1.y, ez + offset1.z)
                    .tex(u1, v2).color(r, g, b, a).endVertex();
        }

        Tessellator.getInstance().draw();
    }

    private void renderSpecialEffects(double sx, double sy, double sz,
                                      double ex, double ey, double ez,
                                      Vec3d right, Vec3d up,
                                      LaserProperties props, double distance, float time) {

        BufferBuilder buf = Tessellator.getInstance().getBuffer();

        // 紫色激光的螺旋效果
        if (props.hasSpiral) {
            renderSpiralEffect(buf, sx, sy, sz, ex, ey, ez, right, up,
                    props.baseWidth * 0.3F, distance, time);
        }

        // 蓝色激光的电弧效果
        if (props.hasElectricArcs && Minecraft.getSystemTime() % 60 < 10) {
            renderElectricArcs(buf, sx, sy, sz, ex, ey, ez, right, up, props.baseWidth);
        }

        // 黑色激光的暗影光环
        if (props.hasDarkAura) {
            renderDarkAura(buf, sx, sy, sz, ex, ey, ez, right, up, props.baseWidth, time);
        }
    }

    private void renderSpiralEffect(BufferBuilder buf,
                                    double sx, double sy, double sz,
                                    double ex, double ey, double ez,
                                    Vec3d right, Vec3d up,
                                    float radius, double distance, float time) {

        buf.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        int points = (int)(distance * 4);
        for (int i = 0; i <= points; i++) {
            double t = (double)i / points;
            double px = sx + (ex - sx) * t;
            double py = sy + (ey - sy) * t;
            double pz = sz + (ez - sz) * t;

            double angle = t * Math.PI * 2 * (distance / 5.0) + time * Math.PI * 2;
            Vec3d offset = right.scale(Math.cos(angle) * radius)
                    .add(up.scale(Math.sin(angle) * radius));

            float alpha = 0.6F * (1.0F - (float)t * 0.5F);
            buf.pos(px + offset.x, py + offset.y, pz + offset.z)
                    .color(0.8F, 0.2F, 1.0F, alpha).endVertex();
        }

        Tessellator.getInstance().draw();
    }

    private void renderElectricArcs(BufferBuilder buf,
                                    double sx, double sy, double sz,
                                    double ex, double ey, double ez,
                                    Vec3d right, Vec3d up, float width) {

        buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // 生成几条电弧
        for (int arc = 0; arc < 3; arc++) {
            int points = 15;
            double lastX = sx, lastY = sy, lastZ = sz;

            for (int i = 1; i <= points; i++) {
                double t = (double)i / points;
                double baseX = sx + (ex - sx) * t;
                double baseY = sy + (ey - sy) * t;
                double baseZ = sz + (ez - sz) * t;

                // 随机偏移
                double offsetMag = width * 0.5 * Math.sin(t * Math.PI) * (1 + arc * 0.3);
                Vec3d randomOffset = right.scale((Math.random() - 0.5) * offsetMag)
                        .add(up.scale((Math.random() - 0.5) * offsetMag));

                double newX = baseX + randomOffset.x;
                double newY = baseY + randomOffset.y;
                double newZ = baseZ + randomOffset.z;

                buf.pos(lastX, lastY, lastZ).color(0.5F, 0.8F, 1.0F, 0.6F).endVertex();
                buf.pos(newX, newY, newZ).color(0.5F, 0.8F, 1.0F, 0.4F).endVertex();

                lastX = newX;
                lastY = newY;
                lastZ = newZ;
            }
        }

        Tessellator.getInstance().draw();
    }

    private void renderDarkAura(BufferBuilder buf,
                                double sx, double sy, double sz,
                                double ex, double ey, double ez,
                                Vec3d right, Vec3d up,
                                float width, float time) {

        // 渲染暗影粒子环
        int rings = (int)(ex - sx) / 3;
        for (int ring = 0; ring < rings; ring++) {
            double t = (double)ring / Math.max(1, rings - 1);
            double px = sx + (ex - sx) * t;
            double py = sy + (ey - sy) * t;
            double pz = sz + (ez - sz) * t;

            buf.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);

            // 中心点
            buf.pos(px, py, pz).color(0.0F, 0.0F, 0.0F, 0.3F).endVertex();

            // 外圈
            float radius = width * 1.5F * (float)Math.sin(time * Math.PI * 2 + t * Math.PI);
            for (int i = 0; i <= 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Vec3d offset = right.scale(Math.cos(angle) * radius)
                        .add(up.scale(Math.sin(angle) * radius));
                buf.pos(px + offset.x, py + offset.y, pz + offset.z)
                        .color(0.2F, 0.0F, 0.3F, 0.0F).endVertex();
            }

            Tessellator.getInstance().draw();
        }
    }

    private void renderBlockedEffect(BufferBuilder buf, double x, double y, double z,
                                     Vec3d right, Vec3d up,
                                     LaserProperties props, float time) {

        float pulse = (float)Math.sin(time * Math.PI) * 0.5F + 0.5F;
        float size = props.baseWidth * 3.0F * pulse;

        buf.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);

        // 中心爆炸点
        buf.pos(x, y, z).color(1.0F, 0.8F, 0.5F, 0.8F).endVertex();

        // 冲击波环
        for (int i = 0; i <= 16; i++) {
            double angle = (Math.PI * 2 * i) / 16;
            Vec3d offset = right.scale(Math.cos(angle) * size)
                    .add(up.scale(Math.sin(angle) * size));

            buf.pos(x + offset.x, y + offset.y, z + offset.z)
                    .color(props.coreR, props.coreG, props.coreB, 0.0F).endVertex();
        }

        Tessellator.getInstance().draw();
    }

    private void spawnLaserParticles(EntityLaserBeam beam, LaserProperties props, double distance) {
        World world = beam.world;
        if (world == null || !world.isRemote) return;

        Entity owner = beam.getOwnerEntity();
        if (owner == null) return;

        Vec3d start = getBeamStartPoint(owner, 0);
        Vec3d end = beam.isBlocked() && beam.getActualEndPos() != null ?
                beam.getActualEndPos() : getBeamEndPoint(beam, 0);
        if (end == null) return;

        // 沿激光路径生成粒子
        int particleCount = Math.min(10, (int)(distance / 8.0));
        for (int i = 0; i < particleCount; i++) {
            double t = world.rand.nextDouble();
            double px = start.x + (end.x - start.x) * t;
            double py = start.y + (end.y - start.y) * t;
            double pz = start.z + (end.z - start.z) * t;

            // 添加小偏移
            px += (world.rand.nextDouble() - 0.5) * props.baseWidth;
            py += (world.rand.nextDouble() - 0.5) * props.baseWidth;
            pz += (world.rand.nextDouble() - 0.5) * props.baseWidth;

            world.spawnParticle(props.particleType, px, py, pz, 0, 0, 0);
        }
    }

    private float calculateDistanceFade(double distance, LaserProperties props) {
        if (distance <= props.fadeStartDistance) {
            return 1.0F; // 近距离全强度
        } else if (distance >= props.maxRenderDistance) {
            return 0.0F; // 超出范围不渲染
        } else {
            // 平滑的二次衰减
            float fadeRange = props.maxRenderDistance - props.fadeStartDistance;
            float fadeProgress = (float)(distance - props.fadeStartDistance) / fadeRange;
            return 1.0F - (fadeProgress * fadeProgress);
        }
    }

    private float calculateSegmentAlpha(double segDistance, double totalDistance, LaserProperties props) {
        float distanceFade = calculateDistanceFade(segDistance, props);
        float positionFade = 1.0F - (float)(segDistance / totalDistance) * 0.3F;
        return distanceFade * positionFade * 0.8F;
    }

    private Vec3d interpolatePosition(Entity entity, float partialTicks) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        return new Vec3d(x, y, z);
    }

    private Vec3d getPerpendicularVector(Vec3d vec) {
        if (Math.abs(vec.y) > 0.99) {
            return new Vec3d(1, 0, 0);
        } else {
            return new Vec3d(0, 1, 0);
        }
    }

    private void setupGL() {
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        bindTexture(TEX);
    }

    private void restoreGL() {
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityLaserBeam entity) {
        return TEX;
    }

    // 激光属性内部类
    private static class LaserProperties {
        float maxRenderDistance;
        float fadeStartDistance;
        float baseWidth;
        float coreR, coreG, coreB;
        float glowR, glowG, glowB;
        EnumParticleTypes particleType;
        int particleFrequency;
        boolean hasSpiral = false;
        boolean hasElectricArcs = false;
        boolean hasDarkAura = false;
    }
}