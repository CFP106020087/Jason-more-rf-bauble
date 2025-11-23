package com.moremod.dungeon.portal;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 修正版傳送門管理器 - 確保數據正確持久化
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class PortalManager extends WorldSavedData {

    private static final String DATA_NAME = "MoreModPortalData";

    // 使用線程安全的Map
    private final Map<String, String> portalLinks = new ConcurrentHashMap<>();

    // 靜態實例引用
    private static PortalManager instance;

    public PortalManager() {
        super(DATA_NAME);
    }

    public PortalManager(String name) {
        super(name);
    }

    /**
     * 清除實例（用於重新加載）
     */
    public static void clearInstance() {
        instance = null;
    }

    /**
     * 獲取管理器實例 - 修正版
     */
    public static PortalManager getInstance() {
        if (instance != null) {
            return instance;
        }

        // 獲取主世界
        WorldServer overworld = DimensionManager.getWorld(0);
        if (overworld == null) {
            return null;
        }

        return getInstance(overworld);
    }

    /**
     * 從世界獲取管理器實例
     */
    public static PortalManager getInstance(World world) {
        // 總是使用主世界存儲
        if (world.provider.getDimension() != 0) {
            WorldServer overworld = DimensionManager.getWorld(0);
            if (overworld != null) {
                world = overworld;
            }
        }

        MapStorage storage = world.getPerWorldStorage();
        PortalManager manager = (PortalManager) storage.getOrLoadData(
                PortalManager.class, DATA_NAME);

        if (manager == null) {
            manager = new PortalManager();
            storage.setData(DATA_NAME, manager);
        }

        instance = manager;
        return manager;
    }

    /**
     * 生成位置鍵值
     */
    private static String makeKey(int dim, BlockPos pos) {
        return String.format("%d_%d_%d_%d", dim, pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * 解析鍵值
     */
    private static LocationData parseKey(String key) {
        try {
            String[] parts = key.split("_");
            if (parts.length != 4) return null;

            int dim = Integer.parseInt(parts[0]);
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);

            return new LocationData(dim, new BlockPos(x, y, z));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 添加傳送鏈接
     */
    public void addLink(World world, BlockPos from, BlockPos to) {
        int dim = world.provider.getDimension();
        String keyFrom = makeKey(dim, from);
        String keyTo = makeKey(dim, to);

        portalLinks.put(keyFrom, keyTo);
        portalLinks.put(keyTo, keyFrom);

        // 立即標記需要保存
        this.markDirty();

        // 強制保存（確保數據寫入）
        forceSave();
    }

    /**
     * 添加跨維度鏈接
     */
    public void addCrossDimLink(int dimFrom, BlockPos from, int dimTo, BlockPos to) {
        String keyFrom = makeKey(dimFrom, from);
        String keyTo = makeKey(dimTo, to);

        portalLinks.put(keyFrom, keyTo);
        portalLinks.put(keyTo, keyFrom);

        this.markDirty();
        forceSave();
    }

    /**
     * 獲取傳送目標（實例方法）
     */
    public LocationData getDestinationFor(World world, BlockPos from) {
        int dim = world.provider.getDimension();
        String key = makeKey(dim, from);
        String destKey = portalLinks.get(key);

        if (destKey == null) {
            return null;
        }

        return parseKey(destKey);
    }

    /**
     * 移除鏈接
     */
    public void removeLink(World world, BlockPos pos) {
        int dim = world.provider.getDimension();
        String key = makeKey(dim, pos);
        String destKey = portalLinks.get(key);

        if (destKey != null) {
            portalLinks.remove(key);
            portalLinks.remove(destKey);

            this.markDirty();
            forceSave();
        }
    }

    /**
     * 檢查是否存在鏈接
     */
    public boolean hasLink(World world, BlockPos pos) {
        int dim = world.provider.getDimension();
        String key = makeKey(dim, pos);
        return portalLinks.containsKey(key);
    }

    /**
     * 獲取所有鏈接（用於調試）
     */
    public Map<String, String> getAllLinks() {
        return new HashMap<>(portalLinks);
    }

    /**
     * 獲取鏈接數量
     */
    public int getLinkCount() {
        return portalLinks.size() / 2;  // 除以2因為每個鏈接是雙向的
    }

    /**
     * 強制保存數據
     */
    private void forceSave() {
        WorldServer overworld = DimensionManager.getWorld(0);
        if (overworld != null) {
            MapStorage storage = overworld.getPerWorldStorage();
            storage.saveAllData();
        }
    }

    // ===== NBT 序列化（最重要的部分）=====

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        portalLinks.clear();

        // 讀取所有鏈接
        if (nbt.hasKey("Links", 9)) {  // 9 = NBT List
            NBTTagList list = nbt.getTagList("Links", 10);  // 10 = NBT Compound

            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound link = list.getCompoundTagAt(i);
                String from = link.getString("From");
                String to = link.getString("To");

                if (!from.isEmpty() && !to.isEmpty()) {
                    // 重要：恢復雙向映射！
                    portalLinks.put(from, to);
                    portalLinks.put(to, from);
                }
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        Set<String> processed = new HashSet<>();

        for (Map.Entry<String, String> entry : portalLinks.entrySet()) {
            String from = entry.getKey();
            String to = entry.getValue();

            // 避免重複保存雙向鏈接
            String pair = from.compareTo(to) < 0 ? from + "||" + to : to + "||" + from;
            if (!processed.contains(pair)) {
                NBTTagCompound link = new NBTTagCompound();
                link.setString("From", from);
                link.setString("To", to);
                list.appendTag(link);
                processed.add(pair);
            }
        }

        nbt.setTag("Links", list);
        return nbt;
    }

    // ===== 靜態便捷方法 =====

    /**
     * 註冊鏈接（靜態方法）
     */
    public static void registerLink(World world, BlockPos from, BlockPos to) {
        PortalManager manager = getInstance(world);
        if (manager != null) {
            manager.addLink(world, from, to);
        }
    }

    /**
     * 獲取目標（靜態方法）
     */
    public static LocationData getDestination(World world, BlockPos from) {
        PortalManager manager = getInstance(world);
        if (manager != null) {
            return manager.getDestinationFor(world, from);
        }
        return null;
    }

    /**
     * 位置數據
     */
    public static class LocationData {
        public final int dimension;
        public final BlockPos pos;

        public LocationData(int dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos;
        }
    }

    // ===== 事件處理 =====

    /**
     * 世界保存時確保數據寫入
     */
    @SubscribeEvent
    public static void onWorldSave(WorldEvent.Save event) {
        if (!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) {
            PortalManager manager = getInstance();
            if (manager != null) {
                manager.markDirty();
            }
        }
    }

    /**
     * 世界載入時加載數據
     */
    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) {
            PortalManager manager = getInstance(event.getWorld());
        }
    }

    /**
     * 世界卸載時清理
     */
    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) {
            if (instance != null) {
                instance.markDirty();
                instance.forceSave();
            }
            instance = null;
        }
    }
}