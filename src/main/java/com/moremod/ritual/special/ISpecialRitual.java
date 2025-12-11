package com.moremod.ritual.special;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * 特殊仪式接口
 * 所有特殊仪式（如灵魂束缚、不可破坏等）都应实现此接口
 *
 * 设计原则：
 * - 每个仪式独立实现，无需修改 TileEntityRitualCore
 * - 支持 CraftTweaker 修改配方参数
 * - 清晰的生命周期方法
 */
public interface ISpecialRitual {

    // ==================== 基础信息 ====================

    /**
     * 获取仪式ID（唯一标识符）
     * 例如: "soulbound", "unbreakable"
     */
    String getId();

    /**
     * 获取显示名称
     * 例如: "灵魂束缚", "不可破坏"
     */
    String getDisplayName();

    /**
     * 获取仪式描述
     */
    String getDescription();

    /**
     * 获取所需祭坛阶层 (1-3)
     */
    int getRequiredTier();

    /**
     * 获取仪式持续时间（tick）
     */
    int getDuration();

    /**
     * 获取失败概率 (0.0-1.0)
     */
    float getFailChance();

    /**
     * 获取每基座能量消耗
     */
    int getEnergyPerPedestal();

    // ==================== 材料检测 ====================

    /**
     * 获取所需的基座物品列表（用于显示）
     * 返回 List<ItemStack>，每个元素代表一个基座的要求
     */
    List<ItemStack> getRequiredPedestalItems();

    /**
     * 检查中心物品是否有效
     * @param centerItem 祭坛中心的物品
     * @return 是否接受此物品
     */
    boolean isValidCenterItem(ItemStack centerItem);

    /**
     * 检查基座材料是否满足要求
     * @param pedestalItems 当前基座上的物品（最多8个）
     * @return 是否满足材料要求
     */
    boolean checkPedestalMaterials(List<ItemStack> pedestalItems);

    // ==================== 仪式生命周期 ====================

    /**
     * 仪式开始时调用
     * @param context 仪式上下文
     */
    void onStart(RitualContext context);

    /**
     * 仪式每tick调用
     * @param context 仪式上下文
     * @param progress 当前进度 (0 到 getDuration())
     */
    void onTick(RitualContext context, int progress);

    /**
     * 仪式成功完成时调用
     * @param context 仪式上下文
     * @return 处理后的中心物品（可以是修改后的物品或新物品）
     */
    ItemStack onComplete(RitualContext context);

    /**
     * 仪式失败时调用
     * @param context 仪式上下文
     * @return 失败后的中心物品（可返回 ItemStack.EMPTY 表示物品被消耗）
     */
    ItemStack onFail(RitualContext context);

    /**
     * 仪式被中断时调用（玩家离开、能量不足等）
     * @param context 仪式上下文
     */
    default void onInterrupt(RitualContext context) {
        // 默认不做处理
    }

    // ==================== 可选功能 ====================

    /**
     * 是否消耗基座材料
     */
    default boolean consumePedestalItems() {
        return true;
    }

    /**
     * 是否在仪式期间显示粒子效果
     */
    default boolean showParticles() {
        return true;
    }

    /**
     * 获取仪式粒子颜色 (RGB)
     */
    default int getParticleColor() {
        return 0x9966FF; // 默认紫色
    }

    /**
     * 是否启用此仪式
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 仪式上下文 - 提供仪式执行时的所有必要信息
     */
    class RitualContext {
        public final World world;
        public final BlockPos corePos;
        public final EntityPlayer player;
        public final ItemStack centerItem;
        public final List<ItemStack> pedestalItems;
        public final List<BlockPos> pedestalPositions;
        public final int altarTier;

        public RitualContext(World world, BlockPos corePos, EntityPlayer player,
                             ItemStack centerItem, List<ItemStack> pedestalItems,
                             List<BlockPos> pedestalPositions, int altarTier) {
            this.world = world;
            this.corePos = corePos;
            this.player = player;
            this.centerItem = centerItem;
            this.pedestalItems = pedestalItems;
            this.pedestalPositions = pedestalPositions;
            this.altarTier = altarTier;
        }
    }
}
