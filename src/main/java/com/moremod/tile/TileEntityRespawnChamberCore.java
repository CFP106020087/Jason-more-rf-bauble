package com.moremod.tile;

import com.moremod.multiblock.MultiblockRespawnChamber;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 重生倉核心 TileEntity
 *
 * 功能：
 * - 存儲綁定的玩家 UUID
 * - 檢測多方塊結構
 * - 提供傳送目標位置
 * - 在破碎之神停機重啟後將玩家傳送至此
 */
public class TileEntityRespawnChamberCore extends TileEntity implements ITickable {

    // 全局追踪：玩家UUID -> 重生倉位置（支持跨維度）
    private static final Map<UUID, RespawnChamberLocation> BOUND_CHAMBERS = new HashMap<>();

    // 綁定的玩家 UUID（本地存儲）
    private UUID boundPlayerUUID = null;
    private String boundPlayerName = "";

    // 結構狀態緩存
    private boolean structureValid = false;
    private int structureTier = 1;
    private int tickCounter = 0;

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        tickCounter++;

        // 每20tick檢測一次結構（1秒）
        if (tickCounter % 20 == 0) {
            updateStructureStatus();
        }

        // 粒子效果（結構完整且有綁定時）
        if (structureValid && boundPlayerUUID != null && tickCounter % 40 == 0) {
            spawnAmbientParticles();
        }
    }

    /**
     * 更新結構狀態
     */
    private void updateStructureStatus() {
        boolean wasValid = structureValid;
        structureValid = MultiblockRespawnChamber.checkStructure(world, pos);
        structureTier = structureValid ? MultiblockRespawnChamber.getFrameTier(world, pos) : 1;

        // 結構變為無效時，清除全局綁定
        if (wasValid && !structureValid && boundPlayerUUID != null) {
            BOUND_CHAMBERS.remove(boundPlayerUUID);
        }

        // 結構變為有效時，恢復全局綁定
        if (!wasValid && structureValid && boundPlayerUUID != null) {
            BOUND_CHAMBERS.put(boundPlayerUUID, new RespawnChamberLocation(world.provider.getDimension(), pos));
        }
    }

    /**
     * 綁定玩家
     */
    public boolean bindPlayer(EntityPlayer player) {
        if (!structureValid) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 重生倉結構不完整，無法綁定！"
            ));
            return false;
        }

        // 解除該玩家之前的綁定
        UUID playerUUID = player.getUniqueID();
        unbindPlayer(playerUUID);

        // 設置新綁定
        boundPlayerUUID = playerUUID;
        boundPlayerName = player.getName();

        // 更新全局追踪
        BOUND_CHAMBERS.put(playerUUID, new RespawnChamberLocation(world.provider.getDimension(), pos));

        markDirty();
        syncToClient();

        // 發送成功消息
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "═══════════════════════════════\n" +
                TextFormatting.GREEN + "✓ 重生倉綁定成功！\n" +
                TextFormatting.GRAY + "位置: " + TextFormatting.AQUA +
                        String.format("X:%d Y:%d Z:%d", pos.getX(), pos.getY(), pos.getZ()) + "\n" +
                TextFormatting.GRAY + "維度: " + TextFormatting.AQUA + world.provider.getDimension() + "\n" +
                TextFormatting.DARK_GRAY + "當機械核心停機重啟後，你將傳送至此。\n" +
                TextFormatting.GREEN + "═══════════════════════════════"
        ));

        // 綁定特效
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            BlockPos tp = MultiblockRespawnChamber.getTeleportPosition(pos);
            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    tp.getX() + 0.5, tp.getY() + 0.5, tp.getZ() + 0.5,
                    50, 0.5, 1.0, 0.5, 0.1);
            ws.spawnParticle(EnumParticleTypes.END_ROD,
                    tp.getX() + 0.5, tp.getY() + 1.0, tp.getZ() + 0.5,
                    20, 0.3, 0.3, 0.3, 0.05);
        }

        // 音效
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.BLOCK_PORTAL_TRIGGER,
                net.minecraft.util.SoundCategory.BLOCKS, 1.0f, 1.2f);

        return true;
    }

    /**
     * 解除玩家綁定
     */
    public void unbindPlayer(UUID playerUUID) {
        // 清除全局追踪
        RespawnChamberLocation oldLoc = BOUND_CHAMBERS.get(playerUUID);
        if (oldLoc != null) {
            BOUND_CHAMBERS.remove(playerUUID);
        }

        // 如果是本重生倉的綁定，清除本地數據
        if (boundPlayerUUID != null && boundPlayerUUID.equals(playerUUID)) {
            boundPlayerUUID = null;
            boundPlayerName = "";
            markDirty();
            syncToClient();
        }
    }

    /**
     * 獲取玩家綁定的重生倉位置（靜態方法供外部調用）
     */
    public static RespawnChamberLocation getBoundChamber(UUID playerUUID) {
        return BOUND_CHAMBERS.get(playerUUID);
    }

    /**
     * 檢查玩家是否有綁定的重生倉
     */
    public static boolean hasBoundChamber(UUID playerUUID) {
        return BOUND_CHAMBERS.containsKey(playerUUID);
    }

    /**
     * 傳送玩家到重生倉
     * @return 是否傳送成功
     */
    public static boolean teleportPlayerToChamber(EntityPlayer player) {
        UUID playerUUID = player.getUniqueID();
        RespawnChamberLocation loc = BOUND_CHAMBERS.get(playerUUID);

        if (loc == null) {
            return false;
        }

        // 獲取目標世界
        World targetWorld = player.getServer().getWorld(loc.dimension);
        if (targetWorld == null) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 重生倉所在維度不可用！"
            ));
            return false;
        }

        // 檢查重生倉結構是否完整
        TileEntity te = targetWorld.getTileEntity(loc.pos);
        if (!(te instanceof TileEntityRespawnChamberCore)) {
            // 重生倉被破壞
            BOUND_CHAMBERS.remove(playerUUID);
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 重生倉已被破壞！隨機傳送中..."
            ));
            return false;
        }

        TileEntityRespawnChamberCore chamber = (TileEntityRespawnChamberCore) te;
        if (!chamber.structureValid) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 重生倉結構不完整！隨機傳送中..."
            ));
            return false;
        }

        // 獲取傳送目標位置
        BlockPos teleportPos = MultiblockRespawnChamber.getTeleportPosition(loc.pos);

        // 跨維度傳送
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;

            if (playerMP.dimension != loc.dimension) {
                // 跨維度傳送
                playerMP.changeDimension(loc.dimension, new net.minecraft.world.Teleporter((WorldServer) targetWorld) {
                    @Override
                    public void placeInPortal(net.minecraft.entity.Entity entity, float rotationYaw) {
                        entity.setPositionAndUpdate(
                                teleportPos.getX() + 0.5,
                                teleportPos.getY(),
                                teleportPos.getZ() + 0.5
                        );
                    }
                });
            } else {
                // 同維度傳送
                playerMP.setPositionAndUpdate(
                        teleportPos.getX() + 0.5,
                        teleportPos.getY(),
                        teleportPos.getZ() + 0.5
                );
            }
        }

        // 傳送特效
        if (targetWorld instanceof WorldServer) {
            WorldServer ws = (WorldServer) targetWorld;
            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    teleportPos.getX() + 0.5, teleportPos.getY() + 0.5, teleportPos.getZ() + 0.5,
                    100, 0.5, 1.0, 0.5, 0.2);
            ws.spawnParticle(EnumParticleTypes.END_ROD,
                    teleportPos.getX() + 0.5, teleportPos.getY() + 1.0, teleportPos.getZ() + 0.5,
                    30, 0.3, 0.5, 0.3, 0.1);
        }

        // 音效
        targetWorld.playSound(null,
                teleportPos.getX(), teleportPos.getY(), teleportPos.getZ(),
                net.minecraft.init.SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 0.8f);

        // 發送成功消息
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "═══════════════════════════════\n" +
                TextFormatting.GREEN + "✓ 已傳送至重生倉\n" +
                TextFormatting.GRAY + "系統重啟完成，核心狀態: " + TextFormatting.GREEN + "在線\n" +
                TextFormatting.GREEN + "═══════════════════════════════"
        ));

        return true;
    }

    /**
     * 隨機傳送玩家到附近（無綁定時使用）
     */
    public static void teleportPlayerRandomly(EntityPlayer player) {
        double x = player.posX + (player.world.rand.nextDouble() - 0.5) * 20;
        double z = player.posZ + (player.world.rand.nextDouble() - 0.5) * 20;

        // 找到安全的Y坐標
        BlockPos targetPos = new BlockPos(x, player.posY, z);
        targetPos = player.world.getTopSolidOrLiquidBlock(targetPos);

        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).setPositionAndUpdate(
                    targetPos.getX() + 0.5,
                    targetPos.getY() + 1,
                    targetPos.getZ() + 0.5
            );
        }

        // 隨機傳送特效
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    player.posX, player.posY + 1, player.posZ,
                    50, 0.5, 1.0, 0.5, 0.1);
        }

        // 音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 0.5f);

        player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "═══════════════════════════════\n" +
                TextFormatting.YELLOW + "⚠ 未綁定重生倉！\n" +
                TextFormatting.GRAY + "已隨機傳送至附近位置。\n" +
                TextFormatting.DARK_GRAY + "建造重生倉並綁定以獲得穩定重生點。\n" +
                TextFormatting.YELLOW + "═══════════════════════════════"
        ));
    }

    /**
     * 生成環境粒子效果
     */
    private void spawnAmbientParticles() {
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            BlockPos tp = MultiblockRespawnChamber.getTeleportPosition(pos);
            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    tp.getX() + 0.5, tp.getY() + 0.5, tp.getZ() + 0.5,
                    3, 0.3, 0.3, 0.3, 0.02);
        }
    }

    /**
     * 同步到客戶端
     */
    private void syncToClient() {
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    // ========== Getter 方法 ==========

    public boolean isStructureValid() {
        return structureValid;
    }

    public int getStructureTier() {
        return structureTier;
    }

    public UUID getBoundPlayerUUID() {
        return boundPlayerUUID;
    }

    public String getBoundPlayerName() {
        return boundPlayerName;
    }

    public boolean hasBoundPlayer() {
        return boundPlayerUUID != null;
    }

    // ========== NBT 序列化 ==========

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        if (boundPlayerUUID != null) {
            compound.setString("BoundPlayerUUID", boundPlayerUUID.toString());
            compound.setString("BoundPlayerName", boundPlayerName);
        }

        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        if (compound.hasKey("BoundPlayerUUID")) {
            try {
                boundPlayerUUID = UUID.fromString(compound.getString("BoundPlayerUUID"));
                boundPlayerName = compound.getString("BoundPlayerName");
            } catch (IllegalArgumentException e) {
                boundPlayerUUID = null;
                boundPlayerName = "";
            }
        } else {
            boundPlayerUUID = null;
            boundPlayerName = "";
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // 加載時恢復全局追踪
        if (!world.isRemote && boundPlayerUUID != null) {
            BOUND_CHAMBERS.put(boundPlayerUUID, new RespawnChamberLocation(world.provider.getDimension(), pos));
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        // 方塊被破壞時清除全局追踪
        if (!world.isRemote && boundPlayerUUID != null) {
            BOUND_CHAMBERS.remove(boundPlayerUUID);
        }
    }

    // ========== 網絡同步 ==========

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setBoolean("StructureValid", structureValid);
        tag.setInteger("StructureTier", structureTier);
        if (boundPlayerUUID != null) {
            tag.setString("BoundPlayerName", boundPlayerName);
        }
        return tag;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound tag = pkt.getNbtCompound();
        structureValid = tag.getBoolean("StructureValid");
        structureTier = tag.getInteger("StructureTier");
        if (tag.hasKey("BoundPlayerName")) {
            boundPlayerName = tag.getString("BoundPlayerName");
        } else {
            boundPlayerName = "";
        }
    }

    /**
     * 重生倉位置記錄
     */
    public static class RespawnChamberLocation {
        public final int dimension;
        public final BlockPos pos;

        public RespawnChamberLocation(int dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos;
        }
    }
}
