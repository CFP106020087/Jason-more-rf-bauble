package com.moremod.client.render.fx;

import com.moremod.entity.fx.EntityLightningArc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.*;

@SideOnly(Side.CLIENT)
public class RenderLightningArc extends Render<EntityLightningArc> {

    // 使用你的新材质
    private static final ResourceLocation TEX_VERTICAL = new ResourceLocation("moremod", "textures/effects/electric_arc_vertical.png");
    private static final ResourceLocation TEX_HORIZONTAL = new ResourceLocation("moremod", "textures/effects/electric_arc_horizontal.png");

    // 默认使用横向材质
    private static final ResourceLocation TEX = TEX_HORIZONTAL;

    // 缓存闪电顶点数据
    private final Map<Integer, LightningBoltData> lightningCache = new HashMap<>();

    // 用于清理旧缓存的计时器
    private long lastCacheClean = 0;

    public RenderLightningArc(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0F;
    }

    @Override
    public void doRender(EntityLightningArc arc, double x, double y, double z, float entityYaw, float partialTicks) {
        Entity from = arc.getFrom();
        Entity to = arc.getTo();

        if (from == null || to == null || !from.isEntityAlive() || !to.isEntityAlive()) {
            return;
        }

        // 判断是否是Boss释放的闪电（通过类名判断）
        boolean isBossArc = from.getClass().getName().contains("Riftwarden") ||
                from.getClass().getName().contains("Boss");

        // 判断是否是物品释放的（玩家使用物品）
        boolean isItemArc = from instanceof net.minecraft.entity.player.EntityPlayer && !isBossArc;

        // 插值实体位置
        Vec3d startWorld = interpolatePosition(from, partialTicks);
        startWorld = new Vec3d(startWorld.x, startWorld.y + from.height * 0.7, startWorld.z);

        Vec3d endWorld = interpolatePosition(to, partialTicks);
        endWorld = new Vec3d(endWorld.x, endWorld.y + to.height * 0.5, endWorld.z);

        // 转换到渲染坐标系
        double sx = startWorld.x - renderManager.viewerPosX;
        double sy = startWorld.y - renderManager.viewerPosY;
        double sz = startWorld.z - renderManager.viewerPosZ;

        double ex = endWorld.x - renderManager.viewerPosX;
        double ey = endWorld.y - renderManager.viewerPosY;
        double ez = endWorld.z - renderManager.viewerPosZ;

        Vec3d start = new Vec3d(sx, sy, sz);
        Vec3d end = new Vec3d(ex, ey, ez);

        // 获取或生成闪电数据（根据类型）
        LightningBoltData boltData = getOrGenerateLightning(
                arc.getEntityId(),
                arc.getSeed(),
                start,
                end,
                arc.ticksExisted,
                partialTicks,
                isBossArc,
                isItemArc
        );

        // 渲染闪电
        renderLightningBolt(boltData, arc.getTicksLeft(), arc.ticksExisted + partialTicks, isBossArc, isItemArc);
    }

    private LightningBoltData getOrGenerateLightning(int entityId, int seed, Vec3d start, Vec3d end,
                                                     int ticksExisted, float partialTicks,
                                                     boolean isBossArc, boolean isItemArc) {
        // 不同类型使用不同的刷新率
        long refreshRate = isItemArc ? 100L : 50L; // 物品电弧更稳定
        long currentTime = Minecraft.getSystemTime();
        long timeKey = currentTime / refreshRate;
        int cacheKey = entityId;

        // 定期清理缓存
        if (currentTime - lastCacheClean > 1000) {
            cleanCache();
            lastCacheClean = currentTime;
        }

        LightningBoltData data = lightningCache.get(cacheKey);

        // 检查是否需要重新生成
        if (data == null || data.timeKey != timeKey) {
            Random rand = new Random(seed + timeKey * 31);
            float animTime = ticksExisted + partialTicks;

            List<LightningSegment> segments;
            if (isItemArc) {
                // 物品电弧：简单、细长、无分支
                segments = generateItemLightning(rand, start, end, animTime);
            } else if (isBossArc) {
                // Boss电弧：狂暴、多分支
                segments = generateBossLightning(rand, start, end, animTime);
            } else {
                // 默认蛇形
                segments = generateSnakeLightning(rand, start, end, animTime);
            }

            data = new LightningBoltData(segments, timeKey);
            lightningCache.put(cacheKey, data);
        }

        return data;
    }

