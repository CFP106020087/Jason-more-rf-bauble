package com.moremod.compat;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Champion模组反射工具类
 * 使用完全反射的方式调用Champion API，无需编译时依赖
 */
public class ChampionReflectionHelper {

    // ==========================================
    // Capability注入（软依赖）
    // ==========================================

    @CapabilityInject(IChampionship.class)
    private static Capability<IChampionship> CHAMPION_CAP = null;

    /**
     * IChampionship接口占位符
     * 实际接口来自Champions mod: c4.champions.common.capability.IChampionship
     */
    private interface IChampionship {
        // 占位符接口，实际使用Champions的IChampionship
    }

    // ==========================================
    // 反射缓存
    // ==========================================

    private static Class<?> capabilityChampionshipClass;
    private static Class<?> iChampionshipClass;
    private static Class<?> iRankClass;
    private static Class<?> rankManagerClass;
    private static Class<?> networkHandlerClass;
    private static Class<?> packetSyncAffixClass;

    private static Method getChampionshipMethod;
    private static Method getRankMethod;
    private static Method getTierMethod;
    private static Method getAffixesMethod;
    private static Method getAffixDataMethod;
    private static Method setRankMethod;
    private static Method setNameMethod;
    private static Method setAffixesMethod;
    private static Method setAffixDataMethod;
    private static Method getNameMethod;
    private static Method getEmptyRankMethod;
    private static Method getRankForTierMethod;
    private static Method sendToMethod;
    private static Method getGrowthFactorMethod;

    private static Field championCapField;
    private static Field networkHandlerInstanceField;

    private static Constructor<?> packetSyncAffixConstructor;

    private static boolean initialized = false;
    private static boolean championsAvailable = false;

    // ==========================================
    // 初始化
    // ==========================================

    /**
     * 初始化反射缓存
     */
    private static void init() {
        if (initialized) return;
        initialized = true;

        try {
            // 加载Champion类
            capabilityChampionshipClass = Class.forName("c4.champions.common.capability.CapabilityChampionship");
            iChampionshipClass = Class.forName("c4.champions.common.capability.IChampionship");
            iRankClass = Class.forName("c4.champions.common.rank.IRank");
            rankManagerClass = Class.forName("c4.champions.common.rank.RankManager");
            networkHandlerClass = Class.forName("c4.champions.network.NetworkHandler");
            packetSyncAffixClass = Class.forName("c4.champions.network.PacketSyncAffix");

            // 获取方法
            getChampionshipMethod = capabilityChampionshipClass.getMethod("getChampionship", EntityLiving.class);
            getRankMethod = iChampionshipClass.getMethod("getRank");
            getTierMethod = iRankClass.getMethod("getTier");
            getGrowthFactorMethod = iRankClass.getMethod("getGrowthFactor");
            getAffixesMethod = iChampionshipClass.getMethod("getAffixes");
            getAffixDataMethod = iChampionshipClass.getMethod("getAffixData", String.class);
            setRankMethod = iChampionshipClass.getMethod("setRank", iRankClass);
            setNameMethod = iChampionshipClass.getMethod("setName", String.class);
            setAffixesMethod = iChampionshipClass.getMethod("setAffixes", Set.class);
            setAffixDataMethod = iChampionshipClass.getMethod("setAffixData", Map.class);
            getNameMethod = iChampionshipClass.getMethod("getName");
            getEmptyRankMethod = rankManagerClass.getMethod("getEmptyRank");
            getRankForTierMethod = rankManagerClass.getMethod("getRankForTier", int.class);

            // 获取字段
            championCapField = capabilityChampionshipClass.getField("CHAMPION_CAP");
            networkHandlerInstanceField = networkHandlerClass.getField("INSTANCE");

            // 获取构造器
            packetSyncAffixConstructor = packetSyncAffixClass.getConstructor(
                int.class, int.class, Map.class, String.class
            );

            // 获取NetworkHandler的sendTo方法
            sendToMethod = networkHandlerInstanceField.getType().getMethod("sendTo", IMessage.class, EntityPlayerMP.class);

            championsAvailable = true;
            System.out.println("[ChampionReflection] Champions模组已加载，反射初始化成功");

        } catch (Exception e) {
            championsAvailable = false;
            System.out.println("[ChampionReflection] Champions模组未加载或反射初始化失败: " + e.getMessage());
        }
    }

    /**
     * 检查Champions模组是否可用
     */
    public static boolean isChampionsAvailable() {
        init();
        return championsAvailable;
    }

    // ==========================================
    // Capability访问
    // ==========================================

