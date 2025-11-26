package com.moremod.accessorybox.unlock.rules;

import net.minecraft.entity.player.EntityPlayer;

/**
 * 解锁条件接口
 */
public interface UnlockCondition {
    
    /**
     * 检查玩家是否满足条件
     */
    boolean check(EntityPlayer player);
    
    /**
     * 是否是临时条件(条件失效时重新锁定)
     */
    boolean isTemporary();
    
    /**
     * 获取条件类型名称
     */
    String getType();
    
    /**
     * 获取条件描述(用于调试)
     */
    String getDescription();
}
