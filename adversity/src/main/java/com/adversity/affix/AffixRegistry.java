package com.adversity.affix;

import com.adversity.Adversity;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 词条注册表 - 管理所有已注册的词条
 */
public class AffixRegistry {

    private static final Map<ResourceLocation, IAffix> REGISTRY = new LinkedHashMap<>();
    private static final List<IAffix> AFFIXES_BY_TYPE_OFFENSIVE = new ArrayList<>();
    private static final List<IAffix> AFFIXES_BY_TYPE_DEFENSIVE = new ArrayList<>();
    private static final List<IAffix> AFFIXES_BY_TYPE_UTILITY = new ArrayList<>();
    private static final List<IAffix> AFFIXES_BY_TYPE_SPECIAL = new ArrayList<>();

    private static boolean initialized = false;

    /**
     * 初始化词条注册表
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        Adversity.LOGGER.info("Initializing Affix Registry");

        // 在这里注册内置词条
        // registerBuiltinAffixes();

        Adversity.LOGGER.info("Affix Registry initialized with {} affixes", REGISTRY.size());
    }

    /**
     * 注册词条
     */
    public static void register(IAffix affix) {
        if (affix == null) {
            throw new IllegalArgumentException("Cannot register null affix");
        }

        ResourceLocation id = affix.getId();
        if (REGISTRY.containsKey(id)) {
            Adversity.LOGGER.warn("Affix with id {} is already registered, skipping", id);
            return;
        }

        REGISTRY.put(id, affix);

        // 按类型分类
        switch (affix.getType()) {
            case OFFENSIVE:
                AFFIXES_BY_TYPE_OFFENSIVE.add(affix);
                break;
            case DEFENSIVE:
                AFFIXES_BY_TYPE_DEFENSIVE.add(affix);
                break;
            case UTILITY:
                AFFIXES_BY_TYPE_UTILITY.add(affix);
                break;
            case SPECIAL:
                AFFIXES_BY_TYPE_SPECIAL.add(affix);
                break;
        }

        Adversity.LOGGER.debug("Registered affix: {}", id);
    }

    /**
     * 根据 ID 获取词条
     */
    @Nullable
    public static IAffix getAffix(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    /**
     * 根据字符串 ID 获取词条
     */
    @Nullable
    public static IAffix getAffix(String id) {
        return getAffix(new ResourceLocation(id));
    }

    /**
     * 获取所有已注册的词条
     */
    public static Collection<IAffix> getAllAffixes() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /**
     * 获取指定类型的所有词条
     */
    public static List<IAffix> getAffixesByType(AffixType type) {
        switch (type) {
            case OFFENSIVE:
                return Collections.unmodifiableList(AFFIXES_BY_TYPE_OFFENSIVE);
            case DEFENSIVE:
                return Collections.unmodifiableList(AFFIXES_BY_TYPE_DEFENSIVE);
            case UTILITY:
                return Collections.unmodifiableList(AFFIXES_BY_TYPE_UTILITY);
            case SPECIAL:
                return Collections.unmodifiableList(AFFIXES_BY_TYPE_SPECIAL);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 获取已注册词条数量
     */
    public static int getAffixCount() {
        return REGISTRY.size();
    }

    /**
     * 检查词条是否已注册
     */
    public static boolean isRegistered(ResourceLocation id) {
        return REGISTRY.containsKey(id);
    }

    /**
     * 获取所有词条 ID
     */
    public static Set<ResourceLocation> getAffixIds() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }
}
