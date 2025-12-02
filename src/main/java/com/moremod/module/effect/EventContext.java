package com.moremod.module.effect;

import com.moremod.item.ItemMechanicalCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                          事件上下文 (EventContext)                            ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  提供模块事件处理所需的所有信息和辅助方法                                         ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                              基础属性                                          ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  • player      - EntityPlayer   玩家实例                                      ║
 * ║  • coreStack   - ItemStack      机械核心物品栈                                 ║
 * ║  • moduleId    - String         模块ID (如 "MAGIC_ABSORB")                    ║
 * ║  • level       - int            当前模块等级 (1 ~ maxLevel)                    ║
 * ║  • worldTime   - long           当前世界时间 (tick)                            ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                              能量方法                                          ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  • consumeEnergy(amount)        消耗能量，返回是否成功                          ║
 * ║  • getEnergy()                  获取当前能量                                   ║
 * ║  • getMaxEnergy()               获取最大能量                                   ║
 * ║  • getEnergyPercent()           获取能量百分比 (0.0 ~ 1.0)                     ║
 * ║  • hasEnergy(amount)            检查是否有足够能量                             ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                              NBT 数据存储                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  所有 NBT 方法会自动添加模块前缀，避免冲突                                        ║
 * ║                                                                              ║
 * ║  • setNBT(key, value)           设置值 (支持 int/float/double/boolean/String) ║
 * ║  • getNBTInt(key)               获取 int 值                                   ║
 * ║  • getNBTFloat(key)             获取 float 值                                 ║
 * ║  • getNBTDouble(key)            获取 double 值                                ║
 * ║  • getNBTBoolean(key)           获取 boolean 值                               ║
 * ║  • getNBTString(key)            获取 String 值                                ║
 * ║  • hasNBT(key)                  检查键是否存在                                 ║
 * ║  • removeNBT(key)               移除键                                        ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                              冷却系统                                          ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  • setCooldown(key, ticks)      设置冷却时间                                   ║
 * ║  • getCooldown(key)             获取剩余冷却时间 (tick)                        ║
 * ║  • isOnCooldown(key)            检查是否在冷却中                               ║
 * ║  • clearCooldown(key)           清除冷却                                      ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                              玩家数据                                          ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  • getPlayerNBT()               获取玩家的 persistentData                      ║
 * ║  • setPlayerData(key, value)    设置玩家数据                                   ║
 * ║  • getPlayerDataInt(key)        获取玩家 int 数据                              ║
 * ║  • ...等同于 NBT 方法                                                          ║
 * ║                                                                              ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
public class EventContext {

    // ===== 基础属性 =====
    public final EntityPlayer player;
    public final ItemStack coreStack;
    public final String moduleId;
    public final int level;
    public final long worldTime;

    // ===== 冷却缓存 (玩家UUID -> 模块ID -> 冷却键 -> 结束时间) =====
    private static final Map<UUID, Map<String, Map<String, Long>>> cooldownCache = new ConcurrentHashMap<>();

    public EventContext(EntityPlayer player, ItemStack coreStack, String moduleId, int level) {
        this.player = player;
        this.coreStack = coreStack;
        this.moduleId = moduleId;
        this.level = level;
        this.worldTime = player.world.getTotalWorldTime();
    }

    // ==================== 能量方法 ====================

    /**
     * 消耗能量
     * @param amount 消耗量 (RF)
     * @return 是否成功消耗
     */
    public boolean consumeEnergy(int amount) {
        return ItemMechanicalCore.consumeEnergy(coreStack, amount);
    }

    /**
     * 获取当前能量
     */
    public int getEnergy() {
        return ItemMechanicalCore.getEnergy(coreStack);
    }

    /**
     * 获取最大能量
     */
    public int getMaxEnergy() {
        return ItemMechanicalCore.getMaxEnergy(coreStack);
    }

    /**
     * 获取能量百分比
     * @return 0.0 ~ 1.0
     */
    public float getEnergyPercent() {
        int max = getMaxEnergy();
        return max > 0 ? (float) getEnergy() / max : 0f;
    }

    /**
     * 检查是否有足够能量
     */
    public boolean hasEnergy(int amount) {
        return getEnergy() >= amount;
    }

    // ==================== NBT 数据存储 (核心物品) ====================

