// JetpackAnimationSync.java - 动画状态同步系统
package com.moremod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

public class JetpackAnimationSync {

    // 动画状态数据包
    public static class PacketJetpackAnimation implements IMessage {
        private UUID playerUUID;
        private boolean isFlying;
        private boolean isHovering;
        private float horizontalSpeed;
        private float verticalSpeed;
        private float pitch;
        private float yaw;

        // 默认构造函数（必需）
        public PacketJetpackAnimation() {}

        public PacketJetpackAnimation(EntityPlayer player, boolean isFlying, boolean isHovering) {
            this.playerUUID = player.getUniqueID();
            this.isFlying = isFlying;
            this.isHovering = isHovering;
            this.horizontalSpeed = (float) Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
            this.verticalSpeed = (float) player.motionY;
            this.pitch = player.rotationPitch;
            this.yaw = player.rotationYaw;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            long mostSig = buf.readLong();
            long leastSig = buf.readLong();
            this.playerUUID = new UUID(mostSig, leastSig);
            this.isFlying = buf.readBoolean();
            this.isHovering = buf.readBoolean();
            this.horizontalSpeed = buf.readFloat();
            this.verticalSpeed = buf.readFloat();
            this.pitch = buf.readFloat();
            this.yaw = buf.readFloat();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeLong(playerUUID.getMostSignificantBits());
            buf.writeLong(playerUUID.getLeastSignificantBits());
            buf.writeBoolean(isFlying);
            buf.writeBoolean(isHovering);
            buf.writeFloat(horizontalSpeed);
            buf.writeFloat(verticalSpeed);
            buf.writeFloat(pitch);
            buf.writeFloat(yaw);
        }

        // Getter methods
        public UUID getPlayerUUID() { return playerUUID; }
        public boolean isFlying() { return isFlying; }
        public boolean isHovering() { return isHovering; }
        public float getHorizontalSpeed() { return horizontalSpeed; }
        public float getVerticalSpeed() { return verticalSpeed; }
        public float getPitch() { return pitch; }
        public float getYaw() { return yaw; }
    }