    // 物品电弧 - 简洁的追踪光束
    private List<LightningSegment> generateItemLightning(Random rand, Vec3d start, Vec3d end, float time) {
        List<LightningSegment> segments = new ArrayList<>();

        double distance = start.distanceTo(end);
        int segmentCount = Math.min((int)(distance * 2) + 4, 12); // 较少的段数

        Vec3d direction = end.subtract(start).normalize();
        Vec3d perpendicular = getPerpendicular(direction);

        List<Vec3d> path = new ArrayList<>();

        for (int i = 0; i <= segmentCount; i++) {
            float t = i / (float)segmentCount;

            // 基础贝塞尔曲线
            Vec3d point = start.add(end.subtract(start).scale(t));

            // 很小的波动
            double wave = Math.sin(t * Math.PI * 2 + time * 0.08) * distance * 0.02;
            double microNoise = rand.nextGaussian() * 0.01 * distance;

            point = point.add(perpendicular.scale(wave + microNoise));
            path.add(point);
        }

        // 轻度平滑
        path = smoothPath(path, 1);

        // 生成主弧段
        for (int i = 0; i < path.size() - 1; i++) {
            segments.add(new LightningSegment(
                    path.get(i),
                    path.get(i + 1),
                    1.0f,
                    true,
                    i / (float)(path.size() - 1)
            ));
        }

        // 物品电弧没有分支
        return segments;
    }

    // Boss电弧 - 狂暴的闪电风暴
    private List<LightningSegment> generateBossLightning(Random rand, Vec3d start, Vec3d end, float time) {
        List<LightningSegment> segments = new ArrayList<>();

        // 生成主路径 - 更混乱
        List<Vec3d> mainPath = generateChaoticPath(start, end, rand, time);
        mainPath = smoothPath(mainPath, 2);

        // 主弧
        for (int i = 0; i < mainPath.size() - 1; i++) {
            segments.add(new LightningSegment(
                    mainPath.get(i),
                    mainPath.get(i + 1),
                    1.0f,
                    true,
                    i / (float)(mainPath.size() - 1)
            ));
        }

        // 大量分支
        for (int i = 2; i < mainPath.size() - 2; i++) {
            if (rand.nextFloat() < 0.25f) { // 25%概率
                Vec3d branchStart = mainPath.get(i);

                // 多级分支
                int branchCount = 1 + rand.nextInt(3);
                for (int b = 0; b < branchCount; b++) {
                    List<Vec3d> branch = generateWildBranch(branchStart, mainPath.get(i + 1), rand, time);

                    for (int j = 0; j < branch.size() - 1; j++) {
                        float progress = j / (float)(branch.size() - 1);
                        segments.add(new LightningSegment(
                                branch.get(j),
                                branch.get(j + 1),
                                0.8f * (1.0f - progress * 0.7f),
                                false,
                                progress
                        ));
                    }

                    // 二级分支
                    if (branch.size() > 3 && rand.nextFloat() < 0.3f) {
                        Vec3d subStart = branch.get(branch.size() / 2);
                        List<Vec3d> subBranch = generateMicroBranch(subStart, rand);

                        for (int j = 0; j < subBranch.size() - 1; j++) {
                            segments.add(new LightningSegment(
                                    subBranch.get(j),
                                    subBranch.get(j + 1),
                                    0.5f,
                                    false,
                                    j / (float)(subBranch.size() - 1)
                            ));
                        }
                    }
                }
            }
        }

        return segments;
    }

