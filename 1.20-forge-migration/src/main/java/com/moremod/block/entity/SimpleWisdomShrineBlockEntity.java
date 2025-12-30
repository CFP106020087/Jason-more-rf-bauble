package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 简易智慧之泉BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 多方块结构验证
 * - 解锁村民交易
 * - 加速村民成长
 */
public class SimpleWisdomShrineBlockEntity extends BlockEntity {

    private static final int DEFAULT_RANGE = 8;
    private static final int EFFECT_INTERVAL = 100; // 5秒

    private boolean isFormed = false;
    private int range = DEFAULT_RANGE;
    private int tickCounter = 0;

    public SimpleWisdomShrineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIMPLE_WISDOM_SHRINE.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        tickCounter++;

        // 定期检查结构
        if (tickCounter % 100 == 0) {
            checkStructure();
        }

        // 对范围内村民施加效果
        if (isFormed && tickCounter % EFFECT_INTERVAL == 0) {
            applyEffectsToVillagers();
        }
    }

    public void checkStructure() {
        if (level == null) return;

        boolean oldFormed = isFormed;

        // TODO: 实现3x3x3多方块结构检查
        // 简化版：假设结构总是有效
        isFormed = true;

        if (oldFormed != isFormed) {
            setChanged();
        }
    }

    private void applyEffectsToVillagers() {
        if (level == null || !isFormed) return;

        AABB area = new AABB(
                worldPosition.getX() - range, worldPosition.getY() - range, worldPosition.getZ() - range,
                worldPosition.getX() + range + 1, worldPosition.getY() + range + 1, worldPosition.getZ() + range + 1
        );

        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, area);

        for (Villager villager : villagers) {
            // 加速村民成长（如果是小村民）
            if (villager.isBaby()) {
                // 减少成长时间
                int age = villager.getAge();
                if (age < 0) {
                    villager.setAge(Math.min(0, age + 100)); // 加速成长
                }
            }

            // TODO: 解锁交易功能需要更复杂的逻辑
            // 可能需要通过Mixin或自定义交易系统实现
        }
    }

    public void onBroken() {
        isFormed = false;
        setChanged();
    }

    public boolean isFormed() {
        return isFormed;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("IsFormed", isFormed);
        tag.putInt("Range", range);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isFormed = tag.getBoolean("IsFormed");
        range = tag.contains("Range") ? tag.getInt("Range") : DEFAULT_RANGE;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
}
