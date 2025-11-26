package com.moremod.util;

import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 反射辅助工具类
 * 处理 Minecraft 混淆名和开发名的映射
 */
public class ReflectionHelper {

    // 缓存已找到的字段
    private static final Map<String, Field> fieldCache = new HashMap<>();
    private static final Map<String, Method> methodCache = new HashMap<>();

    /**
     * MobSpawnerBaseLogic 字段名映射
     * 左边是开发环境名，右边是混淆名
     */
    public static class SpawnerFields {
        // 1.12.2 的字段映射
        public static final String[] SPAWN_DELAY = {"spawnDelay", "field_98286_b", "b"};
        public static final String[] MIN_SPAWN_DELAY = {"minSpawnDelay", "field_98283_g", "g"};
        public static final String[] MAX_SPAWN_DELAY = {"maxSpawnDelay", "field_98293_h", "h"};
        public static final String[] SPAWN_COUNT = {"spawnCount", "field_98294_i", "i"};
        public static final String[] MAX_NEARBY_ENTITIES = {"maxNearbyEntities", "field_98292_k", "k"};
        public static final String[] ACTIVATION_RANGE = {"activatingRangeFromPlayer", "field_98289_l", "l"};
        public static final String[] SPAWN_RANGE = {"spawnRange", "field_98290_m", "m"};
        public static final String[] REQUIRED_PLAYER_RANGE = {"requiredPlayerRange", "field_98289_l", "l"};
    }