    // 混乱路径生成（Boss用）
    private List<Vec3d> generateChaoticPath(Vec3d start, Vec3d end, Random rand, float time) {
        List<Vec3d> path = new ArrayList<>();

        double distance = start.distanceTo(end);
        int segments = (int)(distance * 5) + 12;

        Vec3d direction = end.subtract(start).normalize();
        Vec3d perp1 = getPerpendicular(direction);
        Vec3d perp2 = direction.crossProduct(perp1).normalize();

        for (int i = 0; i <= segments; i++) {
            float t = i / (float)segments;
            Vec3d point = start.add(end.subtract(start).scale(t));

            // 多重混乱波动
            double chaos1 = Math.sin(t * Math.PI * 3 + time * 0.1) * distance * 0.2;
            double chaos2 = Math.cos(t * Math.PI * 2.5 - time * 0.07) * distance * 0.15;
            double spike = (rand.nextDouble() - 0.5) * distance * 0.1;

            point = point
                    .add(perp1.scale(chaos1 + spike))
                    .add(perp2.scale(chaos2));

            path.add(point);
        }

        return path;
    }

    // 狂野分支（Boss用）
    private List<Vec3d> generateWildBranch(Vec3d start, Vec3d direction, Random rand, float time) {
        List<Vec3d> branch = new ArrayList<>();

        double length = 1.0 + rand.nextDouble() * 3;
        int segments = 6;

        Vec3d dir = direction.subtract(start).normalize();
        double angle = (rand.nextDouble() - 0.5) * Math.PI / 2; // ±90度

        Vec3d perp = getPerpendicular(dir);
        dir = rotateAroundAxis(dir, perp, angle);

        for (int i = 0; i <= segments; i++) {
            float t = i / (float)segments;
            Vec3d point = start.add(dir.scale(length * t));

            // 添加混乱
            double chaos = Math.sin(t * Math.PI * 4 + time * 0.2) * 0.2;
            point = point.add(perp.scale(chaos));

            branch.add(point);
        }

        return branch;
    }

    // 微小分支
    private List<Vec3d> generateMicroBranch(Vec3d start, Random rand) {
        List<Vec3d> branch = new ArrayList<>();

        Vec3d randomDir = new Vec3d(
                rand.nextGaussian(),
                rand.nextGaussian(),
                rand.nextGaussian()
        ).normalize();

        double length = 0.5 + rand.nextDouble() * 0.5;

        branch.add(start);
        branch.add(start.add(randomDir.scale(length)));

        return branch;
    }

    // 添加旋转辅助方法
    private Vec3d rotateAroundAxis(Vec3d vec, Vec3d axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        Vec3d cross = axis.crossProduct(vec);
        double dot = axis.dotProduct(vec);

        return vec.scale(cos)
                .add(cross.scale(sin))
                .add(axis.scale(dot * (1 - cos)));
    }

    // 生成蛇形闪电
    private List<LightningSegment> generateSnakeLightning(Random rand, Vec3d start, Vec3d end, float time) {
        List<LightningSegment> segments = new ArrayList<>();

        // 生成蛇形主路径
        List<Vec3d> mainPath = generateSnakePath(start, end, rand, time);

        // 平滑路径
        mainPath = smoothPath(mainPath, 3);

        // 添加主闪电段
        for (int i = 0; i < mainPath.size() - 1; i++) {
            segments.add(new LightningSegment(
                    mainPath.get(i),
                    mainPath.get(i + 1),
                    1.0f,
                    true,
                    i / (float)(mainPath.size() - 1)
            ));
        }

        // 生成螺旋分支
        for (int i = 3; i < mainPath.size() - 3; i++) {
            if (rand.nextFloat() < 0.08f) { // 8%概率产生分支
                Vec3d branchStart = mainPath.get(i);
                List<Vec3d> branch = generateSpiralBranch(branchStart, mainPath.get(i + 1), rand, time);

                for (int j = 0; j < branch.size() - 1; j++) {
                    float progress = j / (float)(branch.size() - 1);
                    segments.add(new LightningSegment(
                            branch.get(j),
                            branch.get(j + 1),
                            0.7f * (1.0f - progress * 0.6f),
                            false,
                            progress
                    ));
                }
            }
        }

        return segments;
    }

