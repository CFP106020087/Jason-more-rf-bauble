package com.moremod.module.host;

import com.moremod.module.api.IModuleHost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 玩家模块宿主 - 将玩家作为模块载体
 *
 * 特性:
 * - 基于玩家 EntityData 存储数据
 * - 支持持久化
 * - 自动管理数据生命周期
 */
public class PlayerModuleHost implements IModuleHost {

    private static final String DATA_PREFIX = "moremod.module.";

    private final EntityPlayer player;

    public PlayerModuleHost(@Nonnull EntityPlayer player) {
        this.player = player;
    }

    @Nonnull
    @Override
    public String getHostId() {
        return "player:" + player.getUniqueID().toString();
    }

    @Nonnull
    @Override
    public String getHostType() {
        return "player";
    }

    @Nullable
    @Override
    public Object getNativeHost() {
        return player;
    }

    @Nullable
    @Override
    public Object getHostData(@Nonnull String key) {
        NBTTagCompound data = player.getEntityData();
        String fullKey = DATA_PREFIX + key;

        if (data.hasKey(fullKey)) {
            // 支持多种类型
            if (data.hasKey(fullKey + "_string")) {
                return data.getString(fullKey + "_string");
            } else if (data.hasKey(fullKey + "_int")) {
                return data.getInteger(fullKey + "_int");
            } else if (data.hasKey(fullKey + "_long")) {
                return data.getLong(fullKey + "_long");
            } else if (data.hasKey(fullKey + "_double")) {
                return data.getDouble(fullKey + "_double");
            } else if (data.hasKey(fullKey + "_boolean")) {
                return data.getBoolean(fullKey + "_boolean");
            }
        }
        return null;
    }

    @Override
    public void setHostData(@Nonnull String key, @Nullable Object value) {
        NBTTagCompound data = player.getEntityData();
        String fullKey = DATA_PREFIX + key;

        // 清除旧数据
        data.removeTag(fullKey + "_string");
        data.removeTag(fullKey + "_int");
        data.removeTag(fullKey + "_long");
        data.removeTag(fullKey + "_double");
        data.removeTag(fullKey + "_boolean");

        if (value == null) {
            return;
        }

        // 根据类型存储
        if (value instanceof String) {
            data.setString(fullKey + "_string", (String) value);
        } else if (value instanceof Integer) {
            data.setInteger(fullKey + "_int", (Integer) value);
        } else if (value instanceof Long) {
            data.setLong(fullKey + "_long", (Long) value);
        } else if (value instanceof Double || value instanceof Float) {
            data.setDouble(fullKey + "_double", ((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            data.setBoolean(fullKey + "_boolean", (Boolean) value);
        } else {
            // 其他类型转为字符串
            data.setString(fullKey + "_string", value.toString());
        }
    }

    @Override
    public boolean isValid() {
        return player != null && !player.isDead;
    }

    @Override
    public boolean supportsPersistence() {
        return true;
    }

    /**
     * 获取玩家对象
     */
    public EntityPlayer getPlayer() {
        return player;
    }
}
