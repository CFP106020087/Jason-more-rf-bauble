// com/moremod/api/IUpgradeModule.java
package com.moremod.upgrades.api;

import com.moremod.upgrades.energy.EnergyDepletionManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 升级模块接口 - 最小化版本
 */
public interface IUpgradeModule {
    
    String getModuleId();                // 模块ID (如 "LOOTING_MODULE")
    String getDisplayName();              // 显示名称
    int getMaxLevel();                    // 最大等级
    
    // 被动效果 - 每tick调用
    void onTick(EntityPlayer player, ItemStack core, int level);
    
    // 生命周期
    void onEquip(EntityPlayer player, ItemStack core, int level);
    void onUnequip(EntityPlayer player, ItemStack core, int level);
    
    // 能量
    int getPassiveEnergyCost(int level);  // 被动消耗 (RF/s)
    boolean canRunWithEnergyStatus(EnergyDepletionManager.EnergyStatus status);
    
    // 事件处理 - 主动效果
    void handleEvent(Event event, EntityPlayer player, ItemStack core, int level);
}