    // 生成蛇形路径
    private List<Vec3d> generateSnakePath(Vec3d start, Vec3d end, Random rand, float time) {
        List<Vec3d> path = new ArrayList<>();

        double distance = start.distanceTo(end);
        int segments = (int)(distance * 4) + 8; // 更多段以形成平滑曲线

        Vec3d direction = end.subtract(start).normalize();
        Vec3d perpendicular1 = getPerpendicular(direction);
        Vec3d perpendicular2 = direction.crossProduct(perpendicular1).normalize();

        for (int i = 0; i <= segments; i++) {
            float t = i / (float)segments;

            // 基础线性插值
            Vec3d point = start.add(end.subtract(start).scale(t));

            // 添加蛇形波动
            double waveAmplitude = distance * 0.15; // 波动幅度
            double frequency = 2.0 + rand.nextDouble(); // 波动频率

            // 主波动（慢速）
            double wave1 = Math.sin(t * Math.PI * frequency + time * 0.05) * waveAmplitude;
            double wave2 = Math.cos(t * Math.PI * frequency * 0.7 + time * 0.03) * waveAmplitude * 0.8;

            // 次级波动（快速，小幅）
            double microWave1 = Math.sin(t * Math.PI * 8 + time * 0.1) * waveAmplitude * 0.1;
            double microWave2 = Math.cos(t * Math.PI * 6 + time * 0.08) * waveAmplitude * 0.1;

            // 添加随机扰动
            double randomOffset1 = (rand.nextGaussian() * 0.05 + microWave1) * distance;
            double randomOffset2 = (rand.nextGaussian() * 0.05 + microWave2) * distance;

            // 组合所有偏移
            point = point
                    .add(perpendicular1.scale(wave1 + randomOffset1))
                    .add(perpendicular2.scale(wave2 + randomOffset2));

            path.add(point);
        }

        return path;
    }

    // 生成螺旋分支
    private List<Vec3d> generateSpiralBranch(Vec3d start, Vec3d direction, Random rand, float time) {
        List<Vec3d> branch = new ArrayList<>();

        double length = 1.5 + rand.nextDouble() * 2;
        int segments = 8;

        Vec3d dir = direction.subtract(start).normalize();
        Vec3d perp1 = getPerpendicular(dir);
        Vec3d perp2 = dir.crossProduct(perp1).normalize();

        // 随机旋转方向
        double spiralSpeed = (rand.nextDouble() - 0.5) * 4;

        for (int i = 0; i <= segments; i++) {
            float t = i / (float)segments;

            // 沿方向延伸
            Vec3d point = start.add(dir.scale(length * t));

            // 添加螺旋
            double radius = (1.0 - t) * 0.3; // 逐渐收缩的半径
            double angle = t * Math.PI * 2 * spiralSpeed + time * 0.1;

            point = point
                    .add(perp1.scale(Math.cos(angle) * radius))
                    .add(perp2.scale(Math.sin(angle) * radius));

            branch.add(point);
        }

        return branch;
    }

    private void renderLightningBolt(LightningBoltData boltData, int ticksLeft, float time,
                                     boolean isBossArc, boolean isItemArc) {
        GlStateManager.pushMatrix();

        // 设置渲染状态
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // 加法混合
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        // 确保全亮度
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

        // 绑定材质
        bindTexture(TEX);

        // 计算基础透明度
        float baseAlpha = Math.min(1.0f, ticksLeft / 10.0f);

        // 不同类型的效果
        if (isItemArc) {
            // 物品：稳定的光束
            float pulse = 0.9f + 0.1f * (float)Math.sin(time * 0.2);
            baseAlpha *= pulse;
        } else if (isBossArc) {
            // Boss：剧烈闪烁
            float flicker = 0.7f + 0.3f * (float)Math.sin(time * 0.5);
            float spike = rand.nextFloat() > 0.9f ? 1.5f : 1.0f; // 随机闪光
            baseAlpha *= flicker * spike;
        } else {
            // 默认脉动
            float pulse = 0.8f + 0.2f * (float)Math.sin(time * 0.3);
            baseAlpha *= pulse;
        }

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        // 渲染多层
        if (isItemArc) {
            renderItemLayers(buf, tess, boltData.segments, baseAlpha, time);
        } else if (isBossArc) {
            renderBossLayers(buf, tess, boltData.segments, baseAlpha, time);
        } else {
            renderLayers(buf, tess, boltData.segments, baseAlpha, time);
        }

        // 恢复渲染状态
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();

        GlStateManager.popMatrix();
    }

