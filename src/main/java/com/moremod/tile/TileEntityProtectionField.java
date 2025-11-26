// 文件：com/moremod/tile/TileEntityProtectionField.java
package com.moremod.tile;

import com.moremod.moremod;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;


import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保护领域发生器 TileEntity（修改版）
 * - 维护能量/范围/激活态
 * - 攻击或开箱子会导致10秒保护失效
 * - 服务端：定期清仇恨、同步客户端/比较器/光照
 * - 客户端：粒子与 TESR
 */
public class TileEntityProtectionField extends TileEntity implements ITickable, IEnergyStorage {

    // ===== 能量参数 =====
    private static final int MAX_ENERGY         = 1_000_000;
    private static final int MAX_RECEIVE        =   10_000;
    private static final int ENERGY_PER_TICK    =      100;
    private static final int MIN_ENERGY_TO_WORK =    1_000;

    // ===== 冷却时间 =====
    private static final long COOLDOWN_TIME = 10000; // 10秒 (毫秒)

    // ===== 状态 =====
    private int  energyStored = 0;
    private boolean active    = false;

    // ===== 领域参数 =====
    private int  range = 16;       // 方块格半径
    private int  tickCounter = 0;  // 客户端/服务端均自增

    // 受保护玩家缓存（仅服务端）
    private final Set<UUID> protectedPlayers = new HashSet<>();

    // 玩家违规时间戳记录（全局静态，跨所有保护领域共享）
    private static final Map<UUID, Long> playerViolationTimestamps = new ConcurrentHashMap<>();

    // ============== Tick ==============
    @Override
    public void update() {
        tickCounter++;

        if (!world.isRemote) {
            // 每 10 tick (~0.5s) 刷一次受保护列表
            if (tickCounter % 10 == 0) {
                rebuildProtectedPlayers();
            }

            // 能量判定与切换激活
            boolean canWork = energyStored >= ENERGY_PER_TICK && energyStored >= MIN_ENERGY_TO_WORK;
            setActiveInternal(canWork);

            if (active) {
                // 消耗能量
                energyStored -= ENERGY_PER_TICK;
                markDirty();

                // 清仇恨（每 5 tick）
                if (tickCounter % 5 == 0) {
                    clearHostilityAround();
                }
            }

            // 每秒刷新比较器输出
            if (tickCounter % 20 == 0) {
                IBlockState s = world.getBlockState(pos);
                world.updateComparatorOutputLevel(pos, s.getBlock());

                // 清理过期的违规记录
                cleanupExpiredViolations();
            }

        } else {
            // 客户端粒子
            if (active) {
                spawnProtectionParticles();
            }
        }
    }

