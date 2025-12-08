package com.moremod.mixin.fermiummixins;

/**
 * Duck interface 匹配 FermiumMixins 的 IEntity 接口
 * 用于覆盖假玩家的 fake entity 检测
 */
public interface IEntityDuck {
    void fermiummixins$setFakeEntity(boolean val);
    boolean fermiummixins$isFakeEntity();
}
