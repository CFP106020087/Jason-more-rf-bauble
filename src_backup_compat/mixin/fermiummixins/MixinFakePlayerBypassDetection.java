package com.moremod.mixin.fermiummixins;

import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin 绕过 FermiumMixins 的假实体检测
 *
 * FermiumMixins 会给所有 Entity 添加 IEntity 接口，
 * 当 fermiummixins$isFakeEntity() 返回 true 时，MoBends 的渲染会被取消。
 *
 * 这个 mixin 确保 FakePlayer（及其子类如 ModFakePlayer）
 * 不会被错误地标记为 "fake entity"，避免功能被阻断。
 */
@Mixin(value = FakePlayer.class, priority = 2000)
public abstract class MixinFakePlayerBypassDetection implements IEntityDuck {

    @Unique
    private boolean moremod$forceNotFake = true;

    /**
     * 覆盖 FermiumMixins 添加的方法
     * 让 FakePlayer 始终报告自己不是 "fake entity"
     */
    @Override
    public boolean fermiummixins$isFakeEntity() {
        return !moremod$forceNotFake;
    }

    /**
     * 允许设置标志，但我们的实现会忽略它
     */
    @Override
    public void fermiummixins$setFakeEntity(boolean val) {
        // 忽略外部设置，始终保持 false
        // 如果需要动态控制，可以修改这里
    }
}