    // 客户端处理器
    public static class HandlerClient implements IMessageHandler<PacketJetpackAnimation, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketJetpackAnimation message, MessageContext ctx) {
            // 确保在主线程中处理
            Minecraft.getMinecraft().addScheduledTask(() -> {
                handleClientAnimation(message);
            });
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClientAnimation(PacketJetpackAnimation message) {
            EntityPlayer player = Minecraft.getMinecraft().world.getPlayerEntityByUUID(message.getPlayerUUID());
            if (player != null && player != Minecraft.getMinecraft().player) {
                // 更新其他玩家的飞行状态（用于多人游戏同步）
                updatePlayerFlightState(player, message);
            }
        }

        @SideOnly(Side.CLIENT)
        private void updatePlayerFlightState(EntityPlayer player, PacketJetpackAnimation message) {
            // 这里可以存储其他玩家的飞行状态到客户端缓存
            // 用于在 ModelPlayerFlying 中获取其他玩家的准确飞行信息
            ClientFlightStateCache.updatePlayerState(
                    player.getUniqueID(),
                    message.isFlying(),
                    message.isHovering(),
                    message.getHorizontalSpeed(),
                    message.getVerticalSpeed()
            );
        }
    }

    // 服务端处理器（如果需要的话）
    public static class HandlerServer implements IMessageHandler<PacketJetpackAnimation, IMessage> {
        @Override
        public IMessage onMessage(PacketJetpackAnimation message, MessageContext ctx) {
            // 服务端接收到客户端的动画状态更新
            // 可以进行验证和转发给其他客户端
            return null;
        }
    }

    // 客户端飞行状态缓存
    @SideOnly(Side.CLIENT)
    public static class ClientFlightStateCache {
        private static final java.util.Map<UUID, FlightState> playerStates = new java.util.HashMap<>();

        public static void updatePlayerState(UUID playerUUID, boolean isFlying, boolean isHovering,
                                             float horizontalSpeed, float verticalSpeed) {
            FlightState state = playerStates.get(playerUUID);
            if (state == null) {
                state = new FlightState();
                playerStates.put(playerUUID, state);
            }

            state.isFlying = isFlying;
            state.isHovering = isHovering;
            state.horizontalSpeed = horizontalSpeed;
            state.verticalSpeed = verticalSpeed;
            state.lastUpdate = System.currentTimeMillis();
        }

        public static FlightState getPlayerState(UUID playerUUID) {
            FlightState state = playerStates.get(playerUUID);
            if (state != null && System.currentTimeMillis() - state.lastUpdate > 5000) {
                // 5秒后过期
                playerStates.remove(playerUUID);
                return null;
            }
            return state;
        }

        public static class FlightState {
            public boolean isFlying = false;
            public boolean isHovering = false;
            public float horizontalSpeed = 0.0F;
            public float verticalSpeed = 0.0F;
            public long lastUpdate = 0;
        }
    }

    // 动画同步管理器
    public static class AnimationSyncManager {
        private static long lastSyncTime = 0;
        private static final long SYNC_INTERVAL = 100; // 每100ms同步一次

        // 发送动画状态到服务端和其他客户端
        public static void syncAnimationState(EntityPlayer player, boolean isFlying, boolean isHovering) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSyncTime < SYNC_INTERVAL) {
                return; // 限制同步频率
            }

            lastSyncTime = currentTime;

            // 创建动画数据包
            PacketJetpackAnimation packet = new PacketJetpackAnimation(player, isFlying, isHovering);

            // 发送到服务端（如果是客户端）
            if (player.world.isRemote) {
                // 这里需要你的网络处理器来发送数据包
                // PacketHandler.INSTANCE.sendToServer(packet);
            }
        }

        // 验证动画状态的合理性
        public static boolean validateAnimationState(EntityPlayer player, boolean isFlying, boolean isHovering) {
            // 基础验证
            if (isFlying && player.onGround) {
                return false; // 在地面上不能飞行
            }

            if (isHovering && !isFlying) {
                return false; // 悬停必须在飞行状态
            }

            // 速度验证
            double speed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
            if (isFlying && speed > 5.0) {
                return false; // 速度过快，可能是作弊
            }

            return true;
        }

        // 平滑插值动画状态
        public static FlightAnimationData interpolateFlightState(EntityPlayer player,
                                                                 FlightAnimationData current,
                                                                 FlightAnimationData target,
                                                                 float partialTicks) {
            FlightAnimationData result = new FlightAnimationData();

            // 线性插值
            result.bodyTilt = lerp(current.bodyTilt, target.bodyTilt, partialTicks * 0.1F);
            result.armRotationX = lerp(current.armRotationX, target.armRotationX, partialTicks * 0.1F);
            result.armRotationZ = lerp(current.armRotationZ, target.armRotationZ, partialTicks * 0.1F);
            result.legRotation = lerp(current.legRotation, target.legRotation, partialTicks * 0.1F);

            return result;
        }

        private static float lerp(float start, float end, float alpha) {
            return start + (end - start) * MathHelper.clamp(alpha, 0.0F, 1.0F);
        }
    }

    // 飞行动画数据
    public static class FlightAnimationData {
        public float bodyTilt = 0.0F;
        public float armRotationX = 0.0F;
        public float armRotationZ = 0.0F;
        public float legRotation = 0.0F;
        public float headTilt = 0.0F;

        public FlightAnimationData() {}

        public FlightAnimationData(float bodyTilt, float armRotationX, float armRotationZ, float legRotation) {
            this.bodyTilt = bodyTilt;
            this.armRotationX = armRotationX;
            this.armRotationZ = armRotationZ;
            this.legRotation = legRotation;
        }

        public void reset() {
            bodyTilt = 0.0F;
            armRotationX = 0.0F;
            armRotationZ = 0.0F;
            legRotation = 0.0F;
            headTilt = 0.0F;
        }
    }
}