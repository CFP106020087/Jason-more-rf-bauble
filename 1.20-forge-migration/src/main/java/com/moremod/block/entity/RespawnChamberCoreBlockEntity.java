package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 重生仓核心BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 多方块结构验证
 * - 玩家绑定管理
 * - 重生点设置
 */
public class RespawnChamberCoreBlockEntity extends BlockEntity {

    private UUID boundPlayerUUID;
    private String boundPlayerName = "";
    private boolean structureValid = false;
    private int structureTier = 1;
    private int checkCooldown = 0;

    public RespawnChamberCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESPAWN_CHAMBER_CORE.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        // 定期检查结构
        checkCooldown++;
        if (checkCooldown >= 100) { // 每5秒检查一次
            checkCooldown = 0;
            checkStructure();
        }
    }

    public void checkStructure() {
        if (level == null) return;

        // TODO: 实现多方块结构验证
        // 简化版：假设结构有效
        boolean oldValid = structureValid;
        structureValid = true; // 实际需要检查周围方块
        structureTier = 1;

        if (oldValid != structureValid) {
            setChanged();
        }
    }

    public void bindPlayer(Player player) {
        this.boundPlayerUUID = player.getUUID();
        this.boundPlayerName = player.getName().getString();
        setChanged();

        // 设置玩家重生点
        if (level != null && !level.isClientSide()) {
            player.setRespawnPosition(level.dimension(), worldPosition, 0.0F, true, true);
        }
    }

    public void unbindPlayer() {
        this.boundPlayerUUID = null;
        this.boundPlayerName = "";
        setChanged();
    }

    public boolean hasBoundPlayer() {
        return boundPlayerUUID != null;
    }

    @Nullable
    public UUID getBoundPlayerUUID() {
        return boundPlayerUUID;
    }

    public String getBoundPlayerName() {
        return boundPlayerName;
    }

    public boolean isStructureValid() {
        return structureValid;
    }

    public int getStructureTier() {
        return structureTier;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (boundPlayerUUID != null) {
            tag.putUUID("BoundPlayer", boundPlayerUUID);
            tag.putString("BoundPlayerName", boundPlayerName);
        }
        tag.putBoolean("StructureValid", structureValid);
        tag.putInt("StructureTier", structureTier);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("BoundPlayer")) {
            boundPlayerUUID = tag.getUUID("BoundPlayer");
            boundPlayerName = tag.getString("BoundPlayerName");
        } else {
            boundPlayerUUID = null;
            boundPlayerName = "";
        }
        structureValid = tag.getBoolean("StructureValid");
        structureTier = tag.getInt("StructureTier");
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