    /**
     * 获取核心物品的 NBT
     */
    private NBTTagCompound getCoreNBT() {
        if (!coreStack.hasTagCompound()) {
            coreStack.setTagCompound(new NBTTagCompound());
        }
        return coreStack.getTagCompound();
    }

    /**
     * 获取模块专用的 NBT 键名
     */
    private String getModuleKey(String key) {
        return "Module_" + moduleId + "_" + key;
    }

    /**
     * 设置 int 值
     */
    public void setNBT(String key, int value) {
        getCoreNBT().setInteger(getModuleKey(key), value);
    }

    /**
     * 设置 float 值
     */
    public void setNBT(String key, float value) {
        getCoreNBT().setFloat(getModuleKey(key), value);
    }

    /**
     * 设置 double 值
     */
    public void setNBT(String key, double value) {
        getCoreNBT().setDouble(getModuleKey(key), value);
    }

    /**
     * 设置 boolean 值
     */
    public void setNBT(String key, boolean value) {
        getCoreNBT().setBoolean(getModuleKey(key), value);
    }

    /**
     * 设置 String 值
     */
    public void setNBT(String key, String value) {
        getCoreNBT().setString(getModuleKey(key), value);
    }

    /**
     * 设置 long 值
     */
    public void setNBT(String key, long value) {
        getCoreNBT().setLong(getModuleKey(key), value);
    }

    /**
     * 获取 int 值
     */
    public int getNBTInt(String key) {
        return getCoreNBT().getInteger(getModuleKey(key));
    }

    /**
     * 获取 int 值 (带默认值)
     */
    public int getNBTInt(String key, int defaultValue) {
        String fullKey = getModuleKey(key);
        return getCoreNBT().hasKey(fullKey) ? getCoreNBT().getInteger(fullKey) : defaultValue;
    }

    /**
     * 获取 float 值
     */
    public float getNBTFloat(String key) {
        return getCoreNBT().getFloat(getModuleKey(key));
    }

    /**
     * 获取 float 值 (带默认值)
     */
    public float getNBTFloat(String key, float defaultValue) {
        String fullKey = getModuleKey(key);
        return getCoreNBT().hasKey(fullKey) ? getCoreNBT().getFloat(fullKey) : defaultValue;
    }

    /**
     * 获取 double 值
     */
    public double getNBTDouble(String key) {
        return getCoreNBT().getDouble(getModuleKey(key));
    }

    /**
     * 获取 boolean 值
     */
    public boolean getNBTBoolean(String key) {
        return getCoreNBT().getBoolean(getModuleKey(key));
    }

    /**
     * 获取 String 值
     */
    public String getNBTString(String key) {
        return getCoreNBT().getString(getModuleKey(key));
    }

    /**
     * 获取 long 值
     */
    public long getNBTLong(String key) {
        return getCoreNBT().getLong(getModuleKey(key));
    }

    /**
     * 检查键是否存在
     */
    public boolean hasNBT(String key) {
        return getCoreNBT().hasKey(getModuleKey(key));
    }

    /**
     * 移除键
     */
    public void removeNBT(String key) {
        getCoreNBT().removeTag(getModuleKey(key));
    }

    // ==================== 冷却系统 ====================

