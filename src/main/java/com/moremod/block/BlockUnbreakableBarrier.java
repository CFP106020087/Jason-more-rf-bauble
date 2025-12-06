package com.moremod.block;

import com.moremod.moremod;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public class BlockUnbreakableBarrier extends Block {

    public enum BarrierType {
        VOID_CRYSTAL("虚空水晶", 0x4A0080, true),
        QUANTUM_FIELD("量子力场", 0x00FFFF, false),
        TEMPORAL_LOCK("时间锁定", 0xFFD700, false),
        DIMENSIONAL_ANCHOR("维度锚定", 0xFF00FF, false),
        ETHEREAL_WALL("以太墙壁", 0x87CEEB, false);

        public final String name;
        public final int color;
        public final boolean canTeleport;

        BarrierType(String name, int color, boolean canTeleport) {
            this.name = name;
            this.color = color;
            this.canTeleport = canTeleport;
        }
    }

    private final BarrierType type;

    private static final Material UNBREAKABLE_MATERIAL = new Material(MapColor.PURPLE) {
        @Override
        public boolean isToolNotRequired() {
            return false;
        }

        @Override
        public boolean isReplaceable() {
            return false;
        }
    };

    public BlockUnbreakableBarrier(BarrierType type) {
        super(UNBREAKABLE_MATERIAL);
        this.type = type;

        String path = "unbreakable_barrier_" + type.name().toLowerCase(java.util.Locale.ROOT);
        setRegistryName(moremod.MODID, path);
        setTranslationKey(moremod.MODID + "." + path);
        setCreativeTab(moremodCreativeTab.moremod_TAB);

        setBlockUnbreakable();
        setResistance(Float.MAX_VALUE);
        setSoundType(SoundType.GLASS);
        setLightLevel(type == BarrierType.VOID_CRYSTAL ? 0.8F : 0.5F);
        setTickRandomly(true);
    }

    public BarrierType getType() {
        return type;
    }

    // 不可破坏
    @Override
    public float getBlockHardness(IBlockState state, World world, BlockPos pos) {
        return -1.0F;
    }

    @Override
    public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World world, BlockPos pos) {
        return 0.0F;
    }

    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return false;
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, net.minecraft.world.Explosion explosion) {
        if (!world.isRemote) {
            spawnProtectionParticles(world, pos);
            world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                    SoundCategory.BLOCKS, 1.0F, 2.0F);
        }
    }

    @Override
    public boolean canDropFromExplosion(net.minecraft.world.Explosion explosion) {
        return false;
    }

    // 交互
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float x, float y, float z) {

        if (!world.isRemote) {
            if (type == BarrierType.VOID_CRYSTAL) {
                // 虚空水晶需要维度钥匙来激活
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "【虚空水晶】" +
                                TextFormatting.AQUA + " 需要维度钥匙来激活传送门"
                ), true);

                // 特殊粒子效果
                spawnCrystalParticles(world, pos);
                world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                        SoundCategory.BLOCKS, 0.5F, 1.5F);
                return true; // 虚空水晶消费交互
            }
            // 其他屏障类型（包括QUANTUM祭坛）不消费右键事件
            // 这样可以让事件传递到BossAltarInteractionHandler处理Boss召唤
        }
        return false; // 不消费事件，让其他处理器可以处理
    }

    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
        if (!world.isRemote) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 无法破坏 " + type.name + "！"
            ), true);

            spawnDenialParticles(world, pos, player.getHorizontalFacing());
            world.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR,
                    SoundCategory.BLOCKS, 1.0F, 0.5F);
        }
    }

    // 渲染

    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        IBlockState neighbor = world.getBlockState(pos.offset(side));
        return neighbor.getBlock() != this;
    }

    @Override
    public int getLightOpacity(IBlockState state) {
        return 1;
    }

    // 粒子效果
    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!world.isRemote && random.nextInt(3) == 0) {
            spawnAmbientParticles(world, pos);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        if (type == BarrierType.VOID_CRYSTAL) {
            // 虚空水晶特殊效果
            if (random.nextInt(5) == 0) {
                double x = pos.getX() + 0.5;
                double y = pos.getY() + random.nextDouble();
                double z = pos.getZ() + 0.5;

                world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z,
                        (random.nextDouble() - 0.5) * 0.1,
                        random.nextDouble() * 0.1,
                        (random.nextDouble() - 0.5) * 0.1);

                if (random.nextInt(10) == 0) {
                    world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                            x, y + 1.5, z,
                            (random.nextDouble() - 0.5) * 2.0,
                            random.nextDouble(),
                            (random.nextDouble() - 0.5) * 2.0);
                }
            }
        } else if (random.nextInt(10) == 0) {
            // 其他屏障的普通效果
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, 0, 0, 0);
        }
    }

    private void spawnProtectionParticles(World world, BlockPos pos) {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        for (int i = 0; i < 20; i++) {
            double angle = (Math.PI * 2) * i / 20;
            double x = pos.getX() + 0.5 + Math.cos(angle) * 1.5;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * 1.5;
            ws.spawnParticle(EnumParticleTypes.SPELL_WITCH, false,
                    x, pos.getY() + 0.5, z, 1, 0, 0.5, 0, 0.1);
        }
    }

    private void spawnDenialParticles(World world, BlockPos pos, EnumFacing facing) {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        double x = pos.getX() + 0.5 + facing.getXOffset() * 0.6;
        double y = pos.getY() + 0.5 + facing.getYOffset() * 0.6;
        double z = pos.getZ() + 0.5 + facing.getZOffset() * 0.6;
        ws.spawnParticle(EnumParticleTypes.REDSTONE, false,
                x, y, z, 10, 0.2, 0.2, 0.2, 0);
    }

    private void spawnCrystalParticles(World world, BlockPos pos) {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        // 螺旋上升效果
        for (int i = 0; i < 10; i++) {
            double angle = (Math.PI * 2) * i / 10;
            double xOffset = Math.cos(angle) * 0.5;
            double zOffset = Math.sin(angle) * 0.5;

            ws.spawnParticle(EnumParticleTypes.PORTAL, false,
                    x + xOffset, y + i * 0.2, z + zOffset,
                    1, 0, 0, 0, 0.05);
        }
    }

    private void spawnAmbientParticles(World world, BlockPos pos) {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        switch (type) {
            case VOID_CRYSTAL:
                ws.spawnParticle(EnumParticleTypes.PORTAL, false,
                        x, y, z, 5, 0.5, 0.5, 0.5, 0.01);
                break;
            case QUANTUM_FIELD:
                ws.spawnParticle(EnumParticleTypes.END_ROD, false,
                        x, y, z, 3, 0.3, 0.3, 0.3, 0.02);
                break;
            case TEMPORAL_LOCK:
                ws.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, false,
                        x, y + 1, z, 5, 1, 0, 1, 0.5);
                break;
            case DIMENSIONAL_ANCHOR:
                ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH, false,
                        x, y, z, 3, 0.2, 0.2, 0.2, 0.01);
                break;
            case ETHEREAL_WALL:
                ws.spawnParticle(EnumParticleTypes.CLOUD, false,
                        x, y, z, 2, 0.1, 0.1, 0.1, 0);
                break;
        }
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        // 只能在维度100放置
        if (world.provider.getDimension() != 100) {
            return false;
        }
        return super.canPlaceBlockAt(world, pos);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        // 不掉落
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return ItemStack.EMPTY;
    }
}