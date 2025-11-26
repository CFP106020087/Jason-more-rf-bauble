package com.moremod.item;

import net.minecraft.entity.Entity;
import com.moremod.dimension.PersonalDimensionManager;
import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.entity.EntityRiftPortal;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.LoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 维度撕裂者 - 按键版本
 * 通过KeyBindHandler中配置的按键操作
 */
public class ItemDimensionalRipper extends Item implements IBauble {

    // ================= 能量参数（Forge Energy） =================
    private static final String NBT_ENERGY = "Energy";
    public  static final int    MAX_ENERGY        = 2_000_000;
    public  static final int    MAX_RECEIVE       = 10_000;
    private static final int    ENERGY_PER_ACTIVATE = 100_000;

    // ================= NBT（传送点/模式/统计） =================
    private static final String NBT_PORTAL_A_POS = "PortalAPos";
    private static final String NBT_PORTAL_A_DIM = "PortalADim";
    private static final String NBT_PORTAL_B_POS = "PortalBPos";
    private static final String NBT_PORTAL_B_DIM = "PortalBDim";
    private static final String NBT_RIFTS_CREATED = "RiftsCreated";
    private static final String NBT_LAST_USE = "LastUseTime";
    private static final String NBT_STATE = "CurrentState"; // 0=设置起点, 1=设置终点, 2=激活

    // 区块加载票据管理
    private static final Map<UUID, List<Ticket>> chunkTickets = new HashMap<>();
    private static final Map<UUID, List<BlockPos>> activePortals = new HashMap<>();

    // Mod ID用于区块加载
    private static final String MOD_ID = "moremod";

    public ItemDimensionalRipper() {
        setRegistryName("dimensional_ripper");
        setTranslationKey("dimensional_ripper");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    /**
     * 初始化区块加载回调（在主Mod类中调用）
     */
    public static void initChunkLoading() {
        ForgeChunkManager.setForcedChunkLoadingCallback(MOD_ID, new ChunkLoadCallback());
    }

    // ================= Baubles =================
    @Override
    public BaubleType getBaubleType(ItemStack stack) { return BaubleType.AMULET; }

    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase wearer) { return wearer instanceof EntityPlayer; }

