package com.moremod.tile;

import com.moremod.multiblock.MultiblockRespawnChamber;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
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
 * - 能量存儲（死亡拦截需要70%以上電量）
 * - 站在重生仓内给予回血效果
 */
public class TileEntityRespawnChamberCore extends TileEntity implements ITickable {

    // ========== 能量配置 ==========
    private static final int ENERGY_CAPACITY = 1_000_000;  // 1M RF
    private static final int MAX_RECEIVE = 10_000;          // 10K RF/t 輸入
    private static final int DEATH_INTERCEPT_THRESHOLD = 70; // 死亡拦截需要70%電量
    private static final int HEAL_INTERVAL = 40;            // 每2秒回血一次 (40 ticks)
    private static final int HEAL_AMPLIFIER = 1;            // 回血等級 (II級)
    private static final int HEAL_DURATION = 60;            // 回血持續時間 (3秒)
    private static final int ENERGY_PER_HEAL = 100;         // 每次回血消耗 100 RF

    // 能量存儲
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                markDirty();
            }
            return received;
        }
    };

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

        // 結構完整時的功能
        if (structureValid) {
            // 主動從周圍方塊抽取能量
            if (energy.getEnergyStored() < energy.getMaxEnergyStored()) {
                pullEnergyFromNeighbors();
            }

            // 每 HEAL_INTERVAL ticks 給站在重生仓内的玩家回血
            if (tickCounter % HEAL_INTERVAL == 0 && energy.getEnergyStored() >= ENERGY_PER_HEAL) {
                healPlayersInChamber();
            }

            // 粒子效果（結構完整且有綁定時）
            if (boundPlayerUUID != null && tickCounter % 40 == 0) {
                spawnAmbientParticles();
            }
        }
    }

    /**
     * 主動從周圍方塊抽取能量
     */
    private void pullEnergyFromNeighbors() {
        int spaceAvailable = energy.getMaxEnergyStored() - energy.getEnergyStored();
        if (spaceAvailable <= 0) return;

        int toPull = Math.min(MAX_RECEIVE, spaceAvailable);

        for (EnumFacing facing : EnumFacing.values()) {
            if (toPull <= 0) break;

            TileEntity neighbor = world.getTileEntity(pos.offset(facing));
            if (neighbor != null && neighbor.hasCapability(CapabilityEnergy.ENERGY, facing.getOpposite())) {
                IEnergyStorage neighborEnergy = neighbor.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
                if (neighborEnergy != null && neighborEnergy.canExtract()) {
                    int extracted = neighborEnergy.extractEnergy(toPull, false);
                    if (extracted > 0) {
                        energy.receiveEnergy(extracted, false);
                        toPull -= extracted;
                    }
                }
            }
        }
    }

    /**
     * 給站在重生仓内的玩家回血
     */
    private void healPlayersInChamber() {
        // 玩家空間位於核心上方 (y+1)，檢測 3x2x3 區域
        BlockPos chamberCenter = pos.up();
        AxisAlignedBB area = new AxisAlignedBB(
                chamberCenter.getX() - 1, chamberCenter.getY(), chamberCenter.getZ() - 1,
                chamberCenter.getX() + 2, chamberCenter.getY() + 2, chamberCenter.getZ() + 2
        );

        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        for (EntityPlayer player : players) {
            // 給予再生效果
            player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, HEAL_DURATION, HEAL_AMPLIFIER, false, true));
            extractEnergyInternal(ENERGY_PER_HEAL);

            // 回血粒子效果
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.HEART,
                        player.posX, player.posY + 1.5, player.posZ,
                        3, 0.3, 0.3, 0.3, 0.0);
            }

            // 能量不足時停止
            if (energy.getEnergyStored() < ENERGY_PER_HEAL) break;
        }
    }

    /**
     * 內部提取能量
     */
    private void extractEnergyInternal(int amount) {
        int stored = energy.getEnergyStored();
        int toExtract = Math.min(amount, stored);
        if (toExtract > 0) {
            try {
                java.lang.reflect.Field field = EnergyStorage.class.getDeclaredField("energy");
                field.setAccessible(true);
                field.setInt(energy, stored - toExtract);
            } catch (Exception ignored) {}
            markDirty();
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
     * ⚠️ 修復：增加維度檢查，避免在地獄/末地調用時卡死
     */
    public static void teleportPlayerRandomly(EntityPlayer player) {
        // ⚠️ 安全檢查：如果玩家在地獄(-1)或末地(1)，不執行傳送
        // 因為 getTopSolidOrLiquidBlock 在這些維度可能導致卡死
        int dimension = player.dimension;
        if (dimension == -1 || dimension == 1) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚠ 檢測到異常維度，跳過隨機傳送"
            ));
            return;
        }

        // ⚠️ 安全檢查：確保世界對象有效
        if (player.world == null || !(player.world instanceof WorldServer)) {
            return;
        }

        try {
            double x = player.posX + (player.world.rand.nextDouble() - 0.5) * 20;
            double z = player.posZ + (player.world.rand.nextDouble() - 0.5) * 20;

            // 找到安全的Y坐標（添加超時保護）
            BlockPos targetPos = new BlockPos(x, player.posY, z);

            // ⚠️ 安全檢查：確保區塊已加載，避免觸發區塊生成導致卡頓
            if (!player.world.isBlockLoaded(targetPos)) {
                // 如果區塊未加載，使用玩家當前位置
                targetPos = player.getPosition();
            } else {
                targetPos = player.world.getTopSolidOrLiquidBlock(targetPos);
            }

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
        } catch (Exception e) {
            // 捕獲任何異常，避免崩潰
            System.err.println("[RespawnChamber] 隨機傳送失敗: " + e.getMessage());
        }
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

    // ========== 能量相關 ==========

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public int getEnergyPercent() {
        return (int) ((energy.getEnergyStored() * 100L) / energy.getMaxEnergyStored());
    }

    /**
     * 檢查是否可以拦截死亡
     * @return 是否可以拦截（結構完整 + 電量>=70%）
     */
    public boolean canInterceptDeath() {
        return structureValid && getEnergyPercent() >= DEATH_INTERCEPT_THRESHOLD;
    }

    /**
     * 消耗全部能量（用於死亡拦截）
     */
    public void consumeAllEnergy() {
        extractEnergyInternal(energy.getEnergyStored());
    }

    /**
     * 設置能量值（用於客戶端同步）
     */
    public void setClientEnergy(int value) {
        try {
            java.lang.reflect.Field field = EnergyStorage.class.getDeclaredField("energy");
            field.setAccessible(true);
            field.setInt(energy, Math.min(value, ENERGY_CAPACITY));
        } catch (Exception ignored) {}
    }

    // ========== Capability 支持 ==========

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) energy;
        }
        return super.getCapability(capability, facing);
    }

    // ========== NBT 序列化 ==========

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setInteger("Energy", energy.getEnergyStored());

        if (boundPlayerUUID != null) {
            compound.setString("BoundPlayerUUID", boundPlayerUUID.toString());
            compound.setString("BoundPlayerName", boundPlayerName);
        }

        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        if (compound.hasKey("Energy")) {
            setClientEnergy(compound.getInteger("Energy"));
        }

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
        tag.setInteger("Energy", energy.getEnergyStored());
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
        if (tag.hasKey("Energy")) {
            setClientEnergy(tag.getInteger("Energy"));
        }
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