    /**
     * 设置刷怪笼参数
     */
    public static void setSpawnerParams(MobSpawnerBaseLogic logic, SpawnerConfig config) {
        try {
            setFieldValue(logic, SpawnerFields.SPAWN_DELAY, config.spawnDelay);
            setFieldValue(logic, SpawnerFields.MIN_SPAWN_DELAY, config.minSpawnDelay);
            setFieldValue(logic, SpawnerFields.MAX_SPAWN_DELAY, config.maxSpawnDelay);
            setFieldValue(logic, SpawnerFields.SPAWN_COUNT, config.spawnCount);
            setFieldValue(logic, SpawnerFields.MAX_NEARBY_ENTITIES, config.maxNearbyEntities);
            setFieldValue(logic, SpawnerFields.SPAWN_RANGE, config.spawnRange);
            setFieldValue(logic, SpawnerFields.REQUIRED_PLAYER_RANGE, config.requiredPlayerRange);
        } catch (Exception e) {
            System.err.println("[ReflectionHelper] 设置刷怪笼参数失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 通过NBT设置刷怪笼（更安全的方式）
     */
    public static void setSpawnerByNBT(MobSpawnerBaseLogic logic, SpawnerConfig config) {
        NBTTagCompound nbt = new NBTTagCompound();
        logic.writeToNBT(nbt);

        // 修改NBT值
        nbt.setShort("Delay", (short)config.spawnDelay);
        nbt.setShort("MinSpawnDelay", (short)config.minSpawnDelay);
        nbt.setShort("MaxSpawnDelay", (short)config.maxSpawnDelay);
        nbt.setShort("SpawnCount", (short)config.spawnCount);
        nbt.setShort("MaxNearbyEntities", (short)config.maxNearbyEntities);
        nbt.setShort("RequiredPlayerRange", (short)config.requiredPlayerRange);
        nbt.setShort("SpawnRange", (short)config.spawnRange);

        // 设置生成权重（如果需要多种怪物）
        if (config.spawnPotentials != null && !config.spawnPotentials.isEmpty()) {
            NBTTagList potentials = new NBTTagList();
            for (SpawnPotential potential : config.spawnPotentials) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setInteger("Weight", potential.weight);

                NBTTagCompound entity = new NBTTagCompound();
                entity.setString("id", potential.entityId);
                if (potential.nbt != null) {
                    entity.merge(potential.nbt);
                }
                entry.setTag("Entity", entity);

                potentials.appendTag(entry);
            }
            nbt.setTag("SpawnPotentials", potentials);
        }

        // 读回NBT
        logic.readFromNBT(nbt);
    }

    /**
     * 设置字段值
     */
    private static void setFieldValue(Object obj, String[] fieldNames, Object value) throws Exception {
        Field field = findField(obj.getClass(), fieldNames);
        field.setAccessible(true);
        field.set(obj, value);
    }

    /**
     * 获取字段值
     */
    public static Object getFieldValue(Object obj, String[] fieldNames) throws Exception {
        Field field = findField(obj.getClass(), fieldNames);
        field.setAccessible(true);
        return field.get(obj);
    }

    /**
     * 查找字段（带缓存）
     */
    private static Field findField(Class<?> clazz, String[] names) throws NoSuchFieldException {
        String cacheKey = clazz.getName() + ":" + String.join(",", names);

        if (fieldCache.containsKey(cacheKey)) {
            return fieldCache.get(cacheKey);
        }

        for (String name : names) {
            try {
                Field field = findFieldRecursive(clazz, name);
                if (field != null) {
                    fieldCache.put(cacheKey, field);
                    return field;
                }
            } catch (NoSuchFieldException e) {
                // 继续尝试下一个名字
            }
        }

        throw new NoSuchFieldException("Field not found in " + clazz.getName() + ": " + String.join(", ", names));
    }

    /**
     * 递归查找字段（包括父类）
     */
    private static Field findFieldRecursive(Class<?> clazz, String name) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                return findFieldRecursive(superClass, name);
            }
            throw e;
        }
    }

    /**
     * 刷怪笼配置类
     */
    public static class SpawnerConfig {
        public int spawnDelay = 200;
        public int minSpawnDelay = 200;
        public int maxSpawnDelay = 400;
        public int spawnCount = 1;
        public int maxNearbyEntities = 6;
        public int requiredPlayerRange = 16;
        public int spawnRange = 4;
        public List<SpawnPotential> spawnPotentials;

        // 预设配置
        public static SpawnerConfig normal() {
            SpawnerConfig config = new SpawnerConfig();
            config.spawnDelay = 200;
            config.minSpawnDelay = 200;
            config.maxSpawnDelay = 400;
            config.spawnCount = 1;
            config.maxNearbyEntities = 4;
            return config;
        }

        public static SpawnerConfig hard() {
            SpawnerConfig config = new SpawnerConfig();
            config.spawnDelay = 100;
            config.minSpawnDelay = 100;
            config.maxSpawnDelay = 200;
            config.spawnCount = 2;
            config.maxNearbyEntities = 6;
            config.spawnRange = 5;
            return config;
        }

        public static SpawnerConfig extreme() {
            SpawnerConfig config = new SpawnerConfig();
            config.spawnDelay = 60;
            config.minSpawnDelay = 60;
            config.maxSpawnDelay = 120;
            config.spawnCount = 3;
            config.maxNearbyEntities = 8;
            config.spawnRange = 6;
            return config;
        }

        public static SpawnerConfig boss() {
            SpawnerConfig config = new SpawnerConfig();
            config.spawnDelay = 600;
            config.minSpawnDelay = 600;
            config.maxSpawnDelay = 1200;
            config.spawnCount = 1;
            config.maxNearbyEntities = 1;
            config.requiredPlayerRange = 32;
            config.spawnRange = 8;
            return config;
        }
    }

    /**
     * 刷怪潜力配置（用于随机生成不同怪物）
     */
    public static class SpawnPotential {
        public String entityId;
        public int weight;
        public NBTTagCompound nbt;

        public SpawnPotential(String entityId, int weight) {
            this.entityId = entityId;
            this.weight = weight;
        }

        public SpawnPotential withNBT(NBTTagCompound nbt) {
            this.nbt = nbt;
            return this;
        }
    }
}