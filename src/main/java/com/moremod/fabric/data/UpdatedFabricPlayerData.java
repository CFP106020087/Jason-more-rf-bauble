package com.moremod.fabric.data;

import com.moremod.fabric.system.FabricWeavingSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

/**
 * 更新后的玩家数据管理系统
 */
public class UpdatedFabricPlayerData {
    private static final Map<UUID, PlayerFabricInfo> PLAYER_DATA = new HashMap<>();

    /**
     * 获取玩家的布料数据
     */
    public static PlayerFabricInfo getPlayerData(UUID playerUUID) {
        return PLAYER_DATA.computeIfAbsent(playerUUID, k -> new PlayerFabricInfo());
    }

    /**
     * 获取玩家的布料数据
     */
    public static PlayerFabricInfo getPlayerData(EntityPlayer player) {
        return getPlayerData(player.getUniqueID());
    }

    /**
     * 保存玩家数据到NBT
     */
    public static NBTTagCompound savePlayerData(EntityPlayer player) {
        PlayerFabricInfo info = getPlayerData(player);
        return info.serializeNBT();
    }

    /**
     * 从NBT加载玩家数据
     */
    public static void loadPlayerData(EntityPlayer player, NBTTagCompound nbt) {
        PlayerFabricInfo info = new PlayerFabricInfo();
        info.deserializeNBT(nbt);
        PLAYER_DATA.put(player.getUniqueID(), info);
    }

    /**
     * 清理玩家数据
     */
    public static void clearPlayerData(UUID playerUUID) {
        PLAYER_DATA.remove(playerUUID);
    }

    /**
     * 玩家布料信息类 - 更新版
     */
    public static class PlayerFabricInfo {
        // ===== 深渊布料数据 =====
        public int abyssKills = 0;                    // 击杀数
        public long lastKillTime = 0;                 // 上次击杀时间
        public float abyssAttackBonus = 0;            // 攻击加成
        public int bloodthirstLevel = 0;              // 嗜血等级

        // ===== 时序布料数据 =====
        public LinkedList<TemporalSnapshot> temporalHistory = new LinkedList<>();  // 时间快照
        public long lastTimeStopActivation = 0;       // 上次时停激活时间
        public int rewindCount = 0;                   // 回溯次数（用于递减机制）
        public long lastRewindTime = 0;               // 上次回溯时间（毫秒）
        public float temporalEnergy = 100.0f;         // 时间能量（新增）

        // ===== 时空布料数据 =====
        public float storedDamage = 0;                // 存储的伤害（上限20）
        public int spatialDodgeCount = 0;             // 空间闪避次数
        public long lastTeleportTime = 0;             // 上次传送时间
        public int phaseStrikeCount = 0;              // 相位打击次数
        public float dimensionalEnergy = 100.0f;      // 维度能量（新增）

        // ===== 异界纤维数据 =====
        public int insight = 0;                       // 灵视值（0-100）
        public int sanity = 100;                      // 理智值（0-100）
        public int forbiddenKnowledge = 0;            // 禁忌知识数量
        public int abyssGazeStacks = 0;               // 深渊凝视层数
        public long lastWhisperTime = 0;              // 上次低语时间
        public Map<String, Integer> seenEntities = new HashMap<>();  // 已看到的隐形实体

        // ===== 通用数据 =====
        public Map<FabricType, Integer> fabricPiecesEquipped = new HashMap<>();  // 装备的布料件数
        public Map<FabricType, Long> fabricEquipTime = new HashMap<>();          // 装备时间
        public int totalFabricPower = 0;              // 总布料强度

        /**
         * 更新装备的布料件数
         */
        public void updateEquippedFabrics(EntityPlayer player) {
            fabricPiecesEquipped.clear();
            totalFabricPower = 0;

            for (ItemStack armor : player.getArmorInventoryList()) {
                FabricType type = FabricWeavingSystem.getFabricType(armor);
                if (type != null) {
                    fabricPiecesEquipped.merge(type, 1, Integer::sum);
                    totalFabricPower++;

                    // 记录装备时间
                    if (!fabricEquipTime.containsKey(type)) {
                        fabricEquipTime.put(type, System.currentTimeMillis());
                    }
                }
            }
        }

        /**
         * 获取特定布料的装备件数
         */
        public int getFabricCount(FabricType type) {
            return fabricPiecesEquipped.getOrDefault(type, 0);
        }

        /**
         * 检查是否装备了特定布料
         */
        public boolean hasFabric(FabricType type) {
            return getFabricCount(type) > 0;
        }