    private Map<String, Long> getModuleCooldowns() {
        return cooldownCache
                .computeIfAbsent(player.getUniqueID(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(moduleId, k -> new ConcurrentHashMap<>());
    }

    /**
     * 设置冷却时间
     * @param key 冷却键名
     * @param ticks 冷却时间 (tick)
     */
    public void setCooldown(String key, int ticks) {
        getModuleCooldowns().put(key, worldTime + ticks);
    }

    /**
     * 获取剩余冷却时间
     * @return 剩余tick数，0表示冷却结束
     */
    public int getCooldown(String key) {
        Long endTime = getModuleCooldowns().get(key);
        if (endTime == null) return 0;
        long remaining = endTime - worldTime;
        return remaining > 0 ? (int) remaining : 0;
    }

    /**
     * 检查是否在冷却中
     */
    public boolean isOnCooldown(String key) {
        return getCooldown(key) > 0;
    }

    /**
     * 清除冷却
     */
    public void clearCooldown(String key) {
        getModuleCooldowns().remove(key);
    }

    /**
     * 尝试使用技能 (检查冷却并消耗能量)
     * @param cooldownKey 冷却键
     * @param cooldownTicks 冷却时间
     * @param energyCost 能量消耗
     * @return 是否成功
     */
    public boolean tryUseAbility(String cooldownKey, int cooldownTicks, int energyCost) {
        if (isOnCooldown(cooldownKey)) return false;
        if (!hasEnergy(energyCost)) return false;

        consumeEnergy(energyCost);
        setCooldown(cooldownKey, cooldownTicks);
        return true;
    }

    // ==================== 玩家持久数据 ====================

    /**
     * 获取玩家的持久数据 NBT
     */
    public NBTTagCompound getPlayerNBT() {
        return player.getEntityData();
    }

    /**
     * 获取玩家数据的模块键名
     */
    private String getPlayerModuleKey(String key) {
        return "MechCore_" + moduleId + "_" + key;
    }

    /**
     * 设置玩家数据 (int)
     */
    public void setPlayerData(String key, int value) {
        getPlayerNBT().setInteger(getPlayerModuleKey(key), value);
    }

    /**
     * 设置玩家数据 (float)
     */
    public void setPlayerData(String key, float value) {
        getPlayerNBT().setFloat(getPlayerModuleKey(key), value);
    }

    /**
     * 设置玩家数据 (boolean)
     */
    public void setPlayerData(String key, boolean value) {
        getPlayerNBT().setBoolean(getPlayerModuleKey(key), value);
    }

    /**
     * 设置玩家数据 (String)
     */
    public void setPlayerData(String key, String value) {
        getPlayerNBT().setString(getPlayerModuleKey(key), value);
    }

    /**
     * 设置玩家数据 (long)
     */
    public void setPlayerData(String key, long value) {
        getPlayerNBT().setLong(getPlayerModuleKey(key), value);
    }

    /**
     * 获取玩家数据 (int)
     */
    public int getPlayerDataInt(String key) {
        return getPlayerNBT().getInteger(getPlayerModuleKey(key));
    }

    /**
     * 获取玩家数据 (float)
     */
    public float getPlayerDataFloat(String key) {
        return getPlayerNBT().getFloat(getPlayerModuleKey(key));
    }

    /**
     * 获取玩家数据 (boolean)
     */
    public boolean getPlayerDataBoolean(String key) {
        return getPlayerNBT().getBoolean(getPlayerModuleKey(key));
    }

    /**
     * 获取玩家数据 (String)
     */
    public String getPlayerDataString(String key) {
        return getPlayerNBT().getString(getPlayerModuleKey(key));
    }

    /**
     * 获取玩家数据 (long)
     */
    public long getPlayerDataLong(String key) {
        return getPlayerNBT().getLong(getPlayerModuleKey(key));
    }

    /**
     * 检查玩家数据是否存在
     */
    public boolean hasPlayerData(String key) {
        return getPlayerNBT().hasKey(getPlayerModuleKey(key));
    }

    // ==================== 工具方法 ====================

    /**
     * 发送消息给玩家 (ActionBar)
     */
    public void sendActionBar(String message) {
        player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(message), true);
    }

    /**
     * 发送消息给玩家 (Chat)
     */
    public void sendMessage(String message) {
        player.sendMessage(new net.minecraft.util.text.TextComponentString(message));
    }

    /**
     * 播放声音
     */
    public void playSound(net.minecraft.util.SoundEvent sound, float volume, float pitch) {
        player.world.playSound(null, player.posX, player.posY, player.posZ, sound,
                net.minecraft.util.SoundCategory.PLAYERS, volume, pitch);
    }

    /**
     * 在玩家位置生成粒子
     */
    public void spawnParticle(net.minecraft.util.EnumParticleTypes particle,
                              double offsetX, double offsetY, double offsetZ,
                              double speedX, double speedY, double speedZ, int count) {
        if (player.world instanceof net.minecraft.world.WorldServer) {
            ((net.minecraft.world.WorldServer) player.world).spawnParticle(
                    particle,
                    player.posX + offsetX, player.posY + offsetY, player.posZ + offsetZ,
                    count, speedX, speedY, speedZ, 0.0D
            );
        }
    }

    /**
     * 清理玩家的冷却缓存 (在玩家登出时调用)
     */
    public static void clearPlayerCooldowns(UUID playerUUID) {
        cooldownCache.remove(playerUUID);
    }
}
