package com.moremod.api;

/** 提供「工作進度」的最小接口（避免容器用反射/硬轉型） */
public interface IProgressProvider {
    /** 當前進度（0..getWorkMax） */
    int getWorkProgress();
    /** 進度上限（不可為 0） */
    int getWorkMax();
}