    // ============== 清理过期违规记录 ==============
    private void cleanupExpiredViolations() {
        long currentTime = System.currentTimeMillis();
        playerViolationTimestamps.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > COOLDOWN_TIME
        );
    }

    // ============== 清仇恨核心逻辑（服务端） ==============
    private void clearHostilityAround() {
        if (!active) return;

        BlockPos p = this.getPos();
        final int extra = 10; // 外扩以覆盖将靠近的怪
        AxisAlignedBB search = new AxisAlignedBB(
                p.getX() - range - extra, p.getY() - range - extra, p.getZ() - range - extra,
                p.getX() + range + extra + 1, p.getY() + range + extra + 1, p.getZ() + range + extra + 1
        );

        List<EntityLiving> mobs = world.getEntitiesWithinAABB(EntityLiving.class, search);
        for (EntityLiving mob : mobs) {
            EntityLivingBase at = mob.getAttackTarget();
            if (at instanceof EntityPlayer && isPlayerProtectedLocal((EntityPlayer) at)) {
                mob.setAttackTarget(null);
                mob.setRevengeTarget(null);
                mob.setLastAttackedEntity(null);
                mob.getNavigator().clearPath();
            }
            EntityLivingBase rv = mob.getRevengeTarget();
            if (rv instanceof EntityPlayer && isPlayerProtectedLocal((EntityPlayer) rv)) {
                mob.setRevengeTarget(null);
                mob.setLastAttackedEntity(null);
                mob.getNavigator().clearPath();
            }
        }
    }

    // ============== 受保护玩家列表构建（服务端） ==============
    private void rebuildProtectedPlayers() {
        protectedPlayers.clear();
        if (!active) return;

        BlockPos p = this.getPos();
        AxisAlignedBB aabb = new AxisAlignedBB(
                p.getX() - range, p.getY() - range, p.getZ() - range,
                p.getX() + range + 1, p.getY() + range + 1, p.getZ() + range + 1
        );

        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, aabb);
        for (EntityPlayer pl : players) {
            if (!pl.isCreative() && !pl.isSpectator()) {
                protectedPlayers.add(pl.getUniqueID());
            }
        }
    }

    // ============== 客户端粒子（修改为显示警告粒子） ==============
    // ============== 客戶端粒子（修改為使用反射） ==============
    private void spawnProtectionParticles() {
        // 使用反射獲取客戶端玩家，避免直接引用
        EntityPlayer localPlayer = getClientPlayer();
        boolean isOnCooldown = localPlayer != null && isPlayerOnCooldown(localPlayer);

        // 邊界點粒子
        if (tickCounter % 2 == 0) {
            int cnt = 12;
            BlockPos p = this.getPos();
            double cx = p.getX() + 0.5, cy = p.getY() + 0.5, cz = p.getZ() + 0.5;
            for (int i = 0; i < cnt; i++) {
                double t  = world.rand.nextDouble() * Math.PI * 2;
                double ph = world.rand.nextDouble() * Math.PI;
                double x = cx + range * Math.sin(ph) * Math.cos(t);
                double y = cy + range * Math.cos(ph);
                double z = cz + range * Math.sin(ph) * Math.sin(t);

                // 根據冷卻狀態顯示不同顏色
                if (isOnCooldown) {
                    world.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, 1.0, 0.0, 0.0); // 紅色
                } else {
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB, x, y, z, 0.5, 1.0, 1.0); // 青色
                }
            }
        }

        // 地面環
        if (tickCounter % 10 == 0) {
            int ring = 48;
            BlockPos p = this.getPos();
            double cx = p.getX() + 0.5, cz = p.getZ() + 0.5;
            for (int i = 0; i < ring; i++) {
                double a = (Math.PI * 2 * i) / ring;
                double x = cx + range * Math.cos(a);
                double z = cz + range * Math.sin(a);
                for (int h = 0; h < 3; h++) {
                    double y = p.getY() + h * 0.5;
                    // REDSTONE 在 1.12 用 dx,dy,dz 表示顏色分量（0..1）
                    if (isOnCooldown) {
                        world.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, 1.0, 0.0, 0.0); // 紅色警告
                    } else {
                        world.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, 0.0, 1.0, 1.0); // 青色正常
                    }
                }
            }
        }

        // 中心能量柱
        if (tickCounter % 3 == 0) {
            BlockPos p = this.getPos();
            double cx = p.getX() + 0.5, cy = p.getY() + 0.5, cz = p.getZ() + 0.5;
            for (int i = 0; i < 5; i++) {
                double yOff = i * 0.3;
                world.spawnParticle(EnumParticleTypes.SPELL_WITCH, cx, cy + yOff, cz, 0, 0.1, 0);
                double rot = (tickCounter * 0.1 + i * 0.5) % (Math.PI * 2);
                world.spawnParticle(EnumParticleTypes.PORTAL,
                        cx + Math.cos(rot) * 0.5, cy + yOff, cz + Math.sin(rot) * 0.5,
                        0, 0.05, 0);
            }
        }
    }

    /**
     * 使用反射獲取客戶端玩家，避免服務器載入客戶端類
     * @return 客戶端玩家實體，如果在服務器端或獲取失敗則返回 null
     */
    private EntityPlayer getClientPlayer() {
        try {
            // 使用反射獲取 net.minecraft.client.Minecraft 類
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");

            // 調用 getMinecraft() 方法獲取 Minecraft 實例
            Object minecraftInstance = minecraftClass.getMethod("getMinecraft").invoke(null);
            if (minecraftInstance == null) {
                return null;
            }

            // 嘗試使用 MCP 名稱獲取 player 字段
            try {
                java.lang.reflect.Field playerField = minecraftClass.getDeclaredField("player");
                playerField.setAccessible(true);
                Object playerObj = playerField.get(minecraftInstance);
                if (playerObj instanceof EntityPlayer) {
                    return (EntityPlayer) playerObj;
                }
            } catch (NoSuchFieldException e) {
                // 如果 MCP 名稱失敗，嘗試 SRG 名稱 field_71439_g
                try {
                    java.lang.reflect.Field playerField = minecraftClass.getDeclaredField("field_71439_g");
                    playerField.setAccessible(true);
                    Object playerObj = playerField.get(minecraftInstance);
                    if (playerObj instanceof EntityPlayer) {
                        return (EntityPlayer) playerObj;
                    }
                } catch (NoSuchFieldException e2) {
                    // 兩個名稱都失敗，記錄錯誤
                    System.err.println("[TileEntityProtectionField] Failed to find player field in Minecraft class");
                }
            }
        } catch (ClassNotFoundException e) {
            // 這在服務器端是正常的，因為服務器沒有客戶端類
            // 不需要記錄錯誤
        } catch (Exception e) {
            // 其他錯誤，記錄但不崩潰
            System.err.println("[TileEntityProtectionField] Error getting client player via reflection: " + e.getMessage());
        }

        return null;
    }

    // ============== 同步 & 切换激活 ==============
    private void setActiveInternal(boolean v) {
        if (this.active != v) {
            this.active = v;
            if (!v) protectedPlayers.clear();
            markDirty();
            if (!world.isRemote) {
                syncBlock();
            }
        }
    }

    private void syncBlock() {
        IBlockState s = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, s, s, 3);
        world.markBlockRangeForRenderUpdate(pos, pos);
        world.updateComparatorOutputLevel(pos, s.getBlock());
        world.checkLight(pos);
    }

    // ============== IEnergyStorage ==============
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        int add = Math.min(MAX_ENERGY - energyStored, Math.min(MAX_RECEIVE, maxReceive));
        if (!simulate && add > 0) {
            energyStored += add;
            markDirty();
            if (!world.isRemote) {
                world.updateComparatorOutputLevel(pos, world.getBlockState(pos).getBlock());
            }
        }
        return add;
    }

    @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
    @Override public int getEnergyStored()   { return energyStored; }
    @Override public int getMaxEnergyStored(){ return MAX_ENERGY; }
    @Override public boolean canExtract()    { return false; }
    @Override public boolean canReceive()    { return true; }

    // ============== 渲染范围（TESR 不被裁剪） ==============
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return TileEntity.INFINITE_EXTENT_AABB;
    }
    @Override
    public double getMaxRenderDistanceSquared() {
        return 65536.0D; // 256 格
    }

    // ============== NBT / 同步 ==============
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound c) {
        super.writeToNBT(c);
        c.setInteger("Energy", energyStored);
        c.setBoolean("Active", active);
        c.setInteger("Range", range);
        return c;
    }
    @Override
    public void readFromNBT(NBTTagCompound c) {
        super.readFromNBT(c);
        energyStored = c.getInteger("Energy");
        active = c.getBoolean("Active");
        range = c.getInteger("Range");
    }
    @Override public NBTTagCompound getUpdateTag() {
        NBTTagCompound t = super.getUpdateTag();
        t.setBoolean("Active", active);
        t.setInteger("Range", range);
        t.setInteger("Energy", energyStored);
        return t;
    }
    @Override public void handleUpdateTag(NBTTagCompound t) {
        super.handleUpdateTag(t);
        active = t.getBoolean("Active");
        range = t.getInteger("Range");
        energyStored = t.getInteger("Energy");
    }
    @Override public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }
    @Override public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    // ============== Capability ==============
    @Override
    public boolean hasCapability(Capability<?> cap, @Nullable EnumFacing f) {
        return cap == CapabilityEnergy.ENERGY || super.hasCapability(cap, f);
    }
    @Override
    @Nullable
    public <T> T getCapability(Capability<T> cap, @Nullable EnumFacing f) {
        if (cap == CapabilityEnergy.ENERGY) return CapabilityEnergy.ENERGY.cast(this);
        return super.getCapability(cap, f);
    }

    // ============== 对外/本地 API ==============
    public boolean isActive()               { return active; }
    public int getRange()                   { return range; }
    public void setRange(int r)             { this.range = Math.max(1, Math.min(64, r)); markDirty(); if(!world.isRemote) syncBlock(); }
    public int getEnergyPerTick()           { return ENERGY_PER_TICK; }

    // 检查玩家是否在冷却中
    public static boolean isPlayerOnCooldown(EntityPlayer p) {
        Long violationTime = playerViolationTimestamps.get(p.getUniqueID());
        if (violationTime == null) return false;
        return System.currentTimeMillis() - violationTime < COOLDOWN_TIME;
    }

    // 记录玩家违规
    public static void recordPlayerViolation(EntityPlayer p) {
        playerViolationTimestamps.put(p.getUniqueID(), System.currentTimeMillis());
        // 发送警告消息
        if (!p.world.isRemote) {
            p.sendMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 保护失效10秒！" + TextFormatting.GRAY + " (攻击或开箱导致)"));
        }
    }

    // 获取剩余冷却时间（秒）
    public static int getRemainingCooldown(EntityPlayer p) {
        Long violationTime = playerViolationTimestamps.get(p.getUniqueID());
        if (violationTime == null) return 0;
        long elapsed = System.currentTimeMillis() - violationTime;
        if (elapsed >= COOLDOWN_TIME) return 0;
        return (int)((COOLDOWN_TIME - elapsed) / 1000);
    }

    public boolean isPlayerProtectedLocal(EntityPlayer p) {
        // 检查冷却
        if (isPlayerOnCooldown(p)) return false;
        return active && protectedPlayers.contains(p.getUniqueID());
    }

    /** 全局判断：某玩家是否处于任一激活保护领域中 */
    public static boolean isPlayerProtectedGlobal(EntityPlayer p) {
        if (p == null) return false;
        // 首先检查是否在冷却中
        if (isPlayerOnCooldown(p)) return false;

        for (TileEntity te : p.world.loadedTileEntityList) {
            if (te instanceof TileEntityProtectionField) {
                TileEntityProtectionField f = (TileEntityProtectionField) te;
                if (f.active && f.isPlayerInRange(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 几何判断：玩家是否在本 TE 的范围内 */
    public boolean isPlayerInRange(EntityPlayer p) {
        if (!active) return false;
        double dx = p.posX - (pos.getX() + 0.5);
        double dy = p.posY - (pos.getY() + 0.5);
        double dz = p.posZ - (pos.getZ() + 0.5);
        return (dx*dx + dy*dy + dz*dz) <= (range * range);
    }

    // ================== 合并：事件拦截 ==================
    @Mod.EventBusSubscriber(modid = moremod.MODID)
    public static class Events {

        /** 玩家攻击实体时触发冷却 */
        @SubscribeEvent
        public static void onPlayerAttack(AttackEntityEvent e) {
            EntityPlayer player = e.getEntityPlayer();
            if (player.world.isRemote) return;

            // 检查玩家是否在任意保护领域内
            for (TileEntity te : player.world.loadedTileEntityList) {
                if (te instanceof TileEntityProtectionField) {
                    TileEntityProtectionField field = (TileEntityProtectionField) te;
                    if (field.active && field.isPlayerInRange(player)) {
                        // 记录违规
                        recordPlayerViolation(player);
                        break; // 只需记录一次
                    }
                }
            }
        }

        /** 玩家右键容器时触发冷却 */
        @SubscribeEvent
        public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock e) {
            if (e.getWorld().isRemote) return;

            EntityPlayer player = e.getEntityPlayer();
            BlockPos pos = e.getPos();
            TileEntity te = e.getWorld().getTileEntity(pos);

            // 检查是否是容器（箱子、熔炉等）
            if (te instanceof IInventory) {
                // 检查玩家是否在任意保护领域内
                for (TileEntity protectionTE : player.world.loadedTileEntityList) {
                    if (protectionTE instanceof TileEntityProtectionField) {
                        TileEntityProtectionField field = (TileEntityProtectionField) protectionTE;
                        if (field.active && field.isPlayerInRange(player)) {
                            // 记录违规
                            recordPlayerViolation(player);
                            break;
                        }
                    }
                }
            }
        }

        /** 怪物一旦把目标设为受保护玩家：立刻清空目标 */
        @SubscribeEvent
        public static void onSetAttackTarget(LivingSetAttackTargetEvent e) {
            if (!(e.getTarget() instanceof EntityPlayer)) return;
            EntityPlayer p = (EntityPlayer) e.getTarget();
            if (!isPlayerProtectedGlobal(p)) return;

            EntityLivingBase le = e.getEntityLiving();
            if (le instanceof EntityLiving) {
                EntityLiving mob = (EntityLiving) le;
                mob.setAttackTarget(null);
                mob.setRevengeTarget(null);
                mob.setLastAttackedEntity(null);
                mob.getNavigator().clearPath();
            } else {
                le.setRevengeTarget(null);
                le.setLastAttackedEntity(null);
            }
        }

        /** 降低受保护玩家的可见性 */
        @SubscribeEvent
        public static void onPlayerVisibility(PlayerEvent.Visibility e) {
            if (TileEntityProtectionField.isPlayerProtectedGlobal(e.getEntityPlayer())) {
                e.modifyVisibility(0.0D);
            }
        }

        /** 取消来自生物的伤害 */
        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent e) {
            if (!(e.getEntityLiving() instanceof EntityPlayer)) return;
            EntityPlayer p = (EntityPlayer) e.getEntityLiving();
            if (!isPlayerProtectedGlobal(p)) return;

            EntityLivingBase src = e.getSource().getTrueSource() instanceof EntityLivingBase
                    ? (EntityLivingBase) e.getSource().getTrueSource() : null;

            if (src != null) {
                e.setCanceled(true);
                if (src instanceof EntityLiving) {
                    EntityLiving mob = (EntityLiving) src;
                    mob.setAttackTarget(null);
                    mob.setRevengeTarget(null);
                    mob.setLastAttackedEntity(null);
                    mob.getNavigator().clearPath();
                } else {
                    src.setRevengeTarget(null);
                    src.setLastAttackedEntity(null);
                }
            }
        }
    }
}