package com.moremod.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * 石油提取数据 - 持久化保存到世界
 *
 * 用于追踪每个区块的石油提取量，即使TileEntity被破坏也能保留数据
 */
public class OilExtractionData extends WorldSavedData {

    private static final String DATA_NAME = "moremod_oil_extraction";

    // 区块位置 -> 已提取量
    private final Map<Long, Integer> extractedAmounts = new HashMap<>();

    public OilExtractionData() {
        super(DATA_NAME);
    }

    public OilExtractionData(String name) {
        super(name);
    }

    /**
     * 获取区块的已提取石油量
     */
    public int getExtractedAmount(ChunkPos chunkPos) {
        return extractedAmounts.getOrDefault(getKey(chunkPos), 0);
    }

    /**
     * 增加区块的提取量
     */
    public void addExtractedAmount(ChunkPos chunkPos, int amount) {
        long key = getKey(chunkPos);
        int current = extractedAmounts.getOrDefault(key, 0);
        extractedAmounts.put(key, current + amount);
        markDirty();
    }

    /**
     * 设置区块的提取量
     */
    public void setExtractedAmount(ChunkPos chunkPos, int amount) {
        extractedAmounts.put(getKey(chunkPos), amount);
        markDirty();
    }

    private long getKey(ChunkPos pos) {
        return ((long) pos.x << 32) | (pos.z & 0xFFFFFFFFL);
    }

    private ChunkPos fromKey(long key) {
        int x = (int) (key >> 32);
        int z = (int) key;
        return new ChunkPos(x, z);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        extractedAmounts.clear();

        int[] keys = nbt.getIntArray("ChunkKeys");
        int[] values = nbt.getIntArray("ExtractedValues");

        // 兼容旧格式
        if (keys.length == 0 && nbt.hasKey("Chunks")) {
            NBTTagCompound chunks = nbt.getCompoundTag("Chunks");
            for (String keyStr : chunks.getKeySet()) {
                try {
                    long key = Long.parseLong(keyStr);
                    extractedAmounts.put(key, chunks.getInteger(keyStr));
                } catch (NumberFormatException ignored) {}
            }
        } else {
            // 新格式：使用两个数组（更高效）
            int count = Math.min(keys.length / 2, values.length);
            for (int i = 0; i < count; i++) {
                long key = ((long) keys[i * 2] << 32) | (keys[i * 2 + 1] & 0xFFFFFFFFL);
                extractedAmounts.put(key, values[i]);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        int size = extractedAmounts.size();
        int[] keys = new int[size * 2];
        int[] values = new int[size];

        int i = 0;
        for (Map.Entry<Long, Integer> entry : extractedAmounts.entrySet()) {
            long key = entry.getKey();
            keys[i * 2] = (int) (key >> 32);
            keys[i * 2 + 1] = (int) key;
            values[i] = entry.getValue();
            i++;
        }

        nbt.setIntArray("ChunkKeys", keys);
        nbt.setIntArray("ExtractedValues", values);

        return nbt;
    }

    /**
     * 获取世界的石油提取数据
     */
    public static OilExtractionData get(World world) {
        MapStorage storage = world.getMapStorage();
        if (storage == null) {
            return new OilExtractionData();
        }

        OilExtractionData data = (OilExtractionData) storage.getOrLoadData(OilExtractionData.class, DATA_NAME);

        if (data == null) {
            data = new OilExtractionData();
            storage.setData(DATA_NAME, data);
        }

        return data;
    }
}