        /**
         * 获取装备特定布料的时长（毫秒）
         */
        public long getFabricEquipDuration(FabricType type) {
            if (!fabricEquipTime.containsKey(type)) {
                return 0;
            }
            return System.currentTimeMillis() - fabricEquipTime.get(type);
        }

        /**
         * 序列化到NBT
         */
        public NBTTagCompound serializeNBT() {
            NBTTagCompound nbt = new NBTTagCompound();

            // 深渊布料
            nbt.setInteger("abyssKills", abyssKills);
            nbt.setLong("lastKillTime", lastKillTime);
            nbt.setFloat("abyssAttackBonus", abyssAttackBonus);
            nbt.setInteger("bloodthirstLevel", bloodthirstLevel);

            // 时序布料
            nbt.setLong("lastTimeStopActivation", lastTimeStopActivation);
            nbt.setInteger("rewindCount", rewindCount);
            nbt.setLong("lastRewindTime", lastRewindTime);
            nbt.setFloat("temporalEnergy", temporalEnergy);

            // 保存时间快照
            NBTTagList snapshotList = new NBTTagList();
            for (TemporalSnapshot snapshot : temporalHistory) {
                snapshotList.appendTag(snapshot.serializeNBT());
            }
            nbt.setTag("temporalHistory", snapshotList);

            // 时空布料
            nbt.setFloat("storedDamage", storedDamage);
            nbt.setInteger("spatialDodgeCount", spatialDodgeCount);
            nbt.setLong("lastTeleportTime", lastTeleportTime);
            nbt.setInteger("phaseStrikeCount", phaseStrikeCount);
            nbt.setFloat("dimensionalEnergy", dimensionalEnergy);

            // 异界纤维
            nbt.setInteger("insight", insight);
            nbt.setInteger("sanity", sanity);
            nbt.setInteger("forbiddenKnowledge", forbiddenKnowledge);
            nbt.setInteger("abyssGazeStacks", abyssGazeStacks);
            nbt.setLong("lastWhisperTime", lastWhisperTime);

            // 保存已看到的实体
            NBTTagCompound seenEntitiesNBT = new NBTTagCompound();
            for (Map.Entry<String, Integer> entry : seenEntities.entrySet()) {
                seenEntitiesNBT.setInteger(entry.getKey(), entry.getValue());
            }
            nbt.setTag("seenEntities", seenEntitiesNBT);

            // 通用数据
            nbt.setInteger("totalFabricPower", totalFabricPower);

            // 保存装备的布料件数
            NBTTagCompound fabricEquippedNBT = new NBTTagCompound();
            for (Map.Entry<FabricType, Integer> entry : fabricPiecesEquipped.entrySet()) {
                fabricEquippedNBT.setInteger(entry.getKey().getId(), entry.getValue());
            }
            nbt.setTag("fabricPiecesEquipped", fabricEquippedNBT);

            // 保存装备时间
            NBTTagCompound fabricTimeNBT = new NBTTagCompound();
            for (Map.Entry<FabricType, Long> entry : fabricEquipTime.entrySet()) {
                fabricTimeNBT.setLong(entry.getKey().getId(), entry.getValue());
            }
            nbt.setTag("fabricEquipTime", fabricTimeNBT);

            return nbt;
        }