    // 物品电弧渲染 - 细长精准
    private void renderItemLayers(BufferBuilder buf, Tessellator tess, List<LightningSegment> segments, float baseAlpha, float time) {
        // 外光晕（很淡）
        renderTubeLayer(buf, tess, segments, baseAlpha * 0.15f, 0.15f, time, false);

        // 主体
        renderTubeLayer(buf, tess, segments, baseAlpha * 0.6f, 0.08f, time, false);

        // 核心（很细）
        renderTubeLayer(buf, tess, segments, baseAlpha * 1.0f, 0.04f, time, false);
    }

    // Boss电弧渲染 - 狂暴粗大黑色
    private void renderBossLayers(BufferBuilder buf, Tessellator tess, List<LightningSegment> segments, float baseAlpha, float time) {
        // 巨大黑色光晕
        renderTubeLayer(buf, tess, segments, baseAlpha * 0.15f, 0.8f, time, true);

        // 外层暗影
        renderTubeLayer(buf, tess, segments, baseAlpha * 0.25f, 0.5f, time, true);

        // 中层黑雾
        renderTubeLayer(buf, tess, segments, baseAlpha * 0.5f, 0.3f, time, true);

        // 主体黑弧
        renderTubeLayer(buf, tess, segments, baseAlpha * 0.8f, 0.18f, time, true);

        // 核心深渊
        renderTubeLayer(buf, tess, segments, baseAlpha * 1.0f, 0.1f, time, true);
    }

    private Random rand = new Random(); // 添加随机数生成器成员

    private void renderLayers(BufferBuilder buf, Tessellator tess, List<LightningSegment> segments, float baseAlpha, float time) {
        // 第1层：外光晕
        renderTubeLayer(buf, tess, segments, baseAlpha * 0.2f, 0.4f, time, false);

        // 第2层：中间层
        renderTubeLayer(buf, tess, segments, baseAlpha * 0.4f, 0.25f, time, false);

        // 第3层：主体
        renderTubeLayer(buf, tess, segments, baseAlpha * 0.7f, 0.15f, time, false);

        // 第4层：核心
        renderTubeLayer(buf, tess, segments, baseAlpha * 1.0f, 0.08f, time, false);
    }

    // 渲染管状层
    private void renderTubeLayer(BufferBuilder buf, Tessellator tess, List<LightningSegment> segments, float alpha, float radius, float time) {
        renderTubeLayer(buf, tess, segments, alpha, radius, time, false);
    }

    private void renderTubeLayer(BufferBuilder buf, Tessellator tess, List<LightningSegment> segments, float alpha, float radius, float time, boolean isBossArc) {
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (LightningSegment segment : segments) {
            renderTubeSegment(buf, segment, radius, alpha * segment.brightness, time, isBossArc);
        }

        tess.draw();
    }

