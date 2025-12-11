package com.moremod.ritual.special;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.*;

/**
 * 特殊仪式抽象基类
 * 提供通用实现，简化新仪式的开发
 *
 * 继承此类只需要实现几个核心方法：
 * - getId()
 * - getDisplayName()
 * - getDescription()
 * - isValidCenterItem()
 * - onComplete()
 *
 * 可选覆盖：
 * - onFail() - 自定义失败处理
 * - onTick() - 自定义进度特效
 * - getParticleColor() - 自定义粒子颜色
 */
public abstract class AbstractSpecialRitual implements ISpecialRitual {

    protected final int requiredTier;
    protected final int duration;
    protected final float failChance;
    protected final int energyPerPedestal;
    protected final List<PedestalRequirement> pedestalRequirements;

    /**
     * 构造函数
     * @param requiredTier 所需祭坛阶层 (1-3)
     * @param duration 持续时间（tick）
     * @param failChance 失败率 (0.0-1.0)
     * @param energyPerPedestal 每基座能量消耗
     */
    protected AbstractSpecialRitual(int requiredTier, int duration, float failChance, int energyPerPedestal) {
        this.requiredTier = Math.max(1, Math.min(3, requiredTier));
        this.duration = duration;
        this.failChance = Math.max(0, Math.min(1, failChance));
        this.energyPerPedestal = energyPerPedestal;
        this.pedestalRequirements = new ArrayList<>();
    }

    // ==================== 基础信息实现 ====================

    @Override
    public int getRequiredTier() {
        return requiredTier;
    }

    @Override
    public int getDuration() {
        return SpecialRitualRegistry.getEffectiveDuration(this);
    }

    @Override
    public float getFailChance() {
        return SpecialRitualRegistry.getEffectiveFailChance(this);
    }

    @Override
    public int getEnergyPerPedestal() {
        return SpecialRitualRegistry.getEffectiveEnergyPerPedestal(this);
    }

    // ==================== 材料系统 ====================

    /**
     * 添加基座材料要求
     * @param item 物品
     * @param count 数量
     */
    protected void addPedestalRequirement(Item item, int count) {
        pedestalRequirements.add(new PedestalRequirement(new ItemStack(item), count));
    }

    /**
     * 添加基座材料要求
     * @param stack 物品堆（包含元数据）
     * @param count 数量
     */
    protected void addPedestalRequirement(ItemStack stack, int count) {
        pedestalRequirements.add(new PedestalRequirement(stack.copy(), count));
    }

    @Override
    public List<ItemStack> getRequiredPedestalItems() {
        List<ItemStack> items = new ArrayList<>();
        for (PedestalRequirement req : pedestalRequirements) {
            for (int i = 0; i < req.count; i++) {
                items.add(req.stack.copy());
            }
        }
        return items;
    }

    @Override
    public boolean checkPedestalMaterials(List<ItemStack> pedestalItems) {
        // 检查 CraftTweaker 覆盖
        SpecialRitualRegistry.RitualOverrides override = SpecialRitualRegistry.getOverride(getId());
        if (override != null && override.pedestalItems != null) {
            return checkMaterialsAgainstList(pedestalItems, override.pedestalItems);
        }

        // 使用默认要求
        return checkMaterialsAgainstRequirements(pedestalItems);
    }

