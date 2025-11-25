package com.moremod.synergy.core;

import com.moremod.synergy.api.IInstalledModuleView;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Synergy 上下文
 *
 * 封装 Synergy 条件判断和效果执行所需的所有信息。
 * 这是一个不可变的数据传输对象。
 *
 * 包含：
 * - 触发事件
 * - 玩家信息
 * - 已安装模块快照
 * - 目标实体（如果有）
 * - 自定义数据存储
 */
public class SynergyContext {

    // ==================== 核心字段 ====================

    private final EntityPlayer player;
    private final ItemStack mechanicalCore;
    private final List<IInstalledModuleView> modules;
    private final Set<String> activeModuleIds;
    private final Event triggerEvent;
    private final SynergyEventType eventType;

    // ==================== 可选字段 ====================

    private final EntityLivingBase target;
    private final float originalDamage;
    private final Map<String, Object> customData;

    // ==================== 构造器 ====================

    private SynergyContext(Builder builder) {
        this.player = builder.player;
        this.mechanicalCore = builder.mechanicalCore;
        this.modules = Collections.unmodifiableList(new ArrayList<>(builder.modules));
        this.triggerEvent = builder.triggerEvent;
        this.eventType = builder.eventType;
        this.target = builder.target;
        this.originalDamage = builder.originalDamage;
        this.customData = Collections.unmodifiableMap(new HashMap<>(builder.customData));

        // 预计算激活模块 ID 集合
        Set<String> activeIds = new HashSet<>();
        for (IInstalledModuleView module : this.modules) {
            if (module.isActive()) {
                activeIds.add(module.getModuleId().toUpperCase(Locale.ROOT));
            }
        }
        this.activeModuleIds = Collections.unmodifiableSet(activeIds);
    }

    // ==================== 访问器 ====================

    /**
     * 获取触发 Synergy 的玩家
     */
    @Nonnull
    public EntityPlayer getPlayer() {
        return player;
    }

    /**
     * 获取玩家的机械核心
     */
    @Nullable
    public ItemStack getMechanicalCore() {
        return mechanicalCore;
    }

    /**
     * 获取所有已安装模块的视图（只读）
     */
    @Nonnull
    public List<IInstalledModuleView> getModules() {
        return modules;
    }

    /**
     * 获取所有激活模块的 ID 集合（大写）
     */
    @Nonnull
    public Set<String> getActiveModuleIds() {
        return activeModuleIds;
    }

    /**
     * 获取触发的原始事件
     */
    @Nullable
    public Event getTriggerEvent() {
        return triggerEvent;
    }

    /**
     * 获取事件类型
     */
    @Nonnull
    public SynergyEventType getEventType() {
        return eventType;
    }

    /**
     * 获取目标实体（攻击/伤害事件中的目标）
     */
    @Nullable
    public EntityLivingBase getTarget() {
        return target;
    }

    /**
     * 获取原始伤害值（伤害相关事件）
     */
    public float getOriginalDamage() {
        return originalDamage;
    }

    // ==================== 便捷方法 ====================

    /**
     * 检查玩家是否拥有指定的激活模块
     *
     * @param moduleId 模块ID（不区分大小写）
     * @return true 如果模块激活
     */
    public boolean hasActiveModule(String moduleId) {
        return activeModuleIds.contains(moduleId.toUpperCase(Locale.ROOT));
    }

    /**
     * 检查玩家是否同时拥有所有指定的激活模块
     *
     * @param moduleIds 模块ID列表
     * @return true 如果所有模块都激活
     */
    public boolean hasAllActiveModules(String... moduleIds) {
        for (String id : moduleIds) {
            if (!hasActiveModule(id)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查玩家是否拥有任一指定的激活模块
     *
     * @param moduleIds 模块ID列表
     * @return true 如果任一模块激活
     */
    public boolean hasAnyActiveModule(String... moduleIds) {
        for (String id : moduleIds) {
            if (hasActiveModule(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取指定模块的等级
     *
     * @param moduleId 模块ID
     * @return 模块等级，未安装返回 0
     */
    public int getModuleLevel(String moduleId) {
        String normalizedId = moduleId.toUpperCase(Locale.ROOT);
        for (IInstalledModuleView module : modules) {
            if (module.getModuleId().toUpperCase(Locale.ROOT).equals(normalizedId)) {
                return module.getLevel();
            }
        }
        return 0;
    }

    /**
     * 获取指定模块的视图
     *
     * @param moduleId 模块ID
     * @return 模块视图，未安装返回 null
     */
    @Nullable
    public IInstalledModuleView getModule(String moduleId) {
        String normalizedId = moduleId.toUpperCase(Locale.ROOT);
        for (IInstalledModuleView module : modules) {
            if (module.getModuleId().toUpperCase(Locale.ROOT).equals(normalizedId)) {
                return module;
            }
        }
        return null;
    }

    /**
     * 获取自定义数据
     *
     * @param key 键
     * @param type 期望类型
     * @return 值，如果不存在或类型不匹配返回 null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCustomData(String key, Class<T> type) {
        Object value = customData.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 获取激活模块的数量
     */
    public int getActiveModuleCount() {
        return activeModuleIds.size();
    }

    /**
     * 目标是否为怪物（非玩家生物）
     */
    public boolean isTargetMonster() {
        return target != null && !(target instanceof EntityPlayer);
    }

    /**
     * 目标是否为玩家
     */
    public boolean isTargetPlayer() {
        return target instanceof EntityPlayer;
    }

    // ==================== Builder ====================

    public static Builder builder(EntityPlayer player) {
        return new Builder(player);
    }

    public static class Builder {
        private final EntityPlayer player;
        private ItemStack mechanicalCore = ItemStack.EMPTY;
        private List<IInstalledModuleView> modules = new ArrayList<>();
        private Event triggerEvent = null;
        private SynergyEventType eventType = SynergyEventType.TICK;
        private EntityLivingBase target = null;
        private float originalDamage = 0f;
        private Map<String, Object> customData = new HashMap<>();

        public Builder(EntityPlayer player) {
            this.player = Objects.requireNonNull(player, "Player cannot be null");
        }

        public Builder mechanicalCore(ItemStack core) {
            this.mechanicalCore = core != null ? core : ItemStack.EMPTY;
            return this;
        }

        public Builder modules(List<IInstalledModuleView> modules) {
            this.modules = modules != null ? modules : new ArrayList<>();
            return this;
        }

        public Builder triggerEvent(Event event) {
            this.triggerEvent = event;
            return this;
        }

        public Builder eventType(SynergyEventType type) {
            this.eventType = type != null ? type : SynergyEventType.TICK;
            return this;
        }

        public Builder target(EntityLivingBase target) {
            this.target = target;
            return this;
        }

        public Builder originalDamage(float damage) {
            this.originalDamage = damage;
            return this;
        }

        public Builder customData(String key, Object value) {
            this.customData.put(key, value);
            return this;
        }

        public SynergyContext build() {
            return new SynergyContext(this);
        }
    }
}
