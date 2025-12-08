package com.moremod.structure;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存储捕获的结构数据
 */
public class StructureData {

    /**
     * 单个方块的信息
     */
    public static class BlockInfo {
        public final BlockPos relativePos;  // 相对于结构原点的位置
        public final IBlockState state;
        public final NBTTagCompound tileNBT; // TileEntity 数据，可为 null

        public BlockInfo(BlockPos pos, IBlockState state, NBTTagCompound tileNBT) {
            this.relativePos = pos;
            this.state = state;
            this.tileNBT = tileNBT;
        }
    }

    private final List<BlockInfo> blocks = new ArrayList<>();
    private BlockPos size = BlockPos.ORIGIN;

    // ============== 捕获逻辑 ==============

    /**
     * 从世界中捕获结构
     * @param world 世界
     * @param center 中心点（落点位置）
     * @param radius 半径 (实际范围 = 2*radius + 1)
     * @param excludedBlocks 排除的方块列表（不可破坏的）
     * @return 被捕获并移除的方块位置列表
     */
    public List<BlockPos> captureFromWorld(World world, BlockPos center, int radius, 
                                           List<Block> excludedBlocks) {
        blocks.clear();
        List<BlockPos> capturedPositions = new ArrayList<>();

        BlockPos minPos = center.add(-radius, 0, -radius);
        BlockPos maxPos = center.add(radius, radius * 2, radius);

        int sizeX = 0, sizeY = 0, sizeZ = 0;

        for (BlockPos.MutableBlockPos pos : BlockPos.getAllInBoxMutable(minPos, maxPos)) {
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            // 跳过空气和排除的方块
            if (block == Blocks.AIR) {
                continue;
            }
            if (excludedBlocks != null && excludedBlocks.contains(block)) {
                continue;
            }
            // 跳过不可破坏的方块（硬度 < 0）
            if (state.getBlockHardness(world, pos) < 0) {
                continue;
            }

            // 计算相对位置
            BlockPos relPos = pos.subtract(minPos);

            // 获取 TileEntity 数据
            NBTTagCompound tileNBT = null;
            TileEntity te = world.getTileEntity(pos);
            if (te != null) {
                tileNBT = te.writeToNBT(new NBTTagCompound());
                // 移除绝对坐标，之后部署时重新设置
                tileNBT.removeTag("x");
                tileNBT.removeTag("y");
                tileNBT.removeTag("z");
            }

            blocks.add(new BlockInfo(relPos, state, tileNBT));
            capturedPositions.add(pos.toImmutable());

            // 更新尺寸
            if (relPos.getX() > sizeX) sizeX = relPos.getX();
            if (relPos.getY() > sizeY) sizeY = relPos.getY();
            if (relPos.getZ() > sizeZ) sizeZ = relPos.getZ();
        }

        this.size = new BlockPos(sizeX + 1, sizeY + 1, sizeZ + 1);
        return capturedPositions;
    }

