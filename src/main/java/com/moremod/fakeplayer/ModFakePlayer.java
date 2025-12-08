package com.moremod.fakeplayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * 假玩家实现 - 通过仪式从玩家头颅创建
 * 可以模拟玩家的各种操作：右键、攻击、挖掘等
 * 参考 Cyclic mod 的 UtilFakePlayer 实现
 */
public class ModFakePlayer extends FakePlayer {

    private static final GameProfile DEFAULT_PROFILE = new GameProfile(
        UUID.fromString("41C82C87-7AfB-4024-BA57-13D2C99CAE77"),
        "[MoreMod_FakePlayer]"
    );

    // 源玩家信息
    @Nullable
    private final GameProfile sourceProfile;
    @Nullable
    private final String customName;

    // 缓存的装备（用于属性计算）
    private final ItemStack[] cachedHandInventory = new ItemStack[2];
    private final ItemStack[] cachedArmorArray = new ItemStack[4];

    // 控制器引用
    private WeakReference<BlockPos> controllerPos;

    // 网络连接已初始化标志
    private boolean connectionInitialized = false;

    public ModFakePlayer(WorldServer world, @Nullable GameProfile sourceProfile, @Nullable String customName) {
        super(world, sourceProfile != null ? sourceProfile : DEFAULT_PROFILE);
        this.sourceProfile = sourceProfile;
        this.customName = customName;

        // 初始化
        setSize(0.6F, 1.8F);
        capabilities.disableDamage = true;

        // 初始化网络连接（参考 Cyclic）
        initConnection();
    }

    /**
     * 初始化假玩家的网络连接
     * 防止某些操作因为 connection 为 null 而崩溃
     */
    private void initConnection() {
        if (connectionInitialized) return;

        try {
            this.connection = new NetHandlerPlayServer(
                FMLCommonHandler.instance().getMinecraftServerInstance(),
                new NetworkManager(EnumPacketDirection.SERVERBOUND),
                this
            ) {
                @SuppressWarnings("rawtypes")
                @Override
                public void sendPacket(Packet packetIn) {
                    // 假玩家不需要发送网络包
                }
            };
            this.onGround = true;
            this.setSilent(true);
            connectionInitialized = true;
        } catch (Exception e) {
            System.err.println("[MoreMod] Failed to initialize fake player connection: " + e.getMessage());
        }
    }

    /**
     * 设置假玩家面向的方向和位置
     */
    public void setLocationAndFacing(BlockPos targetPos, EnumFacing facing) {
        double x = targetPos.getX() + 0.5 - facing.getXOffset() * 0.5;
        double y = targetPos.getY() + 0.5 - facing.getYOffset() * 0.5;
        double z = targetPos.getZ() + 0.5 - facing.getZOffset() * 0.5;

        float yaw;
        float pitch;

        switch (facing) {
            case DOWN:
                pitch = 90;
                yaw = 0;
                break;
            case UP:
                pitch = -90;
                yaw = 0;
                break;
            case NORTH:
                yaw = 180;
                pitch = 0;
                break;
            case SOUTH:
                yaw = 0;
                pitch = 0;
                break;
            case WEST:
                yaw = 90;
                pitch = 0;
                break;
            case EAST:
                yaw = -90;
                pitch = 0;
                break;
            default:
                yaw = 0;
                pitch = 0;
        }

        setLocationAndAngles(x, y, z, yaw, pitch);
        // 确保旋转也更新
        this.rotationYawHead = yaw;
        this.prevRotationYawHead = yaw;
    }

    /**
     * 设置控制器位置
     */
    public void setControllerPos(BlockPos pos) {
        this.controllerPos = new WeakReference<>(pos);
    }

    /**
     * 获取控制器位置
     */
    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos != null ? controllerPos.get() : null;
    }

    @Override
    public float getEyeHeight() {
        return 1.62F;
    }

    /**
     * 执行射线追踪
     */
    public RayTraceResult trace(double blockReachDistance) {
        Vec3d start = new Vec3d(this.posX, this.posY + this.getEyeHeight(), this.posZ);
        Vec3d look = this.getLook(1.0F);
        Vec3d end = start.add(look.x * blockReachDistance, look.y * blockReachDistance, look.z * blockReachDistance);
        return this.world.rayTraceBlocks(start, end, false, false, true);
    }

    @Nonnull
    @Override
    public Vec3d getPositionEyes(float partialTicks) {
        return new Vec3d(this.posX, this.posY + this.getEyeHeight(), this.posZ);
    }

    @Override
    public void setActiveHand(@Nonnull EnumHand hand) {
        // 假玩家支持使用物品
        super.setActiveHand(hand);
    }

    /**
     * 清空背包
     */
    public void clearInventory() {
        inventory.clear();
    }

    /**
     * 更新攻击冷却
     */
    public void updateCooldown() {
        this.ticksSinceLastSwing = 20090;
    }

    /**
     * 重置攻击冷却（用于持续攻击）
     */
    public void resetCooldown() {
        this.ticksSinceLastSwing = 0;
    }

    /**
     * 更新装备属性
     */
    public void updateEquipmentAttributes() {
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack cachedStack;

            switch (slot.getSlotType()) {
                case HAND:
                    cachedStack = safeCopy(this.cachedHandInventory[slot.getIndex()]);
                    break;
                case ARMOR:
                    cachedStack = safeCopy(this.cachedArmorArray[slot.getIndex()]);
                    break;
                default:
                    continue;
            }

            ItemStack currentStack = this.getItemStackFromSlot(slot);

            if (!ItemStack.areItemStacksEqual(currentStack, cachedStack)) {
                if (!cachedStack.isEmpty()) {
                    this.getAttributeMap().removeAttributeModifiers(cachedStack.getAttributeModifiers(slot));
                }

                if (!currentStack.isEmpty()) {
                    this.getAttributeMap().applyAttributeModifiers(currentStack.getAttributeModifiers(slot));
                }

                switch (slot.getSlotType()) {
                    case HAND:
                        this.cachedHandInventory[slot.getIndex()] = safeCopy(currentStack);
                        break;
                    case ARMOR:
                        this.cachedArmorArray[slot.getIndex()] = safeCopy(currentStack);
                        break;
                }
            }
        }
    }

    private static ItemStack safeCopy(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName() {
        StringBuilder sb = new StringBuilder("[");

        if (customName != null && !customName.isEmpty()) {
            sb.append(customName);
        } else {
            sb.append("FakePlayer");
        }

        if (sourceProfile != null && sourceProfile.getName() != null) {
            sb.append(" - ").append(sourceProfile.getName());
        }

        sb.append("]");
        return new TextComponentString(sb.toString());
    }

    /**
     * 获取源玩家的GameProfile
     */
    @Nullable
    public GameProfile getSourceProfile() {
        return sourceProfile;
    }

    /**
     * 模拟玩家挖掘方块
     */
    public boolean simulateBlockBreak(BlockPos targetPos) {
        if (world == null || world.isRemote) return false;

        try {
            // 使用 interactionManager 来挖掘方块
            if (this.interactionManager != null) {
                return this.interactionManager.tryHarvestBlock(targetPos);
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return false;
    }
}
