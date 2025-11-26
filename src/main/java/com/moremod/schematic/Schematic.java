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
 */
public class Schematic {

    public int version = 1;
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
        for (int x = 0; x < schematic.width; x++) {
            for (int y = 0; y < schematic.height; y++) {
                for (int z = 0; z < schematic.length; z++) {
                    schematic.blockData[x][y][z] = blockDataIntArray[x + z * schematic.width + y * schematic.width * schematic.length];
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
        return palette.get(blockData[x][y][z]);
    }

    public void setBlockState(int x, int y, int z, IBlockState state) {
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
}