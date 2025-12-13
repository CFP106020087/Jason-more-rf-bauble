package com.moremod.schematic;

// 移除了CubicChunks相关导入
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;
import java.util.Map.Entry;

/**
 * 独立的Schematic类 - 不依赖CubicChunks
 * 基于DimDoors的实现，移除了CubicChunks相关代码
 * 支持 1.13+ Sponge Schematic 格式自动转换到 1.12.2
 */
public class Schematic {

    public int version = 1;

    // 1.13+ 方块名称到 1.12.2 的映射
    private static final Map<String, String> BLOCK_NAME_MAPPING = new HashMap<>();
    // 需要移除的 1.13+ 属性
    private static final Set<String> PROPERTIES_TO_REMOVE = new HashSet<>();

    static {
        // 1.13 扁平化后的方块名称映射
        BLOCK_NAME_MAPPING.put("minecraft:smooth_stone", "minecraft:stone");
        BLOCK_NAME_MAPPING.put("minecraft:smooth_stone_slab", "minecraft:stone_slab");
        BLOCK_NAME_MAPPING.put("minecraft:stone_brick_slab", "minecraft:stone_slab");
        BLOCK_NAME_MAPPING.put("minecraft:stone_brick_stairs", "minecraft:stone_brick_stairs");
        BLOCK_NAME_MAPPING.put("minecraft:cobblestone_stairs", "minecraft:stone_stairs");
        BLOCK_NAME_MAPPING.put("minecraft:cobblestone_wall", "minecraft:cobblestone_wall");
        BLOCK_NAME_MAPPING.put("minecraft:mossy_cobblestone_wall", "minecraft:cobblestone_wall");
        BLOCK_NAME_MAPPING.put("minecraft:cyan_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_NAME_MAPPING.put("minecraft:light_gray_wool", "minecraft:wool");
        BLOCK_NAME_MAPPING.put("minecraft:light_gray_concrete", "minecraft:concrete");
        BLOCK_NAME_MAPPING.put("minecraft:gray_concrete", "minecraft:concrete");
        BLOCK_NAME_MAPPING.put("minecraft:brown_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_NAME_MAPPING.put("minecraft:green_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_NAME_MAPPING.put("minecraft:purple_stained_glass", "minecraft:stained_glass");
        BLOCK_NAME_MAPPING.put("minecraft:nether_brick_fence", "minecraft:nether_brick_fence");
        BLOCK_NAME_MAPPING.put("minecraft:oak_fence", "minecraft:fence");
        BLOCK_NAME_MAPPING.put("minecraft:oak_fence_gate", "minecraft:fence_gate");
        BLOCK_NAME_MAPPING.put("minecraft:oak_trapdoor", "minecraft:trapdoor");
        BLOCK_NAME_MAPPING.put("minecraft:oak_wall_sign", "minecraft:wall_sign");
        BLOCK_NAME_MAPPING.put("minecraft:wall_torch", "minecraft:torch");
        BLOCK_NAME_MAPPING.put("minecraft:potted_dead_bush", "minecraft:flower_pot");

        // 需要移除的 1.13+ 属性
        PROPERTIES_TO_REMOVE.add("waterlogged");
    }
    public String author = null;
    public String name = null;
    public long creationDate;
    public String[] requiredMods = {};
    public short width;
    public short height;
    public short length;
    public int[] offset = {0, 0, 0};
    public int paletteMax;
    public List<IBlockState> palette = new ArrayList<>();
    public short[][][] blockData; //[x][y][z]
    public List<NBTTagCompound> tileEntities = new ArrayList<>();
    public List<NBTTagCompound> entities = new ArrayList<>();

    public Schematic() {
        paletteMax = -1;
    }

    public Schematic(short width, short height, short length) {
        this();
        this.width = width;
        this.height = height;
        this.length = length;
        blockData = new short[width][height][length];
        palette.add(Blocks.AIR.getDefaultState());
        paletteMax++;
        creationDate = System.currentTimeMillis();
    }

    public Schematic(String name, String author, short width, short height, short length) {
        this(width, height, length);
        this.name = name;
        this.author = author;
    }

    public static Schematic loadFromNBT(NBTTagCompound nbt) {
        // 检测 schematic 格式
        // MCEdit/WorldEdit 格式: 有 Blocks 和 Data 标签
        // Sponge 格式: 有 Palette 和 BlockData 标签
        if (nbt.hasKey("Blocks") && nbt.hasKey("Data")) {
            return loadFromMCEditNBT(nbt);
        }
        return loadFromSpongeNBT(nbt);
    }

    /**
     * 加载 MCEdit/WorldEdit 格式的 schematic 文件
     * 这是旧版格式，使用 Blocks (方块ID) + Data (元数据)
     */
    private static Schematic loadFromMCEditNBT(NBTTagCompound nbt) {
        Schematic schematic = new Schematic();
        schematic.version = 1; // MCEdit 格式视为版本1

        schematic.width = nbt.getShort("Width");
        schematic.height = nbt.getShort("Height");
        schematic.length = nbt.getShort("Length");

        System.out.println("[Schematic] 加载MCEdit格式 - 尺寸: " + schematic.width + "x" + schematic.height + "x" + schematic.length);

        // 读取 WorldEdit 偏移量（如果有）
        if (nbt.hasKey("WEOffsetX")) {
            schematic.offset = new int[] {
                nbt.getInteger("WEOffsetX"),
                nbt.getInteger("WEOffsetY"),
                nbt.getInteger("WEOffsetZ")
            };
        }

        // 读取方块数据
        byte[] blocks = nbt.getByteArray("Blocks");
        byte[] data = nbt.getByteArray("Data");
        byte[] addBlocks = nbt.hasKey("AddBlocks") ? nbt.getByteArray("AddBlocks") : null;

        // 初始化方块数据数组
        schematic.blockData = new short[schematic.width][schematic.height][schematic.length];

        // 构建调色板映射 (blockId:meta -> palette index)
        Map<String, Integer> paletteMap = new HashMap<>();
        schematic.palette.clear();
        schematic.paletteMax = -1;

        // 遍历所有方块位置
        for (int y = 0; y < schematic.height; y++) {
            for (int z = 0; z < schematic.length; z++) {
                for (int x = 0; x < schematic.width; x++) {
                    int index = (y * schematic.length + z) * schematic.width + x;
                    if (index >= blocks.length) continue;

                    // 计算方块ID (支持 AddBlocks 扩展)
                    int blockId = blocks[index] & 0xFF;
                    if (addBlocks != null && index / 2 < addBlocks.length) {
                        int addBlockValue = addBlocks[index / 2] & 0xFF;
                        if ((index & 1) == 0) {
                            blockId |= (addBlockValue & 0x0F) << 8;
                        } else {
                            blockId |= (addBlockValue & 0xF0) << 4;
                        }
                    }

                    // 获取元数据
                    int meta = (index < data.length) ? (data[index] & 0x0F) : 0;

                    // 获取或创建调色板条目
                    String key = blockId + ":" + meta;
                    Integer paletteIndex = paletteMap.get(key);
                    if (paletteIndex == null) {
                        // 根据ID和meta获取方块状态
                        IBlockState state = getBlockStateFromIdMeta(blockId, meta);
                        paletteIndex = ++schematic.paletteMax;
                        schematic.palette.add(state);
                        paletteMap.put(key, paletteIndex);
                    }

                    schematic.blockData[x][y][z] = paletteIndex.shortValue();
                }
            }
        }

        System.out.println("[Schematic] MCEdit格式加载完成 - 调色板大小: " + schematic.palette.size());

        // 读取 TileEntities
        if (nbt.hasKey("TileEntities")) {
            NBTTagList tileEntitiesTagList = (NBTTagList) nbt.getTag("TileEntities");
            for (int i = 0; i < tileEntitiesTagList.tagCount(); i++) {
                NBTTagCompound tileEntityTagCompound = tileEntitiesTagList.getCompoundTagAt(i);
                schematic.tileEntities.add(tileEntityTagCompound);
            }
        }

        // 读取 Entities
        if (nbt.hasKey("Entities")) {
            NBTTagList entitiesTagList = (NBTTagList) nbt.getTag("Entities");
            for (int i = 0; i < entitiesTagList.tagCount(); i++) {
                NBTTagCompound entityTagCompound = entitiesTagList.getCompoundTagAt(i);
                schematic.entities.add(entityTagCompound);
            }
        }

        return schematic;
    }

    /**
     * 根据旧版方块ID和元数据获取方块状态
     */
    @SuppressWarnings("deprecation")
    private static IBlockState getBlockStateFromIdMeta(int blockId, int meta) {
        Block block = Block.getBlockById(blockId);
        if (block == null || block == Blocks.AIR) {
            if (blockId != 0) {
                System.err.println("[Schematic] 警告: 未知方块ID " + blockId + ", 替换为空气");
            }
            return Blocks.AIR.getDefaultState();
        }
        try {
            return block.getStateFromMeta(meta);
        } catch (Exception e) {
            return block.getDefaultState();
        }
    }

    /**
     * 加载 Sponge Schematic 格式
     */
    private static Schematic loadFromSpongeNBT(NBTTagCompound nbt) {
        Schematic schematic = new Schematic();
        schematic.version = nbt.getInteger("Version");

        schematic.creationDate = System.currentTimeMillis();
        if (nbt.hasKey("Metadata")) {
            NBTTagCompound metadataCompound = nbt.getCompoundTag("Metadata");
            if (metadataCompound.hasKey("Author")) {
                schematic.author = metadataCompound.getString("Author");
            }
            schematic.name = metadataCompound.getString("Name");

            if (metadataCompound.hasKey("Date")) {
                schematic.creationDate = metadataCompound.getLong("Date");
            } else {
                schematic.creationDate = -1;
            }
            if (metadataCompound.hasKey("RequiredMods")) {
                NBTTagList requiredModsTagList = (NBTTagList) metadataCompound.getTag("RequiredMods");
                schematic.requiredMods = new String[requiredModsTagList.tagCount()];
                for (int i = 0; i < requiredModsTagList.tagCount(); i++) {
                    schematic.requiredMods[i] = requiredModsTagList.getStringTagAt(i);
                }
            }
        }

        schematic.width = nbt.getShort("Width");
        schematic.height = nbt.getShort("Height");
        schematic.length = nbt.getShort("Length");
        if (nbt.hasKey("Offset")) {
            schematic.offset = nbt.getIntArray("Offset");
        }

        NBTTagCompound paletteNBT = nbt.getCompoundTag("Palette");
        Map<Integer, String> paletteMap = new HashMap<>();
        for (String key : paletteNBT.getKeySet()) {
            int paletteID = paletteNBT.getInteger(key);
            paletteMap.put(paletteID, key);
        }

        for (int i = 0; i < paletteMap.size(); i++) {
            String blockStateString = paletteMap.get(i);
            // 转换 1.13+ 格式到 1.12.2
            blockStateString = convert113To112(blockStateString);

            char lastBlockStateStringChar = blockStateString.charAt(blockStateString.length() - 1);
            String blockString;
            String stateString;
            if (lastBlockStateStringChar == ']') {
                String[] blockAndStateStrings = blockStateString.split("\\[");
                blockString = blockAndStateStrings[0];
                stateString = blockAndStateStrings[1];
                stateString = stateString.substring(0, stateString.length() - 1);
            } else {
                blockString = blockStateString;
                stateString = "";
            }
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockString));

            if (block == null || block == Blocks.AIR && !blockString.contains("air")) {
                System.err.println("[Schematic] 警告: 未知方块 " + blockString + " (原始: " + paletteMap.get(i) + ")，替换为空气");
                schematic.palette.add(Blocks.AIR.getDefaultState());
                continue;
            }

            IBlockState blockstate = block.getDefaultState();
            if (!stateString.equals("")) {
                String[] properties = stateString.split(",");
                blockstate = getBlockStateWithProperties(block, properties);
            }
            schematic.palette.add(blockstate);
        }

        if (nbt.hasKey("PaletteMax")) {
            schematic.paletteMax = nbt.getInteger("PaletteMax");
        } else {
            schematic.paletteMax = schematic.palette.size() - 1;
        }

        byte[] blockDataIntArray = nbt.getByteArray("BlockData");
        schematic.blockData = new short[schematic.width][schematic.height][schematic.length];

        // 1.13+ schematic 使用 varint 编码
        if (schematic.version >= 2 && schematic.paletteMax > 127) {
            // 使用 varint 解码
            int[] decodedData = decodeVarintBlockData(blockDataIntArray, schematic.width * schematic.height * schematic.length);
            int idx = 0;
            for (int y = 0; y < schematic.height; y++) {
                for (int z = 0; z < schematic.length; z++) {
                    for (int x = 0; x < schematic.width; x++) {
                        if (idx < decodedData.length) {
                            schematic.blockData[x][y][z] = (short) decodedData[idx++];
                        }
                    }
                }
            }
        } else {
            // 标准格式或小调色板
            for (int x = 0; x < schematic.width; x++) {
                for (int y = 0; y < schematic.height; y++) {
                    for (int z = 0; z < schematic.length; z++) {
                        int idx = x + z * schematic.width + y * schematic.width * schematic.length;
                        if (idx < blockDataIntArray.length) {
                            schematic.blockData[x][y][z] = (short) (blockDataIntArray[idx] & 0xFF);
                        }
                    }
                }
            }
        }

        if (nbt.hasKey("TileEntities")) {
            NBTTagList tileEntitiesTagList = (NBTTagList) nbt.getTag("TileEntities");
            for (int i = 0; i < tileEntitiesTagList.tagCount(); i++) {
                NBTTagCompound tileEntityTagCompound = tileEntitiesTagList.getCompoundTagAt(i);
                schematic.tileEntities.add(tileEntityTagCompound);
            }
        }

        if (nbt.hasKey("Entities")) {
            NBTTagList entitiesTagList = (NBTTagList) nbt.getTag("Entities");
            for (int i = 0; i < entitiesTagList.tagCount(); i++) {
                NBTTagCompound entityTagCompound = entitiesTagList.getCompoundTagAt(i);
                schematic.entities.add(entityTagCompound);
            }
        }

        return schematic;
    }