    /**
     * 移除世界中已捕获的方块
     */
    public static void removeBlocksFromWorld(World world, List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            // 先移除 TileEntity 避免掉落物
            TileEntity te = world.getTileEntity(pos);
            if (te != null) {
                world.removeTileEntity(pos);
            }
            // 设置为空气，不触发更新和掉落
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        }
        // 统一触发更新
        for (BlockPos pos : positions) {
            world.notifyNeighborsOfStateChange(pos, Blocks.AIR, false);
        }
    }

    // ============== 部署逻辑 ==============

    /**
     * 将结构部署到世界
     * @param world 世界
     * @param deployPos 部署位置（结构的最小角）
     * @param overridableBlocks 可被覆盖的方块列表
     * @return 是否成功部署
     */
    public boolean deployToWorld(World world, BlockPos deployPos, List<Block> overridableBlocks) {
        if (blocks.isEmpty()) {
            return false;
        }

        // 第一遍：检查是否可以部署
        Map<BlockPos, IBlockState> existingBlocks = new HashMap<>();
        for (BlockInfo info : blocks) {
            BlockPos worldPos = deployPos.add(info.relativePos);
            IBlockState existing = world.getBlockState(worldPos);

            if (existing.getBlock() != Blocks.AIR) {
                // 检查是否可覆盖
                if (overridableBlocks == null || !overridableBlocks.contains(existing.getBlock())) {
                    // 检查硬度，不可破坏的方块不能被覆盖
                    if (existing.getBlockHardness(world, worldPos) < 0) {
                        return false; // 部署失败，有不可覆盖的方块
                    }
                }
                existingBlocks.put(worldPos, existing);
            }
        }

        // 第二遍：实际部署
        for (BlockInfo info : blocks) {
            BlockPos worldPos = deployPos.add(info.relativePos);

            // 先清除旧的 TileEntity
            TileEntity oldTE = world.getTileEntity(worldPos);
            if (oldTE != null) {
                world.removeTileEntity(worldPos);
            }

            // 设置方块
            world.setBlockState(worldPos, info.state, 2);

            // 恢复 TileEntity
            if (info.tileNBT != null) {
                TileEntity newTE = world.getTileEntity(worldPos);
                if (newTE != null) {
                    NBTTagCompound nbt = info.tileNBT.copy();
                    nbt.setInteger("x", worldPos.getX());
                    nbt.setInteger("y", worldPos.getY());
                    nbt.setInteger("z", worldPos.getZ());
                    newTE.readFromNBT(nbt);
                    newTE.markDirty();
                }
            }
        }

        // 第三遍：通知更新
        for (BlockInfo info : blocks) {
            BlockPos worldPos = deployPos.add(info.relativePos);
            world.notifyNeighborsOfStateChange(worldPos, info.state.getBlock(), false);
        }

        return true;
    }

    // ============== NBT 序列化 ==============

    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList blockList = new NBTTagList();

        for (BlockInfo info : blocks) {
            NBTTagCompound blockNBT = new NBTTagCompound();

            // 保存相对位置
            blockNBT.setInteger("x", info.relativePos.getX());
            blockNBT.setInteger("y", info.relativePos.getY());
            blockNBT.setInteger("z", info.relativePos.getZ());

            // 保存方块状态
            NBTTagCompound stateNBT = new NBTTagCompound();
            NBTUtil.writeBlockState(stateNBT, info.state);
            blockNBT.setTag("state", stateNBT);

            // 保存 TileEntity 数据
            if (info.tileNBT != null) {
                blockNBT.setTag("tile", info.tileNBT);
            }

            blockList.appendTag(blockNBT);
        }

        nbt.setTag("blocks", blockList);
        nbt.setInteger("sizeX", size.getX());
        nbt.setInteger("sizeY", size.getY());
        nbt.setInteger("sizeZ", size.getZ());
    }

    public void readFromNBT(NBTTagCompound nbt) {
        blocks.clear();

        NBTTagList blockList = nbt.getTagList("blocks", Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < blockList.tagCount(); i++) {
            NBTTagCompound blockNBT = blockList.getCompoundTagAt(i);

            BlockPos pos = new BlockPos(
                blockNBT.getInteger("x"),
                blockNBT.getInteger("y"),
                blockNBT.getInteger("z")
            );

            IBlockState state = NBTUtil.readBlockState(blockNBT.getCompoundTag("state"));

            NBTTagCompound tileNBT = null;
            if (blockNBT.hasKey("tile")) {
                tileNBT = blockNBT.getCompoundTag("tile");
            }

            blocks.add(new BlockInfo(pos, state, tileNBT));
        }

        this.size = new BlockPos(
            nbt.getInteger("sizeX"),
            nbt.getInteger("sizeY"),
            nbt.getInteger("sizeZ")
        );
    }

    // ============== 工具方法 ==============

    public int getBlockCount() {
        return blocks.size();
    }

    public BlockPos getSize() {
        return size;
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    public List<BlockInfo> getBlocks() {
        return blocks;
    }
}