package com.moremod.capability;

public interface IPlayerTimeData {
    int getTotalDaysPlayed();
    void setTotalDaysPlayed(int days);
    long getLastLoginTime();
    void setLastLoginTime(long time);
    long getTotalPlayTime();
    void setTotalPlayTime(long time);
    boolean hasEquippedTemporalHeart();  // 是否曾经装备过（永久标记）
    void setHasEquippedTemporalHeart(boolean equipped);
    boolean isFirstTimeEquip();  // 新增：是否是第一次装备
    void setFirstTimeEquip(boolean first);
    void addPlayTime(long ticks);
    void copyFrom(IPlayerTimeData other);
}