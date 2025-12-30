package com.moremod.tile;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;

/**
 * 3D打印机方块实体
 * 
 * 简单示例 - 你可以根据需要扩展功能：
 * - 物品存储
 * - 打印进度
 * - 能量消耗
 * - 等等
 */
public class TileEntity3DPrinter extends TileEntity implements ITickable {
    
    // 动画计时器（如果需要基于TileEntity的时间而不是世界时间）
    private int animationTick = 0;
    
    // 打印状态
    private boolean isPrinting = false;
    private int printProgress = 0;
    private int printTime = 200; // 10秒完成一次打印
    
    @Override
    public void update() {
        if (isPrinting) {
            printProgress++;
            if (printProgress >= printTime) {
                // 打印完成
                isPrinting = false;
                printProgress = 0;
                onPrintComplete();
            }
        }
        
        // 动画始终运行
        animationTick++;
    }
    
    /**
     * 获取动画进度（用于渲染器）
     */
    public float getAnimationTick(float partialTicks) {
        return animationTick + partialTicks;
    }
    
    /**
     * 获取打印进度 (0.0 - 1.0)
     */
    public float getPrintProgress() {
        return (float) printProgress / printTime;
    }
    
    /**
     * 开始打印
     */
    public void startPrinting() {
        if (!isPrinting) {
            isPrinting = true;
            printProgress = 0;
            markDirty();
        }
    }
    
    /**
     * 打印完成时调用
     */
    protected void onPrintComplete() {
        // 在这里实现打印完成逻辑
        // 例如：生成物品、播放声音等
    }
    
    public boolean isPrinting() {
        return isPrinting;
    }
}