    public NBTTagCompound saveToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setInteger("Version", version);
        NBTTagCompound metadataCompound = new NBTTagCompound();
        if (author != null) metadataCompound.setString("Author", author);
        metadataCompound.setString("Name", name);
        if (creationDate != -1) metadataCompound.setLong("Date", creationDate);
        NBTTagList requiredModsTagList = new NBTTagList();
        for (String requiredMod : requiredMods) {
            requiredModsTagList.appendTag(new NBTTagString(requiredMod));
        }
        metadataCompound.setTag("RequiredMods", requiredModsTagList);
        nbt.setTag("Metadata", metadataCompound);

        nbt.setShort("Width", width);
        nbt.setShort("Height", height);
        nbt.setShort("Length", length);
        nbt.setIntArray("Offset", offset);
        nbt.setInteger("PaletteMax", paletteMax);

        NBTTagCompound paletteNBT = new NBTTagCompound();
        for (int i = 0; i < palette.size(); i++) {
            IBlockState state = palette.get(i);
            String blockStateString = getBlockStateStringFromState(state);
            paletteNBT.setInteger(blockStateString, i);
        }
        nbt.setTag("Palette", paletteNBT);

        byte[] blockDataIntArray = new byte[width * height * length];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    blockDataIntArray[x + z * width + y * width * length] = (byte) blockData[x][y][z];
                }
            }
        }
        nbt.setByteArray("BlockData", blockDataIntArray);

        NBTTagList tileEntitiesTagList = new NBTTagList();
        for (NBTTagCompound tileEntityTagCompound : tileEntities) {
            tileEntitiesTagList.appendTag(tileEntityTagCompound);
        }
        nbt.setTag("TileEntities", tileEntitiesTagList);

        NBTTagList entitiesTagList = new NBTTagList();
        for (NBTTagCompound entityTagCompound : entities) {
            entitiesTagList.appendTag(entityTagCompound);
        }
        nbt.setTag("Entities", entitiesTagList);

        return nbt;
    }

    static IBlockState getBlockStateWithProperties(Block block, String[] properties) {
        Map<String, String> propertyAndBlockStringsMap = new HashMap<>();
        for (String property : properties) {
            String[] propertyAndBlockStrings = property.split("=");
            propertyAndBlockStringsMap.put(propertyAndBlockStrings[0], propertyAndBlockStrings[1]);
        }
        BlockStateContainer container = block.getBlockState();
        IBlockState chosenState = block.getDefaultState();
        for (Entry<String, String> entry : propertyAndBlockStringsMap.entrySet()) {
            IProperty<?> property = container.getProperty(entry.getKey());
            if (property != null) {
                Comparable<?> value = null;
                for (Comparable<?> object : property.getAllowedValues()) {
                    if (object.toString().equals(entry.getValue())) {
                        value = object;
                        break;
                    }
                }
                if (value != null) {
                    chosenState = chosenState.withProperty((IProperty) property, (Comparable) value);
                }
            }
        }
        return chosenState;
    }

    private static String getBlockStateStringFromState(IBlockState state) {
        Block block = state.getBlock();
        String blockNameString = String.valueOf(Block.REGISTRY.getNameForObject(block));
        StringBuilder blockStateString = new StringBuilder();
        String totalString;
        IBlockState defaultState = block.getDefaultState();
        if (state == defaultState) {
            totalString = blockNameString;
        } else {
            BlockStateContainer container = block.getBlockState();
            for (IProperty<?> property : container.getProperties()) {
                String defaultPropertyValue = defaultState.getProperties().get(property).toString();
                String thisPropertyValue = state.getProperties().get(property).toString();
                if (!defaultPropertyValue.equals(thisPropertyValue)) {
                    String firstHalf = property.getName();
                    String secondHalf = state.getProperties().get(property).toString();
                    String propertyString = firstHalf + "=" + secondHalf;
                    blockStateString.append(propertyString).append(",");
                }
            }
            blockStateString = new StringBuilder(blockStateString.substring(0, blockStateString.length() - 1));
            totalString = blockNameString + "[" + blockStateString + "]";
        }
        return totalString;
    }

    public static Schematic createFromWorld(World world, BlockPos from, BlockPos to) {
        BlockPos dimensions = to.subtract(from).add(1, 1, 1);
        Schematic schematic = new Schematic((short) dimensions.getX(), (short) dimensions.getY(), (short) dimensions.getZ());

        Set<String> mods = new HashSet<>();

        for (int x = 0; x < dimensions.getX(); x++) {
            for (int y = 0; y < dimensions.getY(); y++) {
                for (int z = 0; z < dimensions.getZ(); z++) {
                    BlockPos pos = new BlockPos(from.getX() + x, from.getY() + y, from.getZ() + z);

                    IBlockState state = world.getBlockState(pos);
                    String id = getBlockStateStringFromState(state);
                    if (id.contains(":")) mods.add(id.split(":")[0]);
                    schematic.setBlockState(x, y, z, state);

                    TileEntity tileEntity = world.getChunk(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
                    if (tileEntity != null) {
                        NBTTagCompound tileEntityNBT = tileEntity.serializeNBT();
                        tileEntityNBT.setInteger("x", tileEntityNBT.getInteger("x") - from.getX());
                        tileEntityNBT.setInteger("y", tileEntityNBT.getInteger("y") - from.getY());
                        tileEntityNBT.setInteger("z", tileEntityNBT.getInteger("z") - from.getZ());

                        schematic.tileEntities.add(tileEntityNBT);
                    }
                }
            }
        }

        for (Entity entity : world.getEntitiesInAABBexcluding(null, getBoundingBox(from, to), entity -> !(entity instanceof EntityPlayerMP))) {
            NBTTagCompound entityNBT = entity.serializeNBT();

            NBTTagList posNBT = (NBTTagList) entityNBT.getTag("Pos");
            NBTTagList newPosNBT = new NBTTagList();
            newPosNBT.appendTag(new NBTTagDouble(posNBT.getDoubleAt(0) - from.getX()));
            newPosNBT.appendTag(new NBTTagDouble(posNBT.getDoubleAt(1) - from.getY()));
            newPosNBT.appendTag(new NBTTagDouble(posNBT.getDoubleAt(2) - from.getZ()));
            entityNBT.setTag("Pos", newPosNBT);

            schematic.entities.add(entityNBT);
        }

        schematic.requiredMods = mods.toArray(new String[mods.size()]);
        schematic.creationDate = System.currentTimeMillis();

        return schematic;
    }

    private static AxisAlignedBB getBoundingBox(Vec3i from, Vec3i to) {
        return new AxisAlignedBB(new BlockPos(from.getX(), from.getY(), from.getZ()),
                new BlockPos(to.getX(), to.getY(), to.getZ()));
    }

    public void place(World world, int xBase, int yBase, int zBase) {
        // 放置方块
        setBlocks(world, xBase, yBase, zBase);

        // 设置TileEntity数据
        for (NBTTagCompound tileEntityNBT : tileEntities) {
            Vec3i schematicPos = new BlockPos(tileEntityNBT.getInteger("x"),
                    tileEntityNBT.getInteger("y"),
                    tileEntityNBT.getInteger("z"));
            BlockPos pos = new BlockPos(xBase, yBase, zBase).add(schematicPos);
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity != null) {
                String schematicTileEntityId = tileEntityNBT.getString("id");
                String blockTileEntityId = TileEntity.getKey(tileEntity.getClass()).toString();
                if (schematicTileEntityId.equals(blockTileEntityId)) {
                    tileEntity.readFromNBT(tileEntityNBT);

                    // 修正位置
                    tileEntity.setWorld(world);
                    tileEntity.setPos(pos);
                    tileEntity.markDirty();
                } else {
                    System.err.println("TileEntity类型不匹配: " + schematicTileEntityId + " vs " + blockTileEntityId);
                }
            }
        }

        // 生成实体
        for (NBTTagCompound entityNBT : entities) {
            NBTTagList posNBT = (NBTTagList) entityNBT.getTag("Pos");
            NBTTagList newPosNBT = new NBTTagList();
            newPosNBT.appendTag(new NBTTagDouble(posNBT.getDoubleAt(0) + xBase));
            newPosNBT.appendTag(new NBTTagDouble(posNBT.getDoubleAt(1) + yBase));
            newPosNBT.appendTag(new NBTTagDouble(posNBT.getDoubleAt(2) + zBase));
            NBTTagCompound adjustedEntityNBT = entityNBT.copy();
            adjustedEntityNBT.setTag("Pos", newPosNBT);
            adjustedEntityNBT.setUniqueId("UUID", UUID.randomUUID());

            Entity entity = EntityList.createEntityFromNBT(adjustedEntityNBT, world);
            if (entity != null) {
                world.spawnEntity(entity);
            }
        }
    }

    public IBlockState getBlockState(int x, int y, int z) {
        // 边界检查，防止越界导致整个房间生成失败
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= length) {
            return Blocks.AIR.getDefaultState();
        }
        return palette.get(blockData[x][y][z]);
    }

    public void setBlockState(int x, int y, int z, IBlockState state) {
        // 边界检查，防止越界导致整个房间生成失败
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= length) {
            // 静默忽略越界的方块放置，但记录警告（可选）
            // System.err.println("[Schematic] 警告: 方块越界 (" + x + ", " + y + ", " + z + ") 范围: (" + width + ", " + height + ", " + length + ")");
            return;
        }
        if (palette.contains(state)) {
            blockData[x][y][z] = (short) palette.indexOf(state);
        } else {
            palette.add(state);
            blockData[x][y][z] = (short) ++paletteMax;
        }
    }

    /**
     * 优化的方块放置方法 - 只使用标准Minecraft chunk系统
     */
    private void setBlocks(World world, int xBase, int yBase, int zBase) {
        long setTime = 0;
        long relightTime = 0;

        System.out.println("[moremod] 开始放置 schematic 方块...");

        // 使用标准的chunk系统
        for (int chunkX = 0; chunkX <= (width >> 4) + 1; chunkX++) {
            for (int chunkZ = 0; chunkZ <= (length >> 4) + 1; chunkZ++) {
                long setStart = System.nanoTime();

                // 获取chunk - 使用兼容1.12.2的方法
                Chunk chunk = world.getChunk((xBase >> 4) + chunkX, (zBase >> 4) + chunkZ);
                ExtendedBlockStorage[] storageArray = chunk.getBlockStorageArray();

                for (int storageY = 0; storageY <= (height >> 4) + 1; storageY++) {
                    // 获取或创建存储区
                    int storageIndex = (yBase >> 4) + storageY;
                    if (storageIndex < 0 || storageIndex >= 16) continue;

                    ExtendedBlockStorage storage = storageArray[storageIndex];
                    boolean needsNewStorage = (storage == null);

                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                int sx = (chunkX << 4) + x - (xBase & 0x0F);
                                int sy = (storageY << 4) + y - (yBase & 0x0F);
                                int sz = (chunkZ << 4) + z - (zBase & 0x0F);

                                if (sx >= 0 && sy >= 0 && sz >= 0 && sx < width && sy < height && sz < length) {
                                    IBlockState state = palette.get(blockData[sx][sy][sz]);

                                    if (!state.getBlock().equals(Blocks.AIR)) {
                                        if (needsNewStorage) {
                                            storage = new ExtendedBlockStorage(storageY << 4, world.provider.hasSkyLight());
                                            storageArray[storageIndex] = storage;
                                            needsNewStorage = false;
                                        }
                                        storage.set(x, y, z, state);
                                    } else if (storage != null) {
                                        storage.set(x, y, z, state);
                                    }
                                }
                            }
                        }
                    }
                }

                setTime += System.nanoTime() - setStart;

                // 更新光照
                long relightStart = System.nanoTime();
                chunk.setLightPopulated(false);
                chunk.setTerrainPopulated(true);
                chunk.resetRelightChecks();
                chunk.checkLight();
                relightTime += System.nanoTime() - relightStart;

                chunk.markDirty();
            }
        }

        // 触发客户端更新
        world.markBlockRangeForRenderUpdate(xBase, yBase, zBase, xBase + width, yBase + height, zBase + length);

        System.out.println("[moremod] Schematic放置完成 - 方块设置: " + setTime / 1000000 + "ms, 光照计算: " + relightTime / 1000000 + "ms");
    }

    /**
     * 将 1.13+ 方块状态字符串转换为 1.12.2 格式
     */
    private static String convert113To112(String blockStateString) {
        String blockName;
        String properties = "";

        if (blockStateString.contains("[")) {
            int bracketIdx = blockStateString.indexOf('[');
            blockName = blockStateString.substring(0, bracketIdx);
            properties = blockStateString.substring(bracketIdx + 1, blockStateString.length() - 1);
        } else {
            blockName = blockStateString;
        }

        // 转换方块名称
        if (BLOCK_NAME_MAPPING.containsKey(blockName)) {
            blockName = BLOCK_NAME_MAPPING.get(blockName);
        }

        // 处理属性
        if (!properties.isEmpty()) {
            StringBuilder newProps = new StringBuilder();
            String[] propArray = properties.split(",");
            for (String prop : propArray) {
                String[] kv = prop.split("=");
                if (kv.length == 2) {
                    String key = kv[0];
                    String value = kv[1];

                    // 跳过 1.13+ 专有属性
                    if (PROPERTIES_TO_REMOVE.contains(key)) {
                        continue;
                    }

                    // 转换属性值
                    // 1.13 使用 "none", "low", "tall" 来表示墙的连接状态
                    // 1.12 使用 "true", "false"
                    if (key.equals("north") || key.equals("south") || key.equals("east") || key.equals("west")) {
                        if (value.equals("none")) {
                            value = "false";
                        } else if (value.equals("low") || value.equals("tall")) {
                            value = "true";
                        }
                    }

                    // 1.13 石砖楼梯的 shape 属性处理
                    // "straight", "inner_left", "inner_right", "outer_left", "outer_right"
                    // 在 1.12.2 中这些是自动计算的，不需要手动设置

                    // type 属性: bottom/top/double 需要转换
                    if (key.equals("type")) {
                        if (value.equals("bottom")) {
                            // 1.12 使用 half=bottom
                            key = "half";
                            value = "bottom";
                        } else if (value.equals("top")) {
                            key = "half";
                            value = "top";
                        } else if (value.equals("double")) {
                            // double slab 在 1.12 是单独的方块
                            continue;
                        }
                    }

                    if (newProps.length() > 0) {
                        newProps.append(",");
                    }
                    newProps.append(key).append("=").append(value);
                }
            }
            properties = newProps.toString();
        }

        if (properties.isEmpty()) {
            return blockName;
        } else {
            return blockName + "[" + properties + "]";
        }
    }

    /**
     * 解码 varint 编码的方块数据
     */
    private static int[] decodeVarintBlockData(byte[] data, int expectedSize) {
        int[] result = new int[expectedSize];
        int resultIdx = 0;
        int dataIdx = 0;

        while (dataIdx < data.length && resultIdx < expectedSize) {
            int value = 0;
            int shift = 0;

            while (true) {
                if (dataIdx >= data.length) break;
                byte b = data[dataIdx++];
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
            }

            result[resultIdx++] = value;
        }

        return result;
    }

    /**
     * 从大 schematic 中提取子区域
     * @param startX 起始 X
     * @param startY 起始 Y
     * @param startZ 起始 Z
     * @param w 宽度
     * @param h 高度
     * @param l 长度
     * @return 新的 Schematic 包含子区域
     */
    public Schematic extractRegion(int startX, int startY, int startZ, int w, int h, int l) {
        Schematic sub = new Schematic((short) w, (short) h, (short) l);
        sub.name = this.name + "_sub";
        sub.author = this.author;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                for (int z = 0; z < l; z++) {
                    int srcX = startX + x;
                    int srcY = startY + y;
                    int srcZ = startZ + z;

                    if (srcX >= 0 && srcX < width && srcY >= 0 && srcY < height && srcZ >= 0 && srcZ < length) {
                        IBlockState state = getBlockState(srcX, srcY, srcZ);
                        sub.setBlockState(x, y, z, state);
                    }
                }
            }
        }

        return sub;
    }
}