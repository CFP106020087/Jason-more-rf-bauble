package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.player.IPlayer;
import crafttweaker.api.world.IWorld;
import crafttweaker.api.world.IBlockPos;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 传送助手（MC 1.12.2）
 * 二段射线：先近距探测并把起点推到命中方块外侧，再做远距射线；忽略无碰撞箱，真正按目光所及传送。
 */
@ZenRegister
@ZenClass("mods.moremod.TeleportHelper")
public class TeleportHelper {

    private static final boolean DEBUG = false; // 需要调试时改为 true

    // ===========================
    // 传送（二段射线稳妥版）
    // ===========================
    @ZenMethod
    public static boolean teleportToLookPos(IPlayer player, IWorld world, double maxDistance) {
        try {
            EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
            World mcWorld = CraftTweakerMC.getWorld(world);
            if (mcWorld.isRemote) return false;

            final Vec3d eye  = mcPlayer.getPositionEyes(1.0F);
            final Vec3d look = mcPlayer.getLook(1.0F);
            final Vec3d endFar = eye.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);

            // 1) 近距探测（6 格）：定位“近处第一块方块”
            final Vec3d shortEnd = eye.add(look.x * 6.0D, look.y * 6.0D, look.z * 6.0D);
            RayTraceResult nearHit = mcWorld.rayTraceBlocks(eye, shortEnd, false, true, false);
            if (DEBUG) {
                System.out.println("[TPDBG] eye=" + eye + " look=" + look + " endFar=" + endFar);
                System.out.println("[TPDBG] nearHit=" + (nearHit == null ? "null" :
                        (nearHit.typeOfHit + "@" + nearHit.getBlockPos() + " face=" + nearHit.sideHit)));
            }

            // 2) 把起点推到近处命中方块的外侧（沿面法线 0.501 格）
            Vec3d start2 = eye;
            if (nearHit != null && nearHit.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos np = nearHit.getBlockPos();
                EnumFacing nf = nearHit.sideHit;
                Vec3d n = new Vec3d(nf.getXOffset(), nf.getYOffset(), nf.getZOffset());
                // 方块中心 + 法线 * 0.501 => 一定在方块外
                start2 = new Vec3d(np.getX() + 0.5, np.getY() + 0.5, np.getZ() + 0.5)
                        .add(n.x * 0.501D, n.y * 0.501D, n.z * 0.501D);
            } else {
                // 没有近处遮挡：如果愿意，也可把起点推进到 reach+ε；保持为 eye 也可
                double reach = getServerReachDistance(mcPlayer) + 0.0625D;
                start2 = eye.add(look.x * reach, look.y * reach, look.z * reach);
            }

            // 3) 远距射线（忽略无碰撞箱：true）
            RayTraceResult farHit = mcWorld.rayTraceBlocks(start2, endFar, false, true, false);
            if (DEBUG) {
                System.out.println("[TPDBG] start2=" + start2);
                System.out.println("[TPDBG] farHit=" + (farHit == null ? "null" :
                        (farHit.typeOfHit + "@" + farHit.getBlockPos() + " face=" + farHit.sideHit)));
            }

            if (farHit != null && farHit.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos facePos = farHit.getBlockPos();
                BlockPos target  = facePos.offset(farHit.sideHit);

                // 两格高可站立检查（最多向上找 3 格）
                BlockPos safe = findSafeAbove(mcWorld, target, 3);
                if (safe == null) safe = target;

                mcPlayer.setPositionAndUpdate(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
                return true;
            } else {
                // 沿线没有命中方块：在终点向下找地面
                BlockPos endPos = new BlockPos(endFar.x, endFar.y, endFar.z);
                BlockPos ground = findGroundBelow(mcWorld, endPos, 64);
                if (DEBUG) System.out.println("[TPDBG] groundBelow=" + ground);
                if (ground != null) {
                    BlockPos stand = ground.up();
                    mcPlayer.setPositionAndUpdate(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
                    return true;
                }
                return false;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    // ===========================
    // 查询：与传送相同风格的射线
    // ===========================
    @ZenMethod
    public static IBlockPos getTargetBlock(IPlayer player, double maxDistance) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        World w = p.world;

        // 复用二段逻辑：避免近处方块
        final Vec3d eye  = p.getPositionEyes(1.0F);
        final Vec3d look = p.getLook(1.0F);
        final Vec3d endFar = eye.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);
        final Vec3d shortEnd = eye.add(look.x * 6.0D, look.y * 6.0D, look.z * 6.0D);
        RayTraceResult nearHit = w.rayTraceBlocks(eye, shortEnd, false, true, false);

        Vec3d start2 = eye;
        if (nearHit != null && nearHit.typeOfHit == RayTraceResult.Type.BLOCK) {
            BlockPos np = nearHit.getBlockPos();
            EnumFacing nf = nearHit.sideHit;
            Vec3d n = new Vec3d(nf.getXOffset(), nf.getYOffset(), nf.getZOffset());
            start2 = new Vec3d(np.getX() + 0.5, np.getY() + 0.5, np.getZ() + 0.5)
                    .add(n.x * 0.501D, n.y * 0.501D, n.z * 0.501D);
        }
        RayTraceResult farHit = w.rayTraceBlocks(start2, endFar, false, true, false);
        if (farHit != null && farHit.typeOfHit == RayTraceResult.Type.BLOCK) {
            return CraftTweakerMC.getIBlockPos(farHit.getBlockPos());
        }
        return null;
    }

    @ZenMethod
    public static double getTargetDistance(IPlayer player, double maxDistance) {
        EntityPlayer p = CraftTweakerMC.getPlayer(player);
        World w = p.world;

        IBlockPos ib = getTargetBlock(player, maxDistance);
        if (ib != null) {
            BlockPos t = CraftTweakerMC.getBlockPos(ib);
            double dx = t.getX() + 0.5 - p.posX;
            double dy = t.getY() + 0.5 - (p.posY + p.getEyeHeight());
            double dz = t.getZ() + 0.5 - p.posZ;
            return Math.sqrt(dx*dx + dy*dy + dz*dz);
        }
        return -1.0D;
    }

    // 直接传送到坐标
    @ZenMethod
    public static void teleportTo(IPlayer player, double x, double y, double z) {
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        mcPlayer.setPositionAndUpdate(x, y, z);
    }

    // ===========================
    // 冷却系统（保持原有接口）
    // ===========================
    @ZenMethod
    public static boolean isOnCooldown(IItemStack item, int cooldownTicks, long currentTime) {
        ItemStack s = CraftTweakerMC.getItemStack(item);
        if (!s.hasTagCompound()) return false;
        NBTTagCompound tag = s.getTagCompound();
        if (!tag.hasKey("LastUseTime")) return false;
        long last = tag.getLong("LastUseTime");
        return (currentTime - last) < cooldownTicks;
    }

    @ZenMethod
    public static void setCooldown(IItemStack item, long currentTime) {
        ItemStack s = CraftTweakerMC.getItemStack(item);
        if (!s.hasTagCompound()) {
            s.setTagCompound(new NBTTagCompound());
        }
        s.getTagCompound().setLong("LastUseTime", currentTime);
    }

    @ZenMethod
    public static int getRemainingCooldown(IItemStack item, int cooldownTicks, long currentTime) {
        ItemStack s = CraftTweakerMC.getItemStack(item);
        if (!s.hasTagCompound() || !s.getTagCompound().hasKey("LastUseTime")) return 0;
        long last = s.getTagCompound().getLong("LastUseTime");
        long remain = cooldownTicks - (currentTime - last);
        if (remain <= 0) return 0;
        return (int) Math.ceil(remain / 20.0);
    }

    // ===========================
    // 私有辅助：安全落点
    // ===========================
    private static BlockPos findSafeAbove(World world, BlockPos start, int maxUp) {
        BlockPos p = start;
        for (int i = 0; i <= maxUp; i++) {
            if (world.isAirBlock(p) && world.isAirBlock(p.up())) return p;
            p = p.up();
        }
        return null;
    }

    private static BlockPos findGroundBelow(World world, BlockPos start, int maxDown) {
        BlockPos p = start;
        for (int i = 0; i < maxDown && p.getY() > 1; i++) {
            if (!world.isAirBlock(p) && world.isAirBlock(p.up())) {
                return p;
            }
            p = p.down();
        }
        return null;
    }

    // 实用：读取服务端交互距离（失败时按模式估计）
    private static double getServerReachDistance(EntityPlayer p) {
        if (p instanceof EntityPlayerMP) {
            return ((EntityPlayerMP) p).interactionManager.getBlockReachDistance();
        }
        return p.capabilities.isCreativeMode ? 5.0D : 4.5D;
    }
}
