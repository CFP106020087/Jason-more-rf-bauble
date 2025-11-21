package com.moremod.synergy.tile;

import net.minecraft.tileentity.TileEntity;

/**
 * Synergy Linker TileEntity
 *
 * 说明：
 * - 这是一个非常简单的 TileEntity
 * - 不存储任何数据（所有数据在玩家 NBT 中）
 * - 仅用于标记方块有 TileEntity（某些功能可能需要）
 */
public class TileEntitySynergyLinker extends TileEntity {

    public TileEntitySynergyLinker() {
        // 空构造器，不需要存储数据
    }

    // 不需要 readFromNBT/writeToNBT
    // 因为所有激活状态都存储在玩家 NBT 中
}