    /**
     * 检查基座物品是否满足要求列表
     */
    protected boolean checkMaterialsAgainstRequirements(List<ItemStack> pedestalItems) {
        // 统计每种物品的数量
        Map<String, Integer> available = new HashMap<>();
        for (ItemStack stack : pedestalItems) {
            if (stack.isEmpty()) continue;
            String key = getItemKey(stack);
            available.merge(key, stack.getCount(), Integer::sum);
        }

        // 检查每个要求
        for (PedestalRequirement req : pedestalRequirements) {
            String key = getItemKey(req.stack);
            int have = available.getOrDefault(key, 0);
            if (have < req.count) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查基座物品是否满足指定列表
     */
    protected boolean checkMaterialsAgainstList(List<ItemStack> pedestalItems, List<ItemStack> required) {
        // 统计可用物品
        Map<String, Integer> available = new HashMap<>();
        for (ItemStack stack : pedestalItems) {
            if (stack.isEmpty()) continue;
            String key = getItemKey(stack);
            available.merge(key, stack.getCount(), Integer::sum);
        }

        // 统计需要的物品
        Map<String, Integer> needed = new HashMap<>();
        for (ItemStack stack : required) {
            if (stack.isEmpty()) continue;
            String key = getItemKey(stack);
            needed.merge(key, stack.getCount(), Integer::sum);
        }

        // 检查是否满足
        for (Map.Entry<String, Integer> entry : needed.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取物品的唯一标识键
     */
    protected String getItemKey(ItemStack stack) {
        return stack.getItem().getRegistryName() + "@" + stack.getMetadata();
    }

    // ==================== 生命周期默认实现 ====================

    @Override
    public void onStart(RitualContext context) {
        // 播放开始音效
        context.world.playSound(null, context.corePos,
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS,
                1.0f, 1.0f);

        // 发送消息
        if (context.player != null) {
            context.player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "✦ " + getDisplayName() + " 仪式开始..."));
        }
    }

    @Override
    public void onTick(RitualContext context, int progress) {
        if (!showParticles()) return;

        World world = context.world;
        BlockPos pos = context.corePos;

        // 每5tick生成粒子
        if (progress % 5 == 0 && world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;

            // 中心粒子
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 1.5;
            double cz = pos.getZ() + 0.5;

            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    cx, cy, cz, 3,
                    0.3, 0.3, 0.3, 0.05);

            // 基座连线粒子
            for (BlockPos pedestal : context.pedestalPositions) {
                double px = pedestal.getX() + 0.5;
                double py = pedestal.getY() + 1.2;
                double pz = pedestal.getZ() + 0.5;

                // 从基座向中心发射粒子
                double dx = (cx - px) * 0.1;
                double dy = (cy - py) * 0.1;
                double dz = (cz - pz) * 0.1;

                ws.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                        px, py, pz, 1,
                        dx, dy, dz, 0.5);
            }
        }

        // 每秒播放环境音效
        if (progress % 20 == 0) {
            world.playSound(null, pos,
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS,
                    0.5f, 1.2f + world.rand.nextFloat() * 0.2f);
        }
    }

    @Override
    public ItemStack onFail(RitualContext context) {
        // 默认失败处理：返回原物品（不消耗）
        World world = context.world;
        BlockPos pos = context.corePos;

        // 失败音效
        world.playSound(null, pos,
                SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS,
                1.0f, 0.5f);

        // 失败粒子
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    20, 0.5, 0.5, 0.5, 0.1);
        }

        // 发送失败消息
        if (context.player != null) {
            context.player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ " + getDisplayName() + " 仪式失败！"));
        }

        return context.centerItem; // 默认返回原物品
    }

    @Override
    public void onInterrupt(RitualContext context) {
        // 中断音效
        context.world.playSound(null, context.corePos,
                SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS,
                0.5f, 1.0f);

        if (context.player != null) {
            context.player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚠ " + getDisplayName() + " 仪式被中断"));
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 发送成功消息给玩家
     */
    protected void sendSuccessMessage(EntityPlayer player, String message) {
        if (player != null) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ " + message));
        }
    }

    /**
     * 发送失败消息给玩家
     */
    protected void sendFailMessage(EntityPlayer player, String message) {
        if (player != null) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ " + message));
        }
    }

    /**
     * 播放成功音效
     */
    protected void playSuccessSound(World world, BlockPos pos) {
        world.playSound(null, pos,
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS,
                1.0f, 1.0f);
    }

    /**
     * 生成成功粒子效果
     */
    protected void spawnSuccessParticles(World world, BlockPos pos) {
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.TOTEM,
                    pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    50, 0.5, 1.0, 0.5, 0.2);
        }
    }

    /**
     * 基座材料要求
     */
    protected static class PedestalRequirement {
        public final ItemStack stack;
        public final int count;

        public PedestalRequirement(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }
    }
}