        /**
         * 从NBT反序列化
         */
        public void deserializeNBT(NBTTagCompound nbt) {
            // 深渊布料
            abyssKills = nbt.getInteger("abyssKills");
            lastKillTime = nbt.getLong("lastKillTime");
            abyssAttackBonus = nbt.getFloat("abyssAttackBonus");
            bloodthirstLevel = nbt.getInteger("bloodthirstLevel");

            // 时序布料
            lastTimeStopActivation = nbt.getLong("lastTimeStopActivation");
            rewindCount = nbt.getInteger("rewindCount");
            lastRewindTime = nbt.getLong("lastRewindTime");
            temporalEnergy = nbt.getFloat("temporalEnergy");

            // 加载时间快照
            temporalHistory.clear();
            NBTTagList snapshotList = nbt.getTagList("temporalHistory", 10); // 10 = NBTTagCompound
            for (int i = 0; i < snapshotList.tagCount() && i < 5; i++) { // 最多5个快照
                TemporalSnapshot snapshot = new TemporalSnapshot();
                snapshot.deserializeNBT(snapshotList.getCompoundTagAt(i));
                temporalHistory.add(snapshot);
            }

            // 时空布料
            storedDamage = nbt.getFloat("storedDamage");
            spatialDodgeCount = nbt.getInteger("spatialDodgeCount");
            lastTeleportTime = nbt.getLong("lastTeleportTime");
            phaseStrikeCount = nbt.getInteger("phaseStrikeCount");
            dimensionalEnergy = nbt.getFloat("dimensionalEnergy");

            // 异界纤维
            insight = nbt.getInteger("insight");
            sanity = nbt.getInteger("sanity");
            forbiddenKnowledge = nbt.getInteger("forbiddenKnowledge");
            abyssGazeStacks = nbt.getInteger("abyssGazeStacks");
            lastWhisperTime = nbt.getLong("lastWhisperTime");

            // 加载已看到的实体
            seenEntities.clear();
            if (nbt.hasKey("seenEntities")) {
                NBTTagCompound seenEntitiesNBT = nbt.getCompoundTag("seenEntities");
                for (String key : seenEntitiesNBT.getKeySet()) {
                    seenEntities.put(key, seenEntitiesNBT.getInteger(key));
                }
            }

            // 通用数据
            totalFabricPower = nbt.getInteger("totalFabricPower");

            // 加载装备的布料件数
            fabricPiecesEquipped.clear();
            if (nbt.hasKey("fabricPiecesEquipped")) {
                NBTTagCompound fabricEquippedNBT = nbt.getCompoundTag("fabricPiecesEquipped");
                for (FabricType type : FabricType.values()) {
                    if (fabricEquippedNBT.hasKey(type.getId())) {
                        fabricPiecesEquipped.put(type, fabricEquippedNBT.getInteger(type.getId()));
                    }
                }
            }

            // 加载装备时间
            fabricEquipTime.clear();
            if (nbt.hasKey("fabricEquipTime")) {
                NBTTagCompound fabricTimeNBT = nbt.getCompoundTag("fabricEquipTime");
                for (FabricType type : FabricType.values()) {
                    if (fabricTimeNBT.hasKey(type.getId())) {
                        fabricEquipTime.put(type, fabricTimeNBT.getLong(type.getId()));
                    }
                }
            }
        }
    }

    /**
     * 时间快照（用于时序布料）
     */
    public static class TemporalSnapshot {
        public double x, y, z;
        public float health;
        public int foodLevel;
        public long timestamp;
        public float experience;  // 新增：经验值

        public TemporalSnapshot() {}

        public TemporalSnapshot(double x, double y, double z, float health, int foodLevel, float experience) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.health = health;
            this.foodLevel = foodLevel;
            this.experience = experience;
            this.timestamp = System.currentTimeMillis();
        }

        public TemporalSnapshot(EntityPlayer player) {
            this.x = player.posX;
            this.y = player.posY;
            this.z = player.posZ;
            this.health = player.getHealth();
            this.foodLevel = player.getFoodStats().getFoodLevel();
            this.experience = player.experience;
            this.timestamp = System.currentTimeMillis();
        }

        public NBTTagCompound serializeNBT() {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setDouble("x", x);
            nbt.setDouble("y", y);
            nbt.setDouble("z", z);
            nbt.setFloat("health", health);
            nbt.setInteger("foodLevel", foodLevel);
            nbt.setFloat("experience", experience);
            nbt.setLong("timestamp", timestamp);
            return nbt;
        }

        public void deserializeNBT(NBTTagCompound nbt) {
            x = nbt.getDouble("x");
            y = nbt.getDouble("y");
            z = nbt.getDouble("z");
            health = nbt.getFloat("health");
            foodLevel = nbt.getInteger("foodLevel");
            experience = nbt.getFloat("experience");
            timestamp = nbt.getLong("timestamp");
        }
    }

    /**
     * 布料类型枚举
     */
    public enum FabricType {
        // 高级织布
        ABYSS("abyss", "深渊布料", 0x8B0000),
        TEMPORAL("temporal", "时序布料", 0x00CED1),
        SPATIAL("spatial", "时空布料", 0x9370DB),
        OTHERWORLD("otherworld", "异界纤维", 0x4B0082),

        // 基础织布（便宜版）
        RESILIENT("resilient", "坚韧纤维", 0x808080),      // 灰色 - 护甲+击退抗性
        VITAL("vital", "活力丝线", 0xFF69B4),              // 粉色 - 生命+回复
        LIGHT("light", "轻盈织物", 0x87CEEB),              // 天蓝 - 速度+减摔落
        PREDATOR("predator", "掠食者布料", 0xB22222),      // 暗红 - 攻击+流血
        SIPHON("siphon", "吸魂织带", 0x228B22);            // 深绿 - 击杀回复

        private final String id;
        private final String displayName;
        private final int color;

        FabricType(String id, String displayName, int color) {
            this.id = id;
            this.displayName = displayName;
            this.color = color;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public int getColor() { return color; }

        // 检查是否为基础织布
        public boolean isBasicFabric() {
            return this == RESILIENT || this == VITAL || this == LIGHT || this == PREDATOR || this == SIPHON;
        }
    }
}