    @Override
    public void onEquipped(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer) || wearer.world.isRemote) return;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) { nbt = new NBTTagCompound(); stack.setTagCompound(nbt); }
        if (!nbt.hasKey(NBT_STATE)) nbt.setInteger(NBT_STATE, 0);
        if (getEnergy(stack) == 0) setEnergy(stack, Math.min(ENERGY_PER_ACTIVATE, MAX_ENERGY));
        ((EntityPlayer) wearer).sendStatusMessage(new TextComponentString(TextFormatting.DARK_PURPLE + "⟐ 维度撕裂者已就绪 [在按键设置中配置快捷键]"), true);
        wearer.world.playSound(null, wearer.posX, wearer.posY, wearer.posZ,
                SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 0.6F, 0.8F);
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer) || wearer.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) wearer;
        closeAllPortals(player);
        player.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "⟐ 维度撕裂者已停用"), true);
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) wearer;
        if (!player.world.isRemote && player.world.getTotalWorldTime() % 100 == 0) {
            maintainPortals(player, stack);
        }
    }

    // ================= 服务端处理按键操作 =================
    public static void handleKeyPress(EntityPlayerMP player) {
        // 查找玩家装备的维度撕裂者
        ItemStack ripperStack = findEquippedRipper(player);

        if (ripperStack == null) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "⚠ 未装备维度撕裂者"), true);
            return;
        }

        NBTTagCompound nbt = ripperStack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            ripperStack.setTagCompound(nbt);
        }

        int state = nbt.getInteger(NBT_STATE);

        switch (state) {
            case 0: // 设置起点
                setPortalPoint(player, ripperStack, true);
                break;
            case 1: // 设置终点
                setPortalPoint(player, ripperStack, false);
                break;
            case 2: // 激活传送门
                activatePortals(player, ripperStack);
                // 激活后重置到起点状态
                nbt.setInteger(NBT_STATE, 0);
                break;
        }
    }

    /**
     * 处理私人维度按键
     */
    public static void handlePersonalDimensionKey(EntityPlayerMP player) {
        // 查找玩家装备的维度撕裂者
        ItemStack ripperStack = findEquippedRipper(player);

        if (ripperStack == null) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "⚠ 需要装备维度撕裂者才能进入私人维度"), true);
            return;
        }

        // 检查能量
        int energy = getEnergy(ripperStack);
        int requiredEnergy = 50000; // 进入私人维度需要的能量

        if (energy < requiredEnergy) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 能量不足 (" + energy + "/" + requiredEnergy + " FE)"), true);
            return;
        }

        // 如果已在私人维度，返回主世界
        if (player.dimension == PersonalDimensionManager.PERSONAL_DIM_ID) {
            returnFromPersonalDimension(player, ripperStack);
        } else {
            // 进入私人维度
            enterPersonalDimension(player, ripperStack);
        }
    }

    /**
     * 进入私人维度
     */
    private static void enterPersonalDimension(EntityPlayerMP player, ItemStack ripperStack) {
        // 保存当前位置
        NBTTagCompound nbt = ripperStack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            ripperStack.setTagCompound(nbt);
        }

        // 保存返回点
        nbt.setLong("ReturnPos", player.getPosition().toLong());
        nbt.setInteger("ReturnDim", player.dimension);

        // 扣除能量
        setEnergy(ripperStack, getEnergy(ripperStack) - 50000);

        // 生成进入效果
        spawnDimensionEntryEffects(player);

        // 传送到私人维度
        PersonalDimensionManager.teleportToPersonalSpace(player);

        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "═══════════════════════════"
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "   欢迎来到你的私人维度！"
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "   空间大小:  30x15x30 "
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "   再次按键返回原位置"
        ));
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "═══════════════════════════"
        ));
    }

    /**
     * 从私人维度返回
     */
    private static void returnFromPersonalDimension(EntityPlayerMP player, ItemStack ripperStack) {
        NBTTagCompound nbt = ripperStack.getTagCompound();
        if (nbt == null || !nbt.hasKey("ReturnPos")) {
            // 没有返回点，返回世界出生点
            if (player.dimension != 0) {
                player.changeDimension(0, new SimpleTeleporter(player.getServer().getWorld(0)));
            }
            player.setPositionAndUpdate(
                    player.world.getSpawnPoint().getX() + 0.5,
                    player.world.getSpawnPoint().getY(),
                    player.world.getSpawnPoint().getZ() + 0.5
            );
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "返回世界出生点"
            ), true);
            return;
        }

        // 获取返回位置
        BlockPos returnPos = BlockPos.fromLong(nbt.getLong("ReturnPos"));
        int returnDim = nbt.getInteger("ReturnDim");

        // 生成离开效果
        spawnDimensionExitEffects(player);

        // 传送回原位置
        if (returnDim != player.dimension) {
            // 使用简单传送器避免寻找传送门
            WorldServer targetWorld = player.getServer().getWorld(returnDim);
            if (targetWorld != null) {
                player.changeDimension(returnDim, new SimpleTeleporter(targetWorld));
                // 传送后设置精确位置
                player.connection.setPlayerLocation(
                        returnPos.getX() + 0.5,
                        returnPos.getY(),
                        returnPos.getZ() + 0.5,
                        player.rotationYaw,
                        player.rotationPitch
                );
            }
        } else {
            player.setPositionAndUpdate(
                    returnPos.getX() + 0.5,
                    returnPos.getY(),
                    returnPos.getZ() + 0.5
            );
        }

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.GREEN + "✦ 已返回原世界"
        ), true);
    }

    /**
     * 简单传送器 - 不寻找或创建传送门
     */
    private static class SimpleTeleporter extends net.minecraft.world.Teleporter {
        public SimpleTeleporter(WorldServer world) {
            super(world);
        }

        @Override
        public void placeInPortal(Entity entity, float rotationYaw) {
            // 不做任何事，让实体保持原位
        }

        @Override
        public boolean placeInExistingPortal(Entity entity, float rotationYaw) {
            // 不寻找传送门，直接返回true
            return true;
        }

        @Override
        public boolean makePortal(Entity entity) {
            // 不创建传送门
            return true;
        }

        @Override
        public void removeStalePortalLocations(long worldTime) {
            // 不处理传送门
        }
    }

    /**
     * 生成进入维度的特效
     */
    private static void spawnDimensionEntryEffects(EntityPlayerMP player) {
        WorldServer world = player.getServerWorld();
        double x = player.posX;
        double y = player.posY;
        double z = player.posZ;

        // 创建传送门效果
        for (int i = 0; i < 100; i++) {
            double angle = (Math.PI * 2) * i / 100;
            double radius = i * 0.05;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            double py = y + i * 0.03;

        }

        // 大型爆炸效果


        // 音效
        world.playSound(null, x, y, z,
                SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 2.0F, 0.5F);
        world.playSound(null, x, y, z,
                SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * 生成离开维度的特效
     */
    private static void spawnDimensionExitEffects(EntityPlayerMP player) {
        WorldServer world = player.getServerWorld();
        double x = player.posX;
        double y = player.posY;
        double z = player.posZ;

        // 收缩的粒子效果
        for (int i = 0; i < 50; i++) {
            double angle = (Math.PI * 2) * i / 50;
            double radius = 3.0 - (i * 0.06);
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;

            world.spawnParticle(EnumParticleTypes.END_ROD, false,
                    px, y + 1, pz, 1, 0, 0.1, 0, 0.05);
        }

        // 音效
        world.playSound(null, x, y, z,
                SoundEvents.ENTITY_SHULKER_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    // 查找装备的维度撕裂者
    public static ItemStack findEquippedRipper(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof ItemDimensionalRipper) {
                        return stack;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    // 设置传送点（起点或终点）
    private static void setPortalPoint(EntityPlayer player, ItemStack stack, boolean isPointA) {
        World world = player.world;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        // 获取玩家看向的方块
        RayTraceResult ray = player.rayTrace(5.0D, 1.0F);
        BlockPos target;

        if (ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK) {
            // 如果看向方块，使用方块上方位置
            target = ray.getBlockPos().offset(ray.sideHit);
        } else {
            // 如果看向空气，使用玩家当前位置
            target = player.getPosition();
        }

        // 生成大量垂直粒子柱效果来标记位置
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            double cx = target.getX() + 0.5D;
            double cy = target.getY();
            double cz = target.getZ() + 0.5D;

            // 创建垂直粒子柱（从地面到高空）
            for (int i = 0; i < 15; i++) {
                double yOffset = cy + (i * 0.5D);
                // 主粒子柱 - 使用不同颜色区分起点终点
                if (isPointA) {
                    // 起点 - 蓝色系粒子
                    ws.spawnParticle(EnumParticleTypes.WATER_SPLASH, false,
                            cx, yOffset, cz, 25, 0.1D, 0.1D, 0.1D, 0.0D);
                    ws.spawnParticle(EnumParticleTypes.WATER_DROP, false,
                            cx, yOffset, cz, 15, 0.15D, 0.1D, 0.15D, 0.0D);
                } else {
                    // 终点 - 绿色/末影系粒子
                    ws.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, false,
                            cx, yOffset, cz, 20, 0.1D, 0.1D, 0.1D, 0.0D);
                    ws.spawnParticle(EnumParticleTypes.PORTAL, false,
                            cx, yOffset, cz, 15, 0.15D, 0.1D, 0.15D, 0.02D);
                }

                // 通用装饰粒子
                ws.spawnParticle(EnumParticleTypes.END_ROD, false,
                        cx, yOffset, cz, 8, 0.2D, 0.05D, 0.2D, 0.01D);
            }

            // 在底部和顶部添加额外的爆发效果
            // 底部环形爆发
            for (int angle = 0; angle < 360; angle += 30) {
                double rad = Math.toRadians(angle);
                double xOff = Math.cos(rad) * 0.8;
                double zOff = Math.sin(rad) * 0.8;
                ws.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, false,
                        cx + xOff, cy + 0.1D, cz + zOff, 3, 0.05D, 0.1D, 0.05D, 0.05D);
            }

            // 顶部光环
            ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, false,
                    cx, cy + 7.5D, cz, 50, 0.5D, 0.5D, 0.5D, 0.02D);

            // 音效反馈
            world.playSound(null, cx, cy, cz,
                    isPointA ? SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE : SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.PLAYERS, 0.8F, isPointA ? 1.0F : 1.2F);
        }

        if (isPointA) {
            // 设置起点
            nbt.setLong(NBT_PORTAL_A_POS, target.toLong());
            nbt.setInteger(NBT_PORTAL_A_DIM, world.provider.getDimension());
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.BLUE + "⟐ 起点已设置: " + TextFormatting.WHITE + formatLocation(target, world.provider.getDimension())
                            + TextFormatting.GRAY + " [再按键设置终点]"), true);
            nbt.setInteger(NBT_STATE, 1); // 切换到设置终点状态
        } else {
            // 设置终点
            nbt.setLong(NBT_PORTAL_B_POS, target.toLong());
            nbt.setInteger(NBT_PORTAL_B_DIM, world.provider.getDimension());
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "⟐ 终点已设置: " + TextFormatting.WHITE + formatLocation(target, world.provider.getDimension())
                            + TextFormatting.GRAY + " [再按键激活传送门]"), true);
            nbt.setInteger(NBT_STATE, 2); // 切换到激活状态
        }
    }

    // ================= 激活传送门 =================
    private static void activatePortals(EntityPlayer player, ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return;

        if (!nbt.hasKey(NBT_PORTAL_A_POS) || !nbt.hasKey(NBT_PORTAL_B_POS)) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "⚠ 请先设置起点和终点"), true);
            return;
        }

        int energy = getEnergy(stack);
        if (energy < ENERGY_PER_ACTIVATE) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 能量不足 (" + energy + "/" + ENERGY_PER_ACTIVATE + " FE)"), true);
            return;
        }

        BlockPos portalAPos = BlockPos.fromLong(nbt.getLong(NBT_PORTAL_A_POS));
        int portalADim = nbt.getInteger(NBT_PORTAL_A_DIM);
        BlockPos portalBPos = BlockPos.fromLong(nbt.getLong(NBT_PORTAL_B_POS));
        int portalBDim = nbt.getInteger(NBT_PORTAL_B_DIM);

        // 同维度距离限制
        if (portalADim == portalBDim && portalAPos.distanceSq(portalBPos) < 100) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "⚠ 传送点距离过近（需≥10格）"), true);
            return;
        }

        // 在创建传送门前，先在两个位置生成大量垂直粒子标记
        spawnActivationParticles(player, portalAPos, portalADim, true);
        spawnActivationParticles(player, portalBPos, portalBDim, false);

        if (createRiftPortal(portalAPos, portalADim, portalBPos, portalBDim, player)) {
            setEnergy(stack, energy - ENERGY_PER_ACTIVATE);
            nbt.setInteger(NBT_RIFTS_CREATED, nbt.getInteger(NBT_RIFTS_CREATED) + 1);
            nbt.setLong(NBT_LAST_USE, player.world.getTotalWorldTime());

            List<BlockPos> portals = activePortals.computeIfAbsent(player.getUniqueID(), k -> new ArrayList<>());
            portals.add(portalAPos);
            portals.add(portalBPos);

            player.sendMessage(new TextComponentString(TextFormatting.LIGHT_PURPLE + "⟐ 维度裂隙已开启（持续约5分钟）"));
            player.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "[按键重新设置起点]"), true);
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1.0F, 1.0F);
        } else {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "⚠ 传送门创建失败"), true);
        }
    }

    // 生成激活时的粒子效果
    private static void spawnActivationParticles(EntityPlayer player, BlockPos pos, int dimension, boolean isPortalA) {
        if (player.getServer() == null) return;
        WorldServer world = player.getServer().getWorld(dimension);
        if (world == null) return;

        double cx = pos.getX() + 0.5D;
        double cy = pos.getY();
        double cz = pos.getZ() + 0.5D;

        // 创建巨大的垂直粒子柱（激活效果）
        for (int i = 0; i < 30; i++) {
            double yOffset = cy + (i * 0.4D);

            // 螺旋上升效果
            double angle = (i * 30) % 360;
            double rad = Math.toRadians(angle);
            double spiralX = Math.cos(rad) * 0.3;
            double spiralZ = Math.sin(rad) * 0.3;

            // 主粒子柱 - 紫色末影粒子
            world.spawnParticle(EnumParticleTypes.PORTAL, false,
                    cx + spiralX, yOffset, cz + spiralZ, 30, 0.2D, 0.1D, 0.2D, 0.1D);

            // 能量粒子
            world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, false,
                    cx, yOffset, cz, 20, 0.5D, 0.1D, 0.5D, 1.0D);

            // 闪光粒子
            if (i % 3 == 0) {
                world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, false,
                        cx, yOffset, cz, 15, 0.3D, 0.1D, 0.3D, 0.2D);
            }
        }

        // 底部冲击波效果
        for (int ring = 0; ring < 3; ring++) {
            double radius = (ring + 1) * 1.5;
            for (int angle = 0; angle < 360; angle += 15) {
                double rad = Math.toRadians(angle);
                double xOff = Math.cos(rad) * radius;
                double zOff = Math.sin(rad) * radius;

                world.spawnParticle(EnumParticleTypes.SPELL_WITCH, false,
                        cx + xOff, cy + 0.1D, cz + zOff, 2, 0.0D, 0.1D, 0.0D, 0.05D);

                if (ring == 0) {
                    world.spawnParticle(EnumParticleTypes.DRAGON_BREATH, false,
                            cx + xOff * 0.5, cy + 0.2D, cz + zOff * 0.5, 1, 0.0D, 0.05D, 0.0D, 0.01D);
                }
            }
        }

        // 顶部爆炸效果
        world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, false,
                cx, cy + 12.0D, cz, 1, 0.0D, 0.0D, 0.0D, 0.0D);

        // 额外的装饰粒子雨
        world.spawnParticle(EnumParticleTypes.END_ROD, false,
                cx, cy + 10.0D, cz, 100, 2.0D, 2.0D, 2.0D, 0.05D);

        // 标记颜色（区分起点终点）
        if (isPortalA) {
            // 起点 - 额外的蓝色粒子
            world.spawnParticle(EnumParticleTypes.WATER_WAKE, false,
                    cx, cy + 5.0D, cz, 50, 1.0D, 3.0D, 1.0D, 0.0D);
        } else {
            // 终点 - 额外的绿色粒子
            world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, false,
                    cx, cy + 5.0D, cz, 50, 1.0D, 3.0D, 1.0D, 0.0D);
        }

        // 播放音效
        world.playSound(null, cx, cy, cz,
                SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.PLAYERS, 0.5F, 1.2F);
    }

    // ================= 区块加载系统 =================
    private static boolean createRiftPortal(BlockPos posA, int dimA, BlockPos posB, int dimB, EntityPlayer player) {
        if (player.getServer() == null) return false;
        WorldServer worldA = player.getServer().getWorld(dimA);
        WorldServer worldB = player.getServer().getWorld(dimB);
        if (worldA == null || worldB == null) return false;

        // 强制加载传送门所在的区块
        boolean chunkLoadSuccess = forceLoadPortalChunks(worldA, posA, worldB, posB, player);
        if (!chunkLoadSuccess) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.YELLOW + "⚠ 区块加载失败，传送门可能不稳定"), true);
        }

        EntityRiftPortal a = new EntityRiftPortal(worldA, posA, posB, dimB, player.getUniqueID());
        EntityRiftPortal b = new EntityRiftPortal(worldB, posB, posA, dimA, player.getUniqueID());

        boolean okA = worldA.spawnEntity(a);
        boolean okB = worldB.spawnEntity(b);
        return okA && okB;
    }

    // 强制加载传送门区块
    private static boolean forceLoadPortalChunks(WorldServer worldA, BlockPos posA, WorldServer worldB, BlockPos posB, EntityPlayer player) {
        try {
            // 为起点创建区块加载票据
            Ticket ticketA = ForgeChunkManager.requestTicket(MOD_ID, worldA, Type.NORMAL);
            if (ticketA != null) {
                ticketA.getModData().setString("type", "dimensional_ripper");
                ticketA.getModData().setLong("pos", posA.toLong());
                ticketA.getModData().setUniqueId("player", player.getUniqueID());
                ticketA.getModData().setLong("timestamp", System.currentTimeMillis());

                // 加载3x3区块范围（中心及周围）
                int chunkX = posA.getX() >> 4;
                int chunkZ = posA.getZ() >> 4;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        ForgeChunkManager.forceChunk(ticketA, new ChunkPos(chunkX + dx, chunkZ + dz));
                    }
                }

                // 保存票据
                List<Ticket> tickets = chunkTickets.computeIfAbsent(player.getUniqueID(), k -> new ArrayList<>());
                tickets.add(ticketA);
            }

            // 为终点创建区块加载票据
            Ticket ticketB = ForgeChunkManager.requestTicket(MOD_ID, worldB, Type.NORMAL);
            if (ticketB != null) {
                ticketB.getModData().setString("type", "dimensional_ripper");
                ticketB.getModData().setLong("pos", posB.toLong());
                ticketB.getModData().setUniqueId("player", player.getUniqueID());
                ticketB.getModData().setLong("timestamp", System.currentTimeMillis());

                // 加载3x3区块范围
                int chunkX = posB.getX() >> 4;
                int chunkZ = posB.getZ() >> 4;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        ForgeChunkManager.forceChunk(ticketB, new ChunkPos(chunkX + dx, chunkZ + dz));
                    }
                }

                List<Ticket> tickets = chunkTickets.computeIfAbsent(player.getUniqueID(), k -> new ArrayList<>());
                tickets.add(ticketB);
            }

            return ticketA != null && ticketB != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 区块加载回调类
    public static class ChunkLoadCallback implements LoadingCallback {
        @Override
        public void ticketsLoaded(List<Ticket> tickets, World world) {
            for (Ticket ticket : tickets) {
                NBTTagCompound data = ticket.getModData();

                // 检查是否是维度撕裂者的票据
                if ("dimensional_ripper".equals(data.getString("type"))) {
                    // 检查是否过期（5分钟）
                    long timestamp = data.getLong("timestamp");
                    if (System.currentTimeMillis() - timestamp > 5 * 60 * 1000) {
                        // 过期了，释放票据
                        ForgeChunkManager.releaseTicket(ticket);
                        continue;
                    }

                    if (data.hasKey("pos")) {
                        BlockPos pos = BlockPos.fromLong(data.getLong("pos"));

                        // 重新加载3x3区块
                        int chunkX = pos.getX() >> 4;
                        int chunkZ = pos.getZ() >> 4;

                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                ForgeChunkManager.forceChunk(ticket,
                                        new ChunkPos(chunkX + dx, chunkZ + dz));
                            }
                        }

                        System.out.println("[维度撕裂者] 恢复区块加载: " + pos + " in dim " + world.provider.getDimension());
                    } else {
                        // 无效票据，释放它
                        ForgeChunkManager.releaseTicket(ticket);
                    }
                }
            }
        }
    }

    private void maintainPortals(EntityPlayer player, ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || !nbt.hasKey(NBT_LAST_USE)) return;
        long dt = player.world.getTotalWorldTime() - nbt.getLong(NBT_LAST_USE);

        // 5分钟后释放区块加载并清理记录
        if (dt > 6000) {
            releaseChunkTickets(player);
            activePortals.remove(player.getUniqueID());
        }
    }

    private void closeAllPortals(EntityPlayer player) {
        releaseChunkTickets(player);
        activePortals.remove(player.getUniqueID());
    }

    // 释放区块加载票据
    private static void releaseChunkTickets(EntityPlayer player) {
        List<Ticket> tickets = chunkTickets.remove(player.getUniqueID());
        if (tickets != null) {
            for (Ticket ticket : tickets) {
                ForgeChunkManager.releaseTicket(ticket);
            }
            if (!player.world.isRemote) {
                player.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "⟐ 区块加载已释放"), true);
            }
        }
    }

    // ================= Tooltip =================
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tip, ITooltipFlag flag) {
        tip.add("");
        tip.add(TextFormatting.DARK_PURPLE + "═══ 维度撕裂者 ═══");
        tip.add(TextFormatting.GRAY + "饰品类型: " + TextFormatting.WHITE + "护符");
        tip.add("");

        int energy = getEnergy(stack);
        double pct = (double) energy / MAX_ENERGY * 100.0;
        String col = pct > 75 ? TextFormatting.GREEN.toString() :
                pct > 50 ? TextFormatting.YELLOW.toString() :
                        pct > 25 ? TextFormatting.GOLD.toString() : TextFormatting.RED.toString();
        tip.add(col + "能量: " + String.format(Locale.ROOT, "%,d", energy) + " / " + String.format(Locale.ROOT, "%,d", MAX_ENERGY) + " FE"
                + TextFormatting.GRAY + " (" + String.format(Locale.ROOT, "%.0f%%", pct) + ")");

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null) {
            int state = nbt.getInteger(NBT_STATE);
            String stateText = state == 0 ? "等待设置起点" : state == 1 ? "等待设置终点" : "准备激活";
            TextFormatting stateColor = state == 0 ? TextFormatting.BLUE : state == 1 ? TextFormatting.GREEN : TextFormatting.LIGHT_PURPLE;
            tip.add(TextFormatting.GRAY + "当前状态: " + stateColor + stateText);

            if (nbt.hasKey(NBT_PORTAL_A_POS)) {
                BlockPos pa = BlockPos.fromLong(nbt.getLong(NBT_PORTAL_A_POS));
                int dimA = nbt.getInteger(NBT_PORTAL_A_DIM);
                tip.add(TextFormatting.BLUE + "起点: " + TextFormatting.GRAY + formatLocation(pa, dimA));
            }

            if (nbt.hasKey(NBT_PORTAL_B_POS)) {
                BlockPos pb = BlockPos.fromLong(nbt.getLong(NBT_PORTAL_B_POS));
                int dimB = nbt.getInteger(NBT_PORTAL_B_DIM);
                tip.add(TextFormatting.GREEN + "终点: " + TextFormatting.GRAY + formatLocation(pb, dimB));
            }

            int rifts = nbt.getInteger(NBT_RIFTS_CREATED);
            if (rifts > 0) tip.add(TextFormatting.GOLD + "已创建裂隙: " + rifts);
        }

        tip.add("");
        tip.add(TextFormatting.YELLOW + "操作说明:");
        tip.add(TextFormatting.GRAY + "• 传送门按键：设置起点 → 终点 → 激活");
        tip.add(TextFormatting.GRAY + "• 私人维度按键：进入/离开私人空间");
        tip.add(TextFormatting.GRAY + "• 按键可在游戏设置中配置");
        tip.add(TextFormatting.GRAY + "• 看向方块按键会在方块上设置传送点");
        tip.add(TextFormatting.GRAY + "• 看向空气按键会在当前位置设置传送点");

        if (GuiScreen.isShiftKeyDown()) {
            tip.add("");
            tip.add(TextFormatting.GOLD + "详细信息:");
            tip.add(TextFormatting.GRAY + "• 激活传送门: " + ENERGY_PER_ACTIVATE + " FE");
            tip.add(TextFormatting.GRAY + "• 进入私人维度: 50,000 FE");
            tip.add(TextFormatting.GRAY + "• 传送门持续约5分钟");
            tip.add(TextFormatting.GRAY + "• 支持跨维度传送");
            tip.add(TextFormatting.GRAY + "• 自动加载3x3区块范围");
            tip.add(TextFormatting.GRAY + "• 私人维度256x256x256独立空间");
        }
    }

    @Override public boolean hasEffect(ItemStack stack) { return true; }

    @Override public boolean showDurabilityBar(ItemStack stack){ return true; }
    @Override public double getDurabilityForDisplay(ItemStack stack){
        double cap = MAX_ENERGY, cur = Math.max(0, Math.min(cap, getEnergy(stack)));
        return 1.0D - (cur / cap);
    }
    @Override public int getRGBDurabilityForDisplay(ItemStack stack){ return 0x2EE6FF; }

    // ================= Forge Energy Capability =================
    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new EnergyCapProvider(stack);
    }

    private static class EnergyCapProvider implements ICapabilityProvider, IEnergyStorage {
        private final ItemStack stack;
        EnergyCapProvider(ItemStack s){ this.stack = s; }
        @Override public boolean hasCapability(Capability<?> cap, EnumFacing f){ return cap == CapabilityEnergy.ENERGY; }
        @SuppressWarnings("unchecked")
        @Override public <T> T getCapability(Capability<T> cap, EnumFacing f){
            return cap == CapabilityEnergy.ENERGY ? (T) this : null;
        }
        @Override public int receiveEnergy(int maxReceive, boolean simulate){
            int energy = getEnergy(stack);
            int recv = Math.min(MAX_RECEIVE, Math.min(maxReceive, MAX_ENERGY - energy));
            if (!simulate && recv > 0) setEnergy(stack, energy + recv);
            return recv;
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate){ return 0; }
        @Override public int getEnergyStored(){ return getEnergy(stack); }
        @Override public int getMaxEnergyStored(){ return MAX_ENERGY; }
        @Override public boolean canExtract(){ return false; }
        @Override public boolean canReceive(){ return true; }
    }

    // ================= 工具方法 =================
    private static boolean isWorn(EntityPlayer p, Item selfItem){
        try{
            IBaublesItemHandler h = BaublesApi.getBaublesHandler(p);
            if (h == null) return false;
            for (int i=0;i<h.getSlots();i++){
                ItemStack s = h.getStackInSlot(i);
                if (!s.isEmpty() && s.getItem() == selfItem) return true;
            }
        }catch (Throwable ignored){}
        return false;
    }

    private static int getEnergy(ItemStack s){ return s.hasTagCompound()? s.getTagCompound().getInteger(NBT_ENERGY) : 0; }
    private static void setEnergy(ItemStack s, int v){
        NBTTagCompound n = s.getTagCompound();
        if (n==null){ n=new NBTTagCompound(); s.setTagCompound(n); }
        n.setInteger(NBT_ENERGY, Math.max(0, Math.min(MAX_ENERGY, v)));
    }

    private static String formatLocation(BlockPos pos, int dimension) {
        String dimName = dimension == 0 ? "主世界" : dimension == -1 ? "下界" : dimension == 1 ? "末地" : "维度" + dimension;
        return String.format(Locale.ROOT, "%s (%d, %d, %d)", dimName, pos.getX(), pos.getY(), pos.getZ());
    }
}