    // 渲染管状段（圆形截面）
    private void renderTubeSegment(BufferBuilder buf, LightningSegment segment, float radius, float alpha, float time, boolean isBossArc) {
        Vec3d from = segment.from;
        Vec3d to = segment.to;

        Vec3d direction = to.subtract(from);
        double length = direction.length();
        if (length < 0.001) return;

        direction = direction.normalize();

        // 获取两个垂直向量
        Vec3d right = getPerpendicular(direction);
        Vec3d up = direction.crossProduct(right).normalize();

        // 圆形截面的分段数
        int sides = segment.isMain ? 8 : 6;

        for (int i = 0; i < sides; i++) {
            float angle1 = (float)(i * Math.PI * 2 / sides);
            float angle2 = (float)((i + 1) * Math.PI * 2 / sides);

            // 计算圆上的点
            Vec3d offset1 = right.scale(Math.cos(angle1) * radius).add(up.scale(Math.sin(angle1) * radius));
            Vec3d offset2 = right.scale(Math.cos(angle2) * radius).add(up.scale(Math.sin(angle2) * radius));

            // 四个顶点
            Vec3d vert1 = from.add(offset1);
            Vec3d vert2 = from.add(offset2);
            Vec3d vert3 = to.add(offset2);
            Vec3d vert4 = to.add(offset1);

            // 颜色设置
            float r, g, b;
            if (isBossArc) {
                // Boss电弧：黑紫色
                r = 0.1f + 0.1f * segment.brightness;  // 深黑
                g = 0.0f;                               // 无绿色
                b = 0.2f + 0.2f * segment.brightness;  // 一点紫色
            } else {
                // 其他电弧：蓝白色
                r = 0.7f + 0.3f * segment.brightness;
                g = 0.8f + 0.2f * segment.brightness;
                b = 1.0f;
            }

            // UV坐标
            float u1 = i / (float)sides;
            float u2 = (i + 1) / (float)sides;
            float texV1 = segment.lengthProgress;
            float texV2 = segment.lengthProgress + 0.1f;

            // 添加UV动画
            float vOffset = (time * 0.02f) % 1.0f;
            texV1 += vOffset;
            texV2 += vOffset;

            // 添加顶点
            buf.pos(vert1.x, vert1.y, vert1.z).tex(u1, texV1).color(r, g, b, alpha).endVertex();
            buf.pos(vert2.x, vert2.y, vert2.z).tex(u2, texV1).color(r, g, b, alpha).endVertex();
            buf.pos(vert3.x, vert3.y, vert3.z).tex(u2, texV2).color(r, g, b, alpha).endVertex();
            buf.pos(vert4.x, vert4.y, vert4.z).tex(u1, texV2).color(r, g, b, alpha).endVertex();
        }
    }

    // 平滑路径
    private List<Vec3d> smoothPath(List<Vec3d> points, int iterations) {
        if (points.size() < 3) return points;

        List<Vec3d> smoothed = points;

        for (int iter = 0; iter < iterations; iter++) {
            List<Vec3d> newPoints = new ArrayList<>();
            newPoints.add(smoothed.get(0)); // 保持起点

            for (int i = 1; i < smoothed.size() - 1; i++) {
                Vec3d prev = smoothed.get(i - 1);
                Vec3d curr = smoothed.get(i);
                Vec3d next = smoothed.get(i + 1);

                // Catmull-Rom样条插值
                Vec3d smoothedPoint = prev.scale(0.25)
                        .add(curr.scale(0.5))
                        .add(next.scale(0.25));

                newPoints.add(smoothedPoint);
            }

            newPoints.add(smoothed.get(smoothed.size() - 1)); // 保持终点
            smoothed = newPoints;
        }

        return smoothed;
    }

    private Vec3d getPerpendicular(Vec3d vec) {
        if (Math.abs(vec.y) > 0.9) {
            return new Vec3d(1, 0, 0).normalize();
        }
        return vec.crossProduct(new Vec3d(0, 1, 0)).normalize();
    }

    private Vec3d interpolatePosition(Entity entity, float partialTicks) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        return new Vec3d(x, y, z);
    }

    private void cleanCache() {
        if (lightningCache.size() > 20) {
            lightningCache.clear();
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityLightningArc entity) {
        return TEX;
    }

    // 内部类
    private static class LightningBoltData {
        final List<LightningSegment> segments;
        final long timeKey;

        LightningBoltData(List<LightningSegment> segments, long timeKey) {
            this.segments = segments;
            this.timeKey = timeKey;
        }
    }

    private static class LightningSegment {
        final Vec3d from;
        final Vec3d to;
        final float brightness;
        final boolean isMain;
        final float lengthProgress;

        LightningSegment(Vec3d from, Vec3d to, float brightness, boolean isMain, float lengthProgress) {
            this.from = from;
            this.to = to;
            this.brightness = brightness;
            this.isMain = isMain;
            this.lengthProgress = lengthProgress;
        }
    }
}