    /**
     * 获取Champion Capability
     */
    public static Capability<?> getChampionCapability() {
        if (!isChampionsAvailable()) return null;

        try {
            return (Capability<?>) championCapField.get(null);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 获取Capability失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取实体的IChampionship对象
     */
    public static Object getChampionship(EntityLiving entity) {
        if (!isChampionsAvailable() || entity == null) return null;

        try {
            return getChampionshipMethod.invoke(null, entity);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 获取Championship失败: " + e.getMessage());
            return null;
        }
    }

    // ==========================================
    // IChampionship方法
    // ==========================================

    /**
     * 获取Rank对象
     */
    public static Object getRank(Object championship) {
        if (!isChampionsAvailable() || championship == null) return null;

        try {
            return getRankMethod.invoke(championship);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 获取Rank失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取Champion等级
     */
    public static int getTier(Object rank) {
        if (!isChampionsAvailable() || rank == null) return 0;

        try {
            return (Integer) getTierMethod.invoke(rank);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 获取Tier失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 获取成长因子
     */
    public static int getGrowthFactor(Object rank) {
        if (!isChampionsAvailable() || rank == null) return 0;

        try {
            return (Integer) getGrowthFactorMethod.invoke(rank);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 获取GrowthFactor失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 获取词条ID集合
     */
    @SuppressWarnings("unchecked")
    public static Set<String> getAffixes(Object championship) {
        if (!isChampionsAvailable() || championship == null) return new HashSet<>();

        try {
            return (Set<String>) getAffixesMethod.invoke(championship);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 获取Affixes失败: " + e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * 获取指定词条的数据
     */
    public static NBTTagCompound getAffixData(Object championship, String affixId) {
        if (!isChampionsAvailable() || championship == null) return null;

        try {
            return (NBTTagCompound) getAffixDataMethod.invoke(championship, affixId);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 获取AffixData失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 设置Rank
     */
    public static void setRank(Object championship, Object rank) {
        if (!isChampionsAvailable() || championship == null) return;

        try {
            setRankMethod.invoke(championship, rank);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 设置Rank失败: " + e.getMessage());
        }
    }

    /**
     * 设置名称
     */
    public static void setName(Object championship, String name) {
        if (!isChampionsAvailable() || championship == null) return;

        try {
            setNameMethod.invoke(championship, name);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 设置Name失败: " + e.getMessage());
        }
    }

    /**
     * 设置词条集合
     */
    public static void setAffixes(Object championship, Set<String> affixes) {
        if (!isChampionsAvailable() || championship == null) return;

        try {
            setAffixesMethod.invoke(championship, affixes);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 设置Affixes失败: " + e.getMessage());
        }
    }

    /**
     * 设置词条数据Map
     */
    public static void setAffixData(Object championship, Map<String, NBTTagCompound> affixData) {
        if (!isChampionsAvailable() || championship == null) return;

        try {
            setAffixDataMethod.invoke(championship, affixData);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 设置AffixData失败: " + e.getMessage());
        }
    }

    /**
     * 获取名称
     */
    public static String getName(Object championship) {
        if (!isChampionsAvailable() || championship == null) return "";

        try {
            return (String) getNameMethod.invoke(championship);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 获取Name失败: " + e.getMessage());
            return "";
        }
    }

    // ==========================================
    // RankManager方法
    // ==========================================

    /**
     * 获取空Rank
     */
    public static Object getEmptyRank() {
        if (!isChampionsAvailable()) return null;

        try {
            return getEmptyRankMethod.invoke(null);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 获取EmptyRank失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据等级获取Rank
     */
    public static Object getRankForTier(int tier) {
        if (!isChampionsAvailable()) return null;

        try {
            return getRankForTierMethod.invoke(null, tier);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 获取RankForTier失败: " + e.getMessage());
            return null;
        }
    }

    // ==========================================
    // 网络同步
    // ==========================================

    /**
     * 创建PacketSyncAffix数据包
     */
    public static Object createPacketSyncAffix(int entityId, int tier, Map<String, NBTTagCompound> affixData, String name) {
        if (!isChampionsAvailable()) return null;

        try {
            return packetSyncAffixConstructor.newInstance(entityId, tier, affixData, name);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 创建PacketSyncAffix失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 发送数据包到玩家
     */
    public static void sendPacketToPlayer(Object packet, EntityPlayerMP player) {
        if (!isChampionsAvailable() || packet == null || player == null) return;

        try {
            Object networkHandler = networkHandlerInstanceField.get(null);
            sendToMethod.invoke(networkHandler, packet, player);
        } catch (Exception e) {
            System.err.println("[ChampionReflection] 发送数据包失败: " + e.getMessage());
        }
    }

    // ==========================================
    // 便捷方法
    // ==========================================

    /**
     * 检测实体是否为Champion
     */
    public static boolean isChampion(EntityLiving entity) {
        if (!isChampionsAvailable()) return false;

        Object championship = getChampionship(entity);
        if (championship == null) return false;

        Object rank = getRank(championship);
        if (rank == null) return false;

        return getTier(rank) > 0;
    }

    /**
     * 获取实体的Champion等级
     */
    public static int getChampionTier(EntityLiving entity) {
        if (!isChampionsAvailable()) return 0;

        Object championship = getChampionship(entity);
        if (championship == null) return 0;

        Object rank = getRank(championship);
        if (rank == null) return 0;

        return getTier(rank);
    }

    /**
     * 获取实体的词条数量
     */
    public static int getAffixCount(EntityLiving entity) {
        if (!isChampionsAvailable()) return 0;

        Object championship = getChampionship(entity);
        if (championship == null) return 0;

        return getAffixes(championship).size();